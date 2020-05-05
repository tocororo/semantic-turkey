package it.uniroma2.art.semanticturkey.changetracking.sail;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.Iterators;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import static org.hamcrest.Matchers.hasSize;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.BLACKLIST;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

/**
 * Test class for {@link ChangeTracker} focusing on blacklisting.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@RunWith(JUnitParamsRunner.class)
public class ChangeTrackerBlacklistingTest extends AbstractChangeTrackerTest {

	@Test
	@Parameters(method = "testBlacklistingArguments")
	@RequiresValidation(blacklisting = true)
	public void testBlacklisting(Literal comment) {
		try (RepositoryConnection conn = coreRepo.getConnection()) {
			conn.begin();
			conn.add(FOAF.PERSON, SKOS.PREF_LABEL, conn.getValueFactory().createLiteral("person", "en"),
					graphA);

			Update addCommitMetadata = conn.prepareUpdate(
			//@formatter:off
				"INSERT DATA {\n" +
				"  GRAPH <" + CHANGETRACKER.COMMIT_METADATA + "> {\n" +
				"    <" + CHANGETRACKER.COMMIT_METADATA + "> <http://semanticturkey.uniroma2.it/ns/st-changelog#parameters> [] ; \n" +
				"      <" + BLACKLIST.TEMPLATE + "> [\n" +
				"        <" + BLACKLIST.TEMPLATE_TYPE + "> <" + BLACKLIST.BLACKLISTED_TERM + "> ;\n" +
				"        <" + BLACKLIST.CONSTANT_BINDING + "> (<" + BLACKLIST.LABEL + "> \"person\"@en)\n" +
				"        ] .\n" +
				"  }\n" +
				"}"
				//@formatter:on
			);
			addCommitMetadata.execute();
			conn.commit();
		}

		Set<Resource> appendedForValidation = Repositories.get(supportRepo, (conn) -> {
			return QueryResults
					.asModel(conn.getStatements(null, RDF.TYPE, CHANGELOG.COMMIT, VALIDATION_GRAPH))
					.subjects();
		});

		assertThat(appendedForValidation, hasSize(1));

		Resource commit = appendedForValidation.iterator().next();

		try (RepositoryConnection conn = coreRepo.getConnection()) {
			conn.begin();
			conn.add(CHANGETRACKER.VALIDATION, CHANGETRACKER.REJECT, commit, CHANGETRACKER.VALIDATION);
			if (comment != null) {
				conn.add(CHANGETRACKER.VALIDATION, RDFS.COMMENT, comment, CHANGETRACKER.VALIDATION);
			}
			conn.commit();
		}

		try (RepositoryConnection conn = supportRepo.getConnection()) {

			assertTrue(conn.size(BLACKLIST_GRAPH) != 0);
			assertTrue(conn.size(VALIDATION_GRAPH) == 0);

			if (comment != null) {
				assertTrue(conn.hasStatement(null, RDFS.COMMENT, comment, false, BLACKLIST_GRAPH));
			}
		}

	}

	public static Object[] testBlacklistingArguments() {
		return new Object[] { new Object[] { null },
				new Object[] { SimpleValueFactory.getInstance().createLiteral("Hello, world", "en") } };
	}
}
