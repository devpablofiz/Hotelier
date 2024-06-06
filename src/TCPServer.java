import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer {
    private static final int PORT = 12345;
    private static ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private static UserRegisterImpl userRegister;
    private static final String END_OF_RESPONSE = "END_OF_RESPONSE";
    private static Map<Socket, String> loggedInUsers = new ConcurrentHashMap<>();
    private static Map<String, Socket> userSockets = new ConcurrentHashMap<>();

    public static void start(UserRegisterImpl userRegister) throws Exception {
        TCPServer.userRegister = userRegister;
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("TCP Server is ready on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            threadPool.execute(new RequestHandler(clientSocket));
        }
    }

    static class RequestHandler implements Runnable {
        private Socket clientSocket;

        public RequestHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String command;
                while ((command = in.readLine()) != null) {
                    String[] parts = command.split("\\(", 2);
                    if (parts.length != 2) {
                        out.println("Invalid command format");
                        out.println(END_OF_RESPONSE);
                        continue;
                    }

                    String cmd = parts[0];
                    String[] args = parts[1].replace(")", "").split(",");
                    if (args.length != 2) {
                        out.println("Invalid arguments format");
                        out.println(END_OF_RESPONSE);
                        continue;
                    }

                    String username = args[0];
                    String password = args[1];

                    switch (cmd) {
                        case "login":
                            handleLogin(out, username, password);
                            break;
                        case "logout":
                            handleLogout(out,username);
                        case "other":
                            handleOther(out, username, password);
                            break;
                        default:
                            out.println("Unknown command");
                            out.println(END_OF_RESPONSE);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                String username = loggedInUsers.remove(clientSocket);
                if (username != null) {
                    userSockets.remove(username);
                }
                try {
                    clientSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleLogout(PrintWriter out, String username) {
            if(!userSockets.containsKey(username)){
                out.println("User is not logged in");
                out.println(END_OF_RESPONSE);
                return;
            }
            if(!userSockets.get(username).equals(clientSocket)){
                out.println("Socket not authenticated for this user");
                out.println(END_OF_RESPONSE);
                return;
            }
            String loggedUsername = loggedInUsers.remove(clientSocket);
            if (loggedUsername != null) {
                userSockets.remove(loggedUsername);
            }
            out.println("Logout successful");
            out.println(END_OF_RESPONSE);
        }

        private void handleLogin(PrintWriter out, String username, String password) {
            if (userSockets.containsKey(username)) {
                out.println("User already logged in");
            } else {
                String result = userRegister.validateUser(username, password);
                if ("Login successful!".equals(result)) {
                    loggedInUsers.put(clientSocket, username);
                    userSockets.put(username, clientSocket);
                }
                out.println(result);
            }
            out.println(END_OF_RESPONSE);
        }

        private void handleOther(PrintWriter out, String username, String password) {
            if (loggedInUsers.containsKey(clientSocket) && loggedInUsers.get(clientSocket).equals(username)) {
                // Process other command
                out.println("Processing other command for " + username);
            } else {
                out.println("Unauthorized");
            }
            out.println(END_OF_RESPONSE);
        }
    }
}
