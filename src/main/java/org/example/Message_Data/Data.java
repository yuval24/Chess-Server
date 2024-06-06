package org.example.Message_Data;

import com.google.gson.Gson;

public class Data {
    private String type;
    private String sender;
    private String recipient;
    private String content;
    private String token;
    private boolean success;

    public Data(String type, String sender, String recipient, String token, boolean success, String content) {
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.token = token;
        this.success = success;
    }

    public String getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getContent() {
        return content;
    }

    public String getToken() {
        return token;
    }

    public boolean isSuccess() {
        return success;
    }

    public static Data fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Data.class);
    }


}
