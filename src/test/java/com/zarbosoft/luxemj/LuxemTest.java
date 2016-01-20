package com.zarbosoft.luxemj;
import java.io.IOException;

import org.junit.Test;

import junit.framework.TestCase;

public class LuxemTest extends TestCase {

	public void check(String s) throws IOException {
		Luxem.parse(null, s);
	}

	@Test
	public void testEmpty() throws IOException {
		check("");
	}

	@Test
	public void testComment() throws IOException {
		check("*nothing to \\* see *");
	}

	@Test
	public void testUntyped() throws IOException {
		check("7");
	}

	@Test
	public void testMultiple() throws IOException {
		check("7, 4");
	}

	@Test
	public void testEmptyType() throws IOException {
		check("() 7");
	}

	@Test
	public void testTyped() throws IOException {
		check("(yaw) 7");
	}

	@Test
	public void testString() throws IOException {
		check("hi");
	}

	@Test
	public void testQuotedString() throws IOException {
		check("\"hi log\"");
	}

	@Test
	public void testEmptyObject() throws IOException {
		check("{}");
	}

	@Test
	public void testObject() throws IOException {
		check("{1: 2}");
	}

	@Test
	public void testObjectSuffix() throws IOException {
		check("{1: 2,}");
	}

	@Test
	public void testEmptyArray() throws IOException {
		check("[]");
	}

	@Test
	public void testArray1() throws IOException {
		check("[1]");
	}

	@Test
	public void testArray2() throws IOException {
		check("[1 ,3]");
	}

	@Test
	public void testArraySuffix() throws IOException {
		check("[1,]");
	}
}
