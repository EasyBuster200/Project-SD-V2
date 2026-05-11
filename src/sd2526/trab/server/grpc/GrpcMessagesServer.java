package sd2526.trab.server.grpc;

import java.net.InetAddress;
import java.util.logging.Logger;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerCredentials;

import sd2526.trab.api.java.Messages;
import sd2526.trab.discovery.Discovery;

public class GrpcMessagesServer {
    public static final int PORT = 9001;
    private static final String GRPC_CTX = "/grpc";
    private static final String SERVER_BASE_URI = "grpc://%s:%s%s";
    private static Logger Log = Logger.getLogger(GrpcMessagesServer.class.getName());

    public static void main(String[] args) throws Exception {
        String hostname = InetAddress.getLocalHost().getHostName();
        int dot = hostname.indexOf('.');
        String domain = (dot >= 0 && dot < hostname.length() - 1)
                ? hostname.substring(dot + 1)
                : hostname;

        GrpcMessagesServerController stub = new GrpcMessagesServerController(domain);
        ServerCredentials cred = InsecureServerCredentials.create();
        Server server = Grpc.newServerBuilderForPort(PORT, cred).addService(stub).build();
        String serverURI = String.format(SERVER_BASE_URI, InetAddress.getLocalHost().getHostAddress(), PORT, GRPC_CTX);

        Discovery.announce(Messages.SERVICE_NAME, domain, serverURI);
        Log.info(String.format("Messages gRPC Server ready @ %s\n", serverURI));
        server.start().awaitTermination();
    }
}