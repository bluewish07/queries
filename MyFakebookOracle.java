package project2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.TreeSet;
import java.util.Vector;

public class MyFakebookOracle extends FakebookOracle {
	
	static String prefix = "yjtang.";
	
	// You must use the following variable as the JDBC connection
	Connection oracleConnection = null;
	
	// You must refer to the following variables for the corresponding tables in your database
	String cityTableName = null;
	String userTableName = null;
	String friendsTableName = null;
	String currentCityTableName = null;
	String hometownCityTableName = null;
	String programTableName = null;
	String educationTableName = null;
	String eventTableName = null;
	String participantTableName = null;
	String albumTableName = null;
	String photoTableName = null;
	String coverPhotoTableName = null;
	String tagTableName = null;
	
	
	// DO NOT modify this constructor
	public MyFakebookOracle(String u, Connection c) {
		super();
		String dataType = u;
		oracleConnection = c;
		// You will use the following tables in your Java code
		cityTableName = prefix+dataType+"_CITIES";
		userTableName = prefix+dataType+"_USERS";
		friendsTableName = prefix+dataType+"_FRIENDS";
		currentCityTableName = prefix+dataType+"_USER_CURRENT_CITY";
		hometownCityTableName = prefix+dataType+"_USER_HOMETOWN_CITY";
		programTableName = prefix+dataType+"_PROGRAMS";
		educationTableName = prefix+dataType+"_EDUCATION";
		eventTableName = prefix+dataType+"_USER_EVENTS";
		albumTableName = prefix+dataType+"_ALBUMS";
		photoTableName = prefix+dataType+"_PHOTOS";
		tagTableName = prefix+dataType+"_TAGS";
	}
	
	
	@Override
	// ***** Query 0 *****
	// This query is given to your for free;
	// You can use it as an example to help you write your own code
	//
	public void findMonthOfBirthInfo() throws SQLException{ 
		
		// Scrollable result set allows us to read forward (using next())
		// and also backward.  
		// This is needed here to support the user of isFirst() and isLast() methods,
		// but in many cases you will not need it.
		// To create a "normal" (unscrollable) statement, you would simply call
		// Statement stmt = oracleConnection.createStatement();
		//
		Statement stmt = oracleConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
		        ResultSet.CONCUR_READ_ONLY);
		
		// For each month, find the number of friends born that month
		// Sort them in descending order of count
		ResultSet rst = stmt.executeQuery("select count(*), month_of_birth from "+
				userTableName+
				" where month_of_birth is not null group by month_of_birth order by 1 desc");
		
		this.monthOfMostFriend = 0;
		this.monthOfLeastFriend = 0;
		this.totalFriendsWithMonthOfBirth = 0;
		
		// Get the month with most friends, and the month with least friends.
		// (Notice that this only considers months for which the number of friends is > 0)
		// Also, count how many total friends have listed month of birth (i.e., month_of_birth not null)
		//
		while(rst.next()) {
			int count = rst.getInt(1);
			int month = rst.getInt(2);
			if (rst.isFirst())
				this.monthOfMostFriend = month;
			if (rst.isLast())
				this.monthOfLeastFriend = month;
			this.totalFriendsWithMonthOfBirth += count;
		}
		
		// Get the names of friends born in the "most" month
		rst = stmt.executeQuery("select user_id, first_name, last_name from "+
				userTableName+" where month_of_birth="+this.monthOfMostFriend);
		while(rst.next()) {
			Long uid = rst.getLong(1);
			String firstName = rst.getString(2);
			String lastName = rst.getString(3);
			this.friendsInMonthOfMost.add(new UserInfo(uid, firstName, lastName));
		}
		
		// Get the names of friends born in the "least" month
		rst = stmt.executeQuery("select first_name, last_name, user_id from "+
				userTableName+" where month_of_birth="+this.monthOfLeastFriend);
		while(rst.next()){
			String firstName = rst.getString(1);
			String lastName = rst.getString(2);
			Long uid = rst.getLong(3);
			this.friendsInMonthOfLeast.add(new UserInfo(uid, firstName, lastName));
		}
		
		// Close statement and result set
		rst.close();
		stmt.close();
	}

	
	
	@Override
	// ***** Query 1 *****
	// Find information about friend names:
	// (1) The longest last name (if there is a tie, include all in result)
	// (2) The shortest last name (if there is a tie, include all in result)
	// (3) The most common last name, and the number of times it appears (if there is a tie, include all in result)
	//
	public void findNameInfo() throws SQLException { // Query1
		Statement stmt = oracleConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
		        ResultSet.CONCUR_READ_ONLY);

		ResultSet rst = stmt.executeQuery("select last_name, length(last_name) as length from "+
				userTableName+
				" order by length desc");

		int max_length = 0;
		while(rst.next()) {
			if (rst.isFirst())
				max_length = rst.getInt(2);
			if (rst.getInt(2) == max_length)
				this.longestLastNames.add(rst.getString(1));
			else
				break;
		}
		rst.afterLast();
		int min_length = max_length;
		while(rst.previous()) {
			if (rst.isLast())
				min_length = rst.getInt(2);
			if (rst.getInt(2) == min_length)
				this.shortestLastNames.add(rst.getString(1));
			else
				break;
		}

		rst = stmt.executeQuery("select last_name, count(*) as freq from "+
				userTableName+" group by last_name order by freq desc");
		int max_freq = 0;
		while(rst.next()) {
			if (rst.isFirst()) {
				max_freq = rst.getInt(2);
				this.mostCommonLastNamesCount = max_freq;
			}
			if (rst.getInt(2) == max_freq)
				this.mostCommonLastNames.add(rst.getString(1));
		}

		// Close statement and result set
		rst.close();
		stmt.close();
		

	}
	
	@Override
	// ***** Query 2 *****
	// Find the user(s) who have no friends in the network
	//
	// Be careful on this query!
	// Remember that if two users are friends, the friends table
	// only contains the pair of user ids once, subject to 
	// the constraint that user1_id < user2_id
	//
	public void lonelyFriends() throws SQLException {
		Statement stmt = oracleConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
		        ResultSet.CONCUR_READ_ONLY);

		ResultSet rst = stmt.executeQuery("select users.user_id, users.first_name, users.last_name from " + 
				"(select user_id from " + userTableName+ " minus " +
					"(select user1_id as user_id from " + friendsTableName + " union " + 
						"select user2_id as user_id from " + friendsTableName
				+ ")) lonelyfriends, " + userTableName + " users " + 	
				"where users.user_id = lonelyfriends.user_id");

		int count = 0;
		while (rst.next()) {
			this.lonelyFriends.add(new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3)));
			count ++;
		}

		this.countLonelyFriends = count;

		// Close statement and result set
		rst.close();
		stmt.close();
	}
	 

	@Override
	// ***** Query 3 *****
	// Find the users who still live in their hometowns
	// (I.e., current_city = hometown_city)
	//	
	public void liveAtHome() throws SQLException {
		Statement stmt = oracleConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
		        ResultSet.CONCUR_READ_ONLY);

		ResultSet rst = stmt.executeQuery("select users.user_id, users.first_name, users.last_name from " + 
					currentCityTableName + " cur, " + hometownCityTableName + " ht, " + userTableName + " users" + 
					" where cur.user_id = users.user_id and ht.user_id = users.user_id and " +
					"cur.current_city_id = ht.hometown_city_id");

		int count = 0;
		while (rst.next()) {
			this.liveAtHome.add(new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3)));
			count++;
		}
		this.countLiveAtHome = count;

		// Close statement and result set
		rst.close();
		stmt.close();

	}



	@Override
	// **** Query 4 ****
	// Find the top-n photos based on the number of tagged users
	// If there are ties, choose the photo with the smaller numeric PhotoID first
	// 
	public void findPhotosWithMostTags(int n) throws SQLException { 
		Statement stmt = oracleConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
		        ResultSet.CONCUR_READ_ONLY);
		ResultSet rst = stmt.executeQuery("select tag_photo_id from " + tagTableName +
										 " group by tag_photo_id " +
										 "order by count(*) desc, tag_photo_id");

		String individualQuery1 = "select p.album_id, a.album_name, p.photo_caption, p.photo_link"
										+ " from " + photoTableName + " p, " + albumTableName + " a " + 
								"where p.photo_id = ? and p.album_id = a.album_id";
		java.sql.PreparedStatement ps1 = oracleConnection.prepareStatement(individualQuery1);


		String individualQuery2 = "select users.user_id, users.first_name, users.last_name from " + 
								userTableName + " users, " + tagTableName + " tags " +	
								" where users.user_id = tags.tag_subject_id and " + 
								"tags.tag_photo_id = ?";
		java.sql.PreparedStatement ps2 = oracleConnection.prepareStatement(individualQuery2);

		String photo_id = "";
		int count = 0;
		while (rst.next()) {
			if (!rst.getString(1).equals(photo_id)) {
				if (count == n)
					break;
				photo_id = rst.getString(1);
				ps1.setString(1, photo_id);
				ResultSet individualrst1 = ps1.executeQuery();
				individualrst1.next();
				PhotoInfo p = new PhotoInfo(photo_id, individualrst1.getString(1), individualrst1.getString(2),
									individualrst1.getString(3), individualrst1.getString(4));
				TaggedPhotoInfo tp = new TaggedPhotoInfo(p);
				
				individualrst1.close();

				ps2.setString(1, photo_id);
				ResultSet individualrst2 = ps2.executeQuery();
				while (individualrst2.next()) {
					tp.addTaggedUser(new UserInfo(individualrst2.getLong(1), individualrst2.getString(2), 
											individualrst2.getString(3)));
					
				}
				individualrst2.close();

				this.photosWithMostTags.add(tp);
				count ++;
			}

		}

		stmt.close();
		ps1.close();
		ps2.close();

	}

	
	
	
	@Override
	// **** Query 5 ****
	// Find suggested "match pairs" of friends, using the following criteria:
	// (1) One of the friends is female, and the other is male
	// (2) Their age difference is within "yearDiff"
	// (3) They are not friends with one another
	// (4) They should be tagged together in at least one photo
	//
	// You should up to n "match pairs"
	// If there are more than n match pairs, you should break ties as follows:
	// (i) First choose the pairs with the largest number of shared photos
	// (ii) If there are still ties, choose the pair with the smaller user_id for the female
	// (iii) If there are still ties, choose the pair with the smaller user_id for the male
	//
	public void matchMaker(int n, int yearDiff) throws SQLException { 
		Statement stmt = oracleConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
		        ResultSet.CONCUR_READ_ONLY);
		ResultSet rst = stmt.executeQuery("select users1.user_id, users1.first_name, users1.last_name, " +
										"users1.year_of_birth, users2.user_id, users2.first_name, " +
										"users2.last_name, users2.year_of_birth from " + 
										userTableName + " users1, " + userTableName + " users2, " + 
										"(select q_users.fuser u1, q_users.muser u2 from " +
											tagTableName + " tags, " + tagTableName + " tags2, " + 
											"(select users1.user_id fuser, users2.user_id muser from " + 
												userTableName + " users1, " + userTableName + " users2, " + 
												friendsTableName + " friends " + 
												"where users1.gender = 'female' and users2.gender = 'male' and " +
												"users1.year_of_birth - users2.year_of_birth <= " + yearDiff + 
												" and " +
												"users1.year_of_birth - users2.year_of_birth >= -" + yearDiff + 
												" and " +
												"not exists (select user1_id, user2_id from " + friendsTableName + 
													" where user1_id = users1.user_id and user2_id = users2.user_id) " +
												"and not exists (select user1_id, user2_id from " + friendsTableName + 
													" where user1_id = users2.user_id and user2_id = users1.user_id)" + 
											") q_users " +
											"where tags.tag_photo_id = tags2.tag_photo_id and " +
											"tags.tag_subject_id = q_users.fuser and " + 
											"tags2.tag_subject_id = q_users.muser " + 
											"group by q_users.fuser, q_users.muser " + 
											"order by count(*) desc, q_users.fuser, q_users.muser" + 
										") results " +
										"where users1.user_id = results.u1 and users2.user_id = results.u2");
		
		String individualQuery = "select shared_photos.tag_photo_id, photo.album_id, album.album_name, " + 
									"photo.photo_caption, photo.photo_link from " + 
								"(select distinct tag_photo_id from " + tagTableName + 
								" where tag_subject_id = ? intersect " + 
								"select distinct tag_photo_id from " + tagTableName + 
								" where tag_subject_id = ?) shared_photos, " + 
								photoTableName + " photo, " + albumTableName + " album " + 
								"where shared_photos.tag_photo_id = photo.photo_id and " + 
									"photo.album_id = album.album_id";
		java.sql.PreparedStatement ps = oracleConnection.prepareStatement(individualQuery);

		int count = 0;
		while (rst.next() && count < n) {
			MatchPair mp = new MatchPair(rst.getLong(1), rst.getString(2), rst.getString(3), rst.getInt(4),
										rst.getLong(5), rst.getString(6), rst.getString(7), rst.getInt(8));
			ps.setLong(1, rst.getLong(1));
			ps.setLong(2, rst.getLong(5));
			ResultSet individualrst = ps.executeQuery();
			while (individualrst.next()) {
				mp.addSharedPhoto(new PhotoInfo(individualrst.getString(1), individualrst.getString(2),
												individualrst.getString(3), individualrst.getString(4),
												individualrst.getString(5)));
			}
			this.bestMatches.add(mp);

			individualrst.close();
		}

		rst.close();
		stmt.close();
		ps.close();

	}

	
	
	// **** Query 6 ****
	// Suggest friends based on mutual friends
	// 
	// Find the top n pairs of users in the database who share the most
	// friends, but such that the two users are not friends themselves.
	//
	// Your output will consist of a set of pairs (user1_id, user2_id)
	// No pair should appear in the result twice; you should always order the pairs so that
	// user1_id < user2_id
	//
	// If there are ties, you should give priority to the pair with the smaller user1_id.
	// If there are still ties, give priority to the pair with the smaller user2_id.
	//
	@Override
	public void suggestFriendsByMutualFriends(int n) throws SQLException {
		Long user1_id = 123L;
		String user1FirstName = "Friend1FirstName";
		String user1LastName = "Friend1LastName";
		Long user2_id = 456L;
		String user2FirstName = "Friend2FirstName";
		String user2LastName = "Friend2LastName";
		FriendsPair p = new FriendsPair(user1_id, user1FirstName, user1LastName, user2_id, user2FirstName, user2LastName);

		p.addSharedFriend(567L, "sharedFriend1FirstName", "sharedFriend1LastName");
		p.addSharedFriend(678L, "sharedFriend2FirstName", "sharedFriend2LastName");
		p.addSharedFriend(789L, "sharedFriend3FirstName", "sharedFriend3LastName");
		this.suggestedFriendsPairs.add(p);
	}
	
	
	//@Override
	// ***** Query 7 *****
	// Given the ID of a user, find information about that
	// user's oldest friend and youngest friend
	// 
	// If two users have exactly the same age, meaning that they were born
	// on the same day, then assume that the one with the larger user_id is older
	//
	public void findAgeInfo(Long user_id) throws SQLException {
		this.oldestFriend = new UserInfo(1L, "Oliver", "Oldham");
		this.youngestFriend = new UserInfo(25L, "Yolanda", "Young");
	}
	
	
	@Override
	// ***** Query 8 *****
	// 
	// Find the name of the city with the most events, as well as the number of 
	// events in that city.  If there is a tie, return the names of all of the (tied) cities.
	//
	public void findEventCities() throws SQLException {
		this.eventCount = 12;
		this.popularCityNames.add("Ann Arbor");
		this.popularCityNames.add("Ypsilanti");
	}
	
	
	
	@Override
//	 ***** Query 9 *****
	//
	// Find pairs of potential siblings and print them out in the following format:
	//   # pairs of siblings
	//   sibling1 lastname(id) and sibling2 lastname(id)
	//   siblingA lastname(id) and siblingB lastname(id)  etc.
	//
	// A pair of users are potential siblings if they have the same last name and hometown, if they are friends, and
	// if they are less than 10 years apart in age.  Pairs of siblings are returned with the lower user_id user first
	// on the line.  They are ordered based on the first user_id and in the event of a tie, the second user_id.
	//  
	//
	public void findPotentialSiblings() throws SQLException {
		Long user1_id = 123L;
		String user1FirstName = "Friend1FirstName";
		String user1LastName = "Friend1LastName";
		Long user2_id = 456L;
		String user2FirstName = "Friend2FirstName";
		String user2LastName = "Friend2LastName";
		SiblingInfo s = new SiblingInfo(user1_id, user1FirstName, user1LastName, user2_id, user2FirstName, user2LastName);
		this.siblings.add(s);


		Statement stmt = oracleConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
		        ResultSet.CONCUR_READ_ONLY);
		/*
		// find all siblings
		//since they also have to be friends, intersect the selected table with FRIENDS
		ResultSet rst = stmt.executeQuery("select U1.user_id, U1.First_Name, U1.Last_Name, U2.user_id, U2.First_Name, U2.Last_Name
			from " + userTableName + " U1, " + userTableName + " U2" +
			" where U1.user_id < U2.user_id AND 
					U1.last_name = U2.last_name AND
					U1.hometown_city = U2.hometown_city AND
					U1.year_of_birth - U2.year_of_birth < 10 AND
					U1.year_of_birth - U2.year_of_birth > -10
			INTERSECT
			select *
			from " + friendsTableName
			"ORDER BY U1.user_id ASC");

		
		// Get the month with most friends, and the month with least friends.
		// (Notice that this only considers months for which the number of friends is > 0)
		// Also, count how many total friends have listed month of birth (i.e., month_of_birth not null)
		//
		while(rst.next()) {
			SiblingInfo s = new SiblingInfo(U1.user_id, U1.First_Name, U1.Last_Name, U2.user_id, U2.First_Name, Last_Name);
			this.siblings.add(s);
		}
		
		// Close statement and result set
		rst.close();
		stmt.close();
		*/

	}
	
}
