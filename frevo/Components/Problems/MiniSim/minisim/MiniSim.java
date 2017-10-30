package minisim;

import core.AbstractSingleProblem;
import core.AbstractRepresentation;
import helper.simMQTT;
import java.util.Hashtable;
import java.util.concurrent.Semaphore;

public class MiniSim extends AbstractSingleProblem {
	/**
	 * Interface to MQTT simulator.
	 */
	private simMQTT sim;
	
	/**
	 * A mutex that lets the evaluateCandidate function wait until the MQTT client receives a valid fitness score.
	 */
	private Semaphore lock;

	/** 
	 * Evaluates the given representation by calculating its corresponding fitness value. A higher fitness means better performance.
	 * @param candidate The candidate solution to be evaluated.
	 * @return the corresponding fitness value. */
	@Override
	public double evaluateCandidate(AbstractRepresentation candidate) {
		// create mutex
		lock = new Semaphore(1);
		
		// define requirements
		Hashtable<String, Integer> requirements = new Hashtable<String, Integer>();
		requirements.put("dimensions", 2);
		
		// run simulation
		sim = new simMQTT("minisim", candidate, requirements, getProperties(), false, lock);
		
		// wait until simulation is over and fitness is available
		double fitness = 1;
		try {
			lock.acquire();
			try {
				// get fitness from mqtt client
				fitness = sim.getFitness();
				
				// close connection to server
				sim.close();
			} finally {
				lock.release();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return fitness;
	}

	/**
	 * Simulate with visualization to introspect a certain candidate representation.
	 * @param AbstractRepresentation candidate: The candidate to introspect.
	 */
	@Override
	public void replayWithVisualization(AbstractRepresentation candidate) {
		// TODO: not yet implemented
	}
	
	/**
	 * Returns the achievable maximum fitness of this problem.
	 * A representations with this fitness value cannot be improved any further.
	 * @return double: The maximally achievable fitness. 
	 */
	public double getMaximumFitness() {
		return Double.MAX_VALUE;
	}
}
