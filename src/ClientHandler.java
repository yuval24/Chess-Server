import javax.imageio.IIOException;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable{
    public static ArrayList<ClientHandler> clients = new ArrayList<>();


    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private int clientUserName;

    public ClientHandler(Socket socket){
        try{
            this.clientUserName = clients.size();
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            clients.add(this);
        } catch(IOException e){
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        String messageFromClient;

        while(socket.isConnected()){
            try{
                messageFromClient = bufferedReader.readLine();
                System.out.println(this.clientUserName + messageFromClient);
                if(messageFromClient != null){
                    if(messageFromClient.equals("ENTER") || messageFromClient.equals("LEAVE")){
                        sendClientChecker();
                    }
                    else{
                        sendMove(messageFromClient);
                    }
                } else {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            } catch(IOException e){
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    public void sendMove(String move){
        for(ClientHandler clientHandler : clients){
            try{
                if(clientHandler.clientUserName != this.clientUserName){
                    clientHandler.bufferedWriter.write(move);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch(IOException e){
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    public void sendClientChecker(){
        for(ClientHandler clientHandler : clients){
            try{

                String msgSend = "";

                if(clients.size() == 2){
                    msgSend = "YES";
                } else {
                    msgSend = "NO";
                }
                System.out.println(msgSend);
                clientHandler.bufferedWriter.write(msgSend);
                clientHandler.bufferedWriter.newLine();
                clientHandler.bufferedWriter.flush();
            } catch(IOException e){
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }

    }

    private void removeClient(){
        clients.remove(this);
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
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
