/*
 * Copyright (C) 2018 Yaroslav Pronin <proninyaroslav@mail.ru>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.proninyaroslav.template;

import org.junit.Test;
import ru.proninyaroslav.template.exceptions.InternalException;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LexerTest
{
	private final Token tDot = new Token(Token.Type.DOT, ".");
	private final Token tEOF = new Token(Token.Type.EOF, "");
	private final Token tFor = new Token(Token.Type.FOR, "for");
	private final Token tRange = new Token(Token.Type.IDENTIFIER, "range");
	private final Token tLeftDelim = new Token(Token.Type.LEFT_DELIM, "{{");
	private final Token tRightDelim = new Token(Token.Type.RIGHT_DELIM, "}}");
	private final Token tLeftParen = new Token(Token.Type.LEFT_PAREN, "(");
	private final Token tRightParen = new Token(Token.Type.RIGHT_PAREN, ")");
	private final Token tQuote = new Token(Token.Type.STRING, "\"abc\"");
	private final Token tRawQuote = new Token(Token.Type.RAW_STRING, "`abc`");
	private final Token tRawQuoteNewline = new Token(Token.Type.RAW_STRING, "`hello {{\n}} world`");
	private final Token tSpace = new Token(Token.Type.SPACE, " ");
	private final Token tPipe = new Token(Token.Type.PIPE, "|");

	class TestLex
	{
		String name;
		String input;
		Token[] tokens;

		TestLex(String name, String input, Token... tokens)
		{
			this.name = name;
			this.input = input;
			this.tokens = tokens;
		}
	}

	@Test
	public void testLex()
	{
		ArrayList<TestLex> tests = new ArrayList<>();
		tests.add(new TestLex("empty", "", tEOF));
		tests.add(new TestLex("spaces", " \n\t",
				    new Token(Token.Type.TEXT, " \n\t"),
				    tEOF));
		tests.add(new TestLex("text", "hello world",
				    new Token(Token.Type.TEXT, "hello world"),
				    tEOF));
		tests.add(new TestLex("text with comment", "hello {{/*comment*/}} world",
				    new Token(Token.Type.TEXT, "hello "),
			            new Token(Token.Type.TEXT, " world"),
				    tEOF));
		tests.add(new TestLex("punctuation", "{{,@% }}",
				    tLeftDelim,
				    new Token(Token.Type.CHAR, ","),
			            new Token(Token.Type.CHAR, "@"),
			            new Token(Token.Type.CHAR, "%"),
				    tSpace,
				    tRightDelim,
				    tEOF));
		tests.add(new TestLex("parens", "{{((5))}}",
				    tLeftDelim,
			            tLeftParen,
			            tLeftParen,
			            new Token(Token.Type.NUMBER, "5"),
				    tRightParen,
				    tRightParen,
				    tRightDelim,
				    tEOF));
		tests.add(new TestLex("empty action", "{{}}",
				    tLeftDelim, tRightDelim, tEOF));
		tests.add(new TestLex("range", "{{range}}",
				    tLeftDelim, tRange, tRightDelim, tEOF));
		tests.add(new TestLex("quote", "{{\"abc\"}}",
				    tLeftDelim, tQuote, tRightDelim, tEOF));
		tests.add(new TestLex("raw quote", "{{`abc`}}",
				    tLeftDelim, tRawQuote, tRightDelim, tEOF));
		tests.add(new TestLex("raw quote with newline", "{{`hello {{\n}} world`}}",
				    tLeftDelim, tRawQuoteNewline, tRightDelim, tEOF));
		tests.add(new TestLex("numbers", "{{1 1.2 02 0x1f 0xff 1e3 +1.2e-4}}",
				    tLeftDelim,
				    new Token(Token.Type.NUMBER, "1"),
			            tSpace,
			            new Token(Token.Type.NUMBER, "1.2"),
			            tSpace,
				    new Token(Token.Type.NUMBER, "02"),
			            tSpace,
				    new Token(Token.Type.NUMBER, "0x1f"),
			            tSpace,
				    new Token(Token.Type.NUMBER, "0xff"),
			            tSpace,
				    new Token(Token.Type.NUMBER, "1e3"),
			            tSpace,
				    new Token(Token.Type.NUMBER, "+1.2e-4"),
				    tRightDelim,
				    tEOF));
		tests.add(new TestLex("characters", "{{'a' '\u00FF' '嗨'}}",
				    tLeftDelim,
				    new Token(Token.Type.CHAR_CONSTANT, "'a'"),
			            tSpace,
			            new Token(Token.Type.CHAR_CONSTANT, "'\u00FF'"),
			            tSpace,
				    new Token(Token.Type.CHAR_CONSTANT, "'嗨'"),
				    tRightDelim,
				    tEOF));
		tests.add(new TestLex("booleans", "{{true false}}",
				    tLeftDelim,
				    new Token(Token.Type.BOOL, "true"),
			            tSpace,
			            new Token(Token.Type.BOOL, "false"),
				    tRightDelim,
				    tEOF));
		tests.add(new TestLex("dot", "{{.}}",
				    tLeftDelim, tDot, tRightDelim, tEOF));
		tests.add(new TestLex("null", "{{null}}",
				    tLeftDelim,
				    new Token(Token.Type.NULL, "null"),
				    tRightDelim,
				    tEOF));
		tests.add(new TestLex("dots", "{{.x . .1 .x.y.z}}",
				    tLeftDelim,
				    new Token(Token.Type.FIELD, ".x"),
			            tSpace,
			            tDot,
			            tSpace,
				    new Token(Token.Type.NUMBER, ".1"),
				    tSpace,
				    new Token(Token.Type.FIELD, ".x"),
				    new Token(Token.Type.FIELD, ".y"),
				    new Token(Token.Type.FIELD, ".z"),
				    tRightDelim,
				    tEOF));
		tests.add(new TestLex("keywords", "{{for if else end with break continue}}",
				    tLeftDelim,
				    tFor,
				    tSpace,
				    new Token(Token.Type.IF, "if"),
				    tSpace,
				    new Token(Token.Type.ELSE, "else"),
				    tSpace,
				    new Token(Token.Type.END, "end"),
				    tSpace,
				    new Token(Token.Type.WITH, "with"),
				    tSpace,
				    new Token(Token.Type.BREAK, "break"),
				    tSpace,
				    new Token(Token.Type.CONTINUE, "continue"),
				    tRightDelim,
				    tEOF));
		tests.add(new TestLex("variables", "{{$a = range $ $hello $123 $ $var.field .method}}",
				    tLeftDelim,
				    new Token(Token.Type.VARIABLE, "$a"),
				    tSpace,
				    new Token(Token.Type.EQUALS, "="),
				    tSpace,
				    tRange,
				    tSpace,
				    new Token(Token.Type.VARIABLE, "$"),
				    tSpace,
				    new Token(Token.Type.VARIABLE, "$hello"),
				    tSpace,
				    new Token(Token.Type.VARIABLE, "$123"),
				    tSpace,
				    new Token(Token.Type.VARIABLE, "$"),
				    tSpace,
				    new Token(Token.Type.VARIABLE, "$var"),
				    new Token(Token.Type.FIELD, ".field"),
				    tSpace,
				    new Token(Token.Type.FIELD, ".method"),
				    tRightDelim,
				    tEOF));
		tests.add(new TestLex("variable invocation", "{{$x 123}}",
				    tLeftDelim,
				    new Token(Token.Type.VARIABLE, "$x"),
				    tSpace,
				    new Token(Token.Type.NUMBER, "123"),
				    tRightDelim,
				    tEOF));
		tests.add(new TestLex("variables", "intro {{print hi 1.2 |noargs|args 1 \"hi\"}} outro",
				    new Token(Token.Type.TEXT, "intro "),
				    tLeftDelim,
				    new Token(Token.Type.IDENTIFIER, "print"),
				    tSpace,
				    new Token(Token.Type.IDENTIFIER, "hi"),
				    tSpace,
				    new Token(Token.Type.NUMBER, "1.2"),
				    tSpace,
				    tPipe,
				    new Token(Token.Type.IDENTIFIER, "noargs"),
				    tPipe,
				    new Token(Token.Type.IDENTIFIER, "args"),
				    tSpace,
				    new Token(Token.Type.NUMBER, "1"),
				    tSpace,
				    new Token(Token.Type.STRING, "\"hi\""),
				    tRightDelim,
				    new Token(Token.Type.TEXT, " outro"),
				    tEOF));
		tests.add(new TestLex("declaration", "{{$v = 1}}",
				    tLeftDelim,
				    new Token(Token.Type.VARIABLE, "$v"),
				    tSpace,
				    new Token(Token.Type.EQUALS, "="),
				    tSpace,
				    new Token(Token.Type.NUMBER, "1"),
				    tRightDelim,
				    tEOF));
		tests.add(new TestLex("field of parenthesized expression", "{{(.x).y}}",
				    tLeftDelim,
				    tLeftParen,
				    new Token(Token.Type.FIELD, ".x"),
				    tRightParen,
				    new Token(Token.Type.FIELD, ".y"),
				    tRightDelim,
				    tEOF));
		/* Errors */
		tests.add(new TestLex("unclosed action", "{{\n}}",
				    tLeftDelim,
				    new Token(Token.Type.ERROR, "unclosed action")));
		tests.add(new TestLex("EOF in action", "{{for",
				    tLeftDelim,
				    tFor,
				    new Token(Token.Type.ERROR, "unclosed action")));
		tests.add(new TestLex("unclosed quote", "{{\"\n\"}}",
				    tLeftDelim,
				    new Token(Token.Type.ERROR, "unterminated quoted string")));
		tests.add(new TestLex("unclosed raw quote", "{{`abc}}",
				    tLeftDelim,
				    new Token(Token.Type.ERROR, "unterminated raw quoted string")));
		tests.add(new TestLex("unclosed char constant", "{{'\n}}",
				    tLeftDelim,
				    new Token(Token.Type.ERROR, "unterminated character constant")));
		tests.add(new TestLex("bad number", "{{5k}}",
				    tLeftDelim,
				    new Token(Token.Type.ERROR, "bad number syntax: '5k'")));
		tests.add(new TestLex("unclosed paren", "{{(5}}",
				    tLeftDelim,
				    tLeftParen,
				    new Token(Token.Type.NUMBER, "5"),
				    new Token(Token.Type.ERROR, "unclosed left paren")));
		tests.add(new TestLex("extra right paren", "{{5)}}",
				    tLeftDelim,
				    new Token(Token.Type.NUMBER, "5"),
				    tRightParen,
				    new Token(Token.Type.ERROR, "unexpected right paren )")));
		tests.add(new TestLex("long pipeline deadlock", "{{|||||}}",
				    tLeftDelim,
				    tPipe,
				    tPipe,
				    tPipe,
				    tPipe,
				    tPipe,
				    tRightDelim,
				    tEOF));
		tests.add(new TestLex("bad comment", "hello {{/*/}} world",
				    new Token(Token.Type.TEXT, "hello "),
				    new Token(Token.Type.ERROR, "unclosed comment")));
		tests.add(new TestLex("text with comment close separated from delim", "hello {{/* */ }} world",
				    new Token(Token.Type.TEXT, "hello "),
				    new Token(Token.Type.ERROR, "comment ends before closing delimiter")));
		tests.add(new TestLex("unmatched right delimiter", "hello {.}} world",
				    new Token(Token.Type.TEXT, "hello {.}} world"),
				    tEOF));

		for (TestLex test : tests) {
			try {
				Token[] tokens = collect(test, null, null);
				assertTrue(String.format("%s:\nExpected :%s\nActual   :%s",
							 test.name, Arrays.toString(test.tokens),
							 Arrays.toString(tokens)),
							 equal(tokens, test.tokens, false));
			} catch (Exception e) {
				fail(String.format("%s: %s", test.name, e));
			}
		}
	}

	@Test
	public void testPos()
	{
		ArrayList<TestLex> tests = new ArrayList<>();
		tests.add(new TestLex("empty", "",
				    new Token(Token.Type.EOF, 0, "", 1)));
		tests.add(new TestLex("punctuation", "{{,@%#}}",
				    new Token(Token.Type.LEFT_DELIM, 0, "{{", 1),
				    new Token(Token.Type.CHAR, 2, ",", 1),
				    new Token(Token.Type.CHAR, 3, "@", 1),
				    new Token(Token.Type.CHAR, 4, "%", 1),
				    new Token(Token.Type.CHAR, 5, "#", 1),
				    new Token(Token.Type.RIGHT_DELIM, 6, "}}", 1),
				    new Token(Token.Type.EOF, 8, "", 1)));
		tests.add(new TestLex("sample", "0123\n{{hello}}\nxyz",
				    new Token(Token.Type.TEXT, 0, "0123\n", 1),
				    new Token(Token.Type.LEFT_DELIM, 5, "{{", 2),
				    new Token(Token.Type.IDENTIFIER, 7, "hello", 2),
				    new Token(Token.Type.RIGHT_DELIM, 12, "}}", 2),
				    new Token(Token.Type.TEXT, 14, "\nxyz", 2),
				    new Token(Token.Type.EOF, 18, "", 3)));

		for (TestLex test : tests) {
			boolean fail = false;
			try {
				Token[] tokens = collect(test, null, null);
				if (!equal(tokens, test.tokens, true)) {
					fail = true;
					System.out.println(String.format("%s:\nExpected :%s\nActual   :%s",
									 test.name, Arrays.toString(test.tokens),
									 Arrays.toString(tokens)));
					if (tokens.length == test.tokens.length) {
						for (int i = 0; i < tokens.length; i++) {
							if (!equal(Arrays.copyOfRange(tokens, i, i + 1),
								   Arrays.copyOfRange(test.tokens, i, i + 1), true)) {
								Token t1 = test.tokens[i];
								Token t2 = tokens[i];
								System.out.println(String.format("%d:\nExpected :{%s %d %s %d}\nActual   :{%s %d %s %d}",
												 i, t1.type, t1.pos, t1.val, t1.line,
												 t2.type, t2.pos, t2.val, t2.line));
							}
						}
					}
				}
			} catch (Exception e) {
				fail(String.format("%s: %s", test.name, e));
			}
			if (fail)
				fail();
		}
	}

	@Test
	public void testDelim()
	{
		final Token tLeftDelim = new Token(Token.Type.LEFT_DELIM, "$$");
		final Token tRightDelim = new Token(Token.Type.RIGHT_DELIM, "@@");

		ArrayList<TestLex> tests = new ArrayList<>();
		tests.add(new TestLex("punctuation", "$$,@%{{}}@@",
				    tLeftDelim,
				    new Token(Token.Type.CHAR, ","),
				    new Token(Token.Type.CHAR, "@"),
				    new Token(Token.Type.CHAR, "%"),
				    new Token(Token.Type.CHAR, "{"),
				    new Token(Token.Type.CHAR, "{"),
				    new Token(Token.Type.CHAR, "}"),
				    new Token(Token.Type.CHAR, "}"),
				    tRightDelim,
				    tEOF));
		tests.add(new TestLex("empty action", "$$@@",
				    tLeftDelim, tRightDelim, tEOF));
		tests.add(new TestLex("for", "$$for@@",
				    tLeftDelim, tFor, tRightDelim, tEOF));
		tests.add(new TestLex("quote", "$$\"abc\"@@",
				    tLeftDelim, tQuote, tRightDelim, tEOF));
		tests.add(new TestLex("raw quote", "$$`abc`@@",
				    tLeftDelim, tRawQuote, tRightDelim, tEOF));

		for (TestLex test : tests) {
			try {
				Token[] tokens = collect(test, "$$", "@@");
				assertTrue(String.format("%s:\nExpected :%s\nActual   :%s",
							 test.name, Arrays.toString(test.tokens),
							 Arrays.toString(tokens)),
							 equal(tokens, test.tokens, false));
			} catch (Exception e) {
				fail(String.format("%s: %s", test.name, e));
			}
		}
	}

	private boolean equal(Token[] t1, Token[] t2, boolean checkPos)
	{
		if (t1 == null || t2 == null || t1.length != t2.length)
			return false;

		for (int i = 0; i < t1.length; i++) {
			if (t1[i].type != t2[i].type)
				return false;
			if (!t1[i].val.equals(t2[i].val))
				return false;
			if (checkPos && t1[i].pos != t2[i].pos)
				return false;
			if (checkPos && t1[i].line != t2[i].line)
				return false;
		}

		return true;
	}

	private Token[] collect(TestLex test, String leftDelim, String rightDelim) throws InternalException
	{
		Lexer lex = new Lexer(test.name, test.input, leftDelim, rightDelim);
		ArrayList<Token> tokens = new ArrayList<>();
		for (;;) {
			Token token = lex.nextToken();
			tokens.add(token);
			if (token.type == Token.Type.EOF ||
			    token.type == Token.Type.ERROR)
				break;
		}

		return tokens.toArray(new Token[tokens.size()]);
	}
}