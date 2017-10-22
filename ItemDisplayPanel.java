import java.awt.GridLayout;

import javax.swing.JPanel;

/**
 * Main screen of the client, it loads up a item list and item display.
 * 
 * @author Richard Cook
 * @version 1.0
 * @date May 15, 2014
 */
public class ItemDisplayPanel extends JPanel implements CustomPanel {
	
	private static final long serialVersionUID = 1L;

	// Type of item display panel
	public static final int NORMAL = 0;
	public static final int SEARCH = 1;
	public static final int MY_AUCTIONS = 2;
	public static final int MY_BIDS = 3;
	public static final int CATEGORY = 4;
	public static final int USER = 5;

	private GUI f;
	private int type;
	private String search;
	private Item i;
	private ItemPanel di;
	
	/**
	 * Two constructors - one takes all parameters, other assumes they're null
	 */
	public ItemDisplayPanel( GUI f, int type, String search, Item i ) {
		this.f = f;
		this.type = type;
		this.search = search;
		this.i = i;
	}
	
	public ItemDisplayPanel( GUI f, int type ) {
		this.f = f;
		this.type = type;
		this.search = null;
		this.i = null;
	}
	
	public void init() {
		this.setLayout( new GridLayout( 1, 2 ) ); // Basic layout
		
		// What panel it loads depends on the type of list
		if( type == NORMAL || type == SEARCH || type == USER || type == CATEGORY  ) {
			ItemList il = new ItemList( f, this, type, search );
			il.init();
			this.add( il );
		} else if( type == MY_AUCTIONS ) {
			MyAuctions ma = new MyAuctions( f, this );
			ma.init();
			this.add( ma );
		} else {
			MyBids mb = new MyBids( f, this );
			mb.init();
			this.add( mb );
		}

		di = new ItemPanel( f, i );
		di.init();
		this.add( di );
	}

	// Changes the item
	public void displayItem(String itemCode) {
		di.setItem( itemCode );
	}
}