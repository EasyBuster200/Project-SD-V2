---
name: project-f1-kafka-plan
description: Agreed architecture for F1 Kafka feature — state-machine replication of messages domain
metadata:
  type: project
---

**F1 Kafka feature plan**, agreed with user in handoff. Target: 8/8 pts, tests 109a-116b.

**Why:** spec requires replicating ANY messages domain via state machine replication using Kafka, tolerating failure of any replica. Tester runs 3 replicas per replicated domain (messages0/1/2.ourorg0 etc.) with a single Kafka broker. Kafka is intra-domain only; cross-domain communication still goes via REST (`Clients.AdminMessagesClient`).

**How to apply:** when resuming F1, follow this plan unless user redirects.

**Architecture:** separate `ReplicatedMessages` + `RestReplicatedMessagesServer`, leave `RestMessagesServer` and `ZohoMessages` untouched. `messages.props` already has `MESSAGES_REP_SERVER_MAINCLASS` placeholder.

**Files to create:**
- `src/sd2526/trab/impl/kafka/` — `KafkaPublisher`, `KafkaSubscriber`, `KafkaUtils`, `RecordProcessor`, `SyncPoint` (copy from teacher's lab11 zip, change package decl)
- `src/sd2526/trab/impl/replicated/ReplicatedMessages.java` — parallel of `JavaMessages`, Kafka-driven. Each write publishes Op envelope (JSON) to Kafka, blocks on `SyncPoint.waitForResult(offset)`. Consumer thread executes ops in offset order against local Hibernate DB.
- `src/sd2526/trab/impl/replicated/Operation.java` — discriminated-union record (`type`: POST_MESSAGE, REMOVE_INBOX, DELETE_MESSAGE, REMOTE_POST, REMOTE_DELETE, REMOTE_DELETE_INBOX + args). Use Gson.
- `src/sd2526/trab/impl/rest/servers/RestReplicatedMessagesServer.java`
- `src/sd2526/trab/impl/rest/servers/RestReplicatedMessagesResource.java` — every read calls `syncPoint.waitForVersion(X-MESSAGES-VERSION header)` before serving, returns current offset in response header.

**Key design decisions:**
- One Kafka topic per replicated domain: `messages-<domain>` (e.g. `messages-ourorg0`)
- **MID assigned by consumer**, not publisher — deterministic across replicas because all consume same ops in same order. Op contains `originId` but no mid; consumer generates `"%s+%04d".formatted(domain, counter.incrementAndGet())`.
- **Idempotency cache (originId → mid)** persisted in DB, not in-memory (must survive restart).
- Cross-domain calls go through REST AdminMessagesClient, NOT Kafka.
- Cross-domain RECEIVING (`remotePostMessage`) IS published to local Kafka so all 3 replicas see it.
- Reads served locally after `waitForVersion(headerValue)`, return current offset as `X-MESSAGES-VERSION`.

**messages.props entries needed:**
```
MESSAGES_REP_SERVER_MAINCLASS=sd2526.trab.impl.rest.servers.RestReplicatedMessagesServer
MESSAGES_REP_PORT=4567
MESSAGES_REP_EXTRA_ARGS_FIRST=-secret secret
MESSAGES_REP_EXTRA_ARGS_OTHER=-secret secret
```

**pom.xml dependency:** `org.apache.kafka:kafka-clients:4.2.0`

**Lab 11 helper API** (from teacher's starter):
- `KafkaPublisher.createPublisher(addr)`, `.publish(topic, value) → offset`
- `KafkaSubscriber.createSubscriber(addr, List<topic>)`, `.start(RecordProcessor)` forever-loop thread, `.consume(SubscriberListener)` synchronous
- `KafkaUtils.createTopic(name)` idempotent
- `SyncPoint` singleton — `waitForResult(offset)` blocks until ready, `setResult(offset, res)`, `waitForVersion(offset)` blocks until local state caught up
- Address strings: `"localhost:9092, kafka:9092"`

**Implementation order:**
1. Add kafka-clients dep to pom.xml
2. Copy 5 lab 11 files, fix package decls
3. Write Operation.java
4. Write ReplicatedMessages.java (bulk of work)
5. Write REST server + resource
6. Update messages.props
7. Build, run test 109a first, iterate through 110-116
