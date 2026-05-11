package sd2526.trab.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.hibernate.Session;

import sd2526.trab.api.Message;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.client.java.Clients;
import sd2526.trab.server.persistence.Hibernate;
import sd2526.trab.server.persistence.InboxEntry;

public class MessagesService implements Messages {

    private static final long DELETE_LIMIT_MS = 30_000;
    private static final long FORWARD_TIMEOUT_MS = 90_000;
    private static final String REASON_TIMEOUT = "TIMEOUT";
    private static final String REASON_UNKNOWN = "UNKNOWN USER";
    private final Hibernate hibernate;
    private final String domain;
    private static Logger logger = Logger.getLogger(MessagesService.class.getName());

    private sealed interface PendingTask permits ForwardTask, DeleteTask {
    }

    private record ForwardTask(Message msg, String destUser, long enqueuedAt) implements PendingTask {
    }

    private record DeleteTask(String mid, long enqueuedAt) implements PendingTask {
    }

    private final ConcurrentHashMap<String, LinkedBlockingQueue<PendingTask>> domainQueues = new ConcurrentHashMap<>();

    public MessagesService(String domain) {
        this.domain = domain;
        this.hibernate = Hibernate.getInstance();
    }

    private Map<String, Boolean> getLocalDestinationsStatus(Set<String> destinations) {
        Map<String, Boolean> localDestExists = new HashMap<String, Boolean>();
        for (String dest : destinations) {
            String destinationDomain = dest.contains("@") ? dest.split("@")[1] : domain;
            String destinationName = dest.contains("@") ? dest.split("@")[0] : dest;
            if (destinationDomain.equals(domain)) {
                Result<Boolean> exists = Clients.UsersClient.get(domain).userExists(destinationName);
                // not retrying
                localDestExists.put(dest, exists.isOK() && exists.value());
            }
        }

        return localDestExists;
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        logger.info("postMessage: pwd=" + pwd + " ; message=" + msg);

        if (pwd == null || msg == null || msg.getSender() == null
                || msg.getDestination() == null || msg.getDestination().isEmpty()
                || msg.getSubject() == null || msg.getContents() == null) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        // Parse sender
        String senderName = msg.getSender().contains("@") ? msg.getSender().split("@")[0] : msg.getSender();
        String senderDomain = msg.getSender().contains("@") ? msg.getSender().split("@")[1] : domain;

        // Validate sender
        Result<User> userResult = Clients.UsersClient.get(senderDomain).getUser(senderName, pwd);
        if (!userResult.isOK()) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        String displaySender = String.format("%s <%s@%s>", userResult.value().getDisplayName(), senderName,
                senderDomain);

        Map<String, Boolean> localDestExists = getLocalDestinationsStatus(msg.getDestination());

        Result<String> result = hibernate.execute(session -> {
            // Idempotency
            String existingId = findExistingMessage(session, msg, displaySender);
            if (existingId != null) {
                return Result.ok(existingId);
            }

            // Assign ID and transform
            String mid = UUID.randomUUID().toString();
            msg.setId(mid);
            msg.setSender(displaySender);

            session.persist(msg);

            persistLocal(session, msg, localDestExists);

            return Result.ok(mid);
        });

        if (!result.isOK()) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        // Remote destinations — enqueue for async
        Set<String> enqueuedDomains = new HashSet<>();
        for (String destination : msg.getDestination()) {
            String destDomain = destination.contains("@") ? destination.split("@")[1] : domain;
            if (!destDomain.equals(domain) && enqueuedDomains.add(destDomain)) {
                queueFor(destDomain).add(new ForwardTask(msg, destination, System.currentTimeMillis()));
            }
        }

        return result;
    }

    private LinkedBlockingQueue<PendingTask> queueFor(String remoteDomain) {
        return domainQueues.computeIfAbsent(remoteDomain, d -> {
            var queue = new LinkedBlockingQueue<PendingTask>();
            Thread t = new Thread(() -> {
                while (true) {
                    try {
                        PendingTask task = queue.take();

                        Result<?> result;
                        if (task instanceof ForwardTask ft) {
                            long elapsed = System.currentTimeMillis() - ft.enqueuedAt();

                            if (elapsed > FORWARD_TIMEOUT_MS) {
                                // Timeout, local notification

                                hibernate.execute(session -> {
                                    sendFailureNotification(session, ft.msg(), ft.destUser(), REASON_TIMEOUT);
                                    return Result.ok();
                                });
                                continue;
                            }
                            result = tryForwardMessage(d, ft.msg());
                        } else if (task instanceof DeleteTask dt) {
                            long elapsed = System.currentTimeMillis() - dt.enqueuedAt();
                            if (elapsed > FORWARD_TIMEOUT_MS) {
                                logger.warning("Delete message with mid=" + dt.mid() + " timed out.");
                                continue;
                            }

                            result = tryDeleteMessage(d, dt.mid());
                        } else {
                            result = Result.error(Result.ErrorCode.INTERNAL_ERROR);
                        }

                        if (!result.isOK()) {
                            // Put it back, gives priority to untried tasks
                            queue.add(task);
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }, "async-" + d);
            t.setDaemon(true);
            t.start();
            return queue;
        });
    }

    private Result<Void> tryDeleteMessage(String remoteDomain, String mid) {
        logger.info("tryDeleteMessage ; remoteDomain=" + remoteDomain + " ; mid=" + mid);
        return Clients.MessagesClient.get(remoteDomain).deleteInternal(mid);
    }

    private Result<Void> tryForwardMessage(String remoteDomain, Message msg) {
        logger.info("tryForwardMessage ; remoteDomain=" + remoteDomain + " ; msg=" + msg);
        return Clients.MessagesClient.get(remoteDomain).deliverInternal(msg);
    }

    public Result<Void> deleteInternal(String mid) {
        return hibernate.execute(session -> {
            session.createMutationQuery(
                    "DELETE FROM InboxEntry WHERE messageId = :mid")
                    .setParameter("mid", mid)
                    .executeUpdate();

            Message msg = session.find(Message.class, mid);

            if (msg != null) {
                session.remove(msg);
            }

            return Result.ok();
        });
    }

    public Result<Void> deliverInternal(Message msg) {
        logger.info("deliverInternal ; msg=" + msg);
        if (msg == null || msg.getId() == null || msg.getSender() == null
                || msg.getDestination() == null || msg.getDestination().isEmpty()) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        Map<String, Boolean> localDestExists = getLocalDestinationsStatus(msg.getDestination());

        return hibernate.execute(session -> {
            // Idempotency: skip if already stored
            String existingId = findExistingMessage(session, msg, msg.getSender());
            if (existingId != null) {
                return Result.ok();
            }

            session.persist(msg);

            persistLocal(session, msg, localDestExists);

            return Result.ok();
        });
    }

    private void persistLocal(Session session, Message msg, Map<String, Boolean> localDestExists) {
        localDestExists.forEach((destination, exists) -> {
            if (exists) {
                String destName = destination.contains("@") ? destination.split("@")[0] : destination;
                session.persist(
                        new InboxEntry(destName,
                                msg.getId()));
            } else {
                sendFailureNotification(session, msg,
                        destination, REASON_UNKNOWN);
            }
        });
    }

    private void sendFailureNotification(Session session, Message original, String destAddress, String reason) {
        String senderName = original.getSender().contains("<")
                ? original.getSender().split("<")[1].replace(">", "").split("@")[0].trim()
                : original.getSender();
        String senderDomain = original.getSender().contains("<")
                ? original.getSender().split("<")[1].replace(">", "").split("@")[1].replace(">", "").trim()
                : domain;

        Message failMsg = new Message();

        failMsg.setId(original.getId() + "." + destAddress);
        failMsg.setSender(original.getSender());
        failMsg.setDestination(Set.of(senderName + "@" + senderDomain));
        failMsg.setSubject("FAILED TO SEND " + original.getId() + " TO " + destAddress + ": " + reason);
        failMsg.setContents(original.getContents());
        failMsg.setCreationTime(System.currentTimeMillis());

        if (senderDomain.equals(domain)) {
            assert session != null;
            session.persist(failMsg);
            session.persist(new InboxEntry(senderName, failMsg.getId()));
            return;
        } // else we ignore the session

        // destination is remote, enqueue for delivery
        queueFor(senderDomain)
                .add(new ForwardTask(failMsg, senderName + "@" + senderDomain, System.currentTimeMillis()));
    }

    private String findExistingMessage(Session session, Message message, String sender) {
        List<String> ids = session.createQuery(
                "SELECT m.id FROM Message m WHERE m.sender = :sender AND m.subject = :subject AND m.contents = :contents AND m.creationTime = :creationTime",
                String.class)
                .setParameter("sender", sender)
                .setParameter("subject", message.getSubject())
                .setParameter("contents", message.getContents())
                .setParameter("creationTime", message.getCreationTime())
                .getResultList();
        return ids.isEmpty() ? null : ids.get(0);
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        if (name == null || mid == null || pwd == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        var userResult = Clients.UsersClient.get(domain).getUser(name, pwd);
        if (!userResult.isOK())
            return Result.error(Result.ErrorCode.FORBIDDEN);

        return hibernate.execute(session -> {
            List<InboxEntry> entries = session.createQuery(
                    "FROM InboxEntry WHERE userName = :name AND messageId = :mid", InboxEntry.class)
                    .setParameter("name", name)
                    .setParameter("mid", mid)
                    .getResultList();
            if (entries.isEmpty())
                return Result.error(Result.ErrorCode.NOT_FOUND);

            Message msg = session.find(Message.class, mid);
            if (msg == null)
                return Result.error(Result.ErrorCode.NOT_FOUND);

            return Result.ok(msg);
        });
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd) {
        logger.info("getAllInboxMessages : name = " + name + " ; pwd =" + pwd);
        if (name == null || pwd == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        Result<User> userResult = Clients.UsersClient.get(domain).getUser(name, pwd);

        if (!userResult.isOK())
            return Result.error(Result.ErrorCode.FORBIDDEN);

        return hibernate.execute(session -> {

            List<String> ids = session.createQuery(
                    "SELECT i.messageId FROM InboxEntry i WHERE i.userName = :name", String.class)
                    .setParameter("name", name)
                    .getResultList();
            return Result.ok(ids);
        });
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
        if (name == null || mid == null || pwd == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        var userResult = Clients.UsersClient.get(domain).getUser(name, pwd);
        if (!userResult.isOK())
            return Result.error(Result.ErrorCode.FORBIDDEN);

        return hibernate.execute(session -> {
            int deleted = session.createMutationQuery(
                    "DELETE FROM InboxEntry WHERE userName = :name AND messageId = :mid")
                    .setParameter("name", name)
                    .setParameter("mid", mid)
                    .executeUpdate();
            if (deleted == 0) {
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
            return Result.ok();
        });
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        if (name == null || mid == null || pwd == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        // Validate user
        Result<User> userResult = Clients.UsersClient.get(domain).getUser(name, pwd);

        if (!userResult.isOK())
            return Result.error(Result.ErrorCode.FORBIDDEN);

        return hibernate.execute(session -> {
            Message msg = session.find(Message.class, mid);
            if (msg == null)
                return Result.ok(); // silent success

            // Verify sender
            String senderAddress = name + "@" + domain;
            String storedAddress = msg.getSender().contains("<")
                    ? msg.getSender().split("<")[1].replace(">", "").trim()
                    : msg.getSender();
            if (!senderAddress.equals(storedAddress))
                return Result.error(Result.ErrorCode.FORBIDDEN);

            // 30 second window
            if (System.currentTimeMillis() - msg.getCreationTime() > DELETE_LIMIT_MS)
                return Result.ok(); // silent success

            // Delete local inbox entries
            session.createMutationQuery("DELETE FROM InboxEntry i WHERE i.messageId = :mid")
                    .setParameter("mid", mid).executeUpdate();
            session.remove(msg);

            // Async propagate to remote domains
            for (String dest : msg.getDestination()) {
                String destDomain = dest.contains("@") ? dest.split("@")[1] : domain;
                if (!destDomain.equals(domain)) {
                    queueFor(destDomain).add(new DeleteTask(mid, System.currentTimeMillis()));
                }
            }
            return Result.ok();
        });
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query) {
        if (name == null || pwd == null || query == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        var userResult = Clients.UsersClient.get(domain).getUser(name, pwd);
        if (!userResult.isOK())
            return Result.error(Result.ErrorCode.FORBIDDEN);
        String pattern = "%" + query.toLowerCase() + "%";
        List<String> ids = hibernate.jpql(
                "SELECT i.messageId FROM InboxEntry i WHERE i.userName = '" + name +
                        "' AND i.messageId IN (SELECT m.id FROM Message m WHERE LOWER(m.subject) LIKE '" + pattern +
                        "' OR LOWER(m.contents) LIKE '" + pattern + "')",
                String.class);
        return Result.ok(ids);
    }

}
