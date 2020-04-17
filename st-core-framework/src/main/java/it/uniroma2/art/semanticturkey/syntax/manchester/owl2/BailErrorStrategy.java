package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;

import it.uniroma2.art.semanticturkey.exceptions.manchester.ManchesterParserRuntimeException;
import org.antlr.v4.runtime.misc.Interval;

import java.util.ArrayList;
import java.util.List;

public class BailErrorStrategy extends DefaultErrorStrategy {

	@Override
	public void recover(Parser recognizer, RecognitionException e) {
		String msg = e.getMessage();
		int pos = recognizer.getCurrentToken().getStartIndex();
		String currentTerm = recognizer.getCurrentToken().getText();
		List<String> exptectedTokenList = new ArrayList<>();
		for(Interval interval : recognizer.getExpectedTokens().getIntervals()){
			exptectedTokenList.add(recognizer.getVocabulary().getDisplayName(interval.a));
		}
		throw new ManchesterParserRuntimeException(e.getMessage(), pos, currentTerm, exptectedTokenList);
	}
	
	@Override
	public Token recoverInline(Parser recognizer){
		int pos = recognizer.getCurrentToken().getStartIndex();
		String currentTerm = recognizer.getCurrentToken().getText();
		String msg = "Wrong Term "+currentTerm;
		List<String> exptectedTokenList = new ArrayList<>();
		for(Interval interval : recognizer.getExpectedTokens().getIntervals()){
			exptectedTokenList.add(recognizer.getVocabulary().getDisplayName(interval.a));
		}
		throw new ManchesterParserRuntimeException(msg, pos, currentTerm, exptectedTokenList);
	}
	
	@Override
	public void sync(Parser recognizer) throws RecognitionException {

	}
	
}
