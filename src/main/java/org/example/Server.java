package org.example;


import com.google.gson.Gson;
import org.example.Message_Data.ActivityType;
import org.example.Message_Data.Data;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.UUID;

public class Server {
    private final ServerSocket serverSocket;
    private final Gson gson = new Gson();
    private DatabaseConnection databaseConnection;
    private static final int PORT = 3001;


    public Server(ServerSocket serverSocket){
        this.serverSocket = serverSocket;
    }

    public void startServer(){
        try{
            while(!serverSocket.isClosed()){
                databaseConnection = DatabaseConnection.getInstance();
                Thread checkForNewGame = new Thread(this::checkForNewGames);
                checkForNewGame.start();
                Socket socket = serverSocket.accept();
                System.out.println("a new client connected");
                ClientHandler clientHandler = new ClientHandler(socket);

                Thread thread = new Thread(clientHandler);
                thread.start();


            }
        } catch (IOException e){
            closeServer();
            e.printStackTrace();
        }
    }

    // For Request to play. It checks if the queue has at least 2 players,
    // it generates a game for the first 2 players.
    private void checkForNewGames(){
        while(!serverSocket.isClosed()){
            if(ClientHandler.waitToPlayQueue.size() >= 2){
                ClientHandler p1 = ClientHandler.waitToPlayQueue.poll();
                ClientHandler p2 = ClientHandler.waitToPlayQueue.poll();
                if(p1 != null && p2 != null){
                    createsANewOnlineGame(p1, p2);
                } else {
                    System.out.println("Something went wrong");
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // It is choosing a random color for each player and creates a new game in the database.
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
        sendToUserApproveGame(p1, isP1White, p2Username);
        sendToUserApproveGame(p2, !isP1White, p1Username);
    }

    // Send to a user that the game is started.
    private void sendToUserApproveGame(ClientHandler targetClient, boolean isWhite, String otherUsername){

        Data message = new Data(ActivityType.APPROVE_TO_PLAY, otherUsername, targetClient.getClientUserName(), "empty-token", true, convertBooleanIsWhite(isWhite));
        try {
            String messageJson = gson.toJson(message);

            targetClient.getBufferedWriter().write(messageJson);
            targetClient.getBufferedWriter().newLine();
            targetClient.getBufferedWriter().flush();
        } catch(IOException e){
            targetClient.closeEverything();
        }
    }

    // Converts a boolean is White to a string.
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

    // Closing the server socket.
    public void closeServer() {
        try{
            this.serverSocket.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);

        System.out.println("Server listening on port " + PORT);
        Server server = new Server(serverSocket);
        server.startServer();
    }
}
