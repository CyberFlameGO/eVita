package me.wiefferink.evita.features;

import me.wiefferink.evita.Action;
import me.wiefferink.evita.Feature;
import me.wiefferink.evita.Session;

import java.util.SortedSet;

/**
 * Number of actions of a certain type in the first session
 */
public class CodeFrequencyFirstSessionFeature extends Feature<Integer> {
	public int code;

	public CodeFrequencyFirstSessionFeature(int code) {
		this.code = code;
	}

	public Integer calculate(SortedSet<Session> sessions) {
		Integer result = 0;
		for (Action action : sessions.first().actions) {
			if (action.code == code) {
				result++;
			}
		}
		return result;
	}

	public String getWekaHeader() {
		return "code" + code + " NUMERIC";
	}
}
