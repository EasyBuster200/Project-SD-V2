---
name: feedback-sd2526-style
description: Code style conventions to follow in SD2526 TP2 codebase
metadata:
  type: feedback
---

**Match the teacher's pragmatic style — don't over-engineer.**

**Why:** the grader is the teacher's tester script + manual code review; teacher values pragmatic, idiomatic Java that fits the existing template. User wants to mirror existing patterns from `JavaMessages`, `RestMessagesResource`, etc.

**How to apply:** when writing new code in this project:
- Use `Result<T>` wrappers with `.then()` / `.thenWith()` chaining (NOT exceptions for business errors)
- Java records for data classes (Zoho replies, Operation envelopes, etc.)
- Gson for JSON (already in deps)
- Hibernate via `DB.transaction(hibernate -> ...)` pattern — don't bypass DB.java
- `Result.error(ErrorCode.X)` for errors, `Result.ok(value)` for success
- Logger: `private static Logger Log = Logger.getLogger(ClassName.class.getName())` and `Log.info(() -> "...")` (Supplier form, never string concat)
- Match imports and field-ordering style of `JavaMessages` and `RestMessagesResource` when adding new server/resource pairs
- Don't add fallbacks/validation for impossible cases — trust internal code
- Don't add doc comments to obvious code
