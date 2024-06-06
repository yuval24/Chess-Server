package org.example.Message_Data;

import java.util.List;

public class GameHistory {
    private String black_username;
    private String white_username;
    private String result;
    private String gameDate;
    private List<String> moves;

    public GameHistory(String black_username, String white_username, String result, String gameDate, List<String> moves) {
        this.black_username = black_username;
        this.white_username = white_username;
        this.result = result;
        this.gameDate = gameDate;
        this.moves = moves;
    }

    public String getBlack_username() {
        return black_username;
    }

    public String getWhite_username() {
        return white_username;
    }

    public String getResult() {
        return result;
    }
}
