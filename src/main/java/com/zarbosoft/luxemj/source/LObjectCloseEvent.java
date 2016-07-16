package com.zarbosoft.luxemj.source;

import com.zarbosoft.luxemj.Luxem;
import com.zarbosoft.luxemj.LuxemEvent;
import com.zarbosoft.pidgoon.events.Event;

@Luxem.Configuration(name = "object-close")
public class LObjectCloseEvent implements LuxemEvent {

	@Override
	public boolean matches(final Event event) {
		return event.getClass() == getClass();
	}

	@Override
	public String toString() {
		return String.format("}");
	}
}
