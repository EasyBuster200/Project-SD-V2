package sd2526.trab.client.rest;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.rest.RestMessages;

public class RestMessagesClient extends RestClient implements Messages {
    private static Logger logger = Logger.getLogger(RestMessagesClient.class.getName());

    public RestMessagesClient(URI serverURI) {
        super(serverURI, logger);
        target = super.target.path(RestMessages.PATH);
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        return super.retry(() -> {
            Response r = target.queryParam(RestMessages.PWD, pwd)
                    .request().accept(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(msg, MediaType.APPLICATION_JSON));
            return super.processResponse(r, String.class);
        });
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        return super.retry(() -> {
            Response r = target.path(RestMessages.MBOX).path(name).path(mid)
                    .queryParam(RestMessages.PWD, pwd)
                    .request().accept(MediaType.APPLICATION_JSON).get();
            return super.processResponse(r, Message.class);
        });
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd) {
        return super.retry(() -> {
            Response r = target.path(RestMessages.MBOX).path(name)
                    .queryParam(RestMessages.PWD, pwd)
                    .request().accept(MediaType.APPLICATION_JSON).get();
            return super.processResponse(r, new GenericType<List<String>>() {
            });
        });
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
        return super.retry(() -> {
            Response r = target.path(RestMessages.MBOX).path(name).path(mid)
                    .queryParam(RestMessages.PWD, pwd)
                    .request().delete();
            return super.processResponse(r);
        });
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        return super.retry(() -> {
            Response r = target.path(name).path(mid)
                    .queryParam(RestMessages.PWD, pwd)
                    .request().delete();
            return super.processResponse(r);
        });
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query) {
        return super.retry(() -> {
            Response r = target.path(RestMessages.MBOX).path(name)
                    .queryParam(RestMessages.PWD, pwd)
                    .queryParam(RestMessages.QUERY, query)
                    .request().accept(MediaType.APPLICATION_JSON).get();
            return super.processResponse(r, new GenericType<List<String>>() {
            });
        });
    }

    // Internal stuff

    public Result<Void> deliverInternal(Message msg) {
        return super.retry(() -> {
            Response r = target.path(RestMessages.INTERNAL).path("deliver")
                    .request()
                    .post(Entity.entity(msg, MediaType.APPLICATION_JSON));
            return super.processResponse(r);
        });
    }

    public Result<Void> deleteInternal(String mid) {
        return super.retry(() -> {
            Response r = target.path(RestMessages.INTERNAL).path(mid)
                    .request().delete();
            return super.processResponse(r);
        });
    }
}