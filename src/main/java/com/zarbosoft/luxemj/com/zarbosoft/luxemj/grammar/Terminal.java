package com.zarbosoft.luxemj.com.zarbosoft.luxemj.grammar;

import com.zarbosoft.luxemj.Luxem;
import com.zarbosoft.pidgoon.events.Event;

@Luxem.Configuration(name = "terminal")
public abstract class Terminal implements Node {
	@Override
	public com.zarbosoft.pidgoon.internal.Node build() {
		return new com.zarbosoft.pidgoon.events.Terminal(getEvent());
	}

	public abstract Event getEvent();
}
