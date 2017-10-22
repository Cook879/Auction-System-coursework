import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * Class which handles communication between the server and the clients.
 * It works through the file system
 * 
 * @author Richard Cook
 * @version 1.0
 * @date May 15, 2015
 */
public class Comms {

	// To have consistent file access
	public final static String INBOX_NAME = "Messages";
	public final static String INBOX_PATH = INBOX_NAME + "/";
	public final static String SERVER_NAME = "Server";
	public final static String SERVER_PATH = INBOX_NAME + "/" + SERVER_NAME;
	public final static String NOTIFICATION_NAME = "Notifications";
	public final static String NOTIFICATION_PATH = NOTIFICATION_NAME + "/";
	
	public final static String MESSAGE_EXTENSION = ".msg";
	public final static String NOTIFICATION_EXTENSION = ".not";
	public final static String KEY_EXTENSION = ".key";
	
	/**
	 * Constructor checks if the necessary folders already exist
	 */
	public Comms() {
		File inboxFolder = new File( INBOX_NAME );
		
		// Creates new folders is the inbox does not already exist
		if( !inboxFolder.exists() ) {
			inboxFolder.mkdir();
		
			File server = new File( SERVER_PATH );
			server.mkdir();
			
			File notifications = new File( NOTIFICATION_NAME );
			notifications.mkdir();
		}
	}
	
	/**
	 * Sends a message to the specified client/ the server
	 */
	public void sendMessage( Message m, String folder ) {
		String path = INBOX_PATH + folder + "/";
		
		sendObject( m, path, m.getMessageID(), MESSAGE_EXTENSION );
	}
	
	/**
	 * 
	 */
	public void sendNotification( Notification n, String folder ) {
		String path = NOTIFICATION_PATH + folder + "/";
		sendObject( n, path, n.getNotificationID(), NOTIFICATION_EXTENSION );
	}
	
	/**
	 * Handles the actual sending for sendMessage() and sendNotification()
	 */
	public void sendObject( Object o, String path, String ID, String extension  ) {
		try {
			/*
			 * Encryption stuff
			 * First, create a key generator using the Data Encryption Standard
			 * Initialise it with a cryptographically strong random number
			 * Use this to create a new key
			 * Create a DESKeySpec for the key
			 * Use this to save a key into a special ".msg.key" file
			 * Create a Cipher based on the key
			 * Use this in a CipherOutputStream to output the message securely
			 * Once written, get the cipher's Initialization Vector and save it to the key file
			 */
			KeyGenerator kg = KeyGenerator.getInstance("DES");
			kg.init( new SecureRandom() );
			
			SecretKey key = kg.generateKey();
			SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
			Class<?> keySpec = Class.forName("javax.crypto.spec.DESKeySpec");
			DESKeySpec ks = (DESKeySpec) skf.getKeySpec(key, keySpec);
			
			FileOutputStream fos = new FileOutputStream( path + ID + extension + KEY_EXTENSION );
			ObjectOutputStream oos = new ObjectOutputStream( fos );
			oos.writeObject( ks.getKey() );
			
			Cipher c = Cipher.getInstance("DES/CFB8/NoPadding");
			c.init(Cipher.ENCRYPT_MODE, key);
			
			/*
			 * Create a temporary file for the message
			 * Done because otherwise the system tries to read the message while it's writing
			 * And this causes errors  
			 */
			File f = new File( path + ID + ".tmp" );
			FileOutputStream fos2 = new FileOutputStream( f );
			CipherOutputStream cos = new CipherOutputStream( fos2, c );
			ObjectOutputStream oos2 = new ObjectOutputStream( cos );
			
			oos2.writeObject( o );
			
			oos2.flush();
			oos2.close();
						
			oos.writeObject(c.getIV());
			oos.close();
		
			// Now it's written move to the designated format
			File f2 = new File( path + ID + extension );
			f.renameTo( f2 );
		} catch ( Exception e ) {
			System.err.println( "Whoops! Error sending object." );
		}
	}
	
	/**
	 * Checks if the specified client / the server has any messages waiting for them
	 */
	public boolean checkMessage( String folder ) {
		// Path of the specified folder
		String path = INBOX_PATH + folder + "/";
		
		return checkFolder( path, MESSAGE_EXTENSION );	
	}

	/**
	 * Checks for notifications for the specified user
	 */
	public boolean checkNotification( String userID ) {
		String path = NOTIFICATION_PATH + userID + "/";

		return checkFolder( path, NOTIFICATION_EXTENSION );
	}
	
