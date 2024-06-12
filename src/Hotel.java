import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class Hotel {
    private int id;
    private String name;
    private String description;
    private String city;
    private String phone;
    private List<String> services;
    private double rate;
    private double posizione;
    private double pulizia;
    private double servizio;
    private double prezzo;
    private int reviewCount;
    private List<Review> reviews;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCity() {
        return city;
    }

    public String getPhone() {
        return phone;
    }

    public List<String> getServices() {
        return services;
    }

    public double getRate() {
        return rate;
    }

    public double getPosizione() {
        return posizione;
    }

    public double getPulizia() {
        return pulizia;
    }

    public double getServizio() {
        return servizio;
    }

    public double getPrezzo() {
        return prezzo;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void submitReview(double rate, int posizione, int pulizia, int servizio, int prezzo) {
        //update average values
        this.rate = (this.rate * this.reviewCount + rate) / (this.reviewCount + 1);
        this.posizione = (this.posizione * this.reviewCount + posizione) / (this.reviewCount + 1);
        this.pulizia = (this.pulizia * this.reviewCount + pulizia) / (this.reviewCount + 1);
        this.servizio = (this.servizio * this.reviewCount + servizio) / (this.reviewCount + 1);
        this.prezzo = (this.prezzo * this.reviewCount + prezzo) / (this.reviewCount + 1);
        //save review for local score calculations
        this.reviews.add(new Review(rate + posizione + pulizia + servizio + prezzo, LocalDate.now()));
        this.reviewCount++;
    }

    public double getLocalScore() {
        double totalScore = 0;
        double weightSum = 0;
        LocalDate now = LocalDate.now();

        for (Review review : reviews) {
            long daysOld = ChronoUnit.DAYS.between(review.getDate(), now);
            // recent reviews -> higher weight
            double weight = 1.0 / (1 + daysOld);
            totalScore += review.getScore() * weight;
            weightSum += weight;
        }

        double averageWeightedScore = (weightSum != 0) ? totalScore / weightSum : 0;
        // logarithmic factor to account for review count
        double reviewFactor = Math.log(1 + reviewCount);

        if (averageWeightedScore > 0) {
            System.out.println(name + ": " + averageWeightedScore);
        }
        return averageWeightedScore * reviewFactor;
    }

    @Override
    public String toString() {
        return name + '\n' +
                "Descrizione: " + description + '\n' +
                "Città: " + city + '\n' +
                "Telefono: " + phone + '\n' +
                "Servizi: " + services + '\n' +
                "Numero recensioni: " + reviewCount + '\n' +
                "Punteggio complessivo: " + rate + "/5.0 \n" +
                "Punteggi di categoria:" +
                "\n+Posizione: " + posizione + "/5.0" +
                "\n+Pulizia: " + pulizia + "/5.0" +
                "\n+Servizio: " + servizio + "/5.0" +
                "\n+Prezzo: " + prezzo + "/5.0\n" +
                "==============================";
    }
}
