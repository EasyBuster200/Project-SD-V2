package sd2526.trab.client.java;

import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Users;
import sd2526.trab.client.grpc.GrpcMessagesClient;
import sd2526.trab.client.grpc.GrpcUsersClient;
import sd2526.trab.client.rest.RestMessagesClient;
import sd2526.trab.client.rest.RestUsersClient;

public class Clients {

	public static final ClientFactory<Users> UsersClient = new ClientFactory<Users>(Users.SERVICE_NAME,
			RestUsersClient::new, GrpcUsersClient::new);

	public static final ClientFactory<Messages> MessagesClient = new ClientFactory<Messages>(Messages.SERVICE_NAME,
			RestMessagesClient::new, GrpcMessagesClient::new);

}
