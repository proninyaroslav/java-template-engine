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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ParseTest
{
	private FuncMap builtins = new FuncMap();

	class TestParse
	{
		String name;
		String input;
		String result;
		boolean hasError;

		public TestParse(String name, String input,
				 String result, boolean hasError)
		{
			this.name = name;
			this.input = input;
			this.result = result;
			this.hasError = hasError;
		}
	}

	class TestNumber
	{
		String text;
		boolean isInt;
		boolean isFloat;
		int intVal;
		double floatVal;

		TestNumber(String text, boolean isInt, boolean isFloat,
			   int intVal, double floatVal)
		{
			this.text = text;
			this.isInt = isInt;
			this.isFloat = isFloat;
			this.intVal = intVal;
			this.floatVal = floatVal;
		}
	}

	@Before
	public void init()
	{
		builtins.put("printf", "format", String.class);
	}

	@Test
	public void testParse()
	{
		testParse(false);
	}

	/**
	 * Same as testParse, but we copy the node first
	 */
	@Test
	public void testParseCopy()
	{
		testParse(true);
	}

	private void testParse(boolean doCopy)
	{
		ArrayList<TestParse> tests = new ArrayList<>();
		tests.add(new TestParse("empty", "", "", false));
		tests.add(new TestParse("comment", "{{/*\n\n\n*/}}", "", false));
		tests.add(new TestParse("spaces", " \t\n", " \t\n", false));
		tests.add(new TestParse("text", "hello world",
					"hello world", false));
		tests.add(new TestParse("field", "{{.x}}", "{{.x}}", false));
		tests.add(new TestParse("simple command", "{{printf}}",
					"{{printf}}", false));
		tests.add(new TestParse("$ invocation", "{{$}}", "{{$}}",
					false));
		tests.add(new TestParse("variable invocation",
					"{{with $x = 3}}{{$x 123}}{{end}}",
					"{{with $x = 3}}{{$x 123}}{{end}}",
					false));
		tests.add(new TestParse("variable with fields", "{{$.x}}",
					"{{$.x}}", false));
		tests.add(new TestParse("multi-word command",
					"{{printf \"%d\" 123}}",
					"{{printf \"%d\" 123}}", false));
		tests.add(new TestParse("pipeline", "{{.x|.y}}",
					"{{.x | .y}}", false));
		tests.add(new TestParse("pipeline with decl", "{{$x = .x|.y}}",
					"{{$x = .x | .y}}", false));
		tests.add(new TestParse("nested pipeline",
					"{{.x (.y .z) (.a | .b .c) (.e)}}",
					"{{.x (.y .z) (.a | .b .c) (.e)}}",
					false));
		tests.add(new TestParse("field applied to parentheses",
					"{{(.x .y).field}}",
					"{{(.x .y).field}}", false));
		tests.add(new TestParse("dot after string",
					"{{\"hello\".length}}",
					"{{\"hello\".length}}", false));
		tests.add(new TestParse("simple if",
					"{{if .x}}hello world{{end}}",
					"{{if .x}}hello world{{end}}", false));
		tests.add(new TestParse("if with else",
					"{{if .x}}true{{else}}false{{end}}",
					"{{if .x}}true{{else}}false{{end}}",
					false));
		tests.add(new TestParse("if with else if",
					"{{if .x}}true{{else if .y}}false{{end}}",
					"{{if .x}}true{{else}}{{if .y}}false{{end}}{{end}}",
					false));
		tests.add(new TestParse("if else chain",
					"+{{if .x}}x{{else if .y}}y{{else if .z}}z{{end}}+",
					"+{{if .x}}x{{else}}{{if .y}}y{{else}}{{if .z}}z{{end}}{{end}}{{end}}+",
					false));
		tests.add(new TestParse("simple for",
					"{{for .x}}hello{{end}}",
					"{{for .x}}hello{{end}}", false));
		tests.add(new TestParse("chained field for",
					"{{for .x.y.z}}hello{{end}}",
					"{{for .x.y.z}}hello{{end}}", false));
		tests.add(new TestParse("for over pipeline",
					"{{for .x|.y}}true{{else}}false{{end}}",
					"{{for .x | .y}}true{{else}}false{{end}}",
					false));
		tests.add(new TestParse("for int[]",
					"{{for .i}}{{.}}{{end}}",
					"{{for .i}}{{.}}{{end}}", false));
		tests.add(new TestParse("for var",
					"{{for $x = .i}}{{.}}{{end}}",
					"{{for $x = .i}}{{.}}{{end}}", false));
		tests.add(new TestParse("for int[] with break",
					"{{for .i}}{{break}}{{.}}{{end}}",
					"{{for .i}}{{break}}{{.}}{{end}}",
					false));
		tests.add(new TestParse("for int[] with break in else",
					"{{for .i}}{{for .i}}{{.}}{{else}}{{break}}{{end}}{{end}}",
					"{{for .i}}{{for .i}}{{.}}{{else}}{{break}}{{end}}{{end}}",
					false));
		tests.add(new TestParse("for int[] with continue",
					"{{for .i}}{{continue}}{{.}}{{end}}",
					"{{for .i}}{{continue}}{{.}}{{end}}",
					false));
		tests.add(new TestParse("constants",
					"{{for .i 1 true false 'a' null}}{{end}}",
					"{{for .i 1 true false 'a' null}}{{end}}",
					false));
		tests.add(new TestParse("template", "{{template \"x\"}}",
					"{{template \"x\"}}", false));
		tests.add(new TestParse("template with arg", "{{template \"x\" .y}}",
					"{{template \"x\" .y}}", false));
		tests.add(new TestParse("with",
					"{{with .x}}hello{{end}}",
					"{{with .x}}hello{{end}}", false));
		tests.add(new TestParse("with with else",
					"{{with .x}}hello{{else}}world{{end}}",
					"{{with .x}}hello{{else}}world{{end}}",
					false));
		/* Errors */
		tests.add(new TestParse("empty action", "{{}}", "{{}}", true));
		tests.add(new TestParse("unclosed action", "hello{{for", "", true));
		tests.add(new TestParse("unmatched else", "{{else}}", "", true));
		tests.add(new TestParse("unmatched else after if",
					"{{if .x}}hello{{end}}{{else}}", "", true));
		tests.add(new TestParse("multiple else",
					"{{if .x}}1{{else}}2{{else}}3{{end}}",
					"", true));
		tests.add(new TestParse("missing end", "hello{{for .x}}", "", true));
		tests.add(new TestParse("missing end after else",
					"hello{{for .x}}{{else}}", "", true));
		tests.add(new TestParse("undefined function",
					"hello{{undefined}}", "", true));
		tests.add(new TestParse("undefined variable", "{{$x}}", "", true));
		tests.add(new TestParse("variable undefined after end",
					"{{with $x = 1}}{{end}}{{$x}}", "", true));
		tests.add(new TestParse("variable undefined in template",
					"{{template $v}}", "", true));
		tests.add(new TestParse("declare with field",
					"{{with $x.y = 1}}{{end}}", "", true));
		tests.add(new TestParse("template with field ref",
					"{{template .x}}", "", true));
		tests.add(new TestParse("template with var",
					"{{template $v}}", "", true));
		tests.add(new TestParse("invalid punctuation",
					"{{printf 1, 2}}", "", true));
		tests.add(new TestParse("dot applied to parentheses",
					"{{printf (printf .).}}", "", true));
		tests.add(new TestParse("adjacent args",
					"{{printf 3`x`}}", "", true));
		tests.add(new TestParse("multiple declaration",
					"{{$x = $y = 1}}{{$x}}", "", true));
		tests.add(new TestParse("dot after integer", "{{1.e}}", "", true));
		tests.add(new TestParse("dot after float", "{{0.1.e}}", "", true));
		tests.add(new TestParse("dot after boolean", "{{true.e}}", "", true));
		tests.add(new TestParse("dot after char", "{{'a'.e}}", "", true));
		tests.add(new TestParse("dot after dot", "{{..e}}", "", true));
		tests.add(new TestParse("dot after null", "{{null.e}}", "", true));
		tests.add(new TestParse("wrong pipeline dot",
					"{{1|.}}", "", true));
		tests.add(new TestParse("wrong pipeline number",
					"{{.|1|printf}}", "", true));
		tests.add(new TestParse("wrong pipeline string",
					"{{.|printf|\"hello\"}}", "", true));
		tests.add(new TestParse("wrong pipeline char",
					"{{1|printf|'a'}}", "", true));
		tests.add(new TestParse("wrong pipeline boolean",
					"{{.|true}}", "", true));
		tests.add(new TestParse("wrong pipeline null",
					"{{'a'|null}}", "", true));
		tests.add(new TestParse("empty pipeline",
					"{{printf \"%d\" ( ) }}", "", true));
		tests.add(new TestParse("break outside of for",
					"{{break}}", "", true));
		tests.add(new TestParse("break in for else, outside of for",
					"{{for .}}{{.}}{{else}}{{break}}{{end}}",
					"", true));
		tests.add(new TestParse("continue outside of for",
					"{{continue}}", "", true));
		tests.add(new TestParse("continue in for else, outside of for",
					"{{for .}}{{.}}{{else}}{{continue}}{{end}}",
					"", true));
		tests.add(new TestParse("additional break data",
					"{{for .}}{{break label}}{{end}}",
					"", true));

		for (TestParse test : tests) {
			Template tmpl = new Template(test.name);
			try {
				HashMap<String, Tree> trees = Tree.parse(test.name, test.input,
									 null, null, builtins);
				for (String name : trees.keySet())
					tmpl.addParseTree(name, trees.get(name));
			} catch (Exception e) {
				if (test.hasError)
					System.out.println(String.format("%s: %s\n\t%s\n", test.name, test.input, e.getMessage()));
				else
					fail(String.format("%s: unexpected error: %s", test.name, e));
				continue;
			}
			if (test.hasError) {
				System.out.println(String.format("%s: expected error; got none", test.name));
				continue;
			}
			String result;
			if (doCopy)
				result = tmpl.tree.root.copy().toString();
			else
				result = tmpl.tree.root.toString();
			assertEquals(String.format("%s=(%s):", test.name, test.input),
				     test.result, result);
		}
	}


	@Test
	public void testNumberParse()
	{
		ArrayList<TestNumber> tests = new ArrayList<>();
		tests.add(new TestNumber("0", true, true, 0, 0));
		tests.add(new TestNumber("-0", true, true, 0, 0));
		tests.add(new TestNumber("123", true, true, 123, 123));
		tests.add(new TestNumber("0123", true, true, 0123, 0123));
		tests.add(new TestNumber("0x123", true, true, 0x123, 0x123));
		tests.add(new TestNumber("-123", true, true, -123, -123));
		tests.add(new TestNumber("+123", true, true, 123, 123));
		tests.add(new TestNumber("123", true, true, 123, 123));
		tests.add(new TestNumber("23", true, true, 23, 23));
		tests.add(new TestNumber("1e9", true, true, (int)1e9, 1e9));
		tests.add(new TestNumber("-1e9", true, true, (int)-1e9, -1e9));
		tests.add(new TestNumber("1.2", false, true, 0, 1.2));
		tests.add(new TestNumber("-1.2", false, true, 0, -1.2));
		tests.add(new TestNumber("1e19", false, true, 0, 1e19));
		tests.add(new TestNumber("-1e19", false, true, 0, -1e19));
		tests.add(new TestNumber("-0x0", true, true, 0, 0));
		tests.add(new TestNumber("'a'", true, true, 'a', 'a'));
		tests.add(new TestNumber("'嗨'", true, true, '嗨', '嗨'));
		tests.add(new TestNumber("'\u00FF'", true, true, '\u00FF', '\u00FF'));
		/* Broken syntax */
		tests.add(new TestNumber("0x123.", false, false, 0, 0));
		tests.add(new TestNumber("+-1", false, false, 0, 0));
		tests.add(new TestNumber("1e.", false, false, 0, 0));
		tests.add(new TestNumber("'x", false, false, 0, 0));
		tests.add(new TestNumber("'xx'", false, false, 0, 0));
		/* Too large integer */
		tests.add(new TestNumber("0xdeadbeef", false, false, 0, 0));

		for (TestNumber test : tests) {
			Token.Type type = Token.Type.NUMBER;
			Tree tree = new Tree("test_number_parse");
			if (test.text.charAt(0) == '\'')
				type = Token.Type.CHAR_CONSTANT;
			boolean ok = test.isInt || test.isFloat;
			Node.Number num = null;
			try {
				 num = tree.newNumber(0, test.text, type);
			} catch (Exception e) {
				if (ok) {
					fail(String.format("unexpected error for %s: %s",
							   test.text, e));
				} else {
					System.out.println(String.format("expected error for %s: %s",
									 test.text, e));
					continue;
				}
			}
			if (test.isInt) {
				if (!num.isInt)
					fail(String.format("expected integer for %s", test.text));
				if (num.intVal != test.intVal)
					fail(String.format("int for %s should be %d Is %d",
							   test.text, test.intVal, num.intVal));
			} else if (num.isInt) {
				fail(String.format("didn't expect integer for %s", test.text));
			}
			if (test.isFloat) {
				if (!num.isFloat)
					fail(String.format("expected float for %s", test.text));
				if (num.floatVal != test.floatVal)
					fail(String.format("double for %s should be %g Is %g",
						test.text, test.floatVal, num.floatVal));
			} else if (num.isFloat) {
				fail(String.format("didn't expect float for %s", test.text));
			}
		}
	}

	@Test
	@Ignore("too slow, only for benchmarking")
	public void benchmarkLargeParse()
	{
		int n = 100, copies = 10000; /* Maybe changed */
		String text = Utils.join("\n", Collections.nCopies(copies, "{{12345}}"));
		for (int i = 0; i < n; i++)
			try {
				Tree.parse("bench_large_parse", text, null, null, builtins);
			} catch (Exception e) {
				fail(e.toString());
			}
	}
}
