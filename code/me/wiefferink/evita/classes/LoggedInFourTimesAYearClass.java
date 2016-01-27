package me.wiefferink.evita.classes;

import me.wiefferink.evita.Class;
import me.wiefferink.evita.Data2Weka;
import me.wiefferink.evita.Session;

import java.util.Calendar;
import java.util.SortedSet;

public class LoggedInFourTimesAYearClass extends Class {

	@Override
	public String determine(SortedSet<Session> sessions) {
		// Try logged in more as 4 times a year
		Calendar endTime = Data2Weka.getDate("21-11-2015 00:00");
		if(endTime != null) {
			long diff = endTime.getTimeInMillis() - sessions.first().actions.first().date.getTimeInMillis();
			long quarterYear = 7884000000L;
			int shouldLogin = (int) Math.ceil(diff / (double) quarterYear);
			if (sessions.size() >= shouldLogin) {
				return "Adherend";
			}
		}
		return "NotAdherend";
	}

	@Override
	public String getWekaHeader() {
		return "{Adherend,NotAdherend}";
	}

	@Override
	public String getName() {
		return "classLoggedIn";
	}
}
