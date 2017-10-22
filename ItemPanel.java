import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;

public class ItemPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private Item i;
	private GUI f;
	
	public ItemPanel( GUI f, Item i ) {
		this.f = f;
		this.i = i;
	}
	
	public void init() {
		// Can't display an item we don't have
		if( i != null ) {
			GridBagLayout gbl = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();
			this.setLayout( gbl );
			
			// Only add the image to the screen if it exists
			ImageIcon image = i.getImage();
			if( image != null ) {
				JLabel lblImage = new JLabel( image );
				gbl.addLayoutComponent(lblImage, gbc);
				this.add(lblImage);
			}
			
			gbc.gridy = 1;
			gbc.anchor = GridBagConstraints.NORTH;
			JLabel lblTitle = new JLabel( "Details about \"" + i.getTitle() + "\"" );
			lblTitle.setFont( lblTitle.getFont().deriveFont(Font.BOLD).deriveFont(20f) );
			gbl.addLayoutComponent( lblTitle, gbc );
			this.add( lblTitle );
			
			gbc.gridy = 4;
			gbc.gridheight = 1;
			JLabel lblUser = new JLabel( "Sold by " + i.getVendorID() );
			gbl.addLayoutComponent( lblUser, gbc );
			this.add( lblUser );
			
			gbc.gridy = 5;
			Date endTime = i.getEndTime();
			
			// Convert the end time into a nice countdown clock
			JLabel lblEndsIn = new JLabel();
			gbl.addLayoutComponent( lblEndsIn, gbc );
			this.add( lblEndsIn );
			new Thread( new CountdownThread( endTime, lblEndsIn ) ).start();
			
			BidPanel pnlBids = new BidPanel( this, f, i );
			pnlBids.init();
			gbc.gridy = 6;
			gbc.gridheight = 4;
			gbl.setConstraints(pnlBids, gbc);
			this.add( pnlBids );
			
			// Add description through a panel as it's nicer
			DescriptionPanel pnlDesc = new DescriptionPanel( i );
			pnlDesc.init();
			gbc.gridy = 11;
			gbc.gridheight = 4;
			gbl.setConstraints(pnlDesc, gbc);
			this.add( pnlDesc );
			
			// If the seller is viewing the item, give them a withdraw option
			if( i.getVendorID().equals( f.getUserID() ) && i.isActive() ) {
				gbc.gridy = 15;
				gbc.gridheight = 1;
				JButton btnWithdraw = new JButton( "Withdraw your auction" );
				gbl.setConstraints(btnWithdraw, gbc);
				this.add( btnWithdraw);
				
				btnWithdraw.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent arg0) {
						// Want them to confirm it, as withdrawing an auction is quite a big thing
						int option = JOptionPane.showConfirmDialog(f, "Are you sure? "
								+ "If there are bids placed higher than the reserve price "
								+ "you will be given a penalty point. Users with more than two"
								+ " penalty points cannot list auctions.", "Are you sure?", 
								JOptionPane.YES_NO_OPTION );

						if( option == JOptionPane.OK_OPTION ) {
							String clientID = f.getClientID();
							// Send message to the server
							Message m = new CancelAuctionMessage( clientID, i.getItemID() );
							Comms comms = f.getComms();
							comms.sendMessage( m, Comms.SERVER_NAME );
							
							CancelAuctionReturnMessage m2 = (CancelAuctionReturnMessage) comms.checkForReturn( clientID, Message.CANCEL_AUCTION_RETURN_MESSAGE );
							
							JOptionPane.showMessageDialog(f, "Auction cancelled - you have " + m2.getPenaltyPoints() + " penalty points.", "Auction withdrawn", JOptionPane.INFORMATION_MESSAGE );
							f.setScreen( new ItemDisplayPanel(f, ItemDisplayPanel.NORMAL));
						}
					}
				});
			}
			
			f.pack();
		} else {
			// If no item, tell them how to display one
			JLabel lblClick = new JLabel( "Click on an item to view it in more details" );
			this.add( lblClick );
		}
	}
	
	/**
	 * Changes the item in the panel
	 */
	public void setItem( Item i ) {
		this.i = i;
		reset();
	}
	/**
	 * Sets the new item in the panel
	 */
	public void reset() {
		this.removeAll();
		this.repaint();
		this.revalidate();
		init();
	}

	/**
	 * If we only have the itemCode, get the item from the server then call the other setItem()
	 */
	public void setItem(String itemCode) {
		String clientID = f.getClientID();
		
		// Send message to server
		Message m = new ItemDisplayMessage( clientID, itemCode );
		Comms comms = f.getComms();
		comms.sendMessage( m, Comms.SERVER_NAME );
		
		ItemDisplayReturnMessage m2 = (ItemDisplayReturnMessage) comms.checkForReturn( clientID, Message.ITEM_DISPLAY_RETURN_MESSAGE );
		
		setItem( m2.getItem() );
	}
}

