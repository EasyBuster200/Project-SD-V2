package sd2526.trab.impl.rest.servers;

import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.api.java.Messages;
import sd2526.trab.impl.replicated.ReplicatedMessages;
import sd2526.trab.impl.utils.ServerSecret;

/**
 * REST server hosting the replicated Messages service
 * 
 * Smillar to {@link RestMessagesServer} but registers the replicated resource
 * plus the X-MESSAGES-VERSIOn filter
 */
public class RestReplicatedMessagesServer extends AbstractRestServer {

  public static final int PORT = 4567;

  private static final Logger Log = Logger.getLogger(RestReplicatedMessagesServer.class.getName());

  RestReplicatedMessagesServer() {
    super(Log, Messages.SERVICE_NAME, PORT);
  }

  @Override
  void registerResources(ResourceConfig config) {
    // Registered as an INSTANCE, because with an instance Jersey constructs
    // eagerly, which triggers ReplicatedMessages.getInstance() and starts the Kafka
    // consumer
    config.register(new RestReplicatedMessagesResource());
    config.register(MessagesVersionFilter.class);
    config.register(AdminSecretFilter.class);
  }

  public static void main(String[] args) {
    ServerSecret.parse(args);

    // Start the singleton, before we bind HTTP port and announce on Discovery
    ReplicatedMessages.getInstance();

    new RestReplicatedMessagesServer().start();
  }
}