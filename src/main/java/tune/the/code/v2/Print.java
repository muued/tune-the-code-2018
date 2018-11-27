package tune.the.code.v2;

import system.PrintSystem;
import utils.Utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @author Fabian Ohler <fabian.ohler1@rwth-aachen.de>
 * @since 27.11.2018
 */
public class Print implements Runnable {

    public static final int WAIT_ON_PRINT_QUEUE_MS = 1000;

    private final MessageQueue<PrintInputData> messageQueue;
    private final AtomicInteger printCount = new AtomicInteger(0);
    private boolean run = true;

    public Print() {
        this.messageQueue = MessageQueue.getInstance(PrintInputData.class);
    }

    @Override
    public void run() {
        while (run || messageQueue.hasElements()) {
            PrintInputData element = messageQueue.pop();
            if (element == null) {
                System.out.println("Print system waiting " + WAIT_ON_PRINT_QUEUE_MS + " ms for next input");
                Utils.sleep(WAIT_ON_PRINT_QUEUE_MS);
            } else {
                printInkassoConfirmation(element);
            }
        }
    }

    public void stop() {
        run = false;
    }

    private void printInkassoConfirmation(PrintInputData data) {
        boolean success = PrintSystem.doPrintInkassoConfirmation(data.getBank().getName(), data.getBankAccount(), data.getContractPolicyHolderName());

        if (success) {
            printCount.incrementAndGet();
        } else {
            System.err.println("Could not print confirmation for " + data.getBank().toString() + ", " + data.getBankAccount() + ", " + data.getContractPolicyHolderName());
        }
    }
}
