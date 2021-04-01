package pt.tecnico.bicloin.app;

import java.util.Scanner;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.tecnico.bicloin.hub.grpc.HubServiceGrpc;
import pt.ulisboa.tecnico.sdis.zk.*;

public class AppMain {

	public static void main(String[] args) {
		System.out.println(AppMain.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// Check arguments
		if (args.length != 6) {
			System.err.println("ERROR incorrect number of arguments.");

			// FIXME System.err.printf("Usage: java %s port%n", AppMain.class.getName());
			return;
		}

		final String zooKeeperServ = args[0];
		final String zooKeeperPort = args[1];
		final ZKNaming zkNaming = new ZKNaming(zooKeeperServ, zooKeeperPort);
		final String uid = args[2];
		final String phone = args[3];
		final float lat = Float.parseFloat(args[4]);
		final float lon = Float.parseFloat(args[5]);
		String target = "";

		try {
			// FIXME not sure this is correct shouldn't need to use 1
			target = zkNaming.lookup("grpc/bicloin/hub/1").getURI();

		} catch (ZKNamingException e) {
			System.err.println(e.getMessage());
		}

		final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).
				usePlaintext().build();
		HubServiceGrpc.HubServiceBlockingStub stub = HubServiceGrpc.newBlockingStub(channel);
		App app = new App(lat, lon, uid, phone, stub);


		Scanner in = new Scanner(System.in);
		System.out.print(">");
		while (in.hasNextLine()) {
			command(in.nextLine(), app);
			System.out.print(">");
		}

		in.close();
		channel.shutdownNow();
	}


	private static void command(String s, App app) {
		String[] content = s.split(" ");

		try {
			if (content.length == 1 && content[0].equals("balance")) {
				System.out.println(app.balance());
			} else if (content.length == 2 && content[0].equals("top-up")) {
				System.out.println(app.topUp(Integer.parseInt(content[1])));
			} else if (content.length == 4 && content[0].equals("tag")) {
				System.out.println(app.tag(Float.parseFloat(content[1]), Float.parseFloat(content[2]), content[3]));
			} else if (content.length == 2 && content[0].equals("move")) {
				System.out.println(app.move(content[1]));
			} else if (content.length == 1 && content[0].equals("at")) {
				System.out.println(app.at());
			} else if (content.length == 2 && content[0].equals("scan")) {
				System.out.println(app.scan(Integer.parseInt(content[1])));
			} else if (content.length == 2 && content[0].equals("info")) {
				System.out.println(app.info(content[1]));
			} else if (content.length == 2 && content[0].equals("bike-up")) {
				System.out.println(app.bikeUp(content[1]));
			} else if (content.length == 2 && content[0].equals("bike-down")) {
				System.out.println(app.bikeDown(content[1]));
			} else if (content.length == 1 && content[0].equals("ping")) {
				System.out.println(app.ping());
			} else if (content.length == 1 && content[0].equals("sys_status")) {
				System.out.println(app.sys_status());
			} else if (content.length == 2 && content[0].equals("zzz")) {
				Thread.sleep(Integer.parseInt(content[1]));
				System.out.printf("Sleeping for %s%n", content[1]);
			} else if (content.length == 0 || content[0].equals("#")) {
				// skip comments and empty lines
				assert true;
			} else throw new Exception();
			// FIXME implement new Exception


		} catch (NumberFormatException e) {
			System.out.println("Invalid parameters, try >help");
		} catch (StatusRuntimeException e) {
			System.out.println(e.getStatus().getDescription());
		} catch (Exception e) {
			System.out.println("Unknown exception");
		}
	}

}
