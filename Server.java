/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.netprogproj;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author C-ROAD
 */
public class Server {
    
    ServerSocket server = null;
    final int port = 2121;
    public static Map<String, User> userCredentials = new HashMap<>();
    
    // Starting the server
    public void StartingServer(){
        
        try{
            server = new ServerSocket(port);
            System.out.println("Started Server on port: " + port);
        }
        catch(IOException e){
            System.out.println("Failed to start server: " + e.getMessage());
        }
        
    }
    
    // Waiting for Clients
    public void Listening(){
            Socket skt = null;
            
            while(true){
                try
                {
                    skt = server.accept();
                    ThreadingClass t = new ThreadingClass(skt);
                    t.start();
                }
                catch(IOException e)
                {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        
        
    }
    
    //Opening the credentials file
    public static void Credentials(){
        
        BufferedReader users = null;
        try{
            users = new BufferedReader(new FileReader("users.txt"));
            
            String line;
            int LineCount = 0;
            
            while((line = users.readLine()) != null ){
                LineCount++;
                String cleanLine = line.trim();
                
                if(cleanLine.isEmpty()) continue;
                
                String[] seperation = cleanLine.split(":", 3);
                
                if(seperation.length < 3){
                    System.out.println("The line: " + LineCount + " has a Wrong format.");
                    continue;
                }
                
                String username = seperation[0];
                String hashedPassword = seperation[1];
                String role = seperation[2].toLowerCase();
                
                User newUser = new User(username, hashedPassword, role);
                userCredentials.put(username, newUser);
            }
            
            users.close();
        }
        catch(FileNotFoundException e){
            System.out.println("Error: " + e.getMessage());
        }
        catch(IOException e){
            System.out.println("Error: " + e.getMessage());
        }
        finally{
            try{
                
                if (users != null){
                    users.close();
                }
            }
            catch(IOException e){
                System.out.println("Error with closing buffer: " + e.getMessage());
            }
        }
    }
    
    //Handling User data
    public static class User {
    
    private final String username;
    private final String hashedPassword;
    private final String role;

    private String sessionKey;
    private int failedAttempts; // 3 Attempts are allowed before account lockout 

    public User(String username, String hashedPassword, String role) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.role = role;
        
        this.sessionKey = null; // No key is set until the user is logged in (authenticated)
        this.failedAttempts = 0; 
    }

    public String getUsername() { return username; }
    public String getHashedPassword() { return hashedPassword; }
    public String getRole() { return role; }
    public String getSessionKey() { return sessionKey; }

    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
    public void incrementFailedAttempts() { this.failedAttempts++; }
    public void resetFailedAttempts() { this.failedAttempts = 0; }
    public int getFailedAttempts() { return failedAttempts; }

    public boolean isSuperUser() { return role.equalsIgnoreCase("super"); }
}
    public static synchronized void logEvent(String eventType, String username, String message) {
    
    String logFileName = "Logs.txt"; 
    
    String timestamp = LocalDateTime.now().toString();
    
    String logEntry = String.format("[%s] [%s] [%s] %s", timestamp, username, eventType, message);
    
    try (PrintWriter logger = new PrintWriter(new FileWriter(logFileName, true))) {
        logger.println(logEntry);
    } catch (IOException e) {
        System.err.println("CRITICAL LOGGING ERROR: Could not write to log file: " + e.getMessage());
    }
}
    
    public static void main(String[] args) {
        
        Credentials();

        Server s = new Server();
        
        s.StartingServer();
        s.Listening();
        
    }
}
