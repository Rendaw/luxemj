package com.zarbosoft.luxemj;

import com.google.common.collect.ImmutableMap;
import com.zarbosoft.luxemj.path.LuxemArrayPath;
import com.zarbosoft.luxemj.path.LuxemPath;
import com.zarbosoft.luxemj.source.*;
import com.zarbosoft.pidgoon.AbortParse;
import com.zarbosoft.pidgoon.InvalidStream;
import com.zarbosoft.pidgoon.bytes.Callback;
import com.zarbosoft.pidgoon.bytes.Clip;
import com.zarbosoft.pidgoon.bytes.ClipStore;
import com.zarbosoft.pidgoon.events.EventStream;
import com.zarbosoft.pidgoon.internal.BaseParse;
import com.zarbosoft.pidgoon.internal.Pair;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;

public class Parse<O> extends BaseParse<Parse<O>> {

	private Parse(final Parse<O> other) {
		super(other);
	}

	@Override
	protected Parse<O> split() {
		return new Parse<>(this);
	}

	public Parse() {
	}

	public O parse(final String string) {
		return parse(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)));
	}

	public O parse(final InputStream stream) {
		return new com.zarbosoft.pidgoon.bytes.Parse<EventStream<O>>()
				.errorHistory(errorHistoryLimit)
				.uncertainty(uncertaintyLimit)
				.stack(() -> {
					return new Pair<>(new com.zarbosoft.pidgoon.events.Parse<O>()
							.grammar(grammar)
							.node(node)
							.stack(initialStack)
							.callbacks((Map<String, Callback>) (Object) callbacks)
							.parse(), new LuxemArrayPath(null));
				})
				.grammar(Luxem.grammar())
				.node("root")
				.callbacks(new ImmutableMap.Builder<String, Callback>()
						.put("root", s -> {
							final Pair<EventStream<?>, LuxemPath> context = s.stackTop();
							return s.popStack().pushStack(context.first);
						})
						.put("OBJECT_OPEN", wrap(s -> new LObjectOpenEvent()))
						.put("OBJECT_CLOSE", wrap(s -> new LObjectCloseEvent()))
						.put("ARRAY_OPEN", wrap(s -> new LArrayOpenEvent()))
						.put("ARRAY_CLOSE", wrap(s -> new LArrayCloseEvent()))
						.put("key", wrap(s -> new LKeyEvent(s.toString())))
						.put("type", wrap(s -> new LTypeEvent(s.toString())))
						.put("primitive", wrap(s -> new LPrimitiveEvent(s.toString())))
						.build())
				.parse(stream)
				.finish();
	}

	private static Callback wrap(final Function<Clip, LuxemEvent> supplier) {
		return s -> {
			final Pair<EventStream<?>, LuxemPath> context = s.stackTop();
			s = (ClipStore) s.popStack();
			final EventStream<?> substream = context.first;
			final LuxemPath path = context.second;
			final LuxemEvent e = supplier.apply(s.topData());
			try {
				return s.pushStack(new Pair<>(substream.push(e, path.toString()), path.push(e)));
			} catch (final InvalidStream i) {
				throw new AbortParse(i);
			}
		};
	}

}
