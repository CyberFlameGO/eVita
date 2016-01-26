package me.wiefferink.evita;

import java.util.SortedSet;

/**
 * Abstract feature
 *
 * @param <Type> resulting type
 */
public abstract class Feature<Type> {
	public abstract Type calculate(SortedSet<Session> sessions);

	public abstract String getWekaHeader();
}








