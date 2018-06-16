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

import ru.proninyaroslav.template.exceptions.InternalException;
import ru.proninyaroslav.template.exceptions.ParseException;

import java.util.*;

/**
 * Representation of a single parsed template
 */

public class Tree
{
	public String name;                     /* template name */
	public String parseName;                /* name of the top-level template during parsing, for error messages */
	public Node.List root;                  /* top-level root of the tree */
	private String text;                    /* text parsed to create the template (or its parent) */
	/* Parsing only; cleared after runParser */
	private Lexer lex;
	private Token[] token = new Token[3];   /* three-token lookahead for parser */
	private int peekCount;
	private int forDepth;			/* nesting level of for loops */
	private FuncMap[] funcs;
	private ArrayList<String> vars;         /* variables defined at the moment */
	private HashMap<String, Tree> treeSet;

	public Tree(String name, String parseName,
		    String text, FuncMap... funcs)
	{
		this.name = name;
		this.parseName = parseName;
		this.text = text;
		this.funcs = funcs;
	}

	public Tree(String name, FuncMap... funcs)
	{
		this.name = name;
		this.funcs = funcs;
	}

	public static HashMap<String, Tree> parse(String name, String text,
						  String leftDelim, String rightDelim,
						  FuncMap... funcs) throws ParseException, InternalException
	{
		HashMap<String, Tree> treeSet = new HashMap<>();
		Tree tree = new Tree(name);
		tree.text = text;
		tree.parse(text, leftDelim, rightDelim, treeSet, funcs);

		return treeSet;
	}

	private void parse(String text, String leftDelim, String rightDelim,
			   HashMap<String, Tree> treeSet, FuncMap... funcs) throws ParseException, InternalException
	{
		try {
			parseName = name;
			startParse(funcs, new Lexer(name, text, leftDelim, rightDelim), treeSet);
			this.text = text;
			parse();
			add();
		} finally {
			lex.drain();
			stopParse();
		}
	}

	private void startParse(FuncMap[] funcs, Lexer lex, HashMap<String, Tree> treeSet)
	{
		this.lex = lex;
		this.funcs = funcs;
		this.treeSet = treeSet;
		vars = new ArrayList<>();
		vars.add("$");
	}

	private void stopParse()
	{
		lex = null;
		vars = null;
		funcs = null;
		treeSet = null;
		forDepth = 0;
	}

	/**
	 * Adds tree to treeSet
	 */
	private void add() throws ParseException
	{
		Tree tree = treeSet.get(name);
		if (tree == null || isEmptyTree(root)) {
			treeSet.put(name, this);
			return;
		}

		if (!isEmptyTree(root))
			errorf("template: multiple definition of template %s", name);
	}

	public String errorLocation(Node node)
	{
		Tree tree = node.tree;
		if (tree == null)
			tree = this;
		String text = tree.text.substring(0, node.pos);
		int index = text.lastIndexOf('\n');
		if (index == -1) {
			index = node.pos; /* On first line */
		} else {
			++index; /* After newline */
			index = node.pos = index;
		}
		int lineNum = Utils.countChars(text, '\n');

		return String.format("%s:%d:%d", tree.parseName, lineNum, index);
	}

	public String errorContext(Node node)
	{
		String context = node.toString();
		if (context.length() > 20)
			context = String.format("%.20s...", context);

		return context;
	}

	private void errorf(String format, Object... args) throws ParseException
	{
		root = null;
		throw new ParseException(
			String.format(String.format("%s:%d: %s", parseName, token[0].line, format),
				args));
	}

