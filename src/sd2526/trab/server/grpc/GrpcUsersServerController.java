package sd2526.trab.server.grpc;

import java.util.List;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import sd2526.trab.api.User;
import sd2526.trab.api.grpc.GrpcUsersGrpc;
import sd2526.trab.api.grpc.Users.DeleteUserArgs;
import sd2526.trab.api.grpc.Users.DeleteUserResult;
import sd2526.trab.api.grpc.Users.GetUserArgs;
import sd2526.trab.api.grpc.Users.GetUserResult;
import sd2526.trab.api.grpc.Users.GrpcUser;
import sd2526.trab.api.grpc.Users.PostUserResult;
import sd2526.trab.api.grpc.Users.SearchUsersArgs;
import sd2526.trab.api.grpc.Users.UpdateUserArgs;
import sd2526.trab.api.grpc.Users.UserExistsArgs;
import sd2526.trab.api.grpc.Users.UpdateUserResult;
import sd2526.trab.api.grpc.Users.UserExistsResult;
import sd2526.trab.api.java.Result;
import sd2526.trab.server.grpc.util.DataModelAdaptor;
import sd2526.trab.server.utils.GrpcUtils;
import sd2526.trab.service.UsersService;

public class GrpcUsersServerController implements GrpcUsersGrpc.AsyncService, BindableService {

    UsersService impl = new UsersService();

    @Override
    public ServerServiceDefinition bindService() {
        return GrpcUsersGrpc.bindService(this);
    }

    public GrpcUsersServerController() {
    }

    @Override
    public void postUser(GrpcUser user, StreamObserver<PostUserResult> responseObserver) {
        Result<String> res = this.impl.postUser(DataModelAdaptor.fromGrpc(user));

        GrpcUtils.toGrpcResult(res, responseObserver,
                (userAddress) -> PostUserResult.newBuilder().setUserAddress(userAddress).build());
    }

    @Override
    public void getUser(GetUserArgs request, StreamObserver<GetUserResult> responseObserver) {
        Result<User> res = this.impl.getUser(request.getName(), request.getPwd());

        GrpcUtils.toGrpcResult(res, responseObserver,
                (user) -> GetUserResult.newBuilder().setUser(DataModelAdaptor.toGrpc(user)).build());
    }

    @Override
    public void updateUser(UpdateUserArgs request, StreamObserver<UpdateUserResult> responseObserver) {
        Result<User> res = this.impl.updateUser(request.getName(), request.getPwd(),
                DataModelAdaptor.fromGrpc(request.getInfo()));

        GrpcUtils.toGrpcResult(res, responseObserver,
                (user) -> UpdateUserResult.newBuilder().setUser(DataModelAdaptor.toGrpc(user)).build());

    }

    @Override
    public void deleteUser(DeleteUserArgs request, StreamObserver<DeleteUserResult> responseObserver) {
        Result<User> res = this.impl.deleteUser(request.getName(), request.getPwd());

        GrpcUtils.toGrpcResult(res, responseObserver,
                (user) -> DeleteUserResult.newBuilder().setUser(DataModelAdaptor.toGrpc(user)).build());
    }

    @Override
    public void searchUsers(SearchUsersArgs request, StreamObserver<GrpcUser> responseObserver) {
        Result<List<User>> res = this.impl.searchUsers(request.getName(), request.getPwd(), request.getQuery());

        if (!res.isOK()) {
            responseObserver.onError(GrpcUtils.errorCodeToStatus(res.error()));
            return;
        }

        for (User u : res.value()) {
            responseObserver.onNext(DataModelAdaptor.toGrpc(u));
        }
        responseObserver.onCompleted();
    }

    @Override
    public void userExists(UserExistsArgs request, StreamObserver<UserExistsResult> responseObserver) {
        Result<Boolean> res = this.impl.userExists(request.getName());
        GrpcUtils.toGrpcResult(res, responseObserver,
                (exists) -> UserExistsResult.newBuilder().setExists(exists).build());
    }
}
