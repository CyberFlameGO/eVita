package me.wiefferink.evita.features;

import me.wiefferink.evita.Feature;
import me.wiefferink.evita.Session;

import java.util.SortedSet;

/**
 * Number of actions in the first session
 */
public class ActionsFirstSessionFeature extends Feature<Integer> {
	public Integer calculate(SortedSet<Session> sessions) {
		return sessions.first().actions.size();
	}

	public String getWekaHeader() {
		return "actionCount NUMERIC";
	}
}