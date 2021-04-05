package pt.tecnico.rec;


import io.grpc.StatusRuntimeException;
import pt.tecnico.rec.grpc.Rec;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class RecordTester {
	
	public static void main(String[] args) {
		System.out.println(RecordTester.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// Check arguments
		if (args.length != 3) {
			System.err.println("ERROR incorrect number of arguments.");
			System.err.printf("Usage: java %s zooHost zooPort path %n", RecordTester.class.getName());
			return;
		}

		final String zooHost = args[0];
		final int zooPort = Integer.parseInt(args[1]);
		final String path = args[2];

		try(RecFrontend frontend = new RecFrontend(zooHost, zooPort, path)) {

			Rec.CtrlPingRequest request = Rec.CtrlPingRequest.newBuilder().setInput("friend").build();
			Rec.CtrlPingResponse response = frontend.ctrlPing(request);
			System.out.println(response.getOutput());

		} catch (ZKNamingException e) {
			System.err.println("Caught exception when searching for Rec: " + e);
		} catch (StatusRuntimeException e) {
			System.err.println("Caught exception with description: " + e.getStatus().getDescription());
		}
	}
	
}
