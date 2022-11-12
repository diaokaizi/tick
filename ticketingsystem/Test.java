package ticketingsystem;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

class TestUtility {
    public static final int BUY = 0, REFUND = 1, QUERY = 2;

    public static int getRandomOpType() {
        int opType;
        int rV = ThreadLocalRandom.current().nextInt(100);
        if (rV < 20) {
            opType = BUY;
        } else if (rV < 30) {
            opType = REFUND;
        } else {
            opType = QUERY;
        }
        return opType;
    }

    public static int[] getRandomConfig(int routeNum, int stationNum) {
        Random random = ThreadLocalRandom.current();
        int route = random.nextInt(routeNum) + 1;
        int departure = random.nextInt(stationNum) + 1;
        int arrival = random.nextInt(stationNum) + 1;
        while (arrival == departure) {
            arrival = random.nextInt(stationNum) + 1;
        }
        if (arrival < departure) {
            int tmp = arrival;
            arrival = departure;
            departure = tmp;
        }
        return new int[]{route, departure, arrival};
    }
}

class CorrectnessTest {
    private static boolean singleThreadTest(TicketingSystem systemA, TicketingSystem systemB,
                                            int checkTimes, int routeNum, int stationNum, boolean ignoreError) {
        Random random = new Random();
        boolean flag = true;
        long threadId = Thread.currentThread().getId();
        LinkedList<Ticket> aBought = new LinkedList<>(), bBought = new LinkedList<>();
        for (int i = 0; i < checkTimes && (flag); ++i) {
            int opType = TestUtility.getRandomOpType();
            String passengerName = "TEST_USER";
            int[] config = TestUtility.getRandomConfig(routeNum, stationNum);
            int route, departure, arrival;
            route = config[0];
            departure = config[1];
            arrival = config[2];
            switch (opType) {
                case TestUtility.BUY:
                    Ticket ticketA = systemA.buyTicket(passengerName, route, departure, arrival);
                    Ticket ticketB = systemB.buyTicket(passengerName, route, departure, arrival);
                    if (ticketA != null && ticketB != null) {
                        aBought.add(ticketA);
                        bBought.add(ticketB);
                    } else if (ignoreError) {
                        // In concurrent situation, the last ticket might be bought by another thread.
                        // Just keep current thread doing the same to 2 systems.
                        if (ticketA == null && ticketB != null) {
                            systemB.refundTicket(ticketB);
                            ticketB = null;
                        } else if (ticketA != null && ticketB == null) {
                            systemA.refundTicket(ticketA);
                            ticketA = null;
                        }
                    }
                    flag = (ticketA == null && ticketB == null) || (ticketA != null && ticketB != null);
                    if (!flag) {
                        System.out.format("Thread %d -- Error when testing at %d\n", threadId, i);
                        System.out.format("Thread %d -- Buy: passenger=%s, route=%d, departure=%d, arrival=%d\n",
                                threadId, passengerName, route, departure, arrival);
                        TicketUtility.printTicket(ticketA, "A");
                        TicketUtility.printTicket(ticketB, "B");
                        System.out.format("Thread %d -- Inquiring remaining tickets now. A: %d, B: %d\n",
                                threadId, systemA.inquiry(route, departure, arrival), systemB.inquiry(route, departure, arrival));
                    }
                    break;
                case TestUtility.REFUND:
                    if (!aBought.isEmpty() && !bBought.isEmpty()) {
                        int ind = random.nextInt(aBought.size());
                        Ticket ticketABought = aBought.get(ind);
                        Ticket ticketBBought = bBought.get(ind);
                        boolean aSuccess = systemA.refundTicket(ticketABought);
                        boolean bSuccess = systemB.refundTicket(ticketBBought);
                        aBought.remove(ind);
                        bBought.remove(ind);
                        flag = (aSuccess == bSuccess);
                        if (!flag) {
                            System.out.format("Thread %d -- Error when testing at %d\n", threadId, i);
                            System.out.format("Thread %d -- Refund:\n", threadId);
                            System.out.format("Thread %d -- A: %b, B: %b\n", threadId, aSuccess, bSuccess);
                            System.out.format("Thread %d -- Query for now:\n", threadId);
                            System.out.format("Thread %d -- A: %d, B: %d\n",
                                    threadId,
                                    systemA.inquiry(ticketABought.route, ticketABought.departure, ticketABought.arrival),
                                    systemB.inquiry(ticketBBought.route, ticketBBought.departure, ticketBBought.arrival));
                            TicketUtility.printTicket(ticketABought);
                            TicketUtility.printTicket(ticketBBought);
                        }
                    }
                    break;
                case TestUtility.QUERY:
                    int aResult = systemA.inquiry(route, departure, arrival);
                    int bResult = systemB.inquiry(route, departure, arrival);
                    flag = (aResult == bResult) || ignoreError;
                    if (!flag) {
                        System.out.format("Thread %d -- Error when testing at %d\n", threadId, i);
                        System.out.format("Thread %d -- Query: route=%d, departure=%d, arrival=%d\n",
                                threadId, route, departure, arrival);
                        System.out.format("Thread %d -- A: %d, B: %d\n", threadId, aResult, bResult);
                    }
                    break;
                default: // Impossible
                    break;
            }
        }
        return flag;
    }

    private static boolean compareTicketSystemConcurrent(TicketingSystem systemA, TicketingSystem systemB, int checkTimes,
                                                         int threadNum, int routeNum, int stationNum)
            throws InterruptedException {
        System.out.format("Testing concurrent correctness with %d thread(s)...\n", threadNum);
        System.out.format("A = %s\n", systemA.getClass().getSimpleName());
        System.out.format("B = %s\n", systemB.getClass().getSimpleName());

        ExecutorService executor = Executors.newCachedThreadPool();
        for (int i = 0; i < threadNum; ++i) {
            executor.execute(() -> singleThreadTest(systemA, systemB, checkTimes, routeNum, stationNum, true));
        }
        executor.shutdown();
        if (!executor.awaitTermination(180, TimeUnit.SECONDS)) {
            System.out.println("Timeout when waiting concurrent tests finish.");
            return false;
        }

        boolean flag = true;
        System.out.println("Inquiry results after concurrent tests...");
        for (int route = 1; route <= routeNum; ++route) {
            for (int departure = 1; departure < stationNum; ++departure) {
                int arrival = departure + 1;
                int resA = systemA.inquiry(route, departure, arrival);
                int resB = systemB.inquiry(route, departure, arrival);
                if (resA != resB) {
                    System.out.format("route: %d, departure: %d, arrival: %d, A: %d, B: %d\n",
                            route, departure, arrival, resA, resB);
                }
                flag &= resA == resB;
            }
        }
        return flag;
    }


    private static boolean compareTicketSystemSequential(TicketingSystem systemA, TicketingSystem systemB,
                                                         int checkTimes, int routeNum, int stationNum) {
        System.out.println("Testing sequential correctness...");
        System.out.format("A = %s\n", systemA.getClass().getSimpleName());
        System.out.format("B = %s\n", systemB.getClass().getSimpleName());

        boolean flag = singleThreadTest(systemA, systemB, checkTimes, routeNum, stationNum, false);
        System.out.println("Inquiry results after sequential tests...");
        for (int route = 1; route <= routeNum; ++route) {
            for (int departure = 1; departure < stationNum; ++departure) {
                int arrival = departure + 1;
                int resA = systemA.inquiry(route, departure, arrival);
                int resB = systemB.inquiry(route, departure, arrival);
                if (resA != resB) {
                    System.out.format("route: %d, departure: %d, arrival: %d, A: %d, B: %d\n",
                            route, departure, arrival, resA, resB);
                }
                flag &= resA == resB;
            }
        }
        return flag;
    }

