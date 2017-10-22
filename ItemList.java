import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * Creates the list of auctions.
 * 
 * @author Richard Cook
 * @version 1.0
 * @date May 15, 2014
 */
public class ItemList extends JPanel {
	
	private static final long serialVersionUID = 1L;

	private int type; // The type of results being displayed
	private String input; // What the user searched for (if applicable) 
	private List<Item> items;  // Complete list of items
	
	private GUI f;
	private ItemDisplayPanel id;
	
	// Search-related variables
	private JComboBox<String> cbCategory;
	private JTextField tfUser, tfSearch;
	private JCheckBox cbActive;
	private DatePanel pnlDate;
	
	// Table-related variables
	private final String[] columnNames = { "ID", "Name", "Category", "Current Price", "End Time", 
			"Vendor" };
	private CustomTableModel tableModel;
	private JTable table;
	private TableRowSorter<TableModel> rowSorter;
	
	public ItemList( GUI f, ItemDisplayPanel id, int type, String input ) {
		this.f = f;
		this.type = type;
		this.input = input;
		this.id = id;
	}
	
	public void init() {
		// Get the item list from the server
		items = sendRequest( type, input );
		
		// Make the item list into table data
		Object[][] data = updateTableData();
		
		/* Set up table */
		tableModel = new CustomTableModel( columnNames, data );
		table = new JTable( tableModel );

		// ListSelectionModel to click on an item's row and display them in the item panel
		ListSelectionModel lsm = table.getSelectionModel();
		lsm.addListSelectionListener( new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				int row = table.getSelectedRow();
				if( row >= 0 ) {
					String itemCode = (String) table.getValueAt( row, 0);
					id.displayItem( itemCode );
				}
			}
		});
		table.setSelectionModel( lsm );
		
		// RowSorter to sort columns by clicking on the category name
		rowSorter = new TableRowSorter<TableModel>( tableModel );
		/* End set up of table */

		/* Set up GUI */
		GridBagLayout gbl = new GridBagLayout( );
		GridBagConstraints gbc = new GridBagConstraints();
		this.setLayout( gbl );
		
		gbc.gridwidth = 2;
		JLabel lblRefine = new JLabel( "Refine this list" );
		gbl.setConstraints( lblRefine, gbc);
		this.add( lblRefine );
		
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		JLabel lblCategory = new JLabel( "Category: " );
		gbl.setConstraints( lblCategory, gbc );
		this.add( lblCategory );
			
		// Put all the categories into an array proceeded by an empty field
		String[] categories = new String[Item.KEYWORD_CATEGORIES.length + 1];
		categories[0] = null;
		for( int i = 1; i <= Item.KEYWORD_CATEGORIES.length; i++ )
			categories[i] = Item.KEYWORD_CATEGORIES[i-1];
		
		cbCategory = new JComboBox<String>( categories );
		
		if( type == ItemDisplayPanel.CATEGORY ) 
			cbCategory.setSelectedItem( input );

		gbc.gridx = 1;
		gbl.setConstraints( cbCategory, gbc );
		this.add( cbCategory );
		
		JLabel lblUser = new JLabel( "User: " );
		gbc.gridy = 2;
		gbc.gridx = 0;
		gbl.setConstraints( lblUser, gbc );
		this.add( lblUser );
					
		gbc.gridx = 1;
		tfUser = new JTextField( 15 );
		gbl.setConstraints( tfUser, gbc );
		this.add( tfUser );
		
		if( type == ItemDisplayPanel.USER )
			tfUser.setText(input);
		
		JLabel lblDate = new JLabel( "Start date (DD/MM/YYYY HH:MM): " );
		gbc.gridy = 3;
		gbc.gridx = 0;
		gbl.setConstraints( lblDate, gbc );
		this.add( lblDate );

		pnlDate = new DatePanel();
		gbc.gridx = 1;
		gbl.setConstraints( pnlDate, gbc );
		this.add( pnlDate );
		
		JLabel lblSearch = new JLabel( "Search description: " );
		gbc.gridy = 4;
		gbc.gridx = 0;
		gbl.setConstraints( lblSearch, gbc );
		this.add( lblSearch );

		tfSearch = new JTextField( 20 );
		gbc.gridx = 1;
		gbl.setConstraints( tfSearch, gbc);
		this.add( tfSearch );
		
		cbActive = new JCheckBox( "Show ended auctions? " );
		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.gridwidth = 2;
		gbl.setConstraints( cbActive, gbc);
		this.add( cbActive );
		
		gbc.gridx = 0;
		gbc.gridy = 6;
		JButton btnSearch = new JButton( "Search" );
		gbl.setConstraints( btnSearch, gbc );
		this.add( btnSearch );
		
		btnSearch.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				newSearch();
			}
		});

		setTableColumns();
		
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setPreferredSize( new Dimension( 500, 500 ) );
		gbc.gridwidth = 4;
		gbc.gridx = 0;
		gbc.gridy = 7;
		gbl.setConstraints( scrollPane, gbc);
		this.add( scrollPane );
	}
	
	/**
	 * Sends a request to the server for the item list - only used for intial call
	 */
	private List<Item> sendRequest( int type, String input) {
		// Send a message
		String clientID = f.getClientID();
		Message m;
		if( type == ItemDisplayPanel.CATEGORY )
			m = new ItemListMessage( clientID, type, input, null, null, null, false );
		else if( type == ItemDisplayPanel.USER )
			m = new ItemListMessage( clientID, type, null, input, null, null, false );
		else
			m = new ItemListMessage( clientID, type, null, null, null, null, false );
		Comms c = f.getComms();
		c.sendMessage( m, Comms.SERVER_NAME );
		
		// Get the return message
		ItemListReturnMessage m2 = (ItemListReturnMessage) c.checkForReturn( clientID, Message.ITEM_LIST_RETURN_MESSAGE );

		return m2.getItems();
		/* End get items */
	}

	/**
	 * Creates a search based off of the textfields and what not
	 * Sets things to null if the field is left empty
	 */
	private void newSearch() {
		String category;
		if( cbCategory.getSelectedIndex() == 0 )
			category = null;
		else
			category = (String) cbCategory.getSelectedItem();
		
		String user;
		if( tfUser.getText().isEmpty() )
			user = null;
		else
			user = tfUser.getText().toLowerCase();
		
		String day = pnlDate.tfDay.getText();
		String month = pnlDate.tfMonth.getText();
		String year = pnlDate.tfYear.getText();
		String hour = pnlDate.tfHour.getText();
		String minute = pnlDate.tfMinute.getText();
		Date date = null;
		
		// If date is not empty, let's process it
		if( ! (day.isEmpty() && month.isEmpty() && year.isEmpty() && hour.isEmpty() && minute.isEmpty() ) ) {
			// But let's make sure every field has a value, 'cos otherwise we have a problem
			// Why not do that in the first place? So we can display a nice error message
			if( ! (day.isEmpty() || month.isEmpty() || year.isEmpty() || hour.isEmpty() || minute.isEmpty() ) ) {
				
				SimpleDateFormat sdf = new SimpleDateFormat( "d/M/yyyy HH:mm" );
				String dateStr = day + "/" + month + "/" + year + " " + hour + ":" + minute;
				
				// Parse the string into a date value
				try {
					date = sdf.parse(dateStr);
				} catch (ParseException e) {
					JOptionPane.showMessageDialog(f, "Error with your date values", "Error", JOptionPane.WARNING_MESSAGE);
				}
			} else 
				JOptionPane.showMessageDialog(f, "Incomplete date", "Incomplete date", JOptionPane.WARNING_MESSAGE);
		}
		
		String description;
		if( tfSearch.getText().isEmpty() )
			description = null;
		else
			description = tfSearch.getText().toLowerCase();
		
		boolean active = cbActive.isSelected();
		
		// Send the message to the server
		String clientID = f.getClientID();
		Message m = new ItemListMessage( clientID, ItemDisplayPanel.NORMAL, category, user, date, description, active );
		Comms c = f.getComms();
		c.sendMessage( m, Comms.SERVER_NAME );
		
		// Get the return list
		ItemListReturnMessage m2 = (ItemListReturnMessage) c.checkForReturn( clientID, Message.ITEM_LIST_RETURN_MESSAGE );
		items = m2.getItems();

		// Refresh the table data
		tableModel.refresh( updateTableData() );
		setTableColumns();
	}
	
	/**
	 * Updates the table data using the current items variable and returns it in an array
	 */
    private Object[][] updateTableData() {
		Object[][] data = new Object[items.size()][columnNames.length];
		int j = 0;

		for( Item i : items ) {
			DecimalFormat df = new DecimalFormat();
			df.setMinimumFractionDigits(2);

			String price = "£" + df.format( i.getLatestBid().getBidValue() );
			Object[] dataSet = { i.getItemID(), i.getTitle(), i.getCategory(), price, i.getEndTime(), i.getVendorID() };
			int k = 0;
			
			for( Object o : dataSet ) {
				data[j][k] = o;
				k++;
			}
			
			j++;
		}
		return data;
	}
    
    /**
     * Sets up the appearance of the table
     */
    private void setTableColumns() {
		table.setRowSorter(rowSorter);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.getColumnModel().getColumn(0).setPreferredWidth(30); // ID
		table.getColumnModel().getColumn(1).setPreferredWidth(150); // Title
		table.getColumnModel().getColumn(2).setPreferredWidth(100); // Category 
		table.getColumnModel().getColumn(3).setPreferredWidth(75); // Price
		table.getColumnModel().getColumn(4).setPreferredWidth(120); // End Time
		table.getColumnModel().getColumn(4).setCellRenderer( new DateCellRenderer() );
		table.getColumnModel().getColumn(5).setPreferredWidth(100); // Vendor
    }
}