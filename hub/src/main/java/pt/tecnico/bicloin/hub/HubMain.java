package pt.tecnico.bicloin.hub;


import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.bicloin.hub.domain.Station;
import pt.tecnico.bicloin.hub.domain.User;
import pt.ulisboa.tecnico.sdis.zk.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HubMain {

	private static String zooHost;
	private static int zooPort;
	private static ZKNaming zkNaming = null;
	private static String path;
	private static String host;
	private static int port;
	private static int instanceNumber;
	private static String usersFile;
	private static String stationsFile;
	private static boolean initRec;
	private static List<User> userList = new ArrayList<>();
	private static List<Station> stationList = new ArrayList<>();

	public static void main(String[] args) {
		System.out.println(HubMain.class.getSimpleName());
		
		// Receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// Check arguments
		if (args.length != 7 && args.length != 8) {
			System.err.println("ERROR incorrect number of arguments.");
			System.err.printf("Usage: java %s zooHost zooPort host port instanceNumber usersFile stationsFile [initRec]%n", HubMain.class.getName());
			return;
		}

		zooHost = args[0];
		zooPort = Integer.parseInt(args[1]);

		host = args[2];
		port = Integer.parseInt(args[3]);
		instanceNumber = Integer.parseInt(args[4]);
		path = "/grpc/bicloin/hub/" + instanceNumber;

		usersFile = args[5];
		stationsFile = args[6];
		initRec = args.length == 8 && args[7].equals("initRec");

		try {
			importUsers();
			importStations();
		} catch (Exception e) {  // already treated in the respective methods
			return;
		}

		final BindableService impl = new HubImpl(userList, stationList);

		// Create a new server to listen on port
		Server server = ServerBuilder.forPort(port).addService(impl).build();

		// Start the server
		try{
			server.start();

			// Server threads are running in the background.
			System.out.println("Server started.");
		}
		catch(IOException ie) {
			System.err.println("Caught exception when starting the server: " + ie);
			return;
		}

		// Register on ZooKeeper
		try{
			System.out.println("Contacting ZooKeeper at " + zooHost + ":" + zooPort + "...");
			zkNaming = new ZKNaming(zooHost, String.valueOf(zooPort));
			System.out.println("Binding " + path + " to " + host + ":" + port + "...");
			zkNaming.rebind(path, host, String.valueOf(port));
		}
		catch(ZKNamingException e){
			System.err.println("Caught exception during Zookeeper bind: " + e);
			return;
		}

		// Use hook to register a thread to be called on shutdown.
		Runtime.getRuntime().addShutdownHook(new Unbind());

		System.out.println("Server started and awaiting requests on port " + port);

		// Do not exit the main thread. Wait until server is terminated.
		try{
			server.awaitTermination();
		}
		catch(InterruptedException e){
			System.err.println("Server was interrupted.");
		}

	}

	private static void importUsers() throws IOException {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(usersFile));

			String line = "";
			while ((line = br.readLine()) != null) {
				String[] userDetails = line.split(",");

				if(userDetails.length > 0 ) {
					// TODO if initRec
					User u = new User(userDetails[0], userDetails[1], userDetails[2]);
					userList.add(u);
				}
			}
		}
		catch(Exception e) {
			System.err.println("Caught exception while parsing the users file: " + e);
			throw e;
		}
		finally {
			try {
				if (br != null) { br.close(); }
			}
			catch(IOException ie) {
				System.err.println("Caught exception while closing the BufferedReader: " + ie);
			}
		}
	}

	private static void importStations() throws IOException {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(stationsFile));

			String line = "";
			while ((line = br.readLine()) != null) {
				String[] stationDetails = line.split(",");

				if(stationDetails.length > 0 ) {
					// TODO if initRec
					Station s = new Station(stationDetails[0], stationDetails[1], Float.parseFloat(stationDetails[2]),
							Float.parseFloat(stationDetails[3]), Integer.parseInt(stationDetails[4]),
							Integer.parseInt(stationDetails[6]));
					stationList.add(s);
				}
			}
		}
		catch(Exception e) {
			System.err.println("Caught exception while parsing the stations file: " + e);
			throw e;
		}
		finally {
			try {
				if (br != null) { br.close(); }
			}
			catch(IOException ie) {
				System.err.println("Caught exception while closing the BufferedReader: " + ie);
			}
		}
	}

	// Unbind class unbinds replica from ZKNaming after interruption.
	static class Unbind extends Thread {
		@Override
		public void run() {
			if (zkNaming != null) {
				try {
					System.out.println("Unbinding " + path + " from ZooKeeper...");
					zkNaming.unbind(path, host, String.valueOf(port));
				}
				catch (ZKNamingException e) {
					System.err.println("Could not close connection with ZooKeeper: " + e);
				}
			}
		}
	}
}
