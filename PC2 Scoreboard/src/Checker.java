import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

public class Checker
{
	@SuppressWarnings("unused")
	private String inputfilename, outputfilename, answerfilename, resultfilename;
	private String[]kevin;
	
	private Connection con;
	private Statement s;
	
	private static final String 
		noError = "NO ERROR", 
		contactStaff = "No - Other - Contact Staff", 
		YES = "accepted", 
		wrongAnswer="No - Wrong Answer", 
		runtimeError="No - Run-time Error";
		
	private static final String
		correctNoError = "Yes",
		correctError = "There was a peculiar error while processing your program. One of our contest staff <i>should</i> be able to manually remove this message and replace with your error. Unless there is not much time left, it would be advisable to move on and check back here in a few minutes.",
		incorrectNoError = "While the output was incorrect, no error was produced by this program.";
		//incorrectError = "(an error should be here)";
	
	private boolean check(File f1, File f2) throws Exception
	{
		//Thank you Kevin in 2009
				
		BufferedReader input1 = new BufferedReader(new FileReader(f1));
		BufferedReader input2 = new BufferedReader(new FileReader(f2));
		boolean good = true;
		
		String line1, line2;
		int index;
		while ((line1 = input1.readLine()) != null | (line2 = input2.readLine()) != null)
		{
			if (line1 == null) line1 = "";
			if (line2 == null) line2 = "";
			
			index = line1.length() - 1;
			while (index >= 0 && Character.isWhitespace(line1.charAt(index)))
				index--;
			line1 = line1.substring(0, index + 1);
				
			index = line2.length() - 1;
			while (index >= 0 && Character.isWhitespace(line2.charAt(index)))
				index--;
			line2 = line2.substring(0, index + 1);
				
			if (!line1.equals(line2))
				good = false;
		}
		
		input1.close();
		input2.close();
		
		return good;
	}
	
