package sd2526.trab.impl.rest.servers;

import java.util.List;

import jakarta.inject.Singleton;
import sd2526.trab.api.Message;
import sd2526.trab.api.rest.RestMessages;
import sd2526.trab.impl.api.rest.RestAdminMessages;
import sd2526.trab.impl.replicated.ReplicatedMessages;
import sd2526.trab.impl.utils.SyncPoint;

/**
 * REST resource for the replicated Messages service
 */
@Singleton
public class RestReplicatedMessagesResource extends RestResource
    implements RestMessages, RestAdminMessages {

  private final ReplicatedMessages impl = ReplicatedMessages.getInstance();
  private final SyncPoint sync = SyncPoint.getSyncPoint();

  // Write Methods

  @Override
  public String postMessage(String pwd, Message msg) {
    return super.resultOrThrow(impl.postMessage(pwd, msg));
  }

  @Override
  public void removeFromUserInbox(String name, String mid, String pwd) {
    super.resultOrThrow(impl.removeInboxMessage(name, mid, pwd));
  }

  @Override
  public void deleteMessage(String name, String mid, String pwd) {
    super.resultOrThrow(impl.deleteMessage(name, mid, pwd));
  }

  // Read Methods

  @Override
  public Message getMessage(String name, String mid, String pwd) {
    sync.waitForVersion(MessagesVersionFilter.currentRequestVersion());
    return super.resultOrThrow(impl.getInboxMessage(name, mid, pwd));
  }

  @Override
  public List<String> getMessages(String name, String pwd, String query) {
    sync.waitForVersion(MessagesVersionFilter.currentRequestVersion());
    if (query != null && !query.isEmpty())
      return super.resultOrThrow(impl.searchInbox(name, pwd, query));
    else
      return super.resultOrThrow(impl.getAllInboxMessages(name, pwd));
  }

  // Cross Domain

  @Override
  public void remotePostMessage(Message m) {
    super.resultOrThrow(impl.remotePostMessage(m));
  }

  @Override
  public void remoteDeleteMessage(String mid) {
    super.resultOrThrow(impl.remoteDeleteMessage(mid));
  }

  @Override
  public void remoteDeleteUserInbox(String name) {
    super.resultOrThrow(impl.remoteDeleteUserInbox(name));
  }
}