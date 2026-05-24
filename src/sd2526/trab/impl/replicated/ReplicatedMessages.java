package sd2526.trab.impl.replicated;

import static sd2526.trab.api.java.Result.error;
import static sd2526.trab.api.java.Result.ok;
import static sd2526.trab.api.java.Result.ErrorCode.BAD_REQUEST;
import static sd2526.trab.api.java.Result.ErrorCode.FORBIDDEN;
import static sd2526.trab.api.java.Result.ErrorCode.INTERNAL_ERROR;

import java.time.Duration;
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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import sd2526.trab.api.Message;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.impl.api.java.AdminMessages;
import sd2526.trab.impl.db.DB;
import sd2526.trab.impl.java.clients.Clients;
import sd2526.trab.impl.java.servers.InboxEntry;
import sd2526.trab.impl.java.servers.JavaBaseService;
import sd2526.trab.impl.kafka.KafkaPublisher;
import sd2526.trab.impl.kafka.KafkaSubscriber;
import sd2526.trab.impl.kafka.KafkaUtils;
import sd2526.trab.impl.kafka.RecordProcessor;
import sd2526.trab.impl.utils.JSON;
import sd2526.trab.impl.utils.SyncPoint;

public class ReplicatedMessages extends JavaBaseService implements Messages, AdminMessages {

  private static final Logger Log = Logger.getLogger(ReplicatedMessages.class.getName());

  private static final int REMOTE_COMM_DEADLINE = 90000;
  private static final long MESSAGES_CACHE_EXPIRATION = 30000;

  private static final String KAFKA_ADDR = "localhost:9092, kafka:9092";

  private final String topic;
  private final KafkaPublisher publisher;
  private final SyncPoint syncPoint;

  private final AtomicLong counter = new AtomicLong(0L);

  private final ConcurrentHashMap<String, String> originIdToMid = new ConcurrentHashMap<>();

  private final Cache<String, Message> messagesCache = CacheBuilder.newBuilder()
      .expireAfterWrite(Duration.ofMillis(MESSAGES_CACHE_EXPIRATION))
      .build();

  private final ConcurrentHashMap<String, Long> largestSidFromDomain = new ConcurrentHashMap<>();

  private final ConcurrentHashMap<String, Boolean> deletedMids = new ConcurrentHashMap<>();

  private final JobDispatcher jobs = new JobDispatcher();

  private static ReplicatedMessages instance;

  private ReplicatedMessages() {
    this.topic = "messages-" + THIS_DOMAIN;
    KafkaUtils.createTopic(topic);
    this.publisher = KafkaPublisher.createPublisher(KAFKA_ADDR);
    this.syncPoint = SyncPoint.getSyncPoint();

    try {
      DB.select("SELECT count(*) FROM Message", Long.class);
    } catch (Exception ignored) {

    }

    KafkaSubscriber subscriber = KafkaSubscriber.createSubscriber(KAFKA_ADDR, List.of(topic));
    subscriber.start(new ReplicaConsumer());
    Log.info(() -> "ReplicatedMessages started for domain=" + THIS_DOMAIN + " topic=" + topic);
  }

  public static synchronized ReplicatedMessages getInstance() {
    if (instance == null)
      instance = new ReplicatedMessages();
    return instance;
  }

  @Override
  public Result<String> postMessage(String pwd, Message msg) {
    System.err.println(">>> postMessage entry: sender=" + msg.getSender() + " originId=" + msg.originId());
    Log.info(() -> "postMessage : pwd=%s, msg=%s".formatted(pwd, msg));
    if (badParams(pwd, msg) || msg.getSender() == null)
      return error(BAD_REQUEST);

    Result<User> uRes = getUser(msg.getSender(), pwd);
    if (!uRes.isOK())
      return error(uRes.error());

    User u = uRes.value();
    msg.setSender("%s <%s@%s>".formatted(u.getDisplayName(), u.getName(), u.getDomain()));

    List<String> local = msg.getDestination().stream().filter(this::isLocalAddress).toList();
    Set<String> known = new HashSet<>(local);
    Set<String> unknown = new HashSet<>();
    if (!local.isEmpty()) {
      Result<Set<String>> chk = Clients.AdminUsersClient.get().checkUsers(local);
      if (chk.isOK()) {
        unknown = chk.value();
        known.removeAll(unknown);
      }
    }

    long offset = publisher.publish(topic, JSON.encode(Operation.post(msg, known, unknown)));
    System.err.println(">>> published at offset=" + offset);
    if (offset < 0)
      return error(INTERNAL_ERROR);
    String res = syncPoint.waitForResult(offset);
    System.err.println(">>> waitForResult returned: " + res); // TODO: remove .err
    return ResultEnvelope.decode(res, String.class);
  }

