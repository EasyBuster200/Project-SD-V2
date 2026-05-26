package sd2526.trab.impl.rest.servers;

import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.api.java.Messages;
import sd2526.trab.impl.utils.ServerSecret;
import sd2526.trab.impl.zoho.ZohoMessages;

/**
 * REST server hosting the Zoho-backed Messages service.
 */
public class RestZohoMessagesServer extends AbstractRestServer {

  public static final int PORT = 4567;

  private static final Logger Log = Logger.getLogger(RestZohoMessagesServer.class.getName());

  RestZohoMessagesServer() {
    super(Log, Messages.SERVICE_NAME, PORT);
  }

  @Override
  void registerResources(ResourceConfig config) {
    config.register(RestZohoMessagesResource.class);
    config.register(AdminSecretFilter.class);
  }

  public static void main(String[] args) {
    ServerSecret.parse(args);

    // First argument controls whether to start from a clean mailbox, or not.
    boolean wipe = args.length > 0 && "true".equalsIgnoreCase(args[0]);
    if (wipe) {
      Log.info("Startup arg = true -> wiping Zoho mailbox for clean state.");
      ZohoMessages.getInstance().wipeMailbox();
    } else {
      Log.info("Startup arg = false (or unset) -> preserving Zoho mailbox state.");
    }

    new RestZohoMessagesServer().start();
  }
}