	/**
	 * Reports whether this tree (node) is empty of everything but space
	 */
	public static boolean isEmptyTree(Node node) throws ParseException
	{
		if (node == null)
			return true;

		if (node instanceof Node.List) {
			for (Node n : ((Node.List)node).nodes)
				if (isEmptyTree(n))
					return false;
			return true;
		} else if (node instanceof Node.Text) {
			return ((Node.Text)node).text.trim().length() == 0;
		} else if (!(node instanceof Node.Action) &&
			!(node instanceof Node.If) &&
			!(node instanceof Node.For) &&
			!(node instanceof Node.Template) &&
			!(node instanceof Node.With)) {
			throw new ParseException(String.format("unknown node: %s", node));
		}

		return false;
	}

	/**
	 * Backs the input stream up one token
	 */
	private void backup()
	{
		++peekCount;
	}

	/**
	 * Backs the input stream up two tokens.
	 * The zeroth token is already there
	 */
	private void backupTwo(Token t)
	{
		token[1] = t;
		peekCount = 2;
	}

	/**
	 * Backs the input stream up three tokens.
	 * The zeroth token is already there
	 */
	private void backupThree(Token t2, Token t1) /* Reverse order: we're pushing back */
	{
		token[1] = t1;
		token[2] = t2;
		peekCount = 3;
	}

	/**
	 * Returns but does not consume the next token
	 */
	private Token peek() throws InternalException
	{
		if (peekCount > 0)
			return token[peekCount - 1];
		peekCount = 1;
		token[0] = lex.nextToken();

		return token[0];
	}

	private Token peekNonSpace() throws InternalException
	{
		Token token = next();
		for (;;) {
			if (token.type != Token.Type.SPACE)
				break;
			token = next();
		}
		backup();

		return token;
	}

	private Token next() throws InternalException
	{
		if (peekCount > 0)
			--peekCount;
		else
			token[0] = lex.nextToken();

		return token[peekCount];
	}

	private Token nextNonSpace() throws InternalException
	{
		Token token = next();
		for (;;) {
			if (token.type != Token.Type.SPACE)
				break;
			token = next();
		}

		return token;
	}

	/**
	 * Consumes the next token and guarantees it has the required type
	 */
	private Token expect(Token.Type expected, String context) throws ParseException, InternalException
	{
		Token token = nextNonSpace();
		if (token.type != expected)
			unexpected(token, context);

		return token;
	}

	/**
	 * Consumes the next token and guarantees it has one of the required types
	 */
	private Token expectOneOf(Token.Type expected1,
				  Token.Type expected2,
				  String context) throws ParseException, InternalException
	{
		Token token = nextNonSpace();
		if (token.type != expected1 && token.type != expected2)
			unexpected(token, context);

		return token;
	}

	private void unexpected(Token token, String context) throws ParseException
	{
		errorf("unexpected %s in %s", token, context);
	}

	private boolean hasFunction(String name)
	{
		for (FuncMap funcMap : funcs) {
			if (funcMap == null)
				continue;
			if (funcMap.contains(name))
				return true;
		}

		return false;
	}

	/**
	 * Returns a node for a variable reference.
	 * It errors if the variable is not defined
	 */
	private Node useVar(int pos, String name) throws ParseException
	{
		Node.Assign var = newVariable(pos, name);
		for (String varName : vars)
			if (var.ident.get(0).equals(varName))
				return var;
		errorf("undefined variable %s", name);

		return null;
	}

	/**
	 * Trims the variable list to the specified length
	 */
	private void popVars(int n)
	{
		vars = new ArrayList<>(vars.subList(0, n));
	}

	private void parse() throws ParseException, InternalException
	{
		root = newList(peek().pos);
		while (peek().type != Token.Type.EOF) {
			if (peek().type == Token.Type.LEFT_DELIM) {
				Token delim = next();
				if (nextNonSpace().type == Token.Type.DEFINE) {
					/* Name will be updated once we know it */
					Tree newTree = new Tree("definition", parseName, text);
					newTree.startParse(funcs, lex, treeSet);
					newTree.parseDefinition();
					continue;
				}
				backupTwo(delim);
			}
			Node n = textOrAction();
			if (n == null || n.type == Node.Type.END ||
			    n.type == Node.Type.ELSE)
				errorf("unexpected %s", n);
			else
				root.append(n);
		}
	}

