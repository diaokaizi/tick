package ticketingsystem;

public class Train {
    private Seat[][] seats;
    private int coachN;
    private int seatN;

    public Train(int coachN, int seatN, int stationN){
        this.coachN = coachN;
        this.seatN = seatN;
        seats = new Seat[coachN][seatN];
        for(int i = 0; i < coachN; i++){
            for(int j = 0; j < seatN; j++){
                seats[i][j] = new Seat(stationN);
            }
        }
    }

    public int inquriy(int departure, int arrival){
        int res = 0;
        for(int i = 0; i < coachN; i++){
            for(int j = 0; j < seatN; j++){
                seats[i][j].lock();
                try{
                    if(seats[i][j].query(departure, arrival))
                        res++;
                }finally{
                    seats[i][j].unlock();
                }
            }
        }
        return res;
    }

    //返回车厢号coachnum、座位号seatnum
    public int[] buyTicket(int departure, int arrival){
        int[] res = new int[]{-1, -1};
        for(int i = 0; i < coachN; i++){
            for(int j = 0; j < seatN; j++){
                seats[i][j].lock();
                try{
                    if(seats[i][j].query(departure, arrival)){
                        seats[i][j].order(departure, arrival);
                        res[0] = i;
                        res[1] = j;
                        break;
                    }
                }finally{
                    seats[i][j].unlock();
                }
            }
        }
        return res;
    }

    public void refundTicket(int coachnum, int seatnum, int departure, int arrival){
        seats[coachnum][seatnum].lock();
        try{
            seats[coachnum][seatnum].free(departure, arrival);
        }finally{
            seats[coachnum][seatnum].unlock();
        }
    }
}
