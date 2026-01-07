/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.trial_2;


/**
 *
 * @author C-ROAD
 */

import java.net.Socket;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Scanner;

public class Client {

    private Socket skt = null;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner keyboard;

    private String sessionKey;
    private String currentUser;
    
    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }

    public void start() {
        connectToServer();
        login();
        commandLoop();
        closeConnection();
    }

    private void connectToServer() {
        
        try{
            skt = new Socket("localhost", 2121);
            in = new BufferedReader( new InputStreamReader(skt.getInputStream()));
            out = new PrintWriter(skt.getOutputStream());
            keyboard = new Scanner(System.in);
        }
        catch(IOException e){
            System.out.println("Error: " + e.getMessage());
        }
    }


    private void login() {
        
        try{
            String line = in.readLine(); // Welcome line recieved form the server
            System.out.println(line);
            boolean LoggedInStatus = false;
            
            while(!LoggedInStatus ){
                System.out.println("Enter username: ");
                String username = keyboard.nextLine();

                System.out.println("Enter password: ");
                String password = keyboard.nextLine();

                String LoginMessage = "LOGIN " + username + " " + password;
                out.println(LoginMessage);
                out.flush();

                String LoginResponse = in.readLine(); 
                
                if (LoginResponse == null) {
                    System.out.println("The Server has terminated the connection (Account Locked).");
                    System.exit(0); // Close the client so it doesn't try to send more commands
                }
                
                if(LoginResponse.contains("SUCCESS:"))
                {
                    System.out.println(LoginResponse);
                    String[] parts = LoginResponse.split(" ");
                    sessionKey = parts[1];
                    currentUser = username;
                    LoggedInStatus = true;
                }
                else  if (LoginResponse.contains("500 ERROR")) {
                    System.out.println(LoginResponse);
                    System.exit(0); // Exit immediately when account is locked out
                }
                else if (LoginResponse.contains("500 CRITICAL")) {
                    System.out.println(LoginResponse);
                    System.exit(0); //  Exit immediately when account is locked out
                }
                else{
                        System.out.println(LoginResponse);
                }
            }
            
            if(!LoggedInStatus){
                System.out.println("Too many failed login tries.");
                skt.close();
            }
            
        }
        catch(IOException e){
        System.out.println("Error " + e.getMessage());
    }
    }

    private void commandLoop() {

        try{
            while(true){
                System.out.println("Enter command:");
            String Command = keyboard.nextLine();
            String commandType = Command.split(" ")[0].toUpperCase();
            if(commandType.equalsIgnoreCase("LOGOUT")){
                out.println(sessionKey + " " + Command);
                out.flush();
                skt.close();
                break;
            }
            else if(commandType.startsWith("UPLOAD")) {
                uploadFile(Command, currentUser); 
            } 
            else if(commandType.startsWith("DOWNLOAD")) {
                downloadFile(Command); 
            } 
            else if (Command.equalsIgnoreCase("WRONGSK")) {
                String fakeKey = "FAKE-key-for-demo"; 
                System.out.println("Sending intercepted packet with fake key...");
                out.println(fakeKey + " LIST"); //Sending a valid command with an invalid key.
                out.flush();
                readResponse();
            }
            else{
                out.println(sessionKey + " " + Command);
                out.flush();
                
                readResponse();
                
                
            }}
            
        }
        catch(IOException e){
            System.out.println("Error: " + e.getMessage());
        }

    }
 
    private void readResponse() throws IOException {
    String line;
    while ((line = in.readLine()) != null) {
        if (line.equalsIgnoreCase("END")) break;
        System.out.println(line);
    }
}

    private void uploadFile(String command, String username) {
    try {
        String parts[] = command.split(" ", 2);
        if (parts.length < 2) {
            System.out.println("Usage: upload <filepath>");
            return;
        }

        String filePath = parts[1];
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            System.out.println("File not found: " + filePath);
            return;
        }

        String fileName = file.getName();
        long fileSize = file.length();

        // This is the command sent to the server
        String fullCommand = sessionKey + " UPLOAD " + username + " " + file.getName() + " " + fileSize;

        out.println(fullCommand);
        out.flush();

        String response = in.readLine();
        if (!response.startsWith("200 READY")) {
            System.out.println("Server rejected upload: " + response);
            return;
        }

        FileInputStream fis = new FileInputStream(file);
        OutputStream socketOut = skt.getOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        long totalSent = 0;

        while ((bytesRead = fis.read(buffer)) != -1) {
            socketOut.write(buffer, 0, bytesRead);
            totalSent += bytesRead;
        }
        socketOut.flush();
        fis.close();

        System.out.println("Uploaded " + totalSent + " bytes.");

        readResponse();

    } catch (IOException e) {
        System.out.println("Upload error: " + e.getMessage());
    }
}

private void downloadFile(String Command) {
    try {
        String parts[] = Command.split(" ");
        
        if (parts.length < 2) {
            System.out.println("Incomplete command error: You must type 'download <filename>'");
            return; 
        }
        
        String fileName = parts[1];

        // This is the command sent to the server
        out.println(sessionKey + " DOWNLOAD " + fileName);
        out.flush();

        String response = in.readLine();
        if (response == null || !response.startsWith("200 SUCCESS")) {
            System.out.println("Download failed: " + response);
            readResponse(); 
            return;
        }

        // Command recieved from server:  "200 SUCCESS: DOWNLOAD file_name file_size"
        long fileSize = Long.parseLong(response.split(" ")[4]);
        
        out.println("START");
        out.flush();
        String downloadDir = "C:\\Users\\C-ROAD\\Downloads\\";
        File localFile = new File(downloadDir + fileName);
        FileOutputStream fos = new FileOutputStream(localFile);

        InputStream socketIn = skt.getInputStream();
        byte[] buffer = new byte[4096];
        long totalRead = 0;
        int bytesRead;

        while (totalRead < fileSize) {
            bytesRead = socketIn.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalRead));
            if (bytesRead == -1) break; 
            fos.write(buffer, 0, bytesRead);
            totalRead += bytesRead;
        }
        fos.close();
        System.out.println("Downloaded " + totalRead + " bytes.");


        System.out.println("File downloaded: " + fileName);

    } catch (IOException e) {
        System.out.println("Download error: " + e.getMessage());
    }
}

    private void closeConnection() {
        
        try{
            in.close();
            out.close();
            skt.close();
            keyboard.close();
        }
        catch(IOException e){
            System.out.println("Error " + e.getMessage());
        }
        
    }
}

