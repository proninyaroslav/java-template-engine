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

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuiltinsFuncs
{
	static FuncMap create()
	{
		FuncMap fm = new FuncMap();
		HashMap<String, String> builtins = new HashMap<>();
		builtins.put("range", "range");
		builtins.put("index", "index");

		builtins.put("print", "print");
		builtins.put("println", "println");
		fm.put("printf", "format", String.class);

		builtins.put("add", "add");
		builtins.put("sub", "sub");
		builtins.put("mul", "mul");
		builtins.put("div", "div");
		builtins.put("mod", "mod");

		builtins.put("eq", "equal");
		builtins.put("ne", "notEqual");
		builtins.put("lt", "lessThan");
		builtins.put("le", "lessThanOrEqual");
		builtins.put("gt", "greaterThan");
		builtins.put("ge", "greaterThanOrEqual");

		builtins.put("or", "or");
		builtins.put("and", "and");
		builtins.put("not", "not");

		fm.put(builtins, BuiltinsFuncs.class);

		return fm;
	}

	/**
	 * Generate number sequence from 0 to stop with a given step (default 1)
	 *
	 * @param stop stop value
	 * @return number sequence
	 */
	public static int[] range(int stop)
	{
		return range(0, stop);
	}

	/**
	 * Generate number sequence from start to stop with a given step (default 1)
	 *
	 * @param start start value
	 * @param stop stop value
	 * @return number sequence
	 */
	public static int[] range(int start, int stop)
	{
		return range(start, stop, start < stop ? 1 : -1);
	}

	/**
	 * Generate number sequence from start to stop with a given step
	 *
	 * @param start start value
	 * @param stop stop value
	 * @param step step value
	 * @return number sequence
	 */
	public static int[] range(int start, int stop, int step)
	{
		if (step == 0)
			throw new IllegalArgumentException("step must not be zero");
		if (stop == start || start > stop && step > 0 ||
		    start < stop && step < 0)
			return null;

		int length = (int)Math.ceil((double)Math.abs(stop - start) / (double)Math.abs(step));
		int[] arr = new int[length];
		int n = start;
		for (int i = 0; i < length; i++) {
			arr[i] = n;
			n += step;
		}

		return arr;
	}

	/**
	 * Returns the result of indexing its first argument by
	 * the following arguments, e.g index x 1 2 3 returns x[1][2][3]
	 * (or x.get(1).get(2).get(3) if object is List or Map)
	 *
	 * @param arr array
	 * @param indexes indexes
	 * @return object or value
	 */
	public static Object index(Object arr, Object... indexes)
	{
		if (arr == null)
			throw new IllegalArgumentException("the array/list must not be null");

		Object a = arr;
		for (Object i : indexes) {
			if (a.getClass().isArray())
				a = Array.get(a, (int)i);
			else if (List.class.isInstance(a))
				a = ((List) a).get((int)i);
			else if (Map.class.isInstance(a))
				a = ((Map) a).get(i);
			else
				throw new IllegalArgumentException("can't index object with type " + a.getClass());
		}

		return a;
	}

	/**
	 * Uses the default formats for its arguments and returns the resulting string.
	 * Spaces are added between arguments when neither is a string
	 *
	 * @param args arguments
	 * @return formatted string
	 */
	public static String print(Object... args)
	{
		StringBuilder sb = new StringBuilder();
		int size = args.length;
		for (int i = 0; i < size; i++) {
			sb.append(args[i]);
			if (i != size - 1 && !String.class.isInstance(args[i]))
				sb.append(" ");
		}

		return sb.toString();
	}

	/**
	 * Uses the default formats for its arguments and returns the resulting string.
	 * Spaces are always added between operands and a newline is appended
	 *
	 * @param args arguments
	 * @return formatted string
	 */
	public static String println(Object... args)
	{
		StringBuilder sb = new StringBuilder();
		for (Object arg : args)
			sb.append(arg).append(" ");
		sb.append("\n");

		return sb.toString();
	}

	/**
	 * Evaluates the comparison a == b || a == c || ...
	 *
	 * @param arg1 first value
	 * @param arg2 second value
	 * @return comparison result
	 */
	public static boolean equal(Object arg1, Object... arg2)
	{
		if (arg2.length == 0)
			throw new IllegalArgumentException("can't equal only one argument");

		for (Object arg : arg2)
			if (arg1.equals(arg))
				return true;

		return false;
	}

	/**
	 * Evaluates the comparison a != b
	 *
	 * @param a first value
	 * @param b second value
	 * @return comparison result
	 */
	public static boolean notEqual(Object a, Object b) { return !a.equals(b); }

	/**
	 * Evaluates the comparison a {@literal <} b
	 *
	 * @param a first value
	 * @param b second value
	 * @return comparison result
	 */
	public static boolean lessThan(Object a, Object b) { return compare(a, b, "<"); }

	/**
	 * Evaluates the comparison a {@literal <}= b
	 *
	 * @param a first value
	 * @param b second value
	 * @return comparison result
	 */
	public static boolean lessThanOrEqual(Object a, Object b) { return compare(a, b, "<="); }

	/**
	 * Evaluates the comparison a {@literal >} b
	 *
	 * @param a first value
	 * @param b second value
	 * @return comparison result
	 */
	public static boolean greaterThan(Object a, Object b) { return compare(a, b, ">"); }

	/**
	 * Evaluates the comparison a {@literal >}= b
	 *
	 * @param a first value
	 * @param b second value
	 * @return comparison result
	 */
	public static boolean greaterThanOrEqual(Object a, Object b) { return compare(a, b, ">="); }

	/**
	 * Returns the boolean negation of its argument
	 *
	 * @param a value
	 * @return inversion result
	 */
	public static boolean not(Object a) { return !Utils.isTrue(a); }

	/**
	 * Computes the boolean AND of its arguments, returning
	 * the first false argument it encounters, or the last argument
	 *
	 * @param arg0 first argument
	 * @param args other arguments
	 * @return false argument
	 */
	public static Object and(Object arg0, Object... args)
	{
		if (!Utils.isTrue(arg0))
			return arg0;
		for (Object arg : args) {
			arg0 = arg;
			if (!Utils.isTrue(arg0))
				break;
		}

		return arg0;
	}

	/**
	 * Computes the boolean OR of its arguments, returning
	 * the first true argument it encounters, or the last argument
	 *
	 * @param arg0 first argument
	 * @param args other arguments
	 * @return true argument
	 */
	public static Object or(Object arg0, Object... args)
	{
		if (Utils.isTrue(arg0))
			return arg0;
		for (Object arg : args) {
			arg0 = arg;
			if (Utils.isTrue(arg0))
				break;
		}

		return arg0;
	}

	/**
	 * Evaluates a + b
	 *
	 * @param a first summand
	 * @param b second summand
	 * @return sum
	 */
	public static Object add(Object a, Object b) { return doArithmetic(a, b, '+'); }

	/**
	 * Evaluates a - b
	 *
	 * @param a minuend
	 * @param b subtrahend
	 * @return difference
	 */
	public static Object sub(Object a, Object b) { return doArithmetic(a, b, '-'); }

	/**
	 * Evaluates a * b
	 *
	 * @param a multiplicands
	 * @param b multiplier
	 * @return product
	 */
	public static Object mul(Object a, Object b) { return doArithmetic(a, b, '*'); }

	/**
	 * Evaluates a / b
	 *
	 * @param a dividend
	 * @param b divisor
	 * @return quotient
	 */
	public static Object div(Object a, Object b) { return doArithmetic(a, b, '/'); }

	/**
	 * Evaluates a % b
	 *
	 * @param a dividend
	 * @param b divisor
	 * @return modulo
	 */
	public static Object mod(Object a, Object b) { return doArithmetic(a, b, '%'); }

	public static boolean compare(Object a, Object b, String op)
	{
		String errFmt = String.format("can't apply %s to the values %s (%s) and %s (%s)",
			op, a, a.getClass().getName(), b, b.getClass().getName());

		double ad, bd;
		if (a instanceof Number)
			ad = ((Number)a).doubleValue();
		else if (a instanceof Character)
			ad = ((Character)a);
		else
			throw new IllegalArgumentException(errFmt);

		if (b instanceof Number)
			bd = ((Number)b).doubleValue();
		else if (b instanceof Character)
			bd = ((Character)b);
		else
			throw new IllegalArgumentException(errFmt);

		switch (op) {
			case "<":
				return ad < bd;
			case ">":
				return ad > bd;
			case "<=":
				return ad <= bd;
			case ">=":
				return ad >= bd;
			default:
				throw new IllegalArgumentException(errFmt);
		}
	}

	public static Object doArithmetic(Object a, Object b, char op)
	{
		String errFmt = String.format("can't apply %c to the values %s (%s) and %s (%s)",
			op, a, a.getClass().getName(), b, b.getClass().getName());

		int ai = 0, bi = 0;
		long al = 0, bl = 0;
		double ad = 0, bd = 0;

		if (a instanceof Integer || a instanceof Character ||
			a instanceof Short || a instanceof Byte) {
			ai = (a instanceof Character ? ((Character)a) : ((Number)a).intValue());
			if (b instanceof Integer || b instanceof Short || b instanceof Byte) {
				bi = ((Number)b).intValue();
			} else if (b instanceof Character) {
				bi = ((Character)b);
			} else if (b instanceof Long) {
				al = (long)ai;
				ai = 0;
				bl = (long) b;
			} else if (b instanceof Double || b instanceof Float) {
				ad = ((Number)ai).doubleValue();
				ai = 0;
				bd = ((Number)b).doubleValue();
			} else {
				throw new IllegalArgumentException(errFmt);
			}
		} else if (a instanceof Long) {
			al = (long) a;
			if (b instanceof Integer || b instanceof Long ||
				b instanceof Short || b instanceof Byte) {
				bl = ((Number)b).longValue();
			} else if (b instanceof Character) {
				bl = ((Character)b);
			} else if (b instanceof Double || b instanceof Float) {
				ad = ((Number)al).doubleValue();
				al = 0;
				bd = ((Number)b).doubleValue();
			} else {
				throw new IllegalArgumentException(errFmt);
			}
		} else if (a instanceof Double || a instanceof Float) {
			ad = ((Number)a).doubleValue();
			if (b instanceof Integer || b instanceof Long ||
				b instanceof Short || b instanceof Byte) {
				bd = (double) ((Number)b).longValue();
			} else if (b instanceof Character) {
				bd = ((Character)b);
			} else if (b instanceof Double || b instanceof Float) {
				bd = ((Number)b).doubleValue();
			} else {
				throw new IllegalArgumentException(errFmt);
			}
		} else if (a instanceof String) {
			String as = (String)a;
			if (b instanceof String && op == '+')
				return as + b;
			else
				throw new IllegalArgumentException(errFmt);
		} else {
			throw new IllegalArgumentException(errFmt);
		}

		switch (op) {
			case '+':
				if (ai != 0 || bi != 0)
					return ai + bi;
				else if (al != 0 || bl != 0)
					return al + bl;
				else if (ad != 0 || bd != 0)
					return ad + bd;
				return 0;
			case '-':
				if (ai != 0 || bi != 0)
					return ai - bi;
				else if (al != 0 || bl != 0)
					return al - bl;
				else if (ad != 0 || bd != 0)
					return ad - bd;
				return 0;
			case '*':
				if (ai != 0 || bi != 0)
					return ai * bi;
				else if (al != 0 || bl != 0)
					return al * bl;
				else if (ad != 0 || bd != 0)
					return ad * bd;
				return 0;
			case '/':
				if (bi != 0)
					return ai / bi;
				else if (bl != 0)
					return al / bl;
				else if (bd != 0)
					return ad / bd;
				throw new IllegalArgumentException("can't divide the value by 0");
			case '%':
				if (bi != 0)
					return ai % bi;
				else if (bl != 0)
					return al % bl;
				else if (bd != 0)
					return ad % bd;
				throw new IllegalArgumentException("can't modulo the value by 0");
			default:
				throw new IllegalArgumentException("no such an operation " + op);
		}
	}
}

