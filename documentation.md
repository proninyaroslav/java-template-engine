Java Template Engine documentation
=====================

## Table of contents

* [Actions](documentation.md#actions)
* [Arguments](documentation.md#arguments)
* [Pipelines](documentation.md#pipelines)
* [Variables](documentation.md#variables)
* [Examples](documentation.md#examples)
* [Functions](documentation.md#Functions)
* [Associated templates](documentation.md#associated-templates)
* [Nested template definitions](documentation.md#nested-template-definitions)
* [API](documentation.md#api)

Templates are executed by applying them to the class object. Annotations in the template refer to fields and methods of the class object to control execution and derive values to be displayed. Execution of the template walks the object and sets the cursor, represented by a period `.` and called "dot", to the value at the current location in the object as execution proceeds.

The input text for a template is UTF-8-encoded text in any format. "Actions" - data evaluations or control structures - are delimited by `{{` and `}}` (which can be replaced by other symbols); all text outside actions is copied to the output unchanged. Except for raw strings, actions may not span newlines, although comments can.

Once parsed, a template may be executed safely in parallel, although if parallel executions share a `OutputStream` the output may be interleaved.

This is a simple example that prints "Hello World!":

```Java
Template template = new Template("example");
template.parse("{{`Hello World!`}}");
template.execute(System.out, new Example());
```

More intricate example in [Example.java](https://gitlab.com/proninyaroslav/java-template-engine/blob/master/src/test/java/ru/proninyaroslav/template/example/Example.java) file.

Actions
---

There is the list of actions. "Arguments" and "pipelines" are evaluations of data, defined in detail in the corresponding sections that follow:

```
{{/* a comment */}}
	A comment; discarded. May contain newlines.
	Comments do not nest and must start and end at the
	delimiters, as shown here.

{{pipeline}}
	The default textual representation (the same as would be
	printed by System.out.print) of the value of the pipeline is copied
	to the output.

{{if pipeline}} T1 {{end}}
	If the value of the pipeline is empty, no output is generated;
	otherwise, T1 is executed. The empty values are false, 0,
	null pointer, and any array, Collection, Map, or
	String of length zero. Dot is unaffected.

{{if pipeline}} T1 {{else}} T0 {{end}}
	If the value of the pipeline is empty, T0 is executed;
	otherwise, T1 is executed. Dot is unaffected.

{{if pipeline}} T1 {{else if pipeline}} T0 {{end}}
	To simplify the appearance of if-else chains, the else action
	of an if may include another if directly; the effect is exactly
	the same as writing
		{{if pipeline}} T1 {{else}}{{if pipeline}} T0 {{end}}{{end}}

{{for pipeline}} T1 {{end}}
	The value of the pipeline must be an iterable object, like array,
	Collection or Map. If the value of the pipeline has length zero or null,
	nothing is output; otherwise, dot is set to the successive elements
	of the iterable object and T1 is executed.

{{range pipeline}} T1 {{else}} T0 {{end}}
	The value of the pipeline must be an iterable object, like array,
	Collection or Map. If the value of the pipeline has length zero or null,
	dot is unaffected and T0 is executed; otherwise, dot is set to
	the successive elements of the iterable object and T1 is executed.

{{break}}
	Break out of the surrounding for loop.

{{continue}}
	Begin the next iteration of the surrounding for loop.

{{template "name"}}
	The template with the specified name is executed with null data.

{{template "name" pipeline}}
	The template with the specified name is executed with dot set
	to the value of the pipeline.

{{block "name" pipeline}} T1 {{end}}
	A block is shorthand for defining a template
		{{define "name"}} T1 {{end}}
	and then executing it in place
		{{template "name" .}}
	The typical use is to define a set of root templates that are
	then customized by redefining the block templates within.

{{with pipeline}} T1 {{end}}
	If the value of the pipeline is empty, no output is generated;
	otherwise, dot is set to the value of the pipeline and T1 is
	executed.

{{with pipeline}} T1 {{else}} T0 {{end}}
	If the value of the pipeline is empty, dot is unaffected and T0
	is executed; otherwise, dot is set to the value of the pipeline
	and T1 is executed.
```

Arguments
---

An argument is a simple value, denoted by one of the following:

```
- A boolean, string, character, integer or floating-point
  constant in Java syntax. These behave like Java's constants.
- The keyword null, representing an untyped Java null.
- The character '.' (period):
	.
  The result is the value of dot.
- A variable name, which is a (possibly empty) alphanumeric string
  preceded by a dollar sign, such as
	$var1
  or
	$
  The result is the value of the variable.
  Variables are described below.
- The name of a field of the data, which must be a public object,
  preceded by a period, such as
	.field
  The result is the value of the field. Field invocations may be
  chained:
    .field1.field2
  Fields can also be evaluated on variables, including chaining:
    $x.field1.field2
- The name of a method of the data, preceded by a period,
  such as
	.method
  The result is the value of invoking the method with dot as the
  receiver, dot.method(). Such a method must have return value (of
  any type) and must be public.
  If method throws exception, execution terminates.
  Method invocations may be chained and combined with fields
  to any depth:
    .field1.method1.field2.method2
  Methods can also be evaluated on variables, including chaining:
    $x.method1.field
  Field and method with one name in the same class are not allowed.
- The name of a function, such as
	func
  The result is the value of invoking the function, func(). The return
  types, values and throws behave as in methods.
  Also function must have a 'public static' modifier.
  Functions and function names are described below.
- A parenthesized instance of one the above, for grouping. The result
  may be accessed by a field invocation.
	print (.f1 arg1) (.f2 arg2)
	(.method "arg").field
```

Arguments may evaluate to any type; if they are classes the implementation automatically indirects to the base class when required. If an evaluation yields a function value, such as a function-valued field of a class, the function is not invoked automatically, but it can be used as a truth value for an if action and the like. To invoke it, use the call function, defined below.

Pipelines
---

A pipeline is a possibly chained sequence of "commands". A command is a simple value (argument) or a function or method call, possibly with multiple arguments:

```
argument
	The result is the value of evaluating the argument.
.method [argument...]
	The method can be alone or the last element of a chain but,
	unlike methods in the middle of a chain, it can take arguments.
	The result is the value of calling the method with the
	arguments:
		dot.method(argument1, etc.)
functionName [argument...]
	The result is the value of calling the function associated
	with the name:
		function(argument1, etc.)
	Functions and function names are described below.
```

A pipeline may be "chained" by separating a sequence of commands with pipeline characters `|`. In a chained pipeline, the result of each command is passed as the last argument of the following command. The output of the final command in the pipeline is the value of the pipeline.

Variables
---

A pipeline inside an action may initialize a variable to capture the result. The initialization has syntax

`$variable := pipeline`

where `$variable` is the name of the variable. An action that declares a variable produces no output.

Variables previously declared can also be assigned, using the syntax

`$variable = pipeline`

If a `for` action initializes a variable, the variable is set to the successive elements of the iteration.

A variable's scope extends to the `end` action of the control structure (`if`, `with`, or `for`) in which it is declared, or to the end of the template if there is no such control structure. A template invocation does not inherit variables from the point of its invocation.

When execution begins, `$` is set to the data argument passed to `Template::execute()`, that is, to the starting value of dot.

Examples
---

Here are some example one-line templates demonstrating pipelines and variables. All produce the quoted word "output":

```
{{"\"output\""}}
	A string constant.
{{`"output"`}}
	A raw string constant.
{{printf "%s" "output"}}
	A function call.
{{"output" | printf "%s"}}
	A function call whose final argument comes from the
	previous command.
{{printf "%s" (print "out" "put")}}
	A parenthesized argument.
{{"put" | printf "%s%s" "out" | printf "%s"}}
	A more elaborate call.
{{"output" | printf "%s" | printf "%s"}}
	A longer chain.
{{with "output"}}{{printf "%s" .}}{{end}}
	A with action using dot.
{{with $x := "output" | printf "%s"}}{{$x}}{{end}}
	A with action that creates and uses a variable.
{{with $x := "output"}}{{printf "%s" $x}}{{end}}
	A with action that uses the variable in another action.
{{with $x := "output"}}{{$x | printf "%s"}}{{end}}
	The same, but pipelined.
```

Functions
---

During execution functions are found in two function maps: first in the template, then in the
global function map. By default, no functions are defined in the template but the `Template::addFuncs` method can be used to add them.

Predefined global functions are named as follows:

```
and
	Computes the boolean AND of its arguments, returning the
	first false argument it encounters, or the last argument
index
	Returns the result of indexing its first argument by the
	following arguments, e.g index x 1 2 3 returns x[1][2][3]
	(or x.get(1).get(2).get(3) if object is List or Map)
not
	Returns the boolean negation of its argument
or
	Computes the boolean OR of its arguments, returning the
	first true argument it encounters, or the last argument
print
	Uses the default formats for its arguments and returns the
	resulting string. Spaces are added between
	arguments when neither is a string
printf
	An alias for PrintStream::printf
println
	Uses the default formats for its arguments and returns the
	resulting string. Spaces are always added between
	operands and a newline is appended
range
	Generate number sequence from 0 (or start value if defined)
	to stop with a given step (default 1)
```

The boolean functions take any zero value to be false and a non-zero value to be true.

There is also a set of binary comparison and arithmetic operators defined as functions:

```
eq
	Returns the boolean truth of arg1 == arg2
ne
	Returns the boolean truth of arg1 != arg2
lt
	Returns the boolean truth of arg1 < arg2
le
	Returns the boolean truth of arg1 <= arg2
gt
	Returns the boolean truth of arg1 > arg2
ge
	Returns the boolean truth of arg1 >= arg2
add
	Evaluates arg1 + arg2
sub
	Evaluates arg1 - arg2
mul
	Evaluates arg1 * arg2
div
	Evaluates arg1 / arg2
mod
	Evaluates arg1 % arg2
```

For simpler multi-way equality tests, eq (only) accepts two or more arguments and compares the second and subsequent to the first, returning in effect

`arg1 == arg2 || arg1 == arg3 || arg1 == arg4 ...`

The comparison functions (except `eq`) work on number or character types only. They implement the Java rules for comparison of values.

Associated templates
---

Each template is named by a string specified when it is created. Also, each template is associated with zero or more other templates that it may invoke by name; such associations are transitive and form a name space of templates.

A template may use a template invocation to instantiate another associated template; see the explanation of the "template" action above. The name must be that of a template associated with the template that contains the invocation.

Nested template definitions
---

 When parsing a template, another template may be defined and associated with the template being parsed. Template definitions must appear at the top level of the template.

The syntax of such definitions is to surround each template declaration with a "define" and "end" action.

The define action names the template being created by providing a string constant. Here is a simple example:

```
{{define "T1"}}FIRST{{end}}
{{define "T2"}}SECOND{{end}}
{{define "T3"}}{{template "T1"}} {{template "T2"}}{{end}}
{{template "T3"}}
```

This defines two templates, T1 and T2, and a third T3 that invokes the other two when it is executed. Finally it invokes T3. If executed this template will produce the text

`FIRST SECOND`

By construction, a template may reside in only one association. If it's necessary to have a template addressable from multiple associations, the template definition must be parsed multiple times to create distinct `Template` values, or must be copied with the  `Template::addParseTree` method.

Parse may be called multiple times to assemble the various associated templates; see the `Template::parseTo` method for simple ways to parse related templates stored in files.

A template may be executed directly or through `Template::executeTemplate`, which executes an associated template identified by name. To invoke our example above, we might write,

`template.execute(System.out, "no data needed");`

or to invoke a particular template explicitly by name,

`template.executeTemplate(System.out, "T2", "no data needed")`

API
---

You can see Javadoc [here](http://www.javadoc.io/doc/ru.proninyaroslav/java-template-engine).