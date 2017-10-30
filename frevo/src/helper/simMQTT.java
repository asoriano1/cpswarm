package helper;

import core.AbstractRepresentation;
import core.XMLFieldEntry;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.String;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * An interface to execute simulations over the MQTT broker.
 * @author Micha Rappaport
 */
public class simMQTT implements MqttCallback {
	/**
	 * Title of the simulation to run.
	 */
	private String simulation;
	
	/**
	 * Candidate representation to transform sensor readings to actuator commands.
	 */
	private AbstractRepresentation candidate;
	
	/**
	 * Requirements of the optimization problem on the simulation server.
	 */
	private Hashtable<String, Integer> requirements;
	
	/**
	 * Parameters of the optimization problem for simulation server.
	 */
	private Hashtable<String,XMLFieldEntry> parameters;
	
	/**
	 * Whether to run the simulation with GUI.
	 */
	private boolean visual;
	
	/**
	 * A mutex that lets the calling problem wait until a valid fitness score has been received.
	 */
	private Semaphore lock;
	
	/**
	 * Fitness of the candidate representation evaluated in this simulation.
	 */
	private double fitness;
	
	/**
	 * Whether or not the simulation is currently running.
	 */
	private boolean running = false;

	/**
	 * Quality of service:
	 * 0: No QoS, at most one delivery
	 * 1: Acknowledgement, at least one delivery
	 * 2: Four step handshake, exactly one delivery
	 */
	private int qos;
	
	/**
	 * Address of the MQTT server.
	 */
	private String serverURI;
	
	/**
	 * ID of the MQTT client.
	 */
	private String clientId;
	
	/**
	 * Persistence of connection.
	 */
	private MemoryPersistence persistence = new MemoryPersistence();
	
	/**
	 * The MQTT client.
	 */
	private MqttClient client;
	
	/**
	 * ID of the simulation server.
	 */
	private Integer server;
	
	/**
	 * Hash of the simulation run.
	 */
	private String simulationHash;
	
	/**
	 * Executor for repeatedly sending discovery messages.
	 */
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	/**
	 * Constructor: Store parameters in class variables, connect to the simulation broker and send a discovery message to acquire a simulation server.
	 * @param String simulation: Title of the simulation to run.
	 * @param AbstractRepresentation candidate: The candidate representation to transform sensor readings to actuator commands.
	 * @param Hashtable<String, Integer> requirements: The requirements of the problem on the simulation server.
	 * @param Hashtable<String,XMLFieldEntry> parameters: Parameters for the simulation defined in the problem.
	 * @param boolean visual: Whether to run the simulation with GUI.
	 * @param Semaphore lock: A mutex that lets the calling problem wait until a valid fitness score has been received.
	 */
	public simMQTT(String simulation, AbstractRepresentation candidate, Hashtable<String, Integer> requirements, Hashtable<String,XMLFieldEntry> parameters, boolean visual, Semaphore lock) {
		// store parameters
		this.simulation = simulation;
		this.candidate = candidate;
		this.requirements = requirements;
		this.parameters = parameters;
		this.visual = visual;
		this.lock = lock;
		
		// timeout for repeated transmission of discovery messages
		int timeout = 3;
		
		// read mqtt client settings from xml file
		File file = new File("src/helper/simMQTT.xml");
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder;
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(file);
			serverURI = document.getElementsByTagName("serverURI").item(0).getTextContent();
			clientId = document.getElementsByTagName("clientId").item(0).getTextContent();
			qos = Integer.parseInt(document.getElementsByTagName("qos").item(0).getTextContent());
			timeout = Integer.parseInt(document.getElementsByTagName("timeout").item(0).getTextContent());
		} catch (ParserConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(NumberFormatException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// create unique hash for identifying this simulation (combination of simulation and parameters)
		// TODO: make unique
	 	simulationHash = candidate.getHash().toString();
		
		try {
			// open connection
			client = new MqttClient(serverURI, clientId+simulationHash, persistence);
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(true);
			client.connect(connOpts);
			
			// subscribe to relevant topics
	    	client.setCallback(this);
			client.subscribe("server", qos);
			client.subscribe("models", qos);
			
			// send discovery message periodically, until a server message is received
			Runnable discovery = new Runnable() {
			    public void run() {
			    	sendDiscovery();
			    }
			};
			executor.scheduleAtFixedRate(discovery, 0, timeout, TimeUnit.SECONDS);
			
		} catch (MqttException me) {
			System.out.println("reason "+me.getReasonCode());
			System.out.println("msg "+me.getMessage());
			System.out.println("loc "+me.getLocalizedMessage());
			System.out.println("cause "+me.getCause());
			System.out.println("excep "+me);
			me.printStackTrace();
		}
		
		// block problem code until simulation finishes
		try {
			lock.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Return the fitness value of the simulated candidate.
	 * @return double: The fitness value.
	 */
	public double getFitness() {
		return fitness;
	}
	
	/**
	 * Close connection to MQTT broker.
	 */
	public void close() {
		try {
			client.disconnect();
		} catch (MqttException me) {
			System.out.println("reason "+me.getReasonCode());
			System.out.println("msg "+me.getMessage());
			System.out.println("loc "+me.getLocalizedMessage());
			System.out.println("cause "+me.getCause());
			System.out.println("excep "+me);
			me.printStackTrace();
		}
	}
	
	/**
	 * Send a discovery message to detect available simulation servers.
	 */
	@SuppressWarnings("unchecked")
	private void sendDiscovery() {
		// create json object
		JSONObject discovery = new JSONObject();
		
		// fill json object
		discovery.put("simulation", simulation);
		discovery.put("simulation_hash", simulationHash);
		JSONObject requirementsJson = new JSONObject();
		Set<String> keys = requirements.keySet();
		for(String key: keys){
			requirementsJson.put(key, requirements.get(key).intValue());
		}
		discovery.put("requirements", requirementsJson);
		
		// send discovery message
        sendMessage("discovery", discovery);
	}
	
	/**
	 * Send problem parameters message.
	 * @param Hashtable<String,XMLFieldEntry> params: A hash table containing parameter names and values.  
	 */
	@SuppressWarnings("unchecked")
	private void sendParameters() {
		// json object holding the parameters
		JSONObject paramsJson = new JSONObject();
		
		// identification parameters
		paramsJson.put("server", server);
		paramsJson.put("simulation_hash", simulationHash);
		
		// iterate all parameters
		Set<String> keys = parameters.keySet();
		for(String key: keys){
			// parameter string
			String param = parameters.get(key).getValue();
			
			// place parameter in json object
			try{
				// try to make integer first
				paramsJson.put(key, Integer.parseInt(param));
			}
			catch(NumberFormatException ie){
				try{
					// then try to make it a double
					paramsJson.put(key, Double.parseDouble(param));
				}
				catch(NumberFormatException de){
					// put value as string
					paramsJson.put(key, param);
				}
			}
		}
		
		// send parameters message
        sendMessage("parameters", paramsJson);
	}
	
	/**
	 * Send control message to simulation server.
	 * @param boolean run: Whether to run or terminate a simulation.
	 */
	@SuppressWarnings("unchecked")
	private void sendControl(boolean run) {
	 	// creat json object
	 	JSONObject ctrl = new JSONObject();
	 	
	 	// fill json object
		ctrl.put("server", server);
		ctrl.put("simulation_hash", simulationHash);
		ctrl.put("run", run);
		ctrl.put("visual", visual);
		
		// send control message
        sendMessage("control", ctrl);
	}
	
	/**
	 * Send the actuator commands to a specific agent at the simulation server.
	 * @param int agent: The agent that shall execute the actuator commands.
	 * @param ArrayList<Float> actuator: The actuator commands for the agent.
	 */
	@SuppressWarnings("unchecked")
	private void sendActuator(int agent, ArrayList<Float> actuator) {
		// iterator for actuator commands
		Iterator<Float> it;
		
		// create json object
		JSONObject actuatorJson = new JSONObject();
		
		// fill json object
		actuatorJson.put("simulation_hash", simulationHash);
		actuatorJson.put("agent", agent);
		JSONArray cmd = new JSONArray();
        it = actuator.iterator();
        while (it.hasNext()) {
        	cmd.add(it.next().doubleValue());
        }
        actuatorJson.put("actuator", cmd);
        
        // send actuator message
        sendMessage("actuator", actuatorJson);
	}
	
	/**
	 * Transmit a message via the MQTT broker.
	 * @param String topic: The topic on which the message is sent.
	 * @param JSONObject content: The message content as JSON object.
	 */
	private void sendMessage(String topic, JSONObject content) {
		// create string from json object
		StringWriter jsonOut = new StringWriter();
		try {
			content.writeJSONString(jsonOut);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String contentString = jsonOut.toString();
		
		// transmit message
		MqttMessage messageTx = new MqttMessage(contentString.getBytes());
		messageTx.setQos(qos);
		try {
			client.publish(topic, messageTx);
		} catch (MqttException me) {
			System.out.println("reason "+me.getReasonCode());
			System.out.println("msg "+me.getMessage());
			System.out.println("loc "+me.getLocalizedMessage());
			System.out.println("cause "+me.getCause());
			System.out.println("excep "+me);
			me.printStackTrace();
		}
	}

	/**
	 * Callback method that is called when a new message for the subscribed topics is received.
	 * @param String topic: The topic on which the message was received.
	 * @param MqttMessage message: The received message.
	 */
	@Override
	public void messageArrived(String topic, MqttMessage message) {
        //System.out.println("Received message on topic " + topic + ": " + new String(message.getPayload()));

		try {
			// parse message payload
			JSONParser parser = new JSONParser();
			JSONObject json = (JSONObject) parser.parse(new String(message.getPayload()));
			
			// handle message content
	        switch(topic) {
		        case "server":
					handleServer(json);
					break;
		        case "sensor":
					handleSensor(json);
					break;
		        case "fitness":
					handleFitness(json);
					break;
				default:
					System.out.println("Unknown topic!");
					// TODO: throw error
					break;
	        }
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Handle the contents of a server message: Check if the requested simulation is provided and the requirements are fulfilled.
	 * @param JSONObject serverMsg: The contents of the server message.
	 */
	@SuppressWarnings("unchecked")
	private void handleServer(JSONObject serverMsg) {
		// no simulation running so far
		// check if server fulfills requirements
		if (running == false) {
			// check if server replied to me
			if (serverMsg.get("simulation_hash").toString().equals(simulationHash) == false) {
				return;
			}
			
			// check if server can perform the requested simulation
			boolean simValid = false;
			Iterator<String> it;
			JSONArray simulations = (JSONArray) serverMsg.get("simulations");
			it = simulations.iterator();
	        while (it.hasNext()) {
				if (simulation.equals(it.next())) {
					simValid = true;
					break;
				}
	        }
	        	
			// check if server satisfies requirements
			boolean reqValid = false;
			JSONObject capabilities = (JSONObject) serverMsg.get("capabilities");
			for (String req : requirements.keySet()) {
				// TODO: generalize this check
				if (capabilities.containsKey(req) && ((Long) capabilities.get(req)).intValue() >= requirements.get(req)) {
					reqValid = true;
				}
				else {
					reqValid = false;
					break;
				}
			}
			
			// both simulation and requirements are valid
		 	// TODO: select most appropriate server
			if (simValid && reqValid) {
				// stop sending discovery messages
				executor.shutdownNow();
				
				// store server id
				server = ((Long)serverMsg.get("server")).intValue();
				
				// TODO: debug
				//System.out.println(simulationHash + ": Selected server " + server);
						
				// send parameters to server
				sendParameters();
				
				try {
					// subscribe to relevant topics
				 	client.subscribe("sensor", qos);
				 	client.subscribe("fitness", qos);
				 	
				 	// start simulation
				 	running = true;
				 	sendControl(true);
				} catch (MqttException me) {
					System.out.println("reason "+me.getReasonCode());
					System.out.println("msg "+me.getMessage());
					System.out.println("loc "+me.getLocalizedMessage());
					System.out.println("cause "+me.getCause());
					System.out.println("excep "+me);
					me.printStackTrace();
				}
			}
		}
	}

	/**
	 * Handle the contents of a sensor message that contain the sensor readings of an agent in the simulation:
	 * Use the candidate representation to calculate the actuator commands and send them to the simulation server.
	 * @param JSONObject sensorMsg: The contents of the sensor message.
	 */
	@SuppressWarnings("unchecked")
	private void handleSensor(JSONObject sensorMsg) {
		// only proceed if message is for this simulation
		if (sensorMsg.get("simulation_hash").toString().equals(simulationHash)) {
			// iterator for sensor readings
			Iterator<Double> it;
			
			// get sensor readings from message payload
			ArrayList<Float> input = new ArrayList<Float>();
			JSONArray sensor = (JSONArray) sensorMsg.get("sensor");
			it = sensor.iterator();
	        while (it.hasNext()) {
	            input.add(it.next().floatValue());
	        }
			
	        // use candidate representation to get actuator commands
			ArrayList<Float> output = candidate.getOutput(input);
			
			// TODO: debug
			//System.out.println(simulationHash + ": agent " + sensorMsg.get("agent") + ": " + input.toString() + " --> " + output.toString());
			
			// send actuator message
			sendActuator(((Long) sensorMsg.get("agent")).intValue(), output);
		}
	}

	/**
	 * Handle the contents of a fitness message: Store the fitness and signal end of simulation to the problem component.
	 * @param JSONObject fitnessMsg: The contents of the fitness message.
	 */
	private void handleFitness(JSONObject fitnessMsg) {
		// only proceed if message is for this simulation
		if (fitnessMsg.get("simulation_hash").toString().equals(simulationHash)) {
			// store fitness in class variable
			fitness = (double) fitnessMsg.get("fitness");
			
			// no simulation running anymore
			running = false;
			
			// TODO: debug
			//System.out.println(simulationHash + ": fitness " + fitness);
			
			// allow problem code to return fitness
			lock.release();
		}
	}

	/**
	 * Callback method that is called when the connection to the server is lost.
	 */
	@Override
	public void connectionLost(Throwable arg0) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Callback method that is called when a message has been delivered successfully.
	 */
	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
		
	}
}
