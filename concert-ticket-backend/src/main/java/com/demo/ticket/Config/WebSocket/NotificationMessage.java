package com.demo.ticket.Config.WebSocket;

public class NotificationMessage {

    private String username;
    private String title;
    private String content;

    public NotificationMessage(String username, String title, String content) {
        this.username = username;
        this.title = title;
        this.content = content;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
