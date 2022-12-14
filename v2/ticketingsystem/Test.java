package ticketingsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Test {

	public static void main(String[] args) throws InterruptedException {

//		final TicketingDS tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum);

		//ToDo
		int[] threadnums = new int[]{4, 8, 16, 32, 64};
		for(int i = 0; i < threadnums.length; i++){
			List<Thread> ts = new ArrayList<Thread>();
			TestTicketingDS testds = new TestTicketingDS(threadnums[i]);
			for(int j = 0; j < threadnums[i]; j++){
				ts.add(new Thread(testds));
			}
			long start = System.currentTimeMillis();
			ts.forEach(Thread::start);
			ts.forEach(t -> {
				try{
					t.join();
				}catch(InterruptedException e) {
					e.printStackTrace();
				}
			});
			testds.getResult(start);
		}
	}
}

class TestTicketingDS implements Runnable{
	private TicketingDS tds;
	private int routenum = 5;
	private int coachnum = 8;
	private int seatnum = 100;
	private int stationnum = 10;
	private int threadnum = 16;
	private int testnum = 100000;
	private int[] function = new int[]{30, 40, 100};
	private AtomicLong[] timeConsume = new AtomicLong[3];
	private AtomicInteger[] calls = new AtomicInteger[3];
	{
		for(int i = 0; i < 3; i++){
			timeConsume[i] = new AtomicLong();
			calls[i] = new AtomicInteger();
		}
	}
	public TestTicketingDS(int threadnum){
		this.threadnum = threadnum;
		tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum);
	}
	public TestTicketingDS(int routenum, int coachnum, int seatnum, int stationnum, int threadnum, int testnum, int buyFactor, int refundFactor, int inquriyFactor) {
		tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum);
		this.routenum = routenum;
		this.coachnum = coachnum;
		this.seatnum = seatnum;
		this.stationnum = stationnum;
		this.threadnum = threadnum;
		this.testnum = testnum;
		this.function = new int[]{buyFactor, buyFactor + refundFactor,buyFactor + refundFactor + inquriyFactor};
	}

	@Override
	public void run() {
		Random rand = new Random();
		long[] timeConsume = new long[3];
		int[] calls = new int[3];
		List<Ticket> tickets = new ArrayList<>();
		for(int i = 0; i < testnum; i++){
			int factor = rand.nextInt(function[2]);
			if(factor < function[0]){
				//选择买票
				String passgenger = String.valueOf(i);
				int randomRoute = rand.nextInt(routenum) + 1;
				int randomDeparture = rand.nextInt(stationnum - 1) + 1;
				int randomArrival =randomDeparture + rand.nextInt(stationnum - randomDeparture) + 1;
				long startTime = System.currentTimeMillis();
				Ticket ticket = tds.buyTicket(passgenger, randomRoute, randomDeparture, randomArrival);
				timeConsume[0] += System.currentTimeMillis() - startTime;
				calls[0]++;
				tickets.add(ticket);
			}
			else if(factor < function[1]){
				//选择退票
				if(!tickets.isEmpty()){
					Ticket ticket = tickets.remove(rand.nextInt(tickets.size()));
					long startTime = System.currentTimeMillis();
					tds.refundTicket(ticket);
					timeConsume[1] += System.currentTimeMillis() - startTime;
				}
				calls[1]++;
			}
			else{
				//选择查询
				int randomRoute = rand.nextInt(routenum) + 1;
				int randomDeparture = rand.nextInt(stationnum - 1) + 1;
				int randomArrival =randomDeparture + rand.nextInt(stationnum - randomDeparture) + 1;
				long startTime = System.currentTimeMillis();
				tds.inquiry(randomRoute, randomDeparture, randomArrival);
				timeConsume[2] += System.currentTimeMillis() - startTime;
				calls[2]++;
			}
		}
		for(int i = 0; i < 3; i++){
			while(true){
				long old= this.timeConsume[i].get();
				if(this.timeConsume[i].compareAndSet(old, old + timeConsume[i])) {
					break;
				}
			}
			while(true){
				int old= this.calls[i].get();
				if(this.calls[i].compareAndSet(old, old + calls[i])) {
					break;
				}
			}
		}
	}

	public void getResult(long start){
		//输出结果
		long buyTicketTime = timeConsume[0].get();
		long refundTicketTime = timeConsume[1].get();
		long inquiryTime = timeConsume[2].get();

		int buyTicketCalls = calls[0].get();
		int refundTicketCalls = calls[1].get();
		int inquiryCalls = calls[2].get();
		System.out.printf("%s线程:  buyTicket平均执行时间:%.6f(ms)  refundTicket平均执行时间:%.6f(ms)  inquiry平均执行时间:%.6f(ms)  总吞吐率:%.6f(times/ms)\n",
				threadnum,(double)buyTicketTime / buyTicketCalls, (double)refundTicketTime / refundTicketCalls, (double)inquiryTime / inquiryCalls,
				(double)(buyTicketCalls + refundTicketCalls + inquiryCalls) / (System.currentTimeMillis() - start));
	}
}