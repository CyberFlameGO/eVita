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
		return "\t(" + id + ", " + Data2Weka.format.format(date.getTime()) + ", " + code + ", " + information + ")\n";
	}

	/**
	 * Sort by date
	 */
	@Override
	public int compareTo(Action action) {
		int result = ((Long) date.getTimeInMillis()).compareTo(action.date.getTimeInMillis());
		if(result == 0) { // Otherwise equal date actions are lost
			result = 1;
		}
		return result;
	}
}
