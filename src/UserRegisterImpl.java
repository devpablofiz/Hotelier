import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class UserRegisterImpl extends UnicastRemoteObject implements UserRegister {
    private static final long serialVersionUID = 1L;
    private Map<String, User> users;

    protected UserRegisterImpl() throws RemoteException {
        super();
        users = new HashMap<>();
    }

    @Override
    public synchronized String registerUser(String username, String password) throws RemoteException {
        if (users.containsKey(username)) {
            return "Username already exists!";
        }
        users.put(username, new User(username, password, 0));
        return "User registered successfully!";
    }

    public synchronized String validateUser(String username, String password) {
        if (!users.containsKey(username)) {
            return "Username does not exist!";
        }
        if (!users.get(username).validatePassword(password)) {
            return "Invalid password!";
        }
        return "Login successful!";
    }

    public synchronized boolean isUserValid(String username, String password) {
        return users.containsKey(username) && users.get(username).validatePassword(password);
    }
}
