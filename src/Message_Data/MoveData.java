package Message_Data;

import com.google.gson.Gson;

public class MoveData extends Data {
    private String move;

    public MoveData(String type, String sender, String recipient, String content, String move) {
        super(type, sender, recipient, content);
        this.move = move;
    }

    public String getMove() {
        return move;
    }

    public static MoveData fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, MoveData.class);
    }
}
