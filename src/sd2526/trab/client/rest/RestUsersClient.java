package sd2526.trab.client.rest;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Users;
import sd2526.trab.api.rest.RestUsers;

public class RestUsersClient extends RestClient implements Users {

    private static Logger logger = Logger.getLogger(RestUsersClient.class.getName());

    public RestUsersClient(URI serverURI) {
        super(serverURI, logger);
        target = super.target.path(RestUsers.PATH);
    }

    @Override
    public Result<String> postUser(User user) {
        return super.retry(() -> doPostUser(user));
    }

    private Result<String> doPostUser(User user) {
        Response r = target.request().accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(user, MediaType.APPLICATION_JSON));
        return super.processResponse(r, String.class);
    }

    @Override
    public Result<User> getUser(String userId, String pwd) {
        return super.retry(() -> doGetUser(userId, pwd));
    }

    private Result<User> doGetUser(String userId, String pwd) {
        Response r = target.path(userId)
                .queryParam(RestUsers.PWD, pwd).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        return super.processResponse(r, User.class);
    }

    @Override
    public Result<User> updateUser(String name, String pwd, User info) {
        return super.retry(() -> doUpdateUser(name, pwd, info));
    }

    private Result<User> doUpdateUser(String name, String pwd, User info) {
        Response r = target.path(name)
                .queryParam(RestUsers.PWD, pwd).request()
                .accept(MediaType.APPLICATION_JSON)
                .put(Entity.entity(info, MediaType.APPLICATION_JSON));

        return super.processResponse(r, User.class);
    }

    @Override
    public Result<User> deleteUser(String name, String pwd) {
        return super.retry(() -> doDeleteUser(name, pwd));
    }

    private Result<User> doDeleteUser(String name, String pwd) {
        Response r = target.path(name)
                .queryParam(RestUsers.PWD, pwd).request()
                .accept(MediaType.APPLICATION_JSON)
                .delete();

        return super.processResponse(r, User.class);
    }

    @Override
    public Result<List<User>> searchUsers(String name, String pwd, String query) {
        return super.retry(() -> doSearchUsers(name, pwd, query));
    }

    private Result<List<User>> doSearchUsers(String name, String pwd, String query) {
        Response r = target
                .queryParam(RestUsers.NAME, name)
                .queryParam(RestUsers.PWD, pwd)
                .queryParam(RestUsers.QUERY, query)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        return super.processResponse(r, new GenericType<List<User>>() {
        });
    }

    public Result<Boolean> userExists(String name) {
        return super.retry(() -> {
            Response r = target.path(name).path("exists")
                    .request().accept(MediaType.APPLICATION_JSON).get();
            return super.processResponse(r, Boolean.class);
        });
    }
}
