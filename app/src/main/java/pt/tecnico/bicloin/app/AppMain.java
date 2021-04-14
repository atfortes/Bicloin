package pt.tecnico.bicloin.app;

import java.util.Scanner;
import io.grpc.*;
import pt.tecnico.bicloin.hub.HubFrontend;
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
			System.err.printf("Usage: java %s zk_ip zk_port uid phone lat long%n", AppMain.class.getName());
			return;
		}

		final String zooKeeperServ = args[0];
		final String zooKeeperPort = args[1];
		final String uid = args[2];
		final String phone = args[3];
		final float lat = Float.parseFloat(args[4]);
		final float lon = Float.parseFloat(args[5]);

		try (HubFrontend frontend = new HubFrontend(zooKeeperServ, Integer.parseInt(zooKeeperPort), "/grpc/bicloin/hub")) {

			App app = new App(lat, lon, uid, phone, frontend);
			Scanner in = new Scanner(System.in);

			System.out.print("> ");
			while (in.hasNextLine()) {
				command(in.nextLine(), app);
				System.out.print("> ");
			}

			in.close();

		} catch (ZKNamingException e) {
			System.err.println(e.getMessage());
			System.err.println("Exception reaching hub, shutting down");
		} catch (NumberFormatException e) {
			System.err.println(e.getMessage());
			System.err.println("Incorrect arguments");
		}
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
			} else if (content.length == 3 && content[0].equals("move")) {
				System.out.println(app.move(Float.parseFloat(content[1]), Float.parseFloat(content[2])));
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
			} else if (content.length == 1 && content[0].equals("help")) {
				System.out.println(help());
			} else if (content.length == 2 && content[0].equals("zzz")) {
				System.out.printf("sleeping for %s ms%n", content[1]);
				Thread.sleep(Integer.parseInt(content[1]));
			} else if (content.length > 0 && content[0].charAt(0) == '#') {
				// skip comments and empty lines
				assert true;
			} else System.out.println("incorrect usage, try the command help");


		} catch (NumberFormatException e) {
			System.out.println("Incorrect usage, try the command help");
		} catch (StatusRuntimeException e) {
			System.out.println(e.getStatus().getDescription());
		} catch (BicloinAppException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			System.out.println("Unexpected exception");
		}
	}

	private static String help() {
		return "Application usage:\n" +
				"> balance\n" +
				"> top-up amount\n" +
				"> tag lat long name\n" +
				"> move name\n" +
				"> move lat long\n" +
				"> at\n" +
				"> scan n\n" +
				"> info station-id\n" +
				"> bike-up station-id\n" +
				"> bike-down station-id\n" +
				"> ping\n" +
				"> sys_status\n" +
				"> zzz time(ms)\n";
	}

}
