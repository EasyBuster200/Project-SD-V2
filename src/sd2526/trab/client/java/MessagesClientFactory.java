package sd2526.trab.client.java;

import java.net.URI;

import sd2526.trab.api.java.Messages;
import sd2526.trab.client.grpc.GrpcMessagesClient;
import sd2526.trab.client.rest.RestMessagesClient;
import sd2526.trab.discovery.Discovery;

public class MessagesClientFactory {

	private static final String REST = "/rest";
	private static final String GRPC = "/grpc";
	private static final Object DOMAIN_DELIMITER = "@";

	static public Messages get(String domain) {
		var sn = "%s%s%s".formatted(Messages.SERVICE_NAME, DOMAIN_DELIMITER, domain);
		return newClient(Discovery.getInstance().knownUrisOf(sn, 1)[0]);
	}

	static private Messages newClient(URI serverURI) {
		var path = serverURI.getPath();
		if (path.endsWith(REST))
			return new RestMessagesClient(serverURI);
		if (path.endsWith(GRPC))
			return new GrpcMessagesClient(serverURI);

		System.err.println("Exception at MessagesClientFactory: Unknown service type" + serverURI);
		throw new RuntimeException("Unknown service type..." + serverURI);
	}
}
