import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HotelManager {
    private List<Hotel> hotels;

    public HotelManager(String jsonFilePath) throws IOException {
        loadHotels(jsonFilePath);
    }

    private void loadHotels(String jsonFilePath) throws IOException {
        Gson gson = new Gson();
        Type hotelListType = new TypeToken<List<Hotel>>() {}.getType();
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

    public void submitReview(int hotelId, double newRate, int newPosizione, int newPulizia, int newServizio, int newPrezzo) {
        Hotel hotel = getHotelById(hotelId);
        if (hotel != null) {
            hotel.submitReview(newRate, newPosizione, newPulizia, newServizio, newPrezzo);
        } else {
            System.out.println("Hotel with ID " + hotelId + " not found.");
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
        List<Hotel> cityHotels = new ArrayList<>();
        for (Hotel hotel : hotels) {
            if (hotel.getCity().equalsIgnoreCase(city)) {
                cityHotels.add(hotel);
            }
        }
        return cityHotels;
    }

    @Override
    public String toString() {
        return "HotelManager{" +
                "hotels=" + hotels +
                '}';
    }

    public static void main(String[] args) {
        try {
            HotelManager hotelManager = new HotelManager("hotels.json");
            System.out.println(hotelManager);

            // Example of searching for a hotel by name and city
            Hotel hotel = hotelManager.searchHotelByNameAndCity("Hotel Genova 5", "Genova");
            System.out.println("Search by name and city: " + hotel);

            // Example of listing all hotels in a city
            List<Hotel> cityHotels = hotelManager.searchHotelsByCity("Genova");
            System.out.println("Hotels in Genova: " + cityHotels);

            // Example of submitting a review
            hotelManager.submitReview(25, 4.5, 4, 5, 4, 5);
            System.out.println(hotelManager.getHotelById(25));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
