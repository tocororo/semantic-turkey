package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;

import it.uniroma2.art.semanticturkey.exceptions.ManchesterParserRuntimeException;

public class BailErrorStrategy extends DefaultErrorStrategy {

	@Override
	public void recover(Parser recognizer, RecognitionException e) {
		throw new ManchesterParserRuntimeException(e);
	}
	
	@Override
	public Token recoverInline(Parser recognizer){
		throw new ManchesterParserRuntimeException(new InputMismatchException(recognizer));
	}
	
	@Override
	public void sync(Parser recognizer) throws RecognitionException {}
	
}
