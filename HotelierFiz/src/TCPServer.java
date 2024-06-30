import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TCPServer {
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private static UserRegisterImpl userRegister;
    private static HotelManager hotelManager;
    private static final String END_OF_RESPONSE = "END_OF_RESPONSE";
    private static final Map<Socket, String> loggedInUsers = new HashMap<>(); //socket -> username
    private static final Map<String, Socket> userSockets = new HashMap<>(); //username -> socket
    private static final ReentrantReadWriteLock loggedInUsersLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock userSocketsLock = new ReentrantReadWriteLock();

    public static void start(UserRegisterImpl userRegister, HotelManager hotelManager, int tcpPort) throws Exception {
        TCPServer.userRegister = userRegister;
        TCPServer.hotelManager = hotelManager;

        ServerSocket serverSocket = new ServerSocket(tcpPort);
        System.out.println("TCP Server is ready on port " + tcpPort);

        //accept new connections and pass socket to handler
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

                    //parse client message for command args
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
                            handleLogin(out, args[0], args[1]);
                            break;
                        case "logout":
                            if (args.length != 1) {
                                out.println("Invalid arguments format, usage: logout([username])");
                                out.println(END_OF_RESPONSE);
                                continue;
                            }
                            handleLogout(out, args[0]);
                            break;
                        case "searchHotel":
                            if (args.length != 2) {
                                out.println("Invalid arguments format, usage: searchHotel([hotelName],[cityName])");
                                out.println(END_OF_RESPONSE);
                                continue;
                            }
                            handleSearchHotel(out, args[0], args[1]);
                            break;
                        case "searchAllHotels":
                            if (args.length != 1) {
                                out.println("Invalid arguments format, usage: searchAllHotels([cityName])");
                                out.println(END_OF_RESPONSE);
                                continue;
                            }
                            handleSearchAllHotels(out, args[0]);
                            break;
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
                            if (!isScoreValid(globalScore, positionScore, cleaningScore, serviceScore, priceScore, out)) {
                                continue;
                            }
                            handleInsertReview(out, args[0], args[1], globalScore, positionScore, cleaningScore, serviceScore, priceScore);
                            break;
                        case "showMyBadges":
                            if (args.length != 1 || !args[0].isEmpty()) {
                                out.println("Invalid arguments format, usage: showMyBadges()");
                                out.println(END_OF_RESPONSE);
                                continue;
                            }
                            handleShowMyBadges(out);
                            break;
                        default:
                            out.println("Unknown command");
                            out.println(END_OF_RESPONSE);
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                //once session is done
                cleanup();
            }
        }

        private boolean isScoreValid(int globalScore, int positionScore, int cleaningScore, int serviceScore, int priceScore, PrintWriter out) {
            if (globalScore < 1 || globalScore > 5 || positionScore < 1 || positionScore > 5 ||
                    cleaningScore < 1 || cleaningScore > 5 || serviceScore < 1 ||
                    serviceScore > 5 || priceScore < 1 || priceScore > 5) {
                out.println("Invalid arguments format, scores must be between 1 and 5");
                out.println(END_OF_RESPONSE);
                return false;
            }
            return true;
        }

        private void cleanup() {
            loggedInUsersLock.writeLock().lock();
            userSocketsLock.writeLock().lock();
            try {
                //close socket, remove from user / socket maps
                clientSocket.close();
                String username = loggedInUsers.remove(clientSocket);
                if (username != null) {
                    userSockets.remove(username);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                userSocketsLock.writeLock().unlock();
                loggedInUsersLock.writeLock().unlock();
            }
        }

        private void handleShowMyBadges(PrintWriter out) {
            loggedInUsersLock.readLock().lock();
            try {
                String username = loggedInUsers.get(clientSocket);
                if (username == null) {
                    out.println("User needs to be logged in to request badges");
                    out.println(END_OF_RESPONSE);
                    return;
                }
                String badge = userRegister.getUser(username).getBadge();
                if (badge == null) {
                    out.println("Submit at least one review to start collecting badges");
                    out.println(END_OF_RESPONSE);
                    return;
                }
                out.println(badge);
                out.println(END_OF_RESPONSE);
            } finally {
                loggedInUsersLock.readLock().unlock();
            }
        }

        private void handleInsertReview(PrintWriter out, String hotelName, String city, int globalScore,
                                        int positionScore, int cleaningScore, int serviceScore, int priceScore) {
            loggedInUsersLock.readLock().lock();
            try {
                String username = loggedInUsers.get(clientSocket);
                if (username == null) {
                    out.println("User needs to be logged in to insert a review");
                    out.println(END_OF_RESPONSE);
                    return;
                }
                if (hotelManager.submitReview(hotelName, city, globalScore, positionScore, cleaningScore, serviceScore, priceScore)) {
                    userRegister.getUser(username).incrementReviewCounter();
                    out.println("Review added successfully");
                } else {
                    out.println("Hotel " + hotelName + " not found in " + city + "!");
                }
                out.println(END_OF_RESPONSE);
            } finally {
                loggedInUsersLock.readLock().unlock();
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
            userSocketsLock.writeLock().lock();
            loggedInUsersLock.writeLock().lock();
            try {
                //check if user logged in
                if (!userSockets.containsKey(username)) {
                    out.println("User is not logged in");
                    out.println(END_OF_RESPONSE);
                    return;
                }
                //check if that user has logged in with this socket
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
            } finally {
                loggedInUsersLock.writeLock().unlock();
                userSocketsLock.writeLock().unlock();
            }
        }

        private void handleLogin(PrintWriter out, String username, String password) {
            userSocketsLock.writeLock().lock();
            loggedInUsersLock.writeLock().lock();
            try {
                if (userSockets.containsKey(username)) {
                    out.println("User already logged in");
                } else {
                    String result = userRegister.validateUser(username, password);
                    if ("Login successful!".equals(result)) {
                        loggedInUsers.putIfAbsent(clientSocket, username);
                        userSockets.putIfAbsent(username, clientSocket);
                    }
                    out.println(result);
                }
                out.println(END_OF_RESPONSE);
            } finally {
                loggedInUsersLock.writeLock().unlock();
                userSocketsLock.writeLock().unlock();
            }
        }
    }
}
