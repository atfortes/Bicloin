package pt.tecnico.rec;

import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.*;
import pt.tecnico.rec.grpc.Rec;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RecordIT extends BaseIT {

	static final String TEST_MESSAGE = "TEST";
	static final String TEST_NAME = "name";
	static final String INVALID_TEST_NAME = "";
	static final String TEST_VALUE_STRING = "value";
	static final Any TEST_VALUE = Any.pack(StringValue.newBuilder().setValue(TEST_VALUE_STRING).build());

	static RecFrontend frontend = null;


	// one-time initialization and clean-up
	@BeforeAll
	public static void oneTimeSetUp(){
		try {
			frontend = new RecFrontend(testProps.getProperty("zook.serv"), Integer.parseInt(testProps.getProperty("zook.port")), testProps.getProperty("rec.path"));
		}
		catch (ZKNamingException e){
			System.err.println("Caught exception during Zookeeper bind: " + e);
		}
	}
	
	@AfterAll
	public static void oneTimeTearDown() {
		frontend.close();
	}
	
	// initialization and clean-up for each test
	
	@BeforeEach
	public void setUp() {
		
	}
	
	@AfterEach
	public void tearDown() {

	}
		
	// tests 

	@Test
	public void testCtrlPingSuccess() {
		Rec.CtrlPingRequest request = Rec.CtrlPingRequest.newBuilder().setInput(TEST_MESSAGE).build();
		Rec.CtrlPingResponse response = frontend.ctrlPing(request);
		assertEquals(TEST_MESSAGE,response.getOutput());
	}

	@Test
	public void testReadWriteSuccess() {
		Rec.WriteRequest requestW = Rec.WriteRequest.newBuilder().setName(TEST_NAME).setValue(TEST_VALUE).build();
		frontend.write(requestW);
		Rec.ReadRequest requestR = Rec.ReadRequest.newBuilder().setName(TEST_NAME).build();
		Rec.ReadResponse response = frontend.read(requestR);
		assertEquals(TEST_VALUE, response.getValue());
	}

	@Test
	public void testWriteFailure() {
		Rec.WriteRequest request = Rec.WriteRequest.newBuilder().setName(INVALID_TEST_NAME).setValue(TEST_VALUE).build();
		assertThrows(StatusRuntimeException.class, () -> {frontend.write(request);});
	}

	@Test
	public void testReadFailure() {
		Rec.ReadRequest request = Rec.ReadRequest.newBuilder().setName(INVALID_TEST_NAME).build();
		assertThrows(StatusRuntimeException.class, () -> {frontend.read(request);});
	}

}
