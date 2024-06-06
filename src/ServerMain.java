import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class ServerMain {
    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(1099);
            UserRegisterImpl userRegister = new UserRegisterImpl();
            Naming.rebind("UserRegister", userRegister);
            System.out.println("UserRegister Server is ready.");

            // Start TCP server for various functions
            new Thread(() -> {
                try {
                    TCPServer.start(userRegister);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            System.err.println("UserRegister Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
