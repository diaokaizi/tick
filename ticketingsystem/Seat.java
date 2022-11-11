package ticketingsystem;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Seat {
    private Lock lock;
    private boolean[] range;

    public Seat(int stationnum){
        lock = new ReentrantLock();
        range = new boolean[stationnum];
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public boolean query(int departure, int arrival){
        for(int i = departure; i <= arrival; i++){
            if(range[i] == true)
                return false;
        }
        return true;
    }
    
    public void order(int departure, int arrival){
        for(int i = departure; i <= arrival; i++){
            range[i] = true;
        }
        
    }

    public void free(int departure, int arrival){
        for(int i = departure; i <= arrival; i++){
            range[i] = false;
        }
    }
}
