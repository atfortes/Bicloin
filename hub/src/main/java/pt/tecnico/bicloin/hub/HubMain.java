package pt.tecnico.bicloin.hub;


import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.ulisboa.tecnico.sdis.zk.*;

import java.io.IOException;

public class HubMain {
	
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
			System.err.printf("Usage: java %s port%n", HubMain.class.getName());
			return;
		}

		final String zooKeeperServ = args[0];
		final String zooKeeperPort = args[1];
		final ZKNaming zkNaming = new ZKNaming(zooKeeperServ, zooKeeperPort);

		final String serv = args[2];
		final int port = Integer.parseInt(args[3]);
		final int instanceNumber = Integer.parseInt(args[4]);
		final String usersFile = args[5];
		final String stationsFile = args[6];
		final boolean initRec = args.length == 8 && args[7].equals("initRec");

		final BindableService impl = new HubMainImpl(usersFile, stationsFile, initRec);

		// Create a new server to listen on port
		Server server = ServerBuilder.forPort(port).addService(impl).build();

		// Start the server
		try{
			server.start();
		}
		catch(IOException ie) {
			System.err.println("Caught exception when starting the server: " + ie);
			return;
		}

		// Register server
		try{
			zkNaming.rebind("/grpc/bicloin/hub/" + instanceNumber, serv, String.valueOf(port));
		}
		catch(pt.ulisboa.tecnico.sdis.zk.ZKNamingException e){
			System.err.println("Caught exception during Zookeeper bind: " + e);
			return;
		}

		// Server threads are running in the background.
		System.out.println("Server started.");

		// Do not exit the main thread. Wait until server is terminated.
		try{
			server.awaitTermination();
		}
		catch(InterruptedException e){
			System.err.println("Server was interrupted.");
		}

	}
}
