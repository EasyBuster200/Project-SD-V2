---
name: reference-sd2526-commands
description: Build, run, and test commands for SD2526 TP2 project on Windows + Git Bash
metadata:
  type: reference
---

**Build + dockerize:**
```
mvn clean compile package && docker build --no-cache -t sd2526-tp2-65154 .
```

**Run full test suite:**
```
MSYS_NO_PATHCONV=1 sh test-sd-tp2.sh -image sd2526-tp2-65154 > test.txt
```

**Run a single test (e.g. 109a):**
```
MSYS_NO_PATHCONV=1 sh test-sd-tp2.sh -image sd2526-tp2-65154 -test 109a > test_f1.txt
```

**Gotchas:**
- `MSYS_NO_PATHCONV=1` prefix is REQUIRED in Git Bash to prevent path mangling
- Image name MUST match the build tag in tester command
- ALWAYS rebuild with `--no-cache` after editing `messages.props` (Docker layer cache otherwise serves stale props)
- `pom.xml` artifactId is `sd2526-tp1-ref` (legacy from TP1); the `docker build -t sd2526-tp2-65154 .` form is what user uses, NOT `mvn ... docker:build` (that produces `sd2526-tp1-ref-65154`)
- VS Code Java cache sometimes shows phantom errors — "Java: Clean Java Language Server Workspace" fixes
- Discovery uses multicast 226.226.226.226:2266 — don't change

**Kafka (F1) extras:**
- `start-kafka.sh` script provided by teacher to run Kafka+Zookeeper in docker for local testing
- Tester provides a single Kafka broker in its env, address typically `"localhost:9092, kafka:9092"`
