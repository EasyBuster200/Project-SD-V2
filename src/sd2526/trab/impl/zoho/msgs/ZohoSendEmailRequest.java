package sd2526.trab.impl.zoho.msgs;

public record ZohoSendEmailRequest(
        String fromAddress,
        String toAddress,
        String subject,
        String content,
        String mailFormat) {

    public ZohoSendEmailRequest(String fromAddress, String toAddress, String subject, String content) {
        this(fromAddress, toAddress, subject, content, "plaintext");
    }
}