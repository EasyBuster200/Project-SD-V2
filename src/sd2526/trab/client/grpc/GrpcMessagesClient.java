package sd2526.trab.client.grpc;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.grpc.Messages.PostMessageArgs;
import sd2526.trab.api.grpc.Messages.DeleteMessageArgs;
import sd2526.trab.api.grpc.Messages.DeleteInternalArgs;
import sd2526.trab.api.grpc.Messages.GetInboxMessageArgs;
import sd2526.trab.api.grpc.Messages.GetAllInboxMessagesArgs;
import sd2526.trab.api.grpc.Messages.RemoveInboxMessageArgs;
import sd2526.trab.api.grpc.Messages.SearchInboxArgs;
import sd2526.trab.api.Message;
import sd2526.trab.server.grpc.util.DataModelAdaptor;
import sd2526.trab.api.grpc.GrpcMessagesGrpc;

public class GrpcMessagesClient extends GrpcClient implements Messages {
    private static Logger logger = Logger.getLogger(GrpcMessagesClient.class.getName());
    final GrpcMessagesGrpc.GrpcMessagesBlockingStub stub;

    public GrpcMessagesClient(URI serverURI) {
        super(serverURI, logger);
        stub = GrpcMessagesGrpc.newBlockingStub(channel);
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        return super.processResponse(() -> stub.postMessage(PostMessageArgs.newBuilder()
                .setPwd(pwd)
                .setMessage(DataModelAdaptor.toGrpc(msg))
                .build()).getMid());
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        return super.processResponse(
                () -> DataModelAdaptor.fromGrpc(stub.getInboxMessage(GetInboxMessageArgs.newBuilder()
                        .setName(name).setMid(mid).setPwd(pwd)
                        .build())));
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd) {
        return super.processResponse(() -> stub.getAllInboxMessages(GetAllInboxMessagesArgs.newBuilder()
                .setName(name).setPwd(pwd)
                .build()).getMidsList());
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
        return super.processResponse(() -> {
            stub.removeInboxMessage(RemoveInboxMessageArgs.newBuilder()
                    .setName(name).setMid(mid).setPwd(pwd)
                    .build());
            return null;
        });
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        return super.processResponse(() -> {
            stub.deleteMessage(DeleteMessageArgs.newBuilder()
                    .setName(name).setMid(mid).setPwd(pwd)
                    .build());
            return null;
        });
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query) {
        return super.processResponse(() -> stub.searchInbox(SearchInboxArgs.newBuilder()
                .setName(name).setPwd(pwd).setQuery(query)
                .build()).getMidsList());
    }

    @Override
    public Result<Void> deliverInternal(Message msg) {
        return super.processResponse(() -> {
            stub.deliverInternal(DataModelAdaptor.toGrpc(msg));
            return null;
        });
    }

    @Override
    public Result<Void> deleteInternal(String mid) {
        return super.processResponse(() -> {
            stub.deleteInternal(DeleteInternalArgs.newBuilder().setMid(mid).build());
            return null;
        });
    }
}