package sd2526.trab.impl.replicated;

import java.util.Set;

import sd2526.trab.api.Message;
import sd2526.trab.impl.utils.JSON;

public record Operation(String type, String payload) {

  public static final String POST = "POST";
  public static final String REMOVE_INBOX = "REMOVE_INBOX";
  public static final String DELETE = "DELETE";
  public static final String REMOTE_POST = "REMOTE_POST";
  public static final String REMOTE_DELETE = "REMOTE_DELETE";
  public static final String REMOTE_DELETE_INBOX = "REMOTE_DELETE_INBOX";

  public static record PostPayload(Message msg, Set<String> knownLocal, Set<String> unknownLocal) {
  }

  public static record RemoveInboxPayload(String name, String mid) {
  }

  public static record DeletePayload(String name, String mid) {
  }

  public static record RemotePostPayload(Message msg, Set<String> knownLocal, Set<String> unknownLocal) {
  }

  public static record RemoteDeletePayload(String mid) {
  }

  public static record RemoteDeleteInboxPayload(String name) {
  }

  public static Operation post(Message msg, Set<String> knownLocal, Set<String> unknownLocal) {
    return new Operation(POST, JSON.encode(new PostPayload(msg, knownLocal, unknownLocal)));
  }

  public static Operation removeInbox(String name, String mid) {
    return new Operation(REMOVE_INBOX, JSON.encode(new RemoveInboxPayload(name, mid)));
  }

  public static Operation delete(String name, String mid) {
    return new Operation(DELETE, JSON.encode(new DeletePayload(name, mid)));
  }

  public static Operation remotePost(Message msg, Set<String> knownLocal, Set<String> unknownLocal) {
    return new Operation(REMOTE_POST, JSON.encode(new RemotePostPayload(msg, knownLocal, unknownLocal)));
  }

  public static Operation remoteDelete(String mid) {
    return new Operation(REMOTE_DELETE, JSON.encode(new RemoteDeletePayload(mid)));
  }

  public static Operation remoteDeleteInbox(String name) {
    return new Operation(REMOTE_DELETE_INBOX, JSON.encode(new RemoteDeleteInboxPayload(name)));
  }
}