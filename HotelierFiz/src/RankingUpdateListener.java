import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RankingUpdateListener extends Remote {
    void notifyRankingUpdate(String city, String message) throws RemoteException;
}
