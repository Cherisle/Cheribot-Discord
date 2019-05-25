import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class BattleAccount implements Serializable
{
	static final long serialVersionUID = 1L; //assign a long value
	private final String url = "jdbc:sqlite:C://sqlite/db/kanji_storage.db";
	private String filename;
	private String player_discord_guid;
	private int cheribits;
	private ArrayList<Trophy> roster;
	
	public BattleAccount(String pd_name, String pd_id)
	{
		if(checkPDID(pd_name,pd_id) == true)
		{
			DeserializeAccount(pd_name,pd_id);
		}
		else // new battle account
		{
			System.out.println("Creating new battle account..."); 
			player_discord_guid = pd_name + "#" + pd_id; 
			cheribits = 0;
			roster = new ArrayList<>();
			filename = "C://Users/Michael/Documents/Cheribot Discord/Cheribot/BattleAccounts/" + pd_name + "_" + pd_id + ".ser";
			SerializeAccount(filename, false);
		}
	}
	
	public void DeserializeAccount(String pname, String pdid)
	{
		// Deserialization 
        try
        {    
        	filename = "C://Users/Michael/Documents/Cheribot Discord/Cheribot/BattleAccounts/" 
        			+ pname + "_" + pdid + ".ser";
            FileInputStream file = new FileInputStream(filename); 
            ObjectInputStream in = new ObjectInputStream(file); 
              
            BattleAccount ba_deserial = (BattleAccount) in.readObject(); 
            player_discord_guid = ba_deserial.getPlayerGuid(); 
			cheribits = ba_deserial.getCheribits();
			roster = ba_deserial.getTrophies();
            
            in.close(); 
            file.close(); 
              
            System.out.println("BattleAccount deserialized successfully"); 
            System.out.println("GUID = " + getPlayerGuid()); 
            System.out.println("Cheribits = " + getCheribits()); 
        } 
        catch(IOException ex) 
        { 
            System.out.println("Deserial: IOException is caught - " + ex.getMessage()); 
        } 
        catch(ClassNotFoundException ex) 
        { 
            System.out.println("ClassNotFoundException is caught"); 
        } 
	}
	
	public void SerializeAccount(String fname, boolean update)
	{ 
        try
        {    
        	if(update == true)
        	{
        		System.out.println("Updating battle account..."); 
        		File file = new File(fname);
            	if(file.exists())
            	{
            		file.delete(); //remove outdated serializable file
            	}
        	}
        	
        	//Saving of object in a file 
            FileOutputStream file = new FileOutputStream(fname); 
            ObjectOutputStream out = new ObjectOutputStream(file); 
              
            // Method for serialization of object 
            out.writeObject(this); 
              
            out.close(); 
            file.close(); 
              
            System.out.println("BattleAccount serialized successfully"); 
  
        } 
          
        catch(IOException ex) 
        { 
            System.out.println("Serial: IOException is caught"); 
        } 
	}
	
	private boolean checkPDID(String p_name, String p_id)
	{
		String user_check = "SELECT * FROM users WHERE username = '" + p_name + "#" + p_id + "';";
    	
    	try (Connection conn = DriverManager.getConnection(url))
    	{
    		Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(user_check);
            if(rs.next()) 
            {
            	File testFile = new File("C://Users/Michael/Documents/Cheribot Discord/Cheribot/BattleAccounts/" 
            			+ p_name + "_" + p_id + ".ser");
            	return testFile.exists();
            }
            else
            {
            	return false;
            }
        } 
    	catch (SQLException e) 
    	{
            System.out.println(e.getMessage());
        }
    	return false;
	}
	
	public void gainCheribits(int amount)
	{
		cheribits = cheribits + amount;
	}
	
	public int getCheribits()
	{
		return cheribits;
	}
	
	public String getFileName()
	{
		return filename;
	}
	public String getPlayerGuid()
	{
		return player_discord_guid;
	}
	
	public ArrayList<Trophy> getTrophies()
	{
		return roster;
	}
}
