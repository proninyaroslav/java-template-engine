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

class Token
{
	public enum Type
	{
		ERROR,              /* error occurred; value is text of error */
		TEXT,               /* plain text */
		BOOL,
		CHAR,               /* printable ASCII character */
		CHAR_CONSTANT,
		NUMBER,             /* simple number, including float number */
		SPACE,              /* spaces for separating arguments */
		STRING,             /* quoted string (includes quotes) */
		RAW_STRING,         /* raw quoted string with newlines (includes quotes) */
		DECLARE,	    /* colon-equals (':=') introducing a declaration */
		ASSIGN,		    /* equals ('=') introducing an assignment */
		EOF,
		FIELD,              /* alphanumeric identifier starting with '.' */
		IDENTIFIER,         /* alphanumeric identifier not starting with '.' */
		VARIABLE,           /* variable starting with '$' */
		LEFT_DELIM,         /* left action delimiter */
		RIGHT_DELIM,        /* right action delimiter */
		LEFT_PAREN,         /* '(' inside action */
		RIGHT_PAREN,        /* ')' inside action */
		PIPE,               /* pipe symbol ('|') */
		/* Keywords appear after all the rest */
		KEYWORD,            /* used only to delimit the keywords */
		DOT,                /* the cursor, spelled '.' */
		NULL,               /* easiest to treat as a keyword */
		IF,
		FOR,                /* for-each loop */
		BREAK,              /* break keyword */
		CONTINUE,           /* continue keyword */
		WITH,
		ELSE,
		END,
		DEFINE,
		TEMPLATE
	}

	public Type type;
	public int pos;		/* the starting position, in bytes, of this token in the input string */
	public String val;	/* the value of this token */
	public int line;	/* the line number at the start of this token */

	public Token(Type type, int pos, String val, int line)
	{
		this.type = type;
		this.pos = pos;
		this.val = val;
		this.line = line;
	}

	public Token(Type type, String val)
	{
		this.type = type;
		this.pos = 0;
		this.val = val;
		this.line = 0;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		if (type == Type.EOF)
			sb.append("EOF");
		else if (type.ordinal() > Type.KEYWORD.ordinal())
			sb.append(String.format("<%s>", val));
		else if (val.length() > 50)
			sb.append(String.format("'%.50s...'", val));
		else
			sb.append(String.format("'%s'", val));

		return sb.toString();
	}
}