	/**
	 * Parses a {define} ... {end} template definition and
	 * installs the definition in treeSet.
	 * The "define" keyword has already been scanned
	 */
	private void parseDefinition() throws ParseException, InternalException
	{
		final String context = "define clause";
		Token name = expectOneOf(Token.Type.STRING, Token.Type.RAW_STRING, context);
		try {
			this.name = Utils.unquote(name.val);
		} catch (IllegalArgumentException e) {
			errorf("%s", e.getMessage());
		}
		expect(Token.Type.RIGHT_DELIM, context);

		Node.List[] outRoot = new Node.List[1];
		Node[] outEnd = new Node[1];
		tokenList(outRoot, outEnd);
		root = outRoot[0];
		if (outEnd[0].type != Node.Type.END)
			errorf("unexpected %s in %s", outEnd[0], context);
		add();
		stopParse();
	}

	/**
	 * tokenList:
	 *  textOrAction*
	 *  Terminates at {end} or {else}, returned separately
	 */
	private void tokenList(Node.List[] outList, Node[] outNode) throws ParseException, InternalException
	{
		outList[0] = newList(peekNonSpace().pos);
		while (peekNonSpace().type != Token.Type.EOF) {
			outNode[0] = textOrAction();
			if (outNode[0] != null &&
				(outNode[0].type == Node.Type.END ||
					outNode[0].type == Node.Type.ELSE)) {
				return;
			}
			outList[0].append(outNode[0]);
		}
		errorf("unexpected EOF");
	}

	/**
	 * textOrAction:
	 *  text | action
	 */
	private Node textOrAction() throws ParseException, InternalException
	{
		Token token = nextNonSpace();
		switch (token.type) {
			case TEXT:
				return newText(token.pos, token.val);
			case LEFT_DELIM:
				return action();
			default:
				unexpected(token, "input");
		}

		return null;
	}

	/**
	 * action:
	 *  control
	 *  pipeline
	 * Left delim is past. Now get actions.
	 * First word could be a keyword such as for
	 */
	private Node action() throws ParseException, InternalException
	{
		Token token = nextNonSpace();
		switch (token.type) {
			case ELSE:
				return elseControl();
			case END:
				return endControl();
			case IF:
				return ifControl();
			case FOR:
				return forControl();
			case TEMPLATE:
				return templateControl();
			case WITH:
				return withControl();
			case BREAK:
				return breakControl();
			case CONTINUE:
				return continueControl();
		}
		backup();
		token = peek();

		/* Do not pop variables; they persist until "end" */
		return newAction(token.pos, pipeline("command"));
	}

	/**
	 * pipeline:
	 *  declaration? command ('|' command)*
	 */
	private Node.Pipe pipeline(String context) throws ParseException, InternalException
	{
		ArrayList<Node.Assign> vars = new ArrayList<>();
		boolean decl = false;
		int pos = peekNonSpace().pos;
		Token v = peekNonSpace();
		if (v.type == Token.Type.VARIABLE) {
			next();
			/*
			 * Since space is a token, we need 3-token look-ahead here in
			 * the worst case: in "$x foo" we need to read "foo" (as opposed to "=")
			 * to know that $x is an argument variable rather than a declaration.
			 */
			Token tokenAfterVariable = peek();
			Token next = peekNonSpace();
			if (next.type == Token.Type.ASSIGN || next.type == Token.Type.DECLARE) {
				nextNonSpace();
				vars.add(newVariable(v.pos, v.val));
				this.vars.add(v.val);
				decl = next.type == Token.Type.DECLARE;
			} else if (tokenAfterVariable.type == Token.Type.SPACE) {
				backupThree(v, tokenAfterVariable);
			} else {
				backupTwo(v);
			}
		}

		Node.Pipe pipe = newPipeline(pos, vars);
		pipe.decl = decl;
		for (;;) {
			Token token = nextNonSpace();
			switch (token.type) {
				case RIGHT_DELIM:
				case RIGHT_PAREN:
					checkPipeline(pipe, context);
					if (token.type == Token.Type.RIGHT_PAREN)
						backup();
					return pipe;
				case BOOL:
				case CHAR_CONSTANT:
				case DOT:
				case FIELD:
				case IDENTIFIER:
				case NUMBER:
				case NULL:
				case STRING:
				case RAW_STRING:
				case VARIABLE:
				case LEFT_PAREN:
					backup();
					pipe.append(command());
					break;
				default:
					unexpected(token, context);
					break;
			}
		}
	}

