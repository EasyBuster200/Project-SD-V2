package sd2526.trab.server.rest;

import java.util.List;

import jakarta.inject.Singleton;
import sd2526.trab.api.User;
import sd2526.trab.api.rest.RestUsers;
import sd2526.trab.client.java.Clients;
import sd2526.trab.server.utils.RestUtils;

@Singleton
public class GatewayUsersResource implements RestUsers {

    private static String domain;

    public static void init(String d) {
        domain = d;
    }

    public GatewayUsersResource() {
    }

    @Override
    public String postUser(User user) {
        return RestUtils.resultOrThrow(Clients.UsersClient.get(domain).postUser(user));
    }

    @Override
    public User getUser(String name, String pwd) {
        return RestUtils.resultOrThrow(Clients.UsersClient.get(domain).getUser(name, pwd));
    }

    @Override
    public User updateUser(String name, String pwd, User info) {
        return RestUtils.resultOrThrow(Clients.UsersClient.get(domain).updateUser(name, pwd, info));
    }

    @Override
    public User deleteUser(String name, String pwd) {
        return RestUtils.resultOrThrow(Clients.UsersClient.get(domain).deleteUser(name, pwd));
    }

    @Override
    public List<User> searchUsers(String name, String pwd, String pattern) {
        return RestUtils.resultOrThrow(Clients.UsersClient.get(domain).searchUsers(name, pwd, pattern));
    }

    @Override
    public boolean userExists(String name) {
        return RestUtils.resultOrThrow(Clients.UsersClient.get(domain).userExists(name));
    }
}
