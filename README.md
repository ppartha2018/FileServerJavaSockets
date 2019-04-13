# FileServerJavaSockets
End to end file transfer implementation using Java Sockets.
Notes:
1. The root dir for server where it stores its files is its current working directory.
2. Multiple clients can connect to the server for uploads, downloads and other commands simultaneously without blocking. Implemented with multi-threading.

*** Instructions and Expected Output (prefixed by -> for clarity): ***
1. Compile the files
2. create the jar
  jar cvfe pa1.jar Main *.class
3. Run:
---Start the Server:-----------------------------------------------------------------------------------

java -jar pa1.jar server start 9999
-> Server running on port: 9999

---Start the Client:  -----------------------------------------------------------------------------------
(In a different command window or put server to background by appending &)
<set environment variable PA1_SERVER>
example: set PA1_SERVER=localhost:9999 (on windows) 
or... example: set PA1_SERVER=192.168.1.1:9999
If the PA1_SERVER is not set, the client defaults to localhost:9999

---Upload:---------------------------------------------------------------------------------------------

java -jar pa1.jar client upload ./files/upload.txt ./server/upload.txt
-> Uploading new file...
-> 60% (changes in real time)
-> 100%
-> File Upload successful!

---Resume Partial Upload: (due to any issues related to network, client or server crash:----------------
java -jar pa1.jar client upload ./files/upload.txt ./server/upload.txt
-> Resuming upload...
-> 80% (changes in real time)
-> 100%
-> File Upload successful!

(check the presence of uploaded files by listdir command below)
java -jar pa1.jar client dir ./server/
-> upload.txt

---Download:-------------------------------------------------------------------------------------------

java -jar pa1.jar client download ./server/upload.txt
-> Downloading the file...
-> 60% (changes in real time)
-> 100%
-> File Download successful!

---Download: (File not present in server:--------------------------------------------------------------
java -jar pa1.jar client download ./server/upload-not-present.txt
-> The requested file does not exist in server.

---Resume Partial Download: (due to any issues related to network, client or server crash:--------------
java -jar pa1.jar client download ./server/upload.txt
-> Resuming download...
-> 80% (changes in real time)
-> 100%
-> File Download successful!

(check the presence of downloaded files by listdir command below)
dir
-> upload.txt	pa1.jar

---MKDIR:----------------------------------------------------------------------------------------------
java -jar pa1.jar client mkdir hello
-> Directory hello created in server.

java -jar pa1.jar client mkdir hello
ERR501: Given directory already exist in server.

---LISTDIR:--------------------------------------------------------------------------------------------
java -jar pa1.jar client dir .
-> hello	server		pa1.jar		upload.txt

---RMDIR:----------------------------------------------------------------------------------------------

java -jar pa1.jar client rmdir hello
-> Directory/File 'hello' deleted in server.

java -jar pa1.jar client dir .
->server		pa1.jar		upload.txt

java -jar pa1.jar client rm dir-not-present
-> ERR502: Given directory does not exist in server.

---RM:------------------------------------------------------------------------------------------------

java -jar pa1.jar client rm upload.txt
-> Directory/File 'upload.txt' deleted in server.
java -jar pa1.jar client dir .
->server		pa1.jar

java -jar pa1.jar client rm upload.txt
-> ERR502: Given file does not exist in server.

---SHUTDOWN::--------------------------------------------------------------------------------------------
java -jar pa1.jar client shutdown
-> Server shutdown successful!

Note:
The server would create some part-uploads /part-downloads serialized files to keep track of partial uploads or downloads.
