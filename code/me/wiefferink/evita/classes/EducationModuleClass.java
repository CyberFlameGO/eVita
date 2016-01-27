package me.wiefferink.evita.classes;

import me.wiefferink.evita.Action;
import me.wiefferink.evita.Class;
import me.wiefferink.evita.Session;

import java.util.SortedSet;

public class EducationModuleClass extends Class {
	@Override
	public String determine(SortedSet<Session> sessions) {
		for (Session session : sessions) {
			for (Action action : session.actions) {
				if (action.code == 91) { // education module
					return "Adherend";
				}
			}
		}
		return "NotAdherend";
	}

	@Override
	public String getWekaHeader() {
		return "{Adherend,NotAdherend}";
	}


	@Override
	public String getName() {
		return "classEducationModule";
	}
}
