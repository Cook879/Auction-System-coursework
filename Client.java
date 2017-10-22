import java.util.UUID;

/**
 * Client for the auction system - presents a GUI to the user.
 * 
 * @author Richard Cook
 * @version 1.0
 * @date May 15, 2014
 */
public class Client {
	
	private static String clientID; // Used for communication with the server
	private static Comms c; 
	private String userID = null; // So we can get notifications for the user
	private static GUI f;
	private Thread checkNotifications = null; // Stored here so we can end it if the user logs out

	/**
	 * Main method simply creates a new instance of the class.
	 *  
	 * No command line arguments taken
	 */
	public static void main( String[] args ) {
		new Client();
	}
	
	public Client() {
		clientID = UUID.randomUUID().toString(); // Create a nice complex random client ID
		
		// Setting up the communications layer
		c = new Comms(); 
		c.createClientInbox( clientID ); 

		// Create the GUI
		f = new GUI( this ); 
		f.setScreen( new LogInPanel( f ) );
	}
	
	/**
	 * Creates a new NotificationThread which will check the server for notifications for the user who is currently logged in
	 */
	public void checkNotifications() {
		checkNotifications = new Thread (new NotificationThread( userID, f ) );
		checkNotifications.start();
	}

	
	/**
	 * Setters and getters
	 */
	public String getClientID() { return clientID; }
	public Comms getComms() { return c; }
	public String getUserID() { return userID;}
	
	/**
	 * To options to this one - it will be called both when logging in and logging out 
	 * 		and we want different behaviours for both
	 * Namely stop checking for notifications when logging out and start when logging in
	 * @param userID String
	 */
	public void setUserID( String userID ) {
		if( checkNotifications != null )
			checkNotifications.interrupt();
		this.userID = userID; 
		if( userID != null )
			checkNotifications();
	}
}	

/**
 * Class which checks the server for notifications via the Comms class.
 * 
 * @author Richard Cook
 * @version 1.0
 * @date May 15, 2014
 */
class NotificationThread implements Runnable {

	private Comms c;
	private String userID;
	private GUI f; // Need the GUI to add a notification to it
	
	public NotificationThread( String userID, GUI f ) {
		c = new Comms();
		this.userID = userID;
		this.f = f;
	}
	
	/**
	 * Constantly calls the checkNotification method in the Comms class. 
	 * When this returns true it gets the Notification and adds it to the GUI
	 */
	@Override
	public void run() {
		while( !Thread.interrupted() ) {
			if( c.checkNotification( userID ) ) {
				Notification n = c.receiveNotification( userID );
				f.addNotification( n );
			}
		}
	}
}