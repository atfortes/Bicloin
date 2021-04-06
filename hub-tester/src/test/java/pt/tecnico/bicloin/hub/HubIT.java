package pt.tecnico.bicloin.hub;

import org.junit.jupiter.api.*;
import pt.tecnico.bicloin.hub.grpc.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HubIT extends BaseIT{

	static final String TEST_USER_NAME = "alice";
	static final String TEST_USER_FULL_NAME = "Alice Andrade";
	static final String TEST_USER_PHONE = "+35191102030";
	static final String TEST_INITIAL_USER_BALANCE = "0";
	static final String TEST_FINAL_USER_BALANCE = "100";
	static final int TEST_USER_TOP_UP = 10;

	static final float TEST_LATITUDE_1 = 3;
	static final float TEST_LONGITUDE_1 = 3;

	static final String TEST_STATION_ID = "ocea";
	static final String TEST_STATION_NAME = "Oceanário";
	static final float TEST_STATION_LATITUDE = (float) 38.7633;
	static final float TEST_STATION_LONGITUDE = (float) -9.0950;
	static final int TEST_STATION_CAPACITY = 20;
	static final int TEST_STATION_PRIZE = 2;
	static final int TEST_STATION_NUMBER_OF_BIKES = 15;
	static final int TEST_STATION_NUMBER_OF_PICKUPS = 0;
	static final int TEST_STATION_NUMBER_OF_DELIVERIES = 0;

	static final String TEST_STATION_ID_2 = "ista";
	static final String TEST_STATION_ID_3 = "gulb";

	static final String TEST_MESSAGE = "TEST";

	static HubFrontend frontend = null;
	
	
	// one-time initialization and clean-up
	@BeforeAll
	public static void oneTimeSetUp(){
		try {
			frontend = new HubFrontend(testProps.getProperty("zook.serv"), Integer.parseInt(testProps.getProperty("zook.port")), testProps.getProperty("hub.path"));
		}
		catch (ZKNamingException e){
			System.out.println("Could not close connection with ZooKeeper: " + e);
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
		// reset
	}
		
	// tests 
	
	@Test
	public void testCtrlPing() {
		CtrlPingRequest request = CtrlPingRequest.newBuilder().setInput(TEST_MESSAGE).build();
		CtrlPingResponse response = frontend.ctrlPing(request);
		assertEquals(TEST_MESSAGE,response.getOutput());
	}

	@Test
	public void testBalance() {
		BalanceRequest request = BalanceRequest.newBuilder().setUsername(TEST_USER_NAME).build();
		BalanceResponse response = frontend.balance(request);
		assertEquals(TEST_INITIAL_USER_BALANCE, String.valueOf(response.getBalance()));
	}

	@Test
	public void testTopUp() {
		TopUpRequest request = TopUpRequest.newBuilder()
				.setUsername(TEST_USER_NAME)
				.setAmount(TEST_USER_TOP_UP)
				.setPhoneNumber(TEST_USER_PHONE)
				.build();
		TopUpResponse response = frontend.topUp(request);
		assertEquals(TEST_FINAL_USER_BALANCE,String.valueOf(response.getBalance()));
	}

	@Test
	public void testLocateStation() {
		int k = 3;
		LocateStationRequest request = LocateStationRequest.newBuilder()
				.setLatitude(TEST_STATION_LATITUDE)
				.setLongitude(TEST_STATION_LONGITUDE)
				.setK(k)
				.build();
		LocateStationResponse response = frontend.locateStation(request);
		assertEquals(TEST_STATION_ID, response.getIds(0));
		assertEquals(TEST_STATION_ID_2, response.getIds(1));
		assertEquals(TEST_STATION_ID_3, response.getIds(2));
	}

	@Test
	public void testInfoStation() {
		InfoStationRequest request = InfoStationRequest.newBuilder().setStationId(TEST_STATION_ID).build();
		InfoStationResponse response = frontend.infoStation(request);
		assertEquals(TEST_STATION_NAME, response.getName());
		assertEquals(TEST_STATION_LATITUDE, response.getLatitude());
		assertEquals(TEST_STATION_LONGITUDE, response.getLongitude());
		assertEquals(TEST_STATION_CAPACITY, response.getCapacity());
		assertEquals(TEST_STATION_PRIZE, response.getAward());
		assertEquals(TEST_STATION_NUMBER_OF_BIKES, response.getBikes());
		assertEquals(TEST_STATION_NUMBER_OF_PICKUPS, response.getPickups());
		assertEquals(TEST_STATION_NUMBER_OF_DELIVERIES, response.getDeliveries());
	}

	/*

	// TO DO
	@Test
	public void testSysStatus() {
		SysStatusRequest request = SysStatusRequest.newBuilder().build();
		SysStatusResponse response = frontend.sysStatus(request);
		assertEquals(TEST_MESSAGE,response.getOutput());
	}

	// TO DO
	@Test
	public void testBikeUp() {
		BikeRequest request = BikeRequest.newBuilder()
				.setUsername(TEST_USER_NAME)
				.setLatitude(TEST_LATITUDE_1)
				.setLongitude(TEST_LONGITUDE_1)
				.setStationId(TEST_STATION_ID)
				.build();
		BikeResponse response = frontend.bikeUp(request);
		assertEquals(,response.getResponse());
	}

	// TO DO
	@Test
	public void testBikeDown() {
		BikeRequest request = BikeRequest.newBuilder()
				.setUsername(TEST_USER_NAME)
				.setLatitude(TEST_LATITUDE_1)
				.setLongitude(TEST_LONGITUDE_1)
				.setStationId(TEST_STATION_ID)
				.build();
		BikeResponse response = frontend.bikeDown(request);
		assertEquals(,response.getResponse());
	}
	*/

}
