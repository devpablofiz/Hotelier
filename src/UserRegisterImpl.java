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
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UserRegisterImpl extends UnicastRemoteObject implements UserRegister {
    private final String jsonFilePath;
    private final Map<String, User> users;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

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
        lock.writeLock().lock();
        try {
            User oldUser = users.putIfAbsent(username, new User(username, password, 0));
            if (oldUser != null) {
                return "Username already exists!";
            }
            return "User registered successfully!";
        } finally {
            lock.writeLock().unlock();
        }
    }

    public User getUser(String username) {
        lock.readLock().lock();
        try {
            return users.get(username);
        } finally {
            lock.readLock().unlock();
        }
    }

    public String validateUser(String username, String password) {
        lock.readLock().lock();
        try {
            if (!users.containsKey(username)) {
                return "Username does not exist!";
            }
            if (!users.get(username).validatePassword(password)) {
                return "Invalid password!";
            }
            return "Login successful!";
        } finally {
            lock.readLock().unlock();
        }
    }

    private void saveUsersToJson() {
        System.out.println("Saving user data to disk...");
        lock.writeLock().lock();
        try (FileWriter writer = new FileWriter(jsonFilePath)) {
            Gson gson = new Gson();
            gson.toJson(users, writer);
            System.out.println("User data saved!");
        } catch (IOException e) {
            System.out.println("Error while saving user data!");
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void loadUsersFromJson() {
        lock.writeLock().lock();
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
        } finally {
            lock.writeLock().unlock();
        }
    }

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
