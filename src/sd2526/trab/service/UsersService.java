package sd2526.trab.service;

import java.util.List;
import java.util.logging.Logger;

import org.hibernate.exception.ConstraintViolationException;

import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Users;
import sd2526.trab.server.persistence.Hibernate;

public class UsersService implements Users {
    private final Hibernate hibernate;

    private static Logger Log = Logger.getLogger(UsersService.class.getName());

    public UsersService() {
        this.hibernate = Hibernate.getInstance();
    }

    @Override
    public Result<String> postUser(User user) {
        Log.info("postUser : " + user);

        // Check if user data is valid
        if (user.getName() == null || user.getPwd() == null || user.getDisplayName() == null
                || user.getDomain() == null) {
            Log.info("User object invalid.");

            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        User existing = hibernate.get(User.class, user.getName());
        if (existing != null) {
            if (existing.equals(user))
                return Result.ok(user.getName() + "@" + user.getDomain());
            return Result.error(Result.ErrorCode.CONFLICT);
        }

        try {
            hibernate.persist(user);
        } catch (ConstraintViolationException e) {
            Log.info("User already exists.");
            return Result.error(Result.ErrorCode.CONFLICT);
        } catch (Exception x) {
            x.printStackTrace(); // Unexpected exception. Signal internal server error.
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        return Result.ok(user.getName() + "@" + user.getDomain());
    }

    @Override
    public Result<User> getUser(String name, String pwd) {
        Log.info("getUser : name = " + name + "; pwd = " + pwd);

        // Check if parameters are valid
        if (name == null || pwd == null) {
            Log.info("Name or password null.");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        User user = hibernate.get(User.class, name);

        // Check if user exists and password matches
        if (user == null || !user.getPwd().equals(pwd)) {
            Log.info("User does not exist or password is incorrect.");
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        return Result.ok(user);
    }

    @Override
    public Result<User> updateUser(String name, String pwd, User info) {
        Log.info("updateUser : name = " + name + "; pwd = " + pwd + " ; info = " + info);

        // Covers both cases of null meaning no update and of updating all with equal
        // values
        if (info.getName() != null && !info.getName().equals(name)) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        Result<User> usrRes = this.getUser(name, pwd);
        if (!usrRes.isOK()) {
            return usrRes;
        }

        User usr = usrRes.value();

        if (info.getDomain() != null)
            usr.setDomain(info.getDomain());
        if (info.getPwd() != null)
            usr.setPwd(info.getPwd());
        if (info.getDisplayName() != null)
            usr.setDisplayName(info.getDisplayName());

        try {
            hibernate.update(usr);
            return Result.ok(usr);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<User> deleteUser(String name, String pwd) {
        Log.info("deleteUser : name = " + name + "; pwd = " + pwd);

        Result<User> usrRes = this.getUser(name, pwd);

        if (!usrRes.isOK()) {
            return usrRes;
        }

        User usr = usrRes.value();

        try {
            hibernate.delete(usr);
            return Result.ok(usr);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<List<User>> searchUsers(String name, String pwd, String query) {
        Log.info("searchUsers : name = " + name + "; pwd = " + pwd + "; pattern = " + query);

        Result<User> usrRes = this.getUser(name, pwd);

        if (!usrRes.isOK()) {
            return Result.error(usrRes.error());
        }

        try {
            List<User> list = hibernate.jpql("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER('%" + query + "%')",
                    User.class);

            list.forEach(u -> u.setPwd(""));

            return Result.ok(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    // Internal methods
    public Result<Boolean> userExists(String name) {
        if (name == null)
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        User user = hibernate.get(User.class, name);
        return Result.ok(user != null);
    }

}
