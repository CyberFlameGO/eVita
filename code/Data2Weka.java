package me.wiefferink.evita;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class Data2Weka {

	public static final String SOURCE_CHF = "C:\\Coding\\DataScience\\eVita\\data\\Logdata CHF tot 20-nov-15 - Data Science.txt";
	public static final String SOURCE_DM = "C:\\Coding\\DataScience\\eVita\\data\\Logdata DM tot 20-NOV-15 - Data Science.txt";

	private Set<DataRow> chf;
	private Set<DataRow> dm;

	private SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm");

	public Data2Weka() {
		chf = loadData(SOURCE_CHF);
		dm = loadData(SOURCE_DM);
	}

	/**
	 * Load the data files in memory
	 */
	public Set<DataRow> loadData(String source) {
		progress("Loading data: "+source);
		Set<DataRow> result = new HashSet<>();
		File file = new File(source);
		if(!file.isFile() || !file.exists()) {
			error("  Could not load file: "+file.getAbsolutePath());
			return result;
		}

		try(BufferedReader reader = new BufferedReader(new FileReader(source))) {
			String line = reader.readLine();
			while(line != null) {
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
							result.add(new DataRow(parts[0], date, code, information));
						}
					}
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			error("  Error while reading data file: "+file.getAbsolutePath());
			e.printStackTrace(System.err);
		}
		progress("  Result: "+result.toString());
		return result;
	}


	/**
	 * Represents a row of the data log file
	 */
	private class DataRow {
		public String id;
		public Calendar date;
		public int code;
		public String information;

		public DataRow(String id, Calendar date, int code, String information) {
			this.id = id;
			this.date = date;
			this.code = code;
			this.information = information;
		}
		public DataRow(String id, Date date, int code, String information) {
			this(id, dateToCalendar(date), code, information);
		}

		@Override
		public String toString() {
			return "("+id+", "+format.format(date.getTime())+", "+code+", "+information+")";
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
