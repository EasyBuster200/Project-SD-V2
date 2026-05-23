package sd2526.trab.impl.zoho;

import java.util.Set;

import sd2526.trab.api.Message;

/**
 * Round-trip smoke test for the encode → send → list → fetch → strip → decode
 * pipeline against a real Zoho mailbox.
 *
 * Sends a synthetic Message, fetches it back via the inbox, reconstructs
 * the Message object, and verifies every field round-tripped.
 */
public class ZohoRoundTripTest {

  private static final long INDEX_DELAY_MS = 4000;

  public static void main(String[] args) throws Exception {
    var zoho = Zoho.getInstance();

    // Build a Message with values we'll check field-by-field after the round trip.
    Message original = new Message(
        "ourorg2+9999",
        "Alice <alice@ourorg2>",
        Set.of("bob@ourorg0", "carol@ourorg1"),
        "Round-trip test " + System.currentTimeMillis(),
        "Line one of the message.\nLine two with <html> & special \"chars\".\nLine three.");

    System.out.println("=== Sending message with id=" + original.getId() + " ===");
    String body = MessageBodyCodec.encode(original);
    boolean sent = zoho.sendEmail(Zoho.FROM_ADDRESS, original.getSubject(), body);
    if (!sent) {
      System.err.println("FAILED at send");
      return;
    }

    System.out.println("Waiting " + (INDEX_DELAY_MS / 1000) + "s for indexing...");
    Thread.sleep(INDEX_DELAY_MS);

    System.out.println();
    System.out.println("=== Locating message in inbox ===");
    var inbox = zoho.listInbox();
    var match = inbox.stream()
        .filter(m -> original.getSubject().equals(m.subject()))
        .findFirst()
        .orElse(null);

    if (match == null) {
      System.err.println("FAILED: subject not found in inbox");
      return;
    }
    System.out.println("Found Zoho messageId=" + match.messageId());

    System.out.println();
    System.out.println("=== Fetching + stripping content ===");
    String html = zoho.getEmailContent(match.messageId());
    if (html == null) {
      System.err.println("FAILED to fetch content");
      return;
    }
    String plain = HtmlStripper.strip(html);
    System.out.println("Stripped body:");
    System.out.println(plain);

    System.out.println();
    System.out.println("=== Decoding back into Message ===");
    Message decoded = MessageBodyCodec.decode(plain, match.subject());
    if (decoded == null) {
      System.err.println("FAILED to decode body");
      return;
    }

    System.out.println("Decoded: id=" + decoded.getId());
    System.out.println("         sender=" + decoded.getSender());
    System.out.println("         destinations=" + decoded.getDestination());
    System.out.println("         subject=" + decoded.getSubject());
    System.out.println("         creationTime=" + decoded.getCreationTime());
    System.out.println("         contents=[" + decoded.getContents() + "]");

    System.out.println();
    boolean ok = true;
    ok &= check("id", original.getId(), decoded.getId());
    ok &= check("sender", original.getSender(), decoded.getSender());
    ok &= check("subject", original.getSubject(), decoded.getSubject());
    ok &= check("destinations", original.getDestination(), decoded.getDestination());
    ok &= check("creationTime", original.getCreationTime(), decoded.getCreationTime());
    ok &= check("contents", original.getContents(), decoded.getContents());

    System.out.println();
    System.out.println("=== Cleanup ===");
    zoho.deleteEmail(match.messageId());
    System.out.println("Deleted.");

    System.out.println();
    System.out.println(ok ? "=== ALL OK ===" : "=== SOME FIELDS DIFFER ===");
  }

  private static boolean check(String field, Object expected, Object actual) {
    boolean ok = (expected == null && actual == null) || (expected != null && expected.equals(actual));
    System.out.printf("  %-15s %s%n", field,
        ok ? "OK" : ("MISMATCH | expected=[" + expected + "] actual=[" + actual + "]"));
    return ok;
  }
}