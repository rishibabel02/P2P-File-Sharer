package p2p.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import p2p.utils.UploadUtils;

public class FileSharer {

    private HashMap<Integer, String> availableFiles;

    public FileSharer() {
        this.availableFiles = new HashMap<>();
    }

    public int offerFile(String filePath) {
        int port;

        while(true){
            port = UploadUtils.generateCode();
            if(!availableFiles.containsKey(port)){
                availableFiles.put(port, filePath);
                return port;
            }
        }
    }

    public void startFileServer(int port){
        String filePath = availableFiles.get(port);
        if(filePath == null){
            System.out.println("No file available for port: " + port);
        }

        try(ServerSocket serverSocket = new ServerSocket(port)){
            System.out.println("Serving file: " + filePath + " on port: " + port);
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connection: " + clientSocket.getInetAddress());

            new Thread(new FileSenderHandler(clientSocket, filePath)).start();

        } catch (Exception e) {
            throw new RuntimeException("Error starting file server on port: " + port);
        }
    }

    private static class FileSenderHandler implements Runnable {

        private final Socket clientSocket;
        private final String filePath;

        public FileSenderHandler(Socket clientSocket, String filePath) {
            this.clientSocket = clientSocket;
            this.filePath = filePath;
        }
        @Override
        public void run(){
            try(FileInputStream fis = new FileInputStream(filePath)){
                OutputStream oos = clientSocket.getOutputStream();
                String fileName = new File(filePath).getName();

                String header = "Filename: " + fileName + "\n" ;
                oos.write(header.getBytes());

                byte[] buffer = new byte[4096];
                int bytesRead;

                while((bytesRead = fis.read(buffer)) != -1){
                    oos.write(buffer, 0, bytesRead);
                }

                System.out.println("File"+ fileName + "sent successfully to: " + clientSocket.getInetAddress());

            }catch (Exception e){
                System.out.println("Error sending file: " + filePath);
                e.printStackTrace();
            }finally {
                try {
                    clientSocket.close();
                } catch (Exception e) {
                    System.out.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
    }
}
