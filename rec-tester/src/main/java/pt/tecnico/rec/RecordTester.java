package pt.tecnico.rec;


import io.grpc.StatusRuntimeException;
import pt.tecnico.rec.grpc.Rec;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class RecordTester {
	
	public static void main(String[] args) throws ZKNamingException {
		System.out.println(RecordTester.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// Check arguments
		if (args.length != 2) {
			System.err.println("ERROR incorrect number of arguments.");
			System.err.printf("Usage: java %s host port %n", RecordTester.class.getName());
			return;
		}

		final String host = args[0];
		final int port = Integer.parseInt(args[1]);

		RecFrontend frontend = new RecFrontend(host, port);

		try {
			Rec.CtrlPingRequest request = Rec.CtrlPingRequest.newBuilder().setInput("friend").build();
			Rec.CtrlPingResponse response = frontend.ctrlPing(request);
			System.out.println(response.getOutput());

		} catch (StatusRuntimeException e) {
			System.err.println("Caught exception with description: " + e.getStatus().getDescription());
		}

		frontend.close();
	}
	
}
