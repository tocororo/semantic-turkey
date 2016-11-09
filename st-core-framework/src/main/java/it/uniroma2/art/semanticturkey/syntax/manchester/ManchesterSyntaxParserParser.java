// $ANTLR 3.4 D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g 2016-11-08 15:15:27

package it.uniroma2.art.semanticturkey.syntax.manchester;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

import org.antlr.runtime.tree.*;


@SuppressWarnings({"all", "warnings", "unchecked"})
public class ManchesterSyntaxParserParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "AND", "AST_AND", "AST_BASECLASS", "AST_CARDINALITY", "AST_NOT", "AST_ONEOFLIST", "AST_ONLY", "AST_OR", "AST_PREFIXED_NAME", "AST_SOME", "AST_VALUE", "COMMENT", "ECHAR", "EXACTLY", "HEX", "INTEGER", "IRIREF", "JAVA_LETTER", "LANGTAG", "MAX", "MIN", "MULTILINE_COMMENT", "NEWLINE", "NOT", "ONLY", "OR", "PERCENT", "PLX", "PNAME_LN", "PNAME_NS", "PN_CHARS", "PN_CHARS_BASE", "PN_CHARS_U", "PN_LOCAL", "PN_LOCAL_ESC", "PN_PREFIX", "SOME", "STRING_LITERAL1", "STRING_LITERAL2", "VALUE", "WS", "'('", "')'", "','", "'.'", "'^^'", "'{'", "'}'"
    };

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
    public Parser[] getDelegates() {
        return new Parser[] {};
    }

    // delegators


    public ManchesterSyntaxParserParser(TokenStream input) {
        this(input, new RecognizerSharedState());
    }
    public ManchesterSyntaxParserParser(TokenStream input, RecognizerSharedState state) {
        super(input, state);
    }

protected TreeAdaptor adaptor = new CommonTreeAdaptor();

public void setTreeAdaptor(TreeAdaptor adaptor) {
    this.adaptor = adaptor;
}
public TreeAdaptor getTreeAdaptor() {
    return adaptor;
}
    public String[] getTokenNames() { return ManchesterSyntaxParserParser.tokenNames; }
    public String getGrammarFileName() { return "D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g"; }


    public static class manchesterExpression_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "manchesterExpression"
    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:36:1: manchesterExpression : description ( '.' )? ;
    public final ManchesterSyntaxParserParser.manchesterExpression_return manchesterExpression() throws RecognitionException {
        ManchesterSyntaxParserParser.manchesterExpression_return retval = new ManchesterSyntaxParserParser.manchesterExpression_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal2=null;
        ManchesterSyntaxParserParser.description_return description1 =null;


        Object char_literal2_tree=null;

        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:37:2: ( description ( '.' )? )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:38:2: description ( '.' )?
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_description_in_manchesterExpression108);
            description1=description();

            state._fsp--;

            adaptor.addChild(root_0, description1.getTree());

            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:38:15: ( '.' )?
            int alt1=2;
            int LA1_0 = input.LA(1);

            if ( (LA1_0==48) ) {
                alt1=1;
            }
            switch (alt1) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:38:15: '.'
                    {
                    char_literal2=(Token)match(input,48,FOLLOW_48_in_manchesterExpression111); 
                    char_literal2_tree = 
                    (Object)adaptor.create(char_literal2)
                    ;
                    adaptor.addChild(root_0, char_literal2_tree);


                    }
                    break;

            }


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "manchesterExpression"


    public static class description_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "description"
    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:41:1: description : conjunction ( OR conjunction )* -> {N>0}? ^( AST_OR ( conjunction )+ ) -> ( conjunction )+ ;
    public final ManchesterSyntaxParserParser.description_return description() throws RecognitionException {
        ManchesterSyntaxParserParser.description_return retval = new ManchesterSyntaxParserParser.description_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token OR4=null;
        ManchesterSyntaxParserParser.conjunction_return conjunction3 =null;

        ManchesterSyntaxParserParser.conjunction_return conjunction5 =null;


        Object OR4_tree=null;
        RewriteRuleTokenStream stream_OR=new RewriteRuleTokenStream(adaptor,"token OR");
        RewriteRuleSubtreeStream stream_conjunction=new RewriteRuleSubtreeStream(adaptor,"rule conjunction");
         int N = 0; 
        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:43:2: ( conjunction ( OR conjunction )* -> {N>0}? ^( AST_OR ( conjunction )+ ) -> ( conjunction )+ )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:44:2: conjunction ( OR conjunction )*
            {
            pushFollow(FOLLOW_conjunction_in_description131);
            conjunction3=conjunction();

            state._fsp--;

            stream_conjunction.add(conjunction3.getTree());

            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:44:14: ( OR conjunction )*
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( (LA2_0==OR) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:44:16: OR conjunction
            	    {
            	    OR4=(Token)match(input,OR,FOLLOW_OR_in_description135);  
            	    stream_OR.add(OR4);


            	    pushFollow(FOLLOW_conjunction_in_description137);
            	    conjunction5=conjunction();

            	    state._fsp--;

            	    stream_conjunction.add(conjunction5.getTree());

            	    ++N;

            	    }
            	    break;

            	default :
            	    break loop2;
                }
            } while (true);


            // AST REWRITE
            // elements: conjunction, conjunction
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 45:2: -> {N>0}? ^( AST_OR ( conjunction )+ )
            if (N>0) {
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:45:12: ^( AST_OR ( conjunction )+ )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(AST_OR, "AST_OR")
                , root_1);

                if ( !(stream_conjunction.hasNext()) ) {
                    throw new RewriteEarlyExitException();
                }
                while ( stream_conjunction.hasNext() ) {
                    adaptor.addChild(root_1, stream_conjunction.nextTree());

                }
                stream_conjunction.reset();

                adaptor.addChild(root_0, root_1);
                }

            }

            else // 46:2: -> ( conjunction )+
            {
                if ( !(stream_conjunction.hasNext()) ) {
                    throw new RewriteEarlyExitException();
                }
                while ( stream_conjunction.hasNext() ) {
                    adaptor.addChild(root_0, stream_conjunction.nextTree());

                }
                stream_conjunction.reset();

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "description"


    public static class conjunction_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "conjunction"
    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:49:1: conjunction : primary ( AND primary )* -> {N>0}? ^( AST_AND ( primary )+ ) -> ( primary )+ ;
    public final ManchesterSyntaxParserParser.conjunction_return conjunction() throws RecognitionException {
        ManchesterSyntaxParserParser.conjunction_return retval = new ManchesterSyntaxParserParser.conjunction_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token AND7=null;
        ManchesterSyntaxParserParser.primary_return primary6 =null;

        ManchesterSyntaxParserParser.primary_return primary8 =null;


        Object AND7_tree=null;
        RewriteRuleTokenStream stream_AND=new RewriteRuleTokenStream(adaptor,"token AND");
        RewriteRuleSubtreeStream stream_primary=new RewriteRuleSubtreeStream(adaptor,"rule primary");
         int N = 0; 
        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:51:2: ( primary ( AND primary )* -> {N>0}? ^( AST_AND ( primary )+ ) -> ( primary )+ )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:52:2: primary ( AND primary )*
            {
            pushFollow(FOLLOW_primary_in_conjunction180);
            primary6=primary();

            state._fsp--;

            stream_primary.add(primary6.getTree());

            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:52:10: ( AND primary )*
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( (LA3_0==AND) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:52:12: AND primary
            	    {
            	    AND7=(Token)match(input,AND,FOLLOW_AND_in_conjunction184);  
            	    stream_AND.add(AND7);


            	    pushFollow(FOLLOW_primary_in_conjunction186);
            	    primary8=primary();

            	    state._fsp--;

            	    stream_primary.add(primary8.getTree());

            	    ++N;

            	    }
            	    break;

            	default :
            	    break loop3;
                }
            } while (true);


            // AST REWRITE
            // elements: primary, primary
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 53:2: -> {N>0}? ^( AST_AND ( primary )+ )
            if (N>0) {
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:53:12: ^( AST_AND ( primary )+ )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(AST_AND, "AST_AND")
                , root_1);

                if ( !(stream_primary.hasNext()) ) {
                    throw new RewriteEarlyExitException();
                }
                while ( stream_primary.hasNext() ) {
                    adaptor.addChild(root_1, stream_primary.nextTree());

                }
                stream_primary.reset();

                adaptor.addChild(root_0, root_1);
                }

            }

            else // 54:2: -> ( primary )+
            {
                if ( !(stream_primary.hasNext()) ) {
                    throw new RewriteEarlyExitException();
                }
                while ( stream_primary.hasNext() ) {
                    adaptor.addChild(root_0, stream_primary.nextTree());

                }
                stream_primary.reset();

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "conjunction"


    public static class primary_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "primary"
    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:57:1: primary : ( NOT primary2 -> ^( AST_NOT primary2 ) | primary2 );
    public final ManchesterSyntaxParserParser.primary_return primary() throws RecognitionException {
        ManchesterSyntaxParserParser.primary_return retval = new ManchesterSyntaxParserParser.primary_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token NOT9=null;
        ManchesterSyntaxParserParser.primary2_return primary210 =null;

        ManchesterSyntaxParserParser.primary2_return primary211 =null;


        Object NOT9_tree=null;
        RewriteRuleTokenStream stream_NOT=new RewriteRuleTokenStream(adaptor,"token NOT");
        RewriteRuleSubtreeStream stream_primary2=new RewriteRuleSubtreeStream(adaptor,"rule primary2");
        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:58:2: ( NOT primary2 -> ^( AST_NOT primary2 ) | primary2 )
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0==NOT) ) {
                alt4=1;
            }
            else if ( (LA4_0==IRIREF||(LA4_0 >= PNAME_LN && LA4_0 <= PNAME_NS)||LA4_0==45||LA4_0==50) ) {
                alt4=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 4, 0, input);

                throw nvae;

            }
            switch (alt4) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:59:2: NOT primary2
                    {
                    NOT9=(Token)match(input,NOT,FOLLOW_NOT_in_primary221);  
                    stream_NOT.add(NOT9);


                    pushFollow(FOLLOW_primary2_in_primary223);
                    primary210=primary2();

                    state._fsp--;

                    stream_primary2.add(primary210.getTree());

                    // AST REWRITE
                    // elements: primary2
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 60:2: -> ^( AST_NOT primary2 )
                    {
                        // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:60:5: ^( AST_NOT primary2 )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(
                        (Object)adaptor.create(AST_NOT, "AST_NOT")
                        , root_1);

                        adaptor.addChild(root_1, stream_primary2.nextTree());

                        adaptor.addChild(root_0, root_1);
                        }

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 2 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:62:2: primary2
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_primary2_in_primary238);
                    primary211=primary2();

                    state._fsp--;

                    adaptor.addChild(root_0, primary211.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "primary"


    public static class primary2_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "primary2"
    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:66:1: primary2 : ( restriction | atomic );
    public final ManchesterSyntaxParserParser.primary2_return primary2() throws RecognitionException {
        ManchesterSyntaxParserParser.primary2_return retval = new ManchesterSyntaxParserParser.primary2_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        ManchesterSyntaxParserParser.restriction_return restriction12 =null;

        ManchesterSyntaxParserParser.atomic_return atomic13 =null;



        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:67:2: ( restriction | atomic )
            int alt5=2;
            switch ( input.LA(1) ) {
            case IRIREF:
                {
                int LA5_1 = input.LA(2);

                if ( (LA5_1==EXACTLY||(LA5_1 >= MAX && LA5_1 <= MIN)||LA5_1==ONLY||LA5_1==SOME||LA5_1==VALUE) ) {
                    alt5=1;
                }
                else if ( (LA5_1==EOF||LA5_1==AND||LA5_1==OR||LA5_1==46||LA5_1==48) ) {
                    alt5=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 5, 1, input);

                    throw nvae;

                }
                }
                break;
            case PNAME_LN:
                {
                int LA5_2 = input.LA(2);

                if ( (LA5_2==EXACTLY||(LA5_2 >= MAX && LA5_2 <= MIN)||LA5_2==ONLY||LA5_2==SOME||LA5_2==VALUE) ) {
                    alt5=1;
                }
                else if ( (LA5_2==EOF||LA5_2==AND||LA5_2==OR||LA5_2==46||LA5_2==48) ) {
                    alt5=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 5, 2, input);

                    throw nvae;

                }
                }
                break;
            case PNAME_NS:
                {
                int LA5_3 = input.LA(2);

                if ( (LA5_3==EXACTLY||(LA5_3 >= MAX && LA5_3 <= MIN)||LA5_3==ONLY||LA5_3==SOME||LA5_3==VALUE) ) {
                    alt5=1;
                }
                else if ( (LA5_3==EOF||LA5_3==AND||LA5_3==OR||LA5_3==46||LA5_3==48) ) {
                    alt5=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 5, 3, input);

                    throw nvae;

                }
                }
                break;
            case 45:
            case 50:
                {
                alt5=2;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 5, 0, input);

                throw nvae;

            }

            switch (alt5) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:67:4: restriction
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_restriction_in_primary2252);
                    restriction12=restriction();

                    state._fsp--;

                    adaptor.addChild(root_0, restriction12.getTree());

                    }
                    break;
                case 2 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:70:2: atomic
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_atomic_in_primary2260);
                    atomic13=atomic();

                    state._fsp--;

                    adaptor.addChild(root_0, atomic13.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "primary2"


    public static class restriction_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "restriction"
    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:75:1: restriction : ( prop (qual= SOME |qual= ONLY ) primary -> {$qual.getType() == ONLY}? ^( AST_ONLY prop primary ) -> ^( AST_SOME prop primary ) | prop (card= MIN |card= MAX |card= EXACTLY ) INTEGER -> ^( AST_CARDINALITY prop $card INTEGER ) | prop VALUE value -> ^( AST_VALUE prop value ) );
    public final ManchesterSyntaxParserParser.restriction_return restriction() throws RecognitionException {
        ManchesterSyntaxParserParser.restriction_return retval = new ManchesterSyntaxParserParser.restriction_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token qual=null;
        Token card=null;
        Token INTEGER17=null;
        Token VALUE19=null;
        ManchesterSyntaxParserParser.prop_return prop14 =null;

        ManchesterSyntaxParserParser.primary_return primary15 =null;

        ManchesterSyntaxParserParser.prop_return prop16 =null;

        ManchesterSyntaxParserParser.prop_return prop18 =null;

        ManchesterSyntaxParserParser.value_return value20 =null;


        Object qual_tree=null;
        Object card_tree=null;
        Object INTEGER17_tree=null;
        Object VALUE19_tree=null;
        RewriteRuleTokenStream stream_EXACTLY=new RewriteRuleTokenStream(adaptor,"token EXACTLY");
        RewriteRuleTokenStream stream_SOME=new RewriteRuleTokenStream(adaptor,"token SOME");
        RewriteRuleTokenStream stream_MIN=new RewriteRuleTokenStream(adaptor,"token MIN");
        RewriteRuleTokenStream stream_MAX=new RewriteRuleTokenStream(adaptor,"token MAX");
        RewriteRuleTokenStream stream_ONLY=new RewriteRuleTokenStream(adaptor,"token ONLY");
        RewriteRuleTokenStream stream_VALUE=new RewriteRuleTokenStream(adaptor,"token VALUE");
        RewriteRuleTokenStream stream_INTEGER=new RewriteRuleTokenStream(adaptor,"token INTEGER");
        RewriteRuleSubtreeStream stream_prop=new RewriteRuleSubtreeStream(adaptor,"rule prop");
        RewriteRuleSubtreeStream stream_value=new RewriteRuleSubtreeStream(adaptor,"rule value");
        RewriteRuleSubtreeStream stream_primary=new RewriteRuleSubtreeStream(adaptor,"rule primary");
        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:76:2: ( prop (qual= SOME |qual= ONLY ) primary -> {$qual.getType() == ONLY}? ^( AST_ONLY prop primary ) -> ^( AST_SOME prop primary ) | prop (card= MIN |card= MAX |card= EXACTLY ) INTEGER -> ^( AST_CARDINALITY prop $card INTEGER ) | prop VALUE value -> ^( AST_VALUE prop value ) )
            int alt8=3;
            switch ( input.LA(1) ) {
            case IRIREF:
                {
                switch ( input.LA(2) ) {
                case ONLY:
                case SOME:
                    {
                    alt8=1;
                    }
                    break;
                case EXACTLY:
                case MAX:
                case MIN:
                    {
                    alt8=2;
                    }
                    break;
                case VALUE:
                    {
                    alt8=3;
                    }
                    break;
                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 8, 1, input);

                    throw nvae;

                }

                }
                break;
            case PNAME_LN:
                {
                switch ( input.LA(2) ) {
                case ONLY:
                case SOME:
                    {
                    alt8=1;
                    }
                    break;
                case EXACTLY:
                case MAX:
                case MIN:
                    {
                    alt8=2;
                    }
                    break;
                case VALUE:
                    {
                    alt8=3;
                    }
                    break;
                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 8, 2, input);

                    throw nvae;

                }

                }
                break;
            case PNAME_NS:
                {
                switch ( input.LA(2) ) {
                case ONLY:
                case SOME:
                    {
                    alt8=1;
                    }
                    break;
                case EXACTLY:
                case MAX:
                case MIN:
                    {
                    alt8=2;
                    }
                    break;
                case VALUE:
                    {
                    alt8=3;
                    }
                    break;
                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 8, 3, input);

                    throw nvae;

                }

                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 8, 0, input);

                throw nvae;

            }

            switch (alt8) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:77:2: prop (qual= SOME |qual= ONLY ) primary
                    {
                    pushFollow(FOLLOW_prop_in_restriction277);
                    prop14=prop();

                    state._fsp--;

                    stream_prop.add(prop14.getTree());

                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:77:7: (qual= SOME |qual= ONLY )
                    int alt6=2;
                    int LA6_0 = input.LA(1);

                    if ( (LA6_0==SOME) ) {
                        alt6=1;
                    }
                    else if ( (LA6_0==ONLY) ) {
                        alt6=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 6, 0, input);

                        throw nvae;

                    }
                    switch (alt6) {
                        case 1 :
                            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:77:8: qual= SOME
                            {
                            qual=(Token)match(input,SOME,FOLLOW_SOME_in_restriction282);  
                            stream_SOME.add(qual);


                            }
                            break;
                        case 2 :
                            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:77:20: qual= ONLY
                            {
                            qual=(Token)match(input,ONLY,FOLLOW_ONLY_in_restriction288);  
                            stream_ONLY.add(qual);


                            }
                            break;

                    }


                    pushFollow(FOLLOW_primary_in_restriction291);
                    primary15=primary();

                    state._fsp--;

                    stream_primary.add(primary15.getTree());

                    // AST REWRITE
                    // elements: prop, primary, prop, primary
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 78:2: -> {$qual.getType() == ONLY}? ^( AST_ONLY prop primary )
                    if (qual.getType() == ONLY) {
                        // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:78:32: ^( AST_ONLY prop primary )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(
                        (Object)adaptor.create(AST_ONLY, "AST_ONLY")
                        , root_1);

                        adaptor.addChild(root_1, stream_prop.nextTree());

                        adaptor.addChild(root_1, stream_primary.nextTree());

                        adaptor.addChild(root_0, root_1);
                        }

                    }

                    else // 79:2: -> ^( AST_SOME prop primary )
                    {
                        // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:79:5: ^( AST_SOME prop primary )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(
                        (Object)adaptor.create(AST_SOME, "AST_SOME")
                        , root_1);

                        adaptor.addChild(root_1, stream_prop.nextTree());

                        adaptor.addChild(root_1, stream_primary.nextTree());

                        adaptor.addChild(root_0, root_1);
                        }

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 2 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:81:2: prop (card= MIN |card= MAX |card= EXACTLY ) INTEGER
                    {
                    pushFollow(FOLLOW_prop_in_restriction321);
                    prop16=prop();

                    state._fsp--;

                    stream_prop.add(prop16.getTree());

                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:81:7: (card= MIN |card= MAX |card= EXACTLY )
                    int alt7=3;
                    switch ( input.LA(1) ) {
                    case MIN:
                        {
                        alt7=1;
                        }
                        break;
                    case MAX:
                        {
                        alt7=2;
                        }
                        break;
                    case EXACTLY:
                        {
                        alt7=3;
                        }
                        break;
                    default:
                        NoViableAltException nvae =
                            new NoViableAltException("", 7, 0, input);

                        throw nvae;

                    }

                    switch (alt7) {
                        case 1 :
                            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:81:8: card= MIN
                            {
                            card=(Token)match(input,MIN,FOLLOW_MIN_in_restriction326);  
                            stream_MIN.add(card);


                            }
                            break;
                        case 2 :
                            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:81:19: card= MAX
                            {
                            card=(Token)match(input,MAX,FOLLOW_MAX_in_restriction332);  
                            stream_MAX.add(card);


                            }
                            break;
                        case 3 :
                            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:81:30: card= EXACTLY
                            {
                            card=(Token)match(input,EXACTLY,FOLLOW_EXACTLY_in_restriction338);  
                            stream_EXACTLY.add(card);


                            }
                            break;

                    }


                    INTEGER17=(Token)match(input,INTEGER,FOLLOW_INTEGER_in_restriction342);  
                    stream_INTEGER.add(INTEGER17);


                    // AST REWRITE
                    // elements: card, prop, INTEGER
                    // token labels: card
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleTokenStream stream_card=new RewriteRuleTokenStream(adaptor,"token card",card);
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 82:2: -> ^( AST_CARDINALITY prop $card INTEGER )
                    {
                        // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:82:5: ^( AST_CARDINALITY prop $card INTEGER )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(
                        (Object)adaptor.create(AST_CARDINALITY, "AST_CARDINALITY")
                        , root_1);

                        adaptor.addChild(root_1, stream_prop.nextTree());

                        adaptor.addChild(root_1, stream_card.nextNode());

                        adaptor.addChild(root_1, 
                        stream_INTEGER.nextNode()
                        );

                        adaptor.addChild(root_0, root_1);
                        }

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 3 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:84:2: prop VALUE value
                    {
                    pushFollow(FOLLOW_prop_in_restriction362);
                    prop18=prop();

                    state._fsp--;

                    stream_prop.add(prop18.getTree());

                    VALUE19=(Token)match(input,VALUE,FOLLOW_VALUE_in_restriction364);  
                    stream_VALUE.add(VALUE19);


                    pushFollow(FOLLOW_value_in_restriction366);
                    value20=value();

                    state._fsp--;

                    stream_value.add(value20.getTree());

                    // AST REWRITE
                    // elements: value, prop
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 85:2: -> ^( AST_VALUE prop value )
                    {
                        // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:85:5: ^( AST_VALUE prop value )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(
                        (Object)adaptor.create(AST_VALUE, "AST_VALUE")
                        , root_1);

                        adaptor.addChild(root_1, stream_prop.nextTree());

                        adaptor.addChild(root_1, stream_value.nextTree());

                        adaptor.addChild(root_0, root_1);
                        }

                    }


                    retval.tree = root_0;

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "restriction"


    public static class atomic_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "atomic"
    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:88:1: atomic : ( simpleManchesterClass -> ^( AST_BASECLASS simpleManchesterClass ) | '{' oneOfList '}' -> oneOfList | '(' description ')' -> description );
    public final ManchesterSyntaxParserParser.atomic_return atomic() throws RecognitionException {
        ManchesterSyntaxParserParser.atomic_return retval = new ManchesterSyntaxParserParser.atomic_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal22=null;
        Token char_literal24=null;
        Token char_literal25=null;
        Token char_literal27=null;
        ManchesterSyntaxParserParser.simpleManchesterClass_return simpleManchesterClass21 =null;

        ManchesterSyntaxParserParser.oneOfList_return oneOfList23 =null;

        ManchesterSyntaxParserParser.description_return description26 =null;


        Object char_literal22_tree=null;
        Object char_literal24_tree=null;
        Object char_literal25_tree=null;
        Object char_literal27_tree=null;
        RewriteRuleTokenStream stream_45=new RewriteRuleTokenStream(adaptor,"token 45");
        RewriteRuleTokenStream stream_46=new RewriteRuleTokenStream(adaptor,"token 46");
        RewriteRuleTokenStream stream_50=new RewriteRuleTokenStream(adaptor,"token 50");
        RewriteRuleTokenStream stream_51=new RewriteRuleTokenStream(adaptor,"token 51");
        RewriteRuleSubtreeStream stream_oneOfList=new RewriteRuleSubtreeStream(adaptor,"rule oneOfList");
        RewriteRuleSubtreeStream stream_description=new RewriteRuleSubtreeStream(adaptor,"rule description");
        RewriteRuleSubtreeStream stream_simpleManchesterClass=new RewriteRuleSubtreeStream(adaptor,"rule simpleManchesterClass");
        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:89:2: ( simpleManchesterClass -> ^( AST_BASECLASS simpleManchesterClass ) | '{' oneOfList '}' -> oneOfList | '(' description ')' -> description )
            int alt9=3;
            switch ( input.LA(1) ) {
            case IRIREF:
            case PNAME_LN:
            case PNAME_NS:
                {
                alt9=1;
                }
                break;
            case 50:
                {
                alt9=2;
                }
                break;
            case 45:
                {
                alt9=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 9, 0, input);

                throw nvae;

            }

            switch (alt9) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:90:2: simpleManchesterClass
                    {
                    pushFollow(FOLLOW_simpleManchesterClass_in_atomic389);
                    simpleManchesterClass21=simpleManchesterClass();

                    state._fsp--;

                    stream_simpleManchesterClass.add(simpleManchesterClass21.getTree());

                    // AST REWRITE
                    // elements: simpleManchesterClass
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 91:2: -> ^( AST_BASECLASS simpleManchesterClass )
                    {
                        // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:91:5: ^( AST_BASECLASS simpleManchesterClass )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(
                        (Object)adaptor.create(AST_BASECLASS, "AST_BASECLASS")
                        , root_1);

                        adaptor.addChild(root_1, stream_simpleManchesterClass.nextTree());

                        adaptor.addChild(root_0, root_1);
                        }

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 2 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:93:2: '{' oneOfList '}'
                    {
                    char_literal22=(Token)match(input,50,FOLLOW_50_in_atomic404);  
                    stream_50.add(char_literal22);


                    pushFollow(FOLLOW_oneOfList_in_atomic406);
                    oneOfList23=oneOfList();

                    state._fsp--;

                    stream_oneOfList.add(oneOfList23.getTree());

                    char_literal24=(Token)match(input,51,FOLLOW_51_in_atomic408);  
                    stream_51.add(char_literal24);


                    // AST REWRITE
                    // elements: oneOfList
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 94:2: -> oneOfList
                    {
                        adaptor.addChild(root_0, stream_oneOfList.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 3 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:96:2: '(' description ')'
                    {
                    char_literal25=(Token)match(input,45,FOLLOW_45_in_atomic419);  
                    stream_45.add(char_literal25);


                    pushFollow(FOLLOW_description_in_atomic421);
                    description26=description();

                    state._fsp--;

                    stream_description.add(description26.getTree());

                    char_literal27=(Token)match(input,46,FOLLOW_46_in_atomic424);  
                    stream_46.add(char_literal27);


                    // AST REWRITE
                    // elements: description
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 97:2: -> description
                    {
                        adaptor.addChild(root_0, stream_description.nextTree());

                    }


                    retval.tree = root_0;

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "atomic"


    public static class simpleManchesterClass_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "simpleManchesterClass"
    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:100:1: simpleManchesterClass : res ;
    public final ManchesterSyntaxParserParser.simpleManchesterClass_return simpleManchesterClass() throws RecognitionException {
        ManchesterSyntaxParserParser.simpleManchesterClass_return retval = new ManchesterSyntaxParserParser.simpleManchesterClass_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        ManchesterSyntaxParserParser.res_return res28 =null;



        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:101:2: ( res )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:102:2: res
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_res_in_simpleManchesterClass442);
            res28=res();

            state._fsp--;

            adaptor.addChild(root_0, res28.getTree());

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "simpleManchesterClass"


    public static class oneOfList_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "oneOfList"
    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:148:1: oneOfList : res ( ',' res )* -> ^( AST_ONEOFLIST ( res )+ ) ;
    public final ManchesterSyntaxParserParser.oneOfList_return oneOfList() throws RecognitionException {
        ManchesterSyntaxParserParser.oneOfList_return retval = new ManchesterSyntaxParserParser.oneOfList_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token char_literal30=null;
        ManchesterSyntaxParserParser.res_return res29 =null;

        ManchesterSyntaxParserParser.res_return res31 =null;


        Object char_literal30_tree=null;
        RewriteRuleTokenStream stream_47=new RewriteRuleTokenStream(adaptor,"token 47");
        RewriteRuleSubtreeStream stream_res=new RewriteRuleSubtreeStream(adaptor,"rule res");
        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:149:2: ( res ( ',' res )* -> ^( AST_ONEOFLIST ( res )+ ) )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:150:2: res ( ',' res )*
            {
            pushFollow(FOLLOW_res_in_oneOfList462);
            res29=res();

            state._fsp--;

            stream_res.add(res29.getTree());

            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:150:6: ( ',' res )*
            loop10:
            do {
                int alt10=2;
                int LA10_0 = input.LA(1);

                if ( (LA10_0==47) ) {
                    alt10=1;
                }


                switch (alt10) {
            	case 1 :
            	    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:150:7: ',' res
            	    {
            	    char_literal30=(Token)match(input,47,FOLLOW_47_in_oneOfList465);  
            	    stream_47.add(char_literal30);


            	    pushFollow(FOLLOW_res_in_oneOfList467);
            	    res31=res();

            	    state._fsp--;

            	    stream_res.add(res31.getTree());

            	    }
            	    break;

            	default :
            	    break loop10;
                }
            } while (true);


            // AST REWRITE
            // elements: res
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 151:2: -> ^( AST_ONEOFLIST ( res )+ )
            {
                // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:151:5: ^( AST_ONEOFLIST ( res )+ )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(AST_ONEOFLIST, "AST_ONEOFLIST")
                , root_1);

                if ( !(stream_res.hasNext()) ) {
                    throw new RewriteEarlyExitException();
                }
                while ( stream_res.hasNext() ) {
                    adaptor.addChild(root_1, stream_res.nextTree());

                }
                stream_res.reset();

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "oneOfList"


    public static class prop_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "prop"
    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:154:1: prop : res ;
    public final ManchesterSyntaxParserParser.prop_return prop() throws RecognitionException {
        ManchesterSyntaxParserParser.prop_return retval = new ManchesterSyntaxParserParser.prop_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        ManchesterSyntaxParserParser.res_return res32 =null;



        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:155:2: ( res )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:156:2: res
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_res_in_prop492);
            res32=res();

            state._fsp--;

            adaptor.addChild(root_0, res32.getTree());

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "prop"


    public static class value_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "value"
    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:159:1: value : ( res | rdfLiteral );
    public final ManchesterSyntaxParserParser.value_return value() throws RecognitionException {
        ManchesterSyntaxParserParser.value_return retval = new ManchesterSyntaxParserParser.value_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        ManchesterSyntaxParserParser.res_return res33 =null;

        ManchesterSyntaxParserParser.rdfLiteral_return rdfLiteral34 =null;



        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:160:2: ( res | rdfLiteral )
            int alt11=2;
            int LA11_0 = input.LA(1);

            if ( (LA11_0==IRIREF||(LA11_0 >= PNAME_LN && LA11_0 <= PNAME_NS)) ) {
                alt11=1;
            }
            else if ( ((LA11_0 >= STRING_LITERAL1 && LA11_0 <= STRING_LITERAL2)) ) {
                alt11=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 11, 0, input);

                throw nvae;

            }
            switch (alt11) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:161:2: res
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_res_in_value504);
                    res33=res();

                    state._fsp--;

                    adaptor.addChild(root_0, res33.getTree());

                    }
                    break;
                case 2 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:161:8: rdfLiteral
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_rdfLiteral_in_value508);
                    rdfLiteral34=rdfLiteral();

                    state._fsp--;

                    adaptor.addChild(root_0, rdfLiteral34.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "value"


    public static class res_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "res"
    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:164:1: res : ( IRIREF | prefixedName );
    public final ManchesterSyntaxParserParser.res_return res() throws RecognitionException {
        ManchesterSyntaxParserParser.res_return retval = new ManchesterSyntaxParserParser.res_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token IRIREF35=null;
        ManchesterSyntaxParserParser.prefixedName_return prefixedName36 =null;


        Object IRIREF35_tree=null;

        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:165:2: ( IRIREF | prefixedName )
            int alt12=2;
            int LA12_0 = input.LA(1);

            if ( (LA12_0==IRIREF) ) {
                alt12=1;
            }
            else if ( ((LA12_0 >= PNAME_LN && LA12_0 <= PNAME_NS)) ) {
                alt12=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 12, 0, input);

                throw nvae;

            }
            switch (alt12) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:167:2: IRIREF
                    {
                    root_0 = (Object)adaptor.nil();


                    IRIREF35=(Token)match(input,IRIREF,FOLLOW_IRIREF_in_res522); 
                    IRIREF35_tree = 
                    (Object)adaptor.create(IRIREF35)
                    ;
                    adaptor.addChild(root_0, IRIREF35_tree);


                    }
                    break;
                case 2 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:167:11: prefixedName
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_prefixedName_in_res526);
                    prefixedName36=prefixedName();

                    state._fsp--;

                    adaptor.addChild(root_0, prefixedName36.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "res"


    public static class prefixedName_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "prefixedName"
    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:170:1: prefixedName : ( PNAME_LN -> ^( AST_PREFIXED_NAME PNAME_LN ) | PNAME_NS -> ^( AST_PREFIXED_NAME PNAME_NS ) );
    public final ManchesterSyntaxParserParser.prefixedName_return prefixedName() throws RecognitionException {
        ManchesterSyntaxParserParser.prefixedName_return retval = new ManchesterSyntaxParserParser.prefixedName_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token PNAME_LN37=null;
        Token PNAME_NS38=null;

        Object PNAME_LN37_tree=null;
        Object PNAME_NS38_tree=null;
        RewriteRuleTokenStream stream_PNAME_NS=new RewriteRuleTokenStream(adaptor,"token PNAME_NS");
        RewriteRuleTokenStream stream_PNAME_LN=new RewriteRuleTokenStream(adaptor,"token PNAME_LN");

        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:171:3: ( PNAME_LN -> ^( AST_PREFIXED_NAME PNAME_LN ) | PNAME_NS -> ^( AST_PREFIXED_NAME PNAME_NS ) )
            int alt13=2;
            int LA13_0 = input.LA(1);

            if ( (LA13_0==PNAME_LN) ) {
                alt13=1;
            }
            else if ( (LA13_0==PNAME_NS) ) {
                alt13=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 13, 0, input);

                throw nvae;

            }
            switch (alt13) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:172:3: PNAME_LN
                    {
                    PNAME_LN37=(Token)match(input,PNAME_LN,FOLLOW_PNAME_LN_in_prefixedName540);  
                    stream_PNAME_LN.add(PNAME_LN37);


                    // AST REWRITE
                    // elements: PNAME_LN
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 173:3: -> ^( AST_PREFIXED_NAME PNAME_LN )
                    {
                        // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:173:6: ^( AST_PREFIXED_NAME PNAME_LN )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(
                        (Object)adaptor.create(AST_PREFIXED_NAME, "AST_PREFIXED_NAME")
                        , root_1);

                        adaptor.addChild(root_1, 
                        stream_PNAME_LN.nextNode()
                        );

                        adaptor.addChild(root_0, root_1);
                        }

                    }


                    retval.tree = root_0;

                    }
                    break;
                case 2 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:175:3: PNAME_NS
                    {
                    PNAME_NS38=(Token)match(input,PNAME_NS,FOLLOW_PNAME_NS_in_prefixedName559);  
                    stream_PNAME_NS.add(PNAME_NS38);


                    // AST REWRITE
                    // elements: PNAME_NS
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

                    root_0 = (Object)adaptor.nil();
                    // 176:3: -> ^( AST_PREFIXED_NAME PNAME_NS )
                    {
                        // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:176:6: ^( AST_PREFIXED_NAME PNAME_NS )
                        {
                        Object root_1 = (Object)adaptor.nil();
                        root_1 = (Object)adaptor.becomeRoot(
                        (Object)adaptor.create(AST_PREFIXED_NAME, "AST_PREFIXED_NAME")
                        , root_1);

                        adaptor.addChild(root_1, 
                        stream_PNAME_NS.nextNode()
                        );

                        adaptor.addChild(root_0, root_1);
                        }

                    }


                    retval.tree = root_0;

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "prefixedName"


    public static class rdfLiteral_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "rdfLiteral"
    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:180:1: rdfLiteral : string ( LANGTAG | ( '^^' res ) )? ;
    public final ManchesterSyntaxParserParser.rdfLiteral_return rdfLiteral() throws RecognitionException {
        ManchesterSyntaxParserParser.rdfLiteral_return retval = new ManchesterSyntaxParserParser.rdfLiteral_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token LANGTAG40=null;
        Token string_literal41=null;
        ManchesterSyntaxParserParser.string_return string39 =null;

        ManchesterSyntaxParserParser.res_return res42 =null;


        Object LANGTAG40_tree=null;
        Object string_literal41_tree=null;

        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:181:2: ( string ( LANGTAG | ( '^^' res ) )? )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:183:2: string ( LANGTAG | ( '^^' res ) )?
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_string_in_rdfLiteral589);
            string39=string();

            state._fsp--;

            adaptor.addChild(root_0, string39.getTree());

            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:183:9: ( LANGTAG | ( '^^' res ) )?
            int alt14=3;
            int LA14_0 = input.LA(1);

            if ( (LA14_0==LANGTAG) ) {
                alt14=1;
            }
            else if ( (LA14_0==49) ) {
                alt14=2;
            }
            switch (alt14) {
                case 1 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:183:11: LANGTAG
                    {
                    LANGTAG40=(Token)match(input,LANGTAG,FOLLOW_LANGTAG_in_rdfLiteral593); 
                    LANGTAG40_tree = 
                    (Object)adaptor.create(LANGTAG40)
                    ;
                    adaptor.addChild(root_0, LANGTAG40_tree);


                    }
                    break;
                case 2 :
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:183:21: ( '^^' res )
                    {
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:183:21: ( '^^' res )
                    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:183:23: '^^' res
                    {
                    string_literal41=(Token)match(input,49,FOLLOW_49_in_rdfLiteral599); 
                    string_literal41_tree = 
                    (Object)adaptor.create(string_literal41)
                    ;
                    adaptor.addChild(root_0, string_literal41_tree);


                    pushFollow(FOLLOW_res_in_rdfLiteral601);
                    res42=res();

                    state._fsp--;

                    adaptor.addChild(root_0, res42.getTree());

                    }


                    }
                    break;

            }


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "rdfLiteral"


    public static class string_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "string"
    // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:187:1: string : ( STRING_LITERAL1 | STRING_LITERAL2 );
    public final ManchesterSyntaxParserParser.string_return string() throws RecognitionException {
        ManchesterSyntaxParserParser.string_return retval = new ManchesterSyntaxParserParser.string_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token set43=null;

        Object set43_tree=null;

        try {
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:188:2: ( STRING_LITERAL1 | STRING_LITERAL2 )
            // D:\\java_workspace\\semanticTurkeySpring\\st-core-framework\\src\\main\\java\\it\\uniroma2\\art\\semanticturkey\\syntax\\manchester\\ManchesterSyntaxParser.g:
            {
            root_0 = (Object)adaptor.nil();


            set43=(Token)input.LT(1);

            if ( (input.LA(1) >= STRING_LITERAL1 && input.LA(1) <= STRING_LITERAL2) ) {
                input.consume();
                adaptor.addChild(root_0, 
                (Object)adaptor.create(set43)
                );
                state.errorRecovery=false;
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "string"

    // Delegated rules


 

    public static final BitSet FOLLOW_description_in_manchesterExpression108 = new BitSet(new long[]{0x0001000000000002L});
    public static final BitSet FOLLOW_48_in_manchesterExpression111 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_conjunction_in_description131 = new BitSet(new long[]{0x0000000020000002L});
    public static final BitSet FOLLOW_OR_in_description135 = new BitSet(new long[]{0x0004200308100000L});
    public static final BitSet FOLLOW_conjunction_in_description137 = new BitSet(new long[]{0x0000000020000002L});
    public static final BitSet FOLLOW_primary_in_conjunction180 = new BitSet(new long[]{0x0000000000000012L});
    public static final BitSet FOLLOW_AND_in_conjunction184 = new BitSet(new long[]{0x0004200308100000L});
    public static final BitSet FOLLOW_primary_in_conjunction186 = new BitSet(new long[]{0x0000000000000012L});
    public static final BitSet FOLLOW_NOT_in_primary221 = new BitSet(new long[]{0x0004200300100000L});
    public static final BitSet FOLLOW_primary2_in_primary223 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_primary2_in_primary238 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_restriction_in_primary2252 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_atomic_in_primary2260 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_prop_in_restriction277 = new BitSet(new long[]{0x0000010010000000L});
    public static final BitSet FOLLOW_SOME_in_restriction282 = new BitSet(new long[]{0x0004200308100000L});
    public static final BitSet FOLLOW_ONLY_in_restriction288 = new BitSet(new long[]{0x0004200308100000L});
    public static final BitSet FOLLOW_primary_in_restriction291 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_prop_in_restriction321 = new BitSet(new long[]{0x0000000001820000L});
    public static final BitSet FOLLOW_MIN_in_restriction326 = new BitSet(new long[]{0x0000000000080000L});
    public static final BitSet FOLLOW_MAX_in_restriction332 = new BitSet(new long[]{0x0000000000080000L});
    public static final BitSet FOLLOW_EXACTLY_in_restriction338 = new BitSet(new long[]{0x0000000000080000L});
    public static final BitSet FOLLOW_INTEGER_in_restriction342 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_prop_in_restriction362 = new BitSet(new long[]{0x0000080000000000L});
    public static final BitSet FOLLOW_VALUE_in_restriction364 = new BitSet(new long[]{0x0000060300100000L});
    public static final BitSet FOLLOW_value_in_restriction366 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_simpleManchesterClass_in_atomic389 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_50_in_atomic404 = new BitSet(new long[]{0x0000000300100000L});
    public static final BitSet FOLLOW_oneOfList_in_atomic406 = new BitSet(new long[]{0x0008000000000000L});
    public static final BitSet FOLLOW_51_in_atomic408 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_45_in_atomic419 = new BitSet(new long[]{0x0004200308100000L});
    public static final BitSet FOLLOW_description_in_atomic421 = new BitSet(new long[]{0x0000400000000000L});
    public static final BitSet FOLLOW_46_in_atomic424 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_res_in_simpleManchesterClass442 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_res_in_oneOfList462 = new BitSet(new long[]{0x0000800000000002L});
    public static final BitSet FOLLOW_47_in_oneOfList465 = new BitSet(new long[]{0x0000000300100000L});
    public static final BitSet FOLLOW_res_in_oneOfList467 = new BitSet(new long[]{0x0000800000000002L});
    public static final BitSet FOLLOW_res_in_prop492 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_res_in_value504 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rdfLiteral_in_value508 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_IRIREF_in_res522 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_prefixedName_in_res526 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PNAME_LN_in_prefixedName540 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PNAME_NS_in_prefixedName559 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_string_in_rdfLiteral589 = new BitSet(new long[]{0x0002000000400002L});
    public static final BitSet FOLLOW_LANGTAG_in_rdfLiteral593 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_49_in_rdfLiteral599 = new BitSet(new long[]{0x0000000300100000L});
    public static final BitSet FOLLOW_res_in_rdfLiteral601 = new BitSet(new long[]{0x0000000000000002L});

}