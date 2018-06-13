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

import ru.proninyaroslav.template.exceptions.ParseException;

import java.util.ArrayList;

/**
 * Element in the parse tree
 */

abstract class Node
{
	public Type type;
	public int pos;
	protected Tree tree;

	public Node(Tree tree, Type type, int pos)
	{
		this.tree = tree;
		this.type = type;
		this.pos = pos;
	}

	/**
	 * Makes a deep copy of the node and all its components
	 */
	public abstract Node copy();

	public enum Type {
		TEXT,           /* plain text */
		ACTION,         /* a non-control action such as a field evaluation */
		BOOL,
		CHAIN,          /* a sequence of field accesses */
		COMMAND,        /* An element of a pipeline */
		DOT,            /* the cursor, dot */
		FIELD,          /* a field or method name */
		IDENTIFIER,     /* an identifier; always a function name */
		IF,
		LIST,           /* a list of Nodes */
		NULL,
		NUMBER,
		PIPE,           /* a pipeline of commands */
		FOR,
		ELSE,           /* an else action. Not added to tree */
		END,            /* an end action. Not added to tree */
		WITH,
		BREAK,
		CONTINUE,
		STRING,
		TEMPLATE,       /* a template invocation action */
		VARIABLE
	}

	/**
	 * Holds a sequence of nodes
	 */
	public static class List extends Node
	{
		public ArrayList<Node> nodes;

		public List(Tree tree, int pos)
		{
			super(tree, Type.LIST, pos);
			nodes = new ArrayList<>();
		}

		public void append(Node node)
		{
			nodes.add(node);
		}

		public List copyList()
		{
			List list = new List(tree, pos);
			for (Node node : nodes)
				list.append(node.copy());

			return list;
		}

		@Override
		public Node copy()
		{
			return copyList();
		}

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			for (Node node : nodes)
				sb.append(node);

