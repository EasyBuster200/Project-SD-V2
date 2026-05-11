package sd2526.trab.client.grpc;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import sd2526.trab.api.User;
import sd2526.trab.api.grpc.GrpcUsersGrpc;
import sd2526.trab.api.grpc.Users.DeleteUserArgs;
import sd2526.trab.api.grpc.Users.GetUserArgs;
import sd2526.trab.api.grpc.Users.GetUserResult;
import sd2526.trab.api.grpc.Users.SearchUsersArgs;
import sd2526.trab.api.grpc.Users.UpdateUserArgs;
import sd2526.trab.api.grpc.Users.UserExistsArgs;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Users;
import sd2526.trab.server.grpc.util.DataModelAdaptor;

public class GrpcUsersClient extends GrpcClient implements Users {

    private static Logger logger = Logger.getLogger(GrpcUsersClient.class.getName());

    final GrpcUsersGrpc.GrpcUsersBlockingStub stub;

    public GrpcUsersClient(URI serverURI) {
        super(serverURI, logger);
        stub = GrpcUsersGrpc.newBlockingStub(channel);
    }

    @Override
    public Result<String> postUser(User user) {
        return super.processResponse(() -> stub.postUser(DataModelAdaptor.toGrpc(user)).getUserAddress());
    }

    @Override
    public Result<User> getUser(String name, String pwd) {
        GetUserResult res = stub.getUser(GetUserArgs.newBuilder()
                .setName(name).setPwd(pwd)
                .build());

        return super.processResponse(() -> DataModelAdaptor.fromGrpc(res.getUser()));
    }

    @Override
    public Result<User> updateUser(String name, String pwd, User info) {
        return super.processResponse(() -> DataModelAdaptor.fromGrpc(
                stub.updateUser(UpdateUserArgs.newBuilder()
                        .setName(name).setPwd(pwd)
                        .setInfo(DataModelAdaptor.toGrpc(info))
                        .build()).getUser()));
    }

    @Override
    public Result<User> deleteUser(String name, String pwd) {
        return super.processResponse(() -> DataModelAdaptor.fromGrpc(
                stub.deleteUser(DeleteUserArgs.newBuilder()
                        .setName(name).setPwd(pwd)
                        .build()).getUser()));
    }

    @Override
    public Result<List<User>> searchUsers(String name, String pwd, String query) {
        return super.processResponse(() -> {
            List<User> users = new ArrayList<>();
            stub.searchUsers(SearchUsersArgs.newBuilder()
                    .setName(name).setPwd(pwd).setQuery(query)
                    .build())
                    .forEachRemaining(grpcUser -> users.add(DataModelAdaptor.fromGrpc(grpcUser)));
            return users;
        });
    }

    @Override
    public Result<Boolean> userExists(String name) {
        return super.processResponse(
                () -> stub.userExists(UserExistsArgs.newBuilder().setName(name).build()).getExists());
    }
}
