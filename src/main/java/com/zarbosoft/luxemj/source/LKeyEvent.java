package com.zarbosoft.luxemj.source;

import com.zarbosoft.pidgoon.events.Event;

public class LKeyEvent implements Event {
	public LKeyEvent(String string) {
		value = string;
	}

	public String value;
}
