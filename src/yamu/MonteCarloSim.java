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

/** */
public class MonteCarloSim {
	/** Currency Formatter */
	public static final DecimalFormat FORMAT = new DecimalFormat("#.00");
	
	private Consumer<String> writer;
	private double returnMultiplier;
	private double standardDeviation;
	private double inflationMultiplier;
	private Random rng;

	private List<Double> results;
	
	MonteCarloSim(double mean, double standardDeviation, double inflationRate) {
		this.writer = (String input) -> {
			// Default Null Writer
		};
		this.returnMultiplier = 1.0 + mean / 100;
		
		if (standardDeviation < 0) {
			throw new IllegalArgumentException("Standard Deviation cannot be negative");
		}
		this.standardDeviation = standardDeviation / 100;
		this.inflationMultiplier = 1.0 + (inflationRate / 100);
		this.rng = new Random();
		
		this.results = Collections.emptyList();
	}

	/**
	 * @return mean
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
	 * @return
	 */
	public Double getResultMedian() {
		if (this.results.isEmpty()) {
			return null;
		}

		int mid = this.results.size() / 2;
		if (this.results.size() % 2 == 0) {
			return (this.results.get(mid) + this.results.get(mid + 1)) / 2;
		} else {
			return this.results.get(mid);
		}
	}
	
	/**
	 * @param percentile
	 * @return simulation value given the percentile
	 */
	public Double getResultPercentile(double percentile) {
		if (this.results.isEmpty() || percentile < 0 || percentile > 100.0) {
			return null;
		}
		
		int index = (int) (this.results.size() * (percentile / 100));
		return this.results.get(index);
	}
	
	/**
	 * Optionally write the CSV output to a file
	 * @param csvFile
	 * @throws IOException
	 */
	public void setCsvOutput(final File csvFile) throws IOException {
		if (csvFile == null) {
			setOutputStream(null);
			return;
		}
		
		final PrintStream printer = new PrintStream(csvFile);
		setOutputStream(printer);
	}
	
	/**
	 * @param printer
	 */
	public void setOutputStream(PrintStream printer) {
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
	 * @param numSims
	 * @param periods 
	 * @param start
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
		
		final int lastPeriodIndex = periods - 1;
		this.results = Stream.generate(() -> {return runOneSimulation(periods, start);})
			.limit(numSims)
			.peek(this::recordSimulation)
			.map((values) -> {return values.get(lastPeriodIndex);})
			.sorted()
			.collect(Collectors.toList());
	}
	
	private void recordSimulation(List<Double> values) {
		this.writer.accept(collectionToString(values));
    }

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
	
	private List<Double> runOneSimulation(final int years, final double start) {
		List<Double> values = new ArrayList<Double>(years);
		double result = start;
		for (int i = 0; i < years; ++i) {
			double thisYearReturn = nextGaussian();
			result = result * thisYearReturn / this.inflationMultiplier;
			values.add(result);
		}
		return values;
	}

	private double nextGaussian() {
		return this.rng.nextGaussian() * this.standardDeviation + this.returnMultiplier;
	}
}