	/**
	 * Does the checking for checkMessage and checkNotification
	 */
	public boolean checkFolder( String path, String extension ) {
		File inbox = new File( path );
		
		// Check each file in the folder - if one has the correct extension return true
		if( inbox.list().length > 0 ) {
			for( String s : inbox.list() ) {
				if( s.toLowerCase().endsWith( extension ) )
					return true;
			}
			return false;
		} else
			return false;	
	}
	
	/**
	 * Receives a message from the client's / server's inbox folder
	 * Should only be called if checkMessage() has returned true
	 */
	public Message receiveMessage( String folder ) {
		String path = INBOX_PATH + folder + "/";

		return (Message) receiveObject( path, MESSAGE_EXTENSION );
	}
	
	/**
	 * Receives a notification from a user's inbox
	 * Should only be called if checkNotification() has return true
	 */
	public Notification receiveNotification( String folder ) {
		String path = NOTIFICATION_PATH + folder + "/";
		
		return (Notification) receiveObject( path, NOTIFICATION_EXTENSION );
	}
	
	/**
	 * Code that de-crypts and reads the object file from the server
	 */
	public Object receiveObject( String path, String extension ) {
		File inbox = new File( path );
		String[] files = inbox.list();
		
		Object o = null;
		boolean msgFound = false;
		int i = 0;

		/*
		 *  Check through the files in the folder until a message file is found 
		 * or there are none left - though this shouldn't occur as checkMessage should be called first
		 */
		while( !msgFound && i < files.length ) {
			String s = files[i];
			if( s.toLowerCase().endsWith( extension ) )
				msgFound = true;
			else
				i++;
		}
		
		/*
		 * Once found, we need to read the file
		 * In a while loop as the file is sometimes busy
		 * and we can't open it until the process is done 
		 * (probably converting it from .tmp to .msg)
		 */
		if( msgFound ) {
			boolean opened = false;
			while( !opened ) {
				File f = new File( inbox + "/" + files[i] );

				try {
					/*
					 * Encryption stuff
					 * Open the key file and read the key into a DESKeySpec
					 * Use this and a SecretKeyFactory to generate the key
					 * Read the Initialization Vector from the key file
					 * Use these to generate the correct Cipher
					 * Read the file through the CipherInputStream to de-cypher the message
					 */
					File f2 = new File( f.getAbsoluteFile() + KEY_EXTENSION );
					FileInputStream fis2 = new FileInputStream( f2 );
					ObjectInputStream ois = new ObjectInputStream( fis2 );
					DESKeySpec ks = new DESKeySpec( (byte[]) ois.readObject() );
					
					SecretKeyFactory skf  = SecretKeyFactory.getInstance( "DES" );
					SecretKey key = skf.generateSecret( ks );
					
					Cipher c = Cipher.getInstance("DES/CFB8/NoPadding");
					c.init( Cipher.DECRYPT_MODE, key, new IvParameterSpec( (byte[]) ois.readObject() ) );
					
					FileInputStream fis = new FileInputStream( f );
					CipherInputStream cis = new CipherInputStream( fis, c);
					ObjectInputStream ois2 = new ObjectInputStream( cis );
					
					o = ois2.readObject();
					
					ois2.close();
					ois.close();
					
					f.delete(); // Delete so it isn't received again
					f2.delete(); // No need for the key now the message is gone
					opened = true; // Complete without exceptions! \o/
				} catch (Exception e) { }			
			}
			return o;
		} else {
			System.err.println( "Whoops! Error recieving object." );
			return o;
		}
	}	
	
	/**
	 * Creates an inbox folder for the specified client
	 */
	public void createClientInbox( String c ) {
		File inbox = new File( INBOX_PATH + c );
		inbox.mkdir();
	}

	/**
	 * Creates an notification-inbox folder for the specified user
	 */
	public void createUserInbox( String u ) {
		File inbox = new File( NOTIFICATION_PATH + u );
		inbox.mkdir();
	}

	/**
	 * Called by the client to check for return messages from server
	 * Combines checkMessage() and receieveMessage() and does a little validation for good measure
	 */
	public Message checkForReturn( String clientID, int type ) {
		boolean reply = false;
		Message m = null;
		while( !reply ) {
			if( checkMessage(clientID ) ) {
				m = receiveMessage( clientID );
				if( m.getTypeMessage() == type ) {
					reply = true;
				} else {
					System.err.println( "Wrongly distributed message" );
					/* 
					 * Untested as haven't had a wrongly distributed message yet
					 * and in theory never will as the client only sends one message at a time
					 * and doesn't do anything else until it gets a return
					 */
					sendMessage( m, clientID );
				}
			}
		}
		return m;
	}
}