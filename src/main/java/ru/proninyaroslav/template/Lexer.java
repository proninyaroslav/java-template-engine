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

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class Lexer implements Runnable
{
	private static HashMap<String, Token.Type> initWords()
	{
		HashMap<String, Token.Type> map = new HashMap<>();

		map.put(".", Token.Type.DOT);
		map.put("define", Token.Type.DEFINE);
		map.put("else", Token.Type.ELSE);
		map.put("end", Token.Type.END);
		map.put("if", Token.Type.IF);
		map.put("for", Token.Type.FOR);
		map.put("break", Token.Type.BREAK);
		map.put("continue", Token.Type.CONTINUE);
		map.put("with", Token.Type.WITH);
		map.put("null", Token.Type.NULL);
		map.put("template", Token.Type.TEMPLATE);

		return map;
	}

	private enum State {
		lexText,
		lexLeftDelim,
		lexComment,
		lexInsideAction,
		lexRightDelim,
		lexSpace,
		lexQuote,
		lexRawQuote,
		lexVariable,
		lexChar,
		lexField,
		lexNumber,
		lexIdentifier
	}

	private static final HashMap<String, Token.Type> words = initWords();
	private static final char EOF = '\0';
	private static final char MAX_ASCII = '\u007F'; /* maximum ASCII value */
	private static final String defaultLeftDelim = "{{";
	private static final String defaultRightDelim = "}}";
	private static final String leftComment  = "/*";
	private static final String rightComment = "*/";

	private String name;            /* the name of the input; used only for error reports */
	private String input;           /* the string being scanned */
	private String leftDelim;
	private String rightDelim;
	private int pos;                /* current position in the input */
	private int start;              /* start position of this token */
	private int parenDepth;         /* nesting depth of ( ) exprs */
	private int line;               /* 1 + number of newlines seen */
	private LinkedBlockingQueue<Token> tokens;

	public Lexer(String name, String input, String leftDelim, String rightDelim)
	{
		this.name = name;
		this.input = input;
		this.leftDelim = (leftDelim == null ? defaultLeftDelim : leftDelim);
		this.rightDelim = (rightDelim == null ? defaultRightDelim : rightDelim);
		tokens = new LinkedBlockingQueue<>();
		line = 1;

		new Thread(this).start();
	}

	@Override
	public void run()
	{
		State state = State.lexText;
		while (state != null)
			state = callStateFn(state);
	}

	/**
	 * Drains the output so the lexing thread will exit.
	 * Called by the parser, not in the lexing thread
	 */
	public void drain()
	{
		tokens.clear();
	}

	/**
	 * Called by the parser, not in the lexing thread
	 */
	public Token nextToken() throws InternalException
	{
		Token token;
		try {
			token = tokens.poll(1000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new InternalException(e);
		}

		return token;
	}

	private State callStateFn(State state)
	{
		switch (state) {
			case lexText:
				return lexText();
			case lexLeftDelim:
				return lexLeftDelim();
			case lexComment:
				return lexComment();
			case lexInsideAction:
				return lexInsideAction();
			case lexRightDelim:
				return lexRightDelim();
			case lexSpace:
				return lexSpace();
			case lexQuote:
				return lexQuote();
			case lexRawQuote:
				return lexRawQuote();
			case lexVariable:
				return lexVariable();
			case lexChar:
				return lexChar();
			case lexField:
				return lexField();
			case lexNumber:
				return lexNumber();
			case lexIdentifier:
				return lexIdentifier();
		}

		return null;
	}

	/**
	 * Backup steps back one character. Can only be called once per call of next
	 */
	private void backup()
	{
		--pos;
		if (pos < input.length() && input.charAt(pos) == '\n')
			--line;
	}

	/**
	 * Peek returns but does not consume the next character in the input
	 */
	private char peek()
	{
		char c = next();
		backup();

		return c;
	}

	/**
	 * Returns the next character in the input
	 */
	private char next()
	{
		if (pos >= input.length()) {
			++pos;
			return EOF;
		}

		char c = input.charAt(pos++);
		if (c == '\n')
			++line;

		return c;
	}

	private int countNewlines(String s)
	{
		int count = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '\n')
				++count;
		}

		return count;
	}

	private void emit(Token.Type type) throws InternalError
	{
		String val = input.substring(start, pos);
		if (!tokens.offer(new Token(type, start, val, line)))
			throw new InternalError("lex queue is full: " + tokens.size());
		/* Some items contain text internally. If so, count their newlines */
		switch (type) {
			case TEXT:
			case RAW_STRING:
			case LEFT_DELIM:
			case RIGHT_DELIM:
				line += countNewlines(val);
				break;
		}
		start = pos;
	}

	private State errorf(String format, Object... args)
	{
		tokens.add(new Token(Token.Type.ERROR, start, String.format(format, args), line));

		return null;
	}

	/**
	 * Skips over the pending input before this point
	 */
	private void ignore()
	{
		start = pos;
	}

	/**
	 * Accept advances the lexer if the next character is in valid
	 */
	private boolean accept(String valid)
	{
		if (valid.indexOf(next()) >= 0)
			return true;
		backup();

		return false;
	}

	/**
	 * Advances position as long as the current character is in valid
	 */
	private void acceptAll(String valid)
	{
		while (valid.indexOf(next()) >= 0);

		backup();
	}

	/**
	 * Scans until an opening action delimiter
	 */
	private State lexText()
	{
		int i = input.substring(pos).indexOf(leftDelim);
		if (i >= 0) {
			pos += i;
			if (pos > start)
				emit(Token.Type.TEXT);
			ignore();

			return State.lexLeftDelim;
		} else {
			pos = input.length();
		}

		/* Correctly reached EOF */
		if (pos > start)
			emit(Token.Type.TEXT);
		emit(Token.Type.EOF);

		return null;
	}

	private State lexLeftDelim()
	{
		pos += leftDelim.length();
		if (input.startsWith(leftComment, pos)) {
			ignore();
			return State.lexComment;
		}
		emit(Token.Type.LEFT_DELIM);
		ignore();
		parenDepth = 0;

		return State.lexInsideAction;
	}

	/**
	 * Scans a comment. The left comment marker is known to be present
	 */
	private State lexComment()
	{
		pos += leftComment.length();
		int i = input.substring(pos).indexOf(rightComment);
		if (i < 0)
			return errorf("unclosed comment");
		pos += i + rightComment.length();

		if (!input.startsWith(rightDelim, pos))
			return errorf("comment ends before closing delimiter");
		pos += rightDelim.length();
		ignore();

		return State.lexText;
	}

	/**
	 * Scans the tokens inside action delimiters
	 */
	private State lexInsideAction()
	{
		/*
		 * Either number, quoted string, or identifier.
		 * Spaces separate arguments; string of spaces turn
		 * into SPACE token. Pipe symbols separate and are emitted
		 */

		if (input.startsWith(rightDelim, pos)) {
			if (parenDepth == 0)
				return State.lexRightDelim;
			return errorf("unclosed left paren");
		}

		char c = next();
		if (c == EOF || Utils.isEndOfLine(c)) {
			return errorf("unclosed action");
		} else if (Utils.isSpace(c)) {
			return State.lexSpace;
		} else if (c == '=') {
			emit(Token.Type.ASSIGN);
		} else if (c == ':') {
			if (next() != '=')
				errorf("expected :=");
			emit(Token.Type.DECLARE);
		} else if (c == '|') {
			emit(Token.Type.PIPE);
		} else if (c == '"') {
			return State.lexQuote;
		} else if (c == '`') {
			return State.lexRawQuote;
		} else if (c == '$') {
			return State.lexVariable;
		} else if (c == '\'') {
			return State.lexChar;
		} else if (c == '.') {
			/* special look-ahead for ".field" so we don't break backup() */
			if (pos < input.length()) {
				char ch = input.charAt(pos);
				if (ch < '0' || ch > '9')
					return State.lexField;
			}
			backup();
			return State.lexNumber;
		} else if (c == '+' || c == '-' || ('0' <= c && c <= '9')) {
			backup();
			return State.lexNumber;
		} else if (Utils.isAlphaNumeric(c)) {
			backup();
			return State.lexIdentifier;
		} else if (c == '(') {
			emit(Token.Type.LEFT_PAREN);
			++parenDepth;
		} else if (c == ')') {
			emit(Token.Type.RIGHT_PAREN);
			--parenDepth;
			if (parenDepth < 0)
				errorf("unexpected right paren %c", c);
		} else if (c <= MAX_ASCII) { //TODO: fix character
			emit(Token.Type.CHAR);
			return State.lexInsideAction;
		} else {
			return errorf("unrecognized character in action: %c", c);
		}

		return State.lexInsideAction;
	}

	private State lexRightDelim()
	{
		pos += rightDelim.length();
		emit(Token.Type.RIGHT_DELIM);

		return State.lexText;
	}

	/**
	 * Scans a string of space characters.
	 * One space has already been seen
	 */
	private State lexSpace()
	{
		while (Utils.isSpace(peek()))
			next();
		emit(Token.Type.SPACE);

		return State.lexInsideAction;
	}

	/**
	 * Scans a quoted string
	 */
	private State lexQuote()
	{
		loop:
		for (;;) {
			switch (next()) {
				case '\\':
					char c = next();
					if (c != EOF && c != '\n')
						break;
				case EOF:
				case '\n':
					return errorf("unterminated quoted string");
				case '"':
					break loop;
			}
		}
		emit(Token.Type.STRING);

		return State.lexInsideAction;
	}

	/**
	 * Scans a raw quoted string (`test`)
	 */
	private State lexRawQuote()
	{
		int startLine = line;
		loop:
		for (;;) {
			switch (next()) {
				case EOF:
					/*
					 * Restore line number to location of opening quote.
					 * We will error out so it's ok just to overwrite the field
					 */
					line = startLine;
					return errorf("unterminated raw quoted string");
				case '`':
					break loop;
			}
		}
		emit(Token.Type.RAW_STRING);

		return State.lexInsideAction;
	}

	/**
	 * Scans a variable: $alphanumeric.
	 * The $ has been scanned
	 */
	private State lexVariable()
	{
		if (atTerminator()) {
			emit(Token.Type.VARIABLE);
			return State.lexInsideAction;
		}

		return lexFieldOrVariable(Token.Type.VARIABLE);
	}

	private State lexField()
	{
		return lexFieldOrVariable(Token.Type.FIELD);
	}

	/**
	 * Reports whether the input is at valid termination character to
	 * appear after an identifier. Breaks .X.Y into two pieces
	 */
	private boolean atTerminator()
	{
		char c = peek();
		if (Utils.isSpace(c) || Utils.isEndOfLine(c))
			return true;
		// TODO: perhaps add arithmetic
		switch (c) {
			case EOF:
			case '.':
			case '|':
			case '(':
			case ')':
			case '=':
				return true;
		}
		/*
		 * Does c start the delimiter? This can be ambiguous (with delim == "//", $x/2 will
		 * succeed but should fail) but only in extremely rare cases caused by willfully
		 * bad choice of delimiter.
		 */
		if (c == rightDelim.charAt(0))
			return true;

		return false;
	}

	/**
	 * Scans a field or variable: [.$]alphanumeric.
	 * The . or $ has been scanned
	 */
	private State lexFieldOrVariable(Token.Type type)
	{
		if (atTerminator()) {
			if (type == Token.Type.VARIABLE)
				emit(Token.Type.VARIABLE);
			else
				emit(Token.Type.DOT);

			return State.lexInsideAction;
		}
		char c;
		for (;;) {
			c = next();
			if (!Utils.isAlphaNumeric(c)) {
				backup();
				break;
			}
		}
		if (!atTerminator())
			return errorf("bad character %c", c);
		emit(type);

		return State.lexInsideAction;
	}

	/**
	 * Scans a character constant. The initial quote is already scanned.
	 * Syntax checking is done by the parser
	 */
	private State lexChar()
	{
		loop:
		for (;;) {
			switch (next()) {
				case '\\':
					char c = next();
					if (c != EOF && c != '\n')
						break;
				case EOF:
				case '\n':
					return errorf("unterminated character constant");
				case '\'':
					break loop;
			}
		}
		emit(Token.Type.CHAR_CONSTANT);

		return State.lexInsideAction;
	}

	/**
	 * Scans a number: decimal, octal, hex or float
	 */
	private State lexNumber()
	{
		/* Optional leading sign */
		accept("+-");
		String digits = "0123456789";
		/* It's hex? */
		if (accept("0") && accept("xX"))
			digits = "0123456789abcdefABCDEF";

		acceptAll(digits);
		if (accept("."))
			acceptAll(digits);
		if (accept("eE")) {
			accept("+-");
			acceptAll("0123456789");
		}
		/* Next character mustn't be alphanumeric */
		if (Utils.isAlphaNumeric(peek())) {
			next();
			return errorf("bad number syntax: '%s'", input.substring(start, pos));
		}
		emit(Token.Type.NUMBER);

		return State.lexInsideAction;
	}

	/**
	 * Scans an alphanumeric identifier or reserved word
	 */
	private State lexIdentifier()
	{
		for (;;) {
			char c = next();
			if (!Utils.isAlphaNumeric(c)) {
				backup();
				String word = input.substring(start, pos);
				if (!atTerminator())
					return errorf("bad character %c", c);
				Token.Type key = words.get(word);
				if (key != null)
					emit(key);
				else if (word.charAt(0) == '.')
					emit(Token.Type.FIELD);
				else if (word.equals("true") || word.equals("false"))
					emit(Token.Type.BOOL);
				else
					emit(Token.Type.IDENTIFIER);
				break;
			}
		}

		return State.lexInsideAction;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for (Token t : tokens)
			sb.append(t).append('\n');

		return sb.toString();
	}
}
