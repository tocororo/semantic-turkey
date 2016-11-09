// $ANTLR 3.4 D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g 2016-11-08 15:15:27

package it.uniroma2.art.semanticturkey.syntax.manchester;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked"})
public class ManchesterSyntaxParserLexer extends Lexer {
    public static final int EOF=-1;
    public static final int T__45=45;
    public static final int T__46=46;
    public static final int T__47=47;
    public static final int T__48=48;
    public static final int T__49=49;
    public static final int T__50=50;
    public static final int T__51=51;
    public static final int AND=4;
    public static final int AST_AND=5;
    public static final int AST_BASECLASS=6;
    public static final int AST_CARDINALITY=7;
    public static final int AST_NOT=8;
    public static final int AST_ONEOFLIST=9;
    public static final int AST_ONLY=10;
    public static final int AST_OR=11;
    public static final int AST_PREFIXED_NAME=12;
    public static final int AST_SOME=13;
    public static final int AST_VALUE=14;
    public static final int COMMENT=15;
    public static final int ECHAR=16;
    public static final int EXACTLY=17;
    public static final int HEX=18;
    public static final int INTEGER=19;
    public static final int IRIREF=20;
    public static final int JAVA_LETTER=21;
    public static final int LANGTAG=22;
    public static final int MAX=23;
    public static final int MIN=24;
    public static final int MULTILINE_COMMENT=25;
    public static final int NEWLINE=26;
    public static final int NOT=27;
    public static final int ONLY=28;
    public static final int OR=29;
    public static final int PERCENT=30;
    public static final int PLX=31;
    public static final int PNAME_LN=32;
    public static final int PNAME_NS=33;
    public static final int PN_CHARS=34;
    public static final int PN_CHARS_BASE=35;
    public static final int PN_CHARS_U=36;
    public static final int PN_LOCAL=37;
    public static final int PN_LOCAL_ESC=38;
    public static final int PN_PREFIX=39;
    public static final int SOME=40;
    public static final int STRING_LITERAL1=41;
    public static final int STRING_LITERAL2=42;
    public static final int VALUE=43;
    public static final int WS=44;

    // delegates
    // delegators
    public Lexer[] getDelegates() {
        return new Lexer[] {};
    }

    public ManchesterSyntaxParserLexer() {} 
    public ManchesterSyntaxParserLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public ManchesterSyntaxParserLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);
    }
    public String getGrammarFileName() { return "D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g"; }

    // $ANTLR start "T__45"
    public final void mT__45() throws RecognitionException {
        try {
            int _type = T__45;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:11:7: ( '(' )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:11:9: '('
            {
            match('('); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__45"

    // $ANTLR start "T__46"
    public final void mT__46() throws RecognitionException {
        try {
            int _type = T__46;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:12:7: ( ')' )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:12:9: ')'
            {
            match(')'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__46"

    // $ANTLR start "T__47"
    public final void mT__47() throws RecognitionException {
        try {
            int _type = T__47;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:13:7: ( ',' )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:13:9: ','
            {
            match(','); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__47"

    // $ANTLR start "T__48"
    public final void mT__48() throws RecognitionException {
        try {
            int _type = T__48;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:14:7: ( '.' )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:14:9: '.'
            {
            match('.'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__48"

    // $ANTLR start "T__49"
    public final void mT__49() throws RecognitionException {
        try {
            int _type = T__49;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:15:7: ( '^^' )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:15:9: '^^'
            {
            match("^^"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__49"

    // $ANTLR start "T__50"
    public final void mT__50() throws RecognitionException {
        try {
            int _type = T__50;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:16:7: ( '{' )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:16:9: '{'
            {
            match('{'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__50"

    // $ANTLR start "T__51"
    public final void mT__51() throws RecognitionException {
        try {
            int _type = T__51;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:17:7: ( '}' )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:17:9: '}'
            {
            match('}'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "T__51"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:194:3: ( ( ' ' | '\\t' | '\\f' | '\\r' )+ )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:194:9: ( ' ' | '\\t' | '\\f' | '\\r' )+
            {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:194:9: ( ' ' | '\\t' | '\\f' | '\\r' )+
            int cnt1=0;
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( (LA1_0=='\t'||(LA1_0 >= '\f' && LA1_0 <= '\r')||LA1_0==' ') ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:
            	    {
            	    if ( input.LA(1)=='\t'||(input.LA(1) >= '\f' && input.LA(1) <= '\r')||input.LA(1)==' ' ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt1 >= 1 ) break loop1;
                        EarlyExitException eee =
                            new EarlyExitException(1, input);
                        throw eee;
                }
                cnt1++;
            } while (true);


            _channel=HIDDEN;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "WS"

    // $ANTLR start "NEWLINE"
    public final void mNEWLINE() throws RecognitionException {
        try {
            int _type = NEWLINE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:195:8: ( '\\n' )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:195:11: '\\n'
            {
            match('\n'); 

            _channel=HIDDEN;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "NEWLINE"

    // $ANTLR start "COMMENT"
    public final void mCOMMENT() throws RecognitionException {
        try {
            int _type = COMMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:196:8: ( '//' ( . )* ( '\\n' | '\\r' ) )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:196:10: '//' ( . )* ( '\\n' | '\\r' )
            {
            match("//"); 



            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:196:15: ( . )*
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( (LA2_0=='\n'||LA2_0=='\r') ) {
                    alt2=2;
                }
                else if ( ((LA2_0 >= '\u0000' && LA2_0 <= '\t')||(LA2_0 >= '\u000B' && LA2_0 <= '\f')||(LA2_0 >= '\u000E' && LA2_0 <= '\uFFFF')) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:196:15: .
            	    {
            	    matchAny(); 

            	    }
            	    break;

            	default :
            	    break loop2;
                }
            } while (true);


            if ( input.LA(1)=='\n'||input.LA(1)=='\r' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            _channel=HIDDEN;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "COMMENT"

    // $ANTLR start "MULTILINE_COMMENT"
    public final void mMULTILINE_COMMENT() throws RecognitionException {
        try {
            int _type = MULTILINE_COMMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:197:18: ( '/*' ( options {greedy=false; } : . )* '*/' )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:197:21: '/*' ( options {greedy=false; } : . )* '*/'
            {
            match("/*"); 



            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:197:26: ( options {greedy=false; } : . )*
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( (LA3_0=='*') ) {
                    int LA3_1 = input.LA(2);

                    if ( (LA3_1=='/') ) {
                        alt3=2;
                    }
                    else if ( ((LA3_1 >= '\u0000' && LA3_1 <= '.')||(LA3_1 >= '0' && LA3_1 <= '\uFFFF')) ) {
                        alt3=1;
                    }


                }
                else if ( ((LA3_0 >= '\u0000' && LA3_0 <= ')')||(LA3_0 >= '+' && LA3_0 <= '\uFFFF')) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:197:53: .
            	    {
            	    matchAny(); 

            	    }
            	    break;

            	default :
            	    break loop3;
                }
            } while (true);


            match("*/"); 



            _channel=HIDDEN;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "MULTILINE_COMMENT"

    // $ANTLR start "IRIREF"
    public final void mIRIREF() throws RecognitionException {
        try {
            int _type = IRIREF;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:200:3: ( '<' (~ ( '<' | '>' | '\"' | '{' | '}' | '|' | '^' | '`' | '\\u0000' .. '\\u0020' ) )* '>' )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:201:3: '<' (~ ( '<' | '>' | '\"' | '{' | '}' | '|' | '^' | '`' | '\\u0000' .. '\\u0020' ) )* '>'
            {
            match('<'); 

            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:201:7: (~ ( '<' | '>' | '\"' | '{' | '}' | '|' | '^' | '`' | '\\u0000' .. '\\u0020' ) )*
            loop4:
            do {
                int alt4=2;
                int LA4_0 = input.LA(1);

                if ( (LA4_0=='!'||(LA4_0 >= '#' && LA4_0 <= ';')||LA4_0=='='||(LA4_0 >= '?' && LA4_0 <= ']')||LA4_0=='_'||(LA4_0 >= 'a' && LA4_0 <= 'z')||(LA4_0 >= '~' && LA4_0 <= '\uFFFF')) ) {
                    alt4=1;
                }


                switch (alt4) {
            	case 1 :
            	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:
            	    {
            	    if ( input.LA(1)=='!'||(input.LA(1) >= '#' && input.LA(1) <= ';')||input.LA(1)=='='||(input.LA(1) >= '?' && input.LA(1) <= ']')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z')||(input.LA(1) >= '~' && input.LA(1) <= '\uFFFF') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    break loop4;
                }
            } while (true);


            match('>'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "IRIREF"

    // $ANTLR start "OR"
    public final void mOR() throws RecognitionException {
        try {
            int _type = OR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:206:2: ( 'OR' | 'or' )
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( (LA5_0=='O') ) {
                alt5=1;
            }
            else if ( (LA5_0=='o') ) {
                alt5=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 5, 0, input);

                throw nvae;

            }
            switch (alt5) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:207:2: 'OR'
                    {
                    match("OR"); 



                    }
                    break;
                case 2 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:207:7: 'or'
                    {
                    match("or"); 



                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "OR"

    // $ANTLR start "AND"
    public final void mAND() throws RecognitionException {
        try {
            int _type = AND;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:211:2: ( 'AND' | 'and' )
            int alt6=2;
            int LA6_0 = input.LA(1);

            if ( (LA6_0=='A') ) {
                alt6=1;
            }
            else if ( (LA6_0=='a') ) {
                alt6=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 6, 0, input);

                throw nvae;

            }
            switch (alt6) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:212:2: 'AND'
                    {
                    match("AND"); 



                    }
                    break;
                case 2 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:212:8: 'and'
                    {
                    match("and"); 



                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "AND"

    // $ANTLR start "NOT"
    public final void mNOT() throws RecognitionException {
        try {
            int _type = NOT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:216:2: ( 'NOT' | 'not' )
            int alt7=2;
            int LA7_0 = input.LA(1);

            if ( (LA7_0=='N') ) {
                alt7=1;
            }
            else if ( (LA7_0=='n') ) {
                alt7=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 7, 0, input);

                throw nvae;

            }
            switch (alt7) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:217:2: 'NOT'
                    {
                    match("NOT"); 



                    }
                    break;
                case 2 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:217:8: 'not'
                    {
                    match("not"); 



                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "NOT"

    // $ANTLR start "SOME"
    public final void mSOME() throws RecognitionException {
        try {
            int _type = SOME;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:221:2: ( 'SOME' | 'some' )
            int alt8=2;
            int LA8_0 = input.LA(1);

            if ( (LA8_0=='S') ) {
                alt8=1;
            }
            else if ( (LA8_0=='s') ) {
                alt8=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 8, 0, input);

                throw nvae;

            }
            switch (alt8) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:222:2: 'SOME'
                    {
                    match("SOME"); 



                    }
                    break;
                case 2 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:222:9: 'some'
                    {
                    match("some"); 



                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "SOME"

    // $ANTLR start "ONLY"
    public final void mONLY() throws RecognitionException {
        try {
            int _type = ONLY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:226:2: ( 'ONLY' | 'only' )
            int alt9=2;
            int LA9_0 = input.LA(1);

            if ( (LA9_0=='O') ) {
                alt9=1;
            }
            else if ( (LA9_0=='o') ) {
                alt9=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 9, 0, input);

                throw nvae;

            }
            switch (alt9) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:227:2: 'ONLY'
                    {
                    match("ONLY"); 



                    }
                    break;
                case 2 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:227:9: 'only'
                    {
                    match("only"); 



                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ONLY"

    // $ANTLR start "MIN"
    public final void mMIN() throws RecognitionException {
        try {
            int _type = MIN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:231:2: ( 'MIN' | 'min' )
            int alt10=2;
            int LA10_0 = input.LA(1);

            if ( (LA10_0=='M') ) {
                alt10=1;
            }
            else if ( (LA10_0=='m') ) {
                alt10=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 10, 0, input);

                throw nvae;

            }
            switch (alt10) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:232:2: 'MIN'
                    {
                    match("MIN"); 



                    }
                    break;
                case 2 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:232:8: 'min'
                    {
                    match("min"); 



                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "MIN"

    // $ANTLR start "MAX"
    public final void mMAX() throws RecognitionException {
        try {
            int _type = MAX;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:236:2: ( 'MAX' | 'max' )
            int alt11=2;
            int LA11_0 = input.LA(1);

            if ( (LA11_0=='M') ) {
                alt11=1;
            }
            else if ( (LA11_0=='m') ) {
                alt11=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 11, 0, input);

                throw nvae;

            }
            switch (alt11) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:237:2: 'MAX'
                    {
                    match("MAX"); 



                    }
                    break;
                case 2 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:237:8: 'max'
                    {
                    match("max"); 



                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "MAX"

    // $ANTLR start "EXACTLY"
    public final void mEXACTLY() throws RecognitionException {
        try {
            int _type = EXACTLY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:241:2: ( 'EXACTLY' | 'exactly' )
            int alt12=2;
            int LA12_0 = input.LA(1);

            if ( (LA12_0=='E') ) {
                alt12=1;
            }
            else if ( (LA12_0=='e') ) {
                alt12=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 12, 0, input);

                throw nvae;

            }
            switch (alt12) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:242:2: 'EXACTLY'
                    {
                    match("EXACTLY"); 



                    }
                    break;
                case 2 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:242:12: 'exactly'
                    {
                    match("exactly"); 



                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "EXACTLY"

    // $ANTLR start "VALUE"
    public final void mVALUE() throws RecognitionException {
        try {
            int _type = VALUE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:246:2: ( 'VALUE' | 'value' )
            int alt13=2;
            int LA13_0 = input.LA(1);

            if ( (LA13_0=='V') ) {
                alt13=1;
            }
            else if ( (LA13_0=='v') ) {
                alt13=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 13, 0, input);

                throw nvae;

            }
            switch (alt13) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:247:2: 'VALUE'
                    {
                    match("VALUE"); 



                    }
                    break;
                case 2 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:247:10: 'value'
                    {
                    match("value"); 



                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "VALUE"

    // $ANTLR start "INTEGER"
    public final void mINTEGER() throws RecognitionException {
        try {
            int _type = INTEGER;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:252:3: ( ( '0' .. '9' )+ )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:253:3: ( '0' .. '9' )+
            {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:253:3: ( '0' .. '9' )+
            int cnt14=0;
            loop14:
            do {
                int alt14=2;
                int LA14_0 = input.LA(1);

                if ( ((LA14_0 >= '0' && LA14_0 <= '9')) ) {
                    alt14=1;
                }


                switch (alt14) {
            	case 1 :
            	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:
            	    {
            	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt14 >= 1 ) break loop14;
                        EarlyExitException eee =
                            new EarlyExitException(14, input);
                        throw eee;
                }
                cnt14++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "INTEGER"

    // $ANTLR start "JAVA_LETTER"
    public final void mJAVA_LETTER() throws RecognitionException {
        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:260:3: ( 'a' .. 'z' | 'A' .. 'Z' | '_' )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:
            {
            if ( (input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z') ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "JAVA_LETTER"

    // $ANTLR start "PNAME_NS"
    public final void mPNAME_NS() throws RecognitionException {
        try {
            int _type = PNAME_NS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:266:3: ( ( PN_PREFIX )? ':' )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:267:3: ( PN_PREFIX )? ':'
            {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:267:3: ( PN_PREFIX )?
            int alt15=2;
            int LA15_0 = input.LA(1);

            if ( ((LA15_0 >= 'A' && LA15_0 <= 'Z')||(LA15_0 >= 'a' && LA15_0 <= 'z')||(LA15_0 >= '\u00C0' && LA15_0 <= '\u00D6')||(LA15_0 >= '\u00D8' && LA15_0 <= '\u00F6')||(LA15_0 >= '\u00F8' && LA15_0 <= '\u02FF')||(LA15_0 >= '\u0370' && LA15_0 <= '\u037D')||(LA15_0 >= '\u037F' && LA15_0 <= '\u1FFF')||(LA15_0 >= '\u200C' && LA15_0 <= '\u200D')||(LA15_0 >= '\u2070' && LA15_0 <= '\u218F')||(LA15_0 >= '\u2C00' && LA15_0 <= '\u2FEF')||(LA15_0 >= '\u3001' && LA15_0 <= '\uD7FF')||(LA15_0 >= '\uF900' && LA15_0 <= '\uFDCF')||(LA15_0 >= '\uFDF0' && LA15_0 <= '\uFFFD')) ) {
                alt15=1;
            }
            switch (alt15) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:267:3: PN_PREFIX
                    {
                    mPN_PREFIX(); 


                    }
                    break;

            }


            match(':'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "PNAME_NS"

    // $ANTLR start "PNAME_LN"
    public final void mPNAME_LN() throws RecognitionException {
        try {
            int _type = PNAME_LN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:271:3: ( PNAME_NS PN_LOCAL )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:272:3: PNAME_NS PN_LOCAL
            {
            mPNAME_NS(); 


            mPN_LOCAL(); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "PNAME_LN"

    // $ANTLR start "PN_PREFIX"
    public final void mPN_PREFIX() throws RecognitionException {
        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:279:3: ( PN_CHARS_BASE ( ( PN_CHARS )* PN_CHARS )? )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:280:3: PN_CHARS_BASE ( ( PN_CHARS )* PN_CHARS )?
            {
            mPN_CHARS_BASE(); 


            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:280:17: ( ( PN_CHARS )* PN_CHARS )?
            int alt17=2;
            int LA17_0 = input.LA(1);

            if ( (LA17_0=='-'||(LA17_0 >= '0' && LA17_0 <= '9')||(LA17_0 >= 'A' && LA17_0 <= 'Z')||LA17_0=='_'||(LA17_0 >= 'a' && LA17_0 <= 'z')||LA17_0=='\u00B7'||(LA17_0 >= '\u00C0' && LA17_0 <= '\u00D6')||(LA17_0 >= '\u00D8' && LA17_0 <= '\u00F6')||(LA17_0 >= '\u00F8' && LA17_0 <= '\u037D')||(LA17_0 >= '\u037F' && LA17_0 <= '\u1FFF')||(LA17_0 >= '\u200C' && LA17_0 <= '\u200D')||(LA17_0 >= '\u203F' && LA17_0 <= '\u2040')||(LA17_0 >= '\u2070' && LA17_0 <= '\u218F')||(LA17_0 >= '\u2C00' && LA17_0 <= '\u2FEF')||(LA17_0 >= '\u3001' && LA17_0 <= '\uD7FF')||(LA17_0 >= '\uF900' && LA17_0 <= '\uFDCF')||(LA17_0 >= '\uFDF0' && LA17_0 <= '\uFFFD')) ) {
                alt17=1;
            }
            switch (alt17) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:280:18: ( PN_CHARS )* PN_CHARS
                    {
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:280:18: ( PN_CHARS )*
                    loop16:
                    do {
                        int alt16=2;
                        int LA16_0 = input.LA(1);

                        if ( (LA16_0=='-'||(LA16_0 >= '0' && LA16_0 <= '9')||(LA16_0 >= 'A' && LA16_0 <= 'Z')||LA16_0=='_'||(LA16_0 >= 'a' && LA16_0 <= 'z')||LA16_0=='\u00B7'||(LA16_0 >= '\u00C0' && LA16_0 <= '\u00D6')||(LA16_0 >= '\u00D8' && LA16_0 <= '\u00F6')||(LA16_0 >= '\u00F8' && LA16_0 <= '\u037D')||(LA16_0 >= '\u037F' && LA16_0 <= '\u1FFF')||(LA16_0 >= '\u200C' && LA16_0 <= '\u200D')||(LA16_0 >= '\u203F' && LA16_0 <= '\u2040')||(LA16_0 >= '\u2070' && LA16_0 <= '\u218F')||(LA16_0 >= '\u2C00' && LA16_0 <= '\u2FEF')||(LA16_0 >= '\u3001' && LA16_0 <= '\uD7FF')||(LA16_0 >= '\uF900' && LA16_0 <= '\uFDCF')||(LA16_0 >= '\uFDF0' && LA16_0 <= '\uFFFD')) ) {
                            int LA16_1 = input.LA(2);

                            if ( (LA16_1=='-'||(LA16_1 >= '0' && LA16_1 <= '9')||(LA16_1 >= 'A' && LA16_1 <= 'Z')||LA16_1=='_'||(LA16_1 >= 'a' && LA16_1 <= 'z')||LA16_1=='\u00B7'||(LA16_1 >= '\u00C0' && LA16_1 <= '\u00D6')||(LA16_1 >= '\u00D8' && LA16_1 <= '\u00F6')||(LA16_1 >= '\u00F8' && LA16_1 <= '\u037D')||(LA16_1 >= '\u037F' && LA16_1 <= '\u1FFF')||(LA16_1 >= '\u200C' && LA16_1 <= '\u200D')||(LA16_1 >= '\u203F' && LA16_1 <= '\u2040')||(LA16_1 >= '\u2070' && LA16_1 <= '\u218F')||(LA16_1 >= '\u2C00' && LA16_1 <= '\u2FEF')||(LA16_1 >= '\u3001' && LA16_1 <= '\uD7FF')||(LA16_1 >= '\uF900' && LA16_1 <= '\uFDCF')||(LA16_1 >= '\uFDF0' && LA16_1 <= '\uFFFD')) ) {
                                alt16=1;
                            }


                        }


                        switch (alt16) {
                    	case 1 :
                    	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:
                    	    {
                    	    if ( input.LA(1)=='-'||(input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z')||input.LA(1)=='\u00B7'||(input.LA(1) >= '\u00C0' && input.LA(1) <= '\u00D6')||(input.LA(1) >= '\u00D8' && input.LA(1) <= '\u00F6')||(input.LA(1) >= '\u00F8' && input.LA(1) <= '\u037D')||(input.LA(1) >= '\u037F' && input.LA(1) <= '\u1FFF')||(input.LA(1) >= '\u200C' && input.LA(1) <= '\u200D')||(input.LA(1) >= '\u203F' && input.LA(1) <= '\u2040')||(input.LA(1) >= '\u2070' && input.LA(1) <= '\u218F')||(input.LA(1) >= '\u2C00' && input.LA(1) <= '\u2FEF')||(input.LA(1) >= '\u3001' && input.LA(1) <= '\uD7FF')||(input.LA(1) >= '\uF900' && input.LA(1) <= '\uFDCF')||(input.LA(1) >= '\uFDF0' && input.LA(1) <= '\uFFFD') ) {
                    	        input.consume();
                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop16;
                        }
                    } while (true);


                    mPN_CHARS(); 


                    }
                    break;

            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "PN_PREFIX"

    // $ANTLR start "PN_CHARS"
    public final void mPN_CHARS() throws RecognitionException {
        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:288:3: ( PN_CHARS_U | '-' | '0' .. '9' | '\\u00B7' | '\\u0300' .. '\\u036F' | '\\u203F' .. '\\u2040' )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:
            {
            if ( input.LA(1)=='-'||(input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z')||input.LA(1)=='\u00B7'||(input.LA(1) >= '\u00C0' && input.LA(1) <= '\u00D6')||(input.LA(1) >= '\u00D8' && input.LA(1) <= '\u00F6')||(input.LA(1) >= '\u00F8' && input.LA(1) <= '\u037D')||(input.LA(1) >= '\u037F' && input.LA(1) <= '\u1FFF')||(input.LA(1) >= '\u200C' && input.LA(1) <= '\u200D')||(input.LA(1) >= '\u203F' && input.LA(1) <= '\u2040')||(input.LA(1) >= '\u2070' && input.LA(1) <= '\u218F')||(input.LA(1) >= '\u2C00' && input.LA(1) <= '\u2FEF')||(input.LA(1) >= '\u3001' && input.LA(1) <= '\uD7FF')||(input.LA(1) >= '\uF900' && input.LA(1) <= '\uFDCF')||(input.LA(1) >= '\uFDF0' && input.LA(1) <= '\uFFFD') ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "PN_CHARS"

    // $ANTLR start "PN_LOCAL"
    public final void mPN_LOCAL() throws RecognitionException {
        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:296:3: ( ( PN_CHARS_U | ':' | '0' .. '9' | PLX ) ( ( PN_CHARS | '.' | ':' | PLX )* ( PN_CHARS | ':' | PLX ) )? )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:297:3: ( PN_CHARS_U | ':' | '0' .. '9' | PLX ) ( ( PN_CHARS | '.' | ':' | PLX )* ( PN_CHARS | ':' | PLX ) )?
            {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:297:3: ( PN_CHARS_U | ':' | '0' .. '9' | PLX )
            int alt18=4;
            int LA18_0 = input.LA(1);

            if ( ((LA18_0 >= 'A' && LA18_0 <= 'Z')||LA18_0=='_'||(LA18_0 >= 'a' && LA18_0 <= 'z')||(LA18_0 >= '\u00C0' && LA18_0 <= '\u00D6')||(LA18_0 >= '\u00D8' && LA18_0 <= '\u00F6')||(LA18_0 >= '\u00F8' && LA18_0 <= '\u02FF')||(LA18_0 >= '\u0370' && LA18_0 <= '\u037D')||(LA18_0 >= '\u037F' && LA18_0 <= '\u1FFF')||(LA18_0 >= '\u200C' && LA18_0 <= '\u200D')||(LA18_0 >= '\u2070' && LA18_0 <= '\u218F')||(LA18_0 >= '\u2C00' && LA18_0 <= '\u2FEF')||(LA18_0 >= '\u3001' && LA18_0 <= '\uD7FF')||(LA18_0 >= '\uF900' && LA18_0 <= '\uFDCF')||(LA18_0 >= '\uFDF0' && LA18_0 <= '\uFFFD')) ) {
                alt18=1;
            }
            else if ( (LA18_0==':') ) {
                alt18=2;
            }
            else if ( ((LA18_0 >= '0' && LA18_0 <= '9')) ) {
                alt18=3;
            }
            else if ( (LA18_0=='%'||LA18_0=='\\') ) {
                alt18=4;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 18, 0, input);

                throw nvae;

            }
            switch (alt18) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:297:4: PN_CHARS_U
                    {
                    mPN_CHARS_U(); 


                    }
                    break;
                case 2 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:297:17: ':'
                    {
                    match(':'); 

                    }
                    break;
                case 3 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:297:23: '0' .. '9'
                    {
                    matchRange('0','9'); 

                    }
                    break;
                case 4 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:297:34: PLX
                    {
                    mPLX(); 


                    }
                    break;

            }


            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:297:40: ( ( PN_CHARS | '.' | ':' | PLX )* ( PN_CHARS | ':' | PLX ) )?
            int alt21=2;
            int LA21_0 = input.LA(1);

            if ( (LA21_0=='%'||(LA21_0 >= '-' && LA21_0 <= '.')||(LA21_0 >= '0' && LA21_0 <= ':')||(LA21_0 >= 'A' && LA21_0 <= 'Z')||LA21_0=='\\'||LA21_0=='_'||(LA21_0 >= 'a' && LA21_0 <= 'z')||LA21_0=='\u00B7'||(LA21_0 >= '\u00C0' && LA21_0 <= '\u00D6')||(LA21_0 >= '\u00D8' && LA21_0 <= '\u00F6')||(LA21_0 >= '\u00F8' && LA21_0 <= '\u037D')||(LA21_0 >= '\u037F' && LA21_0 <= '\u1FFF')||(LA21_0 >= '\u200C' && LA21_0 <= '\u200D')||(LA21_0 >= '\u203F' && LA21_0 <= '\u2040')||(LA21_0 >= '\u2070' && LA21_0 <= '\u218F')||(LA21_0 >= '\u2C00' && LA21_0 <= '\u2FEF')||(LA21_0 >= '\u3001' && LA21_0 <= '\uD7FF')||(LA21_0 >= '\uF900' && LA21_0 <= '\uFDCF')||(LA21_0 >= '\uFDF0' && LA21_0 <= '\uFFFD')) ) {
                alt21=1;
            }
            switch (alt21) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:297:41: ( PN_CHARS | '.' | ':' | PLX )* ( PN_CHARS | ':' | PLX )
                    {
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:297:41: ( PN_CHARS | '.' | ':' | PLX )*
                    loop19:
                    do {
                        int alt19=5;
                        int LA19_0 = input.LA(1);

                        if ( (LA19_0=='-'||(LA19_0 >= '0' && LA19_0 <= '9')||(LA19_0 >= 'A' && LA19_0 <= 'Z')||LA19_0=='_'||(LA19_0 >= 'a' && LA19_0 <= 'z')||LA19_0=='\u00B7'||(LA19_0 >= '\u00C0' && LA19_0 <= '\u00D6')||(LA19_0 >= '\u00D8' && LA19_0 <= '\u00F6')||(LA19_0 >= '\u00F8' && LA19_0 <= '\u037D')||(LA19_0 >= '\u037F' && LA19_0 <= '\u1FFF')||(LA19_0 >= '\u200C' && LA19_0 <= '\u200D')||(LA19_0 >= '\u203F' && LA19_0 <= '\u2040')||(LA19_0 >= '\u2070' && LA19_0 <= '\u218F')||(LA19_0 >= '\u2C00' && LA19_0 <= '\u2FEF')||(LA19_0 >= '\u3001' && LA19_0 <= '\uD7FF')||(LA19_0 >= '\uF900' && LA19_0 <= '\uFDCF')||(LA19_0 >= '\uFDF0' && LA19_0 <= '\uFFFD')) ) {
                            int LA19_1 = input.LA(2);

                            if ( (LA19_1=='%'||(LA19_1 >= '-' && LA19_1 <= '.')||(LA19_1 >= '0' && LA19_1 <= ':')||(LA19_1 >= 'A' && LA19_1 <= 'Z')||LA19_1=='\\'||LA19_1=='_'||(LA19_1 >= 'a' && LA19_1 <= 'z')||LA19_1=='\u00B7'||(LA19_1 >= '\u00C0' && LA19_1 <= '\u00D6')||(LA19_1 >= '\u00D8' && LA19_1 <= '\u00F6')||(LA19_1 >= '\u00F8' && LA19_1 <= '\u037D')||(LA19_1 >= '\u037F' && LA19_1 <= '\u1FFF')||(LA19_1 >= '\u200C' && LA19_1 <= '\u200D')||(LA19_1 >= '\u203F' && LA19_1 <= '\u2040')||(LA19_1 >= '\u2070' && LA19_1 <= '\u218F')||(LA19_1 >= '\u2C00' && LA19_1 <= '\u2FEF')||(LA19_1 >= '\u3001' && LA19_1 <= '\uD7FF')||(LA19_1 >= '\uF900' && LA19_1 <= '\uFDCF')||(LA19_1 >= '\uFDF0' && LA19_1 <= '\uFFFD')) ) {
                                alt19=1;
                            }


                        }
                        else if ( (LA19_0==':') ) {
                            int LA19_2 = input.LA(2);

                            if ( (LA19_2=='%'||(LA19_2 >= '-' && LA19_2 <= '.')||(LA19_2 >= '0' && LA19_2 <= ':')||(LA19_2 >= 'A' && LA19_2 <= 'Z')||LA19_2=='\\'||LA19_2=='_'||(LA19_2 >= 'a' && LA19_2 <= 'z')||LA19_2=='\u00B7'||(LA19_2 >= '\u00C0' && LA19_2 <= '\u00D6')||(LA19_2 >= '\u00D8' && LA19_2 <= '\u00F6')||(LA19_2 >= '\u00F8' && LA19_2 <= '\u037D')||(LA19_2 >= '\u037F' && LA19_2 <= '\u1FFF')||(LA19_2 >= '\u200C' && LA19_2 <= '\u200D')||(LA19_2 >= '\u203F' && LA19_2 <= '\u2040')||(LA19_2 >= '\u2070' && LA19_2 <= '\u218F')||(LA19_2 >= '\u2C00' && LA19_2 <= '\u2FEF')||(LA19_2 >= '\u3001' && LA19_2 <= '\uD7FF')||(LA19_2 >= '\uF900' && LA19_2 <= '\uFDCF')||(LA19_2 >= '\uFDF0' && LA19_2 <= '\uFFFD')) ) {
                                alt19=3;
                            }


                        }
                        else if ( (LA19_0=='%') ) {
                            int LA19_3 = input.LA(2);

                            if ( ((LA19_3 >= '0' && LA19_3 <= '9')||(LA19_3 >= 'A' && LA19_3 <= 'F')||(LA19_3 >= 'a' && LA19_3 <= 'f')) ) {
                                int LA19_9 = input.LA(3);

                                if ( ((LA19_9 >= '0' && LA19_9 <= '9')||(LA19_9 >= 'A' && LA19_9 <= 'F')||(LA19_9 >= 'a' && LA19_9 <= 'f')) ) {
                                    int LA19_11 = input.LA(4);

                                    if ( (LA19_11=='%'||(LA19_11 >= '-' && LA19_11 <= '.')||(LA19_11 >= '0' && LA19_11 <= ':')||(LA19_11 >= 'A' && LA19_11 <= 'Z')||LA19_11=='\\'||LA19_11=='_'||(LA19_11 >= 'a' && LA19_11 <= 'z')||LA19_11=='\u00B7'||(LA19_11 >= '\u00C0' && LA19_11 <= '\u00D6')||(LA19_11 >= '\u00D8' && LA19_11 <= '\u00F6')||(LA19_11 >= '\u00F8' && LA19_11 <= '\u037D')||(LA19_11 >= '\u037F' && LA19_11 <= '\u1FFF')||(LA19_11 >= '\u200C' && LA19_11 <= '\u200D')||(LA19_11 >= '\u203F' && LA19_11 <= '\u2040')||(LA19_11 >= '\u2070' && LA19_11 <= '\u218F')||(LA19_11 >= '\u2C00' && LA19_11 <= '\u2FEF')||(LA19_11 >= '\u3001' && LA19_11 <= '\uD7FF')||(LA19_11 >= '\uF900' && LA19_11 <= '\uFDCF')||(LA19_11 >= '\uFDF0' && LA19_11 <= '\uFFFD')) ) {
                                        alt19=4;
                                    }


                                }


                            }


                        }
                        else if ( (LA19_0=='\\') ) {
                            int LA19_4 = input.LA(2);

                            if ( (LA19_4=='!'||(LA19_4 >= '#' && LA19_4 <= '/')||LA19_4==';'||LA19_4=='='||(LA19_4 >= '?' && LA19_4 <= '@')||LA19_4=='_'||LA19_4=='~') ) {
                                int LA19_10 = input.LA(3);

                                if ( (LA19_10=='%'||(LA19_10 >= '-' && LA19_10 <= '.')||(LA19_10 >= '0' && LA19_10 <= ':')||(LA19_10 >= 'A' && LA19_10 <= 'Z')||LA19_10=='\\'||LA19_10=='_'||(LA19_10 >= 'a' && LA19_10 <= 'z')||LA19_10=='\u00B7'||(LA19_10 >= '\u00C0' && LA19_10 <= '\u00D6')||(LA19_10 >= '\u00D8' && LA19_10 <= '\u00F6')||(LA19_10 >= '\u00F8' && LA19_10 <= '\u037D')||(LA19_10 >= '\u037F' && LA19_10 <= '\u1FFF')||(LA19_10 >= '\u200C' && LA19_10 <= '\u200D')||(LA19_10 >= '\u203F' && LA19_10 <= '\u2040')||(LA19_10 >= '\u2070' && LA19_10 <= '\u218F')||(LA19_10 >= '\u2C00' && LA19_10 <= '\u2FEF')||(LA19_10 >= '\u3001' && LA19_10 <= '\uD7FF')||(LA19_10 >= '\uF900' && LA19_10 <= '\uFDCF')||(LA19_10 >= '\uFDF0' && LA19_10 <= '\uFFFD')) ) {
                                    alt19=4;
                                }


                            }


                        }
                        else if ( (LA19_0=='.') ) {
                            alt19=2;
                        }


                        switch (alt19) {
                    	case 1 :
                    	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:297:42: PN_CHARS
                    	    {
                    	    mPN_CHARS(); 


                    	    }
                    	    break;
                    	case 2 :
                    	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:297:53: '.'
                    	    {
                    	    match('.'); 

                    	    }
                    	    break;
                    	case 3 :
                    	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:297:59: ':'
                    	    {
                    	    match(':'); 

                    	    }
                    	    break;
                    	case 4 :
                    	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:297:65: PLX
                    	    {
                    	    mPLX(); 


                    	    }
                    	    break;

                    	default :
                    	    break loop19;
                        }
                    } while (true);


                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:297:71: ( PN_CHARS | ':' | PLX )
                    int alt20=3;
                    int LA20_0 = input.LA(1);

                    if ( (LA20_0=='-'||(LA20_0 >= '0' && LA20_0 <= '9')||(LA20_0 >= 'A' && LA20_0 <= 'Z')||LA20_0=='_'||(LA20_0 >= 'a' && LA20_0 <= 'z')||LA20_0=='\u00B7'||(LA20_0 >= '\u00C0' && LA20_0 <= '\u00D6')||(LA20_0 >= '\u00D8' && LA20_0 <= '\u00F6')||(LA20_0 >= '\u00F8' && LA20_0 <= '\u037D')||(LA20_0 >= '\u037F' && LA20_0 <= '\u1FFF')||(LA20_0 >= '\u200C' && LA20_0 <= '\u200D')||(LA20_0 >= '\u203F' && LA20_0 <= '\u2040')||(LA20_0 >= '\u2070' && LA20_0 <= '\u218F')||(LA20_0 >= '\u2C00' && LA20_0 <= '\u2FEF')||(LA20_0 >= '\u3001' && LA20_0 <= '\uD7FF')||(LA20_0 >= '\uF900' && LA20_0 <= '\uFDCF')||(LA20_0 >= '\uFDF0' && LA20_0 <= '\uFFFD')) ) {
                        alt20=1;
                    }
                    else if ( (LA20_0==':') ) {
                        alt20=2;
                    }
                    else if ( (LA20_0=='%'||LA20_0=='\\') ) {
                        alt20=3;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 20, 0, input);

                        throw nvae;

                    }
                    switch (alt20) {
                        case 1 :
                            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:297:72: PN_CHARS
                            {
                            mPN_CHARS(); 


                            }
                            break;
                        case 2 :
                            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:297:83: ':'
                            {
                            match(':'); 

                            }
                            break;
                        case 3 :
                            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:297:89: PLX
                            {
                            mPLX(); 


                            }
                            break;

                    }


                    }
                    break;

            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "PN_LOCAL"

    // $ANTLR start "PN_CHARS_U"
    public final void mPN_CHARS_U() throws RecognitionException {
        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:302:3: ( PN_CHARS_BASE | '_' )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:
            {
            if ( (input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z')||(input.LA(1) >= '\u00C0' && input.LA(1) <= '\u00D6')||(input.LA(1) >= '\u00D8' && input.LA(1) <= '\u00F6')||(input.LA(1) >= '\u00F8' && input.LA(1) <= '\u02FF')||(input.LA(1) >= '\u0370' && input.LA(1) <= '\u037D')||(input.LA(1) >= '\u037F' && input.LA(1) <= '\u1FFF')||(input.LA(1) >= '\u200C' && input.LA(1) <= '\u200D')||(input.LA(1) >= '\u2070' && input.LA(1) <= '\u218F')||(input.LA(1) >= '\u2C00' && input.LA(1) <= '\u2FEF')||(input.LA(1) >= '\u3001' && input.LA(1) <= '\uD7FF')||(input.LA(1) >= '\uF900' && input.LA(1) <= '\uFDCF')||(input.LA(1) >= '\uFDF0' && input.LA(1) <= '\uFFFD') ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "PN_CHARS_U"

    // $ANTLR start "PN_CHARS_BASE"
    public final void mPN_CHARS_BASE() throws RecognitionException {
        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:309:3: ( 'A' .. 'Z' | 'a' .. 'z' | '\\u00C0' .. '\\u00D6' | '\\u00D8' .. '\\u00F6' | '\\u00F8' .. '\\u02FF' | '\\u0370' .. '\\u037D' | '\\u037F' .. '\\u1FFF' | '\\u200C' .. '\\u200D' | '\\u2070' .. '\\u218F' | '\\u2C00' .. '\\u2FEF' | '\\u3001' .. '\\uD7FF' | '\\uF900' .. '\\uFDCF' | '\\uFDF0' .. '\\uFFFD' )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:
            {
            if ( (input.LA(1) >= 'A' && input.LA(1) <= 'Z')||(input.LA(1) >= 'a' && input.LA(1) <= 'z')||(input.LA(1) >= '\u00C0' && input.LA(1) <= '\u00D6')||(input.LA(1) >= '\u00D8' && input.LA(1) <= '\u00F6')||(input.LA(1) >= '\u00F8' && input.LA(1) <= '\u02FF')||(input.LA(1) >= '\u0370' && input.LA(1) <= '\u037D')||(input.LA(1) >= '\u037F' && input.LA(1) <= '\u1FFF')||(input.LA(1) >= '\u200C' && input.LA(1) <= '\u200D')||(input.LA(1) >= '\u2070' && input.LA(1) <= '\u218F')||(input.LA(1) >= '\u2C00' && input.LA(1) <= '\u2FEF')||(input.LA(1) >= '\u3001' && input.LA(1) <= '\uD7FF')||(input.LA(1) >= '\uF900' && input.LA(1) <= '\uFDCF')||(input.LA(1) >= '\uFDF0' && input.LA(1) <= '\uFFFD') ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "PN_CHARS_BASE"

    // $ANTLR start "PLX"
    public final void mPLX() throws RecognitionException {
        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:318:3: ( PERCENT | PN_LOCAL_ESC )
            int alt22=2;
            int LA22_0 = input.LA(1);

            if ( (LA22_0=='%') ) {
                alt22=1;
            }
            else if ( (LA22_0=='\\') ) {
                alt22=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 22, 0, input);

                throw nvae;

            }
            switch (alt22) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:319:3: PERCENT
                    {
                    mPERCENT(); 


                    }
                    break;
                case 2 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:319:13: PN_LOCAL_ESC
                    {
                    mPN_LOCAL_ESC(); 


                    }
                    break;

            }

        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "PLX"

    // $ANTLR start "PERCENT"
    public final void mPERCENT() throws RecognitionException {
        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:325:3: ( '%' HEX HEX )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:326:3: '%' HEX HEX
            {
            match('%'); 

            mHEX(); 


            mHEX(); 


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "PERCENT"

    // $ANTLR start "HEX"
    public final void mHEX() throws RecognitionException {
        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:331:3: ( '0' .. '9' | 'A' .. 'F' | 'a' .. 'f' )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:
            {
            if ( (input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'F')||(input.LA(1) >= 'a' && input.LA(1) <= 'f') ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "HEX"

    // $ANTLR start "PN_LOCAL_ESC"
    public final void mPN_LOCAL_ESC() throws RecognitionException {
        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:337:3: ( '\\\\' ( '_' | '~' | '.' | '-' | '!' | '$' | '&' | '\\'' | '(' | ')' | '*' | '+' | ',' | ';' | '=' | '/' | '?' | '#' | '@' | '%' ) )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:338:3: '\\\\' ( '_' | '~' | '.' | '-' | '!' | '$' | '&' | '\\'' | '(' | ')' | '*' | '+' | ',' | ';' | '=' | '/' | '?' | '#' | '@' | '%' )
            {
            match('\\'); 

            if ( input.LA(1)=='!'||(input.LA(1) >= '#' && input.LA(1) <= '/')||input.LA(1)==';'||input.LA(1)=='='||(input.LA(1) >= '?' && input.LA(1) <= '@')||input.LA(1)=='_'||input.LA(1)=='~' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "PN_LOCAL_ESC"

    // $ANTLR start "LANGTAG"
    public final void mLANGTAG() throws RecognitionException {
        try {
            int _type = LANGTAG;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:342:2: ( '@' ( 'a' .. 'z' | 'A' .. 'Z' )+ ( '-' ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' )+ )* )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:343:2: '@' ( 'a' .. 'z' | 'A' .. 'Z' )+ ( '-' ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' )+ )*
            {
            match('@'); 

            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:343:6: ( 'a' .. 'z' | 'A' .. 'Z' )+
            int cnt23=0;
            loop23:
            do {
                int alt23=2;
                int LA23_0 = input.LA(1);

                if ( ((LA23_0 >= 'A' && LA23_0 <= 'Z')||(LA23_0 >= 'a' && LA23_0 <= 'z')) ) {
                    alt23=1;
                }


                switch (alt23) {
            	case 1 :
            	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:
            	    {
            	    if ( (input.LA(1) >= 'A' && input.LA(1) <= 'Z')||(input.LA(1) >= 'a' && input.LA(1) <= 'z') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt23 >= 1 ) break loop23;
                        EarlyExitException eee =
                            new EarlyExitException(23, input);
                        throw eee;
                }
                cnt23++;
            } while (true);


            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:343:27: ( '-' ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' )+ )*
            loop25:
            do {
                int alt25=2;
                int LA25_0 = input.LA(1);

                if ( (LA25_0=='-') ) {
                    alt25=1;
                }


                switch (alt25) {
            	case 1 :
            	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:343:28: '-' ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' )+
            	    {
            	    match('-'); 

            	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:343:32: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' )+
            	    int cnt24=0;
            	    loop24:
            	    do {
            	        int alt24=2;
            	        int LA24_0 = input.LA(1);

            	        if ( ((LA24_0 >= '0' && LA24_0 <= '9')||(LA24_0 >= 'A' && LA24_0 <= 'Z')||(LA24_0 >= 'a' && LA24_0 <= 'z')) ) {
            	            alt24=1;
            	        }


            	        switch (alt24) {
            	    	case 1 :
            	    	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:
            	    	    {
            	    	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'Z')||(input.LA(1) >= 'a' && input.LA(1) <= 'z') ) {
            	    	        input.consume();
            	    	    }
            	    	    else {
            	    	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	    	        recover(mse);
            	    	        throw mse;
            	    	    }


            	    	    }
            	    	    break;

            	    	default :
            	    	    if ( cnt24 >= 1 ) break loop24;
            	                EarlyExitException eee =
            	                    new EarlyExitException(24, input);
            	                throw eee;
            	        }
            	        cnt24++;
            	    } while (true);


            	    }
            	    break;

            	default :
            	    break loop25;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LANGTAG"

    // $ANTLR start "STRING_LITERAL1"
    public final void mSTRING_LITERAL1() throws RecognitionException {
        try {
            int _type = STRING_LITERAL1;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:347:2: ( '\\'' ( (~ ( '\\u0027' | '\\u005C' | '\\u000A' | '\\u000D' ) ) | ECHAR )* '\\'' )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:348:2: '\\'' ( (~ ( '\\u0027' | '\\u005C' | '\\u000A' | '\\u000D' ) ) | ECHAR )* '\\''
            {
            match('\''); 

            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:348:7: ( (~ ( '\\u0027' | '\\u005C' | '\\u000A' | '\\u000D' ) ) | ECHAR )*
            loop26:
            do {
                int alt26=3;
                int LA26_0 = input.LA(1);

                if ( ((LA26_0 >= '\u0000' && LA26_0 <= '\t')||(LA26_0 >= '\u000B' && LA26_0 <= '\f')||(LA26_0 >= '\u000E' && LA26_0 <= '&')||(LA26_0 >= '(' && LA26_0 <= '[')||(LA26_0 >= ']' && LA26_0 <= '\uFFFF')) ) {
                    alt26=1;
                }
                else if ( (LA26_0=='\\') ) {
                    alt26=2;
                }


                switch (alt26) {
            	case 1 :
            	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:348:9: (~ ( '\\u0027' | '\\u005C' | '\\u000A' | '\\u000D' ) )
            	    {
            	    if ( (input.LA(1) >= '\u0000' && input.LA(1) <= '\t')||(input.LA(1) >= '\u000B' && input.LA(1) <= '\f')||(input.LA(1) >= '\u000E' && input.LA(1) <= '&')||(input.LA(1) >= '(' && input.LA(1) <= '[')||(input.LA(1) >= ']' && input.LA(1) <= '\uFFFF') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;
            	case 2 :
            	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:348:52: ECHAR
            	    {
            	    mECHAR(); 


            	    }
            	    break;

            	default :
            	    break loop26;
                }
            } while (true);


            match('\''); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "STRING_LITERAL1"

    // $ANTLR start "STRING_LITERAL2"
    public final void mSTRING_LITERAL2() throws RecognitionException {
        try {
            int _type = STRING_LITERAL2;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:353:2: ( '\"' ( (~ ( '\\u0022' | '\\u005C' | '\\u000A' | '\\u000D' ) ) | ECHAR )* '\"' )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:354:2: '\"' ( (~ ( '\\u0022' | '\\u005C' | '\\u000A' | '\\u000D' ) ) | ECHAR )* '\"'
            {
            match('\"'); 

            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:354:6: ( (~ ( '\\u0022' | '\\u005C' | '\\u000A' | '\\u000D' ) ) | ECHAR )*
            loop27:
            do {
                int alt27=3;
                int LA27_0 = input.LA(1);

                if ( ((LA27_0 >= '\u0000' && LA27_0 <= '\t')||(LA27_0 >= '\u000B' && LA27_0 <= '\f')||(LA27_0 >= '\u000E' && LA27_0 <= '!')||(LA27_0 >= '#' && LA27_0 <= '[')||(LA27_0 >= ']' && LA27_0 <= '\uFFFF')) ) {
                    alt27=1;
                }
                else if ( (LA27_0=='\\') ) {
                    alt27=2;
                }


                switch (alt27) {
            	case 1 :
            	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:354:8: (~ ( '\\u0022' | '\\u005C' | '\\u000A' | '\\u000D' ) )
            	    {
            	    if ( (input.LA(1) >= '\u0000' && input.LA(1) <= '\t')||(input.LA(1) >= '\u000B' && input.LA(1) <= '\f')||(input.LA(1) >= '\u000E' && input.LA(1) <= '!')||(input.LA(1) >= '#' && input.LA(1) <= '[')||(input.LA(1) >= ']' && input.LA(1) <= '\uFFFF') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;
            	case 2 :
            	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:354:51: ECHAR
            	    {
            	    mECHAR(); 


            	    }
            	    break;

            	default :
            	    break loop27;
                }
            } while (true);


            match('\"'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "STRING_LITERAL2"

    // $ANTLR start "ECHAR"
    public final void mECHAR() throws RecognitionException {
        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:361:2: ( '\\\\' ( 't' | 'b' | 'n' | 'r' | 'f' | '\\\\' | '\\'' | '\"' ) )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:362:2: '\\\\' ( 't' | 'b' | 'n' | 'r' | 'f' | '\\\\' | '\\'' | '\"' )
            {
            match('\\'); 

            if ( input.LA(1)=='\"'||input.LA(1)=='\''||input.LA(1)=='\\'||input.LA(1)=='b'||input.LA(1)=='f'||input.LA(1)=='n'||input.LA(1)=='r'||input.LA(1)=='t' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ECHAR"

    public void mTokens() throws RecognitionException {
        // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:8: ( T__45 | T__46 | T__47 | T__48 | T__49 | T__50 | T__51 | WS | NEWLINE | COMMENT | MULTILINE_COMMENT | IRIREF | OR | AND | NOT | SOME | ONLY | MIN | MAX | EXACTLY | VALUE | INTEGER | PNAME_NS | PNAME_LN | LANGTAG | STRING_LITERAL1 | STRING_LITERAL2 )
        int alt28=27;
        alt28 = dfa28.predict(input);
        switch (alt28) {
            case 1 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:10: T__45
                {
                mT__45(); 


                }
                break;
            case 2 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:16: T__46
                {
                mT__46(); 


                }
                break;
            case 3 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:22: T__47
                {
                mT__47(); 


                }
                break;
            case 4 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:28: T__48
                {
                mT__48(); 


                }
                break;
            case 5 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:34: T__49
                {
                mT__49(); 


                }
                break;
            case 6 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:40: T__50
                {
                mT__50(); 


                }
                break;
            case 7 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:46: T__51
                {
                mT__51(); 


                }
                break;
            case 8 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:52: WS
                {
                mWS(); 


                }
                break;
            case 9 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:55: NEWLINE
                {
                mNEWLINE(); 


                }
                break;
            case 10 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:63: COMMENT
                {
                mCOMMENT(); 


                }
                break;
            case 11 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:71: MULTILINE_COMMENT
                {
                mMULTILINE_COMMENT(); 


                }
                break;
            case 12 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:89: IRIREF
                {
                mIRIREF(); 


                }
                break;
            case 13 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:96: OR
                {
                mOR(); 


                }
                break;
            case 14 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:99: AND
                {
                mAND(); 


                }
                break;
            case 15 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:103: NOT
                {
                mNOT(); 


                }
                break;
            case 16 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:107: SOME
                {
                mSOME(); 


                }
                break;
            case 17 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:112: ONLY
                {
                mONLY(); 


                }
                break;
            case 18 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:117: MIN
                {
                mMIN(); 


                }
                break;
            case 19 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:121: MAX
                {
                mMAX(); 


                }
                break;
            case 20 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:125: EXACTLY
                {
                mEXACTLY(); 


                }
                break;
            case 21 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:133: VALUE
                {
                mVALUE(); 


                }
                break;
            case 22 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:139: INTEGER
                {
                mINTEGER(); 


                }
                break;
            case 23 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:147: PNAME_NS
                {
                mPNAME_NS(); 


                }
                break;
            case 24 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:156: PNAME_LN
                {
                mPNAME_LN(); 


                }
                break;
            case 25 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:165: LANGTAG
                {
                mLANGTAG(); 


                }
                break;
            case 26 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:173: STRING_LITERAL1
                {
                mSTRING_LITERAL1(); 


                }
                break;
            case 27 :
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:1:189: STRING_LITERAL2
                {
                mSTRING_LITERAL2(); 


                }
                break;

        }

    }


    protected DFA28 dfa28 = new DFA28(this);
    static final String DFA28_eotS =
        "\34\uffff\1\65\5\uffff\1\67\2\uffff\1\67\24\uffff\2\112\2\113\2"+
        "\uffff\1\116\1\117\1\116\1\117\4\uffff\2\124\2\uffff\2\125\12\uffff"+
        "\2\134\3\uffff\2\137\1\uffff";
    static final String DFA28_eofS =
        "\140\uffff";
    static final String DFA28_minS =
        "\1\11\11\uffff\1\52\1\uffff\16\55\1\uffff\1\55\1\45\5\uffff\23\55"+
        "\3\uffff\22\55\2\uffff\2\55\2\uffff\4\55\2\uffff\6\55\1\uffff\2"+
        "\55\1\uffff";
    static final String DFA28_maxS =
        "\1\ufffd\11\uffff\1\57\1\uffff\16\ufffd\1\uffff\2\ufffd\5\uffff"+
        "\23\ufffd\3\uffff\22\ufffd\2\uffff\2\ufffd\2\uffff\4\ufffd\2\uffff"+
        "\6\ufffd\1\uffff\2\ufffd\1\uffff";
    static final String DFA28_acceptS =
        "\1\uffff\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\1\uffff\1\14\16\uffff"+
        "\1\26\2\uffff\1\31\1\32\1\33\1\12\1\13\23\uffff\1\27\1\30\1\15\22"+
        "\uffff\1\16\1\17\2\uffff\1\22\1\23\4\uffff\1\21\1\20\6\uffff\1\25"+
        "\2\uffff\1\24";
    static final String DFA28_specialS =
        "\140\uffff}>";
    static final String[] DFA28_transitionS = {
            "\1\10\1\11\1\uffff\2\10\22\uffff\1\10\1\uffff\1\37\4\uffff\1"+
            "\36\1\1\1\2\2\uffff\1\3\1\uffff\1\4\1\12\12\32\1\34\1\uffff"+
            "\1\13\3\uffff\1\35\1\16\3\33\1\26\7\33\1\24\1\20\1\14\3\33\1"+
            "\22\2\33\1\30\4\33\3\uffff\1\5\2\uffff\1\17\3\33\1\27\7\33\1"+
            "\25\1\21\1\15\3\33\1\23\2\33\1\31\4\33\1\6\1\uffff\1\7\102\uffff"+
            "\27\33\1\uffff\37\33\1\uffff\u0208\33\160\uffff\16\33\1\uffff"+
            "\u1c81\33\14\uffff\2\33\142\uffff\u0120\33\u0a70\uffff\u03f0"+
            "\33\21\uffff\ua7ff\33\u2100\uffff\u04d0\33\40\uffff\u020e\33",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\41\4\uffff\1\40",
            "",
            "\1\44\2\uffff\12\44\1\34\6\uffff\15\44\1\43\3\44\1\42\10\44"+
            "\4\uffff\1\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff"+
            "\37\44\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff"+
            "\2\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff"+
            "\44\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\15\44\1\46\3\44\1\45\10\44\74\uffff\1\44\10\uffff\27\44\1\uffff"+
            "\37\44\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff"+
            "\2\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff"+
            "\44\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\15\44\1\47\14\44\4\uffff\1"+
            "\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\15\44\1\50\14\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\16\44\1\51\13\44\4\uffff\1"+
            "\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\16\44\1\52\13\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\16\44\1\53\13\44\4\uffff\1"+
            "\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\16\44\1\54\13\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\1\56\7\44\1\55\21\44\4\uffff"+
            "\1\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37"+
            "\44\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff"+
            "\2\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff"+
            "\44\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\1\60\7\44\1\57\21\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37"+
            "\44\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff"+
            "\2\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff"+
            "\44\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\27\44\1\61\2\44\4\uffff\1"+
            "\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\27\44\1\62\2\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\1\63\31\44\4\uffff\1\44\1"+
            "\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff"+
            "\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff"+
            "\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff"+
            "\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\1\64\31\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff"+
            "\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff"+
            "\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff"+
            "\u04d0\44\40\uffff\u020e\44",
            "",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            "\1\66\12\uffff\13\66\6\uffff\32\66\1\uffff\1\66\2\uffff\1\66"+
            "\1\uffff\32\66\105\uffff\27\66\1\uffff\37\66\1\uffff\u0208\66"+
            "\160\uffff\16\66\1\uffff\u1c81\66\14\uffff\2\66\142\uffff\u0120"+
            "\66\u0a70\uffff\u03f0\66\21\uffff\ua7ff\66\u2100\uffff\u04d0"+
            "\66\40\uffff\u020e\66",
            "",
            "",
            "",
            "",
            "",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\13\44\1\70\16\44\4\uffff\1"+
            "\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\13\44\1\71\16\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\3\44\1\72\26\44\4\uffff\1"+
            "\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\3\44\1\73\26\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\23\44\1\74\6\44\4\uffff\1"+
            "\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\23\44\1\75\6\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\14\44\1\76\15\44\4\uffff\1"+
            "\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\14\44\1\77\15\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\15\44\1\100\14\44\4\uffff"+
            "\1\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37"+
            "\44\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff"+
            "\2\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff"+
            "\44\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\27\44\1\101\2\44\4\uffff\1"+
            "\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\15\44\1\102\14\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\27\44\1\103\2\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\1\104\31\44\4\uffff\1\44\1"+
            "\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff"+
            "\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff"+
            "\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff"+
            "\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\1\105\31\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff"+
            "\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff"+
            "\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff"+
            "\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\13\44\1\106\16\44\4\uffff"+
            "\1\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37"+
            "\44\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff"+
            "\2\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff"+
            "\44\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\13\44\1\107\16\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "",
            "",
            "",
            "\1\44\2\uffff\12\44\1\34\6\uffff\30\44\1\110\1\44\4\uffff\1"+
            "\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\30\44\1\111\1\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\4\44\1\114\25\44\4\uffff\1"+
            "\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\4\44\1\115\25\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\2\44\1\120\27\44\4\uffff\1"+
            "\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\2\44\1\121\27\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\24\44\1\122\5\44\4\uffff\1"+
            "\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\24\44\1\123\5\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            "",
            "",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            "",
            "",
            "\1\44\2\uffff\12\44\1\34\6\uffff\23\44\1\126\6\44\4\uffff\1"+
            "\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\23\44\1\127\6\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\4\44\1\130\25\44\4\uffff\1"+
            "\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\4\44\1\131\25\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "",
            "",
            "\1\44\2\uffff\12\44\1\34\6\uffff\13\44\1\132\16\44\4\uffff"+
            "\1\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37"+
            "\44\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff"+
            "\2\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff"+
            "\44\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\13\44\1\133\16\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\30\44\1\135\1\44\4\uffff\1"+
            "\44\1\uffff\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\30\44\1\136\1\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44"+
            "\1\uffff\u0286\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2"+
            "\44\57\uffff\u0120\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44"+
            "\u2100\uffff\u04d0\44\40\uffff\u020e\44",
            "",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            "\1\44\2\uffff\12\44\1\34\6\uffff\32\44\4\uffff\1\44\1\uffff"+
            "\32\44\74\uffff\1\44\10\uffff\27\44\1\uffff\37\44\1\uffff\u0286"+
            "\44\1\uffff\u1c81\44\14\uffff\2\44\61\uffff\2\44\57\uffff\u0120"+
            "\44\u0a70\uffff\u03f0\44\21\uffff\ua7ff\44\u2100\uffff\u04d0"+
            "\44\40\uffff\u020e\44",
            ""
    };

    static final short[] DFA28_eot = DFA.unpackEncodedString(DFA28_eotS);
    static final short[] DFA28_eof = DFA.unpackEncodedString(DFA28_eofS);
    static final char[] DFA28_min = DFA.unpackEncodedStringToUnsignedChars(DFA28_minS);
    static final char[] DFA28_max = DFA.unpackEncodedStringToUnsignedChars(DFA28_maxS);
    static final short[] DFA28_accept = DFA.unpackEncodedString(DFA28_acceptS);
    static final short[] DFA28_special = DFA.unpackEncodedString(DFA28_specialS);
    static final short[][] DFA28_transition;

    static {
        int numStates = DFA28_transitionS.length;
        DFA28_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA28_transition[i] = DFA.unpackEncodedString(DFA28_transitionS[i]);
        }
    }

    class DFA28 extends DFA {

        public DFA28(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 28;
            this.eot = DFA28_eot;
            this.eof = DFA28_eof;
            this.min = DFA28_min;
            this.max = DFA28_max;
            this.accept = DFA28_accept;
            this.special = DFA28_special;
            this.transition = DFA28_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( T__45 | T__46 | T__47 | T__48 | T__49 | T__50 | T__51 | WS | NEWLINE | COMMENT | MULTILINE_COMMENT | IRIREF | OR | AND | NOT | SOME | ONLY | MIN | MAX | EXACTLY | VALUE | INTEGER | PNAME_NS | PNAME_LN | LANGTAG | STRING_LITERAL1 | STRING_LITERAL2 );";
        }
    }
 

}