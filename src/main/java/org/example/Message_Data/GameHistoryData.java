package org.example.Message_Data;

import com.google.gson.Gson;

import java.util.List;

public class GameHistoryData extends Data{
    private List<GameHistory> gameHistories;

    public GameHistoryData(String type, String sender, String recipient, String token, boolean success, String content, List<GameHistory> gameHistories) {
        super(type, sender, recipient, token, success, content);
        this.gameHistories = gameHistories;
    }

    public List<GameHistory> getGameHistories() {
        return gameHistories;
    }

    public static GameHistoryData fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, GameHistoryData.class);
    }
}
