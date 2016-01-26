package me.wiefferink.evita;

import me.wiefferink.evita.classes.*;
import me.wiefferink.evita.features.ActionsFirstSessionFeature;
import me.wiefferink.evita.features.CodeFrequencyFirstSessionFeature;
import me.wiefferink.evita.features.FirstSecondLoginTimeFeature;
import me.wiefferink.evita.features.SecondThirdLoginTimeFeature;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Data2Weka {

	public static final String SOURCE = "C:\\Coding\\DataScience\\eVita\\data\\Logdata DM tot 20-NOV-15 - Data Science.txt";
	public static final String TARGET = "C:\\Coding\\DataScience\\eVita\\data\\data.arff";

	public static SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm");

	private Map<String, SortedSet<Action>> actions;
	private Map<String, SortedSet<Session>> sessions;

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
		// Setup features
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

		// Setup classes
		List<Class> classes = new ArrayList<>();
		// Add classes
		classes.add(new LoggedInFourTimesAYearClass());
		classes.add(new EducationModuleClass());
		classes.add(new PersonalGoalClass());
		classes.add(new MeasurementsClass());
		classes.add(new CombinedClass());

		File fileTarget = new File(TARGET);
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(fileTarget))) {
			// Write headers
			writer.write("@RELATION diabetis\n\n% FEATURES\n");
			for(Feature feature : features) {
				writer.write("@ATTRIBUTE "+feature.getWekaHeader()+"\n");
			}
			writer.write("\n% CLASSES\n");
			for (Class c : classes) {
				writer.write("@ATTRIBUTE " + c.getWekaHeader() + "\n");
			}
			writer.write("\n");

			// Write data
			writer.write("@DATA\n");
			for(String id : data.keySet()) {
				writer.write(features.get(0).calculate(data.get(id)) + "");
				for(int i=1; i<features.size(); i++) {
					writer.write("," + features.get(i).calculate(data.get(id)));
				}
				for (Class c : classes) {
					writer.write("," + c.determine(data.get(id)));
				}
				writer.write("\n");
			}
		} catch (IOException e) {
			error("  Error while writing to file: "+fileTarget.getAbsolutePath());
			e.printStackTrace(System.err);
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
	public static Calendar dateToCalendar(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar;
	}

	/**
	 * Get the Calendar object for a given date/time input
	 * @param input The date/time input
	 * @return The Calendar
	 */
	public static Calendar getDate(String input) {
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
