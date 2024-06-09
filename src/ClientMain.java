import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.Naming;
import java.util.Scanner;

public class ClientMain {
    private static UserRegister userRegister;
    private static final String TCP_HOST = "localhost";
    private static final int TCP_PORT = 12345;
    private static final String END_OF_RESPONSE = "END_OF_RESPONSE";

    public static void main(String[] args) {
        try {
            // Lookup the remote object "UserRegister"
            userRegister = (UserRegister) Naming.lookup("rmi://localhost/UserRegister");

            Scanner scanner = new Scanner(System.in);

            // Establish a single TCP connection
            try (Socket socket = new Socket(TCP_HOST, TCP_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                while (true) {
                    System.out.print("> ");
                    String input = scanner.nextLine();

                    if (input.startsWith("register(")) {
                        handleRegister(input);
                    } else if (input.startsWith("login(") || input.startsWith("logout(")) {
                        handleTCPRequest(input, out, in);
                    } else {
                        System.out.println("Unknown command");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("UserRegister Client exception: " + e.toString());
            e.printStackTrace();
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

    private static void handleTCPRequest(String input, PrintWriter out, BufferedReader in) {
        try {
            out.println(input);
            String response;
            while ((response = in.readLine()) != null) {
                if (response.equals(END_OF_RESPONSE)) {
                    break;
                }
                System.out.println(response);
            }
        } catch (Exception e) {
            System.err.println("TCP request exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
