package sd2526.trab.server.grpc;

import java.util.List;

import com.google.protobuf.Empty;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;

import sd2526.trab.api.Message;
import sd2526.trab.api.grpc.GrpcMessagesGrpc;
import sd2526.trab.api.grpc.Messages.PostMessageArgs;
import sd2526.trab.api.grpc.Messages.PostMessageResult;
import sd2526.trab.api.grpc.Messages.GetInboxMessageArgs;
import sd2526.trab.api.grpc.Messages.GetAllInboxMessagesArgs;
import sd2526.trab.api.grpc.Messages.DeleteMessageArgs;
import sd2526.trab.api.grpc.Messages.SearchInboxArgs;
import sd2526.trab.api.grpc.Messages.DeleteInternalArgs;
import sd2526.trab.api.grpc.Messages.SearchInboxResult;
import sd2526.trab.api.grpc.Messages.GrpcMessage;
import sd2526.trab.api.grpc.Messages.GetAllInboxMessagesResult;
import sd2526.trab.api.grpc.Messages.RemoveInboxMessageArgs;
import sd2526.trab.api.java.Result;
import sd2526.trab.server.grpc.util.DataModelAdaptor;
import sd2526.trab.server.utils.GrpcUtils;
import sd2526.trab.service.MessagesService;

public class GrpcMessagesServerController implements GrpcMessagesGrpc.AsyncService, BindableService {
    MessagesService impl;

    public GrpcMessagesServerController(String domain) {
        this.impl = new MessagesService(domain);
    }

    @Override
    public ServerServiceDefinition bindService() {
        return GrpcMessagesGrpc.bindService(this);
    }

    @Override
    public void postMessage(PostMessageArgs request, StreamObserver<PostMessageResult> responseObserver) {
        Result<String> res = impl.postMessage(request.getPwd(), DataModelAdaptor.fromGrpc(request.getMessage()));
        GrpcUtils.toGrpcResult(res, responseObserver,
                (mid) -> PostMessageResult.newBuilder().setMid(mid).build());
    }

    @Override
    public void getInboxMessage(GetInboxMessageArgs request, StreamObserver<GrpcMessage> responseObserver) {
        Result<Message> res = impl.getInboxMessage(request.getName(), request.getMid(), request.getPwd());
        GrpcUtils.toGrpcResult(res, responseObserver,
                (msg) -> DataModelAdaptor.toGrpc(msg));
    }

    @Override
    public void getAllInboxMessages(GetAllInboxMessagesArgs request,
            StreamObserver<GetAllInboxMessagesResult> responseObserver) {
        Result<List<String>> res = impl.getAllInboxMessages(request.getName(), request.getPwd());
        GrpcUtils.toGrpcResult(res, responseObserver,
                (mids) -> GetAllInboxMessagesResult.newBuilder().addAllMids(mids).build());
    }

    @Override
    public void removeInboxMessage(RemoveInboxMessageArgs request, StreamObserver<Empty> responseObserver) {
        Result<Void> res = impl.removeInboxMessage(request.getName(), request.getMid(), request.getPwd());
        GrpcUtils.toGrpcResult(res, responseObserver, (v) -> Empty.getDefaultInstance());
    }

    @Override
    public void deleteMessage(DeleteMessageArgs request, StreamObserver<Empty> responseObserver) {
        Result<Void> res = impl.deleteMessage(request.getName(), request.getMid(), request.getPwd());
        GrpcUtils.toGrpcResult(res, responseObserver, (v) -> Empty.getDefaultInstance());
    }

    @Override
    public void searchInbox(SearchInboxArgs request, StreamObserver<SearchInboxResult> responseObserver) {
        Result<List<String>> res = impl.searchInbox(request.getName(), request.getPwd(), request.getQuery());
        GrpcUtils.toGrpcResult(res, responseObserver,
                (mids) -> SearchInboxResult.newBuilder().addAllMids(mids).build());
    }

    @Override
    public void deliverInternal(GrpcMessage request, StreamObserver<Empty> responseObserver) {
        Result<Void> res = impl.deliverInternal(DataModelAdaptor.fromGrpc(request));
        GrpcUtils.toGrpcResult(res, responseObserver, (v) -> Empty.getDefaultInstance());
    }

    @Override
    public void deleteInternal(DeleteInternalArgs request, StreamObserver<Empty> responseObserver) {
        Result<Void> res = impl.deleteInternal(request.getMid());
        GrpcUtils.toGrpcResult(res, responseObserver, (v) -> Empty.getDefaultInstance());
    }
}