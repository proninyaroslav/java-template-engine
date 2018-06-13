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

import ru.proninyaroslav.template.exceptions.ExecException;
import ru.proninyaroslav.template.exceptions.InternalException;
import ru.proninyaroslav.template.exceptions.ParseException;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Representation of a parsed template
 */

public class Template
{
	String name;
	private String leftDelim;
	private String rightDelim;
	Tree tree;
	Common common;

	/**
	 * Holds the information shared by related templates
	 */
	class Common
	{
		FuncMap funcs;
		/* Protects funcs */
		final ReentrantLock funcsLock = new ReentrantLock();
		HashMap<String, Template> tmpl;

		Common()
		{
			tmpl = new HashMap<>();
			funcs = new FuncMap();
		}
	}

	/**
	 * Holds the dynamic value of a variable
	 */
	static class Variable
	{
		String name;
		Object value;

		Variable(String name, Object value)
		{
			this.name = name;
			this.value = value;
		}
	}

	public Template(String name)
	{
		this.name = name;
		common = new Common();
	}

	/**
	 * Allocates a new, undefined template associated with the
	 * given one and with the same delimiters. The association allows one
	 * template to invoke another with a {template} action
	 *
	 * @param name template name
	 * @param parent parent
	 */
	public Template(String name, Template parent)
	{
		this.name = name;
		common = parent.common;
		leftDelim = parent.leftDelim;
		rightDelim = parent.rightDelim;
	}

	/**
	 * Named template definition ({define ...}) in text
	 * define additional templates associated with
	 * template and are removed from the definition of template itself.
	 * Templates can be redefined in successive calls to parse.
	 * A template definition with a body containing only
	 * white space and comments is considered empty and
	 * will not replace an existing template's body.
	 *
	 * @param text template text
	 * @throws InternalException in case of a internal error
	 * @throws ParseException in case of an parse error
	 */
	public void parse(String text) throws InternalException, ParseException
	{
		HashMap<String, Tree> trees;
		common.funcsLock.lock();
		try {
			trees = Tree.parse(name, text, leftDelim,
					   rightDelim, common.funcs, FuncMap.builtins);
		} finally {
			common.funcsLock.unlock();
		}

		for (String name : trees.keySet())
			addParseTree(name, trees.get(name));
	}

	public void parse(InputStream input) throws InternalException, ParseException, IOException
	{
		parse(new String(FileUtils.toByteArray(input)));
	}

	/**
	 * Create templates from the file list
	 *
	 * @param funcs functions
	 * @param files template files
	 * @throws ParseException in case of a parse error
	 * @throws IOException in case of an I/O error
	 * @return template
	 */
	public static Template parse(FuncMap funcs, File... files) throws ParseException, IOException
	{
		if (files.length == 0)
			throw new ParseException("no files");

		return parse(null, funcs, Utils.filesToString(files));
	}

	/**
	 * Create templates from the map, that represent pair (Name, InputStream)
	 *
	 * @param funcs functions
	 * @param inputs list of {@link InputStream}
	 * @throws ParseException in case of a parse error
	 * @return template
	 */
	public static Template parse(FuncMap funcs, Map<String, String> inputs) throws ParseException
	{
		if (inputs.size() == 0)
			throw new ParseException("no inputs");

		return parse(null, funcs, inputs);
	}

	/**
	 * Create templates from the file list and append to this template
	 *
	 * @param funcs functions
	 * @param files template files
	 * @throws ParseException in case of a parse error
	 * @throws IOException in case of an I/O error
	 * @return template
	 */
	public Template parseTo(FuncMap funcs, File... files) throws ParseException, IOException
	{
		if (files.length == 0)
			throw new ParseException("no files");

		return parse(this, funcs, Utils.filesToString(files));
	}

	/**
	 * Create templates from the map, that represent pair (Name, InputStream)
	 * and append to this template
	 *
	 * @param funcs functions
	 * @param text pair (Name, {@link InputStream})
	 * @throws ParseException in case of a parse error
	 * @return template
	 */
	public Template parseTo(FuncMap funcs, Map<String, String> text) throws ParseException
	{
		if (text.size() == 0)
			throw new ParseException("no inputs");

		return parse(this, funcs, text);
	}

