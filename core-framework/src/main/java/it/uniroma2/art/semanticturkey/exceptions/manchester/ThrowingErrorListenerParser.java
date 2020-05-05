package it.uniroma2.art.semanticturkey.exceptions.manchester;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.Interval;

import java.util.ArrayList;
import java.util.List;

public class ThrowingErrorListenerParser extends BaseErrorListener {

    public static final ThrowingErrorListenerParser INSTANCE = new ThrowingErrorListenerParser();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
            throws ManchesterSyntaxRuntimeException {

        String offendingTerm = e.getOffendingToken().getText();
        int pos = e.getOffendingToken().getStartIndex();

        List<String> exptectedTokenList = new ArrayList<>();
        for(Interval interval : e.getExpectedTokens().getIntervals()){
            exptectedTokenList.add(recognizer.getVocabulary().getDisplayName(interval.a));
        }
        throw  new ManchesterSyntaxRuntimeException(e.getMessage(), pos, offendingTerm, exptectedTokenList);
    }
}
