import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RankingUpdateManager extends Remote {
    void registerListener(String city, RankingUpdateListener listener) throws RemoteException;
    void removeListener(String city, RankingUpdateListener listener) throws RemoteException;
}
