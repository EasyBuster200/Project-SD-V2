package sd2526.trab.impl.rest.servers;

import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.api.java.Messages;
import sd2526.trab.impl.replicated.ReplicatedMessages;
import sd2526.trab.impl.utils.ServerSecret;

public class RestReplicatedMessagesServer extends AbstractRestServer {

  public static final int PORT = 4567;

  private static final Logger Log = Logger.getLogger(RestReplicatedMessagesServer.class.getName());

  RestReplicatedMessagesServer() {
    super(Log, Messages.SERVICE_NAME, PORT);
  }

  @Override
  void registerResources(ResourceConfig config) {
    config.register(new RestReplicatedMessagesResource());
    config.register(MessagesVersionFilter.class);
    config.register(AdminSecretFilter.class);
  }

  public static void main(String[] args) {
    ServerSecret.parse(args);

    ReplicatedMessages.getInstance();

    new RestReplicatedMessagesServer().start();
  }
}