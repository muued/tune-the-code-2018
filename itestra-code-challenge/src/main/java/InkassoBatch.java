
import datamodel.Bank;
import datamodel.BankAccount;
import datamodel.Contract;
import datamodel.PrintInput;
import dataprovider.BankAccountProvider;
import dataprovider.BankProvider;
import dataprovider.ContractProvider;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import system.InkassoSystem;
import system.PrintSystem;
import utils.Utils;

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

class MessageQueue<T> {

	private static final Map<Class<?>, MessageQueue<?>> printSystemMessageQueue = new HashMap<>();

	public static <S> MessageQueue<S> getInstance(Class<S> clazz) {
		MessageQueue<S> queue = (MessageQueue<S>) printSystemMessageQueue.get(clazz);
		if (queue == null) {
			queue = new MessageQueue<>();
			printSystemMessageQueue.put(clazz, queue);
		}
		return queue;
	}

	private T[] buffer = null;
	private int size = 0;

	@SuppressWarnings("unchecked")
	public synchronized T pop() {
		if (size == 0) {
			return null;
		}
		final T obj = buffer[0];

		redim(--size, (Class<T>) obj.getClass());
		return obj;
	}

	@SuppressWarnings("unchecked")
	public synchronized void push(final T obj) {
		assert (obj != null);

		final int bufferLength = buffer == null ? 0 : buffer.length;
		if (size + 1 > bufferLength) {
			redim(size + 1, (Class<T>) obj.getClass());
		}
		buffer[size++] = obj;
	}

	/**
	 * Re-Dimension of buffer-array to the given size and of given type. Data of
	 * current buffer is being preserved. <br/>
	 * In case of increased buffer-size the data is left-aligned.<br/>
	 * In case of decreased buffer-size the data is right-aligned.
	 *
	 * <pre>
	 * buffer	-> method-call			-> buffer
	 * ["A"]	 -> redim(2, String.class) -> ["A", null]
	 * ["A","B"] -> redim(1, String.class) -> ["B"]
	 * </pre>
	 *
	 * @param newSize
	 * @param genericClass
	 */
	@SuppressWarnings("unchecked")
	private void redim(final int newSize, final Class<T> genericClass) {
		// create new buffer
		final T[] newBuffer = (T[]) Array.newInstance(genericClass, newSize);
		// copy data of old to new buffer if needed
		if (buffer != null && newSize > 0) {
			final int copyLength = Math.min(newSize, buffer.length);
			final int copyStartIndex = buffer.length - copyLength;
			System.arraycopy(buffer, copyStartIndex, newBuffer, 0, copyLength);
		}
		buffer = newBuffer;
	}
}

class Print implements Runnable {

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
