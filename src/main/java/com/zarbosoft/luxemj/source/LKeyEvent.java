package com.zarbosoft.luxemj.source;

import com.zarbosoft.luxemj.Luxem;
import com.zarbosoft.luxemj.LuxemEvent;
import com.zarbosoft.pidgoon.events.Event;

@Luxem.Configuration(name = "key")
public class LKeyEvent implements LuxemEvent {
	public LKeyEvent(final String string) {
		value = string;
	}

	public LKeyEvent() {
	}

	@Luxem.Configuration
	public String value;

	@Override
	public boolean matches(final Event event) {
		return event.getClass() == getClass() && (value == null || value.equals(((LKeyEvent) event).value));
	}

	@Override
	public String toString() {
		return String.format("%s:", value == null ? "*" : value);
	}
}
