import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

public class Server {

	private static Map<String, User> userList; // List of users in the system
	private static Map<String, Item> itemList; // List of items in the system
	// PriorityQueue used to determine which auction ends next
	private static PriorityQueue<Item> activeAuctions;  
	private static List<Item> won; // List of auctions won - for server report
	private static Comms comms; 
	private static DataPersistence dp;
	private static JTextPane tpConsole;
	
	/**
	 * Sets up and runs the server
	 */
	public static void main( String[] args ) {


		// Set up variables
		comms = new Comms();
		userList = new HashMap<String, User>();
		itemList = new HashMap<String, Item>();
		activeAuctions = new PriorityQueue<Item>();
		won = new ArrayList<Item>();
		dp = new DataPersistence();
		
		// Call before we do any outputting
		// But has to be after we set things up otherwise we have issues
		redirectSystemStreams();
		
		// Create the GUI for the server report button and console
		createGUI();
		
		// If there's saved data, load it.
		if( dp.saveDataExists() ) {
			dp.readData();
			// Has to be done after reading data or we overwrite the old activity log
			System.out.println( "Server initilazing" );
			System.out.println( "Existing data found. Loading" );	
		} else {
			dp.createFolders();
			System.out.println( "Server initilazing" );
			System.out.println( "No existing data found." );
		}
		
		
		// Constantly checks for messages and checks if an auction has ended
		boolean running = true;
		while( running ) {
			if( comms.checkMessage( Comms.SERVER_NAME ) ) {
				System.out.println( "Message found" );
				Message m = comms.receiveMessage( Comms.SERVER_NAME );
				readMessage( m );
			}
			checkTime();
		}
	}
	