    public static void testSequential(int routeNum, int coachNum, int seatNum, int stationNum, int threadNum) {
        TicketingSystem naiveDS;
        final LinkedList<TicketingSystem> systems = new LinkedList<>();
        systems.add(new TicketingDS(routeNum, coachNum, seatNum, stationNum, threadNum));

        int checkTimes = 100000;
        boolean sequentialResult;
        for (TicketingSystem system : systems) {
            naiveDS = new NaiveTicketSystem(routeNum, coachNum, seatNum, stationNum, threadNum);
            sequentialResult = compareTicketSystemSequential(naiveDS, system, checkTimes, routeNum, stationNum);
            if (sequentialResult) {
                System.out.println("[YES]");
                System.out.format("Our %s has the same results with naive baseline!\n\n"
                        , system.getClass().getSimpleName());
            } else {
                System.out.println("[No]");
                System.out.format("Our %s has different results with naive baseline!\n\n"
                        , system.getClass().getSimpleName());
            }
        }
    }

    public static void testConcurrent(int routeNum, int coachNum, int seatNum, int stationNum, int threadNum)
            throws InterruptedException {
        TicketingSystem naiveDS;
        final LinkedList<TicketingSystem> systems = new LinkedList<>();
        systems.add(new TicketingDS(routeNum, coachNum, seatNum, stationNum, threadNum));

        int checkTimes = 100000;
        boolean concurrentResult;
        for (TicketingSystem system : systems) {
            naiveDS = new NaiveTicketSystem(routeNum, coachNum, seatNum, stationNum, threadNum);
            concurrentResult = compareTicketSystemConcurrent(naiveDS, system, checkTimes, threadNum, routeNum, stationNum);
            if (concurrentResult) {
                System.out.println("[YES]");
                System.out.format("Our %s has the same results with naive baseline!\n\n",
                        system.getClass().getSimpleName());
            } else {
                System.out.println("[No]");
                System.out.format("Our %s has different results with naive baseline!\n\n",
                        system.getClass().getSimpleName());
            }
        }
    }
}

class PerformanceTest {
    private static void singleThreadPerfTask(TicketingSystem system, int routeNum, int stationNum, int repeatTimes) {
        Random random = new Random();
        ArrayList<Ticket> boughtTickets = new ArrayList<>();
        for (int rd = 0; rd < repeatTimes; ++rd) {
            int[] config = TestUtility.getRandomConfig(routeNum, stationNum);
            int route, departure, arrival;
            route = config[0];
            departure = config[1];
            arrival = config[2];
            String passengerName = "PERF_USER";

            int opType = TestUtility.getRandomOpType();
            switch (opType) {
                case TestUtility.BUY: {
                    Ticket ticket = system.buyTicket(passengerName, route, departure, arrival);
                    if (ticket != null) {
                        boughtTickets.add(ticket);
                    }
                    break;
                }
                case TestUtility.QUERY: {
                    system.inquiry(route, departure, arrival);
                    break;
                }
                case TestUtility.REFUND: {
                    if (!boughtTickets.isEmpty()) {
                        int id = random.nextInt(boughtTickets.size());
                        Ticket ticket = boughtTickets.get(id);
                        boughtTickets.remove(id);
                        system.refundTicket(ticket);
                    }
                    break;
                }
                default: // Impossible!
                    break;
            }
        }
    }

