import java.rmi.Remote;
import java.rmi.RemoteException;

public interface UserRegister extends Remote {
    String registerUser(String username, String password) throws RemoteException;
}
