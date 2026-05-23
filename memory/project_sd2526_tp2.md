---
name: project-sd2526-tp2
description: SD2526 TP2 — three optional features extending teacher's base project. Security and E1 done, F1 Kafka next.
metadata:
  type: project
---

**SD2526 TP2** at FCT/NOVA — extends teacher's base project (`sd2526.trab1sol.v2`) with three optional features:

- **Security (4/4 pts)** — ✅ DONE. TLS via `JdkHttpServerFactory` + `SSLContext.getDefault()`, `ServerSecret` filter rejects `/admin` requests missing `X-Secret` header. Keystores in `tls/*.ks` (all password `changeit`).
- **E1 Zoho external mail proxy (6/6 pts)** — ✅ DONE. `RestZohoMessagesServer` + `ZohoMessages` business logic, ScribeJava OAuth2, Gson, `HtmlStripper`/`MessageBodyCodec`. Stores messages in real Zoho mailbox at `dcr.coelho@zohomail.eu`. Tests 108a/108b pass.
- **F1 Kafka replication (8/8 pts)** — 🚧 NOT STARTED. State-machine replication of messages domain. Tester runs 3 replicas per domain. See [[project-f1-kafka-plan]].

**Why:** student is doing all three optional features to maximize grade. Each is independent.

**How to apply:** F1 is the active focus. Deadline **2026-05-26** (3 days from today). Don't modify the working Security or E1 code unless explicitly asked — they're tested and committed.

**Currently passing tests:** 1a-1d, 101a, 102a/b/c, 103a/b/c/d, 105a/b/e, 106a, 107a, 108a, 108b.
**Legitimately skipped (gRPC not implemented):** 101b/c, 104a/b/c, 105c/d.
**Pseudo-failing (gRPC mainclass empty):** 106b.
**Not yet implemented:** 109a-116b (these are F1).

**Zoho credentials** are intentionally committed to private repo (grader needs them). See [[reference-sd2526-commands]] for build/test invocations.
