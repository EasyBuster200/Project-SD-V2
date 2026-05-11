package sd2526.trab.server.rest;

import java.util.List;

import jakarta.inject.Singleton;
import sd2526.trab.api.Message;
import sd2526.trab.api.rest.RestMessages;
import sd2526.trab.client.java.Clients;
import sd2526.trab.server.utils.RestUtils;

@Singleton
public class GatewayMessagesResource implements RestMessages {
    private static String domain;

    public static void init(String d) {
        domain = d;
    }

    public GatewayMessagesResource() {
    }

    @Override
    public String postMessage(String pwd, Message msg) {
        return RestUtils.resultOrThrow(Clients.MessagesClient.get(domain).postMessage(pwd, msg));
    }

    @Override
    public Message getMessage(String name, String mid, String pwd) {
        return RestUtils.resultOrThrow(Clients.MessagesClient.get(domain).getInboxMessage(name, mid, pwd));
    }

    @Override
    public List<String> getMessages(String name, String pwd, String query) {
        return RestUtils.resultOrThrow(Clients.MessagesClient.get(domain).searchInbox(name, pwd, query));
    }

    @Override
    public void removeFromUserInbox(String name, String mid, String pwd) {
        RestUtils.resultOrThrow(Clients.MessagesClient.get(domain).removeInboxMessage(name, mid, pwd));
    }

    @Override
    public void deleteMessage(String name, String mid, String pwd) {
        RestUtils.resultOrThrow(Clients.MessagesClient.get(domain).deleteMessage(name, mid, pwd));
    }

    @Override
    public void deliverInternal(Message msg) {
        RestUtils.resultOrThrow(Clients.MessagesClient.get(domain).deliverInternal(msg));
    }

    @Override
    public void deleteInternal(String mid) {
        RestUtils.resultOrThrow(Clients.MessagesClient.get(domain).deleteInternal(mid));
    }

}
