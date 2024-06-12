import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer {
    private static final int PORT = 12345;
    private static ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private static UserRegisterImpl userRegister;
    private static HotelManager hotelManager;
    private static final String END_OF_RESPONSE = "END_OF_RESPONSE";
    private static final Map<Socket, String> loggedInUsers = new ConcurrentHashMap<>();
    private static final Map<String, Socket> userSockets = new ConcurrentHashMap<>();

    public static void start(UserRegisterImpl userRegister, HotelManager hotelManager) throws Exception {
        TCPServer.userRegister = userRegister;
        TCPServer.hotelManager = hotelManager;
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

                    switch (cmd) {
                        case "login":
                            if (args.length != 2) {
                                out.println("Invalid arguments format, usage: login([username],[password])");
                                out.println(END_OF_RESPONSE);
                                continue;
                            }
                            //username=args[0], password=args[1]
                            handleLogin(out, args[0], args[1]);
                            break;
                        case "logout":
                            if (args.length != 1) {
                                out.println("Invalid arguments format, usage: logout([username])");
                                out.println(END_OF_RESPONSE);
                                continue;
                            }
                            //username=args[0]
                            handleLogout(out, args[0]);
                            break;
                        case "searchHotel":
                            if (args.length != 2) {
                                out.println("Invalid arguments format, usage: searchHotel([hotelName],[cityName])");
                                out.println(END_OF_RESPONSE);
                                continue;
                            }
                            handleSearchHotel(out, args[0], args[1]);
                        case "searchAllHotels":
                            if (args.length != 1) {
                                out.println("Invalid arguments format, usage: searchAllHotels([cityName])");
                                out.println(END_OF_RESPONSE);
                                continue;
                            }
                            handleSearchAllHotels(out, args[0]);
                        case "insertReview":
                            if (args.length != 7) {
                                out.println("Invalid arguments format, usage: " +
                                        "insertReview([hotelName],[cityName],[globalScore]" +
                                        ",[positionScore],[cleaningScore],[serviceScore],[priceScore])");
                                out.println(END_OF_RESPONSE);
                                continue;
                            }
                            int globalScore = Integer.parseInt(args[2]);
                            int positionScore = Integer.parseInt(args[3]);
                            int cleaningScore = Integer.parseInt(args[4]);
                            int serviceScore = Integer.parseInt(args[5]);
                            int priceScore = Integer.parseInt(args[6]);
                            handleInsertReview(out, args[0], args[1], globalScore, positionScore, cleaningScore, serviceScore, priceScore);
                            break;
                        default:
                            out.println("Unknown command");
                            out.println(END_OF_RESPONSE);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void handleInsertReview(PrintWriter out, String hotelName, String city, int globalScore, int positionScore, int cleaningScore, int serviceScore, int priceScore) {
            if (!loggedInUsers.containsKey(clientSocket)) {
                out.println("User needs to be logged in to insert a review");
                out.println(END_OF_RESPONSE);
                return;
            }
            if (hotelManager.submitReview(hotelName, city, globalScore, positionScore, cleaningScore, serviceScore, priceScore)) {
                out.println("Review added successfully");
                out.println(END_OF_RESPONSE);
            } else {
                out.println("Hotel " + hotelName + " not found in " + city + "!");
                out.println(END_OF_RESPONSE);
            }
        }

        private void handleSearchAllHotels(PrintWriter out, String city) {
            List<Hotel> hotels = hotelManager.searchHotelsByCity(city);
            if (hotels.isEmpty()) {
                out.println("No hotel found");
                out.println(END_OF_RESPONSE);
                return;
            }
            int i = 1;
            for (Hotel hotel : hotels) {
                out.println("Local Rank " + i + "/" + hotels.size());
                out.println(hotel);
                i++;
            }
            out.println(END_OF_RESPONSE);
        }

        private void handleSearchHotel(PrintWriter out, String hotelName, String city) {
            Hotel hotel = hotelManager.searchHotelByNameAndCity(hotelName, city);
            if (hotel == null) {
                out.println("No hotel found");
                out.println(END_OF_RESPONSE);
                return;
            }
            out.println(hotel);
            out.println(END_OF_RESPONSE);
        }

        private void handleLogout(PrintWriter out, String username) {
            if (!userSockets.containsKey(username)) {
                out.println("User is not logged in");
                out.println(END_OF_RESPONSE);
                return;
            }
            if (!userSockets.get(username).equals(clientSocket)) {
                out.println("Socket not authenticated for this user");
                out.println(END_OF_RESPONSE);
                return;
            }
            String loggedUsername = loggedInUsers.remove(clientSocket);
            if (loggedUsername != null) {
                userSockets.remove(loggedUsername);
            }
            out.println("Logout successful!");
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