			return sb.toString();
		}
	}

	/**
	 * Holds a plain text
	 */
	public static class Text extends Node
	{
		public String text; /* may span newlines */

		public Text(Tree tree, int pos, String text)
		{
			super(tree, Type.TEXT, pos);
			this.text = text;
		}

		@Override
		public Node copy()
		{
			return new Text(tree, pos, text);
		}

		@Override
		public String toString()
		{
			return text;
		}
	}

	/**
	 * Holds a pipeline with optional declaration
	 */
	public static class Pipe extends Node
	{
		ArrayList<Variable> decl;   /* variable declarations in lexical order */
		ArrayList<Command> cmds;    /* the commands in lexical order */

		public Pipe(Tree tree, int pos, java.util.List<Variable> decl)
		{
			super(tree, Type.PIPE, pos);
			this.decl = new ArrayList<>(decl);
			cmds = new ArrayList<>();
		}

		public void append(Command cmd)
		{
			cmds.add(cmd);
		}

		public Pipe copyPipe()
		{
			ArrayList<Variable> copyDecl = new ArrayList<>();
			for (Variable d : decl)
				copyDecl.add((Variable)d.copy());
			Pipe pipe = new Pipe(tree, pos, copyDecl);
			pipe.decl = new ArrayList<>(decl);
			for (Command cmd : cmds)
				pipe.append((Command)cmd.copy());

			return pipe;
		}

		@Override
		public Node copy()
		{
			return copyPipe();
		}

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder("");

			if (decl.size() == 1) {
				sb.append(decl.get(0));
				sb.append(" = ");
			}

			for (int i = 0; i < cmds.size(); i++) {
				if (i > 0)
					sb.append(" | ");
				sb.append(cmds.get(i));
			}

			return sb.toString();
		}
	}

	/**
	 * Holds a list of variable names, possibly with chained field accesses.
	 * The dollar sign is part of the (first) name
	 */
	public static class Variable extends Node
	{
		public ArrayList<String> ident; /* variable name and fields in lexical order */

		public Variable(Tree tree, int pos, java.util.List<String> ident)
		{
			super(tree, Type.VARIABLE, pos);
			this.ident = new ArrayList<>(ident);
		}

		@Override
		public Node copy()
		{
			return new Variable(tree, pos, new ArrayList<>(ident));
		}

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder("");
			for (int i = 0; i < ident.size(); i++) {
				if (i > 0)
					sb.append(".");
				sb.append(ident.get(i));
			}

			return sb.toString();
		}
	}

	/**
	 * Holds a command (a pipeline inside an evaluating action)
	 */
	public static class Command extends Node
	{
		public ArrayList<Node> args; /* arguments in lexical order: identifier, field, or constant */

		public Command(Tree tree, int pos)
		{
			super(tree, Type.COMMAND, pos);
			args = new ArrayList<>();
		}

		public void append(Node node)
		{
			args.add(node);
		}

		@Override
		public Node copy()
		{
			Command command = new Command(tree, pos);
			for (Node arg : args)
				command.append(arg.copy());

			return command;
		}

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder("");
			for (int i = 0; i < args.size(); i++) {
				if (i > 0)
					sb.append(" ");
				Node arg = args.get(i);
				if (arg.type == Type.PIPE) {
					sb.append("(").append(arg).append(")");
					continue;
				}
				sb.append(arg);
			}

			return sb.toString();
		}
	}

	/**
	 * Holds an action (something bounded by delimiters).
	 * Control actions have their own nodes; Action represents simple
	 * ones such as field evaluations and parenthesized pipelines
	 */
	public static class Action extends Node
	{
		public Pipe pipe;

		public Action(Tree tree, int pos, Pipe pipe)
		{
			super(tree, Type.ACTION, pos);
			this.pipe = pipe;
		}

		@Override
		public Node copy()
		{
			return new Action(tree, pos, pipe.copyPipe());
		}

		@Override
		public String toString()
		{
			return String.format("{{%s}}", pipe);
		}
	}

	public static class Identifier extends Node
	{
		public String ident; /* the identifier's name */

		public Identifier(Tree tree, int pos, String ident)
		{
			super(tree, Type.IDENTIFIER, pos);
			this.ident = ident;
		}

		@Override
		public Node copy()
		{
			return new Identifier(tree, pos, ident);
		}

		@Override
		public String toString()
		{
			return ident;
		}
	}

	/**
	 * Holds the special identifier '.'
	 */
	public static class Dot extends Node
	{
		public Dot(Tree tree, int pos)
		{
			super(tree, Type.DOT, pos);
		}

		@Override
		public Node copy()
		{
			return new Dot(tree, pos);
		}

		@Override
		public String toString()
		{
			return ".";
		}
	}

	public static class Null extends Node
	{
		public Null(Tree tree, int pos)
		{
			super(tree, Type.NULL, pos);
		}

		@Override
		public Node copy()
		{
			return new Null(tree, pos);
		}

		@Override
		public String toString()
		{
			return "null";
		}
	}

	/**
	 * Holds a field (identifier starting with '.').
	 * The names may be chained ('.x.y').
	 * The dot is dropped from each ident
	 */
	public static class Field extends Node
	{
		public ArrayList<String> ident; /* variable name and fields in lexical order */

		public Field(Tree tree, int pos, java.util.List<String> ident)
		{
			super(tree, Type.FIELD, pos);
			this.ident = new ArrayList<>(ident);
		}

		@Override
		public Node copy()
		{
			return new Field(tree, pos, new ArrayList<>(ident));
		}

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder("");
			for (String i : ident) {
				sb.append(".").append(i);
			}

			return sb.toString();
		}
	}

	/**
	 * Holds a term followed by a chain of field accesses (identifier starting with '.').
	 * The names may be chained ('.x.y'). The periods are dropped from each ident
	 */
	public static class Chain extends Node
	{
		public ArrayList<String> field; /* the identifiers in lexical order */
		public Node node;

		public Chain(Tree tree, int pos, Node node)
		{
			super(tree, Type.CHAIN, pos);
			this.node = node;
			this.field = new ArrayList<>();
		}

		public Chain(Tree tree, int pos, Node node, java.util.List<String> field)
		{
			super(tree, Type.CHAIN, pos);
			this.node = node;
			this.field = new ArrayList<>(field);
		}

		/*
		 * Ddds the named field (which should start with a dot) to the end of the chain
		 */
		public void add(String field) throws ParseException
		{
			if (field.length() == 0 || field.charAt(0) != '.')
				throw new ParseException("no dot in field");
			field = field.substring(1);
			if (field.equals(""))
				throw new ParseException("no dot in field");
			this.field.add(field);
		}

		@Override
		public Node copy()
		{
			return new Chain(tree, pos, node, new ArrayList<>(field));
		}

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			if (node.type == Type.PIPE)
				sb.append("(").append(node).append(")");
			else
				sb.append(node);

			for (String f : field) {
				sb.append(".").append(f);
			}

			return sb.toString();
		}
	}

	public static class Bool extends Node
	{
		public boolean boolVal;

		public Bool(Tree tree, int pos, boolean boolVal)
		{
			super(tree, Type.BOOL, pos);
			this.boolVal = boolVal;
		}

		@Override
		public Node copy()
		{
			return new Bool(tree, pos, boolVal);
		}

		@Override
		public String toString()
		{
			return (boolVal ? "true" : "false");
		}
	}

	/**
	 * Holds a number: integer or float.
	 * The value is parsed and stored under
	 * all Java types that can represent the value
	 */
	public static class Number extends Node
	{
		public boolean isInt;
		public boolean isFloat;
		public int intVal;
		public double floatVal;
		public String text; /* the original textual representation from the input */

		public Number(Tree tree, int pos, String text)
		{
			super(tree, Type.NUMBER, pos);
			this.text = text;
		}

		public Number(Number node)
		{
			super(node.tree, node.type, node.pos);
			this.text = node.text;
			this.isInt = node.isInt;
			this.isFloat = node.isFloat;
			this.intVal = node.intVal;
			this.floatVal = node.floatVal;
		}

		@Override
		public Node copy()
		{
			return new Number(this);
		}

		@Override
		public String toString()
		{
			return text;
		}
	}

	/**
	 * Holds a string constant. The value has been unquoted
	 */
	public static class StringConst extends Node
	{
		public String quoted;   /* the original text of the string, with quotes */
		public String text;     /* the string, after quote processing */

		public StringConst(Tree tree, int pos, String quoted, String text)
		{
			super(tree, Type.STRING, pos);
			this.quoted = quoted;
			this.text = text;
		}

		@Override
		public Node copy()
		{
			return new StringConst(tree, pos, quoted, text);
		}

		@Override
		public String toString()
		{
			return quoted;
		}
	}

	/**
	 * Represents an {end} action.
	 * It does not appear in the final runParser tree
	 */
	public static class End extends Node
	{
		public End(Tree tree, int pos)
		{
			super(tree, Type.END, pos);
		}

		@Override
		public Node copy()
		{
			return new End(tree, pos);
		}

		@Override
		public String toString()
		{
			return "{{end}}";
		}
	}

	/**
	 * Represents an {else} action.
	 * It does not appear in the final runParser tree
	 */
	public static class Else extends Node
	{
		public Else(Tree tree, int pos)
		{
			super(tree, Type.ELSE, pos);
		}

		@Override
		public Node copy()
		{
			return new Else(tree, pos);
		}

		@Override
		public String toString()
		{
			return "{{else}}";
		}
	}

	/**
	 * The common representation of if, with and for
	 */
	public static class Branch extends Node
	{
		Pipe pipe;      /* the pipeline to be evaluated */
		List list;      /* what to execute if the value is non-empty */
		List elseList;  /* what to execute if the value is empty (null if absent) */

		public Branch(Tree tree, Type type, int pos,
			      Pipe pipe, List list, List elseList)
		{
			super(tree, type, pos);
			this.pipe = pipe;
			this.list = list;
			this.elseList = elseList;
		}

		@Override
		public Node copy()
		{
			switch (type) {
				case IF:
					return new If(tree, pos, pipe,
						      list, elseList);
				case FOR:
					return new For(tree, pos, pipe,
						       list, elseList);
				case WITH:
					return new With(tree, pos, pipe,
						        list, elseList);
				default:
					return null;
			}
		}

		@Override
		public String toString()
		{
			String name;
			switch (type) {
				case IF:
					name = "if";
					break;
				case FOR:
					name = "for";
					break;
				case WITH:
					name = "with";
					break;
				default:
					return "unknown branch type";
			}
			if (elseList != null)
				return String.format("{{%s %s}}%s{{else}}%s{{end}}", name, pipe, list, elseList);

			return String.format("{{%s %s}}%s{{end}}", name, pipe, list);
		}
	}

	public static class If extends Branch
	{
		public If(Tree tree, int pos, Pipe pipe,
			  List list, List elseList)
		{
			super(tree, Type.IF, pos, pipe, list, elseList);
		}

		@Override
		public Node copy()
		{
			return new If(tree, pos, pipe.copyPipe(), list.copyList(),
				elseList != null ? elseList.copyList() : null);
		}
	}

	public static class For extends Branch
	{
		public For(Tree tree, int pos, Pipe pipe,
			   List list, List elseList)
		{
			super(tree, Type.FOR, pos, pipe, list, elseList);
		}

		@Override
		public Node copy()
		{
			return new For(tree, pos, pipe.copyPipe(), list.copyList(),
				elseList != null ? elseList.copyList() : null);
		}
	}

	public static class With extends Branch
	{
		public With(Tree tree, int pos, Pipe pipe,
			    List list, List elseList)
		{
			super(tree, Type.WITH, pos, pipe, list, elseList);
		}

		@Override
		public Node copy()
		{
			return new With(tree, pos, pipe.copyPipe(), list.copyList(),
				elseList != null ? elseList.copyList() : null);
		}
	}

	public static class Break extends Node
	{
		public Break(Tree tree, int pos)
		{
			super(tree, Type.BREAK, pos);
		}

		@Override
		public Node copy()
		{
			return new Break(tree, pos);
		}

		@Override
		public String toString()
		{
			return "{{break}}";
		}
	}

	public static class Continue extends Node
	{
		public Continue(Tree tree, int pos)
		{
			super(tree, Type.CONTINUE, pos);
		}

		@Override
		public Node copy()
		{
			return new Continue(tree, pos);
		}

		@Override
		public String toString()
		{
			return "{{continue}}";
		}
	}

	/**
	 * Represents a {template} action
	 */
	public static class Template extends Node
	{
		public String name;     /* the name of the template (unquoted) */
		public Pipe pipe;       /* the command to evaluate as dot for the template */

		public Template(Tree tree, int pos, String name, Pipe pipe)
		{
			super(tree, Type.TEMPLATE, pos);
			this.name = name;
			this.pipe = pipe;
		}

		@Override
		public Node copy()
		{
			return new Template(tree, pos, name, pipe != null ? pipe.copyPipe() : null);
		}

		@Override
		public String toString()
		{
			if (pipe == null)
				return String.format("{{template \"%s\"}}", name);

			return String.format("{{template \"%s\" %s}}", name, pipe);
		}
	}
}
