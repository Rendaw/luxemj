package com.zarbosoft.luxemj.com.zarbosoft.luxemj.grammar;

import com.zarbosoft.luxemj.Luxem;

import java.util.List;

@Luxem.Configuration(name = "or")
public class Union implements Node {
	@Luxem.Configuration
	public List<Node> nodes;

	@Override
	public com.zarbosoft.pidgoon.internal.Node build() {
		final com.zarbosoft.pidgoon.nodes.Union out = new com.zarbosoft.pidgoon.nodes.Union();
		for (final Node node : nodes)
			out.add(node.build());
		return out;
	}
}
