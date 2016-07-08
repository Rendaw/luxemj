package com.zarbosoft.luxemj;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.zarbosoft.luxemj.source.*;
import com.zarbosoft.pidgoon.AbortParse;
import com.zarbosoft.pidgoon.bytes.Callback;
import com.zarbosoft.pidgoon.bytes.Grammar;
import com.zarbosoft.pidgoon.bytes.GrammarFile;
import com.zarbosoft.pidgoon.bytes.Parse;
import com.zarbosoft.pidgoon.events.BakedOperator;
import com.zarbosoft.pidgoon.events.Store;
import com.zarbosoft.pidgoon.events.Terminal;
import com.zarbosoft.pidgoon.internal.Mutable;
import com.zarbosoft.pidgoon.internal.Node;
import com.zarbosoft.pidgoon.nodes.*;
import com.zarbosoft.pidgoon.nodes.Set;
import org.reflections.Reflections;

import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.*;
import java.util.*;

public class Luxem {
	private static final Reflections reflections = new Reflections("com.zarbosoft");
	static private Luxem instance = null;
	static private Grammar grammar = null;

	static public Grammar grammar() {
		if (grammar == null) {
			final InputStream grammarStream =
					Thread.currentThread().getContextClassLoader().getResourceAsStream("luxem.pidgoon");
			if (grammarStream == null)
				throw new AssertionError("Could not load luxem.pidgoon");
			grammar = GrammarFile.parse().parse(grammarStream);
		}
		return grammar;
	}

	static private Luxem get() {
		if (instance == null)
			instance = new Luxem();
		return instance;
	}

	static public Grammar grammarForType(final Class<?> target) {
		return get().implementationGrammarForType(target);
	}

	public static class TypeInfo {

		public final Type inner;
		private final Type generic;

		public TypeInfo(final Type target) {
			this.inner = target;
			this.generic = null;
		}

		public TypeInfo(final Field f) {
			this.inner = f.getType();
			this.generic = f.getGenericType();
		}
	}

	public Grammar implementationGrammarForType(final Type target) {
		final Grammar grammar = new Grammar();
		final HashSet<Type> seen = new HashSet<>();
		grammar.add("root", this.implementationNodeForType(seen, grammar, new TypeInfo(target)));
		return grammar;
	}

