package com.zarbosoft.luxemj.grammar;

import com.zarbosoft.luxemj.Luxem;

import java.util.List;

@Luxem.Configuration(name = "seq")
public class Sequence implements Node {
	@Luxem.Configuration
	public List<Node> nodes;

	@Override
	public com.zarbosoft.pidgoon.internal.Node build() {
		final com.zarbosoft.pidgoon.nodes.Sequence out = new com.zarbosoft.pidgoon.nodes.Sequence();
		for (final Node node : nodes)
			out.add(node.build());
		return out;
	}
}
