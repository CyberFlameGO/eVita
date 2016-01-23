package me.wiefferink.evita;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Data2Weka {

	public static final String SOURCE = "C:\\Coding\\DataScience\\eVita\\data\\Logdata DM tot 20-NOV-15 - Data Science.txt";
	public static final String TARGET = "C:\\Coding\\DataScience\\eVita\\data\\data.arff";

	private Map<String, SortedSet<Action>> actions;
	private Map<String, SortedSet<Session>> sessions;

	private SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm");

	public Data2Weka() {
		actions = loadData(SOURCE);
		Map<Integer, Long> codeCounts = countsPerCode(actions);

		sessions = createSessions(actions);
		printWekaFile(sessions);
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
		//progress("  Actions result: "+result.toString());
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
				if(action.date.getTimeInMillis() < lastTime) { // Sanity check
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
					sessionActions.add(action);
				}
				lastTime = action.date.getTimeInMillis();
			}
			// Handle last session
			if (sessionActions.size() > 0) {
				sessions.add(new Session(id, sessionActions));
			}
			result.put(id, sessions);
		}
		//progress("  Sessions result: " + result.toString());
		return result;
	}

	public Map<Integer, Long> countsPerCode(Map<String, SortedSet<Action>> actions) {
		progress("Printing code counts");
		// Count
		Map<Integer, Long> codeCounts = new TreeMap<>();
		for(SortedSet<Action> personActions : actions.values()) {
			for(Action action : personActions) {
				Long value = codeCounts.get(action.code);
				if(value == null) {
					value = 0L;
				}
				value++;
				codeCounts.put(action.code, value);
			}
		}
		return codeCounts;
	}

	/**
	 * Print the features and classes to a Weka file format
	 * @param data The data to print
	 */
	public void printWekaFile(Map<String, SortedSet<Session>> data) {
		List<Feature> features = new ArrayList<>();
		// All code features
		List<Integer> codes = Arrays.asList(10, 21, 22, 30, 31, 33, 34, 35, 40, 50, 51, 52, 53, 56, 70, 71, 90, 91);
		for(Integer code : codes) {
			features.add(new CodeFrequencyFirstSessionFeature(code));
		}
		// ActionCount feature
		features.add(new ActionsFirstSessionFeature());
		// Days between first and second login
		features.add(new FirstSecondLoginTimeFeature());
		// Days between second and third login
		features.add(new SecondThirdLoginTimeFeature());

		File fileTarget = new File(TARGET);
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(fileTarget))) {
			// Write headers
			writer.write("@RELATION diabetis\n\n");
			writer.write("@ATTRIBUTE id STRING\n");
			for(Feature feature : features) {
				writer.write("@ATTRIBUTE "+feature.getWekaHeader()+"\n");
			}
			writer.write("@ATTRIBUTE class {AdherendMeasureCount,AdherendLogins,AdherendCodes,NotAdherend}\n\n");
			// Write data
			writer.write("@DATA\n");
			for(String id : data.keySet()) {
				writer.write(id+",");
				for(Feature feature : features) {
					writer.write(feature.calculate(data.get(id))+",");
				}
				writer.write(getAdherend(data.get(id))+"\n");
			}
		} catch (IOException e) {
			error("  Error while writing to file: "+fileTarget.getAbsolutePath());
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Check if a user is adherend by checking if he logged in four times in a year
	 * @param data The Session data
	 * @return true for adherend, otherwise false
	 */
	private String getAdherend(SortedSet<Session> data) {
		// Try logged in more as 4 times a year
		Calendar endTime = getDate("21-11-2015 00:00");
		long diff = endTime.getTimeInMillis()-data.first().actions.first().date.getTimeInMillis();
		long quarterYear = 7884000000L;
		int shouldLogin = (int)Math.ceil(diff/(double)quarterYear);
		if(data.size() >= shouldLogin) {
			return "AdherendLogins";
		}
		// Try codes
		for(Session session : data) {
			for(Action action : session.actions) {
				if(action.code == 52 || action.code == 91) { // personal goal, education module
					return "AdherendCodes";
				}
			}
		}
		// Measurements on 4 different days
		int count = 0;
		for(Session session : data) {
			for(Action action : session.actions) {
				if(action.code == 35) {
					count++;
					break; // Go to next session
				}
			}
		}
		if(count >= 4) {
			return "AdherendMeasureCount";
		}
		return "NotAdherend";
	}

	/**
	 * Abstract feature
	 * @param <Type> resulting type
	 */
	private abstract class Feature<Type> {
		public abstract Type calculate(SortedSet<Session> sessions);
		public abstract String getWekaHeader();
	}

	/**
	 * Number of actions of a certain type in the first session
	 */
	private class CodeFrequencyFirstSessionFeature extends Feature<Integer> {
		public int code;

		public CodeFrequencyFirstSessionFeature(int code) {
			this.code = code;
		}

		public Integer calculate(SortedSet<Session> sessions) {
			Integer result = 0;
			for(Action action : sessions.first().actions) {
				if(action.code == code) {
					result++;
				}
			}
			return result;
		}

		public String getWekaHeader() {
			return "code"+code+" NUMERIC";
		}
	}

	/**
	 * Number of actions in the first session
	 */
	private class ActionsFirstSessionFeature extends Feature<Integer> {
		public Integer calculate(SortedSet<Session> sessions) {
			return sessions.first().actions.size();
		}

		public String getWekaHeader() {
			return "actionCount NUMERIC";
		}
	}

	/**
	 * Time between first and second login in days
	 */
	private class FirstSecondLoginTimeFeature extends Feature<Integer> {
		public Integer calculate(SortedSet<Session> sessions) {
			Calendar first = null;
			for(Session session : sessions) {
				if(first == null) {
					first = session.actions.last().date;
				} else {
					return (int)((session.actions.first().date.getTimeInMillis() - first.getTimeInMillis()) / 86400000L);
				}
			}
			return -1;
		}

		public String getWekaHeader() {
			return "loginTimeFirstSecond NUMERIC";
		}
	}

	/**
	 * Time between first and second login in days
	 */
	private class SecondThirdLoginTimeFeature extends Feature<Integer> {
		public Integer calculate(SortedSet<Session> sessions) {
			Calendar first = null;
			boolean skip = true;
			for (Session session : sessions) {
				if(skip) {
					skip = false;
				} else if (first == null) {
					first = session.actions.last().date;
				} else {
					return (int) ((session.actions.first().date.getTimeInMillis() - first.getTimeInMillis()) / 86400000L);
				}
			}
			return -1;
		}

		public String getWekaHeader() {
			return "loginTimeSecondThird NUMERIC";
		}
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
	 * Get the Calendar object for a given date/time input
	 * @param input The date/time input
	 * @return The Calendar
	 */
	public Calendar getDate(String input) {
		try {
			Date date = format.parse(input);
			Calendar result = Calendar.getInstance();
			result.setTime(date);
			return result;
		} catch (ParseException ignore) {}
		return null;
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
