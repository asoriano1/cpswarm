package minisim;

/**
 * A simple command line simulator to demonstrate the requirements of the simulator API.
 * The agents must reach the goal before being caught by the defenders.
 *
 * Assumptions:
 * - discrete time
 * - continuous space
 * - agents loose when they touch a defender
 * - agents win when they touch the goal
 * - agents and defenders move at constant speed in a given angle
 * - agents move faster than defenders
 * - agents have four sensors: 0: east, 1: north, 2: west, 3: south
 * - the fitness of a simulation is calculated as the negative sum of the distance of each agent to the goal, maximum is 0
 *
 * @author Micha Rappaport
 */
public class minisim {
	/**
	 * Main function for running the simulation.
	 * @param args
	 */
	public static void main(String[] args) {
		boolean replay = true;
		
		// TODO: when i receive "simulate and replay" from algorithm optimization tool
		if (replay) {
			minisim sim = new minisim();
			System.out.println(sim.helloMessage);
			try {
				// run simulation with gui
				sim.runSim(true);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// TODO: when i receive "simulate and evaluate" from algorithm optimization tool
		else {
			minisim sim = new minisim();
			System.out.println(sim.helloMessage);
			try {
				// run simulation without gui
				sim.runSim(false);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// calculate fitness
			// TODO: send to algorithm optimization tool
			double fitness = sim.calcFitness();
		}
	}
	
	//
	// Simulation parameters.
	// They are passed from the algorithm optimization tool.
	// Their names are predefined for a problem.
	//
	/**
	 * A custom welcome message.
	 */
	private String helloMessage;
	/**
	 * Width of the map.
	 */
	private Integer mapWidth;
	/**
	 * Height of the map.
	 */
	private Integer mapHeight;
	/**
	 * Number of agents.
	 */
	private Integer numAgents;
	/**
	 * Number of defending agents.
	 */
	private Integer numDefenders;
	/**
	 * Distance that an agent travels in one time step.
	 */
	private double speedAgents;
	/**
	 * Distance that a defending agent travels in one time step.
	 */
	private double speedDefenders;
	/**
	 * Milliseconds between two visualizations.
	 */
	private Integer stepSize;
	/**
	 * Maximum number of steps that are simulated.
	 */
	private Integer maxSteps;
	
	//
	// Internal variables.
	//
	/**
	 * Position of each agent.
	 */
	private double[][] agents;
	/**
	 * Position of each defending agent.
	 */
	private double[][] defenders;
	/**
	 * Position of goal.
	 */
	private double[] goal;
	/**
	 * Size of a point (goal, agent).
	 */
	private double pointSize;
	/**
	 * Cells that construct the output map.
	 */
	enum cell {
		FREE,
		OBSTACLE,
		GOAL,
		AGENT,
		DEFENDER
	}
	/**
	 * A sequence of angles that lets one agent escape (for testing).
	 */
	private double[] solution;

	
	/**
	 * Constructor.
	 */
	public minisim() {
		initSim();
	}

	/**
	 * Initialize the simulation.
	 */
	private void initSim() {
		// TODO: get parameters from optimization tool
		helloMessage = "Welcome to minisim!";
		mapWidth = 13;
		mapHeight = 7;
		numAgents = 1;
		numDefenders = 1;
		speedAgents = 1;
		speedDefenders = 0.33;
		stepSize = 250;
		maxSteps = 15;
		
		// init internal parameters
		pointSize = 1.0;
		//solution = new double[] {1.2, 1, 0.6, 0.4, 0.2, 0.0, 0.0, 0.0, 0.0, 0.0, -0.2, -0.4, -0.6, -1, -1.2};
		solution = new double[] {};
		if (solution.length > maxSteps) {
			maxSteps = solution.length;
		}
		
		// define goal
		goal = new double[2];
		goal[0] = mapWidth - pointSize / 2.0;
		goal[1] = mapHeight / 2.0;
		
		// place agents
		agents = new double[numAgents][2];
		double x = pointSize / 2.0;
		double step = (mapHeight - 1.0) / numAgents;
		double y = step / 2.0;
		for (int i=0; i<numAgents; ++i, y+=step) {
			agents[i][0] = x;
			agents[i][1] = y;
		}
		
		// place defenders
		defenders = new double[numDefenders][2];
		x = 2.0 / 3.0 * (mapWidth - 1);
		step = (mapHeight - 1.0) / numDefenders;
		y = step / 2.0;
		for (int i=0; i<numDefenders; ++i, y+=step) {
			defenders[i][0] = x;
			defenders[i][1] = y;
		}
	}
	
	/**
	 * Run the simulation.
	 * @param boolean visual: Whether to display the simulation in the terminal.
	 * @throws InterruptedException
	 */
	private void runSim(boolean visual) throws InterruptedException {
		
		// simulation loop
		for (int i=0; i<maxSteps; ++i) {
			
			// move agents
			for (int j=0; j<numAgents; ++j) {
				// agent is out
				if (atGoal(j) || dead(j))
					continue;
				
				// sense
				sendSensor(j);
				
				// get direction to move
				double angle = receiveActuator(j, i);
				
				// move
				moveAgent(j, angle);
			}
			
			// move defenders
			for (int j=0; j<numDefenders; ++j) {
				
				// find closest agent
				int agent = -1;
				double dist = -1;
				for (int k=0; k<numAgents; ++k) {
					// agent is out
					if (atGoal(k) || dead(k))
						continue;
					double d = Math.hypot(defenders[j][0]-agents[k][0], defenders[j][1]-agents[k][1]);
					if (d < dist || dist < 0) {
						dist = d;
						agent = k;
					}
				}
				
				// move towards closest agent
				if (agent >= 0) {
					// find angle to go to closest agent
					double dx = agents[agent][0] - defenders[j][0];
					double dy = agents[agent][1] - defenders[j][1];
					double angle = Math.atan2(dy, dx);
					// move
					moveDefender(j, angle);
				}
			}
			
			// update agents
			for (int j=0; j<numDefenders; ++j) {
				for (int k=0; k<numAgents; ++k) {
					// agent is out
					if (atGoal(k) || dead(k))
						continue;
					// agent dies
					if (touches(defenders[j], agents[k])) {
						agents[k][0] = -1;
						agents[k][1] = -1;
					}
				}
			}
			
			
			if (visual) {
				displaySim();
				Thread.sleep(stepSize);
			}
		}
	}
	
	/**
	 * Send the sensor information to the algorithm optimization tool.
	 * @param int agent: The agent that is sensing.
	 */
	private void sendSensor(int agent) {
		// sensor values
		double[] sensor = {0.0, 0.0, 0.0, 0.0};
		
		// sense all defenders
		for (int i=0; i<defenders.length; ++i) {
			// offset of defender
			double dx = defenders[i][0] - agents[agent][0];
			double dy = defenders[i][1] - agents[agent][1];
			double d = Math.hypot(dx, dy);
			
			// calculate sensor values
			if (dx > 0 && (dx / Math.pow(d, 2) > sensor[0] || sensor[0] == 0.0))
				sensor[0] = dx / Math.pow(d, 2);
			if (dy > 0 && (dy / Math.pow(d, 2) > sensor[1] || sensor[1] == 0.0))
				sensor[1] = dy / Math.pow(d, 2);
			if (dx < 0 && (-dx / Math.pow(d, 2) > sensor[2] || sensor[2] == 0.0))
				sensor[2] = -dx / Math.pow(d, 2);
			if (dy < 0 && (-dy / Math.pow(d, 2) > sensor[3] || sensor[3] == 0.0))
				sensor[3] = -dy / Math.pow(d, 2);
		}
		
		// TODO: send sensor to optimization tool
	}
	
	/**
	 * Receive the actuator command from the algorithm optimization tool.
	 * It returns the relative direction in which an agent has to move.
	 * @param int agent: The agent that is acting.
	 * @param int step: The simulation time step.
	 * @return double: Angle from the horizontal axis.
	 */
	private double receiveActuator(int agent, int step) {
		// TODO: receive actuator from optimization tool
		Double[] actuator = new Double[]{0.0};
		
		// for testing
		if (0 < solution.length && 0 <= step && step < solution.length)
			return solution[step];
		
		// convert actuator into angle
		return actuator[0] * 2 * 3.14159;
	}
	
	/**
	 * Calculate the fitness score.
	 */
	private double calcFitness() {
		double fitness = 0.0;
		
		// sum up distance of every agent to goal
		for (int i=0; i<numAgents; ++i) {
			fitness += Math.hypot(agents[i][0] - goal[0], agents[i][1] - goal[1]);
		}
		
		return -fitness;
	}
	
	/**
	 * Visualize the simulation on the command line.
	 */
	private void displaySim() {
		// initialize environment
		cell[][] map = new cell[mapWidth][mapHeight];
		for (int x=0; x<mapWidth; ++x) {
			for (int y=0; y<mapHeight; ++y) {
				map[x][y] = cell.FREE;
			}
		}
		map[(int) Math.round(goal[0]-1)][(int) Math.round(goal[1])] = cell.GOAL;
		
		// place agents
		for (int i=0; i<numAgents; ++i) {
			if (atGoal(i) || dead(i))
				continue;
			map[(int) Math.round(agents[i][0])][(int) Math.round(agents[i][1])] = cell.AGENT;
		}
		
		// place defenders
		for (int i=0; i<numDefenders; ++i) {
			map[(int) Math.round(defenders[i][0])][(int) Math.round(defenders[i][1])] = cell.DEFENDER;
		}
		
		// print upper bound
		for (int x=0; x<mapWidth+2; ++x) {
			System.out.print('-');
		}
		System.out.print('\n');
		
		// print field
		for (int y=mapHeight-1; y>=0; --y) {
			// print left bound
			System.out.print('|');
			
			// print environment and players
			for (int x=0; x<mapWidth; ++x) {
				switch (map[x][y]) {
					case OBSTACLE:
						System.out.print('X');
						break;
					case GOAL:
						System.out.print('G');
						break;
					case AGENT:
						System.out.print('A');
						break;
					case DEFENDER:
						System.out.print('D');
						break;
					default:
						System.out.print(' ');
						break;
				}
			}
			
			// print right bound
			System.out.print('|');
			System.out.print('\n');
		}
		
		// print lower bound
		for (int x=0; x<mapWidth+2; ++x) {
			System.out.print('-');
		}
		System.out.print('\n');
	}
	
	/**
	 * Check if an agent reached the goal.
	 * @param agent: The agent to check.
	 * @return boolean: Whether the agent reached the goal.
	 */
	private boolean atGoal(int agent) {
		return touches(agents[agent], goal);
	}
	
	/**
	 * Check if an agent was caught by a defender.
	 * @param agent: The agent to check.
	 * @return boolean: Whether the agent was caught.
	 */
	private boolean dead(int agent) {
		return (agents[agent][0] < 0 || agents[agent][1] < 0);
	}
	
	/**
	 * Check if two points touch.
	 * @param p1
	 * @param p2
	 * @return boolean: True if the the distance between both points is smaller or equal to pointSize.
	 */
	private boolean touches(double[] p1, double[] p2) {
		return Math.hypot(p1[0] - p2[0], p1[1] - p2[1]) <= pointSize;
	}
	
	/**
	 * Move an agent into the given direction.
	 * @param int agent: ID of the agent.
	 * @param double angle: Direction the agent moves in.
	 */
	private void moveAgent(int agent, double angle) {
		movePoint(agents[agent], angle, speedAgents);
	}
	
	/**
	 * Move a defending agent into the given direction.
	 * @param int defender: ID of the defending agent.
	 * @param double angle: Direction the defending agent moves in.
	 */
	private void moveDefender(int defender, double angle) {
		movePoint(defenders[defender], angle, speedDefenders);	
	}
	
	/**
	 * Calculate the goal coordinates of a point that moves from the given coordinates with given angle and speed.
	 * @param double[] point: Current coordinates of the point. Also used for returning the new coordinates.
	 * @param double angle: The angle in which the point moves, mathematically positive from the x-axis.
	 * @param double speed: The distance that the point moves.
	 */
	private void movePoint(double[] point, double angle, double speed) {
		// calculate new position
		double pos[] = new double[2];
		pos[0] = point[0] + Math.cos(angle) * speed;
		pos[1] = point[1] + Math.sin(angle) * speed;
		
		// allow only valid moves
		if(pos[0] < pointSize / 2.0 || (mapWidth - pointSize / 2.0) < pos[0] || pos[1] < pointSize / 2.0 || (mapHeight - pointSize / 2.0) < pos[1])
			return;
		
		// move
		point[0] = pos[0];
		point[1] = pos[1];
	}
}
