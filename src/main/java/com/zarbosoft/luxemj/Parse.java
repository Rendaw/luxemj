package com.zarbosoft.luxemj;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.zarbosoft.luxemj.source.LArrayCloseEvent;
import com.zarbosoft.luxemj.source.LArrayOpenEvent;
import com.zarbosoft.luxemj.source.LKeyEvent;
import com.zarbosoft.luxemj.source.LObjectCloseEvent;
import com.zarbosoft.luxemj.source.LObjectOpenEvent;
import com.zarbosoft.luxemj.source.LPrimitiveEvent;
import com.zarbosoft.luxemj.source.LTypeEvent;
import com.zarbosoft.pidgoon.bytes.Callback;
import com.zarbosoft.pidgoon.events.EventStream;

public class Parse<O> extends com.zarbosoft.pidgoon.events.Parse<O> {
	private Parse(Parse<O> other) {
		super(other);
	}
	
	@Override
	protected Parse<O> split() {
		return new Parse<O>(this);
	}

	public Parse() {}

	public O parse(String string) throws IOException {
		return parse(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)));
	}
	
	@SuppressWarnings("unchecked")
	public O parse(InputStream stream) throws IOException {
		// TODO allow splitting ParseContext, split before each step.  Should be okay since luxem is unambiguous.
		final EventStream<O> higher = new com.zarbosoft.pidgoon.events.Parse<O>()
			.grammar(grammar)
			.callbacks((Map<String, Callback>)(Object)callbacks)
			.parse();
		new com.zarbosoft.pidgoon.bytes.Parse<Object>()
			.grammar(Luxem.grammar())
			.callbacks(new ImmutableMap.Builder<String, Callback>()
				.put("OBJECT_OPEN", s -> higher.push(new LObjectOpenEvent()))
				.put("OBJECT_CLOSE", s -> higher.push(new LObjectCloseEvent()))
				.put("ARRAY_OPEN", s -> higher.push(new LArrayOpenEvent()))
				.put("ARRAY_CLOSE", s -> higher.push(new LArrayCloseEvent()))
				.put("key", s -> higher.push(new LKeyEvent(s.topData().toString())))
				.put("type", s -> higher.push(new LTypeEvent(s.topData().toString())))
				.put("primitive", s -> {
					String type = "";
					Object pretype = s.peekStack();
					if ((type != null) && (pretype instanceof LTypeEvent)) {
						s.popStack();
						type = ((LTypeEvent)pretype).value;
					}
					higher.push(new LPrimitiveEvent(type, s.topData().toString()));
				})
				.build()
			)
			.parse(stream);
		return higher.finish();
	}
}
