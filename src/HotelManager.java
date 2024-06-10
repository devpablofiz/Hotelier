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

    public boolean submitReview(String name, String city, double newRate, int newPosizione, int newPulizia, int newServizio, int newPrezzo) {
        Hotel hotel = searchHotelByNameAndCity(name, city);
        if (hotel != null) {
            hotel.submitReview(newRate, newPosizione, newPulizia, newServizio, newPrezzo);
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
}
