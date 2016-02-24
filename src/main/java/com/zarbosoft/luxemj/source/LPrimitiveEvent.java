package com.zarbosoft.luxemj.source;

import com.zarbosoft.pidgoon.events.Event;

public class LPrimitiveEvent implements Event {
	public LPrimitiveEvent(String type, String value) {
		this.type = type;
		this.value = value;
	}
	public String type;
	public String value;
}