	private void checkPipeline(Node.Pipe pipe, String context) throws ParseException
	{
		/* Reject empty pipelines */
		if (pipe.cmds.size() == 0)
			errorf("missing value for %s", context);
		/* Only the first command of a pipeline can start with a non executable operand */
		for (int i = 1; i < pipe.cmds.size(); i++) {
			Node.Command c = pipe.cmds.get(i);
			switch (c.args.get(0).type) {
				case BOOL:
				case DOT:
				case NULL:
				case NUMBER:
				case STRING:
					errorf("non executable command in pipeline stage %d", i + 1);
			}
		}
	}

	/**
	 * command:
	 *  operand (space operand)*
	 * Space-separated arguments up to a pipeline character or right delimiter.
	 * We consume the pipe character but leave the right delim to terminate the action
	 */
	private Node.Command command() throws ParseException, InternalException
	{
		Node.Command cmd = newCommand(peekNonSpace().pos);
		for (;;) {
			/* Skip leading spaces */
			peekNonSpace();
			Node operand = operand();
			if (operand != null)
				cmd.append(operand);
			Token token = next();
			switch (token.type) {
				case SPACE:
					continue;
				case ERROR:
					errorf("%s", token.val);
					break;
				case RIGHT_DELIM:
				case RIGHT_PAREN:
					backup();
					break;
				case PIPE:
					break;
				default:
					errorf("unexpected %s in operand", token);
			}
			break;
		}
		if (cmd.args.size() == 0) {
			errorf("empty command");
		}

		return cmd;
	}

	/**
	 * operand:
	 *  term .field*
	 * An operand is a space-separated component of a command,
	 * a term possibly followed by field accesses.
	 * A null return means the next token is not an operand
	 */
	private Node operand() throws ParseException, InternalException
	{
		Node node = term();
		if (node == null)
			return null;
		if (peek().type == Token.Type.FIELD) {
			Node.Chain chain = newChain(peek().pos, node);
			while (peek().type == Token.Type.FIELD)
				chain.add(next().val);
			/*
			 * Obvious parsing errors involving literal values are detected here.
			 * More complex error cases will have to be handled at execution time.
			 */
			switch (node.type) {
				case FIELD:
					node = newField(chain.pos, chain.toString());
					break;
				case VARIABLE:
					node = newVariable(chain.pos, chain.toString());
					break;
				case BOOL:
				case NULL:
				case NUMBER:
				case DOT:
					errorf("unexpected . after term %s", node);
				default:
					node = chain;
			}
		}

		return node;
	}

	/**
	 * term:
	 *  literal (number, string, null, boolean)
	 *  function (identifier)
	 *  dot
	 *  .field
	 *  $variable
	 *  '(' pipeline ')'
	 * A term is a simple "expression".
	 * A null return means the next item is not a term
	 */
	private Node term() throws ParseException, InternalException
	{
		Token token = nextNonSpace();
		switch (token.type) {
			case ERROR:
				errorf("%s", token.val);
			case IDENTIFIER:
				if (!hasFunction(token.val))
					errorf("function '%s' not defined", token.val);
				Node.Identifier i = newIdentifier(token.pos, token.val);
				i.tree = this;
				return i;
			case DOT:
				return newDot(token.pos);
			case NULL:
				return newNull(token.pos);
			case VARIABLE:
				return useVar(token.pos, token.val);
			case FIELD:
				return newField(token.pos, token.val);
			case BOOL:
				return newBool(token.pos, token.val.equals("true"));
			case CHAR_CONSTANT:
			case NUMBER:
				return newNumber(token.pos, token.val, token.type);
			case LEFT_PAREN:
				Node.Pipe pipe = pipeline("parenthesized pipeline");
				Token t = next();
				if (t.type != Token.Type.RIGHT_PAREN)
					errorf("unclosed right paren: unexpected %s", token);
				return pipe;
			case STRING:
			case RAW_STRING:
				String s;
				try {
					s = Utils.unquote(token.val);
				} catch (IllegalArgumentException e) {
					throw new ParseException(e.getMessage());
				}
				return newString(token.pos, token.val, s);
		}
		backup();

		return null;
	}

	/**
	 * else:
	 *  {{else}}
	 */
	private Node elseControl() throws ParseException, InternalException
	{
		/* Special case for "else if" */
		Token peek = peekNonSpace();
		if (peek.type == Token.Type.IF) {
			/* We see "{else if ... " but in effect rewrite it to {else}{if ... " */
			return newElse(peek.pos);
		}

		return newElse(expect(Token.Type.RIGHT_DELIM, "else").pos);
	}

	/**
	 * end:
	 *  {{end}}
	 */
	private Node endControl() throws ParseException, InternalException
	{
		return newEnd(expect(Token.Type.RIGHT_DELIM, "end").pos);
	}

	/**
	 * template:
	 *  {{template stringValue pipeline}}
	 * The name must be something that can evaluate to a string
	 */
	private Node templateControl() throws ParseException, InternalException
	{
		final String context = "template clause";
		Token token = nextNonSpace();
		String name = parseTemplateName(token, context);
		Node.Pipe pipe = null;
		if (nextNonSpace().type != Token.Type.RIGHT_DELIM) {
			backup();
			/* Don't pop variables; they persist until "end" */
			pipe = pipeline(context);
		}

		return newTemplate(token.pos, name, pipe);
	}

	private String parseTemplateName(Token token, String context) throws ParseException
	{
		String name = "";
		if (token.type == Token.Type.STRING || token.type == Token.Type.RAW_STRING)
			name = Utils.unquote(token.val);
		else
			unexpected(token, context);

		return name;
	}

	/**
	 * if:
	 *  {{if pipeline}} tokenList {{end}}
	 *  {{if pipeline}} tokenList {{else}} tokenList {{end}}
	 */
	private Node ifControl() throws ParseException, InternalException
	{
		int[] outPos = new int[1];
		Node.Pipe[] outPipe = new Node.Pipe[1];
		Node.List[] outList = new Node.List[1];
		Node.List[] outElseList = new Node.List[1];
		parseControl(true, "if", outPos, outPipe, outList, outElseList);

		return newIf(outPos[0], outPipe[0], outList[0], outElseList[0]);
	}

	/**
	 * for:
	 *  {{for pipeline}} tokenList {{end}}
	 *  {{for pipeline}} tokenList {{else}} tokenList {{end}}
	 */
	private Node forControl() throws ParseException, InternalException
	{
		int[] outPos = new int[1];
		Node.Pipe[] outPipe = new Node.Pipe[1];
		Node.List[] outList = new Node.List[1];
		Node.List[] outElseList = new Node.List[1];
		parseControl(false, "for", outPos, outPipe, outList, outElseList);

		return newFor(outPos[0], outPipe[0], outList[0], outElseList[0]);
	}

	/**
	 * break:
	 *  {{break}}
	 */
	private Node breakControl() throws ParseException, InternalException
	{
		if (forDepth == 0)
			errorf("unexpected break outside of for");

		return newBreak(expect(Token.Type.RIGHT_DELIM, "break").pos);
	}

