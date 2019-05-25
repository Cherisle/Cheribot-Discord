import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.client.entities.Group;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.AudioManager;
import net.dv8tion.jda.core.hooks.EventListener;
import javax.security.auth.login.LoginException;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.time.LocalDate;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CheribotMain extends ListenerAdapter
{

	public static void main(String[] args) throws LoginException, InterruptedException
	{
		try
		{
			String token = "NTA3Mzg0NDQzNTY5NDM4NzUw.Dr2DgA.pQ8uarpO-x2Ke84FxwhBA_w9L1Q";
			// Note: It is important to register your ReadyListener before building
			JDA jda = new JDABuilder(token).addEventListener(new CheribotMain()).build();
			jda.awaitReady(); // Blocking guarantees that JDA will be completely loaded.
		}
        catch (LoginException e)
        {
            //If anything goes wrong in terms of authentication, this is the exception that will represent it
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            //Due to the fact that awaitReady is a blocking method, one which waits until JDA is fully loaded,
            // the waiting can be interrupted. This is the exception that would fire in that situation.
            //As a note: in this extremely simplified example this will never occur. In fact, this will never occur unless
            // you use awaitReady in a thread that has the possibility of being interrupted (async thread usage and interrupts)
            e.printStackTrace();
        }        
	}
	
	private static ArrayList<Player> players;
	private static boolean quizSelectDone;
	private final String url = "jdbc:sqlite:C://sqlite/db/kanji_storage.db";
	private final AudioPlayerManager playerManager;
	private final Map<Long, GuildMusicManager> musicManagers;
	private final Map<Integer,String> ffMap;
	
	private CheribotMain()
	{
	    this.ffMap = new HashMap<>();
	    loadFunFacts(this.ffMap);
		this.musicManagers = new HashMap<>();	    
	    this.playerManager = new DefaultAudioPlayerManager();
	    AudioSourceManagers.registerRemoteSources(playerManager);
	    AudioSourceManagers.registerLocalSource(playerManager);
	    quizSelectDone = false;
	    players = new ArrayList<Player>();
	    createNewDatabase("kanji_storage.db");
	}

	private void loadFunFacts(Map<Integer, String> ffmap)
	{
		ffmap.put(0,"The sun loses 4 million tons of mass every second");
		ffmap.put(1,"The narrowest distance between mainland Russia and mainland Alaska is approximately 55 miles. However, in the body of water between Alaska and Russia, known as the Bering Strait, there lies two small islands known as Big Diomede and Little Diomede. Interestingly enough, Big Diomede is owned by Russia while Little Diomede is owned by the US. The stretch of water between these two islands is only about 2.5 miles wide and actually freezes over during the winter so you could technically walk from the US to Russia on this seasonal sea ice.");
		ffmap.put(2,"Vikings used the bones of slain animals when smithing new weapons believing this would enchant the weapon with the animals spirit. This actually made the weapons stronger because the carbon in the bones coupled with the iron made a primitive version of steel.");
		ffmap.put(3,"Cannibalism is not illegal in most/all of the US. The actual act of eating human flesh is perfectly legal. It's legally aquiring safe human flesh to consume which is the problem. You cant forcibly take it that's assault/murder. You cant ask for flesh, that's coersion. You can't gift human flesh as that's like 30 felonies. You cant take it off a dead guy, that's corpse desecration. You basically have to find human meat laying around, with no one else in sight, no obvious causes of the meat, and still have it be fresh enough to not kill you.\nBut congrats, you now ate person in the US. Legally even.");
		ffmap.put(4,"If you cool liquid helium just a few degrees below its boiling point of –452 degrees Fahrenheit (–269 C) it will suddenly be able to do things that other fluids can't, like dribble through molecule-thin cracks, climb up and over the sides of a dish, and remain motionless when its container is spun.");
	}
	
	private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild)
	{
	    long guildId = Long.parseLong(guild.getId());
	    GuildMusicManager musicManager = musicManagers.get(guildId);

	    if (musicManager == null) {
	      musicManager = new GuildMusicManager(playerManager);
	      musicManagers.put(guildId, musicManager);
	    }

	    guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

	    return musicManager;
	}
	
	@Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        //These are provided with every event in JDA
        JDA jda = event.getJDA();                       //JDA, the core of the api.
        long responseNumber = event.getResponseNumber();//The amount of discord events that JDA has received since the last reconnect.

        //Event specific information
        User author = event.getAuthor();                //The user that sent the message
        Message message = event.getMessage();           //The message that was received.
        MessageChannel channel = event.getChannel();    //This is the MessageChannel that the message was sent to.
                                                        //  This could be a TextChannel, PrivateChannel, or Group!

        String msg = message.getContentDisplay();       //This returns a human readable version of the Message. Similar to
                                                        // what you would see in the client.

        boolean bot = author.isBot();                    //This boolean is useful to determine if the User that
                                                        // sent the Message is a BOT or not!
        
        String[] command = message.getContentRaw().split(" ", 2);

        if (event.isFromType(ChannelType.TEXT))         //If this message was sent to a Guild TextChannel
        {
            //Because we now know that this message was sent in a Guild, we can do guild specific things
            // Note, if you don't check the ChannelType before using these methods, they might return null due
            // the message possibly not being from a Guild!
        	
            Guild guild = event.getGuild();             //The Guild that this message was sent in. (note, in the API, Guilds are Servers)
            TextChannel textChannel = event.getTextChannel(); //The TextChannel that this message was sent to.
            Member member = event.getMember();          //This Member that sent the message. Contains Guild specific information about the User!

            String name;
            if (message.isWebhookMessage())
            {
                name = author.getName();                //If this is a Webhook message, then there is no Member associated
            }                                           // with the User, thus we default to the author for name.
            else
            {
                name = member.getEffectiveName();       //This will either use the Member's nickname if they have one,
            }                                           // otherwise it will default to their username. (User#getName())

            System.out.printf("(%s)[%s]<%s>: %s\n", guild.getName(), textChannel.getName(), name, msg);
        }
        else if (event.isFromType(ChannelType.PRIVATE)) //If this message was sent to a PrivateChannel
        {
            //The message was sent in a PrivateChannel.
            //In this example we don't directly use the privateChannel, however, be sure, there are uses for it!
            PrivateChannel privateChannel = event.getPrivateChannel();

            System.out.printf("[PRIV]<%s>: %s\n", author.getName(), msg);
        }
        else if (event.isFromType(ChannelType.GROUP))   //If this message was sent to a Group. This is CLIENT only!
        {
            //The message was sent in a Group. It should be noted that Groups are CLIENT only.
            Group group = event.getGroup();
            String groupName = group.getName() != null ? group.getName() : "";  //A group name can be null due to it being unnamed.

            System.out.printf("[GRP: %s]<%s>: %s\n", groupName, author.getName(), msg);
        }


        //Now that you have a grasp on the things that you might see in an event, specifically MessageReceivedEvent,
        // we will look at sending / responding to messages!
        //This will be an extremely simplified example of command processing.
        int player_index = 0; //init
        if(players != null)
        {
        	for(int ii=0;ii<players.size();ii++)
        	{
        		if(players.get(ii).getName().equals(author.getName()) && players.get(ii).getId().equals(author.getId()))
        		{
        			player_index = ii;
        			if(players.get(ii).getInQuiz() == true && players.get(ii).getActive() == true)
        			{
        				if(players.get(ii).getQuizSelectStatus())
            			{
            				System.out.println(players.get(ii).getName() + " currently in quiz and typed");
                			int curr_index = players.get(ii).getCurrentIndex()+1;
                			String quiz_response = "Response accepted from currently in-quiz user " + players.get(ii).getName() + " to: Question " + curr_index;
                			channel.sendMessage(quiz_response).queue();
                			players.get(ii).assignAnswer(curr_index, msg);
                			players.get(ii).setCurrentIndex(players.get(ii).getCurrentIndex()+1);
                			if(players.get(ii).getCurrentIndex() == players.get(ii).getQuiz().getTotalQuestions()) //incremented index hit total amount
                			{
                				String thank_response = "Thanks for taking the quiz!";
                				channel.sendMessage(thank_response).queue();
                				players.get(ii).setCurrentIndex(0); //reset the index
                				players.get(ii).setInQuiz(false,1,channel); // end the quiz
                			}
                			else
                			{
                				players.get(ii).getQuiz().printQuestion(players.get(ii).getCurrentIndex(), channel);
                			}
            			}
            			else
            			{
            				
            				if(msg.equals("1") || msg.equals("2") || msg.equals("3") || msg.equals("4") || msg.equals("5")
            						|| msg.equals("N1") || msg.equals("N2") || msg.equals("N3") || msg.equals("N4")
            						|| msg.equals("N5"))
            				{
            					String jlpt_input = "";
            					if(msg.equals("1") || msg.equals("2") || msg.equals("3") || msg.equals("4") || msg.equals("5"))
            					{
            						jlpt_input = "N" + msg;
            					}
            					else
            					{
            						jlpt_input = msg;
            					}
            					//players.get(ii).assignQuiz(getTotalKanjiCount(url,msg),url,jlpt_input);
            					players.get(ii).assignQuiz(10,url,jlpt_input);
             				    players.get(ii).setAnswerSheet(players.get(ii).getQuiz().getTotalQuestions());
             				    players.get(ii).getQuiz().printQuestion(players.get(ii).getCurrentIndex(),channel);
            				}
            				else
            				{
            					channel.sendMessage("Invalid quiz option, setting quiz to default [N5]").queue();
            					//players.get(ii).assignQuiz(getTotalKanjiCount(url,"N5"),url,"N5");
            					players.get(ii).assignQuiz(10,url,"N5");
             				    players.get(ii).setAnswerSheet(players.get(ii).getQuiz().getTotalQuestions());
             				    players.get(ii).getQuiz().printQuestion(players.get(ii).getCurrentIndex(),channel);
            				}
            				players.get(ii).setQuizSelectDone(); // selection done
            			}
            			break;
        			}
        		}
        	}
        }

        //Remember, in all of these .equals checks it is actually comparing
        // message.getContentDisplay().equals, which is comparing a string to a string.
        // If you did message.equals() it will fail because you would be comparing a Message to a String!
        if (msg.equals("!ping"))
        {
            //This will send a message, "pong!", by constructing a RestAction and "queueing" the action with the Requester.
            // By calling queue(), we send the Request to the Requester which will send it to discord. Using queue() or any
            // of its different forms will handle ratelimiting for you automatically!

            channel.sendMessage("pong!").queue();
        }
        else if (msg.equals("!bits"))
        {
        	int my_bits = players.get(player_index).getBattleAccount().getCheribits();
        	channel.sendMessage("You have " + my_bits + " cheribits in your account").queue();
        }
        else if (msg.equals("!kanji"))
        {
        	LocalDate date = LocalDate.now();
        	String str_date = date.toString().replaceAll("-","");
        	int seed = Integer.parseInt(str_date);
        	Random rand = new Random(seed);
        	long rand_val = 0;
        	if(rand.nextInt()<0) // if random value is negative
        	{
        		rand_val = (int)Math.abs(rand.nextInt());
        	}
        	else
        	{
        		rand_val = rand.nextInt();
        	}
        	int k_size = 0; //initialize
        	// Grab kanjis table size
        	String kanjiTable_size = "SELECT count(1) as cnt FROM kanjis;";
        	try (Connection conn = DriverManager.getConnection(url);
                    Statement stmt  = conn.createStatement();
                    ResultSet rs    = stmt.executeQuery(kanjiTable_size))
        	{
                   
                   //loop through the result set
                   if (rs.next()) { k_size = rs.getInt("cnt"); }
            } catch (SQLException e) { System.out.println(e.getMessage()); }
        	
        	int kanji_key = (int) (rand_val % k_size);
        	System.out.println(rand_val);
        	
        	displayKanji(kanji_key,channel,false);
        }
        else if (command[0].equals("!lookup") && command.length == 2)
        {
        	int lookup_id = 0; //init
        	String lookup_sql = "SELECT id FROM kanjis WHERE kanji = '" + command[1] + "';";
        	try (Connection conn = DriverManager.getConnection(url);
                    Statement stmt  = conn.createStatement();
                    ResultSet rs    = stmt.executeQuery(lookup_sql))
        	{                  
                   //loop through the result set
                   if (rs.next()) { lookup_id = rs.getInt("id"); }
            } catch (SQLException e) { System.out.println(e.getMessage()); }
        	System.out.println("Kanji Key: " + lookup_id);
        	displayKanji(lookup_id,channel,true);
        }
        else if (msg.equals("!funfact"))
        {
        	Random rand = new Random();
        	int roll = rand.nextInt(1000000)+1;
        	int fun_fact_key = roll % ffMap.size();
        	channel.sendMessage("Did you know?\n\n" + ffMap.get(fun_fact_key)).queue();
        }
        else if (msg.equals("!roll"))
        {
            //In this case, we have an example showing how to use the Success consumer for a RestAction. The Success consumer
            // will provide you with the object that results after you execute your RestAction. As a note, not all RestActions
            // have object returns and will instead have Void returns. You can still use the success consumer to determine when
            // the action has been completed!

            Random rand = new Random();
            int roll = rand.nextInt(6) + 1; //This results in 1 - 6 (instead of 0 - 5)
            channel.sendMessage("Your roll: " + roll).queue(sentMessage ->  //This is called a lambda statement. If you don't know
            {                                                               // what they are or how they work, try google!
                if (roll < 3)
                {
                    channel.sendMessage("The roll for messageId: " + sentMessage.getId() + " wasn't very good... Must be bad luck!\n").queue();
                }
            });
        }
        else if (command[0].equals("~play") && command.length == 2)
        {
            loadAndPlay(event.getTextChannel(), command[1]);
        }
        else if (msg.equals("~skip"))
        {
        	skipTrack(event.getTextChannel());
        }
        else if (msg.equals("!newuser"))
        {
        	String user_create = "INSERT INTO users(username) VALUES('" + author.getName() + "#" + author.getId() + "');";
        	
        	try (Connection conn = DriverManager.getConnection(url);
                    Statement stmt = conn.createStatement()) 
        	{
                stmt.execute(user_create);
            } catch (SQLException e) {
                System.out.println(e.getMessage());
                channel.sendMessage("もう、先輩のアカウントがありました。。。 D:").queue();
            }
        }
        else if (msg.equals("!drop"))
        {
        	/*
        	// SQL statement for creating a new table
            String delete = "DELETE FROM users;";
                    
            try (Connection conn = DriverManager.getConnection(url);
                    Statement stmt = conn.createStatement()) {
                // create a new table
                stmt.execute(delete);
                conn.commit();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
            */
        	
        	/*
        	// SQL statement for creating a new table
            String drop = "DROP TABLE users;";
                    
            try (Connection conn = DriverManager.getConnection(url);
                    Statement stmt = conn.createStatement()) {
                // create a new table
                stmt.execute(drop);
                conn.commit();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
            
            String vacuum = "DROP TABLE users;";
            
            try (Connection conn = DriverManager.getConnection(url);
                    Statement stmt = conn.createStatement()) {
                // create a new table
                stmt.execute(drop);
                conn.commit();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
            */
        }
        else if (command[0].equals("!insert") && command.length == 2 && author.getName().equals("Capt_nikku"))
        {		
            String ins1 = "INSERT INTO vocabulary (vocab,kanji1,kanji2,kanji3,kana,type,definition,JLPT,romaji) VALUES("
            + command[1] + ");";
            
            System.out.println(ins1);
            try (Connection conn = DriverManager.getConnection(url);
                    Statement stmt = conn.createStatement()) 
            {
                    // create a new table
                    stmt.execute(ins1);
                    channel.sendMessage("Successful insert").queue();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
                channel.sendMessage("Failed, pls voice concerns to Michael").queue();
            }
            
        }
        else if (msg.equals("!select") && author.getName().equals("Cherisle"))
        {
        	/*String kanjis = "SELECT * FROM kanjis;";
        	try (Connection conn = DriverManager.getConnection(url);
                    Statement stmt  = conn.createStatement();
                    ResultSet rs    = stmt.executeQuery(kanjis)){
                   //loop through the result set
                   while (rs.next()) 
                   {
                       //user exists in db
                	   System.out.println(rs.getString("kanji") +  "\n" + 
                               rs.getString("readings") + "\n" +
                               rs.getString("meanings") + "\n" +
                               rs.getString("examples") + "\n" +
                       	       rs.getString("JLPT") + "\n");

                   }
               } catch (SQLException e) {
                   System.out.println(e.getMessage());
               }*/
        	
        	String vocab_sql = "SELECT * FROM vocabulary;";
        	try (Connection conn = DriverManager.getConnection(url);
                    Statement stmt  = conn.createStatement();
                    ResultSet rs    = stmt.executeQuery(vocab_sql)){
                   
        		//loop through the result set
                while (rs.next()) 
                {
             	   System.out.println(rs.getString("vocab") +  "\n" + 
                            rs.getString("kanji1") + "\n" +
                            rs.getString("kanji2") + "\n" +
                            rs.getString("kanji3") + "\n" +
                            rs.getString("kana") + "\n" +
                            rs.getString("type") + "\n" +
                            rs.getString("definition") + "\n" +
                    	    rs.getString("JLPT") + "\n" + 
             	   			rs.getString("romaji") + "\n");
                }
               } catch (SQLException e) {
                   System.out.println(e.getMessage());
               }
        	
        	/*
        	String kanji_table_size = "SELECT count(1) as cnt FROM kanjis;";
        	try (Connection conn = DriverManager.getConnection(url);
                    Statement stmt  = conn.createStatement();
                    ResultSet rs    = stmt.executeQuery(kanji_table_size)){
                   
                   //loop through the result set
                   if (rs.next()) 
                   {
                       //user exists in db
                	   System.out.println(rs.getString("cnt"));
                   }
               } catch (SQLException e) {
                   System.out.println(e.getMessage());
               }*/
        }
        else if(msg.equals("!help"))
        {
        	String help_msg = "Hey! Here are some of my programmed commands:\n"
        			+ "!kanji ~ Displays the kanji of the day\n"
        			+ "!newuser ~ Adds your user account into my database. Remove feature pending."
        			+ "!login ~ Access your account under my database\n"
        			+ "!logout ~ Log out of your account\n"
        			+ "!lookup [kanji symbol] ~ Look up a kanji I have stored in my database\n"
        			+ "!quiz ~ (Requires login) Activates quiz-mode for your account\n"
        			+ "!roll ~ Test your luck! Returns a number from a fair die (1-6)\n"
        			+ "~play [youtube-link] ~ I'll play your link in the first available voice channel! If I'm already playing, I will queue this link :)\n"
        			+ "~skip ~ I'll skip the current song and move to the next song in the queue, if it exists!";
        	channel.sendMessage(help_msg).queue();
        }
        else if(msg.equals("!quiz"))
        {
        	String login_attempt = "SELECT username FROM users WHERE username = '" + author.getName() + "#" + author.getId() + "'";
        	try (Connection conn = DriverManager.getConnection(url);
                    Statement stmt  = conn.createStatement();
                    ResultSet rs    = stmt.executeQuery(login_attempt)){
                   if (rs.next()) 
                   {
                       //user exists in db
                	   if(!players.isEmpty())
                	   {
                		   for(int ii=0;ii<players.size();ii++)
                		   {
                			   if(players.get(ii).getActive() == true && players.get(ii).getName().equals(author.getName()) && players.get(ii).getId().equals(author.getId()))
                			   {
                				   players.get(ii).setInQuiz(true,0,channel);
                				   channel.sendMessage("Which quiz would you like?\n1.N1\n2.N2\n3.N3\n4.N4\n5.N5").queue();
                				   channel.sendMessage("Temporarily, your next input will be disregarded and N5 auto-selected, sorry for the inconvenience :(").queue();
                				   break;
                			   }
                		   }
                	   }
                   }
                   else
                   {
                	   //user DNE
                	   channel.sendMessage("Your account is not set up. You may do this by typing '!newuser'").queue();
                   }
               } catch (SQLException e) { System.out.println(e.getMessage()); }
        }
        else if (msg.equals("!login"))
        {	
        	String login_attempt = "SELECT username FROM users WHERE username = '" + author.getName() + "#" + author.getId() + "'";
        	try (Connection conn = DriverManager.getConnection(url);
                    Statement stmt  = conn.createStatement();
                    ResultSet rs    = stmt.executeQuery(login_attempt)){
                   
                   //loop through the result set
                   if (rs.next()) 
                   {
                       //user exists in db
                	   if(!players.isEmpty())
                	   {
                		   for(int ii=0;ii<players.size();ii++)
                		   {
                			   if(players.get(ii).getActive() == true && players.get(ii).getName().equals(author.getName()) && players.get(ii).getId().equals(author.getId()))
                			   {
                				   String reject_msg = author.getName() + "#" + author.getId().substring(0,4) + ", you are already logged in.";
                				   channel.sendMessage(reject_msg).queue();
                				   break;
                			   }
                			   else if(players.get(ii).getActive() == false && players.get(ii).getName().equals(author.getName()) && players.get(ii).getId().equals(author.getId()))
                			   {
                				   String relog_msg = author.getName() + "#" + author.getId().substring(0,4) + ", you are logged back in. Welcome back!";
                				   channel.sendMessage(relog_msg).queue();
                				   players.get(ii).setActive(true);
                				   BattleAccount ba_store = players.get(ii).getBattleAccount();
                				   String name_store = players.get(ii).getName();
                				   String id_store = players.get(ii).getId(); 
                				   ba_store.DeserializeAccount(name_store,id_store);
                				   break;
                			   }
                			   else if(ii == players.size()-1) //last player in loop
                			   {
                				   if(!players.get(ii).getName().equals(author.getName()) && !players.get(ii).getId().equals(author.getId()))
                				   {
                					   players.add(new Player(author.getName(),author.getId(),true));
                					   String login_msg = "Fresh login for " + author.getName() + "#" + author.getId().substring(0,4) + " successful";
                                	   channel.sendMessage(login_msg).queue();
                                	   break;
                				   }
                			   }
                		   }
                	   }
                	   else // no player created yet
                	   {
                		   players.add(new Player(author.getName(),author.getId(),true));
                		   String login_msg = "Fresh login for " + author.getName() + "#" + author.getId().substring(0,4) + " successful";
                    	   channel.sendMessage(login_msg).queue();
                	   }
                   }
                   else
                   {
                	   //user DNE
                	   channel.sendMessage("Your account is not set up. You may do this by typing '!newuser'").queue();
                   }
               } catch (SQLException e) { System.out.println(e.getMessage()); }
        }
        else if (msg.equals("!logout"))
        {	
        	String login_attempt = "SELECT username FROM users WHERE username = '" + author.getName() + "#" + author.getId() + "'";
        	try (Connection conn = DriverManager.getConnection(url);
                    Statement stmt  = conn.createStatement();
                    ResultSet rs    = stmt.executeQuery(login_attempt)){
                   if (rs.next()) 
                   {
                       //user exists in db
                	   if(!players.isEmpty())
                	   {
                		   for(int ii=0;ii<players.size();ii++)
                		   {
                			   if(players.get(ii).getName().equals(author.getName()) && players.get(ii).getId().equals(author.getId()) && players.get(ii).getActive() == true)
                			   {
                				   players.get(ii).setActive(false);
                				   players.get(ii).setInQuiz(false,0,channel);
                				   players.get(ii).getBattleAccount().SerializeAccount(players.get(ii).getBattleAccount().getFileName(),true);
                				   String logout_msg = author.getName() + "#" + author.getId() + " - Logout successful!";
                				   channel.sendMessage(logout_msg).queue();
                				   break;
                			   }
                			   else if(players.get(ii).getName().equals(author.getName()) && players.get(ii).getId().equals(author.getId()) && players.get(ii).getActive() == false)
                			   {
                				   String reject_logout = author.getName() + "#" + author.getId() + ", you are already logged out.";
                				   channel.sendMessage(reject_logout).queue();
                				   break;
                			   }
                		   }
                	   }
                   }
                   else
                   {
                	   //user DNE
                	   channel.sendMessage("Your account is not set up. You may do this by typing '!newuser'").queue();
                   }
               } catch (SQLException e) { System.out.println(e.getMessage()); }
        }
        else if (msg.startsWith("!kick"))   //Note, I used "startsWith, not equals.
        {
            //This is an admin command. That means that it requires specific permissions to use it, in this case
            // it needs Permission.KICK_MEMBERS. We will have a check before we attempt to kick members to see
            // if the logged in account actually has the permission, but considering something could change after our
            // check we should also take into account the possibility that we don't have permission anymore, thus Discord
            // response with a permission failure!
            //We will use the error consumer, the second parameter in queue!

            //We only want to deal with message sent in a Guild.
            if (message.isFromType(ChannelType.TEXT))
            {
                //If no users are provided, we can't kick anyone!
                if (message.getMentionedUsers().isEmpty())
                {
                    channel.sendMessage("You must mention 1 or more Users to be kicked!").queue();
                }
                else
                {
                    Guild guild = event.getGuild();
                    Member selfMember = guild.getSelfMember();  //This is the currently logged in account's Member object.
                                                                // Very similar to JDA#getSelfUser()!

                    //Now, we the the logged in account doesn't have permission to kick members.. well.. we can't kick!
                    if (!selfMember.hasPermission(Permission.KICK_MEMBERS))
                    {
                        channel.sendMessage("Sorry! I don't have permission to kick members in this Guild!").queue();
                        return; //We jump out of the method instead of using cascading if/else
                    }

                    //Loop over all mentioned users, kicking them one at a time. Mwauahahah!
                    List<User> mentionedUsers = message.getMentionedUsers();
                    for (User user : mentionedUsers)
                    {
                        Member member = guild.getMember(user);  //We get the member object for each mentioned user to kick them!
                        
                        //We need to make sure that we can interact with them. Interacting with a Member means you are higher
                        // in the Role hierarchy than they are. Remember, NO ONE is above the Guild's Owner. (Guild#getOwner())
                        if (!selfMember.canInteract(member))
                        {
                            // use the MessageAction to construct the content in StringBuilder syntax using append calls
                            channel.sendMessage("Cannot kick member: ")
                                   .append(member.getEffectiveName())
                                   .append(", they are higher in the hierarchy than I am!")
                                   .queue();
                            continue;   //Continue to the next mentioned user to be kicked.
                        }

                        //Remember, due to the fact that we're using queue we will never have to deal with RateLimits.
                        // JDA will do it all for you so long as you are using queue!
                        guild.getController().kick(member).queue(
                            success -> channel.sendMessage("Kicked ").append(member.getEffectiveName()).append("! Cya!").queue(),
                            error ->
                            {
                                //The failure consumer provides a throwable. In this case we want to check for a PermissionException.
                                if (error instanceof PermissionException)
                                {
                                    PermissionException pe = (PermissionException) error;
                                    Permission missingPermission = pe.getPermission();  //If you want to know exactly what permission is missing, this is how.
                                                                                        //Note: some PermissionExceptions have no permission provided, only an error message!

                                    channel.sendMessage("PermissionError kicking [")
                                           .append(member.getEffectiveName()).append("]: ")
                                           .append(error.getMessage()).queue();
                                }
                                else
                                {
                                    channel.sendMessage("Unknown error while kicking [")
                                           .append(member.getEffectiveName())
                                           .append("]: <").append(error.getClass().getSimpleName()).append(">: ")
                                           .append(error.getMessage()).queue();
                                }
                            });
                    }
                }
            }
            else
            {
                channel.sendMessage("This is a Guild-Only command!").queue();
            }
        }
        else if (msg.equals("!block"))
        {
            //This is an example of how to use the complete() method on RestAction. The complete method acts similarly to how
            // JDABuilder's awaitReady() works, it waits until the request has been sent before continuing execution.
            //Most developers probably wont need this and can just use queue. If you use complete, JDA will still handle ratelimit
            // control, however if shouldQueue is false it won't queue the Request to be sent after the ratelimit retry after time is past. It
            // will instead fire a RateLimitException!
            //One of the major advantages of complete() is that it returns the object that queue's success consumer would have,
            // but it does it in the same execution context as when the request was made. This may be important for most developers,
            // but, honestly, queue is most likely what developers will want to use as it is faster.

            try
            {
                //Note the fact that complete returns the Message object!
                //The complete() overload queues the Message for execution and will return when the message was sent
                //It does handle rate limits automatically
                Message sentMessage = channel.sendMessage("I blocked and will return the message!").complete();
                //This should only be used if you are expecting to handle rate limits yourself
                //The completion will not succeed if a rate limit is breached and throw a RateLimitException
                Message sentRatelimitMessage = channel.sendMessage("I expect rate limitation and know how to handle it!").complete(false);

                System.out.println("Sent a message using blocking! Luckly I didn't get Ratelimited... MessageId: " + sentMessage.getId());
            }
            catch (RateLimitedException e)
            {
                System.out.println("Whoops! Got ratelimited when attempting to use a .complete() on a RestAction! RetryAfter: " + e.getRetryAfter());
            }
            //Note that RateLimitException is the only checked-exception thrown by .complete()
            catch (RuntimeException e)
            {
                System.out.println("Unfortunately something went wrong when we tried to send the Message and .complete() threw an Exception.");
                e.printStackTrace();
            }
        }
    }
	
	private void displayKanji(int kanji_key, MessageChannel channel, boolean lookup)
	{
		LocalDate date = LocalDate.now();
		
		String retrieve_kanji = "SELECT * FROM kanjis WHERE id = " + kanji_key + ";";
		String kanji = "";
		String readings = "";
		String meanings = "";
		String[] vocab = null;
		String[] furiganas = null;
		String[] romajis = null;
		String[] types = null;
		String[] definitions = null;
		int vocab_cnt = 0;
    	try (Connection conn = DriverManager.getConnection(url);
                Statement stmt  = conn.createStatement();
                ResultSet rs    = stmt.executeQuery(retrieve_kanji))
    	{       
    		if (rs.next()) // kanjis table
	        {
    			kanji = rs.getString("kanji");
	            readings = rs.getString("readings");
	            meanings = rs.getString("meanings");
	        }
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    	
    	String retrieve_ex_count = "SELECT count(1) as count FROM vocabulary WHERE (kanji1 = '" + kanji + "' OR kanji2 = '" + kanji + "') OR kanji3 = '" + kanji + "';";
		String retrieve_examples = "SELECT * FROM vocabulary WHERE (kanji1 = '" + kanji + "' OR kanji2 = '" + kanji + "') OR kanji3 = '" + kanji + "';";
    	
    	try
    	{       
    		Connection conn = DriverManager.getConnection(url);
    		Statement stmt  = conn.createStatement();
    		Statement stmt2  = conn.createStatement();

    		ResultSet rs_count = stmt.executeQuery(retrieve_ex_count);
    		if(rs_count.next())
    		{
    			vocab_cnt = rs_count.getInt("count");
    			//-----------------------------------
    			vocab = new String[vocab_cnt];
    			furiganas = new String[vocab_cnt];
    			romajis = new String[vocab_cnt];
    			types = new String[vocab_cnt];
    			definitions = new String[vocab_cnt];
    			int iterator = 0;
    			ResultSet rs = stmt2.executeQuery(retrieve_examples);
    			while (rs.next()) // vocabulary table
    	        {
        			vocab[iterator] = rs.getString("vocab");
        			furiganas[iterator] = rs.getString("kana");
        			romajis[iterator] = rs.getString("romaji");
    	            types[iterator] = rs.getString("type");
    	            definitions[iterator] = rs.getString("definition");
    	            iterator++;
    	        }
    		}
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    	
    	String display_message = "";
    	if(lookup == true)
    	{
    		display_message = "Today's date: " + date.toString() + "\n\nLookup Kanji: " + kanji;
    	}
    	else // lookup == 0
    	{
    		display_message = "Today's date: " + date.toString() + "\n\nKanji of the day: " + kanji;
    	}
    	
    	System.out.println(vocab_cnt);
    	display_message = display_message + "\nReadings: " + readings;
    	display_message = display_message + "\nMeaning: " + meanings + "\n\n";
    	display_message = display_message + "Examples:\n";
    	for(int ii=0;ii<vocab_cnt;ii++)
    	{
			display_message = display_message + "Word: " + vocab[ii] + "\n";
			display_message = display_message + "Furigana: " + furiganas[ii] + ", Romaji: " + romajis[ii] + "\n";
			display_message = display_message + "Type: " + types[ii] + "\n";
			display_message = display_message + "Definition: " + definitions[ii] + "\n\n";
    	}
    	channel.sendMessage(display_message).queue();
	}
	
	private void loadAndPlay(final TextChannel channel, final String trackUrl)
	{
	    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
	   
	    playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler()
	    {
	      @Override
	      public void trackLoaded(AudioTrack track) 
	      {
	        channel.sendMessage("Adding to queue " + track.getInfo().title).queue();

	        play(channel.getGuild(), musicManager, track);
	        }

	      @Override
	      public void playlistLoaded(AudioPlaylist playlist)
	      {
	        AudioTrack firstTrack = playlist.getSelectedTrack();

	        if (firstTrack == null) 
	        {
	          firstTrack = playlist.getTracks().get(0);
	        }

	        channel.sendMessage("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();

	        play(channel.getGuild(), musicManager, firstTrack);
	      }

	      @Override
	      public void noMatches()
	      {
	        channel.sendMessage("Nothing found by " + trackUrl).queue();
	      }

	      @Override
	      public void loadFailed(FriendlyException exception) 
	      {
	        channel.sendMessage("Could not play: " + exception.getMessage()).queue();
	      }
	    });
	}

	private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track)
	{
		connectToFirstVoiceChannel(guild.getAudioManager());
		if(musicManager.player.getVolume() != 20)
		{
			musicManager.player.setVolume(20);
		}

		musicManager.scheduler.queue(track);
	}

	private void skipTrack(TextChannel channel)
	{
	    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
	    musicManager.scheduler.nextTrack();

	    channel.sendMessage("Skipped to next track.").queue();
	}

	private static void connectToFirstVoiceChannel(AudioManager audioManager)
	{
		if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect())
	    {
			for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels())
			{
				if(voiceChannel.getName().equals("Helena's UFO"))
				{
					audioManager.openAudioConnection(voiceChannel);
					break;
				}
			}
	    }
	}
	
	public static int[] RandomizeArray(int a, int b)
	{
		Random rgen = new Random();  // Random number generator		
		int size = b-a+1;
		int[] array = new int[size];
 
		for(int i=0; i< size; i++){
			array[i] = a+i;
		}
 
		for (int i=0; i<array.length; i++) {
		    int randomPosition = rgen.nextInt(array.length);
		    int temp = array[i];
		    array[i] = array[randomPosition];
		    array[randomPosition] = temp;
		}
 
		for(int s: array)
			System.out.println(s);
 
		return array;
	}
	
	/**
     * Connect to a sample database
     *
     * @param fileName the database file name
     */
    public static void createNewDatabase(String fileName) 
    {
 
        String url = "jdbc:sqlite:C:/sqlite/db/" + fileName;
 
        try (Connection conn = DriverManager.getConnection(url)) 
        {
            if (conn != null) 
            {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("A new database has been created.");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    
    public static int getTotalKanjiCount(String url, String JLPT)
    {
    	String kanji_table_size = "";
    	//Check and make sure JLPT is a correct input before continuing
    	if(JLPT.equals("5") || JLPT.equals("N5"))
    	{
    		kanji_table_size = "SELECT count(1) as cnt FROM kanjis WHERE JLPT='N5';";
    	}
    	else if(JLPT.equals("4") || JLPT.equals("N4"))
    	{
    		kanji_table_size = "SELECT count(1) as cnt FROM kanjis WHERE JLPT='N4';";
    	}
    	else if(JLPT.equals("3") || JLPT.equals("N3"))
    	{
    		kanji_table_size = "SELECT count(1) as cnt FROM kanjis WHERE JLPT='N3';";
    	}
    	else if(JLPT.equals("2") || JLPT.equals("N2"))
    	{
    		kanji_table_size = "SELECT count(1) as cnt FROM kanjis WHERE JLPT='N2';";
    	}
    	else if(JLPT.equals("1") || JLPT.equals("N1"))
    	{
    		kanji_table_size = "SELECT count(1) as cnt FROM kanjis WHERE JLPT='N1';";
    	}
    	int quiz_maxIndex = 0; //init
    	try (Connection conn = DriverManager.getConnection(url);
                Statement stmt  = conn.createStatement();
                ResultSet rs    = stmt.executeQuery(kanji_table_size))
    	{       
        	if (rs.next()) 
        	{
        		quiz_maxIndex = rs.getInt("cnt");
        	}
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    	return quiz_maxIndex;
    }
}

