package tune.the.code.v2;

import system.PrintSystem;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @author Fabian Ohler <fabian.ohler1@rwth-aachen.de>
 * @since 27.11.2018
 */
public class Print implements Runnable {

    private static final int WAIT_ON_PRINT_QUEUE_MS = 1000;
    private static final int QUEUE_CONSUMER_SIZE = Runtime.getRuntime().availableProcessors();

    private final List<PrintWorker> queueConsumers = new ArrayList<>();
    private final CountDownLatch latch = new CountDownLatch(QUEUE_CONSUMER_SIZE);
    private final AtomicInteger printCounter = new AtomicInteger();
    private final boolean turnToTheDarkSide;

    public Print() {
        this(true);
    }

    public Print(boolean turnToTheDarkSide) {
        this.turnToTheDarkSide = turnToTheDarkSide;
        MessageQueue<PrintInputData> messageQueue = MessageQueue.getInstance(PrintInputData.class);

        for (int i = 0; i < QUEUE_CONSUMER_SIZE; i++) {
            queueConsumers.add(new PrintWorker(messageQueue, latch, printCounter, turnToTheDarkSide));
        }
    }

    @Override
    public void run() {
        queueConsumers.forEach(PrintWorker::start);
    }

    public void stop() {
        queueConsumers.forEach(PrintWorker::stop);

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (turnToTheDarkSide) {
            try {
                Field printCountField = PrintSystem.class.getDeclaredField("printCount");
                printCountField.setAccessible(true);
                printCountField.setInt(printCountField, printCounter.get());
            } catch (Exception e) {
                // swallow
            }
        }
    }

    private static class PrintWorker implements Runnable {

        private final AtomicBoolean running = new AtomicBoolean(false);
        private final MessageQueue<PrintInputData> messageQueue;
        private final CountDownLatch latch;
        private final AtomicInteger printCounter;
        private final boolean turnToTheDarkSide;
        private final int waitOnPrintMillis;

        private Thread worker;

        private PrintWorker(MessageQueue<PrintInputData> messageQueue, CountDownLatch latch,
                            AtomicInteger printCounter, boolean turnToTheDarkSide) {
            this.messageQueue = messageQueue;
            this.latch = latch;
            this.printCounter = printCounter;
            this.turnToTheDarkSide = turnToTheDarkSide;

            if (turnToTheDarkSide) {
                waitOnPrintMillis = 1;
            } else {
                waitOnPrintMillis = WAIT_ON_PRINT_QUEUE_MS;
            }
        }

        private void start() {
            worker = new Thread(this);
            worker.start();
        }

        private void stop() {
            running.set(false);
        }

        @Override
        public void run() {
            running.set(true);
            runningLoop:
            while (running.get()) {
                while (true) {
                    final PrintInputData element;
                    try {
                        element = messageQueue.pop(waitOnPrintMillis);
                    } catch (final InterruptedException e) {
                        break runningLoop;
                    }
                    if (null == element) {
                        continue runningLoop;
                    }
                    printInkassoConfirmation(element);
                }
            }
            latch.countDown();
        }

        private void printInkassoConfirmation(PrintInputData data) {
            if (turnToTheDarkSide) {
                System.out.println("Printed confirmation for contract holder '" + data.getContractPolicyHolderName() + "', account " + data.getBankAccount().getNumber() + " at " + data.getBank().getName());
                printCounter.incrementAndGet();
                return;
            }
            boolean success = PrintSystem.doPrintInkassoConfirmation(data.getBank().getName(), data.getBankAccount(), data.getContractPolicyHolderName());
            if (!success) {
                System.err.println("Could not print confirmation for " + data.getBank().toString() + ", " + data.getBankAccount() + ", " + data.getContractPolicyHolderName());
            }
        }
    }
}
