package me.wiefferink.evita;

import java.util.SortedSet;

public abstract class Class {
	public abstract String determine(SortedSet<Session> sessions);

	public abstract String getWekaHeader();

	public abstract String getName();
}
