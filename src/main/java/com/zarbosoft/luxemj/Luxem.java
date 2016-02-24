package com.zarbosoft.luxemj;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.zarbosoft.pidgoon.bytes.Callback;
import com.zarbosoft.pidgoon.bytes.Grammar;
import com.zarbosoft.pidgoon.bytes.GrammarFile;
import com.zarbosoft.pidgoon.bytes.Parse;

public class Luxem {
	static private Grammar grammar = null;
	static public Grammar grammar() throws IOException {
		if (grammar == null) {
			InputStream grammarStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("luxem.pidgoon");
			if (grammarStream == null) throw new AssertionError("Could not load luxem.pidgoon");
			grammar = GrammarFile
				.parse()
				.parse(grammarStream);
		}
		return grammar;
	}
	
	public static void parse(Map<String, Callback> callbacks, String string) throws IOException {
		new Parse<>()
			.grammar(grammar())
			.node("root")
			.callbacks(callbacks)
			.parse(string);
	}
}
