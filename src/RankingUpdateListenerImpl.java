import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RankingUpdateListenerImpl extends UnicastRemoteObject implements RankingUpdateListener {
    protected RankingUpdateListenerImpl() throws RemoteException {
        super();
    }

    @Override
    public void notifyRankingUpdate(String city, String message) throws RemoteException {
        System.out.println("Ranking update for " + city + ": " + message);
    }
}
