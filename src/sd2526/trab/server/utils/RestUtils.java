package sd2526.trab.server.utils;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import sd2526.trab.api.java.Result;

public class RestUtils {
    public static Throwable errorCodeToStatus(Result.ErrorCode error) {
        var status = switch (error) {
            case NOT_FOUND ->
                Status.NOT_FOUND;
            case CONFLICT ->
                Status.CONFLICT;
            case FORBIDDEN ->
                Status.FORBIDDEN;
            case NOT_IMPLEMENTED ->
                Status.NOT_IMPLEMENTED;
            case BAD_REQUEST ->
                Status.BAD_REQUEST;
            default ->
                Status.INTERNAL_SERVER_ERROR;
        };

        return new WebApplicationException(status);
    }

    public static <T> T resultOrThrow(Result<T> res) {
        if (res.isOK()) {
            return res.value();
        }
        throw (WebApplicationException) errorCodeToStatus(res.error());
    }
}
