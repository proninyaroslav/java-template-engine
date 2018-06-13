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

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

public class BuiltinsFuncsTest
{
	class TestCompare
	{
		Object arg1;
		Object[] arg2;
		Object expect;
		String op;

		TestCompare(String op, Object expect, Object arg1, Object... arg2)
		{
			this.arg1 = arg1;
			this.arg2 = arg2;
			this.expect = expect;
			this.op = op;
		}

		@Override
		public String toString()
		{
			return "TestCompare{" +
				"arg1=" + arg1 +
				", arg2=" + Arrays.toString(arg2) +
				", expect=" + expect +
				", op='" + op + '\'' +
				'}';
		}
	}

	class TestArithmetic
	{
		Object a;
		Object b;
		Object expect;
		char op;

		TestArithmetic(Object a, Object b, char op, Object expect)
		{
			this.a = a;
			this.b = b;
			this.expect = expect;
			this.op = op;
		}

		@Override
		public String toString()
		{
			return "TestArithmetic{" +
				"a=" + a +
				", b=" + b +
				", expect=" + expect +
				", op=" + op +
				'}';
		}
	}

	@Test
	public void testCompare()
	{
		ArrayList<TestCompare> exprs = new ArrayList<>();
		exprs.add(new TestCompare("eq", true, true, true));
		exprs.add(new TestCompare("eq", false, true, false));
		exprs.add(new TestCompare("eq", true, 1.5, 1.5));
		exprs.add(new TestCompare("eq", false, 1.5, 2.5));
		exprs.add(new TestCompare("eq", true, 1, 1));
		exprs.add(new TestCompare("eq", false, 1, 2));
		exprs.add(new TestCompare("eq", false, 1.0, 1));
		exprs.add(new TestCompare("eq", true, "xy", "xy"));
		exprs.add(new TestCompare("eq", false, "xy", "xyz"));
		exprs.add(new TestCompare("eq", false, "xy", 1));
		exprs.add(new TestCompare("eq", true, 1, 2, 3, 4, 1));
		exprs.add(new TestCompare("eq", false, 1, 2, 3, 4, 5));
		ArrayList<String> a = new ArrayList<>();
		a.add("test1");
		a.add("test2");
		ArrayList<String> b = new ArrayList<>();
		b.add("test1");
		b.add("test2");
		b.add("test3");
		ArrayList<String> c = new ArrayList<>();
		c.add("test0");
		c.add("test1");
		exprs.add(new TestCompare("eq", true, a, a));
		exprs.add(new TestCompare("eq", false, a, b));
		exprs.add(new TestCompare("eq", false, a, c));
		exprs.add(new TestCompare("ne", false, true, true));
		exprs.add(new TestCompare("ne", true, true, false));
		exprs.add(new TestCompare("ne", false, 1.5, 1.5));
		exprs.add(new TestCompare("ne", true, 1.5, 2.5));
		exprs.add(new TestCompare("ne", false, 1, 1));
		exprs.add(new TestCompare("ne", true, 1, 2));
		exprs.add(new TestCompare("ne", true, 1.0, 1));
		exprs.add(new TestCompare("ne", false, "xy", "xy"));
		exprs.add(new TestCompare("ne", true, "xy", "xyz"));
		exprs.add(new TestCompare("ne", true, "xy", 1));
		exprs.add(new TestCompare("ne", false, a, a));
		exprs.add(new TestCompare("ne", true, a, b));
		exprs.add(new TestCompare("lt", false, 1.5, 1.5));
		exprs.add(new TestCompare("lt", true, 1.5, 2.5));
		exprs.add(new TestCompare("lt", false, 2.5, 1.5));
		exprs.add(new TestCompare("lt", false, 1, 1));
		exprs.add(new TestCompare("lt", true, 1, 2));
		exprs.add(new TestCompare("lt", false, 2, 1));
		exprs.add(new TestCompare("le", true, 1.5, 1.5));
		exprs.add(new TestCompare("le", true, 1.5, 2.5));
		exprs.add(new TestCompare("le", false, 2.5, 1.5));
		exprs.add(new TestCompare("le", true, 1, 1));
		exprs.add(new TestCompare("le", true, 1, 2));
		exprs.add(new TestCompare("le", false, 2, 1));
		exprs.add(new TestCompare("gt", false, 1.5, 1.5));
		exprs.add(new TestCompare("gt", false, 1.5, 2.5));
		exprs.add(new TestCompare("gt", true, 2.5, 1.5));
		exprs.add(new TestCompare("gt", false, 1, 1));
		exprs.add(new TestCompare("gt", false, 1, 2));
		exprs.add(new TestCompare("gt", true, 2, 1));
		exprs.add(new TestCompare("ge", true, 1.5, 1.5));
		exprs.add(new TestCompare("ge", false, 1.5, 2.5));
		exprs.add(new TestCompare("ge", true, 2.5, 1.5));
		exprs.add(new TestCompare("ge", true, 1, 1));
		exprs.add(new TestCompare("ge", false, 1, 2));
		exprs.add(new TestCompare("ge", true, 2, 1));
		/* Errors */
		exprs.add(new TestCompare("lt", null, "xy", "xy"));
		exprs.add(new TestCompare("lt", null, "xy", "xyz"));
		exprs.add(new TestCompare("lt", null, a, a));
		exprs.add(new TestCompare("lt", null, true, true));
		exprs.add(new TestCompare("le", null, "xy", "xy"));
		exprs.add(new TestCompare("le", null, "xy", "xyz"));
		exprs.add(new TestCompare("le", null, a, a));
		exprs.add(new TestCompare("le", null, true, true));
		exprs.add(new TestCompare("gt", null, "xy", "xy"));
		exprs.add(new TestCompare("gt", null, "xy", "xyz"));
		exprs.add(new TestCompare("gt", null, a, a));
		exprs.add(new TestCompare("gt", null, true, true));

		for (TestCompare expr : exprs) {
			try {
				Object result = null;
				switch (expr.op) {
					case "eq":
						result = BuiltinsFuncs.equal(expr.arg1, expr.arg2);
						break;
					case "ne":
						result = BuiltinsFuncs.notEqual(expr.arg1, expr.arg2[0]);
						break;
					case "lt":
						result = BuiltinsFuncs.lessThan(expr.arg1, expr.arg2[0]);
						break;
					case "le":
						result = BuiltinsFuncs.lessThanOrEqual(expr.arg1, expr.arg2[0]);
						break;
					case "gt":
						result = BuiltinsFuncs.greaterThan(expr.arg1, expr.arg2[0]);
						break;
					case "ge":
						result = BuiltinsFuncs.greaterThanOrEqual(expr.arg1, expr.arg2[0]);
						break;
					default:
						fail(String.format("unexpected operator '%s;", expr.op));
				}
				assertEquals(expr.toString(), expr.expect, result);
			} catch (Exception e) {
				if (expr.expect != null)
					fail(String.format("%s: %s", expr, e));
			}
		}
	}