	public static void main(String ... kevin) throws Exception //kevin is a tribute to Kevin Y. Chen because he wrote the all-important checker method in 2009. ;-)
	{
		new Checker(kevin);
	}
	private Checker(String...kevin) throws Exception
	{
		this.kevin = kevin;
		try
		{
			
			//Something is wrong with PC^2 calling the Validator. You cannot test in an IDE such as JCreator or Eclipse.
			if(kevin.length < 4)
			{
				System.out.println("INVALID ARGUMENTS - " + java.util.Arrays.toString(kevin) + ". Note that you must use PC^2 to call the validator. You cannot use an IDE such as JCreator or Eclipse.");
				System.exit(2); //Random number that PC^2 records that could be used for debugging. 1 is the general standard for errors.
			}
			
			//Get file names from PC^2
			inputfilename = kevin[0];
			outputfilename = kevin[1];
			answerfilename = kevin[2];
			resultfilename = kevin[3];
			
			//Construct Java files from PC^2. They will all be in the same directory.			
			File outputfile = new File(outputfilename);
			File answerfile = new File(answerfilename);
			File resultfile = new File(resultfilename);
			
			//Check if the problem is correct, ignoring whitespace at the end of lines and empty lines.
			boolean correct = check(outputfile,answerfile);
			
			String error;
			
			/*
			 *IMPORTANT: It has been determined that a program which produces correct output (so the correct variable above is still true)
			 *but still produces an error should be marked specially and looked at by an administrator.
			 *Therefore, the correct boolean being true IS NOT cause to immediately log a YES for the user. 
			 *
			 */
			
			//Here's the error, or the constant noError (a java variable with an appropriate response for No Error)
			error = getError(new File("estderr.pc2")); //Note: This is an UNDOCUMENTED PC^2 file that it stores the stderr from the contestant's program in. There's also cstderr.pc2 and vstderr.pc2. I'm guessing c is the compiler and v is the validator.
			
			String response;
			String viewableResponse;
			
			if(error.equals(noError))
			{
				//There was NOT an error. The response should now be a YES (java variable)
				//(which is actually "accepted" according to the standard our validator must conform to)
				//or wrongAnswer (java variable) if the answer was wrong (as determined by the correct variable)
				
				if(correct)
				{
					response = YES;
					viewableResponse = correctNoError;
				}
				else
				{
					response = wrongAnswer;
					//Contestants shouldn't be shown anything other than the fact that there wasn't an error
					viewableResponse = incorrectNoError; 
				}
			}
			else
			{
				if(correct)
				{
					//This is that fourth scenario that shouldn't ever really happen. correct answer but produced an error. Administrator should be contacted.
					response = contactStaff;
					viewableResponse = correctError;
				}
				else
				{
					//Incorrect answer and an error was produced - This should be displayed to contestants
					response = runtimeError;
					viewableResponse = "Error: " + error;
				}
			}
							
						
			try
			{
				//Follows the standard to write the response
				BufferedWriter out = new BufferedWriter(new FileWriter(resultfile));
				out.write("<?xml version=\"1.0\"?>");
				out.newLine();
				out.write("<result outcome=\"" + response  + "\" security=\"" + resultfilename + "\"> " + response + " </result>");
				out.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
				if(writeDebug())
					dprintln("Error Writing Result File. Debug Information written to Debug Information.txt - Contact a contest administrator immediately!");
				else
					dprintln("Error Writing Result File. Debug Information COULD NOT be written to Debug Information.txt - Contact a contest administrator immediately!");
			}
			
			try
			{
				try
				{
					Class.forName("com.mysql.jdbc.Driver");
				}
				catch (ClassNotFoundException e)
				{
					e.printStackTrace();
				}
				
				
				String url = "jdbc:mysql://192.168.0.1:3306";

				con = DriverManager.getConnection(url,"validator","contest3");			
				s = con.createStatement();
				
				File folder = new File(".");
				
				File[]files = folder.listFiles();
				
				File javaFile = null;
				for(File a: files)
				{
					String name = a.getName();
					String ext = (name.contains(".") && name.length() >= name.indexOf(".") + 1 ?name.substring(name.indexOf(".") + 1):name);
					
					if(ext.toLowerCase().equals("java"))
					{
						javaFile = a;
						break;
					}
				}
				
				if(javaFile == null)
				{
					dprintln("NO JAVA FILE FOUND");
					throw new Exception("NO JAVA FILE FOUND");				
				}
				
				byte[] sourceFile = getBytesFromFile(javaFile);
				String md5 = null;
				
				
				try
				{
					MessageDigest algorithm = MessageDigest.getInstance("MD5");
					algorithm.reset();
					algorithm.update(sourceFile);
					byte messageDigest[] = algorithm.digest();
				            
					StringBuffer hexString = new StringBuffer();
					for (int i=0;i<messageDigest.length;i++)
					{
						hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
					}
					
					md5 = hexString.toString();
					
				}
				catch(NoSuchAlgorithmException e)
				{
					System.out.println("MD5 Error");
					writeDebug();
					e.printStackTrace();
					System.exit(1);
				}
				
				
				ResultSet r = s.executeQuery("SELECT * FROM errorsystem.errors WHERE md5='" + md5 + "'");
				
				if(!r.first())
				{
					s.executeUpdate("INSERT INTO errorsystem.errors (run, team, md5, error) VALUES ('0','0','" + md5 + "','" + viewableResponse.replace("'","\'") + "')");
					//System.err.println("INSERT INTO errorsystem.errors (md5, error) VALUES ('" + md5 + "','" + viewableResponse.replace("'","\'") + "')");
				}
				else
				{
					s.executeUpdate("UPDATE errorsystem.errors SET error='" + viewableResponse.replace("'","\'") + "' WHERE md5='" + md5 + "' LIMIT 1");
				}
				
				
				/*
				 * 
				 * if(SELECT * FROM errors WHERE md5=java md5)
				 * 	UPDATE errors SET error= java error WHERE md5=java md5 LIMIT 1;
				 * else
				 * 	INSERT INTO errors (md5, error) VALUES ('md5'),( 'error')
				 * 
				 * 
				 * 
				 * 
				 */
				
				con.close();
				
				
			}
			catch(Exception e)
			{
				e.printStackTrace();
				dprintln("VALIDATOR SQL EXCEPTION!");
				dprintln(Arrays.toString(e.getStackTrace()));
			}
			
			
			//Debug - Writes the response to a file
			//writeResponseToFile(response,viewableResponse);
			
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			if(writeDebug())
				dprintln("Debug Information Written to Debug Information.txt - Contact a contest administrator immediately!");
			else
				dprintln("Exception + Debug Exception. Debug File Not Written. - Contact a contest administrator immediately!");
		}
	}
	private byte[] getBytesFromFile(File file) throws Exception
	{
		InputStream is = new FileInputStream(file);
		long length = file.length();
		if(length > Integer.MAX_VALUE)
		{
			throw new Exception("File is too long");
		}
		
		byte [] bytes = new byte[(int)length];
		int offset = 0;
		int numRead = 0;
		while(offset < bytes.length && (numRead = is.read(bytes,offset, bytes.length-offset)) >= 0)
		{
			offset += numRead;
		}
		
		if(offset < bytes.length)
		{
			throw new IOException ("Could not completely read file " + file.getName());
		}
		
		is.close();
		return bytes;
	}
	@SuppressWarnings("unused")
	private void writeResponseToFile(String response, String viewableResponse)
	{
		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter("Responses.txt"));
			out.write("PC^2 Response: " + response);
			out.newLine();
			out.write("Contestant Viewable Response: " + viewableResponse);
			out.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	//Writes debug information. Useful.
	private boolean writeDebug()
	{
		boolean errorFile;
		try
		{
			BufferedWriter temp = new BufferedWriter(new FileWriter("Debug Information.txt"));
			temp.write(java.util.Arrays.toString(kevin));
			temp.newLine();
			temp.write("Team's Output File Name: " + outputfilename);
			temp.newLine();
			temp.write("Judge's Answer File Name: " + answerfilename);
			temp.newLine();
			temp.write("Validator Secret File Name + Security Code: " + resultfilename);
			temp.close();
			
			errorFile = writeErrorFile();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true && errorFile;
	
	}
	//Makes a copy of the error file produced by the contestant's program. Returns true if successful.
	private boolean writeErrorFile()
	{
		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter("Error File.txt"));
			BufferedReader temp = new BufferedReader(new FileReader("estderr.pc2"));
		
			while(temp.ready())
			{
				out.write(temp.readLine());
				if(temp.ready())
					out.newLine();
			}
		
		
			out.close();
			temp.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	private void dprintln(String line)
	{
		System.out.println(line);
		System.err.println(line);
	}
	
	//Returns the exact error produced, or the constant noError if there was no error but it was still an incorrect answer.
	private String getError(File output)
	{
		try
		{
			//Thank you Chirag!
			ArrayList<String> lines = new ArrayList<String>();
			BufferedReader in = new BufferedReader(new FileReader(output));
			String read = "";
			while((read = in.readLine()) != null)
				lines.add(read);
			
			in.close(); // <--- Chirag didn't close his BufferedReader. Bad Chirag. :D
			
			//Insert Matthew Code to check for all Empty Lines
			
			boolean allEmpty = true;
			for(int a =0; a<lines.size(); a++)
			{
				if(!lines.get(a).trim().equals(""))
				{
					allEmpty = false;
					break;
				}
			}
			
			if(allEmpty)
				return noError;
			
			//End Matthew Code
			
			
			ArrayList<Integer> tabbedIndexes = new ArrayList<Integer>();
			for(int i = 0; i < lines.size(); i++)
			{
				String line = lines.get(i);
				if(line.startsWith("\t"))
					if((line.contains("at ") && line.contains(".java:")) || (line.contains("... ") && line.contains("more")))
						tabbedIndexes.add(i);
			}
			
			int min = tabbedIndexes.get(0);
			int max = tabbedIndexes.get(tabbedIndexes.size() - 1);
			
			String ret = "";
			for(int i = min - 1; i <= max; i++)
				ret += lines.get(i) + ( (i+1)<=max?"\n":""); // <-- Chirag failed on this line because his program returned an extra line in the String
			
			return ret;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			if(writeDebug())
				dprintln("There was an error in determining the error produced by the contestant's program. Debug information was written to Debug Information.txt - Contact a contest administrator immediately!");
			else
				dprintln("There was an error in determining the error produced by the contestant's program. Debug information COULD NOT be written. - Contact a contest administrator immediately!");
		}
		return "Error in determining the error! Please alert someone from CHS and ask them to inform Matthew immediately!";
		
	}
}

