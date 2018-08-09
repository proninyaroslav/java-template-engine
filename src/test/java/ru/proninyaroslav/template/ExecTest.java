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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecTest
{
	public class TestExec
	{
		String name;
		String input;
		String output;
		Object data;
		boolean hasError;

		TestExec(String name, String input, String output,
				Object data, boolean hasError)
		{
			this.name = name;
			this.input = input;
			this.output = output;
			this.data = data;
			this.hasError = hasError;
		}
	}

	static class T
	{
		public boolean truth = true;
		public int i = 123;
		public String x = "x";
		public double floatZero;
		/* Nested class */
		public U u = new U("v");
		/* Class with toString() method */
		public V v = new V(123);
		/* Arrays */
		public int[] iArr = new int[]{1, 2, 3};
		public int[] iArrNull;
		public boolean[] bArr = new boolean[]{true, false};
		public List<Integer> iList = newIList();
		/* Maps */
		public Map<String, Integer> siMap = newSiMap();
		public Map<String, Integer> siMapNull;
		/* Template to test evaluation of templates */
		public Template tmpl = newTmpl();
		public ByteArrayOutputStream stream = new ByteArrayOutputStream();
		/* Private field; cannot be accessed by template */
		private int priv;
		public int sameName = 1;

		private List<Integer> newIList()
		{
			List<Integer> list = new ArrayList<>();
			list.add(1); list.add(2); list.add(3);

			return list;
		}

		private Map<String, Integer> newSiMap()
		{
			Map<String, Integer> map = new HashMap<>();
			map.put("one", 1); map.put("two", 2); map.put("three", 3);

			return map;
		}

		private Template newTmpl()
		{
			Template tmpl = new Template("x");
			try {
				tmpl.parse("test template");
			} catch (Exception e) {
				/* Ignore */
			}

			return tmpl;
		}

		public String meth0()
		{
			return "m0";
		}

		public int meth1(int a)
		{
			return a;
		}

		public String meth2(int a, String b)
		{
			return String.format("meth2: %d %s", a, b);
		}

		public String meth3(Object obj)
		{
			return String.format("meth3: %s", obj);
		}

		public static String binaryFunc(String s1, String s2)
		{
			return String.format("[%s=%s]", s1, s2);
		}

		public static String varargsFunc(String... s)
		{
			return "<" + Utils.join("+", s) + ">";
		}

		public static String varargsFuncInt(int i, String... s)
		{
			return Integer.toString(i) + "=<" + Utils.join("+", s) + ">";
		}

		public static String execTemplate(T t) throws Exception
		{
			t.tmpl.execute(t.stream, null);

			return new String(t.stream.toByteArray());
		}

		public int sameName()
		{
			return 1;
		}

		public int sameName(int i)
		{
			return i;
		}
	}

	static class U
	{
		public String v;
		U(String v) { this.v = v; }
	}

	static class V
	{
		public int j;
		V(int j) { this.j = j; }
		@Override
		public String toString() { return "V{" + "j=" + j + '}'; }
	}

	@Test
	public void testExec()
	{
		ArrayList<TestExec> tests = new ArrayList<>();
		T t = new T();
		tests.add(new TestExec("empty", "", "", null, false));
		tests.add(new TestExec("text", "hello world", "hello world",
				       null, false));
		tests.add(new TestExec(".x", "{{.x}}", "x", t, false));
		tests.add(new TestExec(".u.v", "{{.u.v}}", "v", t, false));
		tests.add(new TestExec("map <one>", "{{.siMap.get `one`}}",
				       "1", t, false));
		tests.add(new TestExec("dot int", "{{.}}", "123", 123, false));
		tests.add(new TestExec("dot float", "{{.}}", "1.2", 1.2, false));
		tests.add(new TestExec("dot boolean", "{{.}}", "true", true, false));
		tests.add(new TestExec("dot string", "{{.}}", "hello world",
				       "hello world", false));
		tests.add(new TestExec("dot int", "{{.}}", "123", 123, false));
		tests.add(new TestExec("dot array", "{{.}}", "[1, 2, 3]",
				       new int[]{1, 2, 3}, false));
		tests.add(new TestExec("dot object", "{{.}}", t.toString(),
				       t, false));
		tests.add(new TestExec("$ int", "{{$}}", "123", 123, false));
		tests.add(new TestExec("$.i", "{{$.i}}", "123", t, false));
		tests.add(new TestExec("$.u.v", "{{$.u.v}}", "v", t, false));
		tests.add(new TestExec("declare in action", "{{$x := $.u.v}}{{$x}}",
				       "v", t, false));
		tests.add(new TestExec("simple assignment", "{{$x := 2}}{{$x = 3}}{{$x}}",
				       "3", t, false));
		tests.add(new TestExec("nested assignment",
				       "{{$x := 2}}{{if true}}{{$x = 3}}{{end}}{{$x}}",
				       "3", t, false));
		tests.add(new TestExec("nested assignment changes the last declaration",
				       "{{$x := 1}}{{if true}}{{$x := 2}}{{if true}}{{$x = 3}}{{end}}{{end}}{{$x}}",
				       "1", t, false));
		tests.add(new TestExec("v.toString()", "{{.v}}", t.v.toString(),
				       t, false));
		tests.add(new TestExec(".meth0", "{{.meth0}}", t.meth0(),
				       t, false));
		tests.add(new TestExec(".meth1 123", "{{.meth1 123}}",
				       Integer.toString(t.meth1(123)), t, false));
		tests.add(new TestExec(".meth1 .i", "{{.meth1 .i}}",
				       Integer.toString(t.meth1(t.i)), t, false));
		tests.add(new TestExec(".meth2 1 .x", "{{.meth2 1 .x}}",
				       t.meth2(1, t.x), t, false));
		tests.add(new TestExec(".meth2 1 `test`", "{{.meth2 1 `test`}}",
				       t.meth2(1, "test"), t, false));
		tests.add(new TestExec(".meth3 null", "{{.meth3 null}}",
				       t.meth3(null), t, false));
		tests.add(new TestExec("method on var", "{{if $x := .}}{{$x.meth2 1 $x.x}}{{end}}",
				       t.meth2(1, t.x), t, false));
		tests.add(new TestExec("exec template", "{{execTemplate .}}",
				       "test template", t, false));
		tests.add(new TestExec("binaryFunc", "{{binaryFunc `1` `2`}}",
				       T.binaryFunc("1", "2"), null, false));
		tests.add(new TestExec("varargsFunc0", "{{varargsFunc}}",
				       T.varargsFunc(), null, false));
		tests.add(new TestExec("varargsFunc2", "{{varargsFunc `he` `llo`}}",
				       T.varargsFunc("he", "llo"), null, false));
		tests.add(new TestExec("varargsFuncInt", "{{varargsFuncInt 1 `he` `llo`}}",
				       T.varargsFuncInt(1, "he", "llo"), null, false));
		tests.add(new TestExec("pipeline", "{{.meth0 | .meth2 1}}",
				       t.meth2(1, t.meth0()), t, false));
		tests.add(new TestExec("pipeline func", "{{varargsFunc `llo` | varargsFunc `he`}}",
				       T.varargsFunc("he", T.varargsFunc("llo")),
				       t, false));
		tests.add(new TestExec("parens in pipeline",
				       "{{printf `%d %d %d` (1) (2 | add 3) (add 4 (add 5 6))}}",
				       "1 5 15", null, false));
		tests.add(new TestExec("parens: $ in paren", "{{($).x}}", "x",
				       t, false));
		tests.add(new TestExec("parens: $.u in paren", "{{($.u).v}}", "v",
				       t, false));
		tests.add(new TestExec("parens: $ in paren in pipe",
				       "{{($.x | print).length}}", "1",
				       t, false));
		tests.add(new TestExec("if true", "{{if true}}TRUE{{end}}", "TRUE",
				       null, false));
		tests.add(new TestExec("if false",
				       "{{if false}}TRUE{{else}}FALSE{{end}}",
				       "FALSE", null, false));
		tests.add(new TestExec("if 1",
				       "{{if 1}}NON ZERO{{else}}ZERO{{end}}",
				       "NON ZERO", null, false));
		tests.add(new TestExec("if 0",
				       "{{if 0}}NON ZERO{{else}}ZERO{{end}}",
				       "ZERO", null, false));
		tests.add(new TestExec("if 1.2",
				       "{{if 1.2}}NON ZERO{{else}}ZERO{{end}}",
				       "NON ZERO", null, false));
		tests.add(new TestExec("if 0.0",
				       "{{if 0.0}}NON ZERO{{else}}ZERO{{end}}",
				       "ZERO", null, false));
		tests.add(new TestExec("if empty string",
				       "{{if ``}}NON EMPTY{{else}}EMPTY{{end}}",
				       "EMPTY", null, false));
		tests.add(new TestExec("if string",
				       "{{if `test`}}NON EMPTY{{else}}EMPTY{{end}}",
				       "NON EMPTY", null, false));
		tests.add(new TestExec("if $x with $y int",
				       "{{if $x := true}}{{with $y := .i}}{{$x}},{{$y}}{{end}}{{end}}",
				       "true,123", t, false));
		tests.add(new TestExec("if $x with $x int",
				       "{{if $x := true}}{{with $x := .i}}{{$x}},{{end}}{{$x}}{{end}}",
				       "123,true", t, false));
		tests.add(new TestExec("if else if",
				       "{{if false}}FALSE{{else if true}}TRUE{{end}}",
				       "TRUE", null, false));
		tests.add(new TestExec("if else chain",
				       "{{if eq 1 3}}1{{else if eq 2 3}}2{{else if eq 3 3}}3{{end}}",
				       "3", null, false));
		tests.add(new TestExec("with true",
				       "{{with true}}{{.}}{{end}}", "true",
				       null, false));
		tests.add(new TestExec("with false",
				       "{{with false}}{{.}}{{else}}FALSE{{end}}",
				       "FALSE", null, false));
		tests.add(new TestExec("with 1",
				       "{{with 1}}{{.}}{{else}}ZERO{{end}}",
				       "1", null, false));
		tests.add(new TestExec("with 0",
				       "{{with 0}}{{.}}{{else}}ZERO{{end}}",
				       "ZERO", null, false));
		tests.add(new TestExec("with 1.2",
				       "{{with 1.2}}{{.}}{{else}}ZERO{{end}}",
				       "1.2", null, false));
		tests.add(new TestExec("with 0.0",
				       "{{with 0.0}}{{.}}{{else}}ZERO{{end}}",
				       "ZERO", null, false));
		tests.add(new TestExec("with empty string",
				       "{{with ``}}{{.}}{{else}}EMPTY{{end}}",
				       "EMPTY", null, false));
		tests.add(new TestExec("with string",
				       "{{with `test`}}{{.}}{{else}}EMPTY{{end}}",
				       "test", null, false));
		tests.add(new TestExec("with null arr",
				       "{{with .iArrNull}}{{.}}{{else}}NULL{{end}}",
				       "NULL", t, false));
		tests.add(new TestExec("with arr",
				       "{{with .iArr}}{{.}}{{else}}NULL{{end}}",
				       "[1, 2, 3]", t, false));
		tests.add(new TestExec("with $x int",
				       "{{with $x := .i}}{{$x}}{{end}}", "123",
				       t, false));
		tests.add(new TestExec("with $x .u.v",
				       "{{with $x := $}}{{$x.u.v}}{{end}}", "v",
				       t, false));
		tests.add(new TestExec("with variable and action",
				       "{{with $x := $}}{{$y := $.u.v}}{{$y}}{{end}}",
				       "v", t, false));
		tests.add(new TestExec("for int[]",
				       "{{for .iArr}}-{{.}}-{{end}}",
				       "-1--2--3-", t, false));
		tests.add(new TestExec("for null no else",
				       "{{for .iArrNull}}-{{.}}-{{end}}",
				       "", t, false));
		tests.add(new TestExec("for int[] else",
				       "{{for .iArr}}-{{.}}-{{else}}NULL{{end}}",
				       "-1--2--3-", t, false));
		tests.add(new TestExec("for null else",
				       "{{for .iArrNull}}-{{.}}-{{else}}NULL{{end}}",
				       "NULL", t, false));
		tests.add(new TestExec("for boolean[]",
				       "{{for .bArr}}-{{.}}-{{end}}",
				       "-true--false-", t, false));
		tests.add(new TestExec("for range function",
				       "{{for range 3}}-{{.}}-{{end}}",
				       "-0--1--2-", null, false));
		tests.add(new TestExec("for $x iArr",
				       "{{for $x := .iArr}}<{{$x}}>{{end}}",
				       "<1><2><3>", t, false));
		tests.add(new TestExec("declare in for",
				       "{{for $x := .iArr}}<{{$foo := $x}}{{$x}}>{{end}}",
				       "<1><2><3>", t, false));
		tests.add(new TestExec("for quick break",
				       "{{for .iArr}}{{break}}{{.}}{{end}}",
				       "", t, false));
		tests.add(new TestExec("for break after two",
				       "{{for range 10}}{{if ge . 2}}{{break}}{{end}}-{{.}}-{{end}}",
				       "-0--1-", null, false));
		tests.add(new TestExec("for continue",
				       "{{for .iArr}}{{continue}}{{.}}{{end}}",
				       "", t, false));
		tests.add(new TestExec("for continue condition",
				       "{{for .iArr}}{{if eq . 2}}{{continue}}{{end}}-{{.}}-{{end}}",
				       "-1--3-", t, false));
		/* Errors */
		tests.add(new TestExec("null action", "{{null}}", "", null, true));
		tests.add(new TestExec("private field", "{{.priv}}", "", t, true));
		tests.add(new TestExec("if null", "{{if null}}TRUE{{end}}", "",
				       null, true));
		tests.add(new TestExec("arguments without function", "{{1 2}}",
				       "", null, true));
		tests.add(new TestExec("number var as function", "{{$x := 1}}{{$x 2}}",
				       "", null, true));
		tests.add(new TestExec("number var as function with pipeline",
			               "{{$x := 1}}{{2 | $x}}", "", null, true));
		tests.add(new TestExec("binaryFuncTooFew", "{{binaryFunc `1`}}",
				       "", null, true));
		tests.add(new TestExec("binaryFuncTooMany", "{{binaryFunc `1` `2` `3`}}",
				       "", null, true));
		tests.add(new TestExec("binaryFuncBad0", "{{binaryFunc 1 2}}",
				       "", null, true));
		tests.add(new TestExec("binaryFuncBad1", "{{binaryFunc `1` 2}}",
				       "", null, true));
		tests.add(new TestExec("varargsFuncBad0", "{{varargsFunc 3}}",
				       "", null, true));
		tests.add(new TestExec("varargsFuncIntBad0", "{{varargsFuncInt}}",
				       "", null, true));
		tests.add(new TestExec("varargsFuncIntBad", "{{varargsFuncInt `x`}}",
				       "", null, true));
		tests.add(new TestExec("varargsFuncNullBad", "{{varargsFunc null}}",
				       "", null, true));
		tests.add(new TestExec("field and method with the same name", "{{.sameName}}",
				       null, t, true));
		tests.add(new TestExec("method with the same name as the field but with arguments", "{{.sameName 1}}",
				       null, t, true));

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		FuncMap funcs = new FuncMap();
		HashMap<String, String> map = new HashMap<>();
		map.put("varargsFunc", "varargsFunc");
		map.put("binaryFunc", "binaryFunc");
		map.put("varargsFuncInt", "varargsFuncInt");
		map.put("execTemplate", "execTemplate");
		funcs.put(map, T.class);

		for (TestExec test : tests) {
			Template tmpl = new Template(test.name);
			tmpl.addFuncs(funcs);
			try {
				tmpl.parse(test.input);
			} catch (Exception e) {
				fail(String.format("%s: %s", test.name, e));
			}
			stream.reset();
			try {
				tmpl.execute(stream, test.data);
			} catch (Exception e) {
				if (test.hasError)
					System.out.println(String.format("%s: %s\n\t%s\n", test.name, test.input, e.getMessage()));
				else
					fail(String.format("%s: unexpected error: %s", test.name, e.getMessage()));
				continue;
			}
			if (test.hasError) {
				System.out.println(String.format("%s: expected error; got none", test.name));
				continue;
			}
			String result = new String(stream.toByteArray());
			assertEquals(String.format("%s:", test.name),
						   test.output, result);
		}
	}

	@Test
	public void testDelims()
	{
		String[] delimPairs = new String[]{
			null, null,
			"{{", "}}",
			"<<", ">>",
			"|", "|",
			"(嗨)", "(世)"
		};
		final String hello = "hello world";
		class T
		{
			public String str = hello;
		}
		T val = new T();
		for (int i = 0; i < delimPairs.length; i += 2) {
			String text = ".str";
			String left = delimPairs[i];
			String trueLeft = left;
			String right = delimPairs[i + 1];
			String trueRight = right;
			if (left == null)
				trueLeft = "{{";
			if (right == null)
				trueRight = "}}";
			text = trueLeft + text + trueRight;
			text += trueLeft + "/*comment*/" + trueRight;
			text += trueLeft + "\"" + trueLeft + "\"" + trueRight;
			Template tmpl = new Template("delims");
			tmpl.setDelims(left, right);
			try {
				tmpl.parse(text);
			} catch (Exception e) {
				fail(String.format("delim %s text %s parse err %s",
						   left, text, e));
			}
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			try {
				tmpl.execute(stream, val);
			} catch (Exception e) {
				fail(String.format("delim %s exec err %s",
						   left, e.getMessage()));
			}
			assertEquals(hello + trueLeft, new String(stream.toByteArray()));
		}
	}

	@Test
	public void testMaxExecDepth()
	{
		Template tmpl = new Template("tmpl");
		try {
			tmpl.parse("{{template `tmpl` .}}");
		} catch (Exception e) {
			/* Ignore */
		}
		String got = "<null>";
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
			tmpl.execute(stream, null);
		} catch (Exception e) {
			got = e.toString();
		}
		stream.reset();
		final String want = "exceeded maximum template depth";
		if (!got.contains(want))
			fail(String.format("got error %s; want %s", got, want));
	}
}