	@Test
	public void testArithmetic()
	{
		ArrayList<TestArithmetic> exprs = new ArrayList<>();
		exprs.add(new TestArithmetic(2, 3, '+', 5));
		exprs.add(new TestArithmetic(2, 3, '-', -1));
		exprs.add(new TestArithmetic(2, 3, '*', 6));
		exprs.add(new TestArithmetic(3, 2, '/', 1));
		exprs.add(new TestArithmetic(2, 3, '%', 2));

		exprs.add(new TestArithmetic((byte) 2, 3, '+', 5));
		exprs.add(new TestArithmetic((byte) 2, 3, '-', -1));
		exprs.add(new TestArithmetic((byte) 2, 3, '*', 6));
		exprs.add(new TestArithmetic((byte) 3, 2, '/', 1));
		exprs.add(new TestArithmetic((byte) 2, 3, '%', 2));

		exprs.add(new TestArithmetic(2, (byte) 3, '+', 5));
		exprs.add(new TestArithmetic(2, (byte) 3, '-', -1));
		exprs.add(new TestArithmetic(2, (byte) 3, '*', 6));
		exprs.add(new TestArithmetic(3, (byte) 2, '/', 1));
		exprs.add(new TestArithmetic(2, (byte) 3, '%', 2));

		exprs.add(new TestArithmetic((short) 2, 3, '+', 5));
		exprs.add(new TestArithmetic((short) 2, 3, '-', -1));
		exprs.add(new TestArithmetic((short) 2, 3, '*', 6));
		exprs.add(new TestArithmetic((short) 3, 2, '/', 1));
		exprs.add(new TestArithmetic((short) 2, 3, '%', 2));

		exprs.add(new TestArithmetic(2, (short) 3, '+', 5));
		exprs.add(new TestArithmetic(2, (short) 3, '-', -1));
		exprs.add(new TestArithmetic(2, (short) 3, '*', 6));
		exprs.add(new TestArithmetic(3, (short) 2, '/', 1));
		exprs.add(new TestArithmetic(2, (short) 3, '%', 2));

		exprs.add(new TestArithmetic((char) 2, 3, '+', 5));
		exprs.add(new TestArithmetic((char) 2, 3, '-', -1));
		exprs.add(new TestArithmetic((char) 2, 3, '*', 6));
		exprs.add(new TestArithmetic((char) 3, 2, '/', 1));
		exprs.add(new TestArithmetic((char) 2, 3, '%', 2));

		exprs.add(new TestArithmetic(2, (char) 3, '+', 5));
		exprs.add(new TestArithmetic(2, (char) 3, '-', -1));
		exprs.add(new TestArithmetic(2, (char) 3, '*', 6));
		exprs.add(new TestArithmetic(3, (char) 2, '/', 1));
		exprs.add(new TestArithmetic(2, (char) 3, '%', 2));

		exprs.add(new TestArithmetic((long) 2, 3, '+', (long) 5));
		exprs.add(new TestArithmetic((long) 2, 3, '-', (long) -1));
		exprs.add(new TestArithmetic((long) 2, 3, '*', (long) 6));
		exprs.add(new TestArithmetic((long) 3, 2, '/', (long) 1));
		exprs.add(new TestArithmetic((long) 2, 3, '%', (long) 2));

		exprs.add(new TestArithmetic(2, (long) 3, '+', (long) 5));
		exprs.add(new TestArithmetic(2, (long) 3, '-', (long) -1));
		exprs.add(new TestArithmetic(2, (long) 3, '*', (long) 6));
		exprs.add(new TestArithmetic(3, (long) 2, '/', (long) 1));
		exprs.add(new TestArithmetic(2, (long) 3, '%', (long) 2));

		exprs.add(new TestArithmetic((long) 2, (long) 3, '+', (long) 5));
		exprs.add(new TestArithmetic((long) 2, (long) 3, '-', (long) -1));
		exprs.add(new TestArithmetic((long) 2, (long) 3, '*', (long) 6));
		exprs.add(new TestArithmetic((long) 3, (long) 2, '/', (long) 1));
		exprs.add(new TestArithmetic((long) 2, (long) 3, '%', (long) 2));

		exprs.add(new TestArithmetic(2.0, 3, '+', 5.0));
		exprs.add(new TestArithmetic(2.0, 3, '-', -1.0));
		exprs.add(new TestArithmetic(2.0, 3, '*', 6.0));
		exprs.add(new TestArithmetic(3.0, 2, '/', 1.5));
		exprs.add(new TestArithmetic(2.0, 3, '%', 2.0));

		exprs.add(new TestArithmetic(2, 3.0, '+', 5.0));
		exprs.add(new TestArithmetic(2, 3.0, '-', -1.0));
		exprs.add(new TestArithmetic(2, 3.0, '*', 6.0));
		exprs.add(new TestArithmetic(3, 2.0, '/', 1.5));
		exprs.add(new TestArithmetic(2, 3.0, '%', 2.0));

		exprs.add(new TestArithmetic(2.0, 3.0, '+', 5.0));
		exprs.add(new TestArithmetic(2.0, 3.0, '-', -1.0));
		exprs.add(new TestArithmetic(2.0, 3.0, '*', 6.0));
		exprs.add(new TestArithmetic(3.0, 2.0, '/', 1.5));
		exprs.add(new TestArithmetic(2.0, 3.0, '%', 2.0));

		exprs.add(new TestArithmetic(2.0, (char) 3, '+', 5.0));
		exprs.add(new TestArithmetic(2.0, (char) 3, '-', -1.0));
		exprs.add(new TestArithmetic(2.0, (char) 3, '*', 6.0));
		exprs.add(new TestArithmetic(3.0, (char) 2, '/', 1.5));
		exprs.add(new TestArithmetic(2.0, (char) 3, '%', 2.0));

		exprs.add(new TestArithmetic((char) 2, 3.0, '+', 5.0));
		exprs.add(new TestArithmetic((char) 2, 3.0, '-', -1.0));
		exprs.add(new TestArithmetic((char) 2, 3.0, '*', 6.0));
		exprs.add(new TestArithmetic((char) 3, 2.0, '/', 1.5));
		exprs.add(new TestArithmetic((char) 2, 3.0, '%', 2.0));

		exprs.add(new TestArithmetic(2.0, (byte) 3, '+', 5.0));
		exprs.add(new TestArithmetic(2.0, (byte) 3, '-', -1.0));
		exprs.add(new TestArithmetic(2.0, (byte) 3, '*', 6.0));
		exprs.add(new TestArithmetic(3.0, (byte) 2, '/', 1.5));
		exprs.add(new TestArithmetic(2.0, (byte) 3, '%', 2.0));

		exprs.add(new TestArithmetic((byte) 2, 3.0, '+', 5.0));
		exprs.add(new TestArithmetic((byte) 2, 3.0, '-', -1.0));
		exprs.add(new TestArithmetic((byte) 2, 3.0, '*', 6.0));
		exprs.add(new TestArithmetic((byte) 3, 2.0, '/', 1.5));
		exprs.add(new TestArithmetic((byte) 2, 3.0, '%', 2.0));

		exprs.add(new TestArithmetic(2.0, (short) 3, '+', 5.0));
		exprs.add(new TestArithmetic(2.0, (short) 3, '-', -1.0));
		exprs.add(new TestArithmetic(2.0, (short) 3, '*', 6.0));
		exprs.add(new TestArithmetic(3.0, (short) 2, '/', 1.5));
		exprs.add(new TestArithmetic(2.0, (short) 3, '%', 2.0));

		exprs.add(new TestArithmetic((short) 2, 3.0, '+', 5.0));
		exprs.add(new TestArithmetic((short) 2, 3.0, '-', -1.0));
		exprs.add(new TestArithmetic((short) 2, 3.0, '*', 6.0));
		exprs.add(new TestArithmetic((short) 3, 2.0, '/', 1.5));
		exprs.add(new TestArithmetic((short) 2, 3.0, '%', 2.0));

		exprs.add(new TestArithmetic(2.0, (long) 3, '+', 5.0));
		exprs.add(new TestArithmetic(2.0, (long) 3, '-', -1.0));
		exprs.add(new TestArithmetic(2.0, (long) 3, '*', 6.0));
		exprs.add(new TestArithmetic(3.0, (long) 2, '/', 1.5));
		exprs.add(new TestArithmetic(2.0, (long) 3, '%', 2.0));

		exprs.add(new TestArithmetic((long) 2, 3.0, '+', 5.0));
		exprs.add(new TestArithmetic((long) 2, 3.0, '-', -1.0));
		exprs.add(new TestArithmetic((long) 2, 3.0, '*', 6.0));
		exprs.add(new TestArithmetic((long) 3, 2.0, '/', 1.5));
		exprs.add(new TestArithmetic((long) 2, 3.0, '%', 2.0));

		exprs.add(new TestArithmetic((float) 2.0, 3, '+', 5.0));
		exprs.add(new TestArithmetic((float) 2.0, 3, '-', -1.0));
		exprs.add(new TestArithmetic((float) 2.0, 3, '*', 6.0));
		exprs.add(new TestArithmetic((float) 3.0, 2, '/', 1.5));
		exprs.add(new TestArithmetic((float) 2.0, 3, '%', 2.0));

		exprs.add(new TestArithmetic(2, (float) 3.0, '+', 5.0));
		exprs.add(new TestArithmetic(2, (float) 3.0, '-', -1.0));
		exprs.add(new TestArithmetic(2, (float) 3.0, '*', 6.0));
		exprs.add(new TestArithmetic(3, (float) 2.0, '/', 1.5));
		exprs.add(new TestArithmetic(2, (float) 3.0, '%', 2.0));

		exprs.add(new TestArithmetic((float) 2.0, 3.0, '+', 5.0));
		exprs.add(new TestArithmetic((float) 2.0, 3.0, '-', -1.0));
		exprs.add(new TestArithmetic((float) 2.0, 3.0, '*', 6.0));
		exprs.add(new TestArithmetic((float) 3.0, 2.0, '/', 1.5));
		exprs.add(new TestArithmetic((float) 2.0, 3.0, '%', 2.0));

		exprs.add(new TestArithmetic(2.0, (float) 3.0, '+', 5.0));
		exprs.add(new TestArithmetic(2.0, (float) 3.0, '-', -1.0));
		exprs.add(new TestArithmetic(2.0, (float) 3.0, '*', 6.0));
		exprs.add(new TestArithmetic(3.0, (float) 2.0, '/', 1.5));
		exprs.add(new TestArithmetic(2.0, (float) 3.0, '%', 2.0));

		exprs.add(new TestArithmetic(0, 0, '+', 0));
		exprs.add(new TestArithmetic(0, 0, '-', 0));
		exprs.add(new TestArithmetic(0, 0, '*', 0));
		exprs.add(new TestArithmetic(0, 0, '/', null));
		exprs.add(new TestArithmetic(0, 0, '%', null));
		exprs.add(new TestArithmetic(2, 0, '/', null));
		exprs.add(new TestArithmetic(2, 0, '%', null));
		exprs.add(new TestArithmetic(2.0, 0, '/', null));
		exprs.add(new TestArithmetic(2.0, 0, '%', null));
		exprs.add(new TestArithmetic((long) 2, 0, '/', null));
		exprs.add(new TestArithmetic((long) 2, 0, '%', null));

		exprs.add(new TestArithmetic("foo", "bar", '+', "foobar"));
		exprs.add(new TestArithmetic("foo", "bar", '-', null));
		exprs.add(new TestArithmetic("foo", "bar", '*', null));
		exprs.add(new TestArithmetic("foo", "bar", '/', null));
		exprs.add(new TestArithmetic("foo", "bar", '%', null));
		exprs.add(new TestArithmetic("foo", 2, '+', null));
		exprs.add(new TestArithmetic("foo", 2, '-', null));
		exprs.add(new TestArithmetic("foo", 2, '*', null));
		exprs.add(new TestArithmetic("foo", 2, '/', null));
		exprs.add(new TestArithmetic("foo", 2, '%', null));
		exprs.add(new TestArithmetic(2, "foo", '+', null));
		exprs.add(new TestArithmetic(2, "foo", '-', null));
		exprs.add(new TestArithmetic(2, "foo", '*', null));
		exprs.add(new TestArithmetic(2, "foo", '/', null));
		exprs.add(new TestArithmetic(2, "foo", '%', null));
		exprs.add(new TestArithmetic("foo", (long) 2, '+', null));
		exprs.add(new TestArithmetic("foo", (long) 2, '-', null));
		exprs.add(new TestArithmetic("foo", (long) 2, '*', null));
		exprs.add(new TestArithmetic("foo", (long) 2, '/', null));
		exprs.add(new TestArithmetic("foo", (long) 2, '%', null));
		exprs.add(new TestArithmetic((long) 2, "foo", '+', null));
		exprs.add(new TestArithmetic((long) 2, "foo", '-', null));
		exprs.add(new TestArithmetic((long) 2, "foo", '*', null));
		exprs.add(new TestArithmetic((long) 2, "foo", '/', null));
		exprs.add(new TestArithmetic((long) 2, "foo", '%', null));
		exprs.add(new TestArithmetic("foo", 2.0, '+', null));
		exprs.add(new TestArithmetic("foo", 2.0, '-', null));
		exprs.add(new TestArithmetic("foo", 2.0, '*', null));
		exprs.add(new TestArithmetic("foo", 2.0, '/', null));
		exprs.add(new TestArithmetic("foo", 2.0, '%', null));
		exprs.add(new TestArithmetic(2.0, "foo", '+', null));
		exprs.add(new TestArithmetic(2.0, "foo", '-', null));
		exprs.add(new TestArithmetic(2.0, "foo", '*', null));
		exprs.add(new TestArithmetic(2.0, "foo", '/', null));
		exprs.add(new TestArithmetic(2.0, "foo", '%', null));

		for (TestArithmetic expr : exprs) {
			try {
				Object result = BuiltinsFuncs.doArithmetic(expr.a, expr.b, expr.op);
				assertEquals(expr.toString(), expr.expect, result);
			} catch (Exception e) {
				if (expr.expect != null)
					fail(String.format("%s: %s", expr, e));
			}
		}
	}
}