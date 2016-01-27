package me.wiefferink.evita;


import java.util.SortedSet;

public class Session implements Comparable<Session> {
	public String id;
	public SortedSet<Action> actions;

	public Session(String id, SortedSet<Action> actions) {
		this.id = id;
		this.actions = actions;
	}

	@Override
	public String toString() {
		return "(" + id + ", " + actions.toString() + ")\n";
	}

	/**
	 * Sort by starting date
	 */
	@Override
	public int compareTo(Session session) {
		return ((Long) actions.first().date.getTimeInMillis()).compareTo(session.actions.first().date.getTimeInMillis());
	}
}