  @Override
  public Result<Message> getInboxMessage(String name, String mid, String pwd) {
    Log.info(() -> "getInboxMessage : name=%s, mid=%s".formatted(name, mid));
    if (badParams(name, mid, pwd))
      return error(BAD_REQUEST);
    return getUser(name, pwd)
        .then(() -> DB.getOne(new InboxEntry(mid, name), InboxEntry.class))
        .then(() -> DB.getOne(mid, Message.class));
  }

  @Override
  public Result<List<String>> getAllInboxMessages(String name, String pwd) {
    Log.info(() -> "getAllInboxMessages : name=%s".formatted(name));
    if (badParams(name, pwd))
      return error(BAD_REQUEST);
    var sql = "SELECT m.mid FROM InboxEntry m WHERE m.recipient = '%s'".formatted(esc(name));
    return getUser(name, pwd).then(() -> DB.select(sql, String.class));
  }

  @Override
  public Result<List<String>> searchInbox(String name, String pwd, String query) {
    Log.info(() -> "searchInbox : name=%s, query=%s".formatted(name, query));
    if (badParams(name, pwd, query))
      return error(BAD_REQUEST);
    var sql = """
        SELECT m.id FROM Message m
        INNER JOIN InboxEntry e
        ON e.mid = m.id
        AND e.recipient = '%s'
        WHERE (upper(m.subject) LIKE '%%%s%%' OR upper(m.contents) LIKE '%%%s%%')
        """.formatted(esc(name), esc(query.toUpperCase()), esc(query.toUpperCase()));
    return getUser(name, pwd).then(() -> DB.select(sql, String.class));
  }

  @Override
  public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
    Log.info(() -> "removeInboxMessage : name=%s, mid=%s".formatted(name, mid));
    if (badParams(name, mid, pwd))
      return error(BAD_REQUEST);

    Result<User> u = getUser(name, pwd);
    if (!u.isOK())
      return error(u.error());

