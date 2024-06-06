package org.example.Message_Data;

import com.google.gson.Gson;

public class LoginData extends Data {
    private String username;
    private String password;

    public LoginData(String type, String sender, String recipient, String content, String username, String password) {
        super(type, sender, recipient, "empty-token", true, content);
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public static LoginData fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, LoginData.class);
    }
}
