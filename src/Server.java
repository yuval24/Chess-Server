import javax.imageio.IIOException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private ServerSocket serverSocket;
    private static final int port = 3001;


    public Server(ServerSocket serverSocket){
        this.serverSocket = serverSocket;
    }

    public void startServer(){
        try{
            while(!serverSocket.isClosed()){
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

    public void closeServer() {
        try{
            this.serverSocket.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);

        System.out.println("Server listening on port " + port);
        Server server = new Server(serverSocket);
        server.startServer();
    }
}
