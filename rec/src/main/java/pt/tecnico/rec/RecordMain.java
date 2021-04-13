package pt.tecnico.rec;


import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.ulisboa.tecnico.sdis.zk.*;

import java.io.IOException;

public class RecordMain {

	private static String zooHost;
	private static int zooPort;
	private static ZKNaming zkNaming = null;
	private static String path;
	private static String host;
	private static int port;
	private static int instanceNumber;


	public static void main(String[] args) {
		System.out.println(RecordMain.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// check arguments
		if (args.length != 5) {
			System.err.println("ERROR incorrect number of arguments.");
			System.err.printf("Usage: java %s zooHost zooPort host port instanceNumber %n", RecordMain.class.getName());
			return;
		}
		zooHost = args[0];
		zooPort = Integer.parseInt(args[1]);
		host = args[2];
		port = Integer.parseInt(args[3]);
		instanceNumber = Integer.parseInt(args[4]);
		path = "/grpc/bicloin/rec/" + instanceNumber;

		final BindableService impl = new RecordMainImpl();

		// Create a new server to listen on port
		Server server = ServerBuilder.forPort(port).addService(impl).build();

		// Start the server
		try{
			server.start();
		}
		catch (java.io.IOException e){
			System.err.println("Caught exception when starting the server: " + e);
			return;
		}

		try{
			System.out.println("Contacting ZooKeeper at " + zooHost + ":" + zooPort + "...");
			zkNaming = new ZKNaming(zooHost, String.valueOf(zooPort));
			System.out.println("Binding " + path + " to " + host + ":" + port + "...");
			zkNaming.rebind(path , host , Integer.toString(port));
		}
		catch (ZKNamingException e){
			System.err.println("Caught exception during Zookeeper bind: " + e);
			return;
		}

		// Use hook to register a thread to be called on shutdown.
		Runtime.getRuntime().addShutdownHook(new Unbind());

		System.out.println("Server started and awaiting requests on port " + port + ".");

		// Do not exit the main thread. Wait until server is terminated.
		try{
			server.awaitTermination();
		}
		catch(java.lang.InterruptedException e){
			System.err.println("Server was interrupted.");
		}

	}

	// Unbind class unbinds replica from ZKNaming after interruption.
	static class Unbind extends Thread {
		@Override
		public void run() {
			if (zkNaming != null) {
				try {
					System.out.println("\nUnbinding " + path + " from ZooKeeper...");
					zkNaming.unbind(path, host, String.valueOf(port));
				}
				catch (ZKNamingException e) {
					System.err.println("Could not close connection with ZooKeeper: " + e);
				}
			}
		}
	}
	
}