    long offset = publisher.publish(topic, JSON.encode(Operation.removeInbox(name, mid)));
    if (offset < 0)
      return error(INTERNAL_ERROR);
    String res = syncPoint.waitForResult(offset);
    return ResultEnvelope.decode(res, Void.class);
  }

  @Override
  public Result<Void> deleteMessage(String name, String mid, String pwd) {
    Log.info(() -> "deleteMessage : name=%s, mid=%s".formatted(name, mid));
    if (badParams(name, mid, pwd))
      return error(BAD_REQUEST);

    Result<User> u = getUser(name, pwd);
    if (!u.isOK())
      return error(u.error());

    long offset = publisher.publish(topic, JSON.encode(Operation.delete(name, mid)));
    if (offset < 0)
      return error(INTERNAL_ERROR);
    String res = syncPoint.waitForResult(offset);
    return ResultEnvelope.decode(res, Void.class);
  }

  @Override
  public Result<Void> remotePostMessage(Message msg) {
    Log.info(() -> "remotePostMessage : msg=%s".formatted(msg));

    List<String> local = msg.getDestination().stream().filter(this::isLocalAddress).toList();
    Set<String> known = new HashSet<>(local);
    Set<String> unknown = new HashSet<>();
    if (!local.isEmpty()) {
      Result<Set<String>> chk = Clients.AdminUsersClient.get().checkUsers(local);
      if (chk.isOK()) {
        unknown = chk.value();
        known.removeAll(unknown);
      }
    }

    long offset = publisher.publish(topic, JSON.encode(Operation.remotePost(msg, known, unknown)));
    if (offset < 0)
      return error(INTERNAL_ERROR);
    String res = syncPoint.waitForResult(offset);
    return ResultEnvelope.decode(res, Void.class);
  }

  @Override
  public Result<Void> remoteDeleteMessage(String mid) {
    Log.info(() -> "remoteDeleteMessage : mid=%s".formatted(mid));
    long offset = publisher.publish(topic, JSON.encode(Operation.remoteDelete(mid)));
    if (offset < 0)
      return error(INTERNAL_ERROR);
    String res = syncPoint.waitForResult(offset);
    return ResultEnvelope.decode(res, Void.class);
  }

  @Override
  public Result<Void> remoteDeleteUserInbox(String name) {
    Log.info(() -> "remoteDeleteUserInbox : name=%s".formatted(name));
    long offset = publisher.publish(topic, JSON.encode(Operation.remoteDeleteInbox(name)));
    if (offset < 0)
      return error(INTERNAL_ERROR);
    String res = syncPoint.waitForResult(offset);
    return ResultEnvelope.decode(res, Void.class);
  }

  private final class ReplicaConsumer implements RecordProcessor {
    @Override
    public void onReceive(ConsumerRecord<String, String> r) {
      long offset = r.offset();
      Operation op = JSON.decode(r.value(), Operation.class);
      String resultJson;
      try {
        resultJson = switch (op.type()) {
          case Operation.POST -> applyPost(JSON.decode(op.payload(), Operation.PostPayload.class));
          case Operation.REMOVE_INBOX -> applyRemoveInbox(
              JSON.decode(op.payload(), Operation.RemoveInboxPayload.class));
          case Operation.DELETE -> applyDelete(JSON.decode(op.payload(), Operation.DeletePayload.class));
          case Operation.REMOTE_POST -> applyRemotePost(
              JSON.decode(op.payload(), Operation.RemotePostPayload.class));
          case Operation.REMOTE_DELETE -> applyRemoteDelete(
              JSON.decode(op.payload(), Operation.RemoteDeletePayload.class));
          case Operation.REMOTE_DELETE_INBOX -> applyRemoteDeleteInbox(
              JSON.decode(op.payload(), Operation.RemoteDeleteInboxPayload.class));
          default -> ResultEnvelope.encode(Result.<Void>error(INTERNAL_ERROR));
        };
      } catch (Exception x) {
        x.printStackTrace();
        resultJson = ResultEnvelope.encode(Result.<Void>error(INTERNAL_ERROR));
      }
      syncPoint.setResult(offset, resultJson);
    }
  }

  private String applyPost(Operation.PostPayload p) {
    Message msg = p.msg();

    String existing = originIdToMid.get(msg.originId());
    if (existing != null) {
      return ResultEnvelope.encode(Result.ok(existing));
    }

    String mid = "%s+%04d".formatted(THIS_DOMAIN, counter.incrementAndGet());
    msg.setId(mid);
    originIdToMid.put(msg.originId(), mid);
    messagesCache.put(mid, new Message(msg));

    if (!p.knownLocal().isEmpty())
      persistLocally(p.knownLocal(), msg);
    if (!p.unknownLocal().isEmpty())
      generateUnknownNotifications(p.unknownLocal(), msg);

    Set<String> remote = msg.getDestination().stream()
        .filter(Predicate.not(this::isLocalAddress))
        .collect(Collectors.toSet());
    if (!remote.isEmpty())
      dispatchRemote(remote, msg);

    return ResultEnvelope.encode(Result.ok(mid));
  }

  private String applyRemoveInbox(Operation.RemoveInboxPayload p) {
    Result<Void> r = DB.deleteOne(new InboxEntry(p.mid(), p.name())).mapToVoid();
    return ResultEnvelope.encode(r);
  }

  private String applyDelete(Operation.DeletePayload p) {
    Message cached = messagesCache.getIfPresent(p.mid());
    if (cached == null) {
      Result<Message> fromDB = DB.getOne(p.mid(), Message.class);
      if (!fromDB.isOK())
        return ResultEnvelope.encode(Result.<Void>error(FORBIDDEN));
      cached = fromDB.value();
    }
    if (!p.name().equals(getName(cached.senderAddress())))
      return ResultEnvelope.encode(Result.<Void>error(FORBIDDEN));

    final Message msg = cached;
    Set<String> domains = msg.getDestination().stream().map(this::getDomain).collect(Collectors.toSet());
    for (String d : domains) {
      if (isLocalDomain(d)) {
        deleteLocally(msg.getId());
        deletedMids.put(msg.getId(), Boolean.TRUE);
      } else {
        jobs.submit(d, () -> reTry(
            () -> Clients.AdminMessagesClient.get(d).remoteDeleteMessage(msg.getId()),
            REMOTE_COMM_DEADLINE));
      }
    }
    return ResultEnvelope.encode(Result.<Void>ok());
  }

  private String applyRemotePost(Operation.RemotePostPayload p) {
    Message msg = p.msg();

    String sourceDomain = sourceDomainOf(msg.getId());
    long sid = sidOf(msg.getId());
    if (sourceDomain != null && sid >= 0) {
      long largest = largestSidFromDomain.getOrDefault(sourceDomain, -1L);
      if (sid <= largest) {
        // Already seen a strictly-greater sid; this is a stale retry.
        return ResultEnvelope.encode(Result.<Void>ok());
      }
      largestSidFromDomain.put(sourceDomain, sid);
    }

    if (deletedMids.containsKey(msg.getId())) {
      return ResultEnvelope.encode(Result.<Void>ok());
    }

    String existing = originIdToMid.get(msg.originId());
    if (existing != null) {
      return ResultEnvelope.encode(Result.<Void>ok());
    }
    originIdToMid.put(msg.originId(), msg.getId());
    messagesCache.put(msg.getId(), new Message(msg));

    if (!p.knownLocal().isEmpty())
      persistLocally(p.knownLocal(), msg);
    if (!p.unknownLocal().isEmpty())
      generateUnknownNotifications(p.unknownLocal(), msg);

    return ResultEnvelope.encode(Result.<Void>ok());
  }

  private String applyRemoteDelete(Operation.RemoteDeletePayload p) {
    deletedMids.put(p.mid(), Boolean.TRUE);
    deleteLocally(p.mid());
    return ResultEnvelope.encode(Result.<Void>ok());
  }

  private String applyRemoteDeleteInbox(Operation.RemoteDeleteInboxPayload p) {
    var sql = "SELECT * FROM InboxEntry e WHERE e.recipient = '%s'".formatted(esc(p.name()));
    DB.transaction(hibernate -> hibernate.select(sql, InboxEntry.class)
        .thenWith(entries -> hibernate.deleteMany(entries)));
    return ResultEnvelope.encode(Result.<Void>ok());
  }

  private void persistLocally(Collection<String> knownAddresses, Message msg) {
    DB.transaction(hibernate -> {
      hibernate.persistOne(msg);
      for (String address : knownAddresses)
        hibernate.persistOne(new InboxEntry(msg.getId(), getName(address)));
      return ok();
    });
  }

  private void generateUnknownNotifications(Set<String> unknown, Message msg) {
    String senderDomain = getDomain(msg.senderAddress());
    for (String addr : unknown) {
      Message err = msg.cloneWithUserNotFound(addr);
      if (isLocalDomain(senderDomain)) {
        DB.transaction(hibernate -> {
          hibernate.persistOne(err);
          hibernate.persistOne(new InboxEntry(err.getId(), msg.senderName()));
          return ok();
        });
      } else {
        jobs.submit(senderDomain, () -> reTry(
            () -> Clients.AdminMessagesClient.get(senderDomain).remotePostMessage(err),
            REMOTE_COMM_DEADLINE));
      }
    }
  }

  private void dispatchRemote(Set<String> remoteAddresses, Message msg) {
    var byDomain = remoteAddresses.stream().collect(
        Collectors.groupingBy(this::getDomain,
            Collectors.mapping(a -> a, Collectors.toSet())));
    for (var e : byDomain.entrySet()) {
      String domain = e.getKey();
      Set<String> addrs = e.getValue();
      jobs.submit(domain, () -> {
        Result<Void> r = reTry(
            () -> Clients.AdminMessagesClient.get(domain).remotePostMessage(msg),
            REMOTE_COMM_DEADLINE);
        if (r.error() == ErrorCode.TIMEOUT) {
          for (String addr : addrs) {
            Message err = msg.cloneWithTimeout(addr);
            DB.transaction(hibernate -> {
              hibernate.persistOne(err);
              hibernate.persistOne(new InboxEntry(err.getId(), msg.senderName()));
              return ok();
            });
          }
        }
      });
    }
  }

  private void deleteLocally(String mid) {
    var sql = "SELECT * FROM InboxEntry e WHERE e.mid = '%s'".formatted(mid);
    DB.transaction(hibernate -> {
      hibernate.getOne(mid, Message.class).thenWith(m -> hibernate.deleteOne(m));
      return hibernate.select(sql, InboxEntry.class)
          .thenWith(entries -> hibernate.deleteMany(entries));
    });
  }

  private static String sourceDomainOf(String mid) {
    if (mid == null)
      return null;
    int i = mid.lastIndexOf('+');
    return i > 0 ? mid.substring(0, i) : null;
  }

  private static long sidOf(String mid) {
    if (mid == null)
      return -1;
    int i = mid.lastIndexOf('+');
    if (i < 0 || i + 1 >= mid.length())
      return -1;
    try {
      return Long.parseLong(mid.substring(i + 1));
    } catch (NumberFormatException x) {
      return -1;
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

  private static String esc(String s) {
    return s == null ? "" : s.replace("'", "''");
  }
}