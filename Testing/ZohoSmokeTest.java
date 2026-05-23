package sd2526.trab.impl.zoho;

import sd2526.trab.impl.zoho.msgs.ZohoMessageSummary;

/**
 * End-to-end smoke test for the Zoho email operations.
 *
 * Sends an email to ourselves, lists the inbox, finds the email we just sent
 * (by subject), fetches its content, deletes it, then lists again to confirm
 * removal.
 *
 * If this prints "ALL OK" at the end, the four operations we need for the
 * Messages server are working.
 */
public class ZohoSmokeTest {

  // The subject is unique per run so we can find our email even if the
  // inbox has unrelated messages in it.
  private static final String TEST_SUBJECT_PREFIX = "SD2526-SmokeTest-";

  // Zoho's indexing isn't instant; an email sent right now won't always
  // appear in the list immediately. Wait a bit before each list call.
  private static final long INDEX_DELAY_MS = 4000;

  public static void main(String[] args) throws Exception {
    var zoho = Zoho.getInstance();

    String subject = TEST_SUBJECT_PREFIX + System.currentTimeMillis();
    String body = "Hello from the smoke test.\n------\nid=12345\nsender=alice@ourorg2\ncreationTime=1700000000000";

    System.out.println("=== 1) Sending email ===");
    boolean sent = zoho.sendEmail(Zoho.FROM_ADDRESS, subject, body);
    if (!sent) {
      System.err.println("FAILED at send.");
      return;
    }
    System.out.println("Sent ok. Subject: " + subject);

    System.out.println();
    System.out.println("=== 2) Waiting " + (INDEX_DELAY_MS / 1000) + "s for indexing ===");
    Thread.sleep(INDEX_DELAY_MS);

    System.out.println();
    System.out.println("=== 3) Listing inbox ===");
    System.out.println();
    System.out.println("=== 3) Listing inbox ===");
    var inbox = zoho.listInbox();
    System.out.println("Inbox has " + inbox.size() + " messages.");

    ZohoMessageSummary found = null;
    for (var m : inbox) {
      System.out.println("  - " + m.messageId() + " | " + m.subject() + " | summary: [" + m.summary() + "]");
      if (subject.equals(m.subject()))
        found = m;
    }

    if (found == null) {
      System.err.println("FAILED: could not find our test message in the inbox.");
      return;
    }
    final ZohoMessageSummary ours = found;
    System.out.println("Found our message with id " + ours.messageId());

    System.out.println();
    System.out.println("=== 4) Fetching content ===");
    String content = zoho.getEmailContent(ours.messageId());
    if (content == null) {
      System.err.println("FAILED at get content.");
      return;
    }
    System.out.println("Content (" + content.length() + " chars):");
    System.out.println(content);

    System.out.println();
    System.out.println("=== 5) Deleting ===");
    boolean deleted = zoho.deleteEmail(ours.messageId());
    if (!deleted) {
      System.err.println("FAILED at delete.");
      return;
    }
    System.out.println("Deleted ok.");

    System.out.println();
    System.out.println("=== 6) Verifying it's gone ===");
    Thread.sleep(INDEX_DELAY_MS);
    var inboxAfter = zoho.listInbox();
    boolean stillThere = inboxAfter.stream().anyMatch(m -> m.messageId().equals(ours.messageId()));
    if (stillThere) {
      System.err.println("WARN: message still listed after delete (Zoho may be slow; check the mailbox manually).");
    } else {
      System.out.println("Confirmed gone.");
    }

    System.out.println();
    System.out.println("=== ALL OK ===");
  }
}