	public Node implementationNodeForType(final HashSet<Type> seen, final Grammar grammar, final TypeInfo target) {
		if (target.inner == String.class) {
			return new BakedOperator(new Terminal(new LPrimitiveEvent(null)), s -> {
				final LPrimitiveEvent event = (LPrimitiveEvent) s.top();
				return s.pushStack(event.value);
			});
		} else if ((target.inner == int.class) || (target.inner == Integer.class)) {
			return new BakedOperator(new Terminal(new LPrimitiveEvent(null)), s -> {
				final LPrimitiveEvent event = (LPrimitiveEvent) s.top();
				try {
					return s.pushStack(Integer.valueOf(event.value));
				} catch (final NumberFormatException e) {
					throw new AbortParse(e);
				}
			});
		} else if ((target.inner == double.class) || (target.inner == Double.class)) {
			return new BakedOperator(new Terminal(new LPrimitiveEvent(null)), s -> {
				final LPrimitiveEvent event = (LPrimitiveEvent) s.top();
				try {
					return s.pushStack(Double.valueOf(event.value));
				} catch (final NumberFormatException e) {
					throw new AbortParse(e);
				}
			});
		} else if ((target.inner == boolean.class) || (target.inner == Boolean.class)) {
			return new BakedOperator(new Terminal(new LPrimitiveEvent(null)), s -> {
				final LPrimitiveEvent event = (LPrimitiveEvent) s.top();
				if (event.value.equals("true"))
					return s.pushStack(true);
				else if (event.value.equals("false"))
					return s.pushStack(false);
				else
					throw new AbortParse(String.format("Invalid value [%s] for field type [%s]", event.value, target));
			});
		} else if (List.class.isAssignableFrom((Class<?>) target.inner)) {
			if (target.generic == null)
				throw new AssertionError("Unparameterized list!");
			final Class<?> innerType = (Class<?>) ((ParameterizedType) target.generic).getActualTypeArguments()[0];
			return new Sequence()
					.add(new BakedOperator(new Terminal(new LArrayOpenEvent()), s -> s.pushStack(0)))
					.add(new Repeat(new BakedOperator(this.implementationNodeForType(seen,
							grammar,
							new TypeInfo(innerType)
					), s -> {
						Object temp = s.stackTop();
						s = (Store) s.popStack();
						Integer count = (Integer) s.stackTop();
						s = (Store) s.popStack();
						return s.pushStack(temp).pushStack(count + 1);
					})))
					.add(new BakedOperator(new Terminal(new LArrayCloseEvent()), s -> {
						final List out;
						if (target.inner == List.class)
							out = new ArrayList<>();
						else
							out = (List) Helper.uncheck(() -> ((Class<?>) target.inner).newInstance());
						s = Helper.stackPopSingleList(s, v -> {
							out.add(v);
						});
						Collections.reverse(out);
						return s.pushStack(out);
					}));
		} else if (((Class<?>) target.inner).isInterface()) {
			if (!seen.contains(target.inner)) {
				seen.add(target.inner);
				final Union out = new Union();
				Sets
						.difference(reflections.getSubTypesOf((Class<?>) target.inner), ImmutableSet.of(target))
						.stream()
						.map(s -> (Class<?>) s)
						.filter(s -> !Modifier.isAbstract(s.getModifiers()))
						.forEach(s -> {
							out.add(new Sequence()
									.add(new Terminal(new LTypeEvent(s.getName().toLowerCase())))
									.add(this.implementationNodeForType(seen, grammar, new TypeInfo(s))));
						});
				grammar.add(target.inner.getTypeName(), out);
			}
			return new Reference(target.inner.getTypeName());
		} else {
			Constructor<?> constructor = null;
			try {
				constructor = ((Class<?>) target.inner).getConstructor();
			} catch (final NoSuchMethodException e) {
			}
			if (constructor != null) {
				if (!seen.contains(target.inner)) {
					seen.add(target.inner);
					final Sequence seq = new Sequence();
					seq.add(new Terminal(new LObjectOpenEvent()));
					final Set set = new Set();
					final Mutable<Integer> fieldCount = new Mutable<>(0);
					Class<?> level = (Class<?>) target.inner;
					while (level.getSuperclass() != null) {
						Helper
								.stream(level.getDeclaredFields())
								.filter(f -> f.getAnnotation(Configuration.class) != null)
								.forEach(f -> {
									if ((f.getModifiers() & Modifier.PUBLIC) == 0)
										throw new AssertionError(String.format(
												"Field %s in %s marked for luxem serialization is not public.",
												f,
												target.inner
										));
									set.add(new BakedOperator(new Sequence()
											.add(new Terminal(new LKeyEvent(f.getName())))
											.add(this.implementationNodeForType(seen, grammar, new TypeInfo(f))), s -> {
										return s.pushStack(f);
									}));
									fieldCount.value += 1;
								});
						level = level.getSuperclass();
					}
					seq.add(set);
					seq.add(new Terminal(new LObjectCloseEvent()));
					grammar.add(target.inner.getTypeName(), new BakedOperator(seq, s -> {
						final Object out = Helper.uncheck(() -> ((Class<?>) target.inner).newInstance());
						return Helper.<Field, Object>stackPopDoubleList(s, fieldCount.value, (k, v) -> {
							Helper.uncheck(() -> k.set(out, v));
						}).pushStack(out);
					}));
				}
				return new Reference(target.inner.getTypeName());
			}
		}
		throw new AssertionError(String.format("Unconfigurable field of type [%s]", target.inner));
	}

	public static void parse(final Map<String, Callback> callbacks, final String string) {
		new Parse<>().grammar(grammar()).node("root").callbacks(callbacks).parse(string);
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Configuration {

	}

	public class PathGenerator {
	}
}
