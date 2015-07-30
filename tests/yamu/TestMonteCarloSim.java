package yamu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Random;

import org.junit.Test;

public class TestMonteCarloSim {
	
	// override random number generator with fixed value
	static class TestRandom extends Random {
		/**	Eclipse generated serialVersionUID */
		private static final long serialVersionUID = -7301085010523088604L;

		@Override
		public double nextGaussian() {
			return 0.05;
		}
	}
	
	
	@Test
	public void testObjectConstruction() {
		MonteCarloSim mcSim = new MonteCarloSim(0.0, 10.0, 0.0);
		assertEquals(1.0, mcSim.getReturnMultiplier(), 0.0001);
		assertEquals(0.1, mcSim.getStandardDeviation(), 0.0001);
		assertEquals(1.0, mcSim.getInflationMultiplier(), 0.0001);
	}
	
	@Test
	public void testGetResultPercentilAndMedianForOneRound() {
		MonteCarloSim mcSim = new MonteCarloSim(0.0, 10.0, 0.0);
		mcSim.runSimulations(1,  1, 100.0);
		List<Double> oneSimResults = mcSim.getResults();
		
		assertNotNull(oneSimResults);
		assertEquals(1, oneSimResults.size());
		assertEquals(oneSimResults.get(0), mcSim.getResultMedian());
		assertEquals(oneSimResults.get(0), mcSim.getResultPercentile(100));
		assertEquals(mcSim.getResultMedian(), mcSim.getResultPercentile(50));
	}
	
	@Test
	public void testGetResultPercentilAndMedianForTwoRounds() {
		MonteCarloSim mcSim = new MonteCarloSim(0.0, 10.0, 0.0);
		mcSim.runSimulations(2, 1, 100.0);
		List<Double> oneSimResults = mcSim.getResults();
		
		assertNotNull(oneSimResults);
		assertEquals(2, oneSimResults.size());
		
		double median = (oneSimResults.get(0) + oneSimResults.get(1)) / 2;
		
		assertEquals(median, mcSim.getResultMedian(), 0.0001);
		assertEquals(oneSimResults.get(0), mcSim.getResultPercentile(10));
		assertEquals(oneSimResults.get(1), mcSim.getResultPercentile(90));
		assertEquals(mcSim.getResultMedian(), mcSim.getResultPercentile(50));
	}
	
	@Test
	public void testGetResultPercentilAndMedianForFourRounds() {
		MonteCarloSim mcSim = new MonteCarloSim(0.0, 10.0, 0.0);
		mcSim.runSimulations(4, 1, 100.0);
		List<Double> oneSimResults = mcSim.getResults();
		
		assertNotNull(oneSimResults);
		assertEquals(4, oneSimResults.size());
		
		double median = (oneSimResults.get(1) + oneSimResults.get(2)) / 2;
		
		assertEquals(median, mcSim.getResultMedian(), 0.0001);
		assertEquals(oneSimResults.get(0), mcSim.getResultPercentile(10));
		assertEquals(oneSimResults.get(3), mcSim.getResultPercentile(90));
		
		double percentile25 = (oneSimResults.get(0) + oneSimResults.get(1)) / 2;
		assertEquals(percentile25, mcSim.getResultPercentile(25), 0.0001);
		
		double percentile75 = (oneSimResults.get(2) + oneSimResults.get(3)) / 2;
		assertEquals(percentile75, mcSim.getResultPercentile(75), 0.0001);
		assertEquals(mcSim.getResultMedian(), mcSim.getResultPercentile(50));
	}
	
	@Test
	public void testStandardDeviationSimulationOneYear() {
		MonteCarloSim mcSim = new MonteCarloSim(0.0, 10.0, 0.0);

		mcSim.rng = new TestRandom();
		
		mcSim.runSimulations(1, 1, 100);
		List<Double> simResults = mcSim.getResults();

		assertNotNull(simResults);
		assertEquals(1, simResults.size());
		
		assertEquals(100.5, mcSim.getResultMedian(), 0.0001);
	}
	
	@Test
	public void testStandardDeviationSimulationTwoYears() {
		MonteCarloSim mcSim = new MonteCarloSim(0.0, 10.0, 0.0);

		mcSim.rng = new TestRandom();
		
		mcSim.runSimulations(1, 2, 100);
		List<Double> simResults = mcSim.getResults();

		assertNotNull(simResults);
		assertEquals(1, simResults.size());
		
		assertEquals(100.5 * 1.005, mcSim.getResultMedian(), 0.0001);
	}
	
	@Test
	public void testPositiveAverageReturnTwoRoundsTwoYears() {
		MonteCarloSim mcSim = new MonteCarloSim(10.0, 0.0, 0.0);
		mcSim.rng = new TestRandom();
		
		mcSim.runSimulations(2, 2, 100);
		List<Double> simResults = mcSim.getResults();
		
		assertEquals(2, simResults.size());
		
		for (double oneSim : simResults) {
			assertEquals(110.0 * 1.10, oneSim, 0.0001);
		}
	}
	
	@Test
	public void testNegativeAverageReturnTwoRoundsTwoYears() {
		MonteCarloSim mcSim = new MonteCarloSim(-10.0, 0.0, 0.0);
		mcSim.rng = new TestRandom();
		
		mcSim.runSimulations(2, 2, 100);
		List<Double> simResults = mcSim.getResults();
		
		assertEquals(2, simResults.size());
		
		for (double oneSim : simResults) {
			assertEquals(90.0 * 0.90, oneSim, 0.0001);
		}
	}
	
	@Test
	public void testYearlyInflationRate() {
		MonteCarloSim mcSim = new MonteCarloSim(0.0, 0.0, 10.0);
		mcSim.rng = new TestRandom();
		
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		PrintStream stream = new PrintStream(byteOutput);
		mcSim.setOutputStream(stream);
		mcSim.runSimulations(1, 3, 100);
		
		assertEquals("90.91,82.64,75.13\n", byteOutput.toString());
	}
	
	@Test
	public void testSimulationForManyYears() {
		MonteCarloSim mcSim = new MonteCarloSim(5.0, 10.0, 3.5);
		mcSim.rng = new TestRandom();
		
		mcSim.runSimulations(1, 10, 100);
		List<Double> simResults = mcSim.getResults();

		assertEquals(100.0 * Math.pow((1.05 + (0.1 * 0.05)) / 1.035, 10), simResults.get(0), 0.0001);
	}
}