	/**
	 * continue:
	 *  {{continue}}
	 */
	private Node continueControl() throws ParseException, InternalException
	{
		if (forDepth == 0)
			errorf("unexpected continue outside of for");

		return newContinue(expect(Token.Type.RIGHT_DELIM, "continue").pos);
	}

	/**
	 * with:
	 *  {{with pipeline}} tokenList {{end}}
	 *  {{with pipeline}} tokenList {{else}} tokenList {{end}}
	 */
	private Node withControl() throws ParseException, InternalException
	{
		int[] outPos = new int[1];
		Node.Pipe[] outPipe = new Node.Pipe[1];
		Node.List[] outList = new Node.List[1];
		Node.List[] outElseList = new Node.List[1];
		parseControl(false, "with", outPos, outPipe, outList, outElseList);

		return newWith(outPos[0], outPipe[0], outList[0], outElseList[0]);
	}

	private void parseControl(boolean allowElseIf, String context,
				  int[] outPos, Node.Pipe[] outPipe,
				  Node.List[] outList, Node.List[] outElseList) throws ParseException, InternalException
	{
		int varsSize = vars.size();
		try {
			outPipe[0] = pipeline(context);
			Node[] next = new Node[1];
			if (context.equals("for"))
				++forDepth;
			tokenList(outList, next);
			if (context.equals("for"))
				--forDepth;

			if (next[0].type == Node.Type.ELSE)
				if (allowElseIf && peek().type == Token.Type.IF) {
					/*
					 * Special case for "else if". If the "else" is followed immediately by an "if",
					 * the elseControl will have left the "if" token pending. Treat
					 *  {if a} {else if b} {end}
					 * as
					 *  {if a} {else}{if b} {end}{end}.
					 *  To do this, runParser the "if" as usual and stop at it {end}; the subsequent {end}
					 *  is assumed. This technique works even for long if-else-if chains
					 */
					/* Consume the "if" token */
					next();
					outElseList[0] = newList(next[0].pos);
					outElseList[0].append(ifControl());

				} else { /* Don't consume the next item - only one {end} required */
					tokenList(outElseList, next);
					if (next[0].type != Node.Type.END)
						errorf("expected end; found %s", next[0]);
				}
		} finally {
			popVars(varsSize);
		}
		outPos[0] = outPipe[0].pos;
	}

	Node.List newList(int pos)
	{
		return new Node.List(this, pos);
	}

	Node.Text newText(int pos, String text)
	{
		return new Node.Text(this, pos, text);
	}

	Node.Pipe newPipeline(int pos, List<Node.Assign> decl)
	{
		return new Node.Pipe(this, pos, decl);
	}

	Node.Assign newVariable(int pos, String ident)
	{
		return new Node.Assign(this, pos, Arrays.asList(ident.split("\\.")));
	}

	Node.Command newCommand(int pos)
	{
		return new Node.Command(this, pos);
	}

	Node.Action newAction(int pos, Node.Pipe pipe)
	{
		return new Node.Action(this, pos, pipe);
	}

	Node.Identifier newIdentifier(int pos, String ident)
	{
		return new Node.Identifier(this, pos, ident);
	}

	Node.Dot newDot(int pos)
	{
		return new Node.Dot(this, pos);
	}

	Node.Null newNull(int pos)
	{
		return new Node.Null(this, pos);
	}

	Node.Field newField(int pos, String ident)
	{
		/* substring(1) to drop leading dot */
		return new Node.Field(this, pos, Arrays.asList(ident.substring(1).split("\\.")));
	}

	Node.Chain newChain(int pos, Node node)
	{
		return new Node.Chain(this, pos, node);
	}

	Node.Bool newBool(int pos, boolean boolVal)
	{
		return new Node.Bool(this, pos, boolVal);
	}

