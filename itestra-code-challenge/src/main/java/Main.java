
import config.Config;
import dataprovider.ContractProvider;
import dataprovider.DataSet;
import system.PrintSystem;

class Main {

	/*
	 * Do not change main. This is for testing purposes only.
	 */
	public static void main(final String[] args) {

		// Mini Set for fast Testing. Runs for about 15 seconds
		// DataSet.init(Config.DATA_SET_SIZE_MINI);
		// Medium Set for more realistic testing. Runs for about 3 minutes
		DataSet.init(Config.DATA_SET_SIZE_MEDIUM);
		// Large Set for thorough but still reasonable testing. Runs for about 14 minutes
		// DataSet.init(Config.DATA_SET_SIZE_CHALLENGE);
		// Custom set example
		// DataSet.init(new GeneratorConfig(80, 10, 10, 0.6));

		System.out.println(
				"Expecting " + ContractProvider.getContractsWithBankAccountsCount() + " inkasso & print requests");

		final long startTime = System.currentTimeMillis();
		final Print printSystem = new Print();
		final Thread printSystemThread = new Thread(printSystem);
		printSystemThread.start();

		final InkassoBatch inkassoBatch = new InkassoBatch();
		inkassoBatch.runInkassoBatch();

		printSystem.stop();

		try {
			printSystemThread.join();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
		final long finishTime = System.currentTimeMillis();
		System.out.println("Inkasso count: " + inkassoBatch.getInkassoCount());
		System.out.println("Print requests count: " + inkassoBatch.getPrintRequestsCount());
		System.out.println("Print count: " + PrintSystem.getPrintCount());
		if (inkassoBatch.getInkassoCount() != inkassoBatch.getPrintRequestsCount()
				|| inkassoBatch.getInkassoCount() != ContractProvider.getContractsWithBankAccountsCount()) {
			System.err.println("Probably incorrect computation, Inkasso & Print counts not equal expected results");
		}
		System.out.println("Task completed in " + (finishTime - startTime) + " ms");
	}

}
