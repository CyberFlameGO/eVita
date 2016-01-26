package me.wiefferink.evita.features;

import me.wiefferink.evita.Feature;
import me.wiefferink.evita.Session;

import java.util.Calendar;
import java.util.SortedSet;

/**
 * Time between first and second login in days
 */
public class SecondThirdLoginTimeFeature extends Feature<Integer> {
	public Integer calculate(SortedSet<Session> sessions) {
		Calendar first = null;
		boolean skip = true;
		for (Session session : sessions) {
			if (skip) {
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