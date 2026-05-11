package sd2526.trab.server.rest;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.discovery.Discovery;

public class RestGatewayServer {
    private static Logger Log = Logger.getLogger(RestGatewayServer.class.getName());
    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }
    public static final int PORT = 8085;
    private static final String SERVER_URI_FMT = "http://%s:%s/rest";

    public static void main(String[] args) {
        try {
            ResourceConfig config = new ResourceConfig();

            String ip = InetAddress.getLocalHost().getHostAddress();
            String serverURI = String.format(SERVER_URI_FMT, ip, PORT);
            String hostname = InetAddress.getLocalHost().getHostName();
            int dot = hostname.indexOf('.');
            String domain = (dot >= 0 && dot < hostname.length() - 1)
                    ? hostname.substring(dot + 1)
                    : hostname;

            GatewayMessagesResource.init(domain);
            GatewayUsersResource.init(domain);

            config.register(GatewayMessagesResource.class);
            config.register(GatewayUsersResource.class);
            JdkHttpServerFactory.createHttpServer(URI.create(serverURI), config);

            Discovery.announce("Gateway", domain, serverURI);
            Log.info(String.format("%s Server ready @ %s\n", "Gateway", serverURI));
        } catch (Exception e) {
            Log.severe(e.getMessage());
        }
    }
}
