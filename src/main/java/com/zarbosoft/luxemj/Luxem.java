package com.zarbosoft.luxemj;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import com.zarbosoft.undepurseable.Callback;
import com.zarbosoft.undepurseable.Grammar;
import com.zarbosoft.undepurseable.GrammarParser;

public class Luxem {
	static private Grammar grammar = null;
	public static void parse(Callbacks callbacks, String string) throws IOException {
		if (grammar == null) {
			InputStream grammarStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("luxem.undepu");
			if (grammarStream == null) throw new AssertionError("Could not load luxem.undepu");
			grammar = GrammarParser
				.parse(
					grammarStream, 
					new HashMap<String, Callback>());
		}
		grammar.parse("root", new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8))); 
	}
}
