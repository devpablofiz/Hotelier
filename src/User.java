public class User {
    private final String username;
    private final String password;
    private int reviewsCounter;

    public User(String username, String password, int reviewsCounter) {
        this.username = username;
        this.password = password;
        this.reviewsCounter = reviewsCounter;
    }

    public String getUsername() {
        return username;
    }

    public boolean validatePassword(String password) {
        return this.password.equals(password);
    }

//    public int getReviewsCounter() {
//        return reviewsCounter;
//    }

    public void incrementReviewCounter() {
        this.reviewsCounter++;
    }

    public String getBadge() {
        
    }
}
