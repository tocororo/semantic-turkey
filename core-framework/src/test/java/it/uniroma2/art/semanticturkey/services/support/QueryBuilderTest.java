package it.uniroma2.art.semanticturkey.services.support;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STRequest;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.sparql.GraphPattern;
import it.uniroma2.art.semanticturkey.sparql.GraphPatternBuilder;
import it.uniroma2.art.semanticturkey.sparql.ProjectionElementBuilder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Test cases for {@link QueryBuilder}
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class QueryBuilderTest {
	public static STServiceContext NullSTServiceContext = new STServiceContext() {

		@Override
		public boolean hasContextParameter(String parameter) {
			return false;
		}
		
		@Override
		public String getContextParameter(String string) {
			return null;			
		}

		@Override
		public Resource getWGraph() {
			return null;
		}

		@Override
		public String getVersion() {
			return null;
		}

		@Override
		public String getLanguages() {
			return null;
		}

		@Override
		public String getSessionToken() {
			return null;
		}

		@Override
		public STRequest getRequest() {
			return null;
		}

		@Override
		public Resource[] getRGraphs() {
			return null;
		}

		@Override
		public ProjectConsumer getProjectConsumer() {
			return null;
		}

		@Override
		public Project getProject(int index) {
			return null;
		}

		@Override
		public Project getProject() {
			return null;
		}

		@Override
		public String getExtensionPathComponent() {
			return null;
		}
	};

	@Test
	public void testGraphPatternWithoutJavaPostProcessor() {
		QueryBuilder queryBuilder = new QueryBuilder(NullSTServiceContext,
			//@formatter:off
			"SELECT ?resource WHERE {\n" +
			"  ?resource a owl:Class . \n" +
			"}\n" +
			"GROUP BY ?resource\n"
			//@formatter:on
		);

		queryBuilder.process(new QueryBuilderProcessor() {

			@Override
			public Map<Value, Literal> processBindings(STServiceContext context, List<BindingSet> resultTable) {
				return null;
			}

			@Override
			public boolean introducesDuplicates() {
				return true;
			}

			@Override
			public GraphPattern getGraphPattern(STServiceContext context) {
				return GraphPatternBuilder.create().prefix(RDFS.NS).pattern("?resource rdfs:label ?label")
						.projection(Collections.singletonList(ProjectionElementBuilder.variable("label")))
						.graphPattern();
			}

			@Override
			public String getBindingVariable() {
				return "resource";
			}
		}, "resource", "attr_show");

		Repository repo = new SailRepository(new MemoryStore());
		repo.initialize();
		try (RepositoryConnection conn = repo.getConnection()) {
			ValueFactory vf = conn.getValueFactory();

			IRI personIRI = vf.createIRI("http://example.org/Person");
			conn.add(personIRI, RDF.TYPE, OWL.CLASS);
			conn.add(personIRI, RDFS.LABEL, vf.createLiteral("Person", "en"));
			conn.add(personIRI, RDFS.LABEL, vf.createLiteral("Persona", "it"));

			IRI animalIRI = vf.createIRI("http://example.org/Animal");
			conn.add(animalIRI, RDF.TYPE, OWL.CLASS);
			conn.add(animalIRI, RDFS.LABEL, vf.createLiteral("Animal", "en"));
			conn.add(animalIRI, RDFS.LABEL, vf.createLiteral("Animale", "it"));

			Collection<AnnotatedValue<Resource>> result = queryBuilder.runQuery(conn);

			assertThat(result,
					containsInAnyOrder(
							Matchers.both(hasProperty("value", equalTo(personIRI)))
									.and(Matchers
											.either(hasProperty("attributes",
													equalTo(Collections.singletonMap("show",
															vf.createLiteral("Person", "en")))))
											.or(hasProperty("attributes",
													equalTo(Collections.singletonMap("show",
															vf.createLiteral("Persona", "it")))))),
							Matchers.both(hasProperty("value", equalTo(animalIRI))).and(Matchers
									.either(hasProperty("attributes",
											equalTo(Collections.singletonMap("show",
													vf.createLiteral("Animal", "en")))))
									.or(hasProperty("attributes", equalTo(Collections.singletonMap("show",
											vf.createLiteral("Animale", "it"))))))));
		} finally {
			repo.shutDown();
		}
	}

	@Test
	public void testGraphPatternWithJavaPostProcessor() {
		QueryBuilder queryBuilder = new QueryBuilder(NullSTServiceContext,
		//@formatter:off
			"SELECT ?resource WHERE {\n" +
			"  ?resource a owl:Class . \n" +
			"}\n" +
			"GROUP BY ?resource\n"
			//@formatter:on
		);

		queryBuilder.process(new QueryBuilderProcessor() {

			@Override
			public Map<Value, Literal> processBindings(STServiceContext context, List<BindingSet> resultTable) {
				return resultTable.stream().collect(groupingBy((BindingSet bs) -> bs.getValue("resource"),
						Collectors.collectingAndThen(Collectors.toList(), list -> SimpleValueFactory
								.getInstance()
								.createLiteral(list.stream().map(bs -> (Literal) bs.getValue("label"))
										.sorted(Comparator.comparing((Literal l) -> l.getLanguage().get()))
										.map(Literal::getLabel).collect(joining(", "))))));
			}

			@Override
			public boolean introducesDuplicates() {
				return true;
			}

			@Override
			public GraphPattern getGraphPattern(STServiceContext context) {
				return GraphPatternBuilder.create().prefix(RDFS.NS).pattern("?resource rdfs:label ?label")
						.projection(Collections.singletonList(ProjectionElementBuilder.variable("label")))
						.graphPattern();
			}

			@Override
			public String getBindingVariable() {
				return "resource";
			}
		}, "resource", "attr_show");

		Repository repo = new SailRepository(new MemoryStore());
		repo.initialize();
		try (RepositoryConnection conn = repo.getConnection()) {
			ValueFactory vf = conn.getValueFactory();

			IRI personIRI = vf.createIRI("http://example.org/Person");
			conn.add(personIRI, RDF.TYPE, OWL.CLASS);
			conn.add(personIRI, RDFS.LABEL, vf.createLiteral("Person", "en"));
			conn.add(personIRI, RDFS.LABEL, vf.createLiteral("Persona", "it"));

			IRI animalIRI = vf.createIRI("http://example.org/Animal");
			conn.add(animalIRI, RDF.TYPE, OWL.CLASS);
			conn.add(animalIRI, RDFS.LABEL, vf.createLiteral("Animal", "en"));
			conn.add(animalIRI, RDFS.LABEL, vf.createLiteral("Animale", "it"));

			Collection<AnnotatedValue<Resource>> result = queryBuilder.runQuery(conn);

			assertThat(result,
					containsInAnyOrder(
							Matchers.both(hasProperty("value", equalTo(personIRI)))
									.and(hasProperty("attributes",
											equalTo(Collections.singletonMap("show",
													vf.createLiteral("Person, Persona"))))),
							Matchers.both(hasProperty("value", equalTo(animalIRI)))
									.and(hasProperty("attributes", equalTo(Collections.singletonMap("show",
											vf.createLiteral("Animal, Animale")))))));
		} finally {
			repo.shutDown();
		}
	}

	@Test
	public void testGraphPatternWithTwoAggregagationsAndJavaPostProcessor() {
		QueryBuilder queryBuilder = new QueryBuilder(NullSTServiceContext,
		//@formatter:off
			"SELECT ?resource WHERE {\n" +
			"  ?resource a owl:Class . \n" +
			"}\n" +
			"GROUP BY ?resource\n"
			//@formatter:on
		);

		queryBuilder.process(new QueryBuilderProcessor() {

			@Override
			public Map<Value, Literal> processBindings(STServiceContext context, List<BindingSet> resultTable) {
				return resultTable.stream()
						.collect(Collectors.toMap(bs -> bs.getValue(getBindingVariable()),
								bs -> (Literal) SimpleValueFactory.getInstance()
										.createLiteral(bs.getValue("notation").stringValue() + ":"
												+ bs.getValue("label").stringValue())));
			}

			@Override
			public boolean introducesDuplicates() {
				return true;
			}

			@Override
			public GraphPattern getGraphPattern(STServiceContext context) {
				return GraphPatternBuilder.create().prefix(RDFS.NS).prefix(SKOS.NS).pattern(
						"?resource rdfs:label ?labelInternal . OPTIONAL { ?resource skos:notation ?notationInternal }")
						.projection(
								Arrays.asList(ProjectionElementBuilder.groupConcat("labelInternal", "label"),
										ProjectionElementBuilder.groupConcat("notationInternal", "notation")))
						.graphPattern();
			}

			@Override
			public String getBindingVariable() {
				return "resource";
			}
		}, "resource", "attr_show");

		Repository repo = new SailRepository(new MemoryStore());
		repo.initialize();
		try (RepositoryConnection conn = repo.getConnection()) {
			ValueFactory vf = conn.getValueFactory();

			IRI personIRI = vf.createIRI("http://example.org/Person");
			conn.add(personIRI, RDF.TYPE, OWL.CLASS);
			conn.add(personIRI, RDFS.LABEL, vf.createLiteral("Person", "en"));
			conn.add(personIRI, SKOS.NOTATION, vf.createLiteral("P1"));

			IRI animalIRI = vf.createIRI("http://example.org/Animal");
			conn.add(animalIRI, RDF.TYPE, OWL.CLASS);
			conn.add(animalIRI, RDFS.LABEL, vf.createLiteral("Animal", "en"));
			conn.add(animalIRI, SKOS.NOTATION, vf.createLiteral("A1"));

			Collection<AnnotatedValue<Resource>> result = queryBuilder.runQuery(conn);

			assertThat(result,
					containsInAnyOrder(
							Matchers.both(hasProperty("value", equalTo(personIRI)))
									.and(hasProperty("attributes",
											equalTo(Collections.singletonMap("show",
													vf.createLiteral("P1:Person"))))),
							Matchers.both(hasProperty("value", equalTo(animalIRI)))
									.and(hasProperty("attributes", equalTo(Collections.singletonMap("show",
											vf.createLiteral("A1:Animal")))))));
		} finally {
			repo.shutDown();
		}
	}

}