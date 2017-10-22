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
import javax.swing.text.Document;

/**
 * The data persistence class keeps a back-up of all the data stored on the server
 * This is used in case the server crashes or has to be restarted for whatever reason
 * 
 * @author Richard Cook
 * @version 1.0
 * @date May 15, 2014
 */
public class DataPersistence {
	// Need to have consistency in folder names
	public static final String USER_FOLDER = "Users";
	public static final String ITEM_FOLDER = "Items";
	public static final String USER_EXTENSION = ".usr";
	public static final String ITEM_EXTENSION = ".itm";
	public static final String KEY_EXTENSION = ".key";
	public static final String ACTIVITY_LOG = "activity.log";
	
	// Might as well keep track to save making new File objects every save
	private File users; 
	private File items;
	
	/**
	 * Set up the file objects
	 */
	public DataPersistence( ) {
		users = new File( USER_FOLDER );
		items = new File( ITEM_FOLDER );
	}

	/**
	 * Checks if save data exists (well, more accurately the folders)
	 */
	public boolean saveDataExists() {
		if( users.exists() && items.exists() )
			return true;
		return false;
	}
	
	/**
	 * Saves a user
	 */
	public void saveUser( User u ) {
		File f = new File( USER_FOLDER + "/" + u.getUserID() + USER_EXTENSION );
		saveObject( f, u );
	}

	/**
	 * Saves an item - bids are a part of item and saved through there
	 */
	public void saveItem( Item i ) {
		File f = new File( ITEM_FOLDER + "/" + i.getItemID() + ITEM_EXTENSION );
		saveObject( f, i );
	}

	/**
	 * Save a copy of the activity log sent to the console.
	 */
	public void saveLog( Document document ) {
		File f = new File( ACTIVITY_LOG );
		saveObject( f, document );
	}
	
	/**
	 * Saves an object to a file in its folder with encryption
	 * For details on what all the encryption stuff does see the sendObject() method in Comms.java
	 */
	public void saveObject( File f, Object o ) {
		try {
			KeyGenerator kg = KeyGenerator.getInstance("DES");
			kg.init( new SecureRandom() );
			
			SecretKey key = kg.generateKey();
			SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
			Class<?> keySpec = Class.forName("javax.crypto.spec.DESKeySpec");
			DESKeySpec ks = (DESKeySpec) skf.getKeySpec(key, keySpec);
			
			FileOutputStream fos = new FileOutputStream( f.getAbsolutePath() + KEY_EXTENSION );
			ObjectOutputStream oos = new ObjectOutputStream( fos );
			
			oos.writeObject( ks.getKey() );
			
			Cipher c = Cipher.getInstance("DES/CFB8/NoPadding");
			c.init(Cipher.ENCRYPT_MODE, key);
			FileOutputStream fos2 = new FileOutputStream( f );
			CipherOutputStream cos = new CipherOutputStream( fos2, c );
			
			ObjectOutputStream oos2 = new ObjectOutputStream( cos );
			
			oos2.writeObject( o ); // The bit of the code that saves the object
			
			oos2.flush();
			oos2.close();
			oos.writeObject(c.getIV());
			oos.close();
			
		} catch ( Exception e ) {
			System.err.println( "Whoops! Error saving object." );
		}
	}


	/**
	 * Reads the data from the file - simply calls the two methods
	 */
	public void readData() {
		readUsers();
		readItems();
		readLog();
	}
	
	/**
	 * Reads a user and adds it to the server
	 */
	public void readUsers() {
		for( String s : users.list() ) {
			if( s.toLowerCase().endsWith( USER_EXTENSION ) ) {
				File f = new File( USER_FOLDER + "/" + s );
				User u = (User) readObject(f);
				// DataPersistance is called from Server, so should be no issues here
				Server.addUser( u ); 
			}
		}
	}
	
	/**
	 * Reads an item and adds it to the server
	 */
	public void readItems() {
		for( String s : items.list() ) {
			if( s.toLowerCase().endsWith( ITEM_EXTENSION ) ) {
				File f = new File( ITEM_FOLDER + "/" + s );
				Item i = (Item) readObject(f);
				// DataPersistance is called from Server, so should be no issues here
				Server.addItem( i );
			}
		}
	}
	
	/**
	 * Reads back the activity log to the console.
	 */
	public void readLog() {
		File f = new File( ACTIVITY_LOG );
		Document d = (Document) readObject(f);
		
		Server.loadLog( d );
	}
	
	/**
	 * Reads an object from a file
	 * For comments on what all the encryption stuff does see receiveObject() in Comms.java
	 */
	public Object readObject( File f ) {
		boolean opened = false;
		Object o = null;
		
		// Repeat until it opens as sometimes files are in use and can't be read for a few seconds
		while( !opened ) {
			try {
				ObjectInputStream ois = new ObjectInputStream( new FileInputStream( f.getAbsoluteFile() + ".key" ) );
				DESKeySpec ks = new DESKeySpec((byte[]) ois.readObject());
				SecretKeyFactory skf  = SecretKeyFactory.getInstance("DES");
				SecretKey key = skf.generateSecret(ks);
				
				Cipher c = Cipher.getInstance("DES/CFB8/NoPadding");
				c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec( (byte[]) ois.readObject() ));
				CipherInputStream cis = new CipherInputStream( new FileInputStream( f), c);
				ObjectInputStream ois2 = new ObjectInputStream( cis );
				o = ois2.readObject();
				ois2.close();
				ois.close();
				opened = true;
			} catch ( Exception e) {}
		}
		return o;
	}
		
	/**
	 * Creates the folders needed to save the data in
	 */
	public void createFolders() {
		users.mkdir();
		items.mkdir();
	}
}