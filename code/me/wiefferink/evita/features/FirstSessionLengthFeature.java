package me.wiefferink.evita.features;

import me.wiefferink.evita.Feature;
import me.wiefferink.evita.Session;

import java.util.SortedSet;

/**
 * Number of actions in the first session
 */
public class FirstSessionLengthFeature extends Feature<Integer> {
	public Integer calculate(SortedSet<Session> sessions) {
		return (int)((sessions.first().actions.last().date.getTimeInMillis() - sessions.first().actions.first().date.getTimeInMillis()) / (1000.0*60.0));
	}

	public String getWekaHeader() {
		return "sessionLength NUMERIC";
	}
}