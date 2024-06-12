import java.time.LocalDate;

public class Review {
    private final double score;
    private final LocalDate date;

    public Review(double score, LocalDate date) {
        this.score = score;
        this.date = date;
    }

    public double getScore() {
        return score;
    }

    public LocalDate getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "Review{" +
                "score=" + score +
                ", date=" + date +
                '}';
    }
}