/**
 * Small little panel which handles the GUI and server calls for bidding
 * 
 * @author Richard Cook
 * @version 1.0
 * @date May 15, 2014
 */
class BidPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	// Important variables set up in the constructor
	private GUI f;
	private Item i;
	private ItemPanel di;
	private DecimalFormat df;
	
	public BidPanel( ItemPanel di, GUI f, Item i ) {
		this.f =f;
		this.i = i;
		this.di = di;
		df = new DecimalFormat();
		df.setMinimumFractionDigits(2);
	}
	
	public void init() {
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		this.setLayout(gbl);
		
		// Border to distinguish between parts of the GUI
		this.setBorder( BorderFactory.createEtchedBorder(EtchedBorder.RAISED) );
		
		gbc.gridy = 0;
		gbc.gridwidth = 3;
		JLabel lblBids = new JLabel( "Bids" );
		lblBids.setFont( lblBids.getFont().deriveFont(1).deriveFont(15f) );
		gbl.setConstraints(lblBids, gbc);
		this.add( lblBids );
		
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		JLabel lblCurrentBidTitle = new JLabel( "Current bid:" );
		gbl.addLayoutComponent( lblCurrentBidTitle, gbc );
		this.add( lblCurrentBidTitle );
		
		gbc.gridx = 1;
		gbc.gridwidth = 1;
		Bid bid = i.getLatestBid();
		String bidText;
		
		if( bid != null ) 
			bidText = "£" + df.format(bid.getBidValue() );
		else
			bidText = "No one's bidded yet. Be the first!";
		
		JLabel lblCurrentBid = new JLabel( bidText );
		gbl.addLayoutComponent( lblCurrentBid, gbc );
		this.add( lblCurrentBid );
		
		gbc.gridx = 2;
		JLabel lblPrevious = new JLabel( "(bid history)" );
		
		Map<TextAttribute, Object> map = new Hashtable<TextAttribute, Object>();
		// Make a nice hyperlink feel
		map.put( TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON );
		final Font font = lblPrevious.getFont().deriveFont( map );
		lblPrevious.setFont( font );
		lblPrevious.setForeground( Color.BLUE );
		
		gbl.addLayoutComponent( lblPrevious, gbc );
		this.add( lblPrevious );
		
		lblPrevious.addMouseListener( new MouseListener() {

			@Override
			public void mouseClicked( MouseEvent e ) {
				showBidHistory();
			}

			@Override public void mouseEntered( MouseEvent e ) {}
			@Override public void mouseExited( MouseEvent e ) {	}
			@Override public void mousePressed( MouseEvent e ) {}
			@Override public void mouseReleased( MouseEvent e ) {}
		});

		// Only show the bid field and button when the auction's active
		if( i.isActive() ) {
			gbc.gridy = 2;
			gbc.gridx = 0;
			JLabel lblPlaceBid = new JLabel( "Place a bid: £" );
			gbl.addLayoutComponent( lblPlaceBid, gbc );
			this.add( lblPlaceBid );
			
			gbc.gridx = 1;
			final JTextField tfBid = new JTextField( 6 );
			gbl.addLayoutComponent( tfBid, gbc );
			this.add( tfBid );
			
			gbc.gridx = 2;
			JButton btnBid = new JButton( "Submit" );
			gbl.addLayoutComponent( btnBid, gbc );	
			this.add( btnBid );
			
			/**
			 * Sends the bid to the server and gets response
			 */
			btnBid.addActionListener( new ActionListener() {
	
				@Override
				public void actionPerformed(ActionEvent arg0) {
					String clientID = f.getClientID();
					
					String bid = tfBid.getText();
					// In the unlikely case someone put a pound at the start 
					if( bid.startsWith( "£" ) )
						bid = bid.replace("£", "");
					
					// Send message to server
					Message m = new BidMessage( clientID, i, f.getUserID(), bid );
					Comms c = f.getComms();
					c.sendMessage( m, Comms.SERVER_NAME );
					
					BidReturnMessage m2 = (BidReturnMessage) c.checkForReturn( clientID, Message.BID_RETURN_MESSAGE );
					
					// Display success or error in pop-up message
					switch( m2.getType() ) {
						case BidReturnMessage.SUCCESS:
							JOptionPane.showMessageDialog(f, "Bid succeded!", "Bid succeded", JOptionPane.INFORMATION_MESSAGE);
							di.setItem( m2.getItem() ); // Refresh screen with new bid
							break;
						case BidReturnMessage.BID_MISSING:
							JOptionPane.showMessageDialog(f, "You must enter a value to bid", "Error - missing data", JOptionPane.WARNING_MESSAGE);
							break;
						case BidReturnMessage.INVALID_BID:
							JOptionPane.showMessageDialog(f, "Bid must be a monetary value", "Invalid bid", JOptionPane.WARNING_MESSAGE);
							break;
						case BidReturnMessage.LOW_BID:
							JOptionPane.showMessageDialog(f, "Bid lower than the current highest bid", "Failed bid", JOptionPane.WARNING_MESSAGE);
							break;
						case BidReturnMessage.NEGATIVE_BID:
							JOptionPane.showMessageDialog(f, "Bid must be a positive number", "Invalid bid", JOptionPane.WARNING_MESSAGE);
							break;
						case BidReturnMessage.SELLER_BIDDER:
							JOptionPane.showMessageDialog(f, "You can't bid on your own auction!", "Error - bidding on your own item", JOptionPane.WARNING_MESSAGE);
							break;
					}
				}
			});
			
			gbc.gridx = 0;
			gbc.gridwidth = 3;
			gbc.gridy = 3;
			JLabel lblReservePrice = new JLabel( "Note: This item has a reserve price of £" + df.format( i.getReservePrice() ) );
			gbl.addLayoutComponent( lblReservePrice, gbc );
			this.add( lblReservePrice );
		}
	}

	/**
	 * Opens a new JFrame listing all the previous bids on the item
	 * Simply iterates through the bid list and displays the values on screen
	 */
	protected void showBidHistory() {
		JFrame f = new JFrame( "Bid history for " + i.getTitle() );
		List<Bid> bids = i.getBids();
		
		f.setLayout( new GridLayout( bids.size()+1, 2 ) );
		f.add( new JLabel ( "User" ) );
		f.add( new JLabel( "Value" ) );
		
		for( Bid b : bids ) {
			f.add( new JLabel( b.getBidder() ) );
			f.add( new JLabel( "£" + df.format(b.getBidValue() ) ));
		}
		
		f.pack();
		f.setVisible(true);
	}
}

