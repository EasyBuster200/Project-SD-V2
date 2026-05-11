package sd2526.trab.server.rest;

import java.util.List;

import jakarta.inject.Singleton;
import sd2526.trab.api.User;
import sd2526.trab.api.rest.RestUsers;
import sd2526.trab.server.utils.RestUtils;
import sd2526.trab.service.UsersService;

@Singleton
public class RestUsersResource implements RestUsers {
    UsersService impl = new UsersService();

    public RestUsersResource() {
    }

    @Override
    public String postUser(User user) {
        return RestUtils.resultOrThrow(this.impl.postUser(user));
    }

    @Override
    public User getUser(String name, String pwd) {
        return RestUtils.resultOrThrow(this.impl.getUser(name, pwd));
    }

    @Override
    public User updateUser(String name, String pwd, User info) {
        return RestUtils.resultOrThrow(this.impl.updateUser(name, pwd, info));
    }

    @Override
    public User deleteUser(String name, String pwd) {
        return RestUtils.resultOrThrow(this.impl.deleteUser(name, pwd));
    }

    @Override
    public List<User> searchUsers(String name, String pwd, String query) {
        return RestUtils.resultOrThrow(this.impl.searchUsers(name, pwd, query));
    }

    @Override
    public boolean userExists(String name) {
        return RestUtils.resultOrThrow(impl.userExists(name));
    }

}
