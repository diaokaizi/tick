package ticketingsystem;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TicketingDS implements TicketingSystem {
	private int routeNum = 5;
	private int coachNum = 8;
	private int seatNum = 100;
	private int stationNum = 10;
	private int threadNum = 16;
	private AtomicLong tid = new AtomicLong();
	private ConcurrentHashMap<Long,Ticket> tickets = new ConcurrentHashMap<Long,Ticket>();
	private Train[] trains;

	public TicketingDS(){
		tid.set(0);
		this.trains = new Train[routeNum];
		for(int i = 0; i < routeNum; i++){
			this.trains[i] = new Train(coachNum, seatNum, stationNum);
		}
	}
	//ToDo
	public TicketingDS(int routeNum, int coachNum, int seatNum, int stationNum, int threadNum) {
		tid.set(0);
		Mask.setMask(stationNum);
		this.routeNum = routeNum;
		this.coachNum = coachNum;
		this.seatNum = seatNum;
		this.stationNum = stationNum;
		this.threadNum = threadNum;
		this.trains = new Train[routeNum];
		for(int i = 0; i < routeNum; i++){
			this.trains[i] = new Train(coachNum, seatNum, stationNum);
		}
	}

	@Override
	public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
		// TODO Auto-generated method stub
    	//返回车厢号coachnum、座位号seatnum
		int[] res = this.trains[route - 1].buyTicket(departure - 1, arrival - 1);
		if(res[0] == -1){
			return null;
		}
		Ticket ticket = new Ticket();
		ticket.tid = tid.getAndIncrement();
		ticket.passenger = passenger;
		ticket.route = route;
		ticket.coach = res[0] + 1;
		ticket.seat = res[1] + 1;
		ticket.departure = departure;
		ticket.arrival = arrival;
		tickets.put(ticket.tid, ticket);
		return ticket;
	}

	@Override
	public int inquiry(int route, int departure, int arrival) {
		// TODO Auto-generated method stub
		return this.trains[route - 1].inquriy(departure - 1, arrival - 1);
	}

	@Override
	public boolean refundTicket(Ticket ticket) {
		// TODO Auto-generated method stub
		if(ticket == null || tickets.get(ticket.tid) == null)
			return false;
		tickets.remove(ticket.tid);
		this.trains[ticket.route - 1].refundTicket(ticket.coach - 1, ticket.seat - 1, ticket.departure - 1, ticket.arrival - 1);
		return true;
	}

	@Override
	public boolean buyTicketReplay(Ticket ticket) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean refundTicketReplay(Ticket ticket) {
		// TODO Auto-generated method stub
		return false;
	}
}
