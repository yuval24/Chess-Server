package org.example;

import org.example.Message_Data.GameHistory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DatabaseConnection {
    private static DatabaseConnection databaseConnectionInstance = null;
    private static final String PROPERTIES_FILE = "config/application.properties";
    private Connection c;
    public static Connection getConnection() {
        try (InputStream input = new FileInputStream(PROPERTIES_FILE)) {
            Properties properties = new Properties();
            properties.load(input);

            String jdbcUrl = properties.getProperty("database.url");
            String username = properties.getProperty("database.username");
            String password = properties.getProperty("database.password");

            return DriverManager.getConnection(jdbcUrl, username, password);
        } catch (IOException e) {
            // Log or handle the IOException appropriately
            throw new RuntimeException("Error loading database properties", e);
        } catch (SQLException e) {
            // Log or handle the SQLException appropriately
            throw new RuntimeException("Error connecting to the database", e);
        }
    }

    private DatabaseConnection(){
        c = getConnection();
    }

    public static synchronized DatabaseConnection getInstance(){
        if (databaseConnectionInstance == null) {
            databaseConnectionInstance = new DatabaseConnection();
        }
        return databaseConnectionInstance;
    }

    // Gets a username and returns true or false if this user is in the table users.
    public boolean isUsernameInDatabase(String username){
        try{
            String sql = "SELECT * FROM users WHERE username = ?";
            PreparedStatement preparedStatement = c.prepareStatement(sql);

            preparedStatement.setString(1, username);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                return true;
            }
        } catch (SQLException e){
            System.out.println("hi");
            printSQLException(e);
        }
        return false;
    }

    // Gets a username and a password and checks if they're matching in the database.
    public boolean isUsernameAndPasswordAreValid(String username, String password){
        try{
            String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement preparedStatement = c.prepareStatement(sql);

            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                return true;
            }
        } catch (SQLException e){
            printSQLException(e);
        }
        return false;
    }

    // Gets a username and a password and sets a user in the users table with those values
    public void sendUserToDatabase(String username, String password){
        try{
            String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement preparedStatement = c.prepareStatement(sql);

            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);

            preparedStatement.executeUpdate();
        } catch (SQLException e){
            printSQLException(e);
        }

    }

    public void addMove(String gameId, String moveMade, boolean isWhite){
        try{
            String sql = "INSERT INTO moves (game_id, move_made, is_white) VALUES (?, ?, ?)";
            PreparedStatement preparedStatement = c.prepareStatement(sql);

            preparedStatement.setString(1, gameId);
            preparedStatement.setString(2, moveMade);
            preparedStatement.setBoolean(3, isWhite);


            preparedStatement.executeUpdate();
        } catch (SQLException e){
            printSQLException(e);
        }
    }

    // Gets a game id, white username, black username, and insert to the game table a new game with those values.
    public void createNewGame(String gameId, String whiteUsername, String blackUsername){
        try{
            String sql = "INSERT INTO game (game_id, white_username, black_username) VALUES (?, ?, ?)";
            PreparedStatement preparedStatement = c.prepareStatement(sql);

            preparedStatement.setString(1, gameId);
            preparedStatement.setString(2, whiteUsername);
            preparedStatement.setString(3,blackUsername);


            preparedStatement.executeUpdate();
        } catch (SQLException e){
            printSQLException(e);
        }

    }

    public String getUsernameOfTheOtherPlayer(String currUsername){
        try{
            String sql = "SELECT * FROM game WHERE (black_username = ? OR white_username = ?) AND is_active = ?";
            PreparedStatement preparedStatement = c.prepareStatement(sql);

            preparedStatement.setString(1, currUsername);
            preparedStatement.setString(2, currUsername);
            preparedStatement.setBoolean(3,true);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){

                if (resultSet.getString("black_username").equals(currUsername)) {
                    return resultSet.getString("white_username");
                } else {
                    return resultSet.getString("black_username");
                }

            }
        } catch (SQLException e){
            printSQLException(e);
        }
        return null;
    }

    public String getGameId(String currUsername) {
        try{
            String sql = "SELECT * FROM game WHERE (black_username = ? OR white_username = ?) AND is_active = ?";
            PreparedStatement preparedStatement = c.prepareStatement(sql);

            preparedStatement.setString(1, currUsername);
            preparedStatement.setString(2, currUsername);
            preparedStatement.setBoolean(3,true);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){

                return resultSet.getString("game_id");

            }
        } catch (SQLException e){
            printSQLException(e);
        }
        return null;
    }

    public boolean getIsWhite(String currUsername) {
        try{
            String sql = "SELECT * FROM game WHERE (black_username = ? OR white_username = ?) AND is_active = ?";
            PreparedStatement preparedStatement = c.prepareStatement(sql);

            preparedStatement.setString(1, currUsername);
            preparedStatement.setString(2, currUsername);
            preparedStatement.setBoolean(3,true);

            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){

                return resultSet.getString("white_username").equals(currUsername);

            }
        } catch (SQLException e){
            printSQLException(e);
        }
        return false;
    }

    public List<GameHistory> getGameHistory(String username) {
        List<GameHistory> gameHistoryList = new ArrayList<>();
        try {
            String sql = "SELECT * FROM game WHERE (black_username = ? OR white_username = ?) AND is_active = ?";
            PreparedStatement preparedStatement = c.prepareStatement(sql);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, username);
            preparedStatement.setBoolean(3, false);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                // Retrieve data from the result set and create GameHistory objects
                String who_won = resultSet.getString("who_won");
                String blackUsername = resultSet.getString("black_username");
                String whiteUsername = resultSet.getString("white_username");
                String gameId = resultSet.getString("game_id");
                Timestamp timestamp = resultSet.getTimestamp("timestamp");

                Date date = new Date(timestamp.getTime());

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String gameDate = dateFormat.format(date);

                List<String> moves = getMoveHistoryForGame(gameId);

                // Create a GameHistory object
                GameHistory gameHistory = new GameHistory(blackUsername, whiteUsername, who_won, gameDate, moves);
                // Add the GameHistory object to the list
                gameHistoryList.add(gameHistory);
            }
        } catch (SQLException e) {
            printSQLException(e);
        }
        return gameHistoryList;
    }


    private List<String> getMoveHistoryForGame(String gameId){
        List<String> moves = new ArrayList<>();
        try {
            String sql = "SELECT * FROM moves WHERE game_id = ?";
            PreparedStatement preparedStatement = c.prepareStatement(sql);
            preparedStatement.setString(1, gameId);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                // Retrieve data from the result set and create GameHistory objects
                String moveMade = resultSet.getString("move_made");


                moves.add(moveMade);


            }
        } catch (SQLException e) {
            printSQLException(e);
        }
        return moves;
    }
    public void alterGameTableEndGame(String who_won, String username){
        try{
            String sql = "UPDATE game " +
                    "SET who_won = ? , is_active = ? " +
                    "WHERE (black_username = ? OR white_username = ?) AND is_active = ?";
            PreparedStatement preparedStatement = c.prepareStatement(sql);

            preparedStatement.setString(1, who_won);
            preparedStatement.setBoolean(2, false);
            preparedStatement.setString(3, username);
            preparedStatement.setString(4,username);
            preparedStatement.setBoolean(5,true);

            preparedStatement.executeUpdate();
        } catch (SQLException e){
            printSQLException(e);
        }
    }



    // This function prints the problem for every single sql error case.
    private static void printSQLException(SQLException ex) {
        for (Throwable e: ex) {
            if (e instanceof SQLException) {
                e.printStackTrace(System.err);
                System.err.println("SQLState: " + ((SQLException) e).getSQLState());
                System.err.println("Error Code: " + ((SQLException) e).getErrorCode());
                System.err.println("Message: " + e.getMessage());
                Throwable t = ex.getCause();
                while (t != null) {
                    System.out.println("Cause: " + t);
                    t = t.getCause();
                }
            }
        }
    }



}
