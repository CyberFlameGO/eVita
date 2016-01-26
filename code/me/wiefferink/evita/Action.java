package me.wiefferink.evita;

import java.util.Calendar;
import java.util.Date;

/**
 * Represents a row of the data log file
 */
public class Action implements Comparable<Action> {
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
		this(id, Data2Weka.dateToCalendar(date), code, information);
	}

	@Override
	public String toString() {
		return "(" + id + ", " + Data2Weka.format.format(date.getTime()) + ", " + code + ", " + information + ")";
	}

	/**
	 * Sort by date
	 */
	@Override
	public int compareTo(Action action) {
		return ((Long) date.getTimeInMillis()).compareTo(action.date.getTimeInMillis());
	}
}
