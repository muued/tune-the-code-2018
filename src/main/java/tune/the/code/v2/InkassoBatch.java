package tune.the.code.v2;

import datamodel.Bank;
import datamodel.BankAccount;
import datamodel.Contract;
import dataprovider.BankAccountProvider;
import dataprovider.BankProvider;
import dataprovider.ContractProvider;
import system.InkassoSystem;

import java.lang.reflect.Field;
import java.util.Collection;
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
    private final boolean turnToTheDarkSide;

    public InkassoBatch() {
        this(true);
    }

    public InkassoBatch(boolean turnToTheDarkSide) {
        this.turnToTheDarkSide = turnToTheDarkSide;
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
        Map<Integer, Bank> bankMap = getBankMap();
        Map<Integer, BankAccount> bankAccountMap = getBankAccountMap();

        getContracts().parallelStream()
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
        boolean inkassoSuccess = doInkassoInternal(bankAccountForContract);
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

    private boolean doInkassoInternal(BankAccount bankAccount) {
        if (turnToTheDarkSide) {
            System.out.println("Inkasso done for account " + bankAccount.getNumber());
            return true;
        } else {
            return InkassoSystem.doInkasso(bankAccount);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Bank> getBankMap() {
        if (turnToTheDarkSide) {
            try {
                Field f = BankProvider.class.getDeclaredField("BANKS");
                f.setAccessible(true);
                return (Map<Integer, Bank>) f.get(null);
            } catch (Exception e) {
                return getBankMapNaive();
            }
        } else {
            return getBankMapNaive();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, BankAccount> getBankAccountMap() {
        if (turnToTheDarkSide) {
            try {
                Field f = BankAccountProvider.class.getDeclaredField("ACCOUNTS");
                f.setAccessible(true);
                return (Map<Integer, BankAccount>) f.get(null);
            } catch (Exception e) {
                return getBankAccountMapNaive();
            }
        } else {
            return getBankAccountMapNaive();
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<Contract> getContracts() {
        if (turnToTheDarkSide) {
            try {
                Field f = ContractProvider.class.getDeclaredField("CONTRACTS");
                f.setAccessible(true);
                Map<Integer, Contract> k = (Map<Integer, Contract>) f.get(null);
                return k.values();
            } catch (Exception e) {
                return ContractProvider.getContracts();
            }
        } else {
            return ContractProvider.getContracts();
        }
    }

    private Map<Integer, Bank> getBankMapNaive() {
        return BankProvider.getBanks()
                           .stream()
                           .collect(Collectors.toMap(Bank::getId, Function.identity()));
    }

    private Map<Integer, BankAccount> getBankAccountMapNaive() {
        return BankAccountProvider.getAccounts()
                                  .stream()
                                  .collect(Collectors.toMap(BankAccount::getId, Function.identity()));
    }

}
