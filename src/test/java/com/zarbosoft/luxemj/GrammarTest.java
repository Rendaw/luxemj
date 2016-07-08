package com.zarbosoft.luxemj;

import org.junit.Test;

public class GrammarTest {

	public void check(final String s) {
		Luxem.parse(null, s);
	}

	@Test
	public void testEmpty() {
		check("");
	}

	@Test
	public void testComment() {
		check("*nothing to \\* see *");
	}

	@Test
	public void testUntyped() {
		check("7");
	}

	@Test
	public void testMultiple() {
		check("7, 4");
	}

	@Test
	public void testEmptyType() {
		check("() 7");
	}

	@Test
	public void testTyped() {
		check("(yaw) 7");
	}

	@Test
	public void testString() {
		check("hi");
	}

	@Test
	public void testQuotedString() {
		check("\"hi log\"");
	}

	@Test
	public void testEmptyObject() {
		check("{}");
	}

	@Test
	public void testObject() {
		check("{1: 2}");
	}

	@Test
	public void testObjectSuffix() {
		check("{1: 2,}");
	}

	@Test
	public void testEmptyArray() {
		check("[]");
	}

	@Test
	public void testArray1() {
		check("[1]");
	}

	@Test
	public void testArray2() {
		check("[1 ,3]");
	}

	@Test
	public void testArraySuffix() {
		check("[1,]");
	}

	@Test
	public void testSpaces() {
		check(" ");
		check(" []");
		check("[] ");
		check("[]");
		check("[],[]");
		check("[], []");
		check("[] ,[]");
		check(" {}");
		check("{} ");
		check("{}");
		check("{},{}");
		check("{}, {}");
		check("{} ,{}");
		check("[ ]");
		check("[1]");
		check("[1 ]");
		check("[ 1]");
		check("[1,]");
		check("[1 ,]");
		check("[1, ]");
		check("[ 1,]");
		check("[1,1]");
		check("[1, 1]");
		check("[1,1 ]");
		check("{a:a}");
		check("{ a:a}");
		check("{a :a}");
		check("{a: a}");
		check("{a:a }");
		check("{ a : a }");
		check("{a:a,}");
		check("{a:a ,}");
		check("{a:a, }");
		check("{ a:a,}");
		check("{a:a,a:a}");
		check("{a:a, a:a}");
		check("{a:a,a:a }");
		check("()");
		check("() ");
		check(" ()");
		check("()1");
		check("() 1");
		check("() 1");
	}
}
