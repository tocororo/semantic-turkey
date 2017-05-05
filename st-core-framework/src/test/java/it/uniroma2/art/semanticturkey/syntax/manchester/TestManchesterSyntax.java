package it.uniroma2.art.semanticturkey.syntax.manchester;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.RecognitionException;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import it.uniroma2.art.semanticturkey.exceptions.ManchesterParserException;
import it.uniroma2.art.semanticturkey.exceptions.NotClassAxiomException;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterClassInterface;
import it.uniroma2.art.semanticturkey.syntax.manchester.owl2.ManchesterSyntaxUtils;

//this class is not a real junit test, it is used just to understand how to use the Manchester syntax 
// utilities and data structures

public class TestManchesterSyntax {

	public static void main(String[] args) {

		TestManchesterSyntax testManchesterSyntax = new TestManchesterSyntax();

		ValueFactory valueFactory = testManchesterSyntax.initialize();

		try {
			testManchesterSyntax.startReadTest(valueFactory);
			
			RepositoryConnection conn = testManchesterSyntax.initializeForWrite();
			
			//testManchesterSyntax.startWriteTest(conn);
			
		} catch (RecognitionException e) {
			e.printStackTrace();
		} catch (ManchesterParserException e) {
			e.printStackTrace();
		}

	}

	private ValueFactory initialize() {

		MemoryStore memStore = new MemoryStore();
		memStore.setPersist(false);
		Repository repository = new SailRepository(memStore);
		repository.initialize();

		// String baseDir = "localRepforTest";
		// LocalRepositoryManager localRep = new LocalRepositoryManager(new File(baseDir));
		// localRep.initialize();

		// create a configuration for the SAIL stack
		// MemoryStoreConfig backendConfig = new MemoryStoreConfig();
		// backendConfig.setPersist(true);

		// create a configuration for the repository implementation
		// RepositoryImplConfig repositoryTypeSpec = new SailRepositoryConfig(backendConfig);

		// String repositoryId = "firstRep";
		// RepositoryConfig repConfig = new RepositoryConfig(repositoryId, repositoryTypeSpec);
		// localRep.addRepositoryConfig(repConfig);

		// Repository repository = localRep.getRepository(repositoryId);
		// repository.initialize();

		return repository.getValueFactory();

	}
	
	private RepositoryConnection initializeForWrite(){
		MemoryStore memStore = new MemoryStore();
		memStore.setPersist(false);
		Repository repository = new SailRepository(memStore);
		repository.initialize();
		return repository.getConnection();
	}

	private void startReadTest(ValueFactory valueFactory)
			throws RecognitionException, ManchesterParserException {

		ManchesterClassInterface manchesterClassInterface;

		String class1uri = "<http://test.it#class1>";
		String class2uri = "<http://test.it#class2>";
		String class3uri = "<http://test.it#class3>";
		String class4uri = "<http://test.it#class4>";
		String class1qname = "try:class1";
		String class2qname = ":class2";

		String prop1uri = "<http://test.it#prop1>";
		String prop2uri = "<http://test.it#prop2>";

		String valueLiteral = "\"Mario\"@it";

		Map<String, String> prefixtoNamespaceMap = new HashMap<>();
		prefixtoNamespaceMap.put("my", "http://test.it");

		String inputAssertion = class1uri + " and " + class2uri;
		manchesterClassInterface = ManchesterSyntaxUtils.parseCompleteExpression(inputAssertion, valueFactory, prefixtoNamespaceMap);
		printResults(inputAssertion, manchesterClassInterface);

		inputAssertion = prop1uri + "value " + valueLiteral;
		manchesterClassInterface = ManchesterSyntaxUtils.parseCompleteExpression(inputAssertion, valueFactory, prefixtoNamespaceMap);
		printResults(inputAssertion, manchesterClassInterface);

	}

	
	
	private void startWriteTest(RepositoryConnection conn) throws RecognitionException, ManchesterParserException, NotClassAxiomException {
		ManchesterClassInterface manchesterClassInterface;

		Resource[] graphs = new Resource[] { conn.getValueFactory().createIRI("http://maingraph.it") };
		
		String class1uri = "<http://test.it#class1>";
		String class2uri = "<http://test.it#class2>";
		String class3uri = "<http://test.it#class3>";
		String class4uri = "<http://test.it#class4>";
		String class1qname = "try:class1";
		String class2qname = ":class2";

		String prop1uri = "<http://test.it#prop1>";
		String prop2uri = "<http://test.it#prop2>";

		String valueLiteral = "\"Mario\"@it";
		
		Map<String, String> prefixtoNamespaceMap = new HashMap<>();
		prefixtoNamespaceMap.put("my", "http://test.it");

		String inputAssertion = class1uri + " and " + class2uri;
		manchesterClassInterface = ManchesterSyntaxUtils.parseCompleteExpression(inputAssertion, conn.getValueFactory(), prefixtoNamespaceMap);
		
		
		
		List<Statement> statList = new ArrayList<Statement>();
		BNode bnode = (BNode) ManchesterSyntaxUtils.parseManchesterExpr(manchesterClassInterface, statList, 
				conn.getValueFactory());
		
		addAllStatements(conn, statList, graphs);
		statList.clear();
		
		String outputString = ManchesterSyntaxUtils.getManchExprFromBNode(bnode, graphs, statList, true, conn);
		printResults(inputAssertion, outputString, manchesterClassInterface);
		
	}

	private void printResults(String inputString, String outputString, 
			ManchesterClassInterface manchesterClassInterface){
		System.out.println("\n");
		System.out.println("input assertion: " + inputString);
		System.out.println("parsed assertion: " + manchesterClassInterface.getManchExpr(true));
		if(outputString != null){
			System.out.println("output assertion: " + outputString);
		}
	}
	
	private void printResults(String inputString, ManchesterClassInterface manchesterClassInterface) {
		printResults(inputString, null, manchesterClassInterface);
	}
	
	private void addAllStatements(RepositoryConnection conn, List<Statement> statList, Resource... graphs){
		for(Statement statement : statList){
			conn.add(statement, graphs);
		}
	}

}
