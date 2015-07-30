package yamu;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class for performing forward-looking Monte Carlo Simulations
 */
public class MonteCarloSim {
	/** Currency Formatter */
	public static final DecimalFormat FORMAT = new DecimalFormat("#.00");
	
	private Consumer<String> writer;
	private double returnMultiplier;
	private double standardDeviation;
	private double inflationMultiplier;
	
	// Random number generator is package private to make testing easier
	Random rng;

	private List<Double> results;
	
	/**
	 * Constructs a MonteCarloSim object that can run Monte Carlo simulations.
	 * The rate of return is modeled as a Gaussian distribution with the given
	 * {@code mean} and {@code standardDeviation}.
	 * The simulation results are adjusted by the provided {@code inflationRate}.
	 * @param mean gaussian mean as a percentage
	 * @param standardDeviation gaussian standard deviation as a percentage
	 * @param inflationRate yearly inflation rate as a percentage
	 */
	MonteCarloSim(double mean, double standardDeviation, double inflationRate) {
		if (standardDeviation < 0) {
			throw new IllegalArgumentException("Standard Deviation cannot be negative");
		}
		
		this.writer = (String input) -> {
			// Default Null Writer
		};
		
		// the mean return multiplier
		this.returnMultiplier = 1.0 + mean / 100;
		
		this.standardDeviation = standardDeviation / 100;
		
		// the inflation multiplier
		this.inflationMultiplier = 1.0 + (inflationRate / 100);
		
		this.rng = new Random();
		
		this.results = Collections.emptyList();
	}

	/**
	 * @return mean return multiplier
	 */
	public double getReturnMultiplier() {
	    return this.returnMultiplier;
    }

	/**
	 * @return standard deviation
	 */
	public double getStandardDeviation() {
	    return this.standardDeviation;
    }
	
	/**
	 * @return inflation multiplier
	 */
	public double getInflationMultiplier() {
		return this.inflationMultiplier;
	}
	
	/**
	 * @return
	 */
	public List<Double> getResults() {
		return this.results;
	}
	
	/**
	 * Get the median result value
	 * @return
	 */
	public Double getResultMedian() {
		if (this.results.isEmpty()) {
			return null;
		}
		
		// If list is even get the average of middle two, else get the middle one
		int mid = this.results.size() / 2;
		if (this.results.size() % 2 == 0) {
			return (this.results.get(mid - 1) + this.results.get(mid)) / 2;
		} else {
			return this.results.get(mid);
		}
	}
	
	/**
	 * Get the percentile value of the simulation results.
	 * @param percentile
	 * @return simulation value given the percentile
	 */
	public Double getResultPercentile(double percentile) {
		if (this.results.isEmpty() || percentile < 0 || percentile > 100.0) {
			return null;
		}
		
		double doubleIndex = this.results.size() * (percentile / 100);
		int index = (int)doubleIndex;
		// Note that index will be promoted to a double here
		// If it matches with doubleIndex we have an exact index
		// and should therefore take an average of two adjacent values.
		if (index == this.results.size()) {
			return this.results.get(index - 1);
		} else if (index == doubleIndex) {
			return (this.results.get(index) + this.results.get(index - 1)) / 2;
		} else {
			return this.results.get(index);
		}
	}
	
	/**
	 * Optionally write the CSV output to a file
	 * @param csvFile
	 * @throws IOException
	 */
	public void setCsvOutput(final File csvFile) throws IOException {
		// If outputFile is null, do nothing, else print simulation data to file
		if (csvFile == null) {
			setOutputStream(null);
			return;
		}
		// stream printer to output string to file
		final PrintStream printer = new PrintStream(csvFile);
		setOutputStream(printer);
	}
	
	/**
	 * Set the output stream for the full simulation results.
	 * Set the printer to {@code null} to not have any output.
	 * @param printer the output print stream
	 */
	public void setOutputStream(final PrintStream printer) {
		if (printer == null) {
			this.writer = (String input) -> {
				// Null Writer
			};
			return;
		}

		this.writer = (String input) -> {
			try {
				printer.print(input);
            } catch (Exception e) {
            	throw e; // Re-throw the exception
            }
		};
	}
	
	/**
	 * @param numSims number of simulations to run
	 * @param periods the number of periods to run for each simulation
	 * @param start the starting investment of each simulation run
	 */
	public void runSimulations(final int numSims, final int periods, final double start) {
		if (start <= 0.0) {
			throw new IllegalArgumentException("Can't start with a negative investment value");
		}
		if (numSims < 1) {
			throw new IllegalArgumentException("Number of simulation runs must be positive");
		}
		if (periods < 1) {
			throw new IllegalArgumentException("Number of simulation periods must be positive");
		}
		// the index for the periods th year's return value
		final int lastPeriodIndex = periods - 1;
		
		// generate numSims simulations with each one has periods return values
		// record the simulation values to writer 
		// get the last year's balance and then sort the list
		// finally store into results list
		this.results = Stream.generate(() -> {return runOneSimulation(periods, start);})
			.limit(numSims)
			.peek(this::recordSimulation)
			.map((values) -> {return values.get(lastPeriodIndex);})
			.sorted()
			.collect(Collectors.toList());
	}
	
	/**
	 * Print a single simulation to writer
	 * @param values
	 */
	private void recordSimulation(List<Double> values) {
		this.writer.accept(collectionToString(values));
    }
	
	/**
	 * Convert a list of doubles to comma separated String 
	 * @param list
	 * @return String with comma separated values from <i>list</i>
	 */
	private String collectionToString(Collection<Double> list) {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (Double item : list) {
			if (first) {
				first = false;
			} else {
				builder.append(',');
			}
			builder.append(FORMAT.format(item));
		}
		builder.append('\n');
		return builder.toString();
	}
	
	/**
	 * Run a single simulation
	 * @param years number of years to simulate over
	 * @param start the starting investment value
	 * @return yearly balance adjusted for inflation
	 */
	private List<Double> runOneSimulation(final int years, final double start) {

		List<Double> values = new ArrayList<Double>(years);
		double result = start;

		for (int i = 0; i < years; ++i) {
			// generate this year's return multiplier sampled from a Gaussian distribution
			double thisYearReturn = nextGaussian();
			// multiply last year's balance with the return multiplier and then divide by inflation rate
			result = result * thisYearReturn / this.inflationMultiplier;
			// add each year's return to simulation results
			values.add(result);
		}
		return values;
	}

	private double nextGaussian() {
		// generate random number from standard Gaussian distribution
		// and adjust to the multiplier used by our current simulation
		return this.rng.nextGaussian() * this.standardDeviation + this.returnMultiplier;
	}
}
