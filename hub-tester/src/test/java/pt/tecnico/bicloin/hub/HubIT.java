package pt.tecnico.bicloin.hub;

import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.*;
import pt.tecnico.bicloin.hub.grpc.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HubIT extends BaseIT{

	static final String HUB_PASSWORD = "super_strong_reset_password";

	static final String TEST_USER_NAME = "alice";
	static final String TEST_USER_FULL_NAME = "Alice Andrade";
	static final String TEST_USER_PHONE = "+35191102030";
	static final String TEST_INITIAL_USER_BALANCE = "0";
	static final String TEST_FINAL_USER_BALANCE = "100";
	static final int TEST_USER_TOP_UP = 10;

	static final double TEST_LATITUDE_1 = 3;
	static final double TEST_LONGITUDE_1 = 3;

	static final String TEST_STATION_ID = "ocea";
	static final String TEST_STATION_NAME = "Oceanário";
	static final double TEST_STATION_LATITUDE = 38.7633;
	static final double TEST_STATION_LONGITUDE = -9.0950;
	static final int TEST_STATION_CAPACITY = 20;
	static final int TEST_STATION_PRIZE = 2;
	static final int TEST_STATION_NUMBER_OF_BIKES = 15;
	static final int TEST_STATION_NUMBER_OF_PICKUPS = 0;
	static final int TEST_STATION_NUMBER_OF_DELIVERIES = 0;

	static final String TEST_STATION_ID_2 = "ista";
	static final String TEST_STATION_ID_3 = "gulb";

	static final String TEST_STATION_NO_BIKES_ID = "cate";
	static final double TEST_STATION_NO_BIKES_LATITUDE = 38.7097;
	static final double TEST_STATION_NO_BIKES_LONGITUDE = -9.1336;

	static final String TEST_STATION_FULL_ID = "gulb";
	static final double TEST_STATION_FULL_LATITUDE = 38.7376;
	static final double TEST_STATION_FULL_LONGITUDE = -9.1545;

	static final String TEST_MESSAGE = "TEST";

	static final int TEST_USER_WRONG_TOP_UP = 25;
	static final String TEST_USER_WRONG_NAME = "nimbus";
	static final String TEST_USER_WRONG_PHONE = "+351961789819";

	static final String TEST_STATION_WRONG_ID = "notid";

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
		CtrlResetRequest request = CtrlResetRequest.newBuilder().setPassword(HUB_PASSWORD).build();
		frontend.ctrlReset(request);
	}
		
	// tests 
	
	@Test
	public void testCtrlPingSuccess() {
		CtrlPingRequest request = CtrlPingRequest.newBuilder().setInput(TEST_MESSAGE).build();
		CtrlPingResponse response = frontend.ctrlPing(request);
		assertEquals(TEST_MESSAGE,response.getOutput());
	}

	@Test
	public void testBalanceSuccess() {
		BalanceRequest request = BalanceRequest.newBuilder().setUsername(TEST_USER_NAME).build();
		BalanceResponse response = frontend.balance(request);
		assertEquals(TEST_INITIAL_USER_BALANCE, String.valueOf(response.getBalance()));
	}

	@Test
	public void testBalanceFailureWrongUsername() {
		BalanceRequest request = BalanceRequest.newBuilder().setUsername(TEST_USER_WRONG_NAME).build();
		assertThrows(StatusRuntimeException.class, () -> {frontend.balance(request);});
	}

	@Test
	public void testTopUpSuccess() {
		TopUpRequest request = TopUpRequest.newBuilder()
				.setUsername(TEST_USER_NAME)
				.setAmount(TEST_USER_TOP_UP)
				.setPhoneNumber(TEST_USER_PHONE)
				.build();
		TopUpResponse response = frontend.topUp(request);
		assertEquals(TEST_FINAL_USER_BALANCE,String.valueOf(response.getBalance()));
	}

	@Test
	public void testTopUpFailureWrongUsername() {
		TopUpRequest request = TopUpRequest.newBuilder()
				.setUsername(TEST_USER_WRONG_NAME)
				.setAmount(TEST_USER_TOP_UP)
				.setPhoneNumber(TEST_USER_PHONE)
				.build();
		assertThrows(StatusRuntimeException.class, () -> {frontend.topUp(request);});
	}

	@Test
	public void testTopUpFailureInvalidAmount() {
		TopUpRequest request = TopUpRequest.newBuilder()
				.setUsername(TEST_USER_NAME)
				.setAmount(TEST_USER_WRONG_TOP_UP)
				.setPhoneNumber(TEST_USER_PHONE)
				.build();
		assertThrows(StatusRuntimeException.class, () -> {frontend.topUp(request);});
	}

	@Test
	public void testTopUpFailureWrongPhone() {
		TopUpRequest request = TopUpRequest.newBuilder()
				.setUsername(TEST_USER_NAME)
				.setAmount(TEST_USER_TOP_UP)
				.setPhoneNumber(TEST_USER_WRONG_PHONE)
				.build();
		assertThrows(StatusRuntimeException.class, () -> {frontend.topUp(request);});
	}

	@Test
	public void testLocateStationSuccess() {
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
	public void testLocateStationFailureInvalidK() {
		int k = -1;
		LocateStationRequest request = LocateStationRequest.newBuilder()
				.setLatitude(TEST_STATION_LATITUDE)
				.setLongitude(TEST_STATION_LONGITUDE)
				.setK(k)
				.build();
		assertThrows(StatusRuntimeException.class, () -> {frontend.locateStation(request);});
	}

	@Test
	public void testInfoStationSuccess() {
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

	@Test
	public void testInfoStationFailureWrongStationId() {
		InfoStationRequest request = InfoStationRequest.newBuilder().setStationId(TEST_STATION_WRONG_ID).build();
		assertThrows(StatusRuntimeException.class, () -> {frontend.infoStation(request);});
	}

	@Test
	public void testBikeUpSuccess() {
		testTopUpSuccess();
		BikeRequest bikeUpRequest = BikeRequest.newBuilder()
				.setUsername(TEST_USER_NAME)
				.setLatitude(TEST_STATION_LATITUDE)
				.setLongitude(TEST_STATION_LONGITUDE)
				.setStationId(TEST_STATION_ID)
				.build();
		BikeResponse response = frontend.bikeUp(bikeUpRequest);
		assertEquals(BikeResponse.Response.OK,response.getResponse());
	}

	@Test
	public void testBikeUpFailureWrongUsername() {
		BikeRequest request = BikeRequest.newBuilder()
				.setUsername(TEST_USER_WRONG_NAME)
				.setLatitude(TEST_STATION_LATITUDE)
				.setLongitude(TEST_STATION_LONGITUDE)
				.setStationId(TEST_STATION_ID)
				.build();
		assertThrows(StatusRuntimeException.class, () -> {frontend.bikeUp(request);});
	}

	@Test
	public void testBikeUpFailureWrongStationId() {
		BikeRequest request = BikeRequest.newBuilder()
				.setUsername(TEST_USER_NAME)
				.setLatitude(TEST_STATION_LATITUDE)
				.setLongitude(TEST_STATION_LONGITUDE)
				.setStationId(TEST_STATION_WRONG_ID)
				.build();
		assertThrows(StatusRuntimeException.class, () -> {frontend.bikeUp(request);});
	}

	@Test
	public void testBikeUpFailureFailureOutOfRange() {
		testTopUpSuccess();
		BikeRequest request = BikeRequest.newBuilder()
				.setUsername(TEST_USER_NAME)
				.setLatitude(TEST_LATITUDE_1)
				.setLongitude(TEST_LONGITUDE_1)
				.setStationId(TEST_STATION_ID)
				.build();
		BikeResponse response = frontend.bikeUp(request);
		assertEquals(BikeResponse.Response.OUT_OF_RANGE,response.getResponse());
	}

	@Test
	public void testBikeUpFailureOutOfMoney() {
		BikeRequest bikeUpRequest = BikeRequest.newBuilder()
				.setUsername(TEST_USER_NAME)
				.setLatitude(TEST_STATION_LATITUDE)
				.setLongitude(TEST_STATION_LONGITUDE)
				.setStationId(TEST_STATION_ID)
				.build();
		BikeResponse response = frontend.bikeUp(bikeUpRequest);
		assertEquals(BikeResponse.Response.OUT_OF_MONEY,response.getResponse());
	}

	@Test
	public void testBikeUpFailureAlreadyHasBike() {
		testBikeUpSuccess();
		BikeRequest request = BikeRequest.newBuilder()
				.setUsername(TEST_USER_NAME)
				.setLatitude(TEST_STATION_LATITUDE)
				.setLongitude(TEST_STATION_LONGITUDE)
				.setStationId(TEST_STATION_ID)
				.build();
		BikeResponse response = frontend.bikeUp(request);
		assertEquals(BikeResponse.Response.ALREADY_HAS_BIKE,response.getResponse());
	}

	@Test
	public void testBikeUpFailureNoBikesInStation() {
		testTopUpSuccess();
		BikeRequest request = BikeRequest.newBuilder()
				.setUsername(TEST_USER_NAME)
				.setLatitude(TEST_STATION_NO_BIKES_LATITUDE)
				.setLongitude(TEST_STATION_NO_BIKES_LONGITUDE)
				.setStationId(TEST_STATION_NO_BIKES_ID)
				.build();
		BikeResponse response = frontend.bikeUp(request);
		assertEquals(BikeResponse.Response.NO_BIKES_IN_STATION,response.getResponse());
	}

	@Test
	public void testBikeDownSuccess() {
		testBikeUpSuccess();
		BikeRequest request = BikeRequest.newBuilder()
				.setUsername(TEST_USER_NAME)
				.setLatitude(TEST_STATION_LATITUDE)
				.setLongitude(TEST_STATION_LONGITUDE)
				.setStationId(TEST_STATION_ID)
				.build();
		BikeResponse response = frontend.bikeDown(request);
		assertEquals(BikeResponse.Response.OK,response.getResponse());
	}

	@Test
	public void testBikeDownFailureWrongUsername() {
		BikeRequest request = BikeRequest.newBuilder()
				.setUsername(TEST_USER_WRONG_NAME)
				.setLatitude(TEST_LATITUDE_1)
				.setLongitude(TEST_LONGITUDE_1)
				.setStationId(TEST_STATION_ID)
				.build();
		assertThrows(StatusRuntimeException.class, () -> {frontend.bikeDown(request);});
	}

	@Test
	public void testBikeDownFailureWrongStationId() {
		BikeRequest request = BikeRequest.newBuilder()
				.setUsername(TEST_USER_NAME)
				.setLatitude(TEST_LATITUDE_1)
				.setLongitude(TEST_LONGITUDE_1)
				.setStationId(TEST_STATION_WRONG_ID)
				.build();
		assertThrows(StatusRuntimeException.class, () -> {frontend.bikeDown(request);});
	}

	@Test
	public void testBikeDownFailureOutOfRange() {
		testBikeUpSuccess();
		BikeRequest request = BikeRequest.newBuilder()
				.setUsername(TEST_USER_NAME)
				.setLatitude(TEST_LATITUDE_1)
				.setLongitude(TEST_LONGITUDE_1)
				.setStationId(TEST_STATION_ID)
				.build();
		BikeResponse response = frontend.bikeDown(request);
		assertEquals(BikeResponse.Response.OUT_OF_RANGE,response.getResponse());
	}

	@Test
	public void testBikeDownFailureNoBikeRequested() {
		BikeRequest request = BikeRequest.newBuilder()
				.setUsername(TEST_USER_NAME)
				.setLatitude(TEST_STATION_LATITUDE)
				.setLongitude(TEST_STATION_LONGITUDE)
				.setStationId(TEST_STATION_ID)
				.build();
		BikeResponse response = frontend.bikeDown(request);
		assertEquals(BikeResponse.Response.NO_BIKE_REQUESTED,response.getResponse());
	}

	@Test
	public void testBikeDownFailureStationIsFull() {
		testBikeUpSuccess();
		BikeRequest request = BikeRequest.newBuilder()
				.setUsername(TEST_USER_NAME)
				.setLatitude(TEST_STATION_FULL_LATITUDE)
				.setLongitude(TEST_STATION_FULL_LONGITUDE)
				.setStationId(TEST_STATION_FULL_ID)
				.build();
		BikeResponse response = frontend.bikeDown(request);
		assertEquals(BikeResponse.Response.STATION_IS_FULL,response.getResponse());
	}

	@Test
	public void testSysStatusSuccess() {
		SysStatusRequest request = SysStatusRequest.newBuilder().build();
		SysStatusResponse response = frontend.sysStatus(request);
		assertNotEquals(0, response.getSequenceList().size());
	}

}
