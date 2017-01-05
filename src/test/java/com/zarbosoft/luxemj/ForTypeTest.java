package com.zarbosoft.luxemj;

import com.zarbosoft.pidgoon.Grammar;
import com.zarbosoft.pidgoon.InvalidStream;
import org.junit.Test;

import java.util.*;

import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

public class ForTypeTest {

	private void check(final Class<?> k, final String source, final Object expected) {
		final Grammar grammar = Luxem.grammarForType(k);
		assertReflectionEquals(expected,
				new Parse<String>().grammar(grammar).errorHistory(5).node("root").parse(source)
		);
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
		check(Integer.class, "hamlet,", 4);
	}

	@Test
	public void testDouble() {
		check(Double.class, "4.7", 4.7);
		check(Double.class, "4", 4.0);
	}

	@Test(expected = InvalidStream.class)
	public void testDoubleFail() {
		check(Double.class, "hamlet,", 4.0);
	}

	@Test
	public void testBoolean() {
		check(Boolean.class, "true", true);
		check(Boolean.class, "false", false);
	}

	@Test(expected = InvalidStream.class)
	public void testBooleanFail() {
		check(Boolean.class, "1,", false);
	}

	@Luxem.Configuration
	static class Subject {
		public Subject() {
		}

		@Luxem.Configuration
		public List<Integer> a;

		public Subject(final List<Integer> a) {
			this.a = a;
		}
	}

	@Luxem.Configuration
	static class Subject2 {
		public Subject2() {
		}
	}

	@Luxem.Configuration
	static class Subject3 {
		public Subject3() {
		}

		@Luxem.Configuration
		public Set<Integer> a;

		public Subject3(final Set<Integer> a) {
			this.a = a;
		}
	}

	@Luxem.Configuration
	static class Subject4 {
		public Subject4() {
		}

		@Luxem.Configuration
		public Map<String, Integer> a;

		public Subject4(final Map<String, Integer> a) {
			this.a = a;
		}
	}

	@Luxem.Configuration
	static class Subject5 {
		public Subject5() {
		}

		@Luxem.Configuration
		public String a;

		public Subject5(final String a) {
			this.a = a;
		}
	}

	@Test
	public void testClassAndList() {
		check(Subject.class, "{\"a\": [7, 14]}", new Subject(Arrays.asList(new Integer[] {7, 14})));
	}

	@Test
	public void testOptionalList() {
		check(Subject.class, "{}", new Subject(Arrays.asList(new Integer[] {})));
	}

	@Test
	public void testClassAndSet() {
		check(Subject3.class, "{\"a\": [7, 14]}", new Subject3(new HashSet<>(Arrays.asList(7, 14))));
	}

	@Test
	public void testClassAndMap() {
		final HashMap<String, Integer> data = new HashMap<>();
		data.put("h", 7);
		data.put("q", 12);
		check(Subject4.class, "{\"a\": {h:7, q:12}}", new Subject4(data));
	}

	@Test
	public void testOptionalMap() {
		check(Subject4.class, "{}", new Subject4(new HashMap<>()));
	}

	@Test
	public void test1FieldAbbreviation() {
		check(Subject.class, "[7, 14]", new Subject(Arrays.asList(new Integer[] {7, 14})));
	}

	@Test
	public void test0FieldClass() {
		check(Subject2.class, "{}", new Subject2());
	}

	@Luxem.Configuration
	static class Outer5 {
		@Luxem.Configuration
		public List<Subject5> data;

		public Outer5(final List<Subject5> q) {
			data = q;
		}

		public Outer5() {

		}
	}

	@Test
	public void testRepeatedAbbreviations() {
		check(Outer5.class,
				"{data:[q,q,q,q,q,q,q,q,q,q,q,q,q,q,q,q,q,q,q,q]}",
				new Outer5(Arrays.asList(new Subject5[] {
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q")
				}))
		);
	}

	@Luxem.Configuration
	public static abstract class Subject6 {
	}

	@Luxem.Configuration(name = "6a")
	public static class Subject6A extends Subject6 {
		public Subject6A() {
		}

		@Luxem.Configuration
		public String a;

		@Luxem.Configuration(optional = true)
		public String b;
		@Luxem.Configuration(optional = true)
		public String c;

		public Subject6A(final String a) {
			this.a = a;
		}
	}

	@Luxem.Configuration(name = "6b")
	public static class Subject6B extends Subject6 {
		public Subject6B() {
		}

		@Luxem.Configuration
		public String a;

		public Subject6B(final String a) {
			this.a = a;
		}
	}

	@Luxem.Configuration(name = "6c")
	public static class Subject6C extends Subject6 {
		public Subject6C() {
		}

		@Luxem.Configuration
		public String a;

		public Subject6C(final String a) {
			this.a = a;
		}
	}

	@Luxem.Configuration
	public static class Outer6 {
		@Luxem.Configuration
		public List<Subject6> data;

		public Outer6(final List<Subject6> q) {
			data = q;
		}

		public Outer6() {

		}
	}

	@Test
	public void testRepeatedPolymorphicAbbreviations() {
		check(
				Outer6.class,
				"{data:[(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q]}",
				new Outer6(Arrays.asList(new Subject6[] {
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q")
				}))
		);
	}
}
