package p2p.controller;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import p2p.service.FileSharer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
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

            if (httpExchange.getRequestMethod().equals("OPTIONS")) {
                httpExchange.sendResponseHeaders(200, -1);
                return;
            }

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
            }catch (Exception e){

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







            } catch (Exception e) {

            }
        }
    }

    public static class ParseResult {
        private final String fileName;
        private final byte[] fileData;

        public ParseResult(String fileName, byte[] fileData) {
            this.fileName = fileName;
            this.fileData = fileData;
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