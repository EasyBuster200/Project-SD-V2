package sd2526.trab.server.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class InboxEntry {
    @Id
    @GeneratedValue
    private long id;

    private String userName;
    private String messageId;

    public InboxEntry() {
    }

    public InboxEntry(String userName, String messageId) {
        this.userName = userName;
        this.messageId = messageId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
