package me.wiefferink.evita;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Data2Weka {

	public static final String SOURCE_CHF = "C:\\Coding\\DataScience\\eVita\\data\\Logdata CHF tot 20-nov-15 - Data Science.txt";
	public static final String SOURCE_DM = "C:\\Coding\\DataScience\\eVita\\data\\Logdata DM tot 20-NOV-15 - Data Science.txt";

	private Map<String, SortedSet<Action>> chfActions;
	private Map<String, SortedSet<Session>> chfSessions;
	private Map<String, SortedSet<Action>> dmActions;
	private Map<String, SortedSet<Session>> dmSessions;

	private SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm");

	public Data2Weka() {
		chfActions = loadData(SOURCE_CHF);
		chfSessions = createSessions(chfActions);
		dmActions = loadData(SOURCE_DM);
		dmSessions = createSessions(dmActions);
	}

	/**
	 * Load data and get it into Actions per id
	 * @param source The file to load the data from
	 * @return A map from id to a sorted list of Actions
	 */
	public Map<String, SortedSet<Action>> loadData(String source) {
		progress("Loading data: "+source);
		Map<String, SortedSet<Action>> result = new HashMap<>();
		File file = new File(source);
		if(!file.isFile() || !file.exists()) {
			error("  Could not load file: "+file.getAbsolutePath());
			return result;
		}

		try(BufferedReader reader = new BufferedReader(new FileReader(source))) {
			String line = reader.readLine();
			boolean first = true;
			while(line != null) {
				if(first) { // Ignore file headers
					first = false;
					line = reader.readLine();
				}
				String[] parts = line.split("\t");
				if(parts.length < 3) {
					error("  Incorrect line: "+line);
				} else {
					Date date = null;
					try {
						date = format.parse(parts[1]);
					} catch (ParseException ignore) {}
					if(date == null) {
						error("  Incorrect date: "+parts[1]);
					} else {
						int code = -1;
						try {
							code = Integer.parseInt(parts[2]);
						} catch (NumberFormatException e) {
							error("  Incorrect code: "+parts[2]);
						}
						if(code != -1) {
							String information = "";
							if(parts.length >= 4) {
								information = parts[3];
							}
							SortedSet<Action> set = result.get(parts[0]);
							if(set == null) {
								set = new TreeSet<>();
								result.put(parts[0], set);
							}
							set.add(new Action(parts[0], date, code, information));
						}
					}
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			error("  Error while reading data file: "+file.getAbsolutePath());
			e.printStackTrace(System.err);
		}
		progress("  Actions result: "+result.toString());
		return result;
	}

	/**
	 * Bundle Actions into Sessions
	 * @param actions The actions of from the log file
	 * @return A map from id to a sorted list of sessions
	 */
	public Map<String, SortedSet<Session>> createSessions(Map<String, SortedSet<Action>> actions) {
		progress("Creating sessions");
		Map<String, SortedSet<Session>> result = new HashMap<>();
		for(String id : actions.keySet()) {
			long lastTime = 0;
			SortedSet<Session> sessions = new TreeSet<>();
			SortedSet<Action> sessionActions = new TreeSet<>();
			for(Action action : actions.get(id)) {
				if(action.date.getTimeInMillis() < lastTime) {
					error("  Sorting incorrect");
				}
				if(action.date.getTimeInMillis()-lastTime <= 1800000L) { // Half hour
					// Same session
					sessionActions.add(action);
				} else {
					// New session
					if(sessionActions.size() > 0) { // First one is empty
						sessions.add(new Session(id, sessionActions));
					}
					sessionActions = new TreeSet<>();
				}
				lastTime = action.date.getTimeInMillis();
			}
			// Handle last session
			if (sessionActions.size() > 0) {
				sessions.add(new Session(id, sessionActions));
			}
			result.put(id, sessions);
		}
		progress("  Sessions result: " + result.toString());
		return result;
	}


	/**
	 * Represents a row of the data log file
	 */
	private class Action implements Comparable<Action> {
		public String id;
		public Calendar date;
		public int code;
		public String information;

		public Action(String id, Calendar date, int code, String information) {
			this.id = id;
			this.date = date;
			this.code = code;
			this.information = information;
		}
		public Action(String id, Date date, int code, String information) {
			this(id, dateToCalendar(date), code, information);
		}

		@Override
		public String toString() {
			return "("+id+", "+format.format(date.getTime())+", "+code+", "+information+")";
		}

		/**
		 * Sort by date
		 */
		@Override
		public int compareTo(Action action) {
			return ((Long)date.getTimeInMillis()).compareTo(action.date.getTimeInMillis());
		}
	}

	private class Session implements Comparable<Session> {
		public String id;
		public SortedSet<Action> actions;

		public Session(String id, SortedSet<Action> actions) {
			this.id = id;
			this.actions = actions;
		}

		@Override
		public String toString() {
			return "(" + id + ", "+actions.toString()+")";
		}

		/**
		 * Sort by starting date
		 */
		@Override
		public int compareTo(Session session) {
			return ((Long) actions.first().date.getTimeInMillis()).compareTo(session.actions.first().date.getTimeInMillis());
		}
	}

	public static void main(String[] args) {
		new Data2Weka();
	}

	/**
	 * Wrap a date in a Calendar object
	 * @param date The date to wrap
	 * @return The Calendar
	 */
	public Calendar dateToCalendar(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar;
	}

	/**
	 * Print message to the standard output
	 *
	 * @param message The message to print
	 */
	public static void progress(String message) {
		System.out.println(message);
	}

	/**
	 * Print message to the error output
	 *
	 * @param message The message to print
	 */
	public static void error(String message) {
		System.err.println(message);
	}
}
