package sd2526.trab.impl.rest.servers;

import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.impl.utils.ServerSecret;

public class RestGatewayServer extends AbstractRestServer {

	public static final int PORT = 6666;

	private static Logger Log = Logger.getLogger(RestGatewayServer.class.getName());

	RestGatewayServer() {
		super(Log, null, PORT);
	}

	@Override
	void registerResources(ResourceConfig config) {
		config.registerInstances(new RestUsersResource(true), new RestMessagesResource(true));
//		config.register(.getClass());
//		config.register(.getClass());
		config.register(AdminSecretFilter.class);
	}

	public static void main(String[] args) {
		ServerSecret.parse(args);
		new RestGatewayServer().start();
	}
}