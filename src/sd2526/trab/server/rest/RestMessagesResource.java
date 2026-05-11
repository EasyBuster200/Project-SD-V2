package sd2526.trab.server.rest;

import java.util.List;

import jakarta.inject.Singleton;
import sd2526.trab.api.Message;
import sd2526.trab.api.rest.RestMessages;
import sd2526.trab.server.utils.RestUtils;
import sd2526.trab.service.MessagesService;

@Singleton
public class RestMessagesResource implements RestMessages {

    private static MessagesService impl;

    public static void setImpl(MessagesService messageImpl) {
        impl = messageImpl;
    }

    public RestMessagesResource() {
    }

    @Override
    public String postMessage(String pwd, Message msg) {
        return RestUtils.resultOrThrow(impl.postMessage(pwd, msg));
    }

    @Override
    public Message getMessage(String name, String mid, String pwd) {
        return RestUtils.resultOrThrow(impl.getInboxMessage(name, mid, pwd));
    }

    @Override
    public List<String> getMessages(String name, String pwd, String query) {
        try {
            if (query == null || query.isEmpty())
                return RestUtils.resultOrThrow(impl.getAllInboxMessages(name, pwd));
            return RestUtils.resultOrThrow(impl.searchInbox(name, pwd, query));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void removeFromUserInbox(String name, String mid, String pwd) {
        RestUtils.resultOrThrow(impl.removeInboxMessage(name, mid, pwd));
    }

    @Override
    public void deleteMessage(String name, String mid, String pwd) {
        RestUtils.resultOrThrow(impl.deleteMessage(name, mid, pwd));
    }

    @Override
    public void deliverInternal(Message msg) {
        RestUtils.resultOrThrow(impl.deliverInternal(msg));
    }

    @Override
    public void deleteInternal(String mid) {
        RestUtils.resultOrThrow(impl.deleteInternal(mid));
    }

}