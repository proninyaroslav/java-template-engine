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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

class Utils
{
	static boolean isSpace(char c)
	{
		return c == ' ' || c == '\t';
	}

	static boolean isEndOfLine(char c)
	{
		return c == '\r' || c == '\n';
	}

	static boolean isAlphaNumeric(char c)
	{
		return c == '_' || Character.isLetterOrDigit(c);
	}

	static char unquoteChar(String s, char quote, StringBuilder tail)
	{
		String err = String.format("malformed character constant: %s", s);
		char c = s.charAt(0);
		if (c == quote && (quote == '\'' || quote == '"')) {
			throw new IllegalArgumentException(err);
		} else if (c != '\\') {
			if (tail != null) {
				tail.setLength(0);
				tail.append(s.substring(1, s.length()));
			}
			return c;
		}

		/* Escaped character */
		if (s.length() <= 1)
			throw new IllegalArgumentException(err);
		c = s.charAt(1);
		s = s.substring(2, s.length());
		if (tail != null) {
			tail.setLength(0);
			tail.append(s);
		}

		ArrayList<Integer> chars = new ArrayList<>();
		int val;
		switch (c) {
			case 'n':
				return '\n';
			case 't':
				return '\t';
			case 'b':
				return '\b';
			case 'r':
				return '\r';
			case 'f':
				return '\f';
			case '\\':
				return '\\';
			case '\'':
			case '"':
				if (c != quote)
					throw new IllegalArgumentException(err);
				return c;
			case 'u':
				final int maxLength = 4;
				if (s.length() < maxLength)
					throw new IllegalArgumentException(err);
				val = 0;
				for (int i = 0; i < maxLength; i++) {
					int n;
					try {
						n = unhex(s.charAt(i));
					} catch (IllegalArgumentException e) {
						throw new IllegalArgumentException(err);
					}
					val = (val << maxLength) | n;
				}
				if (val > Character.MAX_VALUE)
					throw new IllegalArgumentException(err);
				if (tail != null) {
					tail.setLength(0);
					tail.append(s.substring(maxLength, s.length()));
				}

				return (char)val;
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
				val = c - '0';
				for (int i = 0; i < s.length(); i++) {
					int n = s.charAt(i) - '0';
					if (n >= 0 && n <= 7)
						chars.add(n);
				}
				int length = chars.size();
				for (int n : chars)
					val = (val << length + 1) | n;
				if (val > Character.MAX_VALUE)
					throw new IllegalArgumentException(err);
				if (tail != null) {
					tail.setLength(0);
					tail.append(s.substring(length, s.length()));
				}

				return (char)val;
			default:
				throw new IllegalArgumentException(err);
		}
	}

	private static int unhex(char c)
	{
		if (c >= '0' && c <= '9')
			return c - '0';
		else if (c >= 'a' && c <= 'f')
			return c - 'a' + 10;
		else if (c >= 'A' && c <= 'F')
			return c - 'A' + 10;

		throw new IllegalArgumentException();
	}

	static String unquote(String s)
	{
		int n = s.length();
		String err = String.format("malformed string constant: %s", s);
		if (n < 2)
			throw new IllegalArgumentException(err);

		char quote = s.charAt(0);
		if (quote != s.charAt(n - 1))
			throw new IllegalArgumentException(err);
		s = s.substring(1, n - 1);

		if (quote == '`') {
			if (s.indexOf('`') >= 0)
				throw new IllegalArgumentException(err);
			if (s.indexOf('\r') >= 0) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < s.length(); i++)
					if (s.charAt(i) != '\r')
						sb.append(s.charAt(i));

				return sb.toString();
			}

			return s;
		}
		if (quote != '"' && quote != '\'')
			throw new IllegalArgumentException(err);
		if (s.indexOf('\n') >= 0)
			throw new IllegalArgumentException(err);

		if (s.indexOf('\\') < 0 && s.indexOf(quote) < 0) {
			switch (quote) {
				case '"':
					return s;
				case '\'':
					if (s.length() == 1)
						return s;
			}
		}

		StringBuilder sb = new StringBuilder();
		while (s.length() > 0) {
			StringBuilder tail = new StringBuilder();
			char c = unquoteChar(s, quote, tail);
			s = tail.toString();
			sb.append(c);
			if (quote == '\'' && s.length() != 0)
				throw new IllegalArgumentException(err);
		}

		return sb.toString();
	}

	/**
	 * returns the string with % replaced by %%, if necessary,
	 * so it can be used safely inside a String.format().
	 */
	static String doublePercent(String s)
	{
		if (s.contains("%"))
			s = s.replaceAll("%", "%%");

		return s;
	}

	static int countChars(String s, char c)
	{
		int n = 0;
		for (int i = 0; i < s.length(); i++)
			if (s.charAt(i) == c)
				++n;

		return n;
	}

	static boolean containsAny(String s, String chars)
	{
		boolean match = false;
		for (char c : chars.toCharArray()) {
			if (s.indexOf(c) >= 0) {
				match = true;
				break;
			}
		}

		return match;
	}

	static boolean isHexConstant(String s)
	{
		return s.length() > 2 && s.charAt(0) == '0' &&
			(s.charAt(1) == 'x' || s.charAt(1) == 'X');
	}

	/**
	 * Reports whether the value is 'true', in the sense of not
	 * the zero of its type, and whether the value has a meaningful truth value.
	 */
	static boolean isTrue(Object val) throws IllegalArgumentException
	{
		if (val == null)
			return false;

		boolean truth = true;
		if (Boolean.class.isInstance(val))
			truth = (Boolean) val;
		else if (String.class.isInstance(val))
			truth = !((String) val).isEmpty();
		else if (Integer.class.isInstance(val))
			truth = ((Integer) val) > 0;
		else if (Double.class.isInstance(val))
			truth = ((Double) val) > 0;
		else if (Long.class.isInstance(val))
			truth = ((Long) val) > 0;
		else if (Collection.class.isInstance(val))
			truth = !((Collection) val).isEmpty();
		else if (Map.class.isInstance(val))
			truth = !((Map) val).isEmpty();
		else if (val.getClass().isArray())
			truth = Array.getLength(val) > 0;
		else if (Float.class.isInstance(val))
			truth = ((Float) val) > 0;
		else if (Short.class.isInstance(val))
			truth = ((Short) val) > 0;
		else if (Byte.class.isInstance(val))
			truth = ((Byte) val) > 0;

		return truth;
	}

	static Map<String, String> filesToString(File... files) throws IOException
	{
		HashMap<String, String> s = new HashMap<>();
		for (File file : files)
			s.put(file.getName(), new String(FileUtils.bytes(file)));

		return s;
	}

	public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements)
	{
		Objects.requireNonNull(delimiter);
		Objects.requireNonNull(elements);
		StringBuilder joiner = new StringBuilder();
		Iterator i = elements.iterator();

		while(i.hasNext()) {
			CharSequence cs = (CharSequence)i.next();
			joiner.append(cs);
			if (i.hasNext())
				joiner.append(delimiter);
		}

		return joiner.toString();
	}

	public static String join(CharSequence delimiter, CharSequence... elements)
	{
		Objects.requireNonNull(delimiter);
		Objects.requireNonNull(elements);
		StringBuilder joiner = new StringBuilder();

		for (int i = 0; i < elements.length; i++) {
			CharSequence cs = elements[i];
			joiner.append(cs);
			if (i + 1 != elements.length)
				joiner.append(delimiter);
		}

		return joiner.toString();
	}
}

