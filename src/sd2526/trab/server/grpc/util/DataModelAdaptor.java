package sd2526.trab.server.grpc.util;

import sd2526.trab.api.User;
import sd2526.trab.api.grpc.Users.GrpcUser;

import java.util.HashSet;

import sd2526.trab.api.Message;
import sd2526.trab.api.grpc.Messages.GrpcMessage;

public class DataModelAdaptor {

	public static User fromGrpc(GrpcUser from) {
		return new User(
				from.getName().equals("") ? null : from.getName(),
				from.hasPwd() ? from.getPwd() : null,
				from.hasDisplayName() ? from.getDisplayName() : null,
				from.hasDomain() ? from.getDomain() : null);
	}

	public static GrpcUser toGrpc(User from) {
		GrpcUser.Builder b = GrpcUser.newBuilder();

		if (from.getName() != null)
			b.setName(from.getName());

		if (from.getPwd() != null)
			b.setPwd(from.getPwd());

		if (from.getDisplayName() != null)
			b.setDisplayName(from.getDisplayName());

		if (from.getDomain() != null)
			b.setDomain(from.getDomain());

		return b.build();
	}

	public static Message fromGrpc(GrpcMessage from) {
		Message msg = new Message();
		msg.setId(from.getId());
		msg.setSender(from.getSender());
		msg.setDestination(new HashSet<>(from.getDestinationList()));
		msg.setCreationTime(from.getCreationTime());
		msg.setSubject(from.getSubject());
		msg.setContents(from.getContents());
		return msg;
	}

	public static GrpcMessage toGrpc(Message from) {
		return GrpcMessage.newBuilder()
				.setId(from.getId() != null ? from.getId() : "")
				.setSender(from.getSender() != null ? from.getSender() : "")
				.addAllDestination(from.getDestination())
				.setCreationTime(from.getCreationTime())
				.setSubject(from.getSubject() != null ? from.getSubject() : "")
				.setContents(from.getContents() != null ? from.getContents() : "")
				.build();
	}

}
