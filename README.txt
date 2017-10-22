/**
 * README for auction system coursework for Programming II (COMP1206)
 *
 * @author Richard Cook
 * @version 1.0
 * @date May 15, 2014
 */
 
/* Contents */
 1) What is in the folder?
 2) How to run the auction system
 3) Cool features
/* End Contents */
 
/* 1. What Is In The Folder? */
  * Source code
	- As required, the source code is present
	- Contains 19 files, 42 classes, 1 interface, 23 anonymous classes and 4205 lines of code
  * There is also a folder called Runnables, which has runnable versions of the Server and Client
    - Contains Client.jar and Server.jar
	- Also contains a shortcut to Client.jar
  * This Readme!
/* End 1. */
 
/* 2. How To Run The Auction System */
  * To run correctly, Client.jar and Server.jar have to be in the same folder
	 * This is because they will share common folders, which are by default made in the folder the .jars are being run from
	 * This may be inconvenient for the end user, hence the shortcut to Client.jar also in the folder
  * To boot-up the server, just open Server.jar. This will open up the Server's GUI and create the following folders
	     * Items
	     * Messages
	     * Notifications
	     * Users
	 * as well as the file activity.log and activity.log.key
	 * Future calls to the server will use these folders and files to re-start the server in it's previous state
  * To run the client, simply open the Client.jar file. Multiple clients can run at once.

/* End 2. */

/* 3. Cool Features */
 As well as the specified content, there is also the following extensions and cool features in the system:
 * All data is encrypted using DES. This includes messages, notifications, user files, item files and the activity log
 * Images can be added to auctions. If images are above a certain size, they will be resized to be more screen-appropriate
 * Users can withdraw items from sale. If there are bids on these items (higher than the reserve price), the user gains penalty points. More than two points and they can no longer list auctions
 * User's can search item's description field
 * When displaying an item on the screen there's a countdown clock saying the number of days, hours, minutes and seconds left. Updated every second by a dedicated thread
 * Notifications are displayed when a user bids, has been outbidded, has won or lost an auction, has sold or failed to sell an auction or an auction has been withdrawn. When the user receives a notification, and the client is logged in, they will automatically receive a notification symbol on the menubar (it turns red with the number of notifications). If not logged in, notifications will appear when the user next logs on.
 * The activity log (console) is saved and loaded at the start of a new server session, allowing you to see what the sever was doing before crashing/restarting
 * Combined search: You can combine searches, so if you want all items by user bob in the entertainment category, including auctions that are no longer active, you can display this. All five searches/filters ( category, user, start date, description search, show inactive auctions) can be combined at once.
 * Users can view the bidding history of an item
 * The item list is displayed in a JTable, allowing the sorting of each column by clicking on the column header.
/* End 3. */