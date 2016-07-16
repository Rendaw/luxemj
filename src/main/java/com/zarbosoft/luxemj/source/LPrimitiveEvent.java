package com.zarbosoft.luxemj.source;

import com.zarbosoft.luxemj.Luxem;
import com.zarbosoft.luxemj.LuxemEvent;
import com.zarbosoft.pidgoon.events.Event;

@Luxem.Configuration(name = "primitive")
public class LPrimitiveEvent implements LuxemEvent {
	public LPrimitiveEvent(final String value) {
		this.value = value;
	}

	public LPrimitiveEvent() {
	}

	@Luxem.Configuration
	public String value;

	@Override
	public boolean matches(final Event event) {
		return event.getClass() == getClass() && (value == null || value.equals(((LPrimitiveEvent) event).value));
	}

	@Override
	public String toString() {
		return String.format("%s", value == null ? "*" : value);
	}
}
