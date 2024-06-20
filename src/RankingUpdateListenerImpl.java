import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class RankingUpdateListenerImpl extends UnicastRemoteObject implements RankingUpdateListener {
    //rankings are stored but no function to read them is needed in the client program
    private final Map<String, Map<Integer, String>> rankings;

    protected RankingUpdateListenerImpl() throws RemoteException {
        super();
        this.rankings = new HashMap<>();
    }

    @Override
    public void notifyRankingUpdate(String city, String message) throws RemoteException {
        System.out.println("Ranking update for " + city + ": " + message);
        updateRankings(city, message);
    }

    private void updateRankings(String city, String message) {
        // Clear the current rankings for the city
        Map<Integer, String> cityRankings = new HashMap<>();

        // Parse the message to extract the rankings
        String[] lines = message.split("\n");
        for (String line : lines) {
            if (line.matches("\\d+\\. .* - Score: .*")) {
                String[] parts = line.split("\\. ", 2);
                int rank = Integer.parseInt(parts[0]);
                String hotelInfo = parts[1];
                cityRankings.put(rank, hotelInfo);
            }
        }

        // Update the rankings map
        synchronized (rankings) {
            rankings.put(city, cityRankings);
        }
    }

//    public Map<Integer, String> getRankingsForCity(String city) {
//        synchronized (rankings) {
//            return rankings.getOrDefault(city, new HashMap<>());
//        }
//    }
}