	/**
	 * Create the GUI for the server report button and console.
	 */
	private static void createGUI() {		
		JFrame f = new JFrame( "Server controls" );
		
		f.setLayout( new BorderLayout() );
		
		JButton btnReport = new JButton( "Generate a server report" );
		f.add( btnReport, BorderLayout.SOUTH );
		
		/**
		 * Creates a window with a list of won auctions and their winner's name
		 */
		btnReport.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println( "Generating server report" );

				JFrame f = new JFrame( "Server Report" );		
				f.setLayout( new GridLayout( won.size()+1, 2 ) );
				
				f.add( new JLabel ( "Person" ) );
				f.add( new JLabel( "Item" ) );
				
				for( Item i : won ) {
					User u = userList.get( i.getLatestBid().getBidder() );
					f.add( new JLabel( u.getGivenName() + " " + u.getFamilyName() ) );
					f.add( new JLabel( i.getTitle() ) );
				}
				
				f.pack();
				f.setVisible(true);
			}
		});
		
		tpConsole = new JTextPane( );
		tpConsole.setPreferredSize( new Dimension( 500, 300 ) );
		JScrollPane sp = new JScrollPane( tpConsole );		
		f.add( sp, BorderLayout.CENTER );

		
		f.pack();
		f.setVisible( true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// Have to do after creation or there's nowhere to redirect the output to and we wait for ever!
		System.out.println( "Creating server GUI" );
	}
	
	/**
	 * Reads a message from the server and calls the relevant method, depending on the type
	 */
	private static void readMessage( Message m ) {
		switch( m.getTypeMessage() ) {
			case Message.LOG_IN_MESSAGE:
				validateLoginInformation( (LogInMessage) m );
				break;
			case Message.USER_REGISTRATION_MESSAGE:
				createUser( (UserRegistrationMessage) m );
				break;
			case Message.CREATE_AUCTION_MESSAGE:
				createAuction( (CreateAuctionMessage) m);
				break;
			case Message.ITEM_LIST_MESSAGE:
				getItemList( (ItemListMessage) m );
				break;
			case Message.ITEM_DISPLAY_MESSAGE:
				getItem( (ItemDisplayMessage) m);
				break;
			case Message.BID_MESSAGE:
				createBid( (BidMessage) m);
				break;
			case Message.MY_AUCTIONS_MESSAGE:
				getMyAuctions( (MyAuctionsMessage) m );
				break;
			case Message.MY_BIDS_MESSAGE:
				getMyBids( (MyBidsMessage) m );
				break;
			case Message.CANCEL_AUCTION_MESSAGE:
				withdrawnAuction( (CancelAuctionMessage) m );
				break;
		}
	}
	
	/**
	 * Validates a user trying to log into the system
	 */
	private static void validateLoginInformation( LogInMessage m ) {
		System.out.println( "Validating user login information" );
		
		String userName = m.getUserName().toLowerCase();
		Message m2;
		
		if( userList.containsKey( userName ) ) {
			User u = userList.get( userName );
			char[] password = m.getPassword();

			if( Arrays.equals( u.getPassword(), password ) ) {
				m2 = new LogInReturnMessage( LogInReturnMessage.SUCCESSFUL );
				System.out.println( "User succesfully logged in" ); 
			} else {
				m2 = new LogInReturnMessage( LogInReturnMessage.ERROR_PASSWORD );
				System.out.println( "User login failed" );
			}
		} else {
			m2 = new LogInReturnMessage( LogInReturnMessage.ERROR_USERNAME );	
			System.out.println( "User login failed" );
		}

		// Send results of the log in attempt to the client
		comms.sendMessage(m2, m.getClientID() );
		System.out.println( "Sending message to " + m.getClientID() );
	}
	
	/**
	 * Attempts to create a user
	 */
	private static void createUser( UserRegistrationMessage m ) {
		System.out.println( "Validating user registration details" );

		int type = validateUserRegistration( m );

		if( type == UserRegistrationReturnMessage.SUCCESS ) {
			String userName = m.getUserName();
			
			// Create user, add to server and save a backup
			User u = new User( userName, m.getFirstName(), m.getSurname(), m.getPassword() );
			addUser( u );
			comms.createUserInbox( userName );
			dp.saveUser( u );
			
			System.out.println( "Creating user account" );
		} else {
			System.out.println( "User account details not valid." );
		}

		String clientID = m.getClientID();
		Message m2 = new UserRegistrationReturnMessage( type );
		comms.sendMessage( m2, clientID );
		
		System.out.println( "Sending message to " + clientID );
	}

	/**
	 * Validates the information provided by the user and returns the relevant message type
	 */
	private static int validateUserRegistration( UserRegistrationMessage m ) {
		String firstName = m.getFirstName();
		String surname = m.getSurname();
		// Username is not case dependent as it can cause problems with folder names
		String userName = m.getUserName().toLowerCase();
		char[] password = m.getPassword();
		char[] passwordConfirm = m.getPasswordConfirm();
		
		// Basic not empty checks
		if( userName.isEmpty() ) 
			return UserRegistrationReturnMessage.USER_NAME_MISSING;
		
		if( password.length == 0 )
			return UserRegistrationReturnMessage.PASSWORD_MISSING;
			
		if( passwordConfirm.length == 0 )
			return UserRegistrationReturnMessage.PASSWORD_CONFIRM_MISSING;
			
		if( firstName.isEmpty() )
			return UserRegistrationReturnMessage.FIRST_NAME_MISSING;
			
		if( surname.isEmpty() )
			return UserRegistrationReturnMessage.SURNAME_MISSING;
		
		// Checks the username is not already in use
		if( userList.containsKey( userName ) )
			return UserRegistrationReturnMessage.USER_NAME_TAKEN;
			
		// Check the passwords provided are equal
		if( !Arrays.equals( password, passwordConfirm ) )
			return UserRegistrationReturnMessage.PASSWORDS_DIFFERENT;
			
		return UserRegistrationReturnMessage.SUCCESS;
	}
	
	/**
	 * Attempts to create an auction
	 */
	private static void createAuction( CreateAuctionMessage m ) {
		// Validate the auction
		int value = validateAuction( m );
		Item i = null;
		
		// If successful, create the auction
		if( value == CreateAuctionReturnMessage.SUCCESS ) {
				
			// Very primitive way of working out what the item ID should be 
			// Would do something more random but needs to be user-friendly
			int itemID = 0;
			while( itemList.containsKey( Integer.toString( itemID ) ) )
				itemID++;
			
			// Convert price from String into a two d.p. float
			String reservePrice = m.getReservePrice();
			
			if( reservePrice.startsWith( "£" ) )
				reservePrice.replace("£", "");
			
			float price = Float.parseFloat(reservePrice);
			price = Math.round(price*100.0f)/100.0f;
			
			// Create item and add to the relevant lists
			i = new Item( m.getTitle(), m.getDescription(), m.getCategory(), m.getVendorID(), m.getEndTime(), price, Integer.toString( itemID), m.getImage() );
			itemList.put(Integer.toString( itemID ), i);
			activeAuctions.add(i);
			dp.saveItem( i );
			
			System.out.println( "Creating a new auction for item \"" + i.getTitle() + "\"" );
		} else {
			System.out.println( "Auction creation failed" );
		}

		// Send return message to the client
		Message m2 = new CreateAuctionReturnMessage( value, i );
		String clientID = m.getClientID();
		comms.sendMessage( m2, clientID );
		
		System.out.println( "Sending message to " + clientID );
	}
	
	/**
	 * Validates the information the user provided about an auction
	 */
	private static int validateAuction( CreateAuctionMessage m ) {
		System.out.println( "Validating auction details" );

		String title = m.getTitle();
		String description = m.getDescription();
		String category = m.getCategory();
		Date endTime = m.getEndTime();
		String reservePrice = m.getReservePrice();

		User vendor = userList.get( m.getVendorID() );
		
		// If they have more than 2 penalty points they can't list auctions anymore
		if( !vendor.canSubmit() ) 
			return CreateAuctionReturnMessage.PENALTY_POINTS;
		
		// Basic empty checks
		if( title.isEmpty() ) 
			return CreateAuctionReturnMessage.MISSING_TITLE;
		if( description.isEmpty() )
			return CreateAuctionReturnMessage.MISSING_DESCRIPTION;
		if( category == null )
			return CreateAuctionReturnMessage.MISSING_CATEGORY;
		if( endTime == null )
			return CreateAuctionReturnMessage.MISSING_END_TIME;
		if( reservePrice.isEmpty() )
			return CreateAuctionReturnMessage.MISSING_RESERVE_PRICE;
		
		// Price checks
		float price;
		try {
			price = Float.parseFloat( reservePrice );
		} catch ( Exception e ) {
			return CreateAuctionReturnMessage.INVALID_RESERVE_PRICE;
		}
		
		if( price < 0 )
			return CreateAuctionReturnMessage.RESERVE_PRICE_NEGATIVE;
		
		// Date check
		if( endTime.before( new Date() ) )
			return CreateAuctionReturnMessage.END_TIME_INVALID;
		
		return CreateAuctionReturnMessage.SUCCESS;
	}

	/**
	 * Gets an item from the database and returns it to the user
	 */
	private static void getItem(ItemDisplayMessage m) {
		String code = m.getItemCode();

		System.out.println( "Searching for item with id " + code );

		Message m2;
		if( itemList.containsKey( code ) ) {
			m2 = new ItemDisplayReturnMessage( itemList.get(code) );
			System.out.println( "Item with id " + code + " found" );
		} else {
			m2 = new ItemDisplayReturnMessage(null);
			System.out.println( "Item with id " + code + " not found");
		}
		
		String clientID = m.getClientID();		
		comms.sendMessage( m2, clientID );
		System.out.println( "Message sent to " + clientID );
	}

	/**
	 * Returns a list of items in the auction system, filtered by user input, if provided
	 */
	private static void getItemList( ItemListMessage m ) {
		System.out.println( "Accessing item list" );
		
		// Will filter down later
		List<Item> items = new ArrayList<Item>( itemList.values() );
		
		String category = m.getCategory();
		String user = m.getUser();
		Date date = m.getDate();
		String description = m.getDescription();
		boolean ended = m.getEnded();
		
		/*
		 * Each of the following chunks operates in the following way:
		 * 1) If the search is null skip
		 * 2) Otherwise, create a new item list
		 * 3) For each of the items in the current list, check if it meets search requirements
		 * 4) If so add to the new list
		 * 5) Make the items list =  the new list
		 */
		if( category != null ) {
			System.out.println( "Filtering item list by selected category" );
			List<Item> items2 = new ArrayList<Item>( );
			for( Item i : items ) {
				if( category.equals( i.getCategory()) )
					items2.add(i);
			}
			items = items2;
		}

		if( user != null ) {
			System.out.println( "Filtering item list by selected user" );
			List<Item> items2 = new ArrayList<Item>();
			for( Item i : items ) {
				if( user.equals( i.getVendorID() ) )
					items2.add(i);
			}
			items = items2;
		}
		
		if( date != null ) {
			System.out.println( "Filtering item list by selected start date" );
			List<Item> items2 = new ArrayList<Item>();
			for( Item i : items ) {
				if( date.before( i.getStartTime() ) )
					items2.add(i);
			}
			items = items2;
		}
		
		if( description != null ) {
			System.out.println( "Filtering item list by searching the description" );
			List<Item> items2 = new ArrayList<Item>();
			for( Item i : items ) {
				if( i.getDescription().toLowerCase().contains( description ) )
					items2.add(i);
			}
			items = items2;
		}
		
		if( ended == false ) {
			System.out.println( "Filtering inactive auctions out of the item list" );
			List<Item> items2 = new ArrayList<Item>();
			for( Item i : items ) {
				if( i.isActive() )
					items2.add(i);
			}
			items = items2;
		}
		
	
		Message m2 = new ItemListReturnMessage( items );
		
		String clientID = m.getClientID();
		comms.sendMessage(m2, clientID );
		System.out.println( "Sending message to " + clientID );
	}

	/**
	 * Returns a list of bids made by the user
	 */
	private static void getMyBids( MyBidsMessage m ) {
		String clientID = m.getClientID();
		String userID = m.getUserID();
		
		System.out.println( "Obtaining list of user " + userID + "'s bids" );

		// Make a map of bids and their related auctions
		Map<Bid,Item> auctions = new HashMap<Bid, Item>();
		
		// Go through each item
		for( Item i : itemList.values() ) {
			// Go through each bid on the item
			List<Bid> bids = i.getBids();
			for( Bid b : bids ) {
				// If the user made the bid, save it to the list
				if( b.getBidder().equals( userID ))
					auctions.put( b, i );
			}
		}
		
		// Return results to the client
		Message m2 = new MyBidsReturnMessage( auctions );
		comms.sendMessage( m2, clientID );
		System.out.println( "Sending message to " + clientID );
	}

	/**
	 * Returns a list of auctions created by the user
	 */
	private static void getMyAuctions(MyAuctionsMessage m) {
		String clientID = m.getClientID();
		String userID = m.getUserID();
		
		System.out.println( "Obtaining list of user " + userID + "'s auctions" );
		
		List<Item> auctions = new ArrayList<Item>();

		// Check the vendorID of each auction against the userID
		for( Item i : itemList.values() ) {
			if( i.getVendorID().equals( userID ) ) {
				auctions.add( i );
			}
		}
		
		// Return results to the client
		Message m2 = new MyAuctionsReturnMessage( auctions );
		comms.sendMessage( m2, clientID );
		System.out.println( "Sending message to " + clientID );
	}

	/**
	 * Creates a bid
	 */
	private static void createBid( BidMessage m ) {
		System.out.println( "Validating bid" );

		// Validate the bid details
		int type = validateBid( m );
		
		// We have to update the Server version of the item with the bid, not the client's version
		Item i = itemList.get( m.getItem().getItemID() );
		if( type == BidReturnMessage.SUCCESS ) {
			
			float price = Float.parseFloat( m.getBid() );
			// Get price to 2 decimal places
			price = Math.round(price*100.0f)/100.0f;
			
			String userID = m.getUserID();
			
			// Need to save this so we can use it to notify the previous seller
			Bid lastBid = i.getLatestBid();

			// Make a bid on the item
			i.bid( price, userID );
			
			// Have to remove and add the item as 
			//removeItem(i);
			//addItem(i);
			
			dp.saveItem(i);

			// Send notification to the seller
			Notification n = new Notification( i, Notification.SELLING_BID );
			comms.sendNotification( n, i.getVendorID() );
			System.out.println( "Sending notification to " + i.getVendorID() );
			
			// Send notification to the person who's been outbidded
			if( lastBid.getBidder() != null ) {
				n = new Notification( i, Notification.BIDDING_OUT_BID );
				comms.sendNotification( n, lastBid.getBidder() );
				System.out.println( "Sending notification to " + lastBid.getBidder() );
			} 
		}
		
		// Return message to user
		Message m2 = new BidReturnMessage( type, i );
		String clientID = m.getClientID();
		comms.sendMessage( m2, clientID );
		System.out.println( "Sending message to " + clientID );
	} 
	
	/**
	 * Validates the bid a user has attempted to place
	 */
	private static int validateBid( BidMessage m ) {
		Item i = m.getItem();
		String userID = m.getUserID();
		String bid = m.getBid();
		
		// Can't bid on your own auction
		if( userID.equals( i.getVendorID() ) )
			return BidReturnMessage.SELLER_BIDDER;
			
		if( bid.isEmpty() )
			return BidReturnMessage.BID_MISSING;
			
		// Convert string to float
		float price;
		try{
			price = Float.parseFloat( bid );
			
			if( price <= 0 )
				return BidReturnMessage.NEGATIVE_BID;
			
			Bid lastBid = i.getLatestBid();
			if( price <= lastBid.getBidValue() )
				return BidReturnMessage.LOW_BID;
				
		} catch ( Exception e ) {
			return BidReturnMessage.INVALID_BID;
		}
		
		return BidReturnMessage.SUCCESS;
	}


	/**
	 * Checks if there are any auctions that are due to close
	 */
	private static void checkTime() {
		// Only check if there are any auctions which are running
		if( activeAuctions.size() > 0 ) {
			Item i = activeAuctions.peek();
			Date now = new Date();
			Date end = i.getEndTime();

			// Compare first auction's end time with the current time
			// End if the auction's time has passed
			if( end.before( now ) || end.equals( now ) ) {
				endAuction( i );
				activeAuctions.remove();
			}
		}
	}

	/**
	 * Ends an auction by working out if it was success or not and notify the necessary parties
	 */
	private static void endAuction(Item i) {
		System.out.println( "Auction \"" + i.getTitle() + "\" has ended");

		Bid highestBid = i.getLatestBid();
		// Second condition removes a rare bug where reserve price is zero and no one has bid
		if( highestBid.getBidValue() >= i.getReservePrice() && highestBid.getBidder() != null ) {
			System.out.println( "Auction succeded" );
			
			won.add( i );
			
			// Send a notification to the seller
			Notification n = new Notification( i, Notification.SELLING_END_SUCCESS );
			comms.sendNotification( n, i.getVendorID() );
			System.out.println( "Sending notification to " + i.getVendorID() );
			
			// Send a notification to the winning bidder
			n = new Notification( i, Notification.BIDDING_WIN );
			comms.sendNotification( n, i.getLatestBid().getBidder() );
			System.out.println( "Sending notification to " + i.getLatestBid().getBidder() );
		} else {
			System.out.println( "Auction failed" );
			
			// Send a notification to the seller
			Notification n = new Notification( i, Notification.SELLING_END_FAIL );
			comms.sendNotification( n, i.getVendorID() );
			System.out.println( "Sending notification to " + i.getVendorID() );
			
			// If there was a bid below the reserve price, notify the bidder
			if( highestBid.getBidder() != null ) {
				n = new Notification( i, Notification.BIDDING_LOSE );
				comms.sendNotification( n, i.getLatestBid().getBidder() );
				System.out.println( "Sending notification to " + i.getLatestBid().getBidder() );
			}
		}
		
		// End the auction
		i.endAuction();
		
		// Update the save file
		dp.saveItem( i );
	}
	
	/**
	 * Withdrawing an auction from sale
	 */
	private static void withdrawnAuction(CancelAuctionMessage m) {
		String clientID = m.getClientID();
		Item i = itemList.get( m.getItemID() );
		User u = userList.get( i.getVendorID() );
		
		i.endAuction();
		activeAuctions.remove( i );

		System.out.println( "Auction \"" + i.getTitle() + "\" withdrawn.");
		
		// Notify seller
		Notification n = new Notification( i, Notification.WITHDRAWN_SELLER );
		comms.sendNotification( n, i.getVendorID() );
		System.out.println( "Sending notification to " + i.getVendorID() );
		
		// If there's a valid bid, notify bidder and give penalty point
		if( i.getLatestBid().getBidValue() >= i.getReservePrice() ) {
			u.addPenaltyPoint();
			
			n = new Notification( i, Notification.WITHDRAWN_BIDDER );
			comms.sendNotification( n, i.getLatestBid().getBidder() );
			System.out.println( "Sending notification to " + i.getLatestBid().getBidder() );
		}
		
		// Return message to the client
		Message m2 = new CancelAuctionReturnMessage( u.getPenaltyPoints() );
		comms.sendMessage( m2, clientID );
		System.out.println( "Sending message to " + clientID );	
	}

	/**
	 * Adds an item to the server
	 */
	public static void addItem( Item i ) {
		// Add to the complete list
		itemList.put( i.getItemID(), i);
		
		// If active, add to the active list
		if( i.isActive() )
			activeAuctions.add(i);
		// If won, add to the won list so we can add it to the server report
		else if( i.getLatestBid().getBidValue() >= i.getReservePrice() )
			won.add( i );
	}
	
	/**
	 * Adds a user to the server
	 */
	public static void addUser( User u ) {
		userList.put( u.getUserID(), u );
	}
	
	/**
	 * Adds a line to the console JTextPane
	 */
	private static void updateConsole( final String text ) {
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				Document doc = tpConsole.getDocument();
				try {
					doc.insertString( doc.getLength(), text, null );
				} catch( BadLocationException e ) {}
				tpConsole.setCaretPosition( doc.getLength() -1);
			}
		});
		
		dp.saveLog( tpConsole.getDocument() );
	}
	
	/**
	 * Redirects System.out and System.err to taConsole
	 */
	private static void redirectSystemStreams() {
		OutputStream out = new OutputStream() {
			@Override
		      public void write( final int b ) throws IOException {
		         updateConsole( String.valueOf( (char) b ) );
		       }

			@Override
		    public void write(byte[] b, int off, int len) throws IOException {
		        updateConsole(new String(b, off, len));
		    }
		};
		
		System.setOut( new PrintStream( out, true) );
		System.setErr( new PrintStream( out, true) );
	}
	
	/**
	 * Loads a saved activity log into the console
	 */
	public static void loadLog( Document d ) {
		tpConsole.setDocument(d);
	}
}