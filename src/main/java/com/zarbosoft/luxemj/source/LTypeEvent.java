package com.zarbosoft.luxemj.source;

import com.zarbosoft.pidgoon.events.Event;

public class LTypeEvent implements Event {

	public LTypeEvent(String string) {
		this.value = string;
	}

	public String value;

}
