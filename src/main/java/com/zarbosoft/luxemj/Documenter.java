package com.zarbosoft.luxemj;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.zarbosoft.pidgoon.internal.Helper;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.*;
import java.util.*;

import static com.zarbosoft.luxemj.Luxem.getConfigurationName;
import static com.zarbosoft.luxemj.Luxem.reflections;
import static j2html.TagCreator.*;

public class Documenter {
	private static Documenter instance = null;

	public static Documenter get() {
		if (instance == null)
			instance = new Documenter();
		return instance;
	}

	private static String shorten(final List<String> strings, final Type type) {
		String temp = type.getTypeName();
		for (final String string : strings)
			temp = temp.replaceAll(string, "");
		return temp;
	}

	public String documentImplementation(final Class<?> root, final List<String> shorten) {
		final Map<Type, Tag> types = new HashMap<>();
		final ContainerTag body = body();
		final ContainerTag toc = ul();
		body.with(div().withClass("toc").with(h1("Table of Contents"), toc),
				a().withName("top"),
				h1("Introduction"),
				p().with(text(String.format("This documentation describes the luxem serialized format of %s.",
						shorten(shorten, root)
						)),
						text("  For a description of the syntax, see "),
						a("the luxem spec homepage").withHref("https://github.com/rendaw/luxem"),
						text(".")
				),
				p("Note that types only need to be specified where indicated."),
				p("Also, if a type only has one required field the outer type, the field can be persisted directly, so:"),
				code().withClass("block").withText("(repeat) { count: 4 }"),
				p("might optionally be shortened to:"),
				code().withClass("block").withText("(repeat) 4"),
				h2("Document Root"),
				documentValuesOf(new Luxem.TypeInfo(root), types, shorten)
		);
		if (!types.isEmpty()) {
			body.with(h1("Types"));
			types.entrySet().stream().sorted(new Comparator<Map.Entry<Type, Tag>>() {
				@Override
				public int compare(
						final Map.Entry<Type, Tag> o1, final Map.Entry<Type, Tag> o2
				) {
					return o1.getKey().getTypeName().compareTo(o2.getKey().getTypeName());
				}
			}).forEach(e -> {
				toc.with(li().with(a(shorten(shorten, e.getKey())).withHref(String.format("#%s",
						e.getKey().getTypeName()
				))));
				body.with(e.getValue());
			});
		}
		return html().with(head().with(title(String.format("%s: Luxem format documentation", shorten(shorten, root))),
				link().withRel("stylesheet").withHref("style.css")
		), body).render();
	}

	public void document(final File out, final Class<?> root, final List<String> shorten) {
		try (PrintWriter writer = new PrintWriter(out)) {
			writer.println(documentImplementation(root, shorten));
		} catch (final FileNotFoundException e) {
			throw new Helper.UncheckedException(e);
		}
	}

