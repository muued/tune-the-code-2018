package tune.the.code.v2;

import datamodel.Bank;
import datamodel.BankAccount;
import datamodel.Contract;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @author Fabian Ohler <fabian.ohler1@rwth-aachen.de>
 * @since 27.11.2018
 */
public class PrintInputData {

    private final Bank bank;
    private final BankAccount bankAccount;
    private final String contractPolicyHolderName;

    public PrintInputData(Contract contract, Bank bank, BankAccount bankAccount) {
        this.bank = bank;
        this.bankAccount = bankAccount;
        this.contractPolicyHolderName = contract.getPolicyHolderName();
    }

    public String getContractPolicyHolderName() {
        return contractPolicyHolderName;
    }

    public Bank getBank() {
        return bank;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

}
