package ticketingsystem;

import java.util.Objects;

public class TicketUtility {
    public static Ticket createTicket(
            long tid,
            String passenger,
            int routeId,
            int coachId,
            int seatId,
            int departure,
            int arrival) {
        Ticket ticket = new Ticket();
        ticket.tid = tid;
        ticket.passenger = passenger;
        ticket.route = routeId;
        ticket.coach = coachId;
        ticket.seat = seatId;
        ticket.departure = departure;
        ticket.arrival = arrival;
        return ticket;
    }

    public static boolean isSameTicket(Ticket a, Ticket b, boolean careID) {
        boolean flag = a.route == b.route &&
                a.coach == b.coach &&
                a.seat == b.seat &&
                a.departure == b.departure &&
                a.arrival == b.arrival &&
                a.passenger.equals(b.passenger);
        if (careID) {
            flag &= a.tid == b.tid;
        }
        return flag;
    }

    public static void printTicket(Ticket ticket) {
        printTicket(ticket, "");
    }

    public static void printTicket(Ticket ticket, String buyer) {
        if (buyer.equals("")) {
            buyer = "_";
        }
        if (ticket == null) {
            System.out.format("Thread %d -- %s: null\n", Thread.currentThread().getId(), buyer);
        } else {
            System.out.format("Thread %d -- %s: {tid=%d, passenger=%s, route=%d, coach=%d, seat=%d, departure=%d, arrival=%d}\n",
                    Thread.currentThread().getId(), buyer, ticket.tid, ticket.passenger, ticket.route, ticket.coach,
                    ticket.seat, ticket.departure, ticket.arrival);
        }
    }
}

