package tune.the.code.v2;

import datamodel.Bank;
import datamodel.BankAccount;
import datamodel.Contract;
import dataprovider.BankAccountProvider;
import dataprovider.BankProvider;
import dataprovider.ContractProvider;
import system.InkassoSystem;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @author Fabian Ohler <fabian.ohler1@rwth-aachen.de>
 * @since 27.11.2018
 */
public class InkassoBatch {

    private final AtomicInteger inkassoCount = new AtomicInteger(0);
    private final AtomicInteger printRequestsCount = new AtomicInteger(0);
    private final MessageQueue<PrintInputData> printSystemMessageQueue;

    public InkassoBatch() {
        this.printSystemMessageQueue = MessageQueue.getInstance(PrintInputData.class);
    }

    public int getInkassoCount() {
        return inkassoCount.get();
    }

    public int getPrintRequestsCount() {
        return printRequestsCount.get();
    }

    public void runInkassoBatch() {
        // 1) because looking for an id in a list is O(n)
        // 2) because their accessor methods force the thread to sleep
        Map<Integer, Contract> contractMap =
                ContractProvider.getContracts()
                                .stream()
                                .collect(Collectors.toMap(Contract::getId, Function.identity()));

        Map<Integer, Bank> bankMap =
                BankProvider.getBanks()
                            .stream()
                            .collect(Collectors.toMap(Bank::getId, Function.identity()));

        Map<Integer, BankAccount> bankAccountMap =
                BankAccountProvider.getAccounts()
                                   .stream()
                                   .collect(Collectors.toMap(BankAccount::getId, Function.identity()));

        contractMap.values()
                   .parallelStream()
                   .forEach(contract -> {
                       BankAccount bankAccountForContract = bankAccountMap.get(contract.getBankAccountId());
                       if (bankAccountForContract != null) {
                           Bank bankForBankAccount = bankMap.get(bankAccountForContract.getBankId());
                           processContract(contract, bankAccountForContract, bankForBankAccount);
                       }
                   });
    }

    private void processContract(Contract contract, BankAccount bankAccountForContract, Bank bankForBankAccount) {
        int currentInkassoCount = inkassoCount.incrementAndGet();
        boolean inkassoSuccess = InkassoSystem.doInkasso(bankAccountForContract);
        if (currentInkassoCount % 1000 == 0) {
            System.out.println("Current state: Completed " + currentInkassoCount + " Inkasso tasks");
        }

        if (!inkassoSuccess) {
            return;
        }

        if (bankForBankAccount == null) {
            System.err.println("No bank found for account " + bankAccountForContract.getNumber());
            return;
        }

        printRequestsCount.incrementAndGet();
        printSystemMessageQueue.push(new PrintInputData(contract, bankForBankAccount, bankAccountForContract));
    }
}
