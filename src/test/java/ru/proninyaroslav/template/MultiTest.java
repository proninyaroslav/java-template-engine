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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests for multiple-template parsing and execution
 */

public class MultiTest
{
	class TestMultiParse
	{
		String name;
		String input;
		boolean hasError;
		String[] names;
		String[] results;

		public TestMultiParse(String name, String input, boolean hasError,
				      String[] names, String[] results)
		{
			this.name = name;
			this.input = input;
			this.hasError = hasError;
			this.names = names;
			this.results = results;
		}
	}

	@Test
	public void testMultiParse()
	{
		ArrayList<TestMultiParse> tests = new ArrayList<>();
		tests.add(new TestMultiParse("empty", "", false, null, null));
		tests.add(new TestMultiParse("one", "{{define \"foo\"}} FOO {{end}}",
					     false,
					     new String[]{"foo"},
					     new String[]{" FOO "}));
		tests.add(new TestMultiParse("two", "{{define \"foo\"}} FOO {{end}}{{define \"bar\"}} BAR {{end}}",
					     false,
					     new String[]{"foo", "bar"},
					     new String[]{" FOO ", " BAR "}));
		/* Errors */
		tests.add(new TestMultiParse("missing end", "{{define \"foo\"}} FOO ",
					     true, null, null));
		tests.add(new TestMultiParse("malformed name", "{{define \"foo}} FOO ",
					     true, null, null));

		for (TestMultiParse test : tests) {
			Template tmpl = new Template(test.name);
			try {
				tmpl.parse(test.input);
			} catch (Exception e) {
				if (test.hasError)
					System.out.println(String.format("%s: %s\n\t%s\n", test.name, test.input, e));
				else
					fail(String.format("%s: unexpected error: %s", test.name, e));
				continue;
			}
			if (test.hasError) {
				System.out.println(String.format("%s: expected error; got none", test.name));
				continue;
			}
			if (test.names != null && tmpl.common.tmpl.size() != test.names.length + 1)
				fail(String.format("%s: wrong number of templates; wanted %d got %d",
						   test.name, test.names.length,
						   tmpl.common.tmpl.size()));
			if (test.names == null)
				continue;
			for (int i = 0; i < test.names.length; i++) {
				Template t = tmpl.common.tmpl.get(test.names[i]);
				if (t == null)
					fail(String.format("%s: can't find template %s",
							   test.name, test.names[i]));
				String result = t.tree.root.toString();
				assertEquals(String.format("%s=(%s):", test.name, test.input),
							   test.results[i], result);
			}
		}
	}
}
