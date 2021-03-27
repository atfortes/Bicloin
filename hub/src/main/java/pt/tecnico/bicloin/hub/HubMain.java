package pt.tecnico.bicloin.hub;


public class HubMain {
	
	public static void main(String[] args) {
		System.out.println(HubMain.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}
	}
	
}
