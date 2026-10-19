package org.chatapp.server.socket;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.chatapp.server.security.CustomUserDetailsService;
import org.chatapp.server.security.JwtUtil;
import org.chatapp.server.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ChatServer {

    private ServerSocket serverSocket;
    private final ConnectionManager connectionManager;
    private final MessageRouter messageRouter;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final ExecutorService clientExecutor =
            new ThreadPoolExecutor(
                    10,
                    200,
                    60L,
                    TimeUnit.SECONDS,
                    new SynchronousQueue<>(),
                    new ThreadPoolExecutor.AbortPolicy()
            );

    @Value("${server.socket.port}")
    private int port;

    @Value("${server.socket.tls.key-store}")
    private String keystorePath;

    @Value("${server.socket.tls.key-store-password}")
    private String keystorePassword;

    private volatile boolean running = true;
    private Thread socketThread;

    // Constructor with port and initializing socket and client list
    public ChatServer(ConnectionManager connectionManager, MessageRouter messageRouter, UserService userService, JwtUtil jwtUtil, CustomUserDetailsService userDetailsService){
        this.connectionManager = connectionManager;
        this.messageRouter = messageRouter;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @PostConstruct
    public void init() {
        socketThread = new Thread(this::start, "server-socket");
        socketThread.setDaemon(true);
        socketThread.start();
        log.info("SOCKET SERVER > Chat socket server thread started.");
    }

    public void start(){
        try {
//            serverSocket = new ServerSocket(port);
            SSLContext sslContext = ServerTls.createContext(
                    Path.of(keystorePath),
                    keystorePassword.toCharArray()
            );

            SSLServerSocket sslServerSocket =
                    (SSLServerSocket) sslContext
                            .getServerSocketFactory()
                            .createServerSocket(port);

            sslServerSocket.setEnabledProtocols(
                    new String[]{"TLSv1.3", "TLSv1.2"}
            );

            serverSocket = sslServerSocket;
            log.info("SOCKET SERVER > Server started");
            log.info("SOCKET SERVER > Waiting for client...");

            while(running && !serverSocket.isClosed()){
                try{
                    // Accept socket connection and print out information about the remote socket
                    Socket clientSocket = serverSocket.accept();
                    log.info("SOCKET SERVER > New connection: {}", clientSocket.getRemoteSocketAddress());

                    // Perform TLS handshake
                    if (clientSocket instanceof SSLSocket sslSocket) {
                        sslSocket.setUseClientMode(false);
                        sslSocket.startHandshake();
                    }

                    // Create a client handler thread that handles the reading input from each new connection
                    ClientHandler client = new ClientHandler(clientSocket, connectionManager, messageRouter, userService, jwtUtil, userDetailsService);
                    clientExecutor.execute(client);
                    connectionManager.addClient(client);

                } catch (IOException e){
                    if(!running) {
                        log.info("SOCKET SERVER > Stopped accepting new connections.");
                        break;
                    }
                    log.warn("SOCKET SERVER > Accept failed: {}", e.getMessage());
                }
            }
        } catch (IOException e){
            log.error("SOCKET SERVER > Socket error: {}", e.getMessage());

        } catch (Exception e) {
            log.error("SOCKET SERVER > Error creating SSL socket: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("SOCKET SERVER > Shutting down socket server...");
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // unblocks accept()
            }
        } catch (IOException e) {
            log.info("SOCKET SERVER > Error closing server socket: {}", e.getMessage());
        }

        if (socketThread != null && socketThread.isAlive()) {
            try {
                socketThread.join(1000); // wait up to 1 second for it to stop
            } catch (InterruptedException ignored) {
            }
        }

        log.info("SOCKET SERVER > Socket server stopped cleanly.");
    }
}
