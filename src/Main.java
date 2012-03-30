import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

	/**
	 * TheSaloonBot is a telnet bot that searches for specific keywords located
	 * in triggers.txt and replaces those words with the entered string.
	 * 
	 * 
	 * Variables that must be set:
	 * 
	 * @key		    The BYOND account key that will be automatically connecting and parsing.
	 * @password	The telnet password associated with that BYOND key.
	 * @admin       The admin key allowed to manually send commands.
	 */
	
	private final static String SaloonIP = "99.235.169.66";		// server IP to look for when connecting to The Saloon.
	private final static int SaloonPort = 6667;
	
	private final static String REGEX_WORLD = ".+\\[1m\\] (\\[37;40m\\[j)?(\\[34m)?([^\\[]+)(.+): (\\[31m)?(.+)$";
	private final static String REGEX_PM = ".+\\[1m\\] (\\[37;40m\\[j)?(\\[34m|\\[31m)?([^\\[]+)(.+): (\\[31m)?(.+)$";
	
	private final static String key = "";          		// key to authenticate against.
	private final static String password = "";      	// telnet password
	private final static String admin = "";				// key to listen to for remote management.
	
	private static ArrayList<String[]> Triggers = new ArrayList<String[]>();
	private static ArrayList<String[]> Replacements = new ArrayList<String[]>();
	
	// To send console commands to the bot directory, PM it a message start with >

	private static String ParseSpeech(String ToParse, String UserSpeaking) {
		String OutputParse = ToParse;

		// for parsing string replacements found in replacements.txt
    	for (String[] row : Replacements) {
    		String ReplacementFind = row[0];
        	if(ToParse.contains(ReplacementFind)) {
        		OutputParse = OutputParse.replace(ReplacementFind, row[1]);
        	}
    	}
		
		OutputParse = OutputParse.replace("{UserSpeaking}", UserSpeaking);
		return OutputParse;
	}

	private static void LoadTriggers() {
		// Triggers found in triggers.txt, used for find / say.
		try {
			Triggers = new ArrayList<String[]>();
			FileInputStream fstream = new FileInputStream("triggers.txt");
			 DataInputStream in = new DataInputStream(fstream);
			 BufferedReader br = new BufferedReader(new InputStreamReader(in));
			 String strLine;
			 while ((strLine = br.readLine()) != null) {
				 Triggers.add(strLine.split(";"));
			 }
			 in.close();
		} catch (Exception e) {
			System.err.println(e);
			System.exit(0);
		}
	}

	private static void LoadReplacements() {
		// Replacements used for static strings.  Useful for { } Replacement.
		try {
			Replacements = new ArrayList<String[]>();
			FileInputStream fstream = new FileInputStream("replacements.txt");
			 DataInputStream in = new DataInputStream(fstream);
			 BufferedReader br = new BufferedReader(new InputStreamReader(in));
			 String strLine;
			 while ((strLine = br.readLine()) != null) {
				 Replacements.add(strLine.split(";"));
			 }
			 in.close();
		} catch (Exception e) {
			System.err.println(e);
			System.exit(0);
		}
	}
	
	private static String TypeOfMessage(String RawLine) {
        Pattern Patterns1 = Pattern.compile(REGEX_WORLD);
        Matcher Matches1 = Patterns1.matcher(RawLine);
        if(Matches1.matches()) {return "WORLD";}

        Pattern Patterns2 = Pattern.compile(REGEX_PM);
        Matcher Matches2 = Patterns2.matcher(RawLine);
        if(Matches2.matches()) {return "PM";}
        
        return "UNKNOWN";
	}
	
	
    public static void main(String[] args) throws IOException, InterruptedException {

        Socket pingSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        try {
        	// establishes a connection to The Saloon.
            pingSocket = new Socket(SaloonIP, SaloonPort);
            out = new PrintWriter(pingSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(pingSocket.getInputStream()));
        } catch (IOException e) {
            return;
        }

        // Logs into the Saloon through telnet.
        out.print("\n");
		out.flush();
        Thread.sleep(25);
        
        // Auths with The Saloon's telnet interface.
        out.println(String.format("auth %s %s", key, password));
		out.flush();

        LoadTriggers();
        LoadReplacements();
        
        while(true) {

            // Strips all non-printable characters out of the RawLine.
            String RawLine = in.readLine().replaceAll("\\p{C}", "");
            System.out.println(RawLine);

            String MsgType = TypeOfMessage(RawLine);
            
            if(MsgType == "PM") {
            	// Probably a private message.
                Pattern Patterns = Pattern.compile(REGEX_PM);
                Matcher Matches = Patterns.matcher(RawLine);
                
                if (Matches.matches()) {
	                String UserSpeaking = Matches.group(3).replace("@", "");

                	// In PMs there is additional characters at the end of the PM we need to parse out.
	                String UserSaid = Matches.group(6);
	                String UserSaidClean = UserSaid.substring(1, UserSaid.length() - 4);
	                
	            	// If they are saying a command, interpret it.
	            	if ( (UserSaid.startsWith(">") && (UserSpeaking.equalsIgnoreCase(admin))) ) {
	            		
		            	if (UserSaidClean.equalsIgnoreCase("refresh")) {
			            	// Forces refreshing of the lists.
		                    LoadTriggers();
		                    LoadReplacements();
		                    Thread.sleep(25);
		                    out.println("emote is feeling refreshed!");
		                    out.flush();
		            	}
		            	
		            	if (UserSaidClean.equalsIgnoreCase("quit")) {
			            	// Forces refreshing of the lists.
		                    out.println("emote waves goodbye.");
		                    out.flush();
		                    Thread.sleep(25);
		                    System.exit(0);
		            	} else {
		            		// Otherwise just sent it through as a regular command.
		            		out.println( ParseSpeech( UserSaidClean, UserSpeaking) );
		            		out.flush();
		            	}
	            	}

	            	
                }
            }
            
            if (MsgType == "WORLD") {
            	// A message sent to the whole chatroom.
                Pattern Patterns = Pattern.compile(REGEX_WORLD);
                Matcher Matches = Patterns.matcher(RawLine);
                
                if (Matches.matches()) {
                	String UserSpeaking = Matches.group(3).replace("@", "");
		            String UserSaid = Matches.group(6);
	                String UserSaidClean = UserSaid;
		        	
		            // No reason to talk to the other bot.
		            if (UserSpeaking == "Wench") {continue;}
		            
		        	// Look at all triggers and compare.
		        	if (UserSpeaking.toLowerCase() != key.toLowerCase()) {
		            	// If there is a trigger active, and it's not talking to itself.
		            	for (String[] row : Triggers) {
		            		String TriggerFind = row[0].toLowerCase();
		                	if(UserSaidClean.toLowerCase().contains(TriggerFind)) {
		                		out.println( ParseSpeech(row[1], UserSpeaking) );
		                		out.flush();
		                		break;
		                	}
		            	}
		        	}
	            }
            }

            
        }
    }
}
