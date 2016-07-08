package com.zarbosoft.luxemj;

import com.zarbosoft.pidgoon.Grammar;
import com.zarbosoft.pidgoon.InvalidStream;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

public class ForTypeTest {

	private void check(final Class<?> k, final String source, final Object expected) {
		final Grammar grammar = Luxem.grammarForType(k);
		System.out.println(grammar.toString());
		assertReflectionEquals(expected, new Parse<String>().grammar(grammar).node("root").parse(source));
	}

	@Test
	public void testString() {
		check(String.class, "dog", "dog");
	}

	@Test(expected = InvalidStream.class)
	public void testStringFail() {
		check(String.class, "{}", "dog");
	}

	@Test
	public void testInteger() {
		check(Integer.class, "4007", 4007);
	}

	@Test(expected = InvalidStream.class)
	public void testIntegerFail() {
		check(Integer.class, "hamlet ", 4);
	}

	@Test
	public void testDouble() {
		check(Double.class, "4.7", 4.7);
		check(Double.class, "4", 4.0);
	}

	@Test(expected = InvalidStream.class)
	public void testDoubleFail() {
		check(Double.class, "hamlet ", 4.0);
	}

	@Test
	public void testBoolean() {
		check(Boolean.class, "true", true);
		check(Boolean.class, "false", false);
	}

	@Test(expected = InvalidStream.class)
	public void testBooleanFail() {
		check(Boolean.class, "1 ", false);
	}

	static class Subject {
		public Subject() {
		}

		@Luxem.Configuration
		public List<Integer> a;

		public Subject(final List<Integer> a) {
			this.a = a;
		}
	}

	@Test
	public void testClassAndList() {
		check(Subject.class, "{\"a\": [7, 14]}", new Subject(Arrays.asList(new Integer[] {7, 14})));
	}
}
