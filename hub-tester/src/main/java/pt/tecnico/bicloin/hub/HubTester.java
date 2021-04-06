package pt.tecnico.bicloin.hub;


import io.grpc.StatusRuntimeException;
import pt.tecnico.bicloin.hub.grpc.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class HubTester {
	
	public static void main(String[] args) {
		System.out.println(HubTester.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}
		// Check arguments
		if (args.length != 3) {
			System.err.println("ERROR incorrect number of arguments.");
			System.err.printf("Usage: java %s zooHost zooPort path %n", HubTester.class.getName());
			return;
		}

		final String zooHost = args[0];
		final int zooPort = Integer.parseInt(args[1]);
		String path = args[2];
		HubFrontend frontend = null;
		try {
			frontend = new HubFrontend(zooHost, zooPort, path);
			CtrlPingRequest request = CtrlPingRequest.newBuilder().setInput("friend").build();
			CtrlPingResponse response = frontend.ctrlPing(request);
			System.out.println(response.getOutput());

		} catch (ZKNamingException e) {
			System.err.println("Caught exception when searching for Rec: " + e);
		} catch (StatusRuntimeException e) {
			System.err.println("Caught exception with description: " + e.getStatus().getDescription());
		}

		if (frontend != null) {
			frontend.close();
		}
	}
	
}
