package me.wiefferink.evita.classes;

import me.wiefferink.evita.Action;
import me.wiefferink.evita.Class;
import me.wiefferink.evita.Session;

import java.util.SortedSet;

public class MeasurementsClass extends Class {
	@Override
	public String determine(SortedSet<Session> sessions) {
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
			return "Adherend";
		}
		return "NotAdherend";
	}

	@Override
	public String getWekaHeader() {
		return "classMeasurements {Adherend,NotAdherend}";
	}
}
