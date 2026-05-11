package sd2526.trab.client.java;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import sd2526.trab.discovery.Discovery;

public class ClientFactory<T> {

	private static final String REST = "/rest";
	private static final String GRPC = "/grpc";
	private static final Object DOMAIN_DELIMITER = "@";

	private final String serviceName;
	private final Function<URI, T> restClientFunc;
	private final Function<URI, T> grpcClientFunc;

	private final ConcurrentHashMap<URI, T> clientCache = new ConcurrentHashMap<>();

	ClientFactory(String serviceName, Function<URI, T> restClientFunc, Function<URI, T> grpcClientFunc) {
		this.restClientFunc = restClientFunc;
		this.grpcClientFunc = grpcClientFunc;
		this.serviceName = serviceName;
	}

	public T get(String domain) {
		var sn = "%s%s%s".formatted(serviceName, DOMAIN_DELIMITER, domain);
		URI uri = Discovery.getInstance().knownUrisOf(sn, 1)[0];
		return clientCache.computeIfAbsent(uri, this::newClient);
	}

	private T newClient(URI serverURI) {
		var path = serverURI.getPath();
		if (path.endsWith(REST))
			return restClientFunc.apply(serverURI);
		if (path.endsWith(GRPC))
			return grpcClientFunc.apply(serverURI);

		System.err.println("Exception at ClientFactory: Unknown service type" + serverURI);
		throw new RuntimeException("Unknown service type..." + serverURI);
	}

}
