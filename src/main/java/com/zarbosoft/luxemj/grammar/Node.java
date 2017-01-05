package com.zarbosoft.luxemj.grammar;

import com.zarbosoft.luxemj.Luxem;

@Luxem.Configuration
public interface Node {
	com.zarbosoft.pidgoon.internal.Node build();
}
