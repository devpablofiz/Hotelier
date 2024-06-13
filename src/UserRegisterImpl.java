
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class UserRegisterImpl extends UnicastRemoteObject implements UserRegister {
    private static final long serialVersionUID = 1L;
    private static final String JSON_FILE_PATH = "users.json";
    private final Map<String, User> users;

    protected UserRegisterImpl() throws RemoteException {
        super();
        users = new ConcurrentHashMap<>();
        loadUsersFromJson();
        schedulePeriodicSave(30 * 1000); // Save every 30 secs
    }

    @Override
    public synchronized String registerUser(String username, String password) throws RemoteException {
        if (users.containsKey(username)) {
            return "Username already exists!";
        }
        users.put(username, new User(username, password, 0));
        return "User registered successfully!";
    }

    public synchronized User getUser(String username) {
        return users.get(username);
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

//    public synchronized boolean isUserValid(String username, String password) {
//        return users.containsKey(username) && users.get(username).validatePassword(password);
//    }

    // Method to save users to JSON file
    private synchronized void saveUsersToJson() {
        System.out.println("Saving user data to disk...");
        try (FileWriter writer = new FileWriter(JSON_FILE_PATH)) {
            Gson gson = new Gson();
            gson.toJson(users, writer);
            System.out.println("User data saved!");
        } catch (IOException e) {
            System.out.println("Error while saving user data!");
            e.printStackTrace();
        }
    }

    // Method to load users from JSON file
    private synchronized void loadUsersFromJson() {
        try {
            String json = new String(Files.readAllBytes(Paths.get(JSON_FILE_PATH)));
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, User>>() {
            }.getType();
            Map<String, User> loadedUsers = gson.fromJson(json, type);
            if (loadedUsers != null) {
                users.putAll(loadedUsers);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to schedule periodic saving of users
    private void schedulePeriodicSave(long periodInMillis) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                saveUsersToJson();
            }
        }, periodInMillis, periodInMillis);
    }
}
