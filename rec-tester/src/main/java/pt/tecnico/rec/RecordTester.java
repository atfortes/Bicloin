package pt.tecnico.rec;


import com.google.protobuf.*;
import io.grpc.Status;
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
		if (args.length != 4) {
			System.err.println("ERROR incorrect number of arguments.");
			System.err.printf("Usage: java %s zooHost zooPort path cid %n", RecordTester.class.getName());
			return;
		}

		final String zooHost = args[0];
		final int zooPort = Integer.parseInt(args[1]);
		final String path = args[2];
		final int cid = Integer.parseInt(args[3]);

		try(RecFrontend frontend = new RecFrontend(zooHost, zooPort, path, cid)) {

			Rec.WriteRequest writeRequest = Rec.WriteRequest.newBuilder().setName("test").setValue(Any.pack(Int32Value.newBuilder().setValue(2).build())).build();
			frontend.write(writeRequest);
			Rec.ReadRequest request = Rec.ReadRequest.newBuilder().setName("test").build();
			Rec.ReadResponse response = frontend.read(request);

			System.out.print(response.getValue().unpack(Int32Value.class));

			Rec.CtrlPingRequest pingRequest = Rec.CtrlPingRequest.newBuilder().setInput("OK").build();
			System.out.println(frontend.ctrlPing(pingRequest).getOutput());

		} catch (ZKNamingException e) {
			System.err.println("Caught exception when searching for Rec: " + e);
		} catch (StatusRuntimeException e) {
			if (e.getStatus() == Status.NOT_FOUND)
				System.out.println("Rec not found");
			else
				System.err.println("Caught exception with description: " + e.getStatus().getDescription());
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
	}
	
}