	Node.Number newNumber(int pos, String text, Token.Type type) throws ParseException
	{
		Node.Number n = new Node.Number(this, pos, text);
		if (type == Token.Type.CHAR_CONSTANT) {
			char c;
			StringBuilder tail = new StringBuilder();
			try {
				c = Utils.unquoteChar(text.substring(1, text.length()),
						      text.charAt(0), tail);
			} catch (IllegalArgumentException e) {
				throw new ParseException(e);
			}
			if (!tail.toString().equals("'"))
				throw new ParseException(String.format("malformed character constant: %s", text));
			n.isInt = true;
			n.intVal = (int)c;
			n.isFloat = true;
			n.floatVal = (double)c;

			return n;
		}

		boolean isNegative = text.startsWith("-");
		/* Trim leading sign */
		String unsignedNum;
		if (isNegative || text.startsWith("+"))
			unsignedNum = text.substring(1, text.length());
		else
			unsignedNum = text;
		if (!isNegative && unsignedNum.startsWith("-"))
			throw new ParseException(String.format("illegal number syntax: %s", text));
		try {
			long i; /* This is long for int overflow detection */
			if (unsignedNum.toLowerCase().startsWith("0x")) /* Is hex */
				/* Ignore leading 0x */
				i = Long.parseLong(unsignedNum.substring(2), 16);
			else if (text.charAt(0) == '0') /* Is octal */
				i = Long.parseLong(unsignedNum, 8);
			else
				i = Long.parseLong(unsignedNum);
			if (i > Integer.MAX_VALUE || i < Integer.MIN_VALUE)
				throw new ParseException(String.format("integer overflow: %s", text));
			n.isInt = true;
			n.intVal = (int)i;
		} catch (NumberFormatException e) {
			/* Ignore */
		}

		/* If an integer extraction succeeded, promote the float */
		if (n.isInt) {
			n.isFloat = true;
			n.floatVal = (double)n.intVal;
		} else {
			try {
				double f = Double.parseDouble(unsignedNum);
				/*
				 * If we parsed it as a float but it
				 * looks like an integer, it's a huge number
				 * too large to fit in a long. Reject it
				 */
				if (!Utils.containsAny(unsignedNum, ".eE"))
					throw new ParseException(String.format("integer overflow: %s", text));
				n.isFloat = true;
				n.floatVal = f;
				/*
				 * if a floating-point extraction succeeded,
				 * extract the int if needed
				 */
				if (!n.isInt && (double)(int)f == f) {
					n.isInt = true;
					n.intVal = (int)f;
				}
			} catch (NumberFormatException e) {
				/* Ignore */
			}
		}
		if (isNegative) {
			n.intVal = -n.intVal;
			n.floatVal = -n.floatVal;
		}

		if (!n.isInt && !n.isFloat)
			throw new ParseException(String.format("illegal number syntax: %s", text));

		return n;
	}

	Node.StringConst newString(int pos, String orig, String text)
	{
		return new Node.StringConst(this, pos, orig, text);
	}

	Node.End newEnd(int pos)
	{
		return new Node.End(this, pos);
	}

	Node.Else newElse(int pos)
	{
		return new Node.Else(this, pos);
	}

	Node.If newIf(int pos, Node.Pipe pipe,
		      Node.List list, Node.List elseList)
	{
		return new Node.If(this, pos, pipe, list, elseList);
	}

	Node.For newFor(int pos, Node.Pipe pipe,
			Node.List list, Node.List elseList)
	{
		return new Node.For(this, pos, pipe, list, elseList);
	}

	Node.Break newBreak(int pos)
	{
		return new Node.Break(this, pos);
	}

	Node.Continue newContinue(int pos)
	{
		return new Node.Continue(this, pos);
	}

	Node.With newWith(int pos, Node.Pipe pipe,
			  Node.List list, Node.List elseList)
	{
		return new Node.With(this, pos, pipe, list, elseList);
	}

	Node.Template newTemplate(int pos, String name, Node.Pipe pipe)
	{
		return new Node.Template(this, pos, name, pipe);
	}
}

