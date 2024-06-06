package org.example;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.example.Message_Data.ActivityType;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

public class Authentication {
    private static final String PROPERTIES_FILE = "config/application.properties";
    private static String secretKey;

    // Load secret key from properties file
    static {
        try (InputStream input = new FileInputStream(PROPERTIES_FILE)) {
            Properties properties = new Properties();
            properties.load(input);
            secretKey = properties.getProperty("jwt.secretKey");
        } catch (IOException e) {
            throw new RuntimeException("Error loading JWT secret key from properties file", e);
        }
    }

    private Authentication(){
        //making sure it cannot get initialized
    }

    // generating a token for this username
    public static String generateJWTToken(String username) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secretKey);
            return JWT.create()
                    .withSubject(username)
                    .withExpiresAt(new Date(System.currentTimeMillis() + 3600000)) // 1 hour expiration
                    .sign(algorithm);
        } catch (Exception e) {
            throw new RuntimeException("Error generating JWT token", e);
        }
    }

    // Verifying if the token is valid to this username
    public static boolean verifyJWTToken(String token, String expectedUsername) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secretKey);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withSubject(expectedUsername)
                    .build();
            verifier.verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public static String getUsernameFromJWTToken(String token) {
        try {
            DecodedJWT decodedJWT = JWT.decode(token);
            return decodedJWT.getSubject();
        } catch (JWTDecodeException e) {
            throw new RuntimeException("Error decoding JWT token", e);
        }
    }


    public static boolean isProtectedActivity(String activityType) {
        return activityType.equals(ActivityType.MOVE) ||
                activityType.equals(ActivityType.REQUEST_TO_PLAY) ||
                activityType.equals(ActivityType.REQUEST_TO_PLAY_SOMEONE) ||
                activityType.equals(ActivityType.END_GAME) ||
                activityType.equals(ActivityType.LEAVE_GAME) ||
                activityType.equals(ActivityType.GAME_HISTORY);
    }


}
