package pt.tecnico.rec;


import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.ulisboa.tecnico.sdis.zk.*;

import java.io.IOException;

public class RecordMain {
	
	public static void main(String[] args) {
		System.out.println(RecordMain.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// check arguments
		if (args.length != 5) {
			System.err.println("Incorrect Number of Arguments!");
			System.err.printf("Usage: java %s port%n", RecordMain.class.getName());
			return;
		}
		final String zooKeeperServ = args[0];
		final String zooKeeperPort = args[1];
		final ZKNaming zkNaming = new ZKNaming(zooKeeperServ, zooKeeperPort);

		final String serv = args[2];
		final int port = Integer.parseInt(args[3]);
		final int instanceNumber = Integer.parseInt(args[4]);
		final BindableService impl = new RecordMainImpl();

		// Create a new server to listen on port
		Server server = ServerBuilder.forPort(port).addService(impl).build();

		// Start the server
		try{
			server.start();
		}
		catch(Exception e){
			System.out.println(e);
		}
		try{
			zkNaming.rebind("/grpc/bicloin/rec/" + instanceNumber , serv , Integer.toString(port));
		}
		catch(Exception e){
			System.out.println(e);
		}

		// Server threads are running in the background.
		System.out.println("Server started");

		// Do not exit the main thread. Wait until server is terminated.
		try{
			server.awaitTermination();
		}
		catch(Exception e){
			System.out.println(e);
		}

	}
	
}
