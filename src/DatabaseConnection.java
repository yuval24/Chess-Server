import java.sql.*;

public class DatabaseConnection {
    private static DatabaseConnection databaseConnectionInstance = null;
    private Connection c;
    public static Connection getConnection() throws SQLException {
        String jdbcUrl = "jdbc:postgresql://localhost:5432/Chess";
        String username = "postgres";
        String password = "password";

        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private DatabaseConnection(){
        try{
            c = getConnection();
        } catch (SQLException e){
            printSQLException(e);
        }
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