	private static Template parse(Template t, FuncMap funcs,
				      Map<String, String> inputs) throws ParseException
	{
		if (inputs.size() == 0)
			throw new ParseException("no text data");

		for (Map.Entry<String, String> text : inputs.entrySet()) {
			try {
				String name = text.getKey();
				Template tmpl;
				if (t == null) {
					t = new Template(name);
					if (funcs != null)
						t.addFuncs(funcs);
				}
				if (name.equals(t.name))
					tmpl = t;
				else
					tmpl = new Template(name, t);
				tmpl.parse(text.getValue());
			} catch (Exception e) {
				throw new ParseException(e);
			}
		}

		return t;
	}

	/**
	 * Applies a parsed template to the specified data object,
	 * and writes the output to OutputStream.
	 * If an error occurs executing the template or writing its output,
	 * execution stops, but partial results may
	 * already have been written to the output writer
	 *
	 * @param os {@link OutputStream} object
	 * @param data data
	 * @throws ExecException in case of an execute error
	 */
	public void execute(OutputStream os, Object data) throws ExecException
	{
		ArrayList<Variable> vars = new ArrayList<>();
		vars.add(new Variable("$", data));
		Exec state = new Exec(this, new PrintWriter(os), vars);
		try {
			if (tree == null || tree.root == null)
				state.errorf("%s is an incomplete or empty template", name);
			state.walk(data, tree.root);
		} finally {
			state.pw.close();
		}
	}

	/**
	 * Applies the template associated with this template that has the given name
	 * to the specified data object and writes the output to OutputStream
	 *
	 * @param os {@link OutputStream} object
	 * @param name template name
	 * @param data data
	 * @throws ExecException in case of an execute error
	 */
	public void executeTemplate(OutputStream os, String name, Object data) throws ExecException
	{
		Template tmpl = null;
		if (common != null)
			tmpl = common.tmpl.get(name);
		if (tmpl == null)
			throw new ExecException(String.format("no template %s associated with template %s", name, this.name));
		tmpl.execute(os, data);
	}

	public void addFuncs(FuncMap funcs)
	{
		if (funcs == null)
			throw new NullPointerException();

		common.funcsLock.lock();
		try {
			common.funcs.put(funcs);
		} finally {
			common.funcsLock.unlock();
		}
	}

	public void setDelims(String left, String right)
	{
		leftDelim = left;
		rightDelim = right;
	}

	public Template[] getTemplates()
	{
		if (common == null)
			return null;

		return common.tmpl.values().toArray(new Template[common.tmpl.size()]);
	}

	public Template getTemplate(String name)
	{
		if (common == null)
			return null;

		return common.tmpl.get(name);
	}

	/**
	 * Adds parse tree for template with given name and associates it with template.
	 * If the template does not already exist, it will create a new one.
	 * If the template does exist, it will be replaced
	 *
	 * @param name template name
	 * @param tree parse tree
	 * @throws InternalException in case of an internal error
	 * @throws ParseException in case of an execute error
	 */
	public void addParseTree(String name, Tree tree) throws InternalException, ParseException
	{
		/* If the name is the name of this template, overwrite this template */
		Template newTemplate = this;
		if (!name.equals(this.name))
			newTemplate = new Template(name, this);

		if (associate(newTemplate, tree) || newTemplate.tree == null)
			newTemplate.tree = tree;
	}

	/**
	 * Installs the new template into the group of
	 * templates associated with template.
	 * The two are already known to share the TemplateCommon class
	 */
	private boolean associate(Template newTemplate, Tree tree) throws InternalException, ParseException
	{
		if (newTemplate.common != common)
			throw new InternalException("associate not common");

		Template old = common.tmpl.get(newTemplate.name);
		/* If a template by that name exists, don't replace it with an empty template */
		if (old != null && Tree.isEmptyTree(tree.root) && old.tree != null)
			return false;
		common.tmpl.put(newTemplate.name, newTemplate);

		return true;
	}

	List<Method> findFunc(String name)
	{
		common.funcsLock.lock();
		try {
			List<Method> func = common.funcs.get(name);
			if (func != null)
				return func;
		} finally {
			common.funcsLock.unlock();
		}

		return FuncMap.builtins.get(name);
	}
}