    private static void singleThreadLatencyTask(TicketingSystem system, int routeNum, int stationNum, int repeatTimes) {
        Random random = new Random();
        ArrayList<Ticket> boughtTickets = new ArrayList<>();
        long buyTime = 0, refundTime = 0, inquiryTime = 0;
        long buyCount = 0, refundCount = 0, inquiryCount = 0;
        for (int rd = 0; rd < repeatTimes; ++rd) {
            int[] config = TestUtility.getRandomConfig(routeNum, stationNum);
            int route, departure, arrival;
            route = config[0];
            departure = config[1];
            arrival = config[2];
            String passengerName = "PERF_USER";

            int opType = TestUtility.getRandomOpType();
            switch (opType) {
                case TestUtility.BUY: {
                    long start = System.nanoTime();
                    Ticket ticket = system.buyTicket(passengerName, route, departure, arrival);
                    long end = System.nanoTime();
                    buyCount++;
                    buyTime += end - start;
                    if (ticket != null) {
                        boughtTickets.add(ticket);
                    }
                    break;
                }
                case TestUtility.QUERY: {
                    long start = System.nanoTime();
                    system.inquiry(route, departure, arrival);
                    long end = System.nanoTime();
                    inquiryCount++;
                    inquiryTime += end - start;
                    break;
                }
                case TestUtility.REFUND: {
                    if (!boughtTickets.isEmpty()) {
                        int id = random.nextInt(boughtTickets.size());
                        Ticket ticket = boughtTickets.get(id);
                        boughtTickets.remove(id);
                        long start = System.nanoTime();
                        system.refundTicket(ticket);
                        long end = System.nanoTime();
                        refundCount++;
                        refundTime += end - start;
                    }
                    break;
                }
                default: // Impossible!
                    break;
            }
        }
        System.out.format("Latency: buy = %f us, refund = %f us, inquiry = %f us\n",
                (double) buyTime / 1000 / buyCount, (double) refundTime / 1000 / refundCount, (double) inquiryTime / 1000 / inquiryCount);
    }

    private static void testThroughputOne(TicketingSystem system, int routeNum, int stationNum, int threadNum, int repeatTimes) throws InterruptedException {
        System.out.format("Starting test throughput for %s with %d thread(s)...\n",
                system.getClass().getSimpleName(), threadNum);
        Thread thread = new Thread(() -> {
            singleThreadPerfTask(system, routeNum, stationNum, repeatTimes);
        });
        long start = System.nanoTime();
        thread.start();
        thread.join();
        long end = System.nanoTime();
        double throughput = repeatTimes * threadNum / ((double) (end - start) / 1000 / 1000);
        System.out.format("Throughput: %f op/ms\n", throughput);
    }

    private static void testLatencyOne(TicketingSystem system, int routeNum, int stationNum, int threadNum, int repeatTimes) throws InterruptedException {
        System.out.format("Starting test latency for %s with %d thread(s)...\n",
                system.getClass().getSimpleName(), threadNum);
        Thread thread = new Thread(() -> {
            singleThreadLatencyTask(system, routeNum, stationNum, repeatTimes);
        });
        long start = System.nanoTime();
        thread.start();
        thread.join();
        long end = System.nanoTime();
        double throughput = repeatTimes * threadNum / ((double) (end - start) / 1000 / 1000);
    }

    private static void testAll(int routeNum, int coachNum, int seatNum, int stationNum, int threadNum) throws InterruptedException {
        TicketingSystem tds = new TicketingDS(routeNum, coachNum, seatNum, stationNum, threadNum);
        int repeatTimes = 100000;
        testThroughputOne(tds, routeNum, stationNum, threadNum, repeatTimes);
        tds = new TicketingDS(routeNum, coachNum, seatNum, stationNum, threadNum);
        testLatencyOne(tds, routeNum, stationNum, threadNum, repeatTimes);
    }

    public static void testPerformance(int routeNum, int coachNum, int seatNum, int stationNum, int threadNum) throws InterruptedException {
        testAll(routeNum, coachNum, seatNum, stationNum, threadNum);
    }
}

public class Test {
    public static void main(String[] args) throws InterruptedException {
        int routeNum = 10, coachNum = 10, seatNum = 100, stationNum = 20, threadNum = 6;

        if (args.length == 1) {
            try {
                threadNum = Integer.parseInt(args[0]);
            } catch (Exception ignored) {
            }
        }

        CorrectnessTest.testSequential(routeNum, coachNum, seatNum, stationNum, threadNum);
        CorrectnessTest.testConcurrent(routeNum, coachNum, seatNum, stationNum, threadNum);

        PerformanceTest.testPerformance(routeNum, coachNum, seatNum, stationNum, threadNum);
    }
}
