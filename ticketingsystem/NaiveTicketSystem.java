package ticketingsystem;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class NaiveTicketSystem implements TicketingSystem {
    private final int routeNum, coachNum, seatNumPerCoach, stationNum, threadNum, routeCapacity;
    private final ConcurrentHashMap<Long, Ticket>[] soldTickets;
    private final ReentrantLock lock;
    private final boolean[][][] seatStatus;
    private final AtomicLong idCount;

    public NaiveTicketSystem(int routeNum, int coachNum, int seatNumPerCoach, int stationNum, int threadNum) {
        // Verify parameters
        if (routeNum <= 0 || coachNum <= 0 || seatNumPerCoach <= 0 || stationNum <= 0 || threadNum <= 0) {
            throw new RuntimeException("Invalid parameter for TicketingDS");
        }

        // Set fields
        this.routeNum = routeNum;
        this.coachNum = coachNum;
        this.seatNumPerCoach = seatNumPerCoach;
        this.stationNum = stationNum;
        this.threadNum = threadNum;
        this.routeCapacity = coachNum * seatNumPerCoach;

        this.soldTickets = new ConcurrentHashMap[routeNum + 1];
        for (int i = 0; i <= routeNum; ++i) {
            this.soldTickets[i] = new ConcurrentHashMap<>();
        }
        lock = new ReentrantLock();

        seatStatus = new boolean[routeNum + 1][routeCapacity][stationNum + 1];
        idCount = new AtomicLong(0);
    }

    private boolean invalidParameter(int route, int departure, int arrival) {
        return route <= 0 || route > routeNum ||
                departure <= 0 || departure > stationNum ||
                arrival <= 0 || arrival > stationNum ||
                departure >= arrival;
    }

    @Override
    public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        if (invalidParameter(route, departure, arrival)) {
            throw new RuntimeException("Invalid purchase parameter!");
        }
        lock.lock();
        Ticket ticket = null;
        for (int s = 0; s < routeCapacity; ++s) {
            boolean allFree = true;
            for (int i = departure; i < arrival; ++i) {
                if (seatStatus[route][s][i]) {
                    allFree = false;
                    break;
                }
            }
            if (allFree) {
                for (int i = departure; i < arrival; ++i) {
                    seatStatus[route][s][i] = true;
                }
                int coachId = 1 + s / seatNumPerCoach;
                int seatId = 1 + s % seatNumPerCoach;
                long tid = idCount.getAndIncrement();
                ticket = TicketUtility.createTicket(tid, passenger, route, coachId, seatId, departure, arrival);
                soldTickets[route].put(tid, ticket);
                break;
            }
        }
        lock.unlock();
        return ticket;
    }

    @Override
    public int inquiry(int route, int departure, int arrival) {
        if (invalidParameter(route, departure, arrival)) {
            throw new RuntimeException("Invalid purchase parameter!");
        }
        lock.lock();
        int remain = 0;
        for (int s = 0; s < routeCapacity; ++s) {
            boolean allFree = true;
            for (int i = departure; i < arrival; ++i) {
                if (seatStatus[route][s][i]) {
                    allFree = false;
                    break;
                }
            }
            if (allFree) {
                remain++;
            }
        }
        lock.unlock();
        return remain;
    }

    @Override
    public boolean refundTicket(Ticket ticket) {
        if (invalidParameter(ticket.route, ticket.departure, ticket.arrival)) {
            throw new RuntimeException("Invalid purchase parameter!");
        }
        if (soldTickets[ticket.route].containsKey(ticket.tid)) {
            int s = (ticket.coach - 1) * seatNumPerCoach + (ticket.seat - 1);
            lock.lock();
            boolean allOccupied = true;
            for (int i = ticket.departure; i < ticket.arrival; ++i) {
                if (!seatStatus[ticket.route][s][i]) {
                    allOccupied = false;
                    break;
                }
            }
            if (allOccupied) {
                for (int i = ticket.departure; i < ticket.arrival; ++i) {
                    seatStatus[ticket.route][s][i] = false;
                }
                soldTickets[ticket.route].remove(ticket.tid);
            }
            lock.unlock();
            return allOccupied;
        }
        return false;
    }

    @Override
    public boolean buyTicketReplay(Ticket ticket) {
        return false;
    }

    @Override
    public boolean refundTicketReplay(Ticket ticket) {
        return false;
    }
}
