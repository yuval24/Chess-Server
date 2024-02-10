package Message_Data;

import com.google.gson.Gson;

public class Data {
    private String type;
    private String sender;
    private String recipient;
    private String content;

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

    public void setContent(String content) {
        this.content = content;
    }

    public Data(String type, String sender, String recipient, String content) {
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
    }

    public static Data fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Data.class);
    }


}
