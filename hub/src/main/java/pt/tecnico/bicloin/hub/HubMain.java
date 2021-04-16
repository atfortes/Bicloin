package pt.tecnico.bicloin.hub;

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.bicloin.hub.domain.Station;
import pt.tecnico.bicloin.hub.domain.User;
import pt.tecnico.rec.RecFrontend;
import pt.tecnico.rec.grpc.Rec;
import pt.ulisboa.tecnico.sdis.zk.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class HubMain {

	private static String zooHost;
	private static int zooPort;
	private static ZKNaming zkNaming = null;
	private static String path;
	private static String host;
	private static int port;
	private static String usersFile;
	private static String stationsFile;
	private static final String recPath = "/grpc/bicloin/rec/1";
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
			System.err.printf("Usage: java %s zooHost zooPort host port instanceNumber usersFile stationsFile " +
					"[initRec]%n", HubMain.class.getName());
			return;
		}

		zooHost = args[0];
		zooPort = Integer.parseInt(args[1]);

		host = args[2];
		port = Integer.parseInt(args[3]);
		int instanceNumber = Integer.parseInt(args[4]);
		path = "/grpc/bicloin/hub/" + instanceNumber;

		usersFile = args[5];
		stationsFile = args[6];
		boolean initRec = args.length == 8 && args[7].equals("initRec");

		try(RecFrontend frontend = new RecFrontend(zooHost, zooPort, recPath)) {

			importUsers(frontend, initRec);
			importStations(frontend, initRec);

			final BindableService impl = new HubImpl(userList, stationList, frontend);

			// Create a new server to listen on port
			Server server = ServerBuilder.forPort(port).addService(impl).build();

			// Start the server
			startServer(server);

			// Register on ZooKeeper
			registerZookeeper();

			// Use hook to register a thread to be called on shutdown.
			Runtime.getRuntime().addShutdownHook(new Unbind());

			System.out.println("Server started and awaiting requests on port " + port + ".");

			// Do not exit the main thread. Wait until server is terminated.
			server.awaitTermination();

		} catch (ZKNamingException e) {
			System.err.println("Caught exception during Zookeeper bind: " + e);
		} catch(InterruptedException e){
			System.err.println("Server was interrupted.");
		} catch (IOException | ImportDataException e) {
			System.err.println(e.getMessage());
		} catch (NumberFormatException e) {
			System.err.println("NumberFormatException: " + e.getMessage());
		}
	}

	public static void importUsers(RecFrontend frontend, boolean initRec) throws IOException, ImportDataException {

		try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(
				System.getProperty("user.dir") + "/" + usersFile)))) {

			String line = "";
			while ((line = br.readLine()) != null) {
				String[] userDetails = line.split(",");

				if(userDetails.length > 0 ) {

					if(userDetails[0].length() < 3 || userDetails[0].length() > 10 || !userDetails[0].matches("[A-Za-z0-9]+")) {
						throw new ImportDataException("Invalid username: " + userDetails[0]);
					}

					if(userDetails[1].length() < 3 || userDetails[1].length() > 30) {
						throw new ImportDataException("Invalid name: " + userDetails[1]);
					}

					if(!userDetails[2].matches("^\\+\\d{3,15}$")) {
						throw new ImportDataException("Invalid phone number: " + userDetails[2]);
					}

					User u = new User(userDetails[0], userDetails[1], userDetails[2]);
					userList.add(u);

					if(initRec) {
						frontend.write(Rec.WriteRequest.newBuilder().setName("users/" + u.getUsername() + "/balance")
								.setValue(Any.pack(Int32Value.newBuilder().setValue(0).build())).build());
						frontend.write(Rec.WriteRequest.newBuilder().setName("users/" + u.getUsername() + "/bike")
								.setValue(Any.pack(BoolValue.newBuilder().setValue(false).build())).build());

					}
				}
			}

		} catch(IOException | ImportDataException e) {
			System.err.println("Caught exception while parsing the users file: ");
			throw e;
		}
	}

	public static void importStations(RecFrontend frontend, boolean initRec) throws IOException, ImportDataException {

		try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(
				System.getProperty("user.dir") + "/" + stationsFile)))) {

			String line = "";
			while ((line = br.readLine()) != null) {
				String[] stationDetails = line.split(",");

				if(stationDetails.length > 0 ) {

					if(stationDetails[0].length() < 3 || stationDetails[0].length() > 30) {
						throw new ImportDataException("Invalid name: " + stationDetails[0]);
					}

					if(stationDetails[1].length() != 4 || !stationDetails[1].matches("[A-Za-z0-9]+")) {
						throw new ImportDataException("Invalid station id: " + stationDetails[1]);
					}

					Station s = new Station(stationDetails[0], stationDetails[1], Double.parseDouble(stationDetails[2]),
							Double.parseDouble(stationDetails[3]), Integer.parseInt(stationDetails[4]),
							Integer.parseInt(stationDetails[6]));
					stationList.add(s);

					if(initRec) {
						frontend.write(Rec.WriteRequest.newBuilder().setName("stations/" + s.getId() + "/bikes")
								.setValue(Any.pack(Int32Value.newBuilder().setValue(Integer.parseInt(stationDetails[5]))
										.build())).build());
						frontend.write(Rec.WriteRequest.newBuilder().setName("stations/" + s.getId() + "/requests")
								.setValue(Any.pack(Int32Value.newBuilder().setValue(0).build())).build());
						frontend.write(Rec.WriteRequest.newBuilder().setName("stations/" + s.getId() + "/returns")
								.setValue(Any.pack(Int32Value.newBuilder().setValue(0).build())).build());

					}
				}
			}

		} catch(IOException | ImportDataException e) {
			System.err.println("Caught exception while parsing the stations file: ");
			throw e;
		} catch (NumberFormatException e) {
			System.err.println("Caught exception while parsing the stations file: ");
			throw new ImportDataException("Non numeric value: " + e.getMessage());
		}
	}

	private static void startServer(Server server) throws IOException {
		try{

			server.start();

			// Server threads are running in the background.
			System.out.println("Server started.");

		} catch(IOException ie) {
			System.err.println("Caught exception when starting the server: ");
			throw ie;
		}
	}

	private static void registerZookeeper() throws ZKNamingException {

		System.out.println("Contacting ZooKeeper at " + zooHost + ":" + zooPort + "...");
		zkNaming = new ZKNaming(zooHost, String.valueOf(zooPort));
		System.out.println("Binding " + path + " to " + host + ":" + port + "...");
		zkNaming.rebind(path, host, String.valueOf(port));
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
