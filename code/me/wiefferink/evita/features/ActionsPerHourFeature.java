package me.wiefferink.evita.features;

import me.wiefferink.evita.Feature;
import me.wiefferink.evita.Session;

import java.util.SortedSet;

/**
 * Number of actions in the first session
 */
public class ActionsPerHourFeature extends Feature<Integer> {
	public Integer calculate(SortedSet<Session> sessions) {
		int actions = sessions.first().actions.size();
		double minutes = Math.max(1.0, ((sessions.first().actions.last().date.getTimeInMillis() - sessions.first().actions.first().date.getTimeInMillis()) / (1000.0*60.0)));
		return (int) (actions * (60 / minutes));
	}

	public String getWekaHeader() {
		return "actionsPerHour NUMERIC";
	}
}