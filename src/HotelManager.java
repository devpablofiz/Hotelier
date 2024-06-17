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

public class HotelManager {
    private List<Hotel> hotels;
    private Map<String, List<Hotel>> rankedHotelsByCity;
    private final RankingUpdateManagerImpl rankingUpdateManager;
    private final String multicastAddress;
    private final int multicastPort;

    public HotelManager(String jsonFilePath, RankingUpdateManagerImpl rankingUpdateManager, Properties properties) throws IOException {
        this.rankingUpdateManager = rankingUpdateManager;
        this.rankedHotelsByCity = new TreeMap<>();
        this.multicastAddress = properties.getProperty("multicast.ip");
        this.multicastPort = Integer.parseInt(properties.getProperty("multicast.port"));
        loadHotels(jsonFilePath);
        updateRankings();
        long savePeriod = Long.parseLong(properties.getProperty("save.period"));
        long rankingUpdatePeriod = Long.parseLong(properties.getProperty("ranking.update.period"));
        schedulePeriodicSave(savePeriod);
        schedulePeriodicRankingUpdate(rankingUpdatePeriod);
    }

    private void loadHotels(String jsonFilePath) throws IOException {
        Gson gson = new Gson();
        Type hotelListType = new TypeToken<List<Hotel>>() {
        }.getType();
        try (FileReader reader = new FileReader(jsonFilePath)) {
            hotels = gson.fromJson(reader, hotelListType);
            if (hotels == null) {
                hotels = new ArrayList<>();
            }
        }
    }

    public List<Hotel> getHotels() {
        return hotels;
    }

    public Hotel getHotelById(int id) {
        for (Hotel hotel : hotels) {
            if (hotel.getId() == id) {
                return hotel;
            }
        }
        return null;
    }

    public boolean submitReview(String name, String city, double rate, int posizione, int pulizia, int servizio, int prezzo) {
        Hotel hotel = searchHotelByNameAndCity(name, city);
        if (hotel != null) {
            hotel.submitReview(rate, posizione, pulizia, servizio, prezzo);
            return true;
        } else {
            return false;
        }
    }

    public Hotel searchHotelByNameAndCity(String name, String city) {
        for (Hotel hotel : hotels) {
            if (hotel.getName().equalsIgnoreCase(name) && hotel.getCity().equalsIgnoreCase(city)) {
                return hotel;
            }
        }
        return null;
    }

    public List<Hotel> searchHotelsByCity(String city) {
        // Hotels are saved with capital first letter city string
        String capitalizedCity = city.substring(0, 1).toUpperCase() + city.substring(1);
        List<Hotel> cityHotels = rankedHotelsByCity.get(capitalizedCity);
        if (cityHotels == null) {
            return new ArrayList<>(); // Return an empty list if city is not found
        }
        return new ArrayList<>(cityHotels);
    }

    private void saveHotelsToJson() {
        System.out.println("Saving hotel data to disk...");
        try (FileWriter writer = new FileWriter("updated_hotels.json")) {
            Gson gson = new Gson();
            gson.toJson(hotels, writer);
            System.out.println("Hotel data saved!");
        } catch (IOException e) {
            System.out.printf("Error while saving hotel data!");
            e.printStackTrace();
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

        for (String city : newRankings.keySet()) {
            List<Hotel> newCityRankings = newRankings.get(city);
            List<Hotel> oldCityRankings = rankedHotelsByCity.get(city);

            if (!areRankingsEqual(newCityRankings, oldCityRankings)) {
                rankedHotelsByCity.put(city, newCityRankings);
                // RMI Callback
                notifyRankingUpdate(city, newCityRankings);
                // UDP Multicast
                if (isFirstPositionChanged(newCityRankings, oldCityRankings)) {
                    sendMulticastMessage(city, newCityRankings.get(0));
                }
            }
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

    private boolean isFirstPositionChanged(List<Hotel> newRankings, List<Hotel> oldRankings) {
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
        // Initialize a TreeMap to hold the rankings by city (TreeMap keeps keys sorted)
        Map<String, List<Hotel>> rankedHotelsByCity = new TreeMap<>();

        // Group hotels by city
        for (Hotel hotel : hotels) {
            String city = hotel.getCity();
            rankedHotelsByCity
                    .computeIfAbsent(city, k -> new ArrayList<>())
                    .add(hotel);
        }

        // Sort each city's hotels by their rate in descending order
        for (String city : rankedHotelsByCity.keySet()) {
            List<Hotel> hotelsInCity = rankedHotelsByCity.get(city);
            hotelsInCity.sort((h1, h2) -> Double.compare(h2.getLocalScore(), h1.getLocalScore()));
        }

        return rankedHotelsByCity;
    }

    @Override
    public String toString() {
        return "HotelManager{" +
                "hotels=" + hotels +
                '}';
    }
}
