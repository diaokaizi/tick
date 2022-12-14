package ticketingsystem;

import java.util.concurrent.atomic.AtomicInteger;

public class Seat {
    private AtomicInteger ordered = new AtomicInteger(0);;
//    private ReentrantLock lock = new ReentrantLock(true);
//    public Seat(){
//
//    }

   public boolean query(int departure, int arrival){
        return (ordered.get() & Mask.getMask(departure, arrival)) == 0;
    }
    
    public int[] buy(int departure, int arrival){
        while(true){
            int oldorder = ordered.get();
            //如果被占用，则购票失败
            if((oldorder & Mask.getMask(departure, arrival)) != 0){
                return null;
            }
            int neworder = oldorder | Mask.getMask(departure, arrival);
            if(ordered.compareAndSet(oldorder, neworder)) {
                return new int[]{oldorder, neworder};
            }
        }
    }

    public int[] refound(int departure, int arrival){
        while(true){
            int oldorder = ordered.get();
            int neworder = oldorder & ~Mask.getMask(departure, arrival);
            if(ordered.compareAndSet(oldorder, neworder)) {
                return new int[]{oldorder, neworder};
            }
        }
    }
}