/**
 * Custom panel to display the description
 * 
 * @author Richard Cook
 * @version 1.0
 * @date May 15, 2014
 */
class DescriptionPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private Item i;
	
	public DescriptionPanel( Item i ) {
		this.i = i;
	}
	
	public void init() {
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		this.setLayout(gbl);
		
		this.setBorder( BorderFactory.createEtchedBorder(EtchedBorder.LOWERED) );

		JLabel lblDescriptionTitle = new JLabel( "Description" );
		lblDescriptionTitle.setFont( lblDescriptionTitle.getFont().deriveFont(20f).deriveFont(1));
		gbl.addLayoutComponent( lblDescriptionTitle, gbc );
		this.add( lblDescriptionTitle );
		
		gbc.gridwidth = 3;
		gbc.gridheight = 3;
		gbc.gridy = 1;
		// HTML so it line breaks and what not
		JLabel lblDescription = new JLabel( "<html><body style='width:300px'>" + i.getDescription() + "</body><html>" );
		gbl.addLayoutComponent( lblDescription, gbc );
		this.add( lblDescription );
	}
}

/**
 * Little thread which runs the countdown clock
 * 
 * @author Richard Cook
 * @version 1.0
 * @date May 15, 2014
 */
class CountdownThread implements Runnable {

	private Date endTime;
	private JLabel lblEndsIn;
	
	public CountdownThread( Date endTime, JLabel lblEndsIn ) {
		this.endTime = endTime;
		this.lblEndsIn = lblEndsIn;
	}
	
	@Override
	public void run() {
		while( !Thread.currentThread().isInterrupted() ) {
			String endsIn;
			long remaining = endTime.getTime() - new Date().getTime();
			
			while( remaining > 0 ) {
				int noDays = (int) (remaining / (1000*60*60*24));
				remaining -= 1000*60*60*24*noDays;
				int noHours = (int) (remaining / (1000*60*60) );
				remaining -= 1000*60*60*noHours;
				int noMinutes = (int) (remaining / (1000*60));
				remaining -= 1000*60*noMinutes;
				int noSeconds = (int) (remaining/ 1000);
				
				endsIn = "Ends in " + noDays + " days, " + noHours + " hours, " + noMinutes + " minutes and " + noSeconds + " seconds."; 
				lblEndsIn.setText( endsIn );
				
				try {
					Thread.sleep(800); // Want it to update every second, but run every 800 milliseconds so there's time to process the code
				} catch (InterruptedException e) {}
				// Recalculate the remaining, otherwise it becomes the noSeconds left
				remaining = endTime.getTime() - new Date().getTime();
			}
			lblEndsIn.setText( "This auction has ended" );
		}
	}
}