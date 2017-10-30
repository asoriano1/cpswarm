package helper;

import core.AbstractRepresentation;
import helper.Environment;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.lang.ProcessBuilder.Redirect;
import java.lang.String;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * An interface to execute ROS simulations.
 * @author Micha Rappaport
 */
public class simROS {
	private AbstractRepresentation representation;
	private String rosPath;
	private String rosWs;
	private String rosPackage;
	private int numOutputs;
	private Environment env;
	private ArrayList<NavigableMap<Integer,Double>> logs;

	/**
	 * Constructor.
	 * @param AbstractRepresentation representation: The candidate representation.
	 * @param String rosPath: Absolute path to the ROS installation folder.
	 * @param String rosWs: Absolute path to the ROS workspace, e.g. /home/user/my_ros_ws.
	 * @param String rosPackage: Name of the ROS package, i.e. relative path from the ROS workspace's source folder to the ROS package implementing the simulation, e.g. my_ros_pkg.
	 * @param int numOutputs: Number of outputs that the neural network has.
	 */
	public simROS(AbstractRepresentation representation, String rosPath, String rosWs, String rosPackage, int numOutputs) {
		this.representation = representation;
		this.rosPath = rosPath;
		this.rosWs = rosWs;
		this.rosPackage = rosPackage;
		this.numOutputs = numOutputs;

		exportRepresentation();
	}
	
	/**
	 * Export the candidate representation to C code.
	 */
	public void exportRepresentation () {
		try {
			// get c-code of representation
			String content = representation.getC();
			String preamble = "#include <math.h>\n\nclass Result{\npublic:\n  float output[" + numOutputs + "];\n  Result(float outp[], long outputsize){\n    long i;\n	for (i=0L;i<outputsize; i=i+1){\n	  output[i]=outp[i];\n	}\n  }\n};\n\n";
	    	content = preamble + content;
	    	
	    	// output representation to file
	    	PrintWriter	out = new PrintWriter(rosWs + "/src/" + rosPackage + "/src/candidate.c");
			out.print(content);
		    out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Set the environment to use. It must exist already in ROS.
	 * @param Environment env: The environment as selected in the problem parameter.
	 */
	public void setEnvironment(Environment env) {
		this.env = env;
	}
	
	/**
	 * Write problem parameters to YAML file for ROS.
	 * @param Hashtable<String,String> params: A hash table containing parameter names and values.  
	 */
	public void setParameters(Hashtable<String,String> params) {
	    BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(rosWs + "/src/" + rosPackage + "/config/frevo.yaml"));
			for (String param: params.keySet()) {
				writer.write(param + ": " + params.get(param));
				writer.newLine();
			}
			writer.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Return the log file contents.
	 * @return ArrayList<NavigableMap<Integer,Double>>: An array with one map entry for each log file.
	 */
	public ArrayList<NavigableMap<Integer,Double>> getLogs() {
		return logs;
	}
	
	/**
	 * Read the log files produced by ROS.
	 * It assumes log files with two columns, separated by tabulator.
	 * The first column must be an integer, the second a double value.
	 * @return ArrayList<NavigableMap<Integer,Double>>: An array with one map entry for each log file.
	 */
	public void readLogs() {
		// container for data of all log files
		logs = new ArrayList<NavigableMap<Integer,Double>>();
		
		// path to log directory
	    File logPath = new File(rosWs + "/src/" + rosPackage + "/log/");
	    
	    // iterate through all log files
	    String[] logFiles = logPath.list();
	    for ( int i=0; i<logFiles.length; i++ ) {
	    	// container for data of one log file
	    	NavigableMap<Integer,Double> log = new TreeMap<Integer, Double>();
	    	
	    	// read log file
	    	Path logFile = Paths.get(logPath + "/" + logFiles[i]);
	    	try {
	    		BufferedReader logReader = Files.newBufferedReader(logFile);
	    		
	    		// store every line
		    	String line;
				while ((line = logReader.readLine()) != null) {
					if ( line.length() <= 1 || line.startsWith("#") )
						continue;
					
					log.put(Integer.parseInt(line.split("\t")[0]), Double.parseDouble(line.split("\t")[1]));
				}
			}
	    	catch (IOException e) {
				e.printStackTrace();
			}
	    	
	    	// store contents of log file
	    	logs.add(log);
	    }
	}
	
	/**
	 * Run the simulation without GUI.
	 */
	public void run() {
		// run simulation
		String[] cmd = {"bash", "-c", rosWs + "/src/" + rosPackage + "/scripts/" + env + ".sh " + rosPath + " " + rosWs};
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectOutput(Redirect.INHERIT);
		pb.redirectError(Redirect.INHERIT);
		try {
			Process p = pb.start();
			p.waitFor();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		// read log files
		readLogs();
	}
	
	/**
	 * Run the simulation with GUI.
	 */
	public void runVisual(){
		String[] cmd = {"bash", "-c", rosWs + "/src/" + rosPackage + "/scripts/" + env + "_visual.sh " + rosPath + " " + rosWs};
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectOutput(Redirect.INHERIT);
		pb.redirectError(Redirect.INHERIT);
		try {
			Process p = pb.start();
			p.waitFor();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
