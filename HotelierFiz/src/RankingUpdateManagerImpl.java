import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RankingUpdateManagerImpl extends UnicastRemoteObject implements RankingUpdateManager {
    private final Map<String, List<RankingUpdateListener>> cityListeners; //city -> list of listeners for that city

    protected RankingUpdateManagerImpl() throws RemoteException {
        cityListeners = new HashMap<>();
    }

    @Override
    public synchronized void registerListener(String city, RankingUpdateListener listener) throws RemoteException {
        //make sure registered city has capital first letter as thats how we store them
        String capitalizedCity = city.substring(0, 1).toUpperCase() + city.substring(1);
        cityListeners.computeIfAbsent(capitalizedCity, k -> new ArrayList<>()).add(listener);
    }

    @Override
    public synchronized void removeListener(String city, RankingUpdateListener listener) throws RemoteException {
        //make sure registered city has capital first letter as thats how we store them
        String capitalizedCity = city.substring(0, 1).toUpperCase() + city.substring(1);

        List<RankingUpdateListener> listeners = cityListeners.get(capitalizedCity);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                cityListeners.remove(capitalizedCity);
            }
        }
    }

    public void notifyListeners(String city, String message) throws RemoteException {
        List<RankingUpdateListener> listeners = cityListeners.get(city);
        if (listeners != null) {
            for (RankingUpdateListener listener : listeners) {
                listener.notifyRankingUpdate(city, message);
            }
        }
    }
}
