import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.Test;

public class LuxemTests {

	public void check(String s) {
		luxemLexer lexer = new luxemLexer(new ANTLRInputStream(s));
		luxemParser parser = new luxemParser(new CommonTokenStream(lexer));
		parser.setErrorHandler(new BailErrorStrategy());
		try {
			parser.root();
		} catch (ParseCancellationException e) {
			if (e.getCause() instanceof RecognitionException) {
				RecognitionException re = (RecognitionException) e.getCause();
				ParserRuleContext context = (ParserRuleContext) re.getCtx();
				throw new AssertionError(
					String.format(
						"Parse bailed at line %d:%d: expected %s in %s",
						context.start.getLine(),
						context.start.getCharPositionInLine(),
						re.getExpectedTokens().toString(parser.getVocabulary()),
						context.toStringTree(parser)),
					re);
			}
			throw new AssertionError("Parse bailed", e.getCause());
		}
		/*new ParseTreeWalker().walk(
		 new luxemBaseListener() {
		 @Override
		 public void visitErrorNode(@NotNull ErrorNode node) {
		 errors.add("end");
		 throw new AssertionError(String.format(
		 "Parse failed at %s:\n%s",
		 node.toString(),
		 errors.stream().collect(Collectors.joining("\n"))));
		 }
		 },
		 new luxemParser(new CommonTokenStream(lexer)).root()
		 );*/
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
}
