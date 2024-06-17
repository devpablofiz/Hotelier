import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RankingUpdateManagerImpl extends UnicastRemoteObject implements RankingUpdateManager {
    private final Map<String, List<RankingUpdateListener>> cityListeners;

    protected RankingUpdateManagerImpl() throws RemoteException {
        cityListeners = new HashMap<>();
    }

    @Override
    public synchronized void registerListener(String city, RankingUpdateListener listener) throws RemoteException {
        cityListeners.computeIfAbsent(city, k -> new ArrayList<>()).add(listener);
    }

    @Override
    public synchronized void removeListener(String city, RankingUpdateListener listener) throws RemoteException {
        List<RankingUpdateListener> listeners = cityListeners.get(city);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                cityListeners.remove(city);
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
