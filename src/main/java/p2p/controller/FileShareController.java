package p2p.controller;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import p2p.service.FileSharer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.io.IOUtils;

public class FileShareController {

    private final FileSharer fileSharer;
    private final HttpServer httpServer;
    private final String uploadDir;
    private final ExecutorService executorService;

    public FileShareController(int port) throws IOException {
        this.fileSharer = new FileSharer();
        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        this.uploadDir = System.getProperty("java.io.tmpdir") + File.separator + "DropFlo-uploads";
        this.executorService = Executors.newFixedThreadPool(10);

        File uploadDirectory = new File(uploadDir);
        if (!uploadDirectory.exists()) {
            uploadDirectory.mkdirs();
        }

        httpServer.createContext("/upload", new UploadHandler());
        httpServer.createContext("/download", new DownloadHandler());
        httpServer.createContext("/", new CORSHandler());
        httpServer.setExecutor(executorService);
    }

    public void start() {
        httpServer.start();
        System.out.println("API Server started on port: " + httpServer.getAddress().getPort());
    }


    public void stop() {
        httpServer.stop(0);
        executorService.shutdown();
        System.out.println("API Server stopped.");
    }

    private class CORSHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            Headers headers = httpExchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            
            String res = "NOT FOUND";
            httpExchange.sendResponseHeaders(404, res.getBytes().length);

            try (OutputStream oos = httpExchange.getResponseBody()) {
                oos.write(res.getBytes());
            }
        }
    }

    private class UploadHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            Headers headers = httpExchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");

            if (!httpExchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String res = "Method Not Allowed";
                httpExchange.sendResponseHeaders(405, res.getBytes().length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(res.getBytes());
                }
                return;
            }

            Headers RequestHeaders = httpExchange.getRequestHeaders();
            String contentType = RequestHeaders.getFirst("Content-Type");

            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                String res = "Invalid Content-Type: It must be multipart/form-data";
                httpExchange.sendResponseHeaders(400, res.getBytes().length);

                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(res.getBytes());
                }
                return;
            }

            try{
                String boundary = contentType.substring(contentType.indexOf("boundary=") + 9);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                IOUtils.copy(httpExchange.getRequestBody(), baos);
                byte[] requestData = baos.toByteArray();

                MutliParser parser = new MutliParser(requestData, boundary);
                MutliParser.ParseResult result = parser.parse();

                if(result == null){
                    String response = "Bad Req: Could not parse file content.";
                    httpExchange.sendResponseHeaders(400, response.getBytes().length);
                    try (OutputStream os = httpExchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                    return;
                }

                String fileName = result.fileName;
                if(fileName == null || fileName.trim().isEmpty()){
                    fileName = "unnamed-file";
                }

                String uniqueFileName = UUID.randomUUID().toString() + "_" + new File(fileName).getName();
                String filePath = uploadDir + File.separator + uniqueFileName;

                try(FileOutputStream fos = new FileOutputStream(filePath)){
                    fos.write(result.fileContent);
                }

                int port = fileSharer.offerFile(filePath);
                new Thread(() -> fileSharer.startFileServer(port)).start();

                String jsonResponse = "{\"port\":" + port + "}";
                headers.add("Content-Type", "application/json");
                httpExchange.sendResponseHeaders(200, jsonResponse.getBytes().length);

                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes());
                }
            }catch (Exception e){
                System.err.println("Error handling file upload: " + e.getMessage());
                String response = "Internal Server Error: " + e.getMessage();
                httpExchange.sendResponseHeaders(500, response.getBytes().length);

                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }

    private static class MutliParser {
        private final byte[] data;
        private final String boundary;

        public MutliParser(byte[] data, String boundary) {
            this.data = data;
            this.boundary = boundary;
        }

        public ParseResult parse(){
            try{
                String dataAsString = new String(data);
                String fileNameMarker = "filename=\"";

                int fileNameStart = dataAsString.indexOf(fileNameMarker);
                if (fileNameStart == -1) return null;

                fileNameStart += fileNameMarker.length();
                int fileNameEnd = dataAsString.indexOf("\"", fileNameStart);
                String fileName = dataAsString.substring(fileNameStart ,fileNameEnd);

                String contentTypeMarker = "Content-Type: ";
                int contentTypeStart = dataAsString.indexOf(contentTypeMarker, fileNameEnd);
                String contentType = "application/octet-stream";

                if (contentTypeStart != -1) {
                    contentTypeStart += contentTypeMarker.length();
                    int contentTypeEnd = dataAsString.indexOf("\r\n", contentTypeStart);
                    contentType = dataAsString.substring(contentTypeStart, contentTypeEnd);
                }

                String headerEndMarker = "\r\n\r\n";
                int headerEnd = dataAsString.indexOf(headerEndMarker);
                if (headerEnd == -1) {
                    return null;
                }

                int contentStart = headerEnd + headerEndMarker.length();
                byte[] boundaryBytes = ("\r\n--" + boundary + "--").getBytes();

                int contentEnd = findSequence(data, boundaryBytes, contentStart);

                if(contentEnd == -1){
                    boundaryBytes = ("\r\n--" + boundary).getBytes();
                    contentEnd = findSequence(data, boundaryBytes, contentStart);
                }

                if (contentEnd == -1 || contentEnd <= contentStart) {
                    return null;
                }

                byte[] fileContent= new byte[contentEnd - contentStart];
                System.arraycopy(data, contentStart,fileContent, 0, fileContent.length);
                return new ParseResult(fileName, fileContent, contentType);
            } catch (Exception e) {
                System.out.println("Error parsing multipart data: " + e.getMessage());
                return null;
            }
        }


        public static class ParseResult {
            public final String fileName;
            public final byte[] fileContent;
            public final String contentType;


            public ParseResult(String fileName, byte[] fileContent, String contentType) {
                this.fileName = fileName;
                this.fileContent = fileContent;
                this.contentType = contentType;
            }
        }

        private static int findSequence(byte[] data, byte[] sequence, int startPos) {
            outer:
            for(int i=startPos; i<= data.length - sequence.length; i++) {
                for(int j=0; j<sequence.length; j++) {
                    if(data[i + j] != sequence[j]) {
                        continue outer;
                    }
                }
                return i;
            }
            return -1;
        }

    }

private class DownloadHandler implements HttpHandler{

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            Headers headers = httpExchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");

            if (httpExchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                httpExchange.sendResponseHeaders(204, -1);
                return;
            }
            if(!httpExchange.getRequestMethod().equalsIgnoreCase("GET")){
                String res = "Method not allowed!";
                httpExchange.sendResponseHeaders(405, res.getBytes().length);

                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(res.getBytes());
                }
                return;
            }

            String path = httpExchange.getRequestURI().getPath();
            String portStr = path.substring(path.lastIndexOf("/") + 1);

            try{
                int port = Integer.parseInt(portStr);
                try(Socket socket = new Socket("localhost", port)){
                    InputStream socketInput = socket.getInputStream();
                    File tempFile = File.createTempFile("download_", ".tmp");
                    String fileName = "downloaded-file";

                    try(FileOutputStream fos = new FileOutputStream(tempFile)){
                        byte[] buffer = new byte[4096];
                        int byteRead;
                        ByteArrayOutputStream headerbaos = new ByteArrayOutputStream();
                        int b;

                        while((b = socketInput.read()) != -1){
                           if(b == '\n') break;
                           headerbaos.write(b);
                        }

                        String header = headerbaos.toString().trim();
                        if(header.startsWith("Filename:")){
                           fileName = header.substring("Filename: ".length());
                        }

                        while((byteRead = socketInput.read(buffer)) != -1){
                            fos.write(buffer, 0, byteRead);
                        }
                    }

                    headers.add("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                    headers.add("Content-Type", "application/octet-stream");
                    httpExchange.sendResponseHeaders(200, tempFile.length());

                    try (OutputStream os = httpExchange.getResponseBody()) {
                        FileInputStream fis = new FileInputStream(tempFile);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }
                    tempFile.delete();
                }
            } catch (Exception e) {
                System.out.println("Not able to download file: " + e.getMessage());
                String response = "Error downloading file: " + e.getMessage();
                headers.add("Content-Type", "text/plain");
                httpExchange.sendResponseHeaders(400, response.getBytes().length);

                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
}

}