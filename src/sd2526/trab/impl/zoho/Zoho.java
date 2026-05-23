package sd2526.trab.impl.zoho;

import java.util.Collections;
import java.util.List;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import sd2526.trab.impl.utils.JSON;
import sd2526.trab.impl.zoho.msgs.ZohoAccount;
import sd2526.trab.impl.zoho.msgs.ZohoAccountReply;
import sd2526.trab.impl.zoho.msgs.ZohoGenericReply;
import sd2526.trab.impl.zoho.msgs.ZohoMessageContent;
import sd2526.trab.impl.zoho.msgs.ZohoMessageContentReply;
import sd2526.trab.impl.zoho.msgs.ZohoMessageListReply;
import sd2526.trab.impl.zoho.msgs.ZohoMessageSummary;
import sd2526.trab.impl.zoho.msgs.ZohoSendEmailRequest;

public class Zoho {
	static final String MAIL_API_BASE = "https://mail.zoho.eu/api";

	static final String CLIENT_ID       = "1000.OF3ENQSLN1LX1VH9YRCMRWRODU9LWN";
	static final String CLIENT_SECRET   = "6a775bc7518e5b52a2318df0895b3ff44f255e70d6";
	static final String REFRESH_TOKEN   = "1000.08f9088e27eff3ce246e4c6b046bcc7c.ffb2831c13f4098702cb1ce111b0ccb0";

	static final String ACCOUNT_ID      = "8668419000000002002";
	static final String INBOX_FOLDER_ID = "8668419000000002008";
	static final String FROM_ADDRESS    = "dcr.coelho@zohomail.eu";

	private static final String ACCOUNTS = "/accounts";
	private static final String MESSAGES = "/messages";
	private static final String FOLDERS  = "/folders";
	private static final String VIEW     = "/view";
	private static final String CONTENT  = "/content";

	final OAuth20Service service;
	final ZohoTokenManager tokenManager;

	static Zoho instance;

	private Zoho() {
		service = ZohoServiceFactory.buildService(CLIENT_ID, CLIENT_SECRET);
		tokenManager = new ZohoTokenManager(service, REFRESH_TOKEN);
	}

	synchronized public static Zoho getInstance() {
		if (instance == null)
			instance = new Zoho();
		return instance;
	}

	public ZohoAccount getAccount() throws Exception {
		var accessToken = new OAuth2AccessToken(tokenManager.getValidAccessToken());

		OAuthRequest request = new OAuthRequest(Verb.GET, MAIL_API_BASE + ACCOUNTS);
		service.signRequest(accessToken, request);

		try (Response response = service.execute(request)) {
			if (response.isSuccessful()) {
				var body = response.getBody();
				var data = JSON.decode(body, ZohoAccountReply.class).data();
				if (data == null || data.isEmpty())
					return null;
				return data.get(0);
			} else {
				System.err.println(response.getCode() + "/" + response.getBody());
				return null;
			}
		}
	}

	public boolean sendEmail(String toAddress, String subject, String body) throws Exception {
		var accessToken = new OAuth2AccessToken(tokenManager.getValidAccessToken());

		OAuthRequest request = new OAuthRequest(Verb.POST,
				MAIL_API_BASE + ACCOUNTS + "/" + ACCOUNT_ID + MESSAGES);
		request.addHeader("Content-Type", "application/json; charset=utf-8");
		request.addHeader("Accept", "application/json");
		request.setPayload(JSON.encode(new ZohoSendEmailRequest(FROM_ADDRESS, toAddress, subject, body)));

		service.signRequest(accessToken, request);

		try (Response response = service.execute(request)) {
			if (response.isSuccessful()) {
				return true;
			} else {
				System.err.println("sendEmail failed: " + response.getCode() + " / " + response.getBody());
				return false;
			}
		}
	}

	public List<ZohoMessageSummary> listInbox() throws Exception {
		var accessToken = new OAuth2AccessToken(tokenManager.getValidAccessToken());

		OAuthRequest request = new OAuthRequest(Verb.GET,
				MAIL_API_BASE + ACCOUNTS + "/" + ACCOUNT_ID + MESSAGES + VIEW);
		request.addHeader("Accept", "application/json");

		service.signRequest(accessToken, request);

		try (Response response = service.execute(request)) {
			if (response.isSuccessful()) {
				var reply = JSON.decode(response.getBody(), ZohoMessageListReply.class);
				return reply.data() == null ? Collections.emptyList() : reply.data();
			} else {
				System.err.println("listInbox failed: " + response.getCode() + " / " + response.getBody());
				return Collections.emptyList();
			}
		}
	}

	public String getEmailContent(String zohoMessageId) throws Exception {
		var accessToken = new OAuth2AccessToken(tokenManager.getValidAccessToken());

		OAuthRequest request = new OAuthRequest(Verb.GET,
				MAIL_API_BASE + ACCOUNTS + "/" + ACCOUNT_ID
						+ FOLDERS + "/" + INBOX_FOLDER_ID
						+ MESSAGES + "/" + zohoMessageId + CONTENT);
		request.addHeader("Accept", "application/json");

		service.signRequest(accessToken, request);

		try (Response response = service.execute(request)) {
			if (response.isSuccessful()) {
				ZohoMessageContent data = JSON.decode(response.getBody(), ZohoMessageContentReply.class).data();
				return data == null ? null : data.content();
			} else {
				System.err.println("getEmailContent failed: " + response.getCode() + " / " + response.getBody());
				return null;
			}
		}
	}

	public boolean deleteEmail(String zohoMessageId) throws Exception {
		var accessToken = new OAuth2AccessToken(tokenManager.getValidAccessToken());

		OAuthRequest request = new OAuthRequest(Verb.DELETE,
				MAIL_API_BASE + ACCOUNTS + "/" + ACCOUNT_ID
						+ FOLDERS + "/" + INBOX_FOLDER_ID
						+ MESSAGES + "/" + zohoMessageId);
		request.addHeader("Accept", "application/json");

		service.signRequest(accessToken, request);

		try (Response response = service.execute(request)) {
			if (response.isSuccessful()) {
				return true;
			} else {
				System.err.println("deleteEmail failed: " + response.getCode() + " / " + response.getBody());
				return false;
			}
		}
	}

	public int emptyInbox() throws Exception {
		int deleted = 0;
		for (ZohoMessageSummary msg : listInbox()) {
			if (deleteEmail(msg.messageId()))
				deleted++;
		}
		return deleted;
	}

	@SuppressWarnings("unused")
	private static ZohoGenericReply _unusedReference() {
		return null;
	}
}