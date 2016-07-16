package com.zarbosoft.luxemj.source;

import com.zarbosoft.luxemj.Luxem;
import com.zarbosoft.luxemj.LuxemEvent;
import com.zarbosoft.pidgoon.events.Event;

@Luxem.Configuration(name = "type")
public class LTypeEvent implements LuxemEvent {

	public LTypeEvent(final String string) {
		this.value = string;
	}

	public LTypeEvent() {
	}

	@Luxem.Configuration
	public String value;

	@Override
	public boolean matches(final Event event) {
		return event.getClass() == getClass() && (value == null || value.equals(((LTypeEvent) event).value));
	}

	@Override
	public String toString() {
		return String.format("(%s)", value == null ? "*" : value);
	}
}
