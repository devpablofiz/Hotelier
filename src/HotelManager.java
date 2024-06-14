import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class HotelManager {
    private List<Hotel> hotels;
    private Map<String, List<Hotel>> rankedHotelsByCity;

    public HotelManager(String jsonFilePath) throws IOException {
        loadHotels(jsonFilePath);
        updateRankings();
        schedulePeriodicSave(30 * 1000);
        schedulePeriodicRankingUpdate(30 * 1000);
    }

    private void loadHotels(String jsonFilePath) throws IOException {
        Gson gson = new Gson();
        Type hotelListType = new TypeToken<List<Hotel>>() {
        }.getType();
        try (FileReader reader = new FileReader(jsonFilePath)) {
            hotels = gson.fromJson(reader, hotelListType);
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
        //Hotels are saved with capital first letter city string
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
        rankedHotelsByCity = getRankedHotelsByCity();
        System.out.println("Hotel Rankings Updated!");
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
