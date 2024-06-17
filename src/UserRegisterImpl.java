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
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class UserRegisterImpl extends UnicastRemoteObject implements UserRegister {
    private static final long serialVersionUID = 1L;
    private final String jsonFilePath;
    private final Map<String, User> users;

    protected UserRegisterImpl(Properties properties) throws RemoteException {
        super();
        this.jsonFilePath = properties.getProperty("json.file.path", "users.json");
        this.users = new ConcurrentHashMap<>();
        loadUsersFromJson();
        long savePeriod = Long.parseLong(properties.getProperty("save.period", "30000")); // Default to 30 secs
        schedulePeriodicSave(savePeriod);
    }

    @Override
    public String registerUser(String username, String password) throws RemoteException {
        User oldUser = users.putIfAbsent(username, new User(username, password, 0));
        if (oldUser != null) {
            return "Username already exists!";
        }
        return "User registered successfully!";
    }

    public User getUser(String username) {
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

    // Method to save users to JSON file
    private synchronized void saveUsersToJson() {
        System.out.println("Saving user data to disk...");
        try (FileWriter writer = new FileWriter(jsonFilePath)) {
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
            String json = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
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
