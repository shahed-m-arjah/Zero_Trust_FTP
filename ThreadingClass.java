/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.trial_2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

/**
 *
 * @author C-ROAD
 */
public class ThreadingClass extends Thread{
    Socket skt = null;
    
    public ThreadingClass(Socket skt){
        this.skt = skt;
    }
    
    @Override
    public void run(){
        BufferedReader in = null;
        PrintWriter out = null;
        Server.User currentUser = null;
        
        try{
          in = new BufferedReader (new InputStreamReader(skt.getInputStream()));
          out = new PrintWriter(skt.getOutputStream(), true);
          
          out.println("220 Welcome to ZT-FTP Server. Login to get access.");
          
          while(true){
                String line = in.readLine();
            if (line == null) break;

            Server.User userAttempt = LOGIN(line, out);

            if (userAttempt != null) {
                // Logged in Successfuly
                Commands(userAttempt, out, in);
                break; 
            } else {
                String attemptedName = line.trim().split("\\s+")[1];
                Server.User u = Server.userCredentials.get(attemptedName);
                
                // Checking for Account lockout 
                if (u != null && u.getFailedAttempts() >= 3) {
                    out.println("500 CRITICAL: Account " + attemptedName + " is now locked. Disconnecting....");
                    break; // Kill the socket when reached max Tries.
                }
            }
          }
          
          in.close();
          out.close();
          skt.close();
        }
        catch(IOException e){
            System.out.println("Error: " + e.getMessage());
        }
        finally{
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (skt != null) skt.close();
            } catch (IOException e) {
                System.out.println("Error closing resources: " + e.getMessage());
            }
        }
    }
    
    private void Commands(Server.User currentUser, PrintWriter out, BufferedReader in){
        
        String line;
        
        while(true){
            try{
                line = in.readLine();
                if(line == null) break;
                
                String sections[] = line.trim().split("\\s+", 3);
                
                if(sections.length < 2){
                    out.println("Command invalide, must include the correct session KEY.");
                    out.println("END");
                    continue;
                }
                
                String recievedKey = sections[0];
                String Command = sections[1].toUpperCase();
                
                
                if(!recievedKey.equals(currentUser.getSessionKey())){
                    
                    if (sessionValidationFailed(currentUser, out)) { 
                        break; 
                    }
                    
                    out.println("Command invalide, must include the correct session KEY.");
                    out.println("END");
                    continue;
                }
                
                currentUser.resetFailedAttempts();
                
                //the commands
                if (FindCommandLogic(Command, currentUser, line, out, in))
                {
                    break;
                }
                
            }
            catch(IOException e){
                System.out.println("Error: " + e.getMessage());
                break;
            }
        }
    }
    
    // The function to increase the failedloginattempts if the session key is incorrect.
    private boolean sessionValidationFailed(Server.User currentUser, PrintWriter out) {
        
    currentUser.incrementFailedAttempts();
    int attempts = currentUser.getFailedAttempts();
    
    if (currentUser.getFailedAttempts() >= 3) {
        
        Server.logEvent("ZT_TERMINATED", currentUser.getUsername(), 
                         "Connection forcibly closed after 3 failed session key attempts.");
        
        out.println("500 CRITICAL ERROR: Too many session key failures. Disconnecting.");
        out.println("END");
        out.flush();
        return true;
    }
    
    Server.logEvent("ZT_FAILED", currentUser.getUsername(), 
                     "Invalid session key received. Strike " + attempts);
    return false;
}
    
    private boolean FindCommandLogic(String Command, Server.User currentUser, String line, PrintWriter out, BufferedReader in){
        
        switch(Command){
            case "LIST":
                LIST(currentUser, out);
                return false;
            case "LISTALL":
                LISTALL(currentUser, out); //this is only allowed for the "super" user -- admin
                return false;
            case "UPLOAD":
                UPLOAD(currentUser, line, out, in);
                return false;
            case "DOWNLOAD":
                DOWNLOAD(currentUser, line, out, in);
                return false;
            case "DELETE":
                DELETE(currentUser, line, out);
                return false;
            case "LOGOUT":
                LOGOUT(currentUser, out);
                return true;
            default:
                out.println("502 Error: Command not recognized.");
                out.println("END");
                return false;
                
                
        }
    }
    
    private Server.User LOGIN(String line, PrintWriter out){
        
        String []sections = line.trim().split("\\s+", 3);
        
        if(sections.length != 3 || !sections[0].equalsIgnoreCase("LOGIN")){
            out.println("Login command incorrect, the correct format: LOGIN <username> <password>");
            return null;
        }
        
        String username = sections[1];
        String password = sections[2];
        
        Server.User storedUser = Server.userCredentials.get(username);
        
        if(storedUser == null){
            out.println("ERROR: Invalid credentials.");
            return null;
        }
        
        if (storedUser.getFailedAttempts() >= 3) {
            out.println("500 ERROR: Account locked. Too many failed attempts.");
            Server.logEvent("LOGIN_LOCKED", username, "Attempt on already locked account.");
            return null;
        }
        
        String receivedHash = PassHasher.PassHasher(password); 
        
        if (receivedHash.equals(storedUser.getHashedPassword())) {
            String sessionKey = UUID.randomUUID().toString();
            storedUser.setSessionKey(sessionKey);
            storedUser.resetFailedAttempts(); 
        
            out.println("SUCCESS: " + sessionKey);
            Server.logEvent("LOGIN_SUCCESS", username, "Authentication successful. Key: " + sessionKey);
            
            return storedUser;
        }
        else{
            storedUser.incrementFailedAttempts(); 
            int strikes = storedUser.getFailedAttempts();
        
            out.println("ERROR: Invalid credentials.");
            Server.logEvent("LOGIN_FAILED", username, "Authentication failed. Invalid password.");
            
            return null;
        }
    }
    
    private void LIST(Server.User currentUser, PrintWriter out){
        
        String currentUsername = currentUser.getUsername();
        
        String path = "C:\\Users\\C-ROAD\\OneDrive\\Desktop\\NetProgProj\\ServerFiles\\" + currentUsername; 
        File targetDir = new File(path);

        if (!targetDir.exists() || !targetDir.isDirectory()) {
            out.println("404 ERROR: Directory not found for user: " + currentUsername);
            Server.logEvent("COMMAND_LIST_FAIL", currentUsername, "Directory not found for LIST: " + currentUsername);
            return;
        }

        String[] fileList = targetDir.list();
        
        if (fileList == null || fileList.length == 0) {
            out.println("Directory is empty.");
        } else {
            out.println("Files in " + currentUsername + "'s directory:");
            for (String fileName : fileList) {
                out.println(fileName);
            }
        }
        
        out.println("END");
        Server.logEvent("COMMAND_LIST_SUCCESS", currentUsername, "Successfully listed directory: " + currentUsername);
        
    
    }
    private void LISTALL(Server.User currentUser, PrintWriter out) {
    
    // Checking whether the user who is trying to use this command is a super user or not 
    if (!currentUser.getRole().equalsIgnoreCase("super")) {
        out.println("403 ERROR: Forbidden. You must be a 'super' user for this command.");
        out.println("END");
        Server.logEvent("COMMAND_LISTALL_AUTH_FAIL", currentUser.getUsername(), "Attempted unauthorized LISTALL.");
        return;
    }
    
    File rootDir = new File("C:\\Users\\C-ROAD\\OneDrive\\Desktop\\NetProgProj\\ServerFiles\\");
    
    if (!rootDir.exists() || !rootDir.isDirectory()) {
        out.println("404 ERROR: Server directory not found.");
        Server.logEvent("COMMAND_LISTALL_FAIL", currentUser.getUsername(), "Server root directory not found.");
        return;
    }

    String[] userFolders = rootDir.list();
    out.println("200 SUCCESS: Listing ALL user files on server:");
    
    if (userFolders != null) {
        for (String userDirName : userFolders) {
            File userDir = new File(rootDir, userDirName);
            
            if (userDir.isDirectory()) {
                out.println("--- Directory: " + userDirName + " ---");
                
                String[] files = userDir.list();
                if (files != null && files.length > 0) {
                    for (String fileName : files) {
                        out.println("  -> " + fileName);
                    }
                } else {
                    out.println("  (Empty)");
                }
            }
        }
    }
    
    out.println("END");
    
    Server.logEvent("COMMAND_LISTALL_SUCCESS", currentUser.getUsername(), "Successfully executed LISTALL.");
}
    
    private void DOWNLOAD(Server.User currentUser, String fullCommand, PrintWriter out, BufferedReader in) {
        
        String[] parts = fullCommand.trim().split("\\s+");
        if (parts.length < 3) {
            out.println("501 ERROR: Invalid command format.");
            Server.logEvent("COMMAND_DOWNLOAD_FAIL", currentUser.getUsername(), "Format error.");
            return;
        }
        String fileName = parts[2];
        String username = currentUser.getUsername();
        String userRole = currentUser.getRole();
        
        // Checking who the file belongs to.
        File targetFile = findTargetFile(username, userRole, fileName);

        if (targetFile == null) {
            out.println("404 ERROR: File not found or access denied.");
            out.println("END");
            Server.logEvent("COMMAND_DOWNLOAD_FAIL", username, "File not found or forbidden: " + fileName);
            return;
        }

        try (FileInputStream fileIn = new FileInputStream(targetFile)) {

            out.println("200 SUCCESS: DOWNLOAD " + fileName + " " + targetFile.length());
            out.flush();
            
            String clientSignal = in.readLine(); 
            if (clientSignal == null || !clientSignal.equals("START")) return;

            byte[] buffer = new byte[4096];
            int bytesRead;
            OutputStream socketOut = skt.getOutputStream();
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                socketOut.write(buffer, 0, bytesRead);
            }
            socketOut.flush();
            
            Server.logEvent("COMMAND_DOWNLOAD_SUCCESS", username, "Downloaded file: " + targetFile.getName());

        } catch (IOException e) {
            out.println("500 ERROR: Server error during file transfer.");
            out.println("END");
            out.flush();
            Server.logEvent("COMMAND_DOWNLOAD_FAIL", username, "Server I/O error: " + e.getMessage());
        }
}
   
    private File findTargetFile(String currentUsername, String userRole, String fileName) {
    String rootPath = "C:\\Users\\C-ROAD\\OneDrive\\Desktop\\NetProgProj\\ServerFiles\\";
    File serverRoot = new File(rootPath);

    try {
        // Super user 
        if (userRole.equalsIgnoreCase("super")) {
            File found = searchAllFolders(serverRoot, fileName);
            if (found != null) return found;
        }

        // Normal user
        File userFolder = new File(serverRoot, currentUsername);
        File requestedFile = new File(userFolder, fileName);

        // Path traversal Control
        if (requestedFile.getCanonicalPath().startsWith(userFolder.getCanonicalPath())) {
            if (requestedFile.exists() && !requestedFile.isDirectory()) {
                return requestedFile;
            }
        }
    } catch (IOException e) {
        return null;
    }
    return null;
}
    
    private File searchAllFolders(File directory, String fileName) {
    File[] files = directory.listFiles();
    if (files != null) {
        for (File file : files) {
            if (file.isDirectory()) {
                File found = searchAllFolders(file, fileName);
                if (found != null) return found;
            } else if (file.getName().equalsIgnoreCase(fileName)) {
                return file;
            }
        }
    }
    return null;
}
    
    private void UPLOAD(Server.User currentUser, String fullCommand, PrintWriter out, BufferedReader in) {
    try {
        String[] parts = fullCommand.trim().split("\\s+");
        if (parts.length < 5) {
            out.println("501 ERROR: Incorrect command format, try including file name.");
            out.println("END");
            return;
        }

        String targetUser = parts[2];
        String fileName = parts[3];
        long fileSize = Long.parseLong(parts[4]);

        String rootPath = "C:\\Users\\C-ROAD\\OneDrive\\Desktop\\NetProgProj\\ServerFiles\\";
        File targetDir = new File(rootPath, targetUser);
        if (!targetDir.exists()) targetDir.mkdirs();

        File outputFile = new File(targetDir, fileName);

        out.println("200 READY: Send file now");
        out.flush();

        InputStream socketIn = skt.getInputStream();
        FileOutputStream fos = new FileOutputStream(outputFile);

        byte[] buffer = new byte[4096];
        long totalRead = 0;
        int bytesRead;

        while (totalRead < fileSize) {
            bytesRead = socketIn.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalRead));
            if (bytesRead == -1) break; // client disconnected
            fos.write(buffer, 0, bytesRead);
            totalRead += bytesRead;
        }
        fos.close();

        out.println("200 SUCCESS: File uploaded (" + totalRead + " bytes)");
        out.println("END");

        Server.logEvent("COMMAND_UPLOAD_SUCCESS", currentUser.getUsername(),
                "Uploaded " + fileName + " (" + totalRead + " bytes) to " + targetUser);

    } catch (IOException e) {
        out.println("500 ERROR: Upload failed: " + e.getMessage());
        out.println("END");
    }
}

    
private void DELETE(Server.User currentUser, String fullCommand, PrintWriter out) {
    
    String[] parts = fullCommand.trim().split("\\s+");
    if (parts.length < 3) {
        out.println("501 ERROR: Invalid command format. Usage: [Key] DELETE <filename>");
        Server.logEvent("COMMAND_DELETE_FAIL", currentUser.getUsername(), "Format error.");
        return;
    }
    String fileName = parts[2];
    String username = currentUser.getUsername();
    String userRole = currentUser.getRole();
    
    File targetFile = findTargetFile(username, userRole, fileName);

    if (targetFile == null) {
        out.println("404 ERROR: File not found or access denied.");
        out.println("END");

        Server.logEvent("COMMAND_DELETE_FAIL", username, "File not found or forbidden: " + fileName);
        return;
    }

    if (targetFile.delete()) {
        out.println("200 SUCCESS: File " + fileName + " deleted successfully.");
        Server.logEvent("COMMAND_DELETE_SUCCESS", username, "Successfully deleted file: " + targetFile.getAbsolutePath());
    } else {
        out.println("500 ERROR: Could not delete the file. It may be in use.");
        Server.logEvent("COMMAND_DELETE_FAIL", username, "Failed to delete file: " + targetFile.getAbsolutePath());
    }
    
    out.println("END");
}

private void LOGOUT(Server.User currentUser, PrintWriter out) {
    String username = currentUser.getUsername();
    
    currentUser.setSessionKey(null);
    currentUser.resetFailedAttempts();
    
    Server.logEvent("SESSION_LOGOUT", username, "User logged out successfully. Connection closing.");
    
    out.println("200 SUCCESS: Session terminated. Goodbye.");
    out.println("END");
}

}
