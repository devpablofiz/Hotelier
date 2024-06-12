import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddReviewsField {
    public static void main(String[] args) {
        String filePath = "hotels.json";

        try {
            // Read the JSON file into a String
            String json = new String(Files.readAllBytes(Paths.get(filePath)));

            // Regex pattern to find the end of each hotel object (right before the closing brace)
            String regex = "(\\{\\s*\"id\"\\s*:\\s*\\d+,\\s*\"name\"\\s*:\\s*\"[^\"]*\",\\s*\"description\"\\s*:\\s*\"[^\"]*\",\\s*\"city\"\\s*:\\s*\"[^\"]*\",\\s*\"phone\"\\s*:\\s*\"[^\"]*\",\\s*\"services\"\\s*:\\s*\\[[^\\]]*\\],\\s*\"rate\"\\s*:\\s*\\d+,\\s*\"ratings\"\\s*:\\s*\\{\\s*\"cleaning\"\\s*:\\s*\\d+,\\s*\"position\"\\s*:\\s*\\d+,\\s*\"services\"\\s*:\\s*\\d+,\\s*\"quality\"\\s*:\\s*\\d+\\s*\\})";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(json);

            // Replace each match with the same content plus the reviews field
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String hotelObject = matcher.group(1);
                String updatedHotelObject = hotelObject + ", \"reviews\": []";
                matcher.appendReplacement(sb, updatedHotelObject);
            }
            matcher.appendTail(sb);

            // Print the updated JSON string
            String updatedJson = sb.toString();
            System.out.println(updatedJson);

            // Optionally, write the updated JSON string back to a file
            Files.write(Paths.get("updated_hotels.json"), updatedJson.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
