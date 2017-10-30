package emergencyexitlikeros;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;

import net.jodk.lang.FastMath;
import utils.NESRandom;
import GridVisualization.Display;
import GridVisualization.WhiteBoard;
import core.AbstractRepresentation;
import core.AbstractSingleProblem;
import emergencyexitlikeros.Field;
import emergencyexitlikeros.FieldSource;

/**
 * A simulation where multiple Agents try to find the Emergency Exit
 * 
 * @author Thomas Dittrich
 * 
 */
public class EmergencyExitLikeROS extends AbstractSingleProblem {

	int steps;
	int numberofAgents;
	int numberofExits;
	int numberofBlockades;
	int seed;
	int numberofEvaluations;
	agent[] agents;
	Exit[] EmergencyExits;
	blockade[] blockades;
	AbstractRepresentation c;
	JTextField seedTextField;
	Field f = new Field();
	FieldSource FIELDSOURCE;

	// this function is called to simulate without visualization. It is used to
	// find the best Representation
	@Override
	public double evaluateCandidate(AbstractRepresentation candidate) {
		// read config from xml file
		steps = Integer.parseInt(getProperties().get("steps").getValue());
		f.width = Integer.parseInt(getProperties().get("width").getValue());
		f.height = Integer.parseInt(getProperties().get("height").getValue());
		seed = Integer.parseInt(getProperties().get("seedforEmergencyExits")
				.getValue());
		numberofEvaluations = Integer.parseInt(getProperties().get(
				"NumberofEvaluations").getValue());
		String filename = getProperties().get("Filename").getValue();
		FIELDSOURCE = FieldSource.valueOf(getProperties().get("fieldsource")
				.getValue());
		c = candidate;
		double Fitness = 0;
		if (FIELDSOURCE == FieldSource.FIELD_FROM_FILE) {
			setupField(filename);
			Fitness += calcSim();
		} else {
			for (int s = seed; s < seed + numberofEvaluations; s++) {
				setupField(s);
				Fitness += calcSim();
			}
		}

		return (Fitness);// / numberofEvaluations);
	}

	WhiteBoard whiteboard;
	Display display;

	@Override
	public void replayWithVisualization(AbstractRepresentation candidate) {
		steps = 0;
		c = candidate;
		// read config from xml file
		f.width = Integer.parseInt(getProperties().get("width").getValue());
		f.height = Integer.parseInt(getProperties().get("height").getValue());
		seed = Integer.parseInt(getProperties().get("seedforEmergencyExits")
				.getValue());
		String filename = getProperties().get("Filename").getValue();
		FIELDSOURCE = FieldSource.valueOf(getProperties().get("fieldsource")
				.getValue());
		if (FIELDSOURCE == FieldSource.FIELD_FROM_FILE) {
			setupField(filename);
		} else {
			setupField(seed);
		}
		display = new Display(840, 695, "EmergencyExit");
		display.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		whiteboard = new WhiteBoard(600, 600, f.width, f.height, 1);
		whiteboard.addColorToScale(0, Color.WHITE);
		// you can decide whether to take images or colors to represent things
		// on the whiteboard
		// whiteboard.addColorToScale(1, Color.BLACK);
		whiteboard
				.addImageToScale(1,
						"Components/Problems/EmergencyExitLikeROS/emergencyexitlikeros/agent.png");
		// whiteboard.addColorToScale(2, Color.GREEN);
		whiteboard
				.addImageToScale(2,
						"Components/Problems/EmergencyExitLikeROS/emergencyexitlikeros/EmergencyExit.png");
		whiteboard
				.addImageToScale(3,
						"Components/Problems/EmergencyExitLikeROS/emergencyexitlikeros/blockade.png");
		JButton minusbutton = new JButton("<--");
		JButton plusbutton = new JButton("-->");
		seedTextField = new JTextField("" + seed);
		seedTextField.setPreferredSize(new Dimension(50, 20));
		JButton startButton = new JButton("Change seed");
		display.add(whiteboard);
		display.add(minusbutton);
		display.add(plusbutton);
		display.add(seedTextField);
		display.add(startButton);
		minusbutton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (steps > 0)
					steps--;
				String filename = getProperties().get("Filename").getValue();
				if (FIELDSOURCE == FieldSource.FIELD_FROM_FILE) {
					setupField(filename);
				} else {
					setupField(seed);
				}
				double Fitness = calcSim();
				displayResult();
				int agentsleft = 0;
				for (agent a : agents) {
					if (!a.hasReachedExit)
						agentsleft++;
				}
				String FitnessString = String.format("%.4f", Fitness);
				display.setTitle("Emergency Exit    Step: " + steps
						+ "   Fitness: " + FitnessString
						+ "  Number of Agents left: " + agentsleft);
			}
		});
		plusbutton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				steps++;
				String filename = getProperties().get("Filename").getValue();
				if (FIELDSOURCE == FieldSource.FIELD_FROM_FILE) {
					setupField(filename);
				} else {
					setupField(seed);
				}
				double Fitness = calcSim();
				displayResult();
				int agentsleft = 0;
				for (agent a : agents) {
					if (!a.hasReachedExit)
						agentsleft++;
				}
				String FitnessString = String.format("%.4f", Fitness);
				display.setTitle("Emergency Exit    Step: " + steps
						+ "   Fitness: " + FitnessString
						+ "  Number of Agents left: " + agentsleft);
			}
		});
		startButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				steps = 0;
				seed = Integer.parseInt(seedTextField.getText());
				setupField(seed);
				double Fitness = calcSim();
				displayResult();
				int agentsleft = 0;
				for (agent a : agents) {
					if (!a.hasReachedExit)
						agentsleft++;
				}
				String FitnessString = String.format("%.4f", Fitness);
				display.setTitle("Emergency Exit    Step: " + steps
						+ "   Fitness: " + FitnessString
						+ "  Number of Agents left: " + agentsleft);
			}
		});
		display.setVisible(true);
		double Fitness = calcSim();
		displayResult();
		int agentsleft = 0;
		for (agent a : agents) {
			if (!a.hasReachedExit)
				agentsleft++;
		}
		String FitnessString = String.format("%.4f", Fitness);
		display.setTitle("Emergency Exit    Step: " + steps + "   Fitness: "
				+ FitnessString + "  Number of Agents left: " + agentsleft);
	}

	/**
	 * Displays the result of the last Simulation
	 */
	private void displayResult() {
		int[][] data = new int[f.width][f.height];
		for (int x = 0; x < f.width; x++) {
			for (int y = 0; y < f.height; y++) {
				data[x][y] = 0;
			}
		}
		for (agent a : agents) {
			data[a.xpos][a.ypos] = 1;
		}
		for (Exit e : EmergencyExits) {
			data[e.xpos][e.ypos] = 2;
		}
		for (blockade b : blockades) {
			data[b.xpos][b.ypos] = 3;
		}

		whiteboard.setData(data);
		whiteboard.repaint();
	}

	void setupField(String Filename) {
		Field f = new Field();
		File FieldFile = new File(Filename);
		try {
			FileInputStream filein = new FileInputStream(FieldFile);
			ObjectInputStream objectin = new ObjectInputStream(filein);
			f = (Field) objectin.readObject();
			filein.close();
			objectin.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		numberofAgents = f.agents.length;
		numberofExits = f.EmergencyExits.length;
		numberofBlockades = f.blockades.length;
		agents /*        */= new agent[numberofAgents];
		EmergencyExits /**/= new Exit[numberofExits];
		blockades /*     */= new blockade[numberofBlockades];
		// it is important to reset the Representation. Otherwise there would
		// sometimes be simulation mistakes because the Representation wouldn't
		// start from the same seed
		c.reset();
		// create the agents and place them in a straight line from the upper
		// left to the lower right corner

		for (int i = 0; i < agents.length; i++) {
			agents[i] = new agent(c.clone(), f.agents[i].x, f.agents[i].y,
					false);
		}
		for (int i = 0; i < EmergencyExits.length; i++) {
			EmergencyExits[i] = new Exit();
			EmergencyExits[i].xpos = f.EmergencyExits[i].x;
			EmergencyExits[i].ypos = f.EmergencyExits[i].y;
		}
		for (int i = 0; i < blockades.length; i++) {
			blockades[i] = new blockade();
			blockades[i].xpos = f.blockades[i].x;
			blockades[i].ypos = f.blockades[i].y;
		}

	}

	void setupField(int s) {
		// read config from xml file
		numberofAgents /*   */= Integer.parseInt(getProperties().get(
				"NumberofSwarmMembers").getValue());
		numberofExits /*    */= Integer.parseInt(getProperties().get(
				"NumberofEmergencyExits").getValue());
		numberofBlockades /**/= Integer.parseInt(getProperties().get(
				"NumberofBlockades").getValue());

		numberofAgents /**/= Math.min(numberofAgents,
				Math.max(f.width, f.height));
		numberofExits /* */= Math.min(numberofExits, f.width * f.height);
		agents /*        */= new agent[numberofAgents];
		EmergencyExits /**/= new Exit[numberofExits];
		blockades /*     */= new blockade[numberofBlockades];
		// it is important to reset the Representation. Otherwise there would
		// sometimes be simulation mistakes because the Representation wouldn't
		// start from the same seed
		c.reset();
		// create the agents and place them in a straight line from the upper
		// left to the lower right corner

		Random positionGenerator = new NESRandom(s);
		for (int i = 0; i < agents.length; i++) {
			agents[i] = new agent(c.clone(), false);
			boolean PositionExistsAlready = false;
			do {
				agents[i].xpos = positionGenerator.nextInt(f.width);
				agents[i].ypos = positionGenerator.nextInt(f.width);
				PositionExistsAlready = false;
				for (int j = 0; j < i; j++) {
					if (agents[i].xpos == agents[j].xpos
							&& agents[i].ypos == agents[j].ypos)
						PositionExistsAlready = true;
				}
			} while (PositionExistsAlready);
		}
		for (int i = 0; i < EmergencyExits.length; i++) {
			EmergencyExits[i] = new Exit();
			boolean PositionExistsAlready = false;
			do {
				EmergencyExits[i].xpos = positionGenerator.nextInt(f.width);
				EmergencyExits[i].ypos = positionGenerator.nextInt(f.width);
				PositionExistsAlready = false;
				for (int j = 0; j < i; j++) {
					if (EmergencyExits[i].xpos == EmergencyExits[j].xpos
							&& EmergencyExits[i].ypos == EmergencyExits[j].ypos)
						PositionExistsAlready = true;
				}
				for (int j = 0; j < agents.length; j++) {
					if (EmergencyExits[i].xpos == agents[j].xpos
							&& EmergencyExits[i].ypos == agents[j].ypos)
						PositionExistsAlready = true;
				}
			} while (PositionExistsAlready);
		}
		for (int i = 0; i < blockades.length; i++) {
			blockades[i] = new blockade();
			boolean PositionExistsAlready = false;
			do {
				blockades[i].xpos = positionGenerator.nextInt(f.width);
				blockades[i].ypos = positionGenerator.nextInt(f.width);
				PositionExistsAlready = false;
				for (int j = 0; j < i; j++) {
					if (blockades[i].xpos == blockades[j].xpos
							&& blockades[i].ypos == blockades[j].ypos)
						PositionExistsAlready = true;
				}
				for (int j = 0; j < EmergencyExits.length; j++) {
					if (blockades[i].xpos == EmergencyExits[j].xpos
							&& blockades[i].ypos == EmergencyExits[j].ypos)
						PositionExistsAlready = true;
				}
				for (int j = 0; j < agents.length; j++) {
					if (blockades[i].xpos == agents[j].xpos
							&& blockades[i].ypos == agents[j].ypos)
						PositionExistsAlready = true;
				}
			} while (PositionExistsAlready);
		}
	}

	/**
	 * Calculates one Simulation whit a certain amount of steps, which has to be
	 * defined before calling this method
	 * 
	 * @return Returns the negative Sum of the distances between the agents and
	 *         the Emergency Exit
	 */
	double calcSim() {
		int stepfinished = steps;
		for (int step = 0; step < steps; step++) {
			for (int i = 0; i < agents.length; i++) {
				agent a = agents[i];
				if (!a.hasReachedExit) {

					Exit nearestExit = EmergencyExits[0];
					double minimumDistance = FastMath.hypot(
							EmergencyExits[0].xpos - a.xpos,
							EmergencyExits[0].ypos - a.ypos);
					for (int e = 0; e < EmergencyExits.length; e++) {
						double Distance = FastMath.hypot(EmergencyExits[e].xpos
								- a.xpos, EmergencyExits[e].ypos - a.ypos);
						if (Distance < minimumDistance) {
							minimumDistance = Distance;
							nearestExit = EmergencyExits[e];
						}
					}

					// input[0] .. horizontal distance between the agent and the
					// nearest Emergency Exit
					// input[1] .. vertical distance between the agent and the
					// nearest Emergency Exit
					// input[2] .. field east of the agent is occupied
					// input[3] .. field north of the agent is occupied
					// input[4] .. field west of the agent is occupied
					// input[5] .. field south of the agent is occupied

					// determine which fields around the agent are occupied by
					// another agent
					boolean northfree /**/= true;
					boolean eastfree /* */= true;
					boolean southfree /**/= true;
					boolean westfree /* */= true;

					if (a.ypos <= 0) {
						northfree = false;
					} else if (a.ypos >= f.height - 1) {
						southfree = false;
					}

					if (a.xpos <= 0) {
						westfree = false;
					} else if (a.xpos >= f.width - 1) {
						eastfree = false;
					}

					for (int j = 0; j < agents.length; j++) {
						agent ag = agents[j];
						if (!ag.hasReachedExit) { // If a agent has reached the
													// Emergency Exit he cannot
													// occupy a field
							if (a.xpos /**/== ag.xpos
									&& a.ypos - 1 == ag.ypos)
								northfree /**/= false;
							if (a.xpos + 1 == ag.xpos && a.ypos /**/== ag.ypos)
								eastfree /* */= false;
							if (a.xpos /**/== ag.xpos
									&& a.ypos + 1 == ag.ypos)
								southfree /**/= false;
							if (a.xpos - 1 == ag.xpos && a.ypos /**/== ag.ypos)
								westfree /* */= false;
						}
					}
					for (int j = 0; j < blockades.length; j++) {
						blockade b = blockades[j];
						if (a.xpos /**/== b.xpos && a.ypos - 1 == b.ypos)
							northfree /**/= false;
						if (a.xpos + 1 == b.xpos && a.ypos /**/== b.ypos)
							eastfree /* */= false;
						if (a.xpos /**/== b.xpos && a.ypos + 1 == b.ypos)
							southfree /**/= false;
						if (a.xpos - 1 == b.xpos && a.ypos /**/== b.ypos)
							westfree /* */= false;
					}

					// ros has origin in center, frevo at top left
					ArrayList<Float> input = new ArrayList<Float>();
					input.add((float) ((nearestExit.xpos - (f.width - 1) / 2) - (a.xpos - (f.width - 1) / 2)));
					input.add((float) (((f.height - 1) / 2 - nearestExit.ypos) - ((f.height - 1) / 2 - a.ypos)));
					input.add(eastfree /* */? 0.0f : 1.0f);
					input.add(northfree /**/? 0.0f : 1.0f);
					input.add(westfree /* */? 0.0f : 1.0f);
					input.add(southfree /**/? 0.0f : 1.0f);

					// output[0] .. horizontal velocity of the agent
					// output[1] .. vertical velocity of the agent
					ArrayList<Float> output = a.representation.getOutput(input);

					// the elements of output are float values between 0.0 and
					// 1.0
					// for the simulation it is useful to format these values so
					// that you can see what each value means
					float xVfloat = output.get(0).floatValue() * 2.0f - 1.0f;
					float yVfloat = output.get(1).floatValue() * 2.0f - 1.0f;

					int xVelocity = Math.round(xVfloat); // -1 .. move one field
															// in negative
															// horizontal
															// direction
															// 0 .. do not move
															// in any horizontal
															// direction
															// 1 .. move one
															// field in positive
															// horizontal
															// direction
					int yVelocity = Math.round(yVfloat); // -1 .. move one field
															// in negative
															// vertical
															// direction
															// 0 .. do not move
															// in any vertical
															// direction
															// 1 .. move one
															// field in positive
															// vertical
															// direction
					
					// only move in one dimension at a time
					if(xVelocity != 0)
						yVelocity = 0;
					
					// invert to comply with ros, which uses positive for upwards
					yVelocity = -yVelocity;
					
					//System.out.println("NN input: " + input.get(0) + ", " + input.get(1) + ", " + input.get(2) + ", " + input.get(3) + ", " + input.get(4) + ", " + input.get(5));
					//System.out.println("NN output: " + output.get(0) + ", " + output.get(1));
					//System.out.println("Move: (" + xVelocity + ", " + yVelocity + ")");
					
					// move the agent (only if the place, that he wants to move
					// is not occupied by another agent)
					if /*   */(xVelocity == 0/* */&& yVelocity == -1/**/
							&& northfree /**/&& /*                    */a.ypos > 0) {
						a.xpos += 0;
						a.ypos += -1;
					} else if (xVelocity == 1/* */&& yVelocity == 0/* */
							&& eastfree /* */&& a.xpos < f.width - 1) {
						a.xpos += 1;
						a.ypos += 0;
					} else if (xVelocity == 0/* */&& yVelocity == 1/* */
							&& southfree /**/&& /*                    */a.ypos < f.height - 1) {
						a.xpos += 0;
						a.ypos += 1;
					} else if (xVelocity == -1/**/&& yVelocity == 0/* */
							&& westfree /* */&& a.xpos > 0/*      */) {
						a.xpos += -1;
						a.ypos += 0;
					}
					else{
						//System.out.println("Did not move!");
					}

					// if /* */(xVelocity >= 0.9 && a.xpos < width - 1 ) a.xpos
					// += 1;
					// else if (xVelocity <= -0.9 && a.xpos > 0 /* */) a.xpos -=
					// 1;
					// if /* */(yVelocity >= 0.9 && a.ypos < height - 1) a.ypos
					// += 1;
					// else if (yVelocity <= -0.9 && a.ypos > 0 /* */) a.ypos -=
					// 1;
					for (int n = 0; n < EmergencyExits.length
							&& !a.hasReachedExit; n++) {
						if (a.xpos == EmergencyExits[n].xpos
								&& a.ypos == EmergencyExits[n].ypos)
							a.hasReachedExit = true;
						else
							/*                                                                 */a.hasReachedExit = false;
					}
				}
			}
			boolean finished = true;

			for (agent a : agents) {
				if (!a.hasReachedExit)
					finished = false;
			}
			if (finished && stepfinished > step)
				stepfinished = step;
		}
		double Fitness = (double) (steps - stepfinished) / (double) steps;// *
																			// numberofAgents;
		for (agent a : agents) {
			double minimumDistance = FastMath.hypot(EmergencyExits[0].xpos
					- a.xpos, EmergencyExits[0].ypos - a.ypos);
			for (int e = 0; e < EmergencyExits.length; e++) {
				double Distance = FastMath.hypot(EmergencyExits[e].xpos
						- a.xpos, EmergencyExits[e].ypos - a.ypos);
				if (Distance < minimumDistance) {
					minimumDistance = Distance;
				}
			}
			Fitness += -minimumDistance;// / FastMath.hypot(f.width, f.height);
		}
		return (Fitness);// / numberofAgents);
	}

	public class agent {
		public AbstractRepresentation representation;
		public int xpos;
		public int ypos;
		public boolean hasReachedExit;

		/**
		 * @param r
		 *            Representation for this Agent
		 * @param x
		 *            defines the x position of this Agent
		 * @param y
		 *            defines the y position of this Agent
		 */
		public agent(AbstractRepresentation r, int x, int y, boolean reachedExit) {
			representation = r;
			xpos = x;
			ypos = y;
			hasReachedExit = reachedExit;
		}

		public agent(AbstractRepresentation r, boolean reachedExit) {
			representation = r;
			hasReachedExit = reachedExit;
		}
	}

	public class Exit {
		public int xpos;
		public int ypos;
	}

	public class blockade {
		public int xpos;
		public int ypos;
	}

	@Override
	public double getMaximumFitness() {
		// TODO Auto-generated method stub
		return Double.MAX_VALUE;
	}
}
