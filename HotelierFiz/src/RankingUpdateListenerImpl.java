import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class RankingUpdateListenerImpl extends UnicastRemoteObject implements RankingUpdateListener {
    private final Map<String, Map<Integer, String>> rankings; //city -> map of (rank -> hotel info)

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
        //clear the current rankings for the city
        Map<Integer, String> cityRankings = new HashMap<>();

        //parse the message to extract the rankings
        String[] lines = message.split("\n");
        for (String line : lines) {
            if (line.matches("\\d+\\. .* - Score: .*")) {
                String[] parts = line.split("\\. ", 2);
                int rank = Integer.parseInt(parts[0]);
                String hotelInfo = parts[1];
                cityRankings.put(rank, hotelInfo);
            }
        }

        //update the rankings map
        synchronized (rankings) {
            rankings.put(city, cityRankings);
        }
    }
}
