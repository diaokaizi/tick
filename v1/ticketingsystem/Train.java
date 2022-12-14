package ticketingsystem;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public class Train {
    private ConcurrentHashMap<Integer, AtomicInteger> updateNums;
    private Seat[][] seats;
    private int coachN;
    private int seatN;
    private int stationN;

    public Train(int coachN, int seatN, int stationN){
        this.coachN = coachN;
        this.seatN = seatN;
        this.stationN = stationN;
        updateNums = new ConcurrentHashMap<>();
        seats = new Seat[coachN][seatN];
        for(int i = 0; i < coachN; i++){
            for(int j = 0; j < seatN; j++){
                seats[i][j] = new Seat();
            }
        }
        for(int i = 0; i < stationN; i++){
            for(int j = i + 1; j < stationN; j++){
                updateNums.put(Mask.getMask(i, j), new AtomicInteger());
            }
        }
    }

    public int inquriy(int departure, int arrival){
        return seatN * coachN + updateNums.get(Mask.getMask(departure, arrival)).intValue();
    }

    //返回车厢号coachnum、座位号seatnum
    public int[] buyTicket(int departure, int arrival){
        int[] res = new int[]{-1, -1};
        for(int i = 0; i < coachN; i++){
            for(int j = 0; j < seatN; j++){
                int[] diff = seats[i][j].buy(departure, arrival);
                if(diff != null) {
                    //更新updateNums
                    decreaseUpdateNums(diff);
                    res[0] = i;
                    res[1] = j;
                    return res;
                }
            }
        }
        return res;
    }

    public void refundTicket(int coachnum, int seatnum, int departure, int arrival){
        int[] diff = seats[coachnum][seatnum].refound(departure, arrival);
        increaseUpdateNums(diff);
    }

    public void decreaseUpdateNums(int[] diff){
        for(int i = 0; i < stationN; i++){
            for(int j = i + 1; j < stationN; j++){
                int mask = Mask.getMask(i, j);
                if((mask & diff[0]) == 0 && (mask & diff[1]) != 0 ) {
                    updateNums.get(mask).getAndDecrement();
                }
            }
        }
    }
    public void increaseUpdateNums(int[] diff){
        for(int i = 0; i < stationN; i++){
            for(int j = i + 1; j < stationN; j++){
                int mask = Mask.getMask(i, j);
                if((mask & diff[0]) != 0 && (mask & diff[1]) == 0 ) {
                    updateNums.get(mask).getAndIncrement();
                }
            }
        }
    }
}
