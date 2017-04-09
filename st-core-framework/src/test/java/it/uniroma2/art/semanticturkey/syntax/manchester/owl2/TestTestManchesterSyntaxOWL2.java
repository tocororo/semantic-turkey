package it.uniroma2.art.semanticturkey.syntax.manchester.owl2;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import it.uniroma2.art.semanticturkey.exceptions.ManchesterParserException;


public class TestTestManchesterSyntaxOWL2 {

	public static void main(String[] args) {
		TestTestManchesterSyntaxOWL2 testOWL2 = new TestTestManchesterSyntaxOWL2();
		
		 Map <String, String>prefixToNamsepace = new HashMap<String, String>();
		 prefixToNamsepace.put("", "http://test.it/");
		 SimpleValueFactory simpleValueFactory = SimpleValueFactory.getInstance();
		
		
		String dlExpr="";
		ManchesterClassInterface mci = null;
		
		//String dlExpr = "<http://test.it/knows> some <http://test.it/Person>";
		//String dlExpr = "<http://test.it/knows> some {<http://test.it/Person>, <http://test.it/Animal>}";
		//String dlExpr = "<http://test.it/knows> SOME {<http://test.it/Person>, <http://test.it/Animal>}";
		//String dlExpr = "{<http://test.it/Person>, <http://test.it/Animal>}";
		//String dlExpr = "{<http://test.it/Person>, <http://test.it/Animal>} or (<http://test.it>) ";
		//String dlExpr = "<http://test.it/Person> OR (<http://test.it/Animal>) ";
		//String dlExpr = "<http://test.it/Person> OR (<http://test.it/Animal>) OR NOT <http://test.it/Plant>";
		//String dlExpr = "(<http://test.it/Person>) OR <http://test.it/Animal> OR NOT <http://test.it/Plant>";
		//String dlExpr = "<http://test.it/Person> OR "
		//		+ "(<http://test.it/Animal> AND inverse <http://test.it/eatenBy> some <http://test.it/Food>) "
		//		+ "OR NOT <http://test.it/Plant>";
		//String dlExpr = "{<http://test.it/Person>, <http://test.it/Animal>} or (<http://test.it> or "
		//		+ "<http://test.it/knows> Self )";
		//String dlExpr = "<http://test.it/eat> max 6";
		//String dlExpr = "<http://test.it/knows> min 5 integer OR <http://test.it/eat> max 6";
		//String dlExpr = "<http://test.it#prop1>value \"Mario\"@it";
		
		
		//complex test
		dlExpr = 
				" <http://test.it/prop1> some <http://test.it/Class1> OR " +
				" <http://test.it/prop2> only <http://test.it/Class2> OR " + 
				" {<http://test.it/inst1> , <http://test.it/inst2>} OR" +
				" <http://test.it/> value <http://test.it/inst3> OR" +
				" ( <http://test.it/prop3> SELF AND <http://test.it/prop4> SELF ) OR" +
				" <http://test.it/prop4> min 4 OR" + 
				" <http://test.it/prop5> min 5 <http://test.it/Class5> OR" +
				" <http://test.it/prop6> max 6 OR" +
				" <http://test.it/prop7> max 7 <http://test.it/Class7> OR" + 
				" <http://test.it/prop8> exactly 8 OR" +
				" <http://test.it/prop9> exactly 9 <http://test.it/Class9> OR "+
				"<http://test.it#prop1>value \"Mario\"@it";
				;
		try {
			mci = testOWL2.test(dlExpr, simpleValueFactory, prefixToNamsepace, false);
		} catch (ManchesterParserException e) {
			e.printStackTrace();
		}
		
		
				
		//wrong
		/*dlExpr = ":Person and :genre some \"female\"";
		try {
			mci = testOWL2.test(dlExpr, simpleValueFactory, prefixToNamsepace, false);
		} catch (ManchesterParserException e) {
			System.out.println("EXCEPTION: ");
			e.printStackTrace();
		}*/
		
		//right
		/*dlExpr = "(:Animal) ";
		try {
			mci = testOWL2.test(dlExpr, simpleValueFactory, prefixToNamsepace, false);
		} catch (ManchesterParserException e) {
			e.printStackTrace();
		}*/
		
		//right
		/*dlExpr = "( :Person )";
		try {
			mci = testOWL2.test(dlExpr, simpleValueFactory, prefixToNamsepace, false);
		} catch (ManchesterParserException e) {
			e.printStackTrace();
		}*/
		
		
		//inverse
		/*dlExpr = "inverse :regulate value :A";
		try {
			mci = testOWL2.test(dlExpr, simpleValueFactory, prefixToNamsepace, false);
		} catch (ManchesterParserException e) {
			e.printStackTrace();
		}*/
		
	}
	
	private ManchesterClassInterface test(String dlExpr, SimpleValueFactory simpleValueFactory, 
			Map <String, String>prefixToNamsepace, boolean useUpperCase) throws ManchesterParserException{
		ManchesterClassInterface mci = ManchesterSyntaxUtils.parseCompleteExpression(dlExpr, simpleValueFactory, prefixToNamsepace);
		System.out.println("manchester Expression: "+dlExpr);
		//System.out.println("print = "+mci.print(""));
		System.out.println("mancExp = "+mci.getManchExpr(useUpperCase));
		return mci;
	}
	
	/*public ManchesterClassInterface parseDLExpr(String mancExp){
		System.out.println("manchExp = "+mancExp); // da cancellare
		
		
		// Get our lexer
		ManchesterOWL2SyntaxParserLexer lexer = new ManchesterOWL2SyntaxParserLexer(CharStreams.fromString(mancExp));
		// Get a list of matched tokens
	    CommonTokenStream tokens = new CommonTokenStream(lexer);
	    // Pass the tokens to the parser
	    ManchesterOWL2SyntaxParserParser parser = new ManchesterOWL2SyntaxParserParser(tokens);
	    
	    DescriptionContext descriptionContext = parser.description();
	    
	    
	    // Walk it and attach our listener
	    Map prefixToNamsepace = new HashMap<String, String>();
	    //prefixToNamsepace.put("", "http://test.it/");
	    ParseTreeWalker walker = new ParseTreeWalker();
	    ParserDescription parserDescription = new ParserDescription(SimpleValueFactory.getInstance(), 
	    		prefixToNamsepace);
	    walker.walk(parserDescription, descriptionContext);
	    
	    return parserDescription.getManchesterClass();
	}*/

}