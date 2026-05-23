package sd2526.trab.impl.zoho;

import static sd2526.trab.api.java.Result.error;
import static sd2526.trab.api.java.Result.ok;
import static sd2526.trab.api.java.Result.ErrorCode.BAD_REQUEST;
import static sd2526.trab.api.java.Result.ErrorCode.FORBIDDEN;
import static sd2526.trab.api.java.Result.ErrorCode.INTERNAL_ERROR;
import static sd2526.trab.api.java.Result.ErrorCode.NOT_FOUND;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import sd2526.trab.api.Message;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.impl.api.java.AdminMessages;
import sd2526.trab.impl.java.clients.Clients;
import sd2526.trab.impl.java.servers.JavaBaseService;
import sd2526.trab.impl.zoho.msgs.ZohoMessageSummary;

public class ZohoMessages extends JavaBaseService implements Messages, AdminMessages {

  private static final Logger Log = Logger.getLogger(ZohoMessages.class.getName());

  private static final int REMOTE_COMM_DEADLINE = 90000;

  private final Zoho zoho = Zoho.getInstance();
  private final AtomicLong counter = new AtomicLong(0L);
  private final JobDispatcher jobs = new JobDispatcher();

  private final ConcurrentHashMap<String, String> originIdCache = new ConcurrentHashMap<>();

  private static ZohoMessages instance;

  private ZohoMessages() {
  }

  public static synchronized ZohoMessages getInstance() {
    if (instance == null)
      instance = new ZohoMessages();
    return instance;
  }

  public void wipeMailbox() {
    try {
      int n = zoho.emptyInbox();
      Log.info(() -> "Wiped " + n + " emails from Zoho mailbox.");
    } catch (Exception x) {
      Log.severe("Failed to wipe mailbox: " + x.getMessage());
    }
  }

  @Override
  public Result<String> postMessage(String pwd, Message msg) {
    Log.info(() -> "postMessage : pwd=%s, msg=%s".formatted(pwd, msg));
    if (badParams(pwd, msg) || msg.getSender() == null)
      return error(BAD_REQUEST);

    return getUser(msg.getSender(), pwd)
        .thenWith(user -> doPost(user, msg));
  }

  @Override
  public Result<Message> getInboxMessage(String name, String mid, String pwd) {
    Log.info(() -> "getInboxMessage : name=%s, mid=%s".formatted(name, mid));
    if (badParams(name, mid, pwd))
      return error(BAD_REQUEST);

    return verifyLocalUser(name, pwd)
        .thenWith(u -> {
          try {
            Message m = findMessageByMid(mid);
            return m != null ? ok(m) : error(NOT_FOUND);
          } catch (Exception x) {
            x.printStackTrace();
            return error(INTERNAL_ERROR);
          }
        });
  }

  @Override
  public Result<List<String>> getAllInboxMessages(String name, String pwd) {
    Log.info(() -> "getAllInboxMessages : name=%s".formatted(name));
    if (badParams(name, pwd))
      return error(BAD_REQUEST);

    return verifyLocalUser(name, pwd)
        .thenWith(u -> {
          try {
            List<String> mids = new ArrayList<>();
            for (ZohoMessageSummary summary : zoho.listInbox()) {
              Message m = fetchAndDecode(summary);
              if (m != null && m.getId() != null)
                mids.add(m.getId());
            }
            return ok(mids);
          } catch (Exception x) {
            x.printStackTrace();
            return error(INTERNAL_ERROR);
          }
        });
  }

  @Override
  public Result<List<String>> searchInbox(String name, String pwd, String query) {
    Log.info(() -> "searchInbox : name=%s, query=%s".formatted(name, query));
    if (badParams(name, pwd, query))
      return error(BAD_REQUEST);

    String needle = query.toUpperCase();
    return verifyLocalUser(name, pwd)
        .thenWith(u -> {
          try {
            List<String> mids = new ArrayList<>();
            for (ZohoMessageSummary summary : zoho.listInbox()) {
              Message m = fetchAndDecode(summary);
              if (m == null)
                continue;
              String subj = m.getSubject() == null ? "" : m.getSubject().toUpperCase();
              String cont = m.getContents() == null ? "" : m.getContents().toUpperCase();
              if (subj.contains(needle) || cont.contains(needle))
                mids.add(m.getId());
            }
            return ok(mids);
          } catch (Exception x) {
            x.printStackTrace();
            return error(INTERNAL_ERROR);
          }
        });
  }

  @Override
  public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
    Log.info(() -> "removeInboxMessage : name=%s, mid=%s".formatted(name, mid));
    if (badParams(name, mid, pwd))
      return error(BAD_REQUEST);

    return verifyLocalUser(name, pwd)
        .thenWith(u -> {
          try {
            String zohoId = findZohoIdByMid(mid);
            if (zohoId == null)
              return error(NOT_FOUND);
            zoho.deleteEmail(zohoId);
            return Result.<Void>ok();
          } catch (Exception x) {
            x.printStackTrace();
            return error(INTERNAL_ERROR);
          }
        });
  }

  @Override
  public Result<Void> deleteMessage(String name, String mid, String pwd) {
    Log.info(() -> "deleteMessage : name=%s, mid=%s".formatted(name, mid));
    if (badParams(name, mid, pwd))
      return error(BAD_REQUEST);

    return verifyLocalUser(name, pwd)
        .thenWith(u -> {
          try {
            Message m = findMessageByMid(mid);
            if (m == null)
              return error(NOT_FOUND);

            String senderName = m.senderName();
            if (!name.equals(senderName))
              return error(FORBIDDEN);

            Set<String> domains = m.getDestination().stream()
                .map(this::getDomain)
                .collect(Collectors.toSet());
            for (String d : domains) {
              if (isLocalDomain(d)) {
                String zohoId = findZohoIdByMid(mid);
                if (zohoId != null)
                  zoho.deleteEmail(zohoId);
              } else {
                jobs.submit(d, () -> reTry(() -> Clients.AdminMessagesClient.get(d).remoteDeleteMessage(mid),
                    REMOTE_COMM_DEADLINE));
              }
            }
            return Result.<Void>ok();
          } catch (Exception x) {
            x.printStackTrace();
            return error(INTERNAL_ERROR);
          }
        });
  }

  @Override
  public Result<Void> remotePostMessage(Message msg) {
    Log.info(() -> "remotePostMessage : msg=%s".formatted(msg));
    try {
      boolean hasLocalRecipient = msg.getDestination().stream().anyMatch(this::isLocalAddress);
      if (hasLocalRecipient) {
        zoho.sendEmail(Zoho.FROM_ADDRESS, msg.getSubject(), MessageBodyCodec.encode(msg));
      }
      return ok();
    } catch (Exception x) {
      x.printStackTrace();
      return error(INTERNAL_ERROR);
    }
  }

  @Override
  public Result<Void> remoteDeleteMessage(String mid) {
    Log.info(() -> "remoteDeleteMessage : mid=%s".formatted(mid));
    try {
      String zohoId = findZohoIdByMid(mid);
      if (zohoId != null)
        zoho.deleteEmail(zohoId);
      return ok();
    } catch (Exception x) {
      x.printStackTrace();
      return error(INTERNAL_ERROR);
    }
  }

  @Override
  public Result<Void> remoteDeleteUserInbox(String name) {
    Log.info(() -> "remoteDeleteUserInbox : name=%s".formatted(name));
    try {
      zoho.emptyInbox();
      return ok();
    } catch (Exception x) {
      x.printStackTrace();
      return error(INTERNAL_ERROR);
    }
  }

  private Result<User> getUser(String userOrAddress, String pwd) {
    try {
      String name = userOrAddress.split("@", 2)[0];
      return Clients.UsersClient.get().getUser(name, pwd);
    } catch (Exception x) {
      x.printStackTrace();
      return error(INTERNAL_ERROR);
    }
  }

  private Result<User> verifyLocalUser(String name, String pwd) {
    Result<User> r = Clients.UsersClient.get().getUser(name, pwd);
    return r;
  }

  private Result<String> doPost(User sender, Message msg) {
    String previousMid = originIdCache.get(msg.originId());
    if (previousMid != null)
      return ok(previousMid);

    msg.setId("%s+%04d".formatted(THIS_DOMAIN, counter.incrementAndGet()));
    originIdCache.put(msg.originId(), msg.getId());
    msg.setSender("%s <%s@%s>".formatted(sender.getDisplayName(), sender.getName(), sender.getDomain()));

    List<String> local = msg.getDestination().stream().filter(this::isLocalAddress).toList();
    Set<String> remote = msg.getDestination().stream()
        .filter(Predicate.not(this::isLocalAddress))
        .collect(Collectors.toSet());

    if (!local.isEmpty()) {
      postLocally(local, msg);
    }

    if (!remote.isEmpty()) {
      var byDomain = remote.stream().collect(
          Collectors.groupingBy(this::getDomain,
              Collectors.mapping(a -> a, Collectors.toSet())));
      for (var e : byDomain.entrySet()) {
        String domain = e.getKey();
        Set<String> addrs = e.getValue();
        jobs.submit(domain, () -> {
          Result<Void> res = reTry(
              () -> Clients.AdminMessagesClient.get(domain).remotePostMessage(msg),
              REMOTE_COMM_DEADLINE);
          if (res.error() == ErrorCode.TIMEOUT) {
            for (String addr : addrs) {
              Message err = msg.cloneWithTimeout(addr);
              postLocally(List.of(msg.senderAddress()), err);
            }
          }
        });
      }
    }

    return ok(msg.getId());
  }

  private void postLocally(Collection<String> addresses, Message msg) {
    if (!addresses.isEmpty()) {
      try {
        zoho.sendEmail(Zoho.FROM_ADDRESS, msg.getSubject(), MessageBodyCodec.encode(msg));
      } catch (Exception x) {
        x.printStackTrace();
      }
    }
  }

  private Message findMessageByMid(String mid) throws Exception {
    for (ZohoMessageSummary summary : zoho.listInbox()) {
      Message m = fetchAndDecode(summary);
      if (m != null && mid.equals(m.getId()))
        return m;
    }
    return null;
  }

  private String findZohoIdByMid(String mid) throws Exception {
    for (ZohoMessageSummary summary : zoho.listInbox()) {
      Message m = fetchAndDecode(summary);
      if (m != null && mid.equals(m.getId()))
        return summary.messageId();
    }
    return null;
  }

  private Message fetchAndDecode(ZohoMessageSummary summary) throws Exception {
    String html = zoho.getEmailContent(summary.messageId());
    if (html == null)
        return null;
    String plain = HtmlStripper.strip(html);
    String subject = HtmlStripper.strip(summary.subject());
    return MessageBodyCodec.decode(plain, subject);
}

  private static final class JobDispatcher {
    private final ConcurrentHashMap<String, ExecutorService> executors = new ConcurrentHashMap<>();

    void submit(String domain, Runnable job) {
      ExecutorService executor = executors.computeIfAbsent(domain,
          d -> Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setUncaughtExceptionHandler((thr, ex) -> ex.printStackTrace());
            return t;
          }));
      executor.submit(job);
    }
  }
}