import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.ArrayList;

public class Player 
{
	private String name;
	private String id;
	private boolean quizSelectFlag;
	private boolean active;
	private boolean in_quiz;
	private int score;
	private Quiz my_quiz;
	private String[] quiz_answers;
	private int current_index; // index for quiz-taking
	private Random randomizer;
	
	public Player(String p_name, String p_id, boolean p_active)
	{
		name = p_name;
		id = p_id;
		active = p_active;
		in_quiz = false;
		score = 0;
		randomizer = new Random();
		current_index = 0; //init
		quizSelectFlag = false;
	}
	
	public void setActive(boolean b)
	{
		active = b;
	}
	
	public void setCurrentIndex(int index)
	{
		current_index = index;
	}
	
	public void setInQuiz(boolean b, int quiz_complete, MessageChannel c)
	{
		in_quiz = b;
		if(b == false)
		{
			if(quiz_complete == 1)
			{
				quizSelectFlag = false; //reset the quiz select flag for new quiz
				my_quiz.checkAnswers(quiz_answers,c);
			}
			else
			{
				//logout
			}
		}
	}
	
	public void setQuizSelectDone()
	{
		quizSelectFlag = true;
	}
	
	public void assignAnswer(int index, String ans)
	{
		System.out.println("DEBUG LOG: " + (index-1));
		quiz_answers[index-1] = ans;
	}
	
	public void setAnswerSheet(int sheet_length)
	{
		quiz_answers = new String[sheet_length];
	}
	
	public boolean getInQuiz()
	{
		return in_quiz;
	}
	
	public boolean getQuizSelectStatus()
	{
		return quizSelectFlag;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getId()
	{
		return id;
	}
	
	public boolean getActive()
	{
		return active;
	}
	
	public int getScore()
	{
		return score;
	}
	
	public Quiz getQuiz()
	{
		return my_quiz;
	}
	
	public int getCurrentIndex()
	{
		return current_index;
	}
	
	public void assignQuiz(int kanji_count, String url, String JLPT)
	{
		System.out.println(kanji_count);
		String[] keys = new String[kanji_count];
		for(int ii=0;ii<kanji_count;ii++)
		{
			keys[ii] = String.valueOf(ii+1);
			System.out.println(keys[ii]);
		}
		String[] shuffled = shuffle(keys);
		
		System.out.println();
		for(int ii=0;ii<shuffled.length;ii++)
		{
			System.out.println(shuffled[ii]);
		}
		System.out.println();
		
		if(my_quiz == null)
		{
			my_quiz = new Quiz(shuffled.length);
		}
		else
		{
			my_quiz = null; //reset
			my_quiz = new Quiz(shuffled.length);
		}
		my_quiz.create(shuffled,url,JLPT);
		my_quiz.printQnA(); //DEBUG
	}
	
	private String[] shuffle(String[] array) 
	{   // mix-up the array    
		for (int ii = array.length - 1; ii > 0; --ii) 
	    {
	        int jj = randomizer.nextInt(ii + 1);
	        String temp = array[ii];
	        array[ii] = array[jj];
	        array[jj] = temp;
	    }
		return array;
	}
	
	
}
