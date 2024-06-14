import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class User {
    private final String username;
    private final String hashedPassword;
    private final String salt;
    private int reviewsCounter;

    public User(String username, String password, int reviewsCounter) {
        this.username = username;
        this.salt = generateSalt();
        this.hashedPassword = hashPassword(password, salt);
        this.reviewsCounter = reviewsCounter;
    }

    public String getUsername() {
        return username;
    }

    public boolean validatePassword(String password) {
        String hashedInputPassword = hashPassword(password, this.salt);
        return this.hashedPassword.equals(hashedInputPassword);
    }

    public void incrementReviewCounter() {
        this.reviewsCounter++;
    }

    public String getBadge() {
        if (reviewsCounter >= 50) {
            return "Contributore Super";
        }
        if (reviewsCounter >= 20) {
            return "Contributore Esperto";
        }
        if (reviewsCounter >= 10) {
            return "Contributore";
        }
        if (reviewsCounter >= 5) {
            return "Recensore Esperto";
        }
        if (reviewsCounter == 1) {
            return "Recensore";
        }
        return null;
    }

    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(salt));
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
