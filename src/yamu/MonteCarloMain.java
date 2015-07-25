package yamu;

import java.io.File;
import java.io.IOException;

/** */
public class MonteCarloMain {
	/**
	 * @param sim
	 * @param outputFile
	 */
	public static void runSimulation(MonteCarloSim sim, File outputFile) {
		try {
			sim.setCsvOutput(outputFile);
		} catch (IOException e) {
			System.err.println("Unable to open the CSV output file " + outputFile.toString());
			e.printStackTrace(System.err);
			System.exit(1);
		}
		int numSims = 10000;
		int numPeriods = 20;
		System.out.println(String.format("Running %s simulations for %s periods", numSims, numPeriods));
		double startingInvestment = 100000.0;
		System.out.println(String.format("Starting Investment: %.2f", startingInvestment));

		long startTime = System.currentTimeMillis();
		sim.runSimulations(numSims, numPeriods, startingInvestment);
		long duration = System.currentTimeMillis() - startTime;

		System.out.println(String.format("Median         : %.2f", sim.getResultMedian()));
		System.out.println(String.format("10%% Best Case : %.2f", sim.getResultPercentile(90)));
		System.out.println(String.format("10%% Worst Case: %.2f", sim.getResultPercentile(10)));
		System.out.println("Runtime: " + duration + "ms");
		System.out.println();
	}
	
	public static void main(String args[]) {
		System.out.println("Running Aggressive Simulation");
		MonteCarloSim aggressiveSim = new MonteCarloSim(9.4324, 15.675, 3.5);
		// runSimulation(aggressiveSim, new File("output_aggressive.csv"));
		runSimulation(aggressiveSim, null);
		
		System.out.println("Running Very Conservative Simulation");
		MonteCarloSim conservativeSim = new MonteCarloSim(6.189, 6.3438, 3.5);
		// runSimulation(conservativeSim , new File("output_conservative.csv"));
		runSimulation(conservativeSim , null);
	}
}
