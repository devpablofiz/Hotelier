import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class HotelManager {
    private List<Hotel> hotels;
    private final Map<String, List<Hotel>> rankedHotelsByCity; //city->sorted list of hotels by ranking
    private final RankingUpdateManagerImpl rankingUpdateManager;
    private final String multicastAddress;
    private final int multicastPort;
    private String hotelsPath; //json file path for hotels
    private final ReentrantReadWriteLock hotelsLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock rankedHotelsByCityLock = new ReentrantReadWriteLock();

    public HotelManager(RankingUpdateManagerImpl rankingUpdateManager, Properties properties) throws IOException {
        this.rankingUpdateManager = rankingUpdateManager;
        this.rankedHotelsByCity = new HashMap<>();
        this.multicastAddress = properties.getProperty("multicast.ip");
        this.multicastPort = Integer.parseInt(properties.getProperty("multicast.port"));
        this.hotelsPath = properties.getProperty("hotels.json.file.path");

        //load hotels and update rankings once when starting the service
        loadHotels(hotelsPath);
        updateRankings();

        //schedule next updates and data saves
        long savePeriod = Long.parseLong(properties.getProperty("save.period"));
        long rankingUpdatePeriod = Long.parseLong(properties.getProperty("ranking.update.period"));
        schedulePeriodicSave(savePeriod);
        schedulePeriodicRankingUpdate(rankingUpdatePeriod);
    }

    //loads hotels using gson
    private void loadHotels(String jsonFilePath) throws IOException {
        Gson gson = new Gson();
        Type hotelListType = new TypeToken<List<Hotel>>() {
        }.getType();
        try (FileReader reader = new FileReader(jsonFilePath)) {
            hotelsLock.writeLock().lock();
            try {
                hotels = gson.fromJson(reader, hotelListType);
                if (hotels == null) {
                    hotels = new ArrayList<>();
                }
            } finally {
                hotelsLock.writeLock().unlock();
            }
        }
    }

    public boolean submitReview(String name, String city, double rate, int posizione, int pulizia, int servizio, int prezzo) {
        try {
            Hotel hotel = searchHotelByNameAndCity(name, city);
            hotelsLock.writeLock().lock();
            if (hotel != null) {
                hotel.submitReview(rate, posizione, pulizia, servizio, prezzo);
                return true;
            } else {
                return false;
            }
        } finally {
            hotelsLock.writeLock().unlock();
        }
    }

    public Hotel searchHotelByNameAndCity(String name, String city) {
        hotelsLock.readLock().lock();
        try {
            for (Hotel hotel : hotels) {
                if (hotel.getName().equalsIgnoreCase(name) && hotel.getCity().equalsIgnoreCase(city)) {
                    return hotel;
                }
            }
            return null;
        } finally {
            hotelsLock.readLock().unlock();
        }
    }

    public List<Hotel> searchHotelsByCity(String city) {
        //make sure received input is parsed to capital first letter
        String capitalizedCity = city.substring(0, 1).toUpperCase() + city.substring(1);

        rankedHotelsByCityLock.readLock().lock();
        try {
            List<Hotel> cityHotels = rankedHotelsByCity.get(capitalizedCity);
            if (cityHotels == null) {
                return new ArrayList<>();
            }
            return new ArrayList<>(cityHotels);
        } finally {
            rankedHotelsByCityLock.readLock().unlock();
        }
    }

    //save hotel data using gson
    private void saveHotelsToJson() {
        System.out.println("Saving hotel data to disk...");
        hotelsLock.readLock().lock();
        try (FileWriter writer = new FileWriter(this.hotelsPath)) {
            Gson gson = new Gson();
            gson.toJson(hotels, writer);
            System.out.println("Hotel data saved!");
        } catch (IOException e) {
            System.out.printf("Error while saving hotel data!");
            e.printStackTrace();
        } finally {
            hotelsLock.readLock().unlock();
        }
    }

    public void schedulePeriodicSave(long periodInMillis) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                saveHotelsToJson();
            }
        }, periodInMillis, periodInMillis);
    }

    public void schedulePeriodicRankingUpdate(long periodInMillis) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateRankings();
            }
        }, periodInMillis, periodInMillis);
    }

    private void updateRankings() {
        System.out.println("Updating Hotel Rankings...");
        Map<String, List<Hotel>> newRankings = getRankedHotelsByCity();

        rankedHotelsByCityLock.writeLock().lock();
        try {
            //iterate all cities in the new ranking map
            for (String city : newRankings.keySet()) {
                List<Hotel> newCityRankings = newRankings.get(city);
                List<Hotel> oldCityRankings = rankedHotelsByCity.get(city);

                //check if the lists have the same elements in the same order
                if (!areRankingsEqual(newCityRankings, oldCityRankings)) {
                    //update the manager's map
                    rankedHotelsByCity.put(city, newCityRankings);
                    notifyRankingUpdate(city, newCityRankings);
                    //check if the hotel in first position changed
                    if (hasFirstPositionChanged(newCityRankings, oldCityRankings)) {
                        sendMulticastMessage(city, newCityRankings.get(0));
                    }
                }
            }
        } finally {
            rankedHotelsByCityLock.writeLock().unlock();
        }
        System.out.println("Hotel Rankings Updated!");
    }

    private boolean areRankingsEqual(List<Hotel> newRankings, List<Hotel> oldRankings) {
        if (newRankings == null || oldRankings == null) {
            return false;
        }

        if (newRankings.size() != oldRankings.size()) {
            return false;
        }

        for (int i = 0; i < newRankings.size(); i++) {
            if (newRankings.get(i).getId() != oldRankings.get(i).getId()) {
                return false;
            }
        }

        return true;
    }

    private boolean hasFirstPositionChanged(List<Hotel> newRankings, List<Hotel> oldRankings) {
        if (oldRankings == null || oldRankings.isEmpty()) {
            return true;
        }
        return newRankings.get(0).getId() != oldRankings.get(0).getId();
    }

    private void notifyRankingUpdate(String city, List<Hotel> newRankings) {
        StringBuilder message = new StringBuilder();
        message.append("Updated rankings for ").append(city).append(":\n");
        for (int i = 0; i < newRankings.size(); i++) {
            Hotel hotel = newRankings.get(i);
            message.append(i + 1).append(". ").append(hotel.getName()).append(" - Score: ").append(hotel.getLocalScore()).append("\n");
        }

        try {
            rankingUpdateManager.notifyListeners(city, message.toString());
        } catch (RemoteException e) {
            System.err.println("Error notifying listeners for city " + city);
            e.printStackTrace();
        }
    }

    private void sendMulticastMessage(String city, Hotel topHotel) {
        String message = "New top hotel in " + city + ": " + topHotel.getName() + " with score " + topHotel.getLocalScore();
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress group = InetAddress.getByName(multicastAddress);
            byte[] msgBytes = message.getBytes();
            DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, group, multicastPort);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Error sending multicast message for city " + city);
            e.printStackTrace();
        }
    }

    public Map<String, List<Hotel>> getRankedHotelsByCity() {
        Map<String, List<Hotel>> newRankedHotelsByCity = new HashMap<>();

        hotelsLock.readLock().lock();
        try {
            //build map such that city -> list of it's hotels
            for (Hotel hotel : hotels) {
                String city = hotel.getCity();
                newRankedHotelsByCity
                        //add a new city to the map only if its the first time matching it
                        .computeIfAbsent(city, k -> new ArrayList<>())
                        .add(hotel);
            }
        } finally {
            hotelsLock.readLock().unlock();
        }

        //iterate all keys (all cities in the map)
        for (String city : newRankedHotelsByCity.keySet()) {
            //sort the hotels in this city by local score
            List<Hotel> hotelsInCity = newRankedHotelsByCity.get(city);
            hotelsInCity.sort((h1, h2) -> Double.compare(h2.getLocalScore(), h1.getLocalScore()));
        }

        return newRankedHotelsByCity;
    }
}
