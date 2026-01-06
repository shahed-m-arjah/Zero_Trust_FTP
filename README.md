# Zero_Trust_FTP
A client-server file sharing application using TCP sockets, where the server is multi-threaded. The protocol is Zero-Trust towards the client. This means the server assumes every client request is unsafe. The system validates all client actions, enforces strict access controls, and is resilient to malformed or malicious inputs.

1- Group member names:
    SHAHED MOHAMMAD ATIAH ABO EL-ARJAH 0226884
    Hala Essam Yaseen Salmouna 2220498

2- Run Instructions:
Download the files inside the Codes folder and create a new project and place all these files in there also download the users.txt and Logs.txt files and place 
the Logs.txt and the users.txt files in the place where the Project would be able to access them when you run it.

First there are some file paths that need to be changed and checked:
    - In ThreadingClass.java, lines:
        232 : Change the path that points to the ServerFiles folder on your device, as this is the folder that contains User Directories and Files.
        267 : Change the path that points to the ServerFiles folder on your device.
        346 : Change the path that points to the ServerFiles folder on your device
        400 : Change the path that points to the ServerFiles folder on your device.

    - In Client.java:
        239 : Include the path of the Downloads directory in your device, this is where the folders downloaded from the server would be saved.

Next after you've saved all files in the correct places, you can start running the project by running the Server.java file first then the Client.java file.

Credentials to test with:
user1 : password  [Normal]
admin : PASSWORD  [Super]
test : test  [Normal]

- Implemented Features:
NOTE: The command format for all commands are handeled by the Client.java (Including sending the session key with relevant commands), in other words parsing the session key with commands is done by the Client.java, not the user for a better user experience.
    - Commands and how to try each: 
        1- when you log in you are only asked to enter the username and password. 
        2- Once logged in you can list your files: LIST
        3- If you are a super user you are allowed to list all files using: LISTALL
        4- If you want to download a file you can use: DOWNLOAD file_name
        5- If you want to upload a file to your directory on the server use: UPLOAD PATH_TO_THE_FILE_ON_YOUR_DEVICE
        6- Lastely logout, just type the command: LOGOUT
        
    - Features:
        1- Session key validation
        2- Role-based authorization
        3- File upload/download
        4- Listing available files
        5- LISTALL command: only for super user to view all folders and files
        5- Account lockout after 3 failed login attempts
        6- Logging system to log events
        7- Path traversal protection
        8- Passwords are hashed, never saved plain text

- Known limitations:
Traffic is sent in plaintext, no encryption implemented.

