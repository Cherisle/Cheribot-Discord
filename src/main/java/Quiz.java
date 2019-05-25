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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Random;

import java.util.regex.*;

public class Quiz 
{
	private String[] questions;
	private String[] answers;
	private int total_questions;
	
	public Quiz(int q_length)
	{
		questions = new String[q_length];
		answers = new String[q_length];
		total_questions = q_length;
	}
	
	public void create(String[] shuffled_indexes, String url, String JLPT)
	{
		Random rand = new Random();
		for(int ii=0;ii<questions.length;ii++)
		{
			int shuff_index = Integer.parseInt(shuffled_indexes[ii]);
			String kanjis = "SELECT id AS kid,kanji,readings,meanings,JLPT FROM kanjis k WHERE JLPT = '" + JLPT + "';";
			
			//System.out.println("Query: " + kanjis + "\nShuffled Index: " + shuff_index);
			
	    	try (Connection conn = DriverManager.getConnection(url);
	                Statement stmt  = conn.createStatement();
	                ResultSet rs    = stmt.executeQuery(kanjis))
	    	{       
	    		int shuff_counter = 0;
	    		while (shuff_counter < shuff_index) 
	    		{
	    			if(shuff_counter == shuff_index-1)
	    			{
	    				int n = rand.nextInt(3) + 1; //range between 1-3
		    			//System.out.println("[Debug] Question type: " + n);
		    			switch(n)
		    			{
		    				case 1: // readings
		    					questions[ii] = "Type a reading associated with **" + rs.getString("kanji") + "** [Use ~ as prefix/suffix if intended]";
		    					System.out.println("Readings, Question" + ii);
		    					//System.out.println(rs.getString("readings"));
		    					String readings = rs.getString("readings");
		    					String[] r_tokens = readings.split("・");
		    					answers[ii] = "";
		    					for(int jj=0;jj<r_tokens.length;jj++)
		    					{
		    						if(jj == r_tokens.length -1)
		    						{
		    							answers[ii] = answers[ii] + r_tokens[jj];
		    						}
		    						else
		    						{
		    							answers[ii] = answers[ii] + r_tokens[jj] + "  ";
		    						}
		    						//System.out.print(r_tokens[jj] + "  ");
		    					}
		    					answers[ii] = answers[ii].replaceAll("\\*", "");
		    					//System.out.println();
		    					break;
		    				case 2: // meanings
		    					questions[ii] = "Type a meaning associated with **" + rs.getString("kanji") + "**";
		    					System.out.println("Meanings, Question" + ii);
		    					//System.out.println(rs.getString("meanings"));
		    					String meanings = rs.getString("meanings");
		    					String[] m_tokens = meanings.split("; ");
		    					answers[ii] = "";
		    					for(int jj=0;jj<m_tokens.length;jj++)
		    					{
		    						if(jj == m_tokens.length -1)
		    						{
		    							answers[ii] = answers[ii] + m_tokens[jj];
		    						}
		    						else
		    						{
		    							answers[ii] = answers[ii] + m_tokens[jj] + "  ";
		    						}
		    						//System.out.print(m_tokens[jj] + "  ");
		    					}
		    					//System.out.println();
		    					break;
		    				case 3: // vocabulary
		    					String str_kanji = rs.getString("kanji");
		    					String vocab_cnt = "SELECT count(1) as cnt FROM vocabulary WHERE (JLPT='N5') AND (kanji1='" + str_kanji + 
		    					"' OR kanji2='" + str_kanji + "' OR kanji3='" + str_kanji + "');";
		    					ResultSet rs_vCount = stmt.executeQuery(vocab_cnt);
		    					System.out.println("Vocab, Question" + ii + ", Kanji: " + str_kanji);
		    					if(rs_vCount.next()) // if the query above returns value(s) at all
		    					{
		    						System.out.println("[DEBUG]: Entered associated vocabulary section");
		    						int count = rs_vCount.getInt("cnt");
		    						if(count != 0)
		    						{
		    							int random_vocab = rand.nextInt(count)+1;
		    							System.out.println("[Debug] Vocab Question Type: " + random_vocab);
			    						String kanji_vocab = "SELECT * FROM vocabulary WHERE (JLPT='N5') AND (kanji1='" + str_kanji + 
			    		    			"' OR kanji2='" + str_kanji + "' OR kanji3='" + str_kanji + "');";
			    						System.out.println("[Debug] Kanji Vocab: " + kanji_vocab);
			    		    			ResultSet rs_vocabInfo = stmt.executeQuery(kanji_vocab);
			    		    			int nn = 0;
				    					while(nn < random_vocab) // skip to the correct random vocab
				    					{
				    						if(nn == random_vocab -1)
				    						{
				    							int question_type = rand.nextInt(2) + 1;
				        						if(question_type == 1)
				        						{
				        							questions[ii] = "What is the furigana or romaji of **" + rs_vocabInfo.getString("vocab") + "** ?";
				    	    						answers[ii] = rs_vocabInfo.getString("kana") + "  " + rs_vocabInfo.getString("romaji");
				        						}
				        						else if(question_type == 2)
				        						{
				        							questions[ii] = "What is a meaning of **" + rs_vocabInfo.getString("vocab") + "** ?";
				        							answers[ii] = "";
				    								//System.out.println(questions[ii]);
				        							if(rs_vocabInfo.getString("definition").contains("; "))
				        							{
				        								String[] possible_meanings = rs_vocabInfo.getString("definition").split("; ");
					    								for(int mm=0;mm<possible_meanings.length;mm++)
					    								{
					    									if(mm == (possible_meanings.length - 1))
					    									{
					    										answers[ii] = answers[ii] + possible_meanings[mm];
					    									}
					    									else
					    									{
					    										answers[ii] = answers[ii] + possible_meanings[mm] + "  ";
					    									}
					    								}
				        							}
				        							else
				        							{
				        								answers[ii] = answers[ii] + rs_vocabInfo.getString("definition") + "  ";
				        							}
			    								}
			    								else
			    								{
			    									//System.out.println();
			    								}
				    						}
				    						nn++;
				    						rs_vocabInfo.next();
				    					}
		    						}
		    						else //count is 0
		    						{
			    						System.out.println("[DEBUG]: Entered random vocabulary section");
		    							String voc_count = "SELECT count(1) as cnt FROM vocabulary WHERE JLPT='N5';";
			    						ResultSet rs_vocCount = stmt.executeQuery(voc_count);
			    						int random_vocabIndex = rand.nextInt(rs_vocCount.getInt("cnt"));
			    						String select_vocab = "SELECT * FROM vocabulary WHERE JLPT='N5';";
			    		    			ResultSet rs_sel_vocab = stmt.executeQuery(select_vocab);
			    		    			int nn = 0;
				    					while(nn < random_vocabIndex) // skip to the correct random vocab
				    					{
				    						if(nn == random_vocabIndex -1)
				    						{
				    							int question_type = rand.nextInt(2) + 1;
				        						if(question_type == 1)
				        						{
				        							questions[ii] = "What is the furigana or romaji of **" + rs_sel_vocab.getString("vocab") + "** ?";
				    	    						answers[ii] = rs_sel_vocab.getString("kana") + "  " + rs_sel_vocab.getString("romaji");
				        						}
				        						else if(question_type == 2)
				        						{
				        							questions[ii] = "What is a meaning of **" + rs_sel_vocab.getString("vocab") + "** ?";
				        							answers[ii] = "";
				    								//System.out.println(questions[ii]);
				        							if(rs_sel_vocab.getString("definition").contains("; ")) //contains multiple definitions
				        							{
				        								String[] possible_meanings = rs_sel_vocab.getString("definition").split("; ");
					    								for(int mm=0;mm<possible_meanings.length;mm++)
					    								{
					    									if(mm == (possible_meanings.length - 1))
					    									{
					    										answers[ii] = answers[ii] + possible_meanings[mm];
					    									}
					    									else
					    									{
					    										answers[ii] = answers[ii] + possible_meanings[mm] + "  ";
					    									}
					    								}
				        							}
				        							else
				        							{
				        								answers[ii] = answers[ii] + rs_sel_vocab.getString("definition") + "  ";
				        							}
			    								}
			    								else
			    								{
			    									//System.out.println();
			    								}
				    						}
				    						
				    						nn++;
				    						rs_sel_vocab.next();
				    					}
		    						}	    						
		    					}
		    					
		    					/*
		    					String examples = rs.getString("examples");
		    					String[] ex_tokens = examples.split("/");
		    					Random r_token = new Random();
	    						int vocab_num = r_token.nextInt(ex_tokens.length-1);
	    						String[] ex_part = ex_tokens[vocab_num].split("-");
	    						int question_type = r_token.nextInt(2) + 1;
	    						if(question_type == 1)
	    						{
	    							questions[ii] = "What is the furigana or romaji of **" + ex_part[0] + "** ?";
		    						//System.out.println(questions[ii]);
		    						answers[ii] = ex_part[1] + "  " + ex_part[2];
		    						//System.out.println("Answers: " + ex_part[1] + "  " + ex_part[2]);
	    						}
	    						else if(question_type == 2)
	    						{
	    							questions[ii] = "What is a meaning of **" + ex_part[0] + "** ?";
									//System.out.println(questions[ii]);
									String[] ex_meanings = ex_part[3].split("; "); // still need to remove (type) e.g. noun, verb, adj in [0]
									int first_whitespace = ex_meanings[0].indexOf(" ");
									String first_meaning = ex_meanings[0].substring(first_whitespace+1);
									answers[ii] = first_meaning + "  ";
									//System.out.print("Answers: " + first_meaning);
									if(ex_meanings.length > 1)
									{
										//System.out.print("  ");
										for(int mm=1;mm<ex_meanings.length;mm++)
										{
											if(mm == ex_meanings.length -1)
											{
												answers[ii] = answers[ii] + ex_meanings[mm];
											}
											else
											{
												answers[ii] = answers[ii] + ex_meanings[mm] + "  ";
											}
											//System.out.print(ex_meanings[mm] + "  ");
										}
										//System.out.println();
									}
									else
									{
										//System.out.println();
									}
	    						}
	    						*/
	    						break;
		    			}
		    			break;
	    			}
	    			else
	    			{
	    				shuff_counter++;
	    			}
	    			rs.next();
	    		}
	        } catch (SQLException e) { System.out.println(e.getMessage()); }
			
			//questions[ii] = shuffled_indexes[ii]; //assign the shuffled order
			//answers[ii] = shuffled_indexes[ii]; //keep consistency in q/a
		}
		
	}
	
	public int getTotalQuestions()
	{
		return total_questions;
	}
	
	public void printQuestion(int index, MessageChannel c)
	{
		int q_number = index+1;
		c.sendMessage("Question " + q_number + ": " +questions[index]).queue();
	}
	
	public int checkAnswers(String[] response, MessageChannel c)
	{
		int correct = 0;
		ArrayList<Integer> correct_indexes = new ArrayList<Integer>();
		for(int ii=0;ii<questions.length;ii++)
		{
			String response_correction = "";
			if(response[ii].matches("[a-zA-Z0-9]+"))
			{
				response_correction = response[ii].toLowerCase();
			}
			else
			{
				response_correction = response[ii];
			}
			//----------------------------------------------------
			String[] answer_array = null;
			if(answers[ii].contains("  "))
			{
				answer_array = answers[ii].split("  ");
			}
			else
			{
				answer_array = answers[ii].split(" ");
			}
			//----------------------------------------------------
			for(int aa=0; aa<answer_array.length; aa++)
			{
				if(answer_array[aa].equals(response_correction))
				{
					correct += 1;
					correct_indexes.add(ii);
					break;
				}
			}
			System.out.println("Response: " + response[ii]);
			System.out.println("Answers: " + answers[ii]);
		}
		String final_result = "You got " + correct + " questions correct! Here's an overview:\n";
		String cheribit_msg = "";
		for(int ii=0;ii<questions.length;ii++)
		{
			int q_index = ii+1;
			final_result = final_result + "Question " + q_index + ": " + questions[ii] + "\n";
			if(correct_indexes.contains(ii))
			{
				final_result = final_result + "Your Answer: " + response[ii] + " // Point Awarded\n";
			}
			else
			{
				final_result = final_result + "Your Answer: " + response[ii] + "\n";
			}
			String answer_display = "";
			if(answers[ii].contains("  "))
			{
				answer_display = answers[ii].replaceAll("  "," ");
			}
			else
			{
				answer_display = answers[ii];
			}
			final_result = final_result + "Possible Answers: " + answer_display + "\n";
		}
		if(correct == 10)
		{
			correct = correct + 2;
			cheribit_msg = "Bonus! You have gained " + correct + "Cheribits!";
		}
		else
		{
			cheribit_msg = "You have gained " + correct + " Cheribits!";
		}
		c.sendMessage(final_result).queue();
		c.sendMessage(cheribit_msg).queue();
		return correct;
	}
	
	//for debug
	public void printQnA()
	{
		for(int ii=0;ii<questions.length;ii++)
		{
			System.out.println(ii);
			System.out.println(questions[ii]);
			System.out.println(answers[ii]);
		}
	}
}
