package ticketingsystem;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TicketingDS implements TicketingSystem {
    private static boolean[][][][] routes;
    private static AtomicInteger[][][] ticketNum;
    private static AtomicLong tid = new AtomicLong();

    private static int totalCoach = 0;
    private static int totalSeat = 0;
    private static int totalStation = 0;
    //private static int nowCoach = 0;
    //private static int nowSeat = 0;

    private ConcurrentHashMap<Long,Ticket> sold = new ConcurrentHashMap<>();

    public TicketingDS(int routenum, int coachnum, int seatnum, int stationnum, int threadnum) {
    tid.set(0);
    totalCoach = coachnum;
    totalSeat = seatnum;
    totalStation = stationnum;

    routes = new boolean[routenum][][][];
    for(int i=0;i<routenum;i++){
        routes[i] = new boolean[coachnum][][];
        for(int j=0;j<coachnum;j++){
            routes[i][j] = new boolean[seatnum][];
            for(int k=0;k<seatnum;k++){
                routes[i][j][k] = new boolean[stationnum-1];
                for(int l=0;l<stationnum-1;l++){
                    routes[i][j][k][l] = false;
                }
            }
        }
    }
    int totalNum = coachnum * seatnum;

    ticketNum = new AtomicInteger[routenum][stationnum][stationnum];
    for(int i=0;i<routenum;i++){
        for(int j=0;j<stationnum;j++){
            for(int k=0;k<stationnum;k++){
                ticketNum[i][j][k] =  new AtomicInteger(totalNum);
            }
        }// make query faster
    }
    }

    @Override
    public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        if(departure>=arrival||route<=0||departure<=0||arrival>totalStation)return null;
        Ticket result = new Ticket();
        //int tmpCoach =0;
        //int tmpSeat = 0;
        int tmpCoach =new Random().nextInt(totalCoach);
        int tmpSeat = new Random().nextInt(totalSeat);
        int tmpD = departure - 1;
        int tmpA = arrival - 1;
        for(int i=tmpCoach+1;;i++){
            if(inquiry(route,departure,arrival)==0)return null;
            i = i%totalCoach;
            for(int j=tmpSeat+1;;j++){
                j = j%totalSeat;
                int k = tmpD;
                boolean ifSuccess = true;
                synchronized (routes[route-1][i][j]){
                    for(;k<tmpA;k++){
                        if(routes[route-1][i][j][k]){
                            ifSuccess = false;
                            break;
                        }else{
                            routes[route-1][i][j][k] = true;
                        }
                    }
                    if(ifSuccess){
                        int startP = departure-2;
                        int endP = arrival;
                        for(;startP>=0;startP--){
                            if(routes[route-1][i][j][startP]){
                                break;
                            }
                        }
                        for(;endP<totalStation;endP++){
                            //System.out.println("enP:"+endP);
                            if(routes[route-1][i][j][endP-1]){
                                break;
                            }
                        }
                        startP++;
                        endP--;
                        for(;startP<arrival-1;startP++){
                            int tA = (startP+1)>(departure)?(startP+1):(departure);
                            for(;tA<=endP;tA++){
                                ticketNum[route-1][startP][tA].getAndDecrement();
                            }
                        }
                        result.coach = i;
                        result.seat = j;
                        result.passenger = passenger;
                        result.route = route;
                        result.arrival = arrival;
                        result.departure = departure;
                        result.tid = tid.getAndIncrement();

                        sold.put(result.tid,result);
                        return result;
                    }
                    for(;k>=tmpD;k--){
                        routes[route-1][i][j][k]=false;
                    }
                }
                if(j == tmpSeat)break;
            }
            if(i == tmpCoach)break;
        }
        return null;
    }

    @Override
    public int inquiry(int route, int departure, int arrival) {
        return ticketNum[route-1][departure-1][arrival-1].get();
    }

    @Override
    public boolean refundTicket(Ticket ticket) {
        Ticket toJudge = (Ticket) sold.get(ticket.tid);
        if(toJudge == null){
            return false;
        }else{
            if(toJudge.arrival != ticket.arrival)return false;
            if(toJudge.coach != ticket.coach)return false;
            if(toJudge.departure != ticket.departure)return false;
            if(toJudge.route != ticket.route)return false;
            if(!toJudge.passenger.equals(ticket.passenger))return false;
            if(toJudge.seat != ticket.seat)return false;
        }

        int tmpCoach = ticket.coach;
        int tmpSeat = ticket.seat;
        int tmpD = ticket.departure - 1;
        int tmpA = ticket.arrival - 1;
        int tmpR = ticket.route-1;
        synchronized (routes[tmpR][tmpCoach][tmpSeat]){
            for(int i=tmpA-1;i>=tmpD;i--){
                routes[tmpR][tmpCoach][tmpSeat][i]=false;
            }
            int startP = tmpD-1;
            int endP = ticket.arrival;
            for(;startP>=0;startP--){
                if(routes[tmpR][tmpCoach][tmpSeat][startP]){
                    break;
                }
            }
            for(;endP<totalStation;endP++){
                if(routes[tmpR][tmpCoach][tmpSeat][endP-1]){
                    break;
                }
            }
            startP++;
            endP--;
            for(;startP<tmpA;startP++){
                int tA = (startP+1)>tmpD+1?(startP+1):tmpD+1;
                for(;tA<=endP;tA++){
                    ticketNum[tmpR][startP][tA].getAndIncrement();
                }
            }
        }

        sold.remove(ticket.tid);
        return true;
    }
}
