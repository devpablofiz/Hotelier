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

    public void submitReview(double newRate, int newPosizione, int newPulizia, int newServizio, int newPrezzo) {
        this.rate = (this.rate * this.reviewCount + newRate) / (this.reviewCount + 1);
        this.posizione = (this.posizione * this.reviewCount + newPosizione) / (this.reviewCount + 1);
        this.pulizia = (this.pulizia * this.reviewCount + newPulizia) / (this.reviewCount + 1);
        this.servizio = (this.servizio * this.reviewCount + newServizio) / (this.reviewCount + 1);
        this.prezzo = (this.prezzo * this.reviewCount + newPrezzo) / (this.reviewCount + 1);
        this.reviewCount++;
    }

    @Override
    public String toString() {
        return name + '\n' +
                "Descrizione: " + description + '\n' +
                "Città: " + city + '\n' +
                "Telefono: " + phone + '\n' +
                "Servizi: " + services + '\n' +
                "Posizione nella classifica locale: +++TODO+++ \n" +
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
