import java.io.*;
import java.net.Socket;
import java.rmi.Naming;
import java.util.Properties;
import java.util.Scanner;

public class ClientMain {
    private static UserRegister userRegister;
    private static RankingUpdateManager rankingUpdateManager;
    private static MulticastReceiver multicastReceiver;
    private static final String END_OF_RESPONSE = "END_OF_RESPONSE";
    private static boolean isLoggedIn = false;
    private static Properties properties;

    public static void main(String[] args) {
        loadProperties();

        try {
            String rmiHost = properties.getProperty("rmi.host");
            int rmiPort = Integer.parseInt(properties.getProperty("rmi.port"));
            userRegister = (UserRegister) Naming.lookup("rmi://" + rmiHost + ":" + rmiPort + "/UserRegister");
            rankingUpdateManager = (RankingUpdateManager) Naming.lookup("rmi://" + rmiHost + ":" + rmiPort + "/RankingUpdateManager");

            RankingUpdateListenerImpl listener = new RankingUpdateListenerImpl();

            Scanner scanner = new Scanner(System.in);

            String tcpHost = properties.getProperty("tcp.host");
            int tcpPort = Integer.parseInt(properties.getProperty("tcp.port"));

            try (Socket socket = new Socket(tcpHost, tcpPort);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                while (true) {
                    System.out.print("> ");
                    String input = scanner.nextLine();

                    if (input.startsWith("register(")) {
                        handleRegister(input);
                    } else if (input.startsWith("subscribe(")) {
                        handleSubscribe(input, listener);
                    } else if (input.startsWith("unsubscribe(")) {
                        handleUnsubscribe(input, listener);
                    } else if (input.startsWith("login(") || input.startsWith("logout(") ||
                            input.startsWith("searchHotel(") || input.startsWith("searchAllHotels(") ||
                            input.startsWith("insertReview(") || input.startsWith("showMyBadges(")) {
                        handleTCPRequest(input, out, in);
                    } else {
                        System.out.println("Unknown command");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private static void loadProperties() {
        properties = new Properties();
        try (InputStream input = ClientMain.class.getClassLoader().getResourceAsStream("client-config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find client-config.properties");
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void handleRegister(String input) {
        try {
            String[] parts = input.split("\\(", 2);
            String[] args = parts[1].replace(")", "").split(",");
            if (args.length != 2) {
                System.out.println("Invalid arguments format");
                return;
            }

            String username = args[0];
            String password = args[1];
            String result = userRegister.registerUser(username, password);
            System.out.println(result);
        } catch (Exception e) {
            System.err.println("Register exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private static void handleLogin(String input, PrintWriter out, BufferedReader in) {
        try {
            String[] parts = input.split("\\(", 2);
            String[] args = parts[1].replace(")", "").split(",");
            if (args.length != 2) {
                System.out.println("Invalid arguments format");
                return;
            }

            String username = args[0];
            String password = args[1];
            out.println("login(" + username + "," + password + ")");
            StringBuilder responseBuilder = new StringBuilder();
            String response;
            while ((response = in.readLine()) != null) {
                if (response.equals(END_OF_RESPONSE)) {
                    break;
                }
                responseBuilder.append(response).append("\n");
            }
            response = responseBuilder.toString().trim();
            System.out.println(response);
            isLoggedIn = response.contains("Login successful");

            if (isLoggedIn) {
                multicastReceiver = new MulticastReceiver();
                multicastReceiver.startListening();
            }
        } catch (Exception e) {
            System.err.println("Login exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private static void handleLogout(String input, PrintWriter out, BufferedReader in) {
        try {
            out.println(input);
            StringBuilder responseBuilder = new StringBuilder();
            String response;
            while ((response = in.readLine()) != null) {
                if (response.equals(END_OF_RESPONSE)) {
                    break;
                }
                responseBuilder.append(response).append("\n");
            }
            response = responseBuilder.toString().trim();
            System.out.println(response);
            isLoggedIn = !response.contains("Logout successful");

            if (!isLoggedIn && multicastReceiver != null) {
                multicastReceiver.stopListening();
                multicastReceiver = null;
            }
        } catch (Exception e) {
            System.err.println("Logout exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private static void handleSubscribe(String input, RankingUpdateListenerImpl listener) {
        try {
            String[] parts = input.split("\\(", 2);
            String city = parts[1].replace(")", "");
            rankingUpdateManager.registerListener(city, listener);
            System.out.println("Subscribed to ranking updates for " + city);
        } catch (Exception e) {
            System.err.println("Subscribe exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private static void handleUnsubscribe(String input, RankingUpdateListenerImpl listener) {
        try {
            String[] parts = input.split("\\(", 2);
            String city = parts[1].replace(")", "");
            rankingUpdateManager.removeListener(city, listener);
            System.out.println("Unsubscribed from ranking updates for " + city);
        } catch (Exception e) {
            System.err.println("Unsubscribe exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private static void handleTCPRequest(String input, PrintWriter out, BufferedReader in) {
        try {
            out.println(input);
            StringBuilder responseBuilder = new StringBuilder();
            String response;
            while ((response = in.readLine()) != null) {
                if (response.equals(END_OF_RESPONSE)) {
                    break;
                }
                responseBuilder.append(response).append("\n");
            }
            response = responseBuilder.toString().trim();
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("TCP request exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
