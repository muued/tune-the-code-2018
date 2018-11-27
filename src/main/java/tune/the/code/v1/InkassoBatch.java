package tune.the.code.v1;

import datamodel.Bank;
import datamodel.BankAccount;
import datamodel.Contract;
import datamodel.PrintInput;
import dataprovider.BankAccountProvider;
import dataprovider.BankProvider;
import dataprovider.ContractProvider;
import system.InkassoSystem;

public class InkassoBatch {

	private int inkassoCount = 0;

	private int printRequestsCount = 0;
	private final MessageQueue<PrintInput> printSystemMessageQueue;

	public InkassoBatch() {
		this.printSystemMessageQueue = MessageQueue.getInstance(PrintInput.class);
	}

	public int getInkassoCount() {
		return inkassoCount;
	}

	public int getPrintRequestsCount() {
		return printRequestsCount;
	}

	public void runInkassoBatch() {

		for (final Contract contract : ContractProvider.getContracts()) {
			// find bank account for contract
			BankAccount bankAccountForContract = null;

			for (final BankAccount bankAccount : BankAccountProvider.getAccounts()) {
				if (contract.getBankAccountId() != null
						&& contract.getBankAccountId().intValue() == bankAccount.getId()) {
					bankAccountForContract = bankAccount;
				}
			}

			if (bankAccountForContract != null) {
				inkassoCount++;
				final boolean inkassoSuccess = InkassoSystem.doInkasso(bankAccountForContract);
				if (inkassoCount % 1000 == 0) {
					System.out.println("Current state: Completed " + inkassoCount + " Inkasso tasks");
				}

				if (inkassoSuccess) {
					final Bank bankForBankAccount = BankProvider.getBankById(bankAccountForContract.getBankId());
					if (bankForBankAccount == null) {
						System.err.println("No bank found for account " + bankAccountForContract.getNumber());
						return;
					}

					printRequestsCount++;
					printSystemMessageQueue
							.push(new PrintInput(bankForBankAccount.getId(), bankAccountForContract,
									contract.getId()));
				}
			}
		}
	}
}