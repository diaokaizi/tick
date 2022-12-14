package ticketingsystem;

import java.util.concurrent.atomic.AtomicInteger;

public class Train {
    private RemainTicketsNum remainTicketsNum;
    private Seat[][] seats;
    private int coachN;
    private int seatN;
    private int stationN;

    public Train(int coachN, int seatN, int stationN){
        this.coachN = coachN;
        this.seatN = seatN;
        this.stationN = stationN;
        seats = new Seat[coachN][seatN];
        remainTicketsNum = new RemainTicketsNum(stationN, seatN, coachN);
        for(int i = 0; i < coachN; i++){
            for(int j = 0; j < seatN; j++){
                seats[i][j] = new Seat();
            }
        }

    }

    public int inquriy(int departure, int arrival){
        return remainTicketsNum.inquriy(departure, arrival);
    }

    //返回车厢号coachnum、座位号seatnum
    public int[] buyTicket(int departure, int arrival){
        int[] res = new int[]{-1, -1};
        for(int i = 0; i < coachN; i++){
            for(int j = 0; j < seatN; j++){
                int[] diff = seats[i][j].buy(departure, arrival);
                if(diff != null) {
                    //更新updateNums
                    remainTicketsNum.buyTicket(diff);
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
        remainTicketsNum.refundTicket(diff);
    }
}