	public Tag documentValuesOf(
			final Luxem.TypeInfo target, final Map<Type, Tag> document, final List<String> shorten
	) {
		if (target.inner == String.class) {
			return span("Any string");
		} else if ((target.inner == int.class) || (target.inner == Integer.class)) {
			return span(String.format("Any integer [%e,%e]", (double) Integer.MIN_VALUE, (double) Integer.MAX_VALUE));
		} else if ((target.inner == double.class) || (target.inner == Double.class)) {
			return span(String.format("Any double [%.4e,%.4e]", Double.MIN_VALUE, Double.MAX_VALUE));
		} else if ((target.inner == boolean.class) || (target.inner == Boolean.class)) {
			return ul().with(li().with(code().withText("true")), li().with(code().withText("false")));
		} else if (((Class<?>) target.inner).isEnum()) {
			final ContainerTag values = ul().withClass("definitions");
			Helper.stream(((Class<?>) target.inner).getEnumConstants()).forEach(prevalue -> {
				final Enum<?> value = (Enum<?>) prevalue;
				final Field field = Helper.uncheck(() -> ((Class<?>) target.inner).getField(value.name()));
				String description = "";
				String name = null;
				final Luxem.Configuration config = field.getAnnotation(Luxem.Configuration.class);
				if (config != null) {
					if (!"".equals(config.name())) {
						name = config.name();
					}
					if (!"".equals(config.description())) {
						description = config.description();
					}
				}
				if (name == null)
					name = value.name();
				values.with(li().with(code().withText(name), span(description)));
			});
			return values;
		} else if (List.class.isAssignableFrom((Class<?>) target.inner)) {
			if (target.generic == null)
				throw new AssertionError("Unparameterized list!");
			final Type innerType = ((ParameterizedType) target.generic).getActualTypeArguments()[0];
			return span().with(p("List of:"), documentValuesOf(new Luxem.TypeInfo(innerType), document, shorten));
		} else if (java.util.Set.class.isAssignableFrom((Class<?>) target.inner)) {
			if (target.generic == null)
				throw new AssertionError("Unparameterized set!");
			final Type innerType = ((ParameterizedType) target.generic).getActualTypeArguments()[0];
			return span().with(p("Set of:"), documentValuesOf(new Luxem.TypeInfo(innerType), document, shorten));
		} else if (Map.class.isAssignableFrom((Class<?>) target.inner)) {
			if (target.generic == null)
				throw new AssertionError("Unparameterized map!");
			if (((ParameterizedType) target.generic).getActualTypeArguments()[0] != String.class)
				throw new AssertionError("Luxem configurable maps must have String keys.");
			final Type innerType = ((ParameterizedType) target.generic).getActualTypeArguments()[1];
			return span().with(p("Nested:"), documentValuesOf(new Luxem.TypeInfo(innerType), document, shorten));
		} else if (((Class<?>) target.inner).getAnnotation(Luxem.Configuration.class) != null) {
			if (((Class<?>) target.inner).isInterface() ||
					Modifier.isAbstract(((Class<?>) target.inner).getModifiers())) {
				final ContainerTag inner = ul();
				final java.util.Set<String> subclassNames = new HashSet<>();
				Sets
						.difference(reflections.getSubTypesOf((Class<?>) target.inner), ImmutableSet.of(target))
						.stream()
						.map(s -> (Class<?>) s)
						.filter(s -> !Modifier.isAbstract(s.getModifiers()))
						.forEach(s -> {
							String name = Luxem.getConfigurationName(s.getAnnotation(Luxem.Configuration.class));
							if (name == null)
								name = s.getName();
							if (subclassNames.contains(name))
								throw new IllegalArgumentException(String.format(
										"Specific type [%s] of polymorphic type [%s] is ambiguous.",
										name,
										target.inner
								));
							subclassNames.add(name);
							inner.with(li().with(this.documentValuesOf(new Luxem.TypeInfo(s), document, shorten)));
						});
				return span().with(p("One of (specify type):"), inner);
			} else {
				final Constructor<?> constructor;
				try {
					constructor = ((Class<?>) target.inner).getConstructor();
				} catch (final NoSuchMethodException e) {
					throw new AssertionError(String.format(
							"Class [%s] of field marked for luxem serialization has no nullary constructor or constructor is not public.",
							target.inner
					));
				}
				if (constructor != null) {
					final Luxem.Configuration annotation =
							((Class<?>) target.inner).getAnnotation(Luxem.Configuration.class);
					final String name = getConfigurationName(annotation);
					if (!document.containsKey(target.inner)) {
						final ContainerTag section = div();
						document.put(target.inner, section);
						final java.util.Set<Field> fields = new HashSet<>();
						Class<?> level = (Class<?>) target.inner;
						while (level.getSuperclass() != null) {
							Helper
									.stream(level.getDeclaredFields())
									.filter(f -> f.getAnnotation(Luxem.Configuration.class) != null)
									.forEach(f -> {
										if ((f.getModifiers() & Modifier.PUBLIC) == 0)
											throw new AssertionError(String.format("Field %s marked for luxem serialization is not public.",
													f
											));
										fields.add(f);
									});
							level = level.getSuperclass();
						}
						section.with(a("top").withClass("totop").withHref("#top"));
						section.with(a().withName(target.inner.getTypeName()));
						section.with(h2(shorten(shorten, target.inner)));
						if (!annotation.description().isEmpty())
							section.with(p(annotation.description()));
						if (!fields.isEmpty()) {
							final ContainerTag rows = table();
							rows.with(tr().withClass("fields").with(th("Fields"), th()));
							section.with(rows);
							fields.forEach(f -> {
								final Luxem.Configuration fieldAnnotation = f.getAnnotation(Luxem.Configuration.class);
								String fieldName = Luxem.getConfigurationName(fieldAnnotation);
								if (fieldName == null)
									fieldName = f.getName();
								final ContainerTag inner = table().withClass("definitions");
								final ContainerTag cell = td();
								if (!fieldAnnotation.description().equals(""))
									cell.with(p(fieldAnnotation.description()));
								cell.with(inner);
								inner.with(tr().with(td("Values"),
										td().with(documentValuesOf(new Luxem.TypeInfo(f), document, shorten))
								));
								inner.with(tr().with(td("Required"),
										td().with(Luxem.fieldIsRequired(f) ? b("yes") : span("no"))
								));
								if (!Luxem.fieldIsRequired(f) && (
										f.getType() == String.class ||
												f.getType() == int.class ||
												f.getType() == Integer.class ||
												f.getType() == double.class ||
												f.getType() == Double.class ||
												f.getType() == boolean.class ||
												f.getType() == Boolean.class ||
												((Class<?>) f.getType()).isEnum()
								)) {
									final ContainerTag row = tr();
									row.with(td("Default value"));
									final Object value =
											Helper.uncheck(() -> f.get(Helper.uncheck(() -> constructor.newInstance())));
									if (((Class<?>) f.getType()).isEnum()) {
										final Field field =
												Helper.uncheck(() -> ((Class<?>) f.getType()).getField(((Enum<?>) value)
														.name()));
										final Luxem.Configuration config =
												field.getAnnotation(Luxem.Configuration.class);
										String enumName = Luxem.getConfigurationName(config);
										if (enumName == null)
											enumName = value.toString();
										row.with(td().with(code().withText(enumName)));
									} else {
										row.with(td().with(code().withText(String.format("%s", value))));
									}
									inner.with(row);
								}
								rows.with(tr().with(td(fieldName), cell));
							});
						}
					}
					final ContainerTag a = a().withHref(String.format("#%s", target.inner.getTypeName()));
					if (name != null)
						a.with(code().withText(String.format("(%s) ", name)));
					return a.withText(shorten(shorten, target.inner));
				}
			}
		}
		throw new AssertionError(String.format("Undocumentable field of type or derived type [%s]", target.inner));
	}
}
