package sd2526.trab.server.utils;

import java.util.function.Function;

import io.grpc.stub.StreamObserver;
import sd2526.trab.api.java.Result;

public class GrpcUtils {
    public static Throwable errorCodeToStatus(Result.ErrorCode error) {
        var status = switch (error) {
            case NOT_FOUND ->
                io.grpc.Status.NOT_FOUND;
            case CONFLICT ->
                io.grpc.Status.ALREADY_EXISTS;
            case FORBIDDEN ->
                io.grpc.Status.PERMISSION_DENIED;
            case NOT_IMPLEMENTED ->
                io.grpc.Status.UNIMPLEMENTED;
            case BAD_REQUEST ->
                io.grpc.Status.INVALID_ARGUMENT;
            default ->
                io.grpc.Status.INTERNAL;
        };

        return status.asException();
    }

    public static <T, V> void toGrpcResult(Result<T> res, StreamObserver<V> responseObserver, Function<T, V> func) {
        if (res.isOK()) {
            responseObserver.onNext(func.apply(res.value()));
            responseObserver.onCompleted();
            return;
        }
        responseObserver.onError(errorCodeToStatus(res.error()));
    }
}
