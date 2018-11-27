package tune.the.code.v1;

import datamodel.Bank;
import datamodel.BankAccount;
import datamodel.Contract;
import datamodel.PrintInput;
import dataprovider.BankProvider;
import dataprovider.ContractProvider;
import system.PrintSystem;
import utils.Utils;

import java.util.Optional;

public class Print implements Runnable {

    public static final int WAIT_ON_PRINT_QUEUE_MS = 1000;

    private static void printInkassoConfirmation(final int bankId, final BankAccount bankAccount,
                                                 final int contractId) {
        final Optional<Contract> contractOptional = ContractProvider.getContracts().stream()
                                                                    .filter(c -> c.getId() == contractId).findFirst();
        if (!contractOptional.isPresent()) {
            System.err.println("Cannot find contract for account " + bankAccount.getId());
            return;
        }

        final Contract contract = contractOptional.get();
        final Bank bank = BankProvider.getBankById(bankId);
        final boolean printSuccess = PrintSystem.doPrintInkassoConfirmation(bank.getName(), bankAccount,
                contract.getPolicyHolderName());
        if (!printSuccess) {
            System.err.println("Could not print confirmation for " + bank.toString() + ", " + bankAccount + ", "
                    + contract.getPolicyHolderName());
        }

    }

    private final MessageQueue<PrintInput> messageQueue;

    private boolean run = true;

    public Print() {
        this.messageQueue = MessageQueue.getInstance(PrintInput.class);
    }

    private void processNextElement() {
        PrintInput element = messageQueue.pop();
        while (run || element != null) {
            if (element != null) {
                printInkassoConfirmation(element.getBankId(), element.getBankAccount(), element.getContractId());
            } else {
                System.out.println("Print system waiting " + WAIT_ON_PRINT_QUEUE_MS + " ms for next input");
                Utils.sleep(WAIT_ON_PRINT_QUEUE_MS);
            }
            element = messageQueue.pop();
        }
    }

    @Override
    public void run() {
        processNextElement();
    }

    public void stop() {
        run = false;
    }
}