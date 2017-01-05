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
import com.zarbosoft.pidgoon.internal.Helper;
import com.zarbosoft.pidgoon.internal.Node;
import com.zarbosoft.pidgoon.internal.Pair;
import com.zarbosoft.pidgoon.nodes.*;
import com.zarbosoft.pidgoon.nodes.Set;
import org.reflections.Reflections;

import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Luxem {
	static final Reflections reflections = new Reflections("com.zarbosoft");
	static private Luxem instance = null;
	static private Grammar grammar = null;

	static public Grammar grammar() {
		if (grammar == null) {
			final InputStream grammarStream =
					Thread.currentThread().getContextClassLoader().getResourceAsStream("luxem.pidgoon");
			if (grammarStream == null)
				throw new AssertionError("Could not load luxem.pidgoon");
			grammar = GrammarFile.parse().uncertainty(630).parse(grammarStream);
		}
		return grammar;
	}

	static private Luxem get() {
		if (instance == null)
			instance = new Luxem();
		return instance;
	}

	/**
	 * Returns a grammar for deserializing a type.
	 * <p>
	 * The type must be a primitive/box/enum, standard collection, or a class annotated with Configuration.
	 * For annotated classes,
	 * - There must be a nullary constructor
	 * - All fields to deserialize must be annotated with Configuration
	 * - The constructor and target fields must be public
	 * If the annotated class is an interface or abstract class, derived classes must also be public and annotated.
	 * Collection fields are always optional will be populated with a default value type.
	 * <p>
	 * To add custom type deserialization, subclass this and override implementationNodeForType.
	 *
	 * @param target Subject matter
	 * @return A grammar that loads target
	 */
	static public Grammar grammarForType(final Class<?> target) {
		return get().implementationGrammarForType(target);
	}

	public static class TypeInfo {

		public final Type inner;
		final Type generic;

		public TypeInfo(final Type target) {
			if (target instanceof ParameterizedType) {
				this.generic = target;
				this.inner = ((ParameterizedType) target).getRawType();
			} else {
				this.inner = target;
				this.generic = null;
			}
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
		} else if (((Class<?>) target.inner).isEnum()) {
			final Union union = new Union();
			for (final Object prevalue : ((Class<?>) target.inner).getEnumConstants()) {
				final Enum<?> value = (Enum<?>) prevalue;
				final Field field = Helper.uncheck(() -> ((Class<?>) target.inner).getField(value.name()));
				String name = getConfigurationName(field.getAnnotation(Configuration.class));
				if (name == null)
					name = value.name();
				union.add(new BakedOperator(new Terminal(new LPrimitiveEvent(name)), store -> store.pushStack(value)));
			}
			return union;
		} else if (List.class.isAssignableFrom((Class<?>) target.inner)) {
			if (target.generic == null)
				throw new AssertionError("Unparameterized list!");
			final Type innerType = ((ParameterizedType) target.generic).getActualTypeArguments()[0];
			return new Sequence()
					.add(new BakedOperator(new Terminal(new LArrayOpenEvent()), s -> s.pushStack(0)))
					.add(new Repeat(new BakedOperator(this.implementationNodeForType(seen,
							grammar,
							new TypeInfo(innerType)
					), s -> {
						Object temp = s.stackTop();
						s = (Store) s.popStack();
						Integer count = s.stackTop();
						s = (Store) s.popStack();
						return s.pushStack(temp).pushStack(count + 1);
					})))
					.add(new BakedOperator(new Terminal(new LArrayCloseEvent()), s -> {
						final List out;
						if (target.inner == List.class)
							out = new ArrayList<>();
						else
							out = (List) Helper.uncheck(((Class<?>) target.inner)::newInstance);
						s = (Store) Helper.stackPopSingleList(s, out::add);
						Collections.reverse(out);
						return s.pushStack(out);
					}));
		} else if (java.util.Set.class.isAssignableFrom((Class<?>) target.inner)) {
			if (target.generic == null)
				throw new AssertionError("Unparameterized set!");
			final Type innerType = ((ParameterizedType) target.generic).getActualTypeArguments()[0];
			return new Sequence()
					.add(new BakedOperator(new Terminal(new LArrayOpenEvent()), s -> s.pushStack(0)))
					.add(new Repeat(new BakedOperator(this.implementationNodeForType(seen,
							grammar,
							new TypeInfo(innerType)
					), s -> {
						Object temp = s.stackTop();
						s = (Store) s.popStack();
						Integer count = s.stackTop();
						s = (Store) s.popStack();
						return s.pushStack(temp).pushStack(count + 1);
					})))
					.add(new BakedOperator(new Terminal(new LArrayCloseEvent()), s -> {
						final java.util.Set out;
						if (target.inner == java.util.Set.class)
							out = new HashSet();
						else
							out = (java.util.Set) Helper.uncheck(((Class<?>) target.inner)::newInstance);
						s = (Store) Helper.stackPopSingleList(s, (Consumer<Object>) out::add);
						return s.pushStack(out);
					}));
		} else if (Map.class.isAssignableFrom((Class<?>) target.inner)) {
			if (target.generic == null)
				throw new AssertionError("Unparameterized map!");
			if (((ParameterizedType) target.generic).getActualTypeArguments()[0] != String.class)
				throw new AssertionError("Luxem configurable maps must have String keys.");
			final Type innerType = ((ParameterizedType) target.generic).getActualTypeArguments()[1];
			return new Sequence()
					.add(new BakedOperator(new Terminal(new LObjectOpenEvent()), s -> s.pushStack(0)))
					.add(new Repeat(new Sequence()
							.add(new BakedOperator(new Terminal(new LKeyEvent(null)),
									store -> store.pushStack(((LKeyEvent) store.top()).value)
							))
							.add(new BakedOperator(this.implementationNodeForType(seen,
									grammar,
									new TypeInfo(innerType)
							), Helper::stackDoubleElement))))
					.add(new BakedOperator(new Terminal(new LObjectCloseEvent()), s -> {
						final Map out;
						if (target.inner == Map.class)
							out = new HashMap();
						else
							out = (Map) Helper.uncheck(((Class<?>) target.inner)::newInstance);
						s = (Store) Helper.<Pair<String, Object>>stackPopSingleList(s, p -> out.put(p.first, p.second));
						return s.pushStack(out);
					}));
		} else if (((Class<?>) target.inner).getAnnotation(Configuration.class) != null) {
			if (((Class<?>) target.inner).isInterface() ||
					Modifier.isAbstract(((Class<?>) target.inner).getModifiers())) {
				if (!seen.contains(target.inner)) {
					seen.add(target.inner);
					final java.util.Set<String> subclassNames = new HashSet<>();
					final Union out = new Union();
					Sets
							.difference(reflections.getSubTypesOf((Class<?>) target.inner), ImmutableSet.of(target))
							.stream()
							.map(s -> (Class<?>) s)
							.filter(s -> !Modifier.isAbstract(s.getModifiers()))
							.forEach(s -> {
								String name = getConfigurationName(s.getAnnotation(Configuration.class));
								if (name == null)
									name = s.getName();
								if (subclassNames.contains(name))
									throw new IllegalArgumentException(String.format(
											"Specific type [%s] of polymorphic type [%s] is ambiguous.",
											name,
											target.inner
									));
								subclassNames.add(name);
								out.add(new Sequence()
										.add(new Terminal(new LTypeEvent(name.toLowerCase())))
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
					throw new AssertionError(String.format(
							"Class [%s] of field marked for luxem serialization has no nullary constructor or constructor is not public.",
							target.inner
					));
				}
				if (constructor != null) {
					if (!seen.contains(target.inner)) {
						seen.add(target.inner);
						final java.util.Set<Field> fields = new HashSet<>();
						Class<?> level = (Class<?>) target.inner;
						while (level.getSuperclass() != null) {
							Helper
									.stream(level.getDeclaredFields())
									.filter(f -> f.getAnnotation(Configuration.class) != null)
									.forEach(f -> {
										if ((f.getModifiers() & Modifier.PUBLIC) == 0)
											throw new AssertionError(String.format("Field %s marked for luxem serialization is not public.",
													f
											));
										fields.add(f);
									});
							level = level.getSuperclass();
						}
						final Sequence seq = new Sequence();
						{
							seq.add(new BakedOperator(new Terminal(new LObjectOpenEvent()), s -> s.pushStack(0)));
							final Set set = new Set();
							fields.forEach(f -> {
								String fieldName = getConfigurationName(f.getAnnotation(Configuration.class));
								if (fieldName == null)
									fieldName = f.getName();
								final Node subNode;
								try {
									subNode = this.implementationNodeForType(seen, grammar, new TypeInfo(f));
								} catch (final AssertionError e) {
									throw new AssertionError(String.format("Error creating rule for %s", f), e);
								}
								set.add(new BakedOperator(new Sequence()
										.add(new Terminal(new LKeyEvent(fieldName)))
										.add(subNode), s -> {
									s = (Store) s.pushStack(f);
									return Helper.stackDoubleElement(s);
								}), fieldIsRequired(f));
							});
							seq.add(set);
							seq.add(new Terminal(new LObjectCloseEvent()));
						}
						final Node topNode;
						final java.util.Set<Field> minimalFields2 =
								fields.stream().filter(Luxem::fieldIsRequired).collect(Collectors.toSet());
						final java.util.Set<Field> minimalFields;
						if (minimalFields2.size() == 0)
							minimalFields = fields;
						else
							minimalFields = minimalFields2;
						if (minimalFields.size() == 1) {
							final Union temp = new Union();
							temp.add(seq);
							temp.add(new BakedOperator(this.implementationNodeForType(seen,
									grammar,
									new TypeInfo(minimalFields.iterator().next())
							), s -> {
								final Object value = s.stackTop();
								s = (Store) s.popStack();
								return s.pushStack(new Pair<>(value, minimalFields.iterator().next())).pushStack(1);
							}));
							topNode = temp;
						} else {
							topNode = seq;
						}
						grammar.add(target.inner.getTypeName(), new BakedOperator(topNode, s -> {
							final Object out = Helper.uncheck(((Class<?>) target.inner)::newInstance);
							final java.util.Set<Field> fields2 = new HashSet<>();
							fields2.addAll(fields);
							s = (Store) Helper.<Pair<Object, Field>>stackPopSingleList(s, (pair) -> {
								fields2.remove(pair.second);
								Helper.uncheck(() -> pair.second.set(out, pair.first));
							});
							for (final Field field : fields2) {
								final Class<?> fieldType = field.getType();
								final Object value;
								try {
									if (Collection.class.isAssignableFrom(fieldType)) {
										if (fieldType == List.class)
											value = new ArrayList<>();
										else if (fieldType == java.util.Set.class)
											value = new HashSet();
										else
											value = fieldType.newInstance();
									} else if (Map.class.isAssignableFrom(fieldType)) {
										if (fieldType == Map.class)
											value = new HashMap();
										else
											value = fieldType.newInstance();
									} else
										continue;
								} catch (final InstantiationException e) {
									throw new AssertionError(String.format(
											"Uninstantiable field [%s] of type [%s] in [%s]",
											field.getName(),
											fieldType.getName(),
											target.inner
									), e);
								} catch (final IllegalAccessException e) {
									throw new AssertionError(String.format(
											"Uninstantiable field [%s] of type [%s] in [%s]",
											field.getName(),
											fieldType.getName(),
											target.inner
									), e);
								}
								Helper.uncheck(() -> field.set(out, value));
							}
							return s.pushStack(out);
						}));
					}
					return new Reference(target.inner.getTypeName());
				}
			}
		}
		throw new AssertionError(String.format("Unconfigurable field of type or derived type [%s]", target.inner));
	}

	static boolean fieldIsRequired(final Field field) {
		if (Collection.class.isAssignableFrom(field.getType()))
			return false;
		if (Map.class.isAssignableFrom(field.getType()))
			return false;
		final Configuration annotation = field.getAnnotation(Configuration.class);
		if (annotation == null)
			return false;
		if (annotation.optional())
			return false;
		return true;
	}

	static String getConfigurationName(final Luxem.Configuration annotation) {
		if (annotation == null)
			return null;
		if (annotation.name().equals(""))
			return null;
		return annotation.name();
	}

	public static void parse(final Map<String, Callback> callbacks, final String string) {
		new Parse<>().grammar(grammar()).node("root").callbacks(callbacks).parse(string);
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Configuration {
		String name() default "";

		boolean optional() default false;

		String description() default "";
	}
}
