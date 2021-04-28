package pt.tecnico.rec;


import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
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

			Rec.WriteRequest writeRequest = Rec.WriteRequest.newBuilder().setName("MYLITTLETEST").setValue(Any.pack(Int32Value.newBuilder().setValue(2).build())).build();
			Rec.WriteResponse writeResponse = frontend.write(writeRequest);

			Rec.ReadRequest request = Rec.ReadRequest.newBuilder().setName("MYLITTLETEST").build();
			Rec.ReadResponse response = frontend.read(request);

			try{System.out.println(response.getValue().unpack(Int32Value.class).getValue());}
			catch (Exception e){System.out.println(e);}

		} catch (ZKNamingException e) {
			System.err.println("Caught exception when searching for Rec: " + e);
		} catch (StatusRuntimeException e) {
			System.err.println("Caught exception with description: " + e.getStatus().getDescription());
		}
	}
	
}
