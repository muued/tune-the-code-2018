package tune.the.code.v2;

import system.PrintSystem;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

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

    public Print() {
        MessageQueue<PrintInputData> messageQueue = MessageQueue.getInstance(PrintInputData.class);

        for (int i = 0; i < QUEUE_CONSUMER_SIZE; i++) {
            queueConsumers.add(new PrintWorker(messageQueue, latch));
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
    }

    private static class PrintWorker implements Runnable {

        private final AtomicBoolean running = new AtomicBoolean(false);
        private final MessageQueue<PrintInputData> messageQueue;
        private final CountDownLatch latch;

        private Thread worker;

        private PrintWorker(MessageQueue<PrintInputData> messageQueue, CountDownLatch latch) {
            this.messageQueue = messageQueue;
            this.latch = latch;
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
            while (running.get() || messageQueue.hasElements()) {
                PrintInputData element = messageQueue.pop();
                if (element == null) {
                    System.out.println("Print system waiting " + WAIT_ON_PRINT_QUEUE_MS + " ms for next input");
                    Utils.sleep(WAIT_ON_PRINT_QUEUE_MS);
                } else {
                    printInkassoConfirmation(element);
                }
            }
            latch.countDown();
        }

        private void printInkassoConfirmation(PrintInputData data) {
            boolean success = PrintSystem.doPrintInkassoConfirmation(data.getBank().getName(), data.getBankAccount(), data.getContractPolicyHolderName());

            if (!success) {
                System.err.println("Could not print confirmation for " + data.getBank().toString() + ", " + data.getBankAccount() + ", " + data.getContractPolicyHolderName());
            }
        }
    }
}
