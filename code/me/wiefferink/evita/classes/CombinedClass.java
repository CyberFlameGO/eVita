package me.wiefferink.evita.classes;

import me.wiefferink.evita.Action;
import me.wiefferink.evita.Class;
import me.wiefferink.evita.Data2Weka;
import me.wiefferink.evita.Session;

import java.util.Calendar;
import java.util.SortedSet;

public class CombinedClass extends Class {
	@Override
	public String determine(SortedSet<Session> sessions) {
		// Try logged in more as 4 times a year
		Calendar endTime = Data2Weka.getDate("21-11-2015 00:00");
		if(endTime != null) {
			long diff = endTime.getTimeInMillis() - sessions.first().actions.first().date.getTimeInMillis();
			long quarterYear = 7884000000L;
			int shouldLogin = (int) Math.ceil(diff / (double) quarterYear);
			if (sessions.size() >= shouldLogin) {
				return "Adherent";
			}
		}
		// Try codes
		for (Session session : sessions) {
			for (Action action : session.actions) {
				if (action.code == 52 || action.code == 91) { // personal goal, education module
					return "Adherent";
				}
			}
		}
		// Measurements on 4 different days
		int count = 0;
		for (Session session : sessions) {
			for (Action action : session.actions) {
				if (action.code == 35) {
					count++;
					break; // Go to next session
				}
			}
		}
		if (count >= 4) {
			return "Adherent";
		}
		return "NotAdherent";
	}

	@Override
	public String getWekaHeader() {
		return "{Adherent,NotAdherent}";
	}

	@Override
	public String getName() {
		return "classCombined";
	}
}
