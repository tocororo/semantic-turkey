grammar ManchesterOWL2SyntaxParser;

options {
  language  = Java;
  //output   = AST;
  //backtrack = true;
}

tokens {
	AST_BASECLASS,
	AST_OR,
	AST_AND,
	AST_NOT,
	AST_SOME,
	AST_ONLY,
	AST_CARDINALITY,
	AST_VALUE,
	AST_ONEOFLIST,
	AST_PREFIXED_NAME
}

/* 
@header {
package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;
}

@lexer::header {
package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;
}
*/

/*manchesterExpression
	:
	description  '.'?
	;*/


description
	:
	descriptionInner EOF
	//conjunction 'or' conjunction ( 'or' conjunction )*
	//| 
	//conjunction
	;

descriptionInner
	:
	conjunction (OR conjunction)* 
	;
	
conjunction 
	: 
	classIRI THAT notRestriction ( AND notRestriction )* 
	| 
	primary (AND primary)*
	//primary 'and' primary ( 'and' primary )* 
	//| 
	//primary
	;
	
notRestriction
	:
	(not=NOT)? restriction
	;
	
primary 
	: (not=NOT)? ( restriction | atomic )
	;
	

restriction
	: 
	objectPropertyExpression type=SOME primary
	|
	objectPropertyExpression type=ONLY primary
	| 
	objectPropertyExpression type=VALUE individual 
	| 
	objectPropertyExpression type=SELF 
	| 
	objectPropertyExpression type=MIN nonNegativeInteger primary?  
	| 
	objectPropertyExpression type=MAX nonNegativeInteger primary? 
	| 
	objectPropertyExpression type=EXACTLY nonNegativeInteger primary? 
	| 
	dataPropertyExpression type=SOME dataPrimary 
	| 
	dataPropertyExpression type=ONLY dataPrimary
	| 
	dataPropertyExpression type=VALUE literal 
	| 
	dataPropertyExpression type=MIN nonNegativeInteger dataPrimary? 
	| 
	dataPropertyExpression type=MAX nonNegativeInteger dataPrimary?
	| 
	dataPropertyExpression type=EXACTLY nonNegativeInteger dataPrimary?
	;


	

objectPropertyExpression 
	:
	objectPropertyIRI | inverseObjectProperty	
	;
	
atomic : 
	classIRI 
	| 
	'{' individualList '}' 
	| 
	'(' descriptionInner ')'	
	;

dataPrimary 
	:
	(not=NOT)?  dataAtomic
	;

dataAtomic 
	: 
	datatype 
	| 
	'{' literalList '}' 
	//| 
	//datatypeRestriction 
	//| 
	//'(' dataRange ')'
	;


//taken form previous version

literal
	:
	//string ( LANGTAG | ( '^^' IRIREF ) )?
	string ( LANGTAG | ( '^^' classIRI ) )?
	;

string
	:
	STRING_LITERAL1 | STRING_LITERAL2
	;
	
datatype 
	: 
	datatypeIRI 
	| 
	abbr=('integer'|'decimal'|'float'|'string')
	;

datatypeIRI
	:
	IRIREF | prefixedName
	;
	

literalList
	:
	literal (',' literal)*
	;

nonNegativeInteger
	:
	INTEGER
	;

individualList
	:
	individual (',' individual)*
	;

individual
	:
	IRIREF | prefixedName
	;


dataPropertyExpression
	:
	IRIREF | prefixedName
	;

objectPropertyIRI
	:
	IRIREF | prefixedName
	;
	
inverseObjectProperty 
	:
	INVERSE objectPropertyIRI
	;

classIRI
	:
	IRIREF | prefixedName
	;	

prefixedName
  :
  PNAME_LN 
  //|
  //PNAME_NS
  ;	

WS:     (' ' | '\t' )+ -> skip;
NEWLINE:  '\r'? '\n' -> skip;
//COMMENT: '//' .* ('\n' | '\r') -> skip;
COMMENT: '//' .*? ('\n' | '\r') -> skip;
//MULTILINE_COMMENT:  '/*' (options {greedy=false;} : .)* '*/' -> skip;
MULTILINE_COMMENT:  '/*' ()*? '*/' -> skip;

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

SELF
	:
	'SELF'|'Self'|'self'
	;

THAT
	:
	'THAT'|'that'
	;
	
INVERSE
	:
	'INVERSE'|'inverse'
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
