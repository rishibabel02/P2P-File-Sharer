package p2p.controller;

import com.sun.net.httpserver.HttpServer;
import p2p.service.FileSharer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

public class FileShareController {

    private final FileSharer fileSharer;
    private final HttpServer httpServer;
    private final String uploadDir;
    private final ExecutorService executorService;

    public FileShareController (int port) thwows IOException {
        this.fileSharer = new FileSharer();
        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
    }

}
