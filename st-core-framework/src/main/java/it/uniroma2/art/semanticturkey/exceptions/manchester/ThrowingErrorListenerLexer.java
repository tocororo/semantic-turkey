package it.uniroma2.art.semanticturkey.exceptions.manchester;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;

public class ThrowingErrorListenerLexer extends BaseErrorListener {

    public static final ThrowingErrorListenerLexer INSTANCE = new ThrowingErrorListenerLexer();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg,
            RecognitionException e) throws ManchesterSyntaxRuntimeException {
        int pos = charPositionInLine;
        String offendingTerm = "";
        String[] splittedArray = msg.split("'");
        if(splittedArray.length == 2){
            offendingTerm = splittedArray[1];
        }
        throw  new ManchesterSyntaxRuntimeException(msg, pos, offendingTerm, new ArrayList<>());
    }
}
