package sd2526.trab.impl.replicated;

import java.util.Set;

import sd2526.trab.api.Message;
import sd2526.trab.impl.utils.JSON;

public record Operation(String type, String payload) {

  // Type discriminators for all the possible operations
  public static final String POST = "POST";
  public static final String REMOVE_INBOX = "REMOVE_INBOX";
  public static final String DELETE = "DELETE";
  public static final String REMOTE_POST = "REMOTE_POST";
  public static final String REMOTE_DELETE = "REMOTE_DELETE";
  public static final String REMOTE_DELETE_INBOX = "REMOTE_DELETE_INBOX";

  /**
   * @param msg          message to be delivered
   * @param knownLocal   set of local addresses that exist in the domain's Users
   *                     service (created by the REST thread)
   * @param unknownLocal set of local addresses that don't exist, used to generate
   *                     the UserNotFound notifications
   */
  public static record PostPayload(Message msg, Set<String> knownLocal, Set<String> unknownLocal) {
  }

  /**
   * Removes a single message ID from a user's inbox (doesn't delete the message
   * itself)
   * 
   * @param name username of inbox owner
   * @param mid  ID of the message to be removed from the inbox (doesn't delete
   *             the message itself)
   */
  public static record RemoveInboxPayload(String name, String mid) {
  }

  /**
   * Deletes a message sent by {@code name} with message ID {@code mid} across all
   * destinations
   * 
   * @param name username of the sender
   * @param mid  ID of the message to delete
   */
  public static record DeletePayload(String name, String mid) {
  }

  // Notifications from cross-domain actions

  /**
   * Receives a message from another domain to be delivered to users in the local
   * domain.
   * 
   * @param msg          message being recieved
   * @param knownLocal   set of local addresses that exist in the domain's Users
   *                     service (created by the REST thread)
   * @param unknownLocal set of local addresses that don't exist, used to generate
   *                     the UserNotFound notifications
   */
  public static record RemotePostPayload(Message msg, Set<String> knownLocal, Set<String> unknownLocal) {
  }

  /**
   * Receives the ID of a message that has been globally deleted by the sender,
   * and removes it from any local inbox that may have recieved the message
   * 
   * @param mid ID of the message to delete
   */
  public static record RemoteDeletePayload(String mid) {
  }

  /**
   * Receives the username of a user whose inbox has to be cleared, and clears the
   * inbox locally.
   * 
   * @param name username of inbox owner
   */
  public static record RemoteDeleteInboxPayload(String name) {
  }

  // Methods used by REST to build an Operation to be published to Kafka

  /** Builds a local-post operation. See {@link PostPayload}. */
  public static Operation post(Message msg, Set<String> knownLocal, Set<String> unknownLocal) {
    return new Operation(POST, JSON.encode(new PostPayload(msg, knownLocal, unknownLocal)));
  }

  /**
   * Builds a local-remove-from-inbox operation. See {@link RemoveInboxPayload}
   */
  public static Operation removeInbox(String name, String mid) {
    return new Operation(REMOVE_INBOX, JSON.encode(new RemoveInboxPayload(name, mid)));
  }

  /** Builds a local-delete operation. See {@link DeletePayload} */
  public static Operation delete(String name, String mid) {
    return new Operation(DELETE, JSON.encode(new DeletePayload(name, mid)));
  }

  /** Builds a remote-post operation. See {@link RemotePostPayload} */
  public static Operation remotePost(Message msg, Set<String> knownLocal, Set<String> unknownLocal) {
    return new Operation(REMOTE_POST, JSON.encode(new RemotePostPayload(msg, knownLocal, unknownLocal)));
  }

  /** Builds a remote-delete operation. See {@link RemoteDeletePayload} */
  public static Operation remoteDelete(String mid) {
    return new Operation(REMOTE_DELETE, JSON.encode(new RemoteDeletePayload(mid)));
  }

  /**
   * Builds a remote-delete-inbox operation. See {@link RemoteDeleteInboxPayload}
   */
  public static Operation remoteDeleteInbox(String name) {
    return new Operation(REMOTE_DELETE_INBOX, JSON.encode(new RemoteDeleteInboxPayload(name)));
  }
}