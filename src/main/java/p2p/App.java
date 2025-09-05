package p2p;

import p2p.controller.FileShareController;

import java.io.IOException;

public class App {
    public static void main(String[] args) {
        try {
            FileShareController fileShareController = new FileShareController(8080);
            fileShareController.start();
            System.out.println("DropFlo server is running on port 8080");
            System.out.println("UI available at http://localhost:3000");

            Runtime.getRuntime().addShutdownHook(
                    new Thread(
                            () -> {
                                System.out.println("Shutting down DropFlo server...");
                                fileShareController.stop();
                                System.out.println("DropFlo server stopped.");
                            }
                    )
            );

        } catch (IOException e) {
            System.err.println("Failed to start server on port 8080: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
