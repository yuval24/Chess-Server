package org.example;


import com.google.gson.Gson;
import org.example.Message_Data.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
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
    private static String EMPTY_TOKEN = "EMPTY_TOKEN";

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
                System.out.println(clients.size());
                break;
            }
        }
    }

    // Takes a json message and handles it according to its type.
    private void handleMessage(String message){
        try{
            Data messageData = Data.fromJson(message);
            if(Authentication.isProtectedActivity(messageData.getType())){
                boolean successfulVerification = Authentication.verifyJWTToken(messageData.getToken(), messageData.getSender());
                if(!successfulVerification){
                    sendMessageToRecipient(this, "OK", ActivityType.AUTHENTICATE, EMPTY_TOKEN, false);
                    return;
                }
            }

            switch (messageData.getType()) {
                case ActivityType.LOGIN, ActivityType.SIGNUP -> {
                    LoginData messageLoginData = LoginData.fromJson(message);
                    handleLogin(messageLoginData);
                }
                case ActivityType.MOVE -> {
                    MoveData messageMoveData = MoveData.fromJson(message);
                    handleMove(messageMoveData);
                }
                case ActivityType.GAME_HISTORY -> {
                    Data gameHistoryData = Data.fromJson(message);
                    handleGameHistory(gameHistoryData);
                }
                case ActivityType.REQUEST_TO_PLAY, ActivityType.REQUEST_TO_PLAY_SOMEONE, ActivityType.END_GAME, ActivityType.LEAVE_GAME, ActivityType.AUTHENTICATE ->
                        handleGameRequests(messageData);

            }
        } catch (Exception e){
            System.out.println("This message cannot be parsed");
            e.printStackTrace();
        }
    }


    //Gets a Game History message from a user
    // returns for him his games that he played
    private void handleGameHistory(Data gameHistoryData){
        List<GameHistory> gameHistories = databaseConnection.getGameHistory(gameHistoryData.getSender());
        sendGameHistoryToRecipient(gameHistories);
    }


    // Gets a Login Message and checking if the message is a Login or a Signup.
    // For signUp it checks in the database if the username is already exists and sends message to the client accordingly.
    // For login, it checks in the database if the username matches the password and sends message to the client accordingly.
    private void handleLogin(LoginData messageLoginData){
        this.clientUserName = messageLoginData.getUsername();
        String currToken = EMPTY_TOKEN;
        boolean success = false;
        if(messageLoginData.getType().equals(ActivityType.SIGNUP)){

            String content;
            if (databaseConnection.isUsernameInDatabase(this.clientUserName)) {
                content = "FAILED";
            } else {
                databaseConnection.sendUserToDatabase(this.clientUserName, messageLoginData.getPassword());
                content = "OK";
                success = true;
                currToken = Authentication.generateJWTToken(this.clientUserName);
            }
            sendMessageToRecipient(this, content ,ActivityType.SIGNUP, currToken, success);
            System.out.println(content);
        }
        else if(messageLoginData.getType().equals(ActivityType.LOGIN)) {

            String password = messageLoginData.getPassword();
            String content;
            if (databaseConnection.isUsernameAndPasswordAreValid(this.clientUserName, password)) {
                content = "OK";
                success = true;
                currToken = Authentication.generateJWTToken(this.clientUserName);

            } else {
                content = "FAILED";
            }

            sendMessageToRecipient(this, content, ActivityType.LOGIN, currToken, success);
        }
    }

    private void handleMove(MoveData messageMoveData){
        boolean verifyToken = Authentication.verifyJWTToken(messageMoveData.getToken(), messageMoveData.getSender());
        if(messageMoveData.getType().equals(ActivityType.MOVE) && verifyToken){
            String username = databaseConnection.getUsernameOfTheOtherPlayer(this.clientUserName);
            String gameId = databaseConnection.getGameId(this.clientUserName);
            boolean isWhite = databaseConnection.getIsWhite(this.clientUserName);
            String move = messageMoveData.getMove();
            for (ClientHandler client: clients) {
                if(client.clientUserName.equals(username)){
                    System.out.println("Sending");
                    databaseConnection.addMove(gameId, move, isWhite);
                    sendMoveToRecipient(client, move);
                }
            }
        }
    }

    // For Request to play, it adds the player to the queue.
    private void handleGameRequests(Data messageData){
        switch (messageData.getType()) {
            case ActivityType.REQUEST_TO_PLAY -> waitToPlayQueue.offer(this);
            case ActivityType.END_GAME ->
                    databaseConnection.alterGameTableEndGame(messageData.getContent(), messageData.getSender());
            case ActivityType.LEAVE_GAME -> {
                String username = databaseConnection.getUsernameOfTheOtherPlayer(this.clientUserName);
                databaseConnection.alterGameTableEndGame(messageData.getContent(), messageData.getSender());
                String content;
                if (messageData.getContent().equals("white") || messageData.getContent().equals("black")) {
                    content = "resign";
                } else if (messageData.getContent().equals("draw")) {
                    content = "abort";
                } else {
                    content = "invalid";
                }
                for (ClientHandler client : clients) {
                    if (client.clientUserName.equals(username)) {
                        sendMessageToRecipient(client, content, ActivityType.END_GAME, EMPTY_TOKEN, true);
                    }
                }
            }
            case ActivityType.AUTHENTICATE -> {
                boolean successfulVerification = Authentication.verifyJWTToken(messageData.getToken(), messageData.getSender());
                if(successfulVerification){
                    this.clientUserName = messageData.getSender();
                }
                sendMessageToRecipient(this, "OK", ActivityType.AUTHENTICATE, EMPTY_TOKEN, successfulVerification);
            }
            case ActivityType.REQUEST_TO_PLAY_SOMEONE -> {
                ClientHandler otherPlayer = null;
                String otherPlayerName = messageData.getContent();
                for(ClientHandler clientHandler : clients){
                    if(clientHandler.clientUserName.equals(otherPlayerName)){
                        otherPlayer = clientHandler;
                        break;
                    }
                }
                if(otherPlayer != null){
                    createsANewOnlineGame(this, otherPlayer);
                }

            }
        }
    }

    private void createsANewOnlineGame(ClientHandler p1, ClientHandler p2){
        boolean isP1White = getARandomBoolean();
        String p1Username = p1.getClientUserName();
        String p2Username = p2.getClientUserName();
        String uniqueGameId = getAUniqueId();
        if(isP1White){
            databaseConnection.createNewGame(uniqueGameId, p1Username, p2Username);
        } else{
            databaseConnection.createNewGame(uniqueGameId, p2Username, p1Username);
        }
        sendToUserApproveGame(p1, isP1White, p1Username);
        sendToUserApproveGame(p2, !isP1White, p2Username);
    }
    private String convertBooleanIsWhite(boolean isWhite){
        return isWhite ? "WHITE" : "BLACK";
    }

    // Generates a unique Id.
    private String getAUniqueId(){
        return UUID.randomUUID().toString();
    }

    // Generates a Random Boolean.
    private boolean getARandomBoolean(){
        Random rand = new Random();
        return rand.nextBoolean();
    }
    private void sendToUserApproveGame(ClientHandler targetClient, boolean isWhite, String otherUsername){

        Data message = new Data(ActivityType.REQUEST_TO_PLAY_SOMEONE, otherUsername, targetClient.getClientUserName(), "empty-token", true, convertBooleanIsWhite(isWhite));
        try {
            String messageJson = gson.toJson(message);
            System.out.println(messageJson);
            targetClient.getBufferedWriter().write(messageJson);
            targetClient.getBufferedWriter().newLine();
            targetClient.getBufferedWriter().flush();
        } catch(IOException e){
            targetClient.closeEverything();
        }
    }

    // sending the message to the required user/users(later on).
    private void sendMessageToRecipient(ClientHandler targetClient, String content, String activityType, String token, boolean success) {
        // Use your user or session manager to get the recipient's connection information
        // and send the message
        Data message = new Data(activityType, "server", targetClient.clientUserName, token, success, content);
        try {
            String messageJson = gson.toJson(message);
            System.out.println(messageJson);
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
        MoveData message = new MoveData(ActivityType.MOVE, "server", targetClient.clientUserName, "empty-token" , true, "OK",move);

        try {
            String messageJson = gson.toJson(message);
            System.out.println(messageJson);
            targetClient.bufferedWriter.write(messageJson);
            targetClient.bufferedWriter.newLine();
            targetClient.bufferedWriter.flush();
            System.out.println("move was send");
        } catch(IOException e){
            closeEverything();
        }
    }

    private void sendGameHistoryToRecipient(List<GameHistory> gameHistories) {
        GameHistoryData message = new GameHistoryData(ActivityType.GAME_HISTORY, "server", clientUserName, "empty-token" , true, "OK", gameHistories);

        try {
            String messageJson = gson.toJson(message);
            System.out.println(messageJson);
            this.bufferedWriter.write(messageJson);
            this.bufferedWriter.newLine();
            this.bufferedWriter.flush();
            System.out.println("game history was send");
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
