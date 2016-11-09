grammar ManchesterSyntaxParser;

options {
  language  = Java;
  output   = AST;
  //backtrack = true;
}

tokens {
	AST_BASECLASS;
	AST_OR;
	AST_AND;
	AST_NOT;
	AST_SOME;
	AST_ONLY;
	AST_CARDINALITY;
	AST_VALUE;
	AST_ONEOFLIST;
	AST_PREFIXED_NAME;
}


@header {
package it.uniroma2.art.semanticturkey.syntax.manchester;
}

@lexer::header {
package it.uniroma2.art.semanticturkey.syntax.manchester;
}




/* NEW */

manchesterExpression
	:
	description  '.'?
	;

description 
@init { int N = 0; }
	: 
	conjunction ( OR conjunction {++N;})*
	-> {N>0}? ^(AST_OR conjunction+) 
	-> conjunction+  
	;

conjunction
@init { int N = 0; } 
	:
	primary ( AND primary {++N;})*
	-> {N>0}? ^(AST_AND primary+)
	-> primary+
	;

primary 
	:
	NOT primary2
	-> ^(AST_NOT primary2)
	|
	primary2
	//-> primary2
	;
	
primary2
	: restriction
	//-> ^(AST_NOT restriction)
	|
	atomic 
	//-> ^(AST_NOT atomic)
	
	;

restriction
	:
	prop (qual=SOME | qual=ONLY) primary
	-> {$qual.getType() == ONLY}? ^(AST_ONLY prop primary)
	-> ^(AST_SOME prop primary)
	|
	prop (card=MIN | card=MAX | card=EXACTLY ) INTEGER
	-> ^(AST_CARDINALITY prop $card INTEGER)
	|
	prop VALUE value
	-> ^(AST_VALUE prop value)
	;

atomic
	:
	simpleManchesterClass
	-> ^(AST_BASECLASS simpleManchesterClass)
	|
	'{' oneOfList '}'
	-> oneOfList
	|
	'(' description  ')'
	-> description 
	;

simpleManchesterClass
	:
	res
	;

/* END NEW */


/* OLD VERSION

manchesterExpression
	:
	manchesterClass '.'?
	;

manchesterClass 
	:
	simpleManchesterClass
	-> ^(AST_BASECLASS simpleManchesterClass)
	|
	'(' manchesterClass ( (boolSymb=OR manchesterClass)+ | (boolSymb=AND manchesterClass)+ )')'
	-> {$boolSymb.getType() == OR}? ^(AST_OR manchesterClass+)
	-> ^(AST_AND manchesterClass+)
	|
	NOT manchesterClass
	-> ^(AST_NOT manchesterClass)
	|
	'{' oneOfList '}'
	-> oneOfList
	|
	prop (qual=SOME | qual=ONLY) manchesterClass
	-> {$qual.getType() == ONLY}? ^(AST_ONLY prop manchesterClass)
	-> ^(AST_SOME prop manchesterClass)
	|
	prop (card=MIN | card=MAX | card=EXACTLY ) INTEGER
	-> ^(AST_CARDINALITY prop $card INTEGER)
	|
	prop VALUE value
	-> ^(AST_VALUE prop value)
	; 


simpleManchesterClass
	:
	res
	;
*/
	
oneOfList
	:
	res (',' res)*
	-> ^(AST_ONEOFLIST res+)
	;
	
prop
	:
	res
	;

value
	:
	res | rdfLiteral
	;

res
	:
	//IRIREF
	IRIREF | prefixedName
	;

prefixedName
  :
  PNAME_LN 
  -> ^(AST_PREFIXED_NAME PNAME_LN)
  |
  PNAME_NS
  -> ^(AST_PREFIXED_NAME PNAME_NS)
  ;


rdfLiteral 	  
	:
	//string ( LANGTAG | ( '^^' IRIREF ) )?
	string ( LANGTAG | ( '^^' res ) )?
	;


string
	:
	STRING_LITERAL1 | STRING_LITERAL2
	;
	
	

WS:     (' ' | '\t' | '\f' |'\r')+ {$channel=HIDDEN;};
NEWLINE:  '\n' {$channel=HIDDEN;};
COMMENT: '//' .* ('\n' | '\r') {$channel=HIDDEN;};
MULTILINE_COMMENT:  '/*' (options {greedy=false;} : .)* '*/' {$channel=HIDDEN;};
 
IRIREF
  :
  '<' (~('<' | '>' | '"' | '{' | '}' | '|' | '^' | '`' | '\u0000'..'\u0020'))* '>'
  ;


OR
	:
	'OR'|'or'
	;

AND
	:
	'AND'|'and'
	;
	
NOT
	:
	'NOT'|'not'
	;

SOME
	:
	'SOME'|'some'
	;
	
ONLY
	:
	'ONLY'|'only'
	;

MIN
	:
	'MIN'|'min'
	;
	
MAX
	:
	'MAX'|'max'
	;

EXACTLY
	:
	'EXACTLY'|'exactly'
	;
	
VALUE
	:
	'VALUE'|'value'
	;


INTEGER
  :
  '0'..'9'+
  ;
  
  
  fragment
JAVA_LETTER
  :
  'a'..'z' | 'A'..'Z' | '_'
  ;
  
  
  
PNAME_NS
  :
  PN_PREFIX? ':'
  ;
  
PNAME_LN
  :
  PNAME_NS PN_LOCAL
  ;
  
  
 fragment
PN_PREFIX
  :
  PN_CHARS_BASE ((PN_CHARS/*|'.'*/)* PN_CHARS)? // Dot removed since it causes a bug in the generated Lexer
  ;
  
  
  
  
  fragment
PN_CHARS
  :
  PN_CHARS_U | '-' | '0'..'9' | '\u00B7' | '\u0300'..'\u036F' | '\u203F'..'\u2040'
  ;
  
  
  
  fragment
PN_LOCAL
  :
  (PN_CHARS_U | ':' | '0'..'9' | PLX ) ((PN_CHARS | '.' | ':' | PLX)* (PN_CHARS | ':' | PLX) )?
  ;
  
  fragment
PN_CHARS_U
  :
  PN_CHARS_BASE | '_'
  ;
  
  
  fragment
PN_CHARS_BASE
  :
  'A'..'Z' | 'a'..'z'| '\u00C0'..'\u00D6' | '\u00D8'..'\u00F6'| '\u00F8'..'\u02FF' | '\u0370'..'\u037D'|
  '\u037F'..'\u1FFF' | '\u200C'..'\u200D'| '\u2070'..'\u218F' | '\u2C00'..'\u2FEF' | '\u3001'..'\uD7FF' |
  '\uF900'..'\uFDCF' | '\uFDF0'..'\uFFFD' //TODO: add the following | [#x10000-#xEFFFF]
  ;
  
  
  fragment
PLX
  :
  PERCENT | PN_LOCAL_ESC
  ;
  
  
  fragment
PERCENT
  :
  '%' HEX HEX
  ;
  
  fragment
HEX
  :
  '0'..'9' | 'A'..'F' | 'a'..'f'
  ;
  
  fragment
PN_LOCAL_ESC
  :
  '\\' ( '_' | '~' | '.' | '-' | '!' | '$' | '&' | '\'' | '(' | ')' | '*' | '+' | ',' | ';' | '=' | '/' | '?'
  | '#' | '@' | '%' )
  ;
  
  LANGTAG
	:
	'@' ('a'..'z'|'A'..'Z')+ ('-' ('a'..'z'|'A'..'Z'|'0'..'9')+)*
	;

STRING_LITERAL1 	  
	:
	'\'' ( (~('\u0027'|'\u005C'|'\u000A'|'\u000D')) | ECHAR )* '\''
	;


STRING_LITERAL2 	  
	:
	'"' ( (~('\u0022'|'\u005C'|'\u000A'|'\u000D')) | ECHAR )* '"'
	;


fragment
ECHAR
	:
	'\\' ('t'|'b'|'n'|'r'|'f'|'\\'|'\''|'"')
	;
