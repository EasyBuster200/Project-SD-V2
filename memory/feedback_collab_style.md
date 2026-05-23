---
name: feedback-collab-style
description: How this user collaborates — detailed handoffs, expects requests for source rather than guesses
metadata:
  type: feedback
---

**This user provides detailed handoff messages and expects me to follow their stated next step exactly.**

**Why:** the user paid for a previous Claude session to design the architecture and write a comprehensive handoff. Re-deriving or guessing wastes their time and risks divergence from the agreed plan. When the handoff says "ask me for X before starting", that means ask — don't grep the codebase to reconstruct it.

**How to apply:**
- When the user sends a long structured handoff, read it fully and follow the closing instruction literally (e.g. "ask me to share the lab 11 source files" means request those files, don't try to find them yourself)
- Build separate parallel implementations rather than mutating existing working code when the user has flagged something as "DONE / committed" — they value reversibility
- Confirm before destructive cleanup actions; the user did the cleanup themselves between sessions and may have partially done it
- When something in the working tree contradicts the handoff (e.g. a file deleted that should be preserved), flag it explicitly rather than silently going along
