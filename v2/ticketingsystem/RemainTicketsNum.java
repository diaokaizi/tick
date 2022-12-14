package ticketingsystem;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RemainTicketsNum {
    private Map<Integer, Integer> nums = new HashMap<>();
    private int stationN;
    public RemainTicketsNum(int stationN, int seatN, int coachN){
        this.stationN = stationN;
        for(int i = 0; i < stationN; i++){
            for(int j = i + 1; j < stationN; j++){
                nums.put(Mask.getMask(i, j), seatN * coachN);
            }
        }
    }
    synchronized public int inquriy(int departure, int arrival){
        return nums.get(Mask.getMask(departure, arrival));
    }
    synchronized public void buyTicket(int[] diff){
        for(int i = 0; i < stationN; i++){
            for(int j = i + 1; j < stationN; j++){
                int mask = Mask.getMask(i, j);
                if((mask & diff[0]) == 0 && (mask & diff[1]) != 0 ) {
                    nums.get(mask);
                    nums.put(mask, nums.get(mask) - 1);
                }
            }
        }
    }
    synchronized public void refundTicket(int[] diff){
        for(int i = 0; i < stationN; i++){
            for(int j = i + 1; j < stationN; j++){
                int mask = Mask.getMask(i, j);
                if((mask & diff[0]) != 0 && (mask & diff[1]) == 0 ) {
                    nums.get(mask);
                    nums.put(mask, nums.get(mask) + 1);
                }
            }
        }
    }
}
