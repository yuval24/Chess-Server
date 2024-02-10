import Message_Data.ActivityType;
import Message_Data.Data;
import Message_Data.LoginData;
import Message_Data.MoveData;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientHandler implements Runnable{
    public static ArrayList<ClientHandler> clients = new ArrayList<>();
    public static LinkedBlockingQueue<ClientHandler> waitToPlayQueue = new LinkedBlockingQueue<>();
    private DatabaseConnection databaseConnection;
    private Gson gson = new Gson();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUserName;

    public ClientHandler(Socket socket){
        try {
            this.clientUserName = "";
            this.socket = socket;
            this.databaseConnection = DatabaseConnection.getInstance();
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            clients.add(this);
        } catch(IOException e){
            closeEverything();
        }
    }

    @Override
    public void run() {
        String messageFromClient;

        while(socket.isConnected()){
            try{
                messageFromClient = bufferedReader.readLine();
                System.out.println(this.clientUserName + " : "+ messageFromClient);
                if(messageFromClient != null){
                   handleMessage(messageFromClient);
                } else {
                    closeEverything();
                }
            } catch(IOException e){
                closeEverything();
                break;
            }
        }
    }

    // Takes a json message and handles it according to its type.
    private void handleMessage(String message){
        try{
            Data messageData = Data.fromJson(message);

            if(messageData.getType().equals(ActivityType.LOGIN) || messageData.getType().equals(ActivityType.SIGNUP)){
                LoginData messageLoginData = LoginData.fromJson(message);
                handleLogin(messageLoginData);
            } else if(messageData.getType().equals(ActivityType.MOVE)){
                MoveData messageMoveData = MoveData.fromJson(message);
                handleMove(messageMoveData);
            } else if(messageData.getType().equals(ActivityType.REQUEST_TO_PLAY) || messageData.getType().equals(ActivityType.END_GAME) || messageData.getType().equals(ActivityType.LEAVE_GAME)){
                handleGameRequests(messageData);
            }
        } catch (Exception e){
            System.out.println("This message cannot be parsed");
            e.printStackTrace();
        }
    }

    // Gets a Login Message and checking if the message is a Login or a Signup.
    // For signUp it checks in the database if the username is already exists and sends message to the client accordingly.
    // For login, it checks in the database if the username matches the password and sends message to the client accordingly.
    private void handleLogin(LoginData messageLoginData){
        if(messageLoginData.getType().equals(ActivityType.SIGNUP)){
            this.clientUserName = messageLoginData.getUsername();
            String content;
            if (databaseConnection.isUsernameInDatabase(this.clientUserName)) {
                content = "FAILED";
            } else {
                databaseConnection.sendUserToDatabase(this.clientUserName, messageLoginData.getPassword());
                content = "OK";
            }
            sendMessageToRecipient(this, content,ActivityType.SIGNUP);
            System.out.println(content);
        }
        else if(messageLoginData.getType().equals(ActivityType.LOGIN)) {
            this.clientUserName = messageLoginData.getUsername();

            String password = messageLoginData.getPassword();
            String content;
            if (databaseConnection.isUsernameAndPasswordAreValid(this.clientUserName, password)) {
                content = "OK";
            } else {
                content = "FAILED";
            }

            sendMessageToRecipient(this, content, ActivityType.LOGIN);
        }
    }

    private void handleMove(MoveData messageMoveData){
        if(messageMoveData.getType().equals(ActivityType.MOVE)){
            String username = databaseConnection.getUsernameOfTheOtherPlayer(this.clientUserName);
            for (ClientHandler client: clients) {
                if(client.clientUserName.equals(username)){
                    System.out.println("Sending");
                    sendMoveToRecipient(client, messageMoveData.getMove());
                }
            }
        }
    }

    // For Request to play, it adds the player to the queue.
    private void handleGameRequests(Data messageData){
        if(messageData.getType().equals(ActivityType.REQUEST_TO_PLAY)){
            waitToPlayQueue.offer(this);
        } else if(messageData.getType().equals(ActivityType.END_GAME)){
            databaseConnection.alterGameTableEndGame(messageData.getContent(), messageData.getSender());
        } else if(messageData.getType().equals(ActivityType.LEAVE_GAME)){
            String username = databaseConnection.getUsernameOfTheOtherPlayer(this.clientUserName);
            databaseConnection.alterGameTableEndGame(messageData.getContent(), messageData.getSender());
            for (ClientHandler client: clients) {
                if(client.clientUserName.equals(username)){
                    sendMessageToRecipient(client, "OK", ActivityType.END_GAME);
                }
            }

        }
    }

    // sending the message to the required user/users(later on).
    private void sendMessageToRecipient(ClientHandler targetClient, String content, String activityType) {
        // Use your user or session manager to get the recipient's connection information
        // and send the message
        Data message = new Data(activityType, "server", targetClient.clientUserName, content);
        try {
            String messageJson = gson.toJson(message);

            targetClient.bufferedWriter.write(messageJson);
            targetClient.bufferedWriter.newLine();
            targetClient.bufferedWriter.flush();
        } catch(IOException e){
            closeEverything();
        }
    }

    // Sending a move to the target client.
    private void sendMoveToRecipient(ClientHandler targetClient, String move) {
        // Use your user or session manager to get the recipient's connection information
        // and send the message
        MoveData message = new MoveData(ActivityType.MOVE, "server", targetClient.clientUserName, "OK",move);
        try {
            String messageJson = gson.toJson(message);

            targetClient.bufferedWriter.write(messageJson);
            targetClient.bufferedWriter.newLine();
            targetClient.bufferedWriter.flush();
            System.out.println("was send");
        } catch(IOException e){
            closeEverything();
        }
    }

    public String getClientUserName() {
        return clientUserName;
    }

    private void removeClient(){
        clients.remove(this);
        waitToPlayQueue.remove(this);
    }

    public BufferedWriter getBufferedWriter() {
        return bufferedWriter;
    }

    public void closeEverything(){
        removeClient();
        try{
            if(bufferedReader != null){
                bufferedReader.close();
            }
            if(bufferedWriter != null){
                bufferedWriter.close();
            }
            if(socket != null){
                socket.close();
            }
        } catch(IOException e){
            e.printStackTrace();
        }
        System.out.println("client is closing");
    }
}
