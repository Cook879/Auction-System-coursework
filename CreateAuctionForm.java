import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

/**
 * Creates and displays the create auction form to the user
 * 
 * @author Richard Cook
 * @version 1.0
 * @date May 15, 2014
 */
class CreateAuctionForm extends JPanel implements CustomPanel {
	
	private static final long serialVersionUID = 1L;

	private GUI f;
	// Keep track of image here as it's optional
	private ImageIcon image = null;
	private JLabel lblFileName; // File name of image
	
	public CreateAuctionForm( GUI f ) {
		this.f = f;
	}
	
	public void init() {
		GridBagLayout gbl = new GridBagLayout();
		this.setLayout( gbl );
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.gridwidth = 3;
		JLabel lblTitle = new JLabel( "Create an auction" );
		gbl.setConstraints( lblTitle, gbc );
		this.add( lblTitle );
		
		gbc.gridwidth = 1;
		gbc.gridy = 1;
		JLabel lblTitleField = new JLabel( "Title: " );
		gbl.setConstraints( lblTitleField, gbc );
		this.add( lblTitleField );
		
		gbc.gridx = 1;
		gbc.gridwidth = 2;
		final JTextField tfTitle = new JTextField( 25 );
		gbl.setConstraints( tfTitle, gbc );
		this.add( tfTitle );
		
		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.gridy = 2;
		JLabel lblDescription = new JLabel( "Description: " );
		gbl.setConstraints( lblDescription, gbc );
		this.add( lblDescription );
		
		gbc.gridx = 1;
		gbc.gridwidth = 2;
		
		final JTextArea taDescription = new JTextArea( 10, 30  );
		taDescription.setWrapStyleWord(true); 
		taDescription.setLineWrap(true);
		
		// Put in a scroll pane to stop resizing when we exceed the height
		JScrollPane spDescription = new JScrollPane( taDescription );
	
		gbl.setConstraints( spDescription, gbc );
		this.add( spDescription );
		
		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.gridy = 3;
		JLabel lblCategory = new JLabel( "Select a category: " );
		gbl.setConstraints( lblCategory, gbc );
		this.add( lblCategory );
		
		gbc.gridx = 1;
		gbc.gridwidth = 2;
		
		String[] categories = new String[Item.KEYWORD_CATEGORIES.length + 1];
		categories[0] = null; // Put an empty category at the top to make sure they choose one
		for( int i = 1; i <= Item.KEYWORD_CATEGORIES.length; i++ )
			categories[i] = Item.KEYWORD_CATEGORIES[i-1];

		final JComboBox<String> cbCategories = new JComboBox<String>( categories );
		gbl.setConstraints( cbCategories, gbc );
		this.add( cbCategories );
		
		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.gridy = 4;
		JLabel lblEndTime = new JLabel( "End time (DD/MM/YYYY HH:MM): " );
		gbl.setConstraints( lblEndTime, gbc );
		this.add( lblEndTime );
		
		gbc.gridwidth = 2;
		gbc.gridx = 1;
		final DatePanel pnlDate = new DatePanel();
		gbl.setConstraints( pnlDate, gbc );
		this.add( pnlDate );
		
		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.gridy = 5;
		JLabel lblReservePrice = new JLabel( "Reserve price: £" );
		gbl.setConstraints( lblReservePrice, gbc );
		this.add( lblReservePrice );
		
		gbc.gridwidth = 2;
		gbc.gridx = 1;
		final JTextField tfReservePrice = new JTextField( 5 );
		gbl.setConstraints( tfReservePrice, gbc );
		this.add( tfReservePrice );
		
		// Not shown unless needed, but add so it can show without hassle
		gbc.gridx = 0;
		gbc.gridy = 6;
		gbc.gridwidth = 3;
		final JLabel lblError = new JLabel( );
		gbl.setConstraints( lblError, gbc );
		this.add( lblError );
		lblError.setForeground( Color.red );

		gbc.gridy = 7;
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		JLabel lblImage = new JLabel( "Choose an image (optional): " );
		gbl.setConstraints( lblImage, gbc);
		this.add( lblImage );
		
		gbc.gridx = 1;
		JButton btnImage = new JButton( "Select" );
		gbl.setConstraints( btnImage, gbc);
		this.add( btnImage );
		
		gbc.gridx = 2;
		lblFileName = new JLabel();
		gbl.setConstraints( lblFileName, gbc);
		this.add( lblFileName );
		
		/**
		 * Opens up a file chooser
		 * If a file is selected, calls setFile();
		 */
		btnImage.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser jfc = new JFileChooser();
				jfc.setFileFilter( new ImageFileFilter() );
				int result = jfc.showOpenDialog(jfc);
				if( result == JFileChooser.APPROVE_OPTION ){
					setImage( jfc.getSelectedFile());
				}
			}
		});
		
		gbc.gridx = 0;
		gbc.gridy = 8;
		gbc.gridwidth = 3;
		JButton btnSubmit = new JButton( "Create my auction!" );
		gbl.setConstraints( btnSubmit, gbc );
		this.add( btnSubmit );
		
		/**
		 * Communicates with the server to create the auction
		 */
		btnSubmit.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// Get details from the form
				String clientID = f.getClientID();
				String title = tfTitle.getText();
				String description = taDescription.getText();
				String category = (String) cbCategories.getSelectedItem();
				
				Date endTime;
				
				String day = pnlDate.tfDay.getText();
				String month = pnlDate.tfMonth.getText();
				String year = pnlDate.tfYear.getText();
				String hour = pnlDate.tfHour.getText();
				String minute = pnlDate.tfMinute.getText();
					
				// Can't parse a date if we have no data
				if( ! (day.isEmpty() && month.isEmpty() && year.isEmpty() && hour.isEmpty() && minute.isEmpty() ) ) {
					// Let's make sure every field has a value, 'cos otherwise we have a problem
					if( ! (day.isEmpty() || month.isEmpty() || year.isEmpty() || hour.isEmpty() || minute.isEmpty() ) ) {
								
						SimpleDateFormat sdf = new SimpleDateFormat( "d/M/yyyy HH:mm" );
						String dateStr = day + "/" + month + "/" + year + " " + hour + ":" + minute;
								
						// Parse the string into a date value
						// If the date values aren't legit - like a minute of 73 - it simply adds it on top, so 73 minutes = 1hr 13 mins
						try {
							endTime = sdf.parse(dateStr);
						} catch (ParseException e) {
							lblError.setText( "Error with your end date values" );
							f.pack();
							return; // 'cos we can't do anything good with a broken date
						}
					} else {
						lblError.setText( "Incomplete end date" );
						f.pack();
						return; // 'cos we can't do anything good with a broken date
					} 
				} else {
					lblError.setText( "End date is required" );
					f.pack();
					return; // 'cos we can't do anything good with a missing date
				}
				
				String reservePrice = tfReservePrice.getText();
				if( reservePrice.startsWith( "£" ) ) // In the very unlikely chance someone adds a £ in front of their price
					reservePrice = reservePrice.replace("£", "");
				
				String vendorID = f.getUserID();
				
				// Create message and send to server
				Message m = new CreateAuctionMessage( clientID, title, description, category, vendorID, endTime, reservePrice, image );
				Comms c = f.getComms();
				c.sendMessage( m, Comms.SERVER_NAME );
				
				CreateAuctionReturnMessage m2 = (CreateAuctionReturnMessage) c.checkForReturn( clientID, Message.CREATE_AUCTION_RETURN_MESSAGE );

				// Check through the various options
				switch( m2.getType() ) {
					case CreateAuctionReturnMessage.SUCCESS: 
						// Display a message window then take the user to the created auction
						JOptionPane.showMessageDialog(f, "Auction created!", "Auction created", JOptionPane.INFORMATION_MESSAGE);
						f.displayItem( m2.getItem() );
						break;
					case CreateAuctionReturnMessage.INVALID_RESERVE_PRICE:
						lblError.setText( "Reserve price must be a monetary value" );
						break;
					case CreateAuctionReturnMessage.MISSING_CATEGORY:
						lblError.setText( "Category is required" );
						break;
					case CreateAuctionReturnMessage.MISSING_DESCRIPTION:
						lblError.setText( "Description is required" );
						break;
					case CreateAuctionReturnMessage.MISSING_END_TIME:
						lblError.setText( "End time is required" );
						break;
					case CreateAuctionReturnMessage.END_TIME_INVALID:
						lblError.setText( "End time has to be after the current time!" );
						break;
					case CreateAuctionReturnMessage.MISSING_RESERVE_PRICE:
						lblError.setText( "Reserve price is required" );
						break;
					case CreateAuctionReturnMessage.MISSING_TITLE:
						lblError.setText( "Title is required" );
						break;
					case CreateAuctionReturnMessage.RESERVE_PRICE_NEGATIVE:
						lblError.setText( "Reserve price must be positive" );
						break;
					case CreateAuctionReturnMessage.BAD_IMAGE:
						lblError.setText( "Your image file does not exist." );
						break;
					case CreateAuctionReturnMessage.PENALTY_POINTS:
						JOptionPane.showMessageDialog(f, "You have too many penalty points - you can no longer create auctions", "Too many penalty points", JOptionPane.ERROR_MESSAGE);
						break;
				}
				f.pack(); // So everything displays nicely
			}	
		});
	}
	
	/**
	 * Get the image from the file and save in the variable
	 */
	protected void setImage( File selectedFile ) {
		try {
			BufferedImage bi = ImageIO.read( selectedFile );
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write( bi, ".png", baos );
			baos.flush();
			
			final int size = 400;
			// If the image is large, let's resize it to a more suitable size
			Image i2 = null;
			if( bi.getHeight() > size || bi.getWidth() > size ) {
				i2 = bi.getScaledInstance( size, size, 0 );
			}
			
			if( i2 != null )
				image = new ImageIcon( i2 );
			else
				image = new ImageIcon( bi );
		} catch (IOException e) {}
		
		lblFileName.setText( selectedFile.getName() );
	}
}

/**
 * Simple little filter for the JFileChooser so only jpg, jpeg, png and gifs are allowed
 */
class ImageFileFilter extends FileFilter {
	
	public boolean accept( File f ) {
		if( f.isDirectory() ) 
			return true;
		String fileName = f.getName();
		if( fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")||fileName.endsWith(".gif"))
			return true;
		return false;
	}
	
	public String getDescription() {
		return "Image files (*.jpg, *.jpeg, *.png, *.gif)";
	}
}