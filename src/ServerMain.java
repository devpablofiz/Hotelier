import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.Properties;

public class ServerMain {
    private static Properties properties;

    public static void main(String[] args) {
        loadProperties();
        try {
            int rmiPort = Integer.parseInt(properties.getProperty("rmi.port"));
            LocateRegistry.createRegistry(rmiPort);
            UserRegisterImpl userRegister = new UserRegisterImpl(properties);
            Naming.rebind("rmi://localhost:" + rmiPort + "/UserRegister", userRegister);
            System.out.println("UserRegister Server is ready.");

            RankingUpdateManagerImpl rankingUpdateManager = new RankingUpdateManagerImpl();
            Naming.rebind("rmi://localhost:" + rmiPort + "/RankingUpdateManager", rankingUpdateManager);
            System.out.println("RankingUpdateManager Server is ready.");

            HotelManager hotelManager = new HotelManager(rankingUpdateManager, properties);

            //start TCP server in new thread
            new Thread(() -> {
                try {
                    int tcpPort = Integer.parseInt(properties.getProperty("tcp.port"));
                    TCPServer.start(userRegister, hotelManager, tcpPort);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            System.err.println("UserRegister Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private static void loadProperties() {
        properties = new Properties();
        try (InputStream input = new FileInputStream("server-config.properties")) {
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
