package sd2526.trab.impl.zoho.msgs;

public record ZohoMessageSummary(
        String messageId,
        String subject,
        String summary,
        String fromAddress,
        String toAddress,
        String folderId,
        String sentDateInGMT,
        String receivedTime) {
}