package it.uniroma2.art.semanticturkey.data.nature;

import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.VALIDATION;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class NatureRecognitionOrchestratorTest {

	@Test
	@Parameters(method = "computeTripleScopeProvider")
	public void testComputeTripleScopeFromGraphs(List<IRI> graphs, IRI workingGraph,
			TripleScopes expectedScope) {

		TripleScopes actualScope = NatureRecognitionOrchestrator.computeTripleScopeFromGraphs(graphs,
				workingGraph);

		assertEquals(expectedScope, actualScope);

	}

	Object[] computeTripleScopeProvider() {
		IRI graphA = SimpleValueFactory.getInstance().createIRI("http://graphA");
		IRI addGraphA = (IRI) VALIDATION.stagingAddGraph(graphA);
		IRI removeGraphA = (IRI) VALIDATION.stagingRemoveGraph(graphA);

		IRI graphB = SimpleValueFactory.getInstance().createIRI("http://graphB");
		IRI addGraphB = (IRI) VALIDATION.stagingAddGraph(graphB);
		IRI removeGraphB = (IRI) VALIDATION.stagingRemoveGraph(graphB);

		IRI graphC = SimpleValueFactory.getInstance().createIRI("http://graphC");
		IRI addGraphC = (IRI) VALIDATION.stagingAddGraph(graphC);
		IRI removeGraphC = (IRI) VALIDATION.stagingRemoveGraph(graphC);

		// @formatter:off
		return new Object[] {
				new Object[] { Arrays.asList(graphA), graphA, TripleScopes.local },
				new Object[] { Arrays.asList(addGraphA), graphA, TripleScopes.staged },
				new Object[] { Arrays.asList(graphA, removeGraphA), graphA, TripleScopes.del_staged },
				new Object[] { Arrays.asList(graphB), graphA, TripleScopes.imported },
				new Object[] { Arrays.asList(NatureRecognitionOrchestrator.INFERENCE_GRAPH), graphA, TripleScopes.inferred },
				new Object[] { Arrays.asList(graphA, graphB), graphA, TripleScopes.local },
				new Object[] { Arrays.asList(graphA, addGraphB), graphA, TripleScopes.local },
				new Object[] { Arrays.asList(graphA, removeGraphB), graphA, TripleScopes.local },
				new Object[] { Arrays.asList(addGraphB), graphA, TripleScopes.staged },
				new Object[] { Arrays.asList(graphB, removeGraphB), graphA, TripleScopes.del_staged },
				// in the following two cases, the triple is both staged for addition and removal. Prefer staged for deletion
				new Object[] { Arrays.asList(graphB, removeGraphB, addGraphC), graphA, TripleScopes.del_staged },
				new Object[] { Arrays.asList(addGraphB, graphC, removeGraphC), graphA, TripleScopes.del_staged }		
		};
		// @formatter:on
	}

}
