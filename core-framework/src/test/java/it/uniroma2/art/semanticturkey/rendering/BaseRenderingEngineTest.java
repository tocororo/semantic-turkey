package it.uniroma2.art.semanticturkey.rendering;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import it.uniroma2.art.semanticturkey.exceptions.SearchStatusException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import com.google.common.collect.ImmutableMap;

import static org.junit.Assert.assertThat;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.exceptions.DuplicatedResourceException;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectCreationException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.ReservedPropertyUpdateException;
import it.uniroma2.art.semanticturkey.exceptions.UnsupportedLexicalizationModelException;
import it.uniroma2.art.semanticturkey.exceptions.UnsupportedModelException;
import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.NonConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.extpts.repositoryimplconfigurer.RepositoryImplConfigurer;
import it.uniroma2.art.semanticturkey.extension.extpts.search.SearchStrategy;
import it.uniroma2.art.semanticturkey.extension.extpts.urigen.URIGenerationException;
import it.uniroma2.art.semanticturkey.extension.extpts.urigen.URIGenerator;
import it.uniroma2.art.semanticturkey.extension.impl.ExtensionPointManagerImpl;
import it.uniroma2.art.semanticturkey.extension.impl.rendering.AbstractLabelBasedRenderingEngineConfiguration;
import it.uniroma2.art.semanticturkey.extension.impl.rendering.BaseRenderingEngine;
import it.uniroma2.art.semanticturkey.extension.impl.rendering.VariableDefinition;
import it.uniroma2.art.semanticturkey.ontology.STEnviroment;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.project.CreateLocal;
import it.uniroma2.art.semanticturkey.project.ForbiddenProjectAccessException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.Pair;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.TripleForSearch;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.search.SearchMode;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STRequest;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.validation.ValidationUtilities;

public class BaseRenderingEngineTest {

	public static final String TEST_REPOSTORY_IMPL_CONFIGURER = "org.example.TestRepositoryImplConfigurer";
	public static final String REGEX_SEARCH_STRATEGY = "it.uniroma2.art.semanticturkey.extension.impl.search.regex.RegexSearchStrategy";
	public static final String TEST_RENDERING_ENGINE = "org.example.TestRenderingEngine";
	public static final String TEST_URI_GENERATOR = "org.example.TestURIGenerator";

	@Rule
	public STEnviroment stEnv = new STEnviroment(false);

	public static class TestRenderingEngineConfiguration
			extends AbstractLabelBasedRenderingEngineConfiguration {

		@Override
		public String getShortName() {
			return "Test rendering engine";
		}
	}

	public static class TestRenderingEngine extends BaseRenderingEngine {

		public TestRenderingEngine(TestRenderingEngineConfiguration conf) {
			super(conf);
		}

		@Override
		public void getGraphPatternInternal(StringBuilder gp) {
			gp.append("\n?resource <http://www.w3.org/2004/02/skos/core#prefLabel> ?labelInternal .\n");
		}
	}

	public static class TestRenderingEngineFactory
			implements ConfigurableExtensionFactory<TestRenderingEngine, TestRenderingEngineConfiguration> {

		@Override
		public String getId() {
			return TEST_RENDERING_ENGINE;
		}

		@Override
		public String getName() {
			return "Test rendering engine";
		}

		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public TestRenderingEngine createInstance(TestRenderingEngineConfiguration conf)
				throws InvalidConfigurationException {
			return new TestRenderingEngine(conf);
		}

		@Override
		public Collection<TestRenderingEngineConfiguration> getConfigurations() {
			return Arrays.asList(new TestRenderingEngineConfiguration());
		}

	}

	@Before
	public void tearUp() throws RDFParseException, RepositoryException, IOException, ProjectAccessException {
		SecurityContextImpl securityContext = new SecurityContextImpl();
		securityContext.setAuthentication(
				new TestingAuthenticationToken(new STUser("admin@vocbench.com", "admin", null, null), null));
		SecurityContextHolder.setContext(securityContext);

		// initializes custom forms
		(new CustomFormManager()).init();

		// loads PU bindings
		ProjectUserBindingsManager.loadPUBindings();

		ProjectManager.setExtensionPointManager(new ExtensionPointManagerImpl() {
			@Override
			public ExtensionFactory<?> getExtension(String componentID) throws NoSuchExtensionException {
				switch (componentID) {
				case TEST_REPOSTORY_IMPL_CONFIGURER:
					return new NonConfigurableExtensionFactory<RepositoryImplConfigurer>() {

						@Override
						public String getName() {
							return "Test Repository Impl Configurer";
						}

						@Override
						public String getDescription() {
							return "";
						}

						@Override
						public RepositoryImplConfigurer createInstance() {
							return new RepositoryImplConfigurer() {

								@Override
								public RepositoryImplConfig buildRepositoryImplConfig(
										Function<SailImplConfig, SailImplConfig> backendDecorator) {
									SailImplConfig memStore = new MemoryStoreConfig();
									SailImplConfig sailConfig;
									if (backendDecorator != null) {
										sailConfig = backendDecorator.apply(memStore);
									} else {
										sailConfig = memStore;
									}
									return new SailRepositoryConfig(sailConfig);
								}
							};
						}

					};
				case REGEX_SEARCH_STRATEGY:
					return new NonConfigurableExtensionFactory<SearchStrategy>() {
						@Override
						public String getName() {
							return "Regex search strategy stub";
						}

						public String getDescription() {
							return "";
						}

						public SearchStrategy createInstance() {
							return new SearchStrategy() {

								@Override
								public void initialize(RepositoryConnection connection, boolean forceCreation) throws Exception {
									// nothing to do
								}

								@Override
								public void update(RepositoryConnection connection) throws Exception {
									// nothing to do
								}

								@Override
								public String searchResource(STServiceContext stServiceContext,
										String searchString, String[] rolesArray, boolean useLexicalizations,
										boolean useLocalName, boolean useURI, boolean useNotes,
										SearchMode searchMode, List<IRI> schemes, String schemeFilter,
										List<String> langs, boolean includeLocales, IRI lexModel,
										boolean searchInRDFSLabel, boolean searchInSKOSLabel,
										boolean searchInSKOSXLLabel, boolean searchInOntolex,
										Map<String, String> prefixToNamespaceMap)
										throws IllegalStateException, STPropertyAccessException {
									throw new UnsupportedOperationException("Not implemented by the stub");
								}

								@Override
								public Collection<String> searchStringList(STServiceContext stServiceContext,
										String searchString, String[] rolesArray, boolean useLocalName,
										SearchMode searchMode, List<IRI> schemes, String schemeFilter,
										List<String> langs, IRI cls, boolean includeLocales)
										throws IllegalStateException, STPropertyAccessException {
									throw new UnsupportedOperationException("Not implemented by the stub");
								}

								@Override
								public Collection<String> searchURIList(STServiceContext stServiceContext,
										String searchString, String[] rolesArray, SearchMode searchMode,
										List<IRI> schemes, String schemeFilter, IRI cls,
										Map<String, String> prefixToNamespaceMap, int maxNumResults)
										throws IllegalStateException, STPropertyAccessException {
									throw new UnsupportedOperationException("Not implemented by the stub");
								}

								@Override
								public String searchInstancesOfClass(STServiceContext stServiceContext,
										List<List<IRI>> clsListList, String searchString,
										boolean useLexicalizations, boolean useLocalName, boolean useURI,
										boolean useNotes, SearchMode searchMode, List<String> langs,
										boolean includeLocales, boolean searchStringCanBeNull,
										boolean searchInSubTypes, IRI lexModel, boolean searchInRDFSLabel,
										boolean searchInSKOSLabel, boolean searchInSKOSXLLabel,
										boolean searchInOntolex, List<List<IRI>> schemes,
										StatusFilter statusFilter, List<Pair<IRI, List<Value>>> outgoingLinks,
										List<TripleForSearch<IRI, String, SearchMode>> outgoingSearch,
										List<Pair<IRI, List<Value>>> ingoingLinks,
										SearchStrategy searchStrategy, String baseURI,
										Map<String, String> prefixToNamespaceMap)
										throws IllegalStateException {
									throw new UnsupportedOperationException("Not implemented by the stub");
								}

								@Override
								public String searchSpecificModePrepareQuery(String variable, String value,
										SearchMode searchMode, String indexToUse, List<String> langs,
										boolean includeLocales, boolean forLocalName) {
									// TODO Auto-generated method stub
									return null;
								}

								@Override
								public String searchLexicalEntry(STServiceContext stServiceContext,
										String searchString, boolean useLexicalizations, boolean useLocalName,
										boolean useURI, boolean useNotes, SearchMode searchMode,
										List<IRI> lexicons, List<String> langs, boolean includeLocales,
										IRI iri, boolean searchInRDFSLabel, boolean searchInSKOSLabel,
										boolean searchInSKOSXLLabel, boolean searchInOntolex,
										Map<String, String> prefixToNamespaceMap)
										throws IllegalStateException {
									throw new UnsupportedOperationException("Not implemented by the stub");
								}

								@Override
								public boolean isSearchPossible(RepositoryConnection connection, boolean throwExceptionIfNotSearchNotPossible) throws SearchStatusException {
									return true;
								}

							};
						}
					};
				case TEST_RENDERING_ENGINE:
					return new TestRenderingEngineFactory();
				case TEST_URI_GENERATOR:
					return new NonConfigurableExtensionFactory<URIGenerator>() {

						@Override
						public String getName() {
							return "Test URI Geneator";
						}

						@Override
						public String getDescription() {
							return null;
						}

						@Override
						public URIGenerator createInstance() {
							return new URIGenerator() {

								@Override
								public IRI generateIRI(STServiceContext stServiceContext, String xRole,
										Map<String, Value> args) throws URIGenerationException {
									return SimpleValueFactory.getInstance().createIRI(
											stServiceContext.getProject().getBaseURI() + UUID.randomUUID());
								}

							};
						}
					};
				default:
					throw new IllegalArgumentException("Unsupported extension: " + componentID);
				}
			}
		});

	}

	@Test
	public void testBaseRenderingEngine() throws ClassNotFoundException, InvalidProjectNameException,
			ProjectInexistentException, ProjectAccessException, ForbiddenProjectAccessException,
			DuplicatedResourceException, ProjectCreationException, WrongPropertiesException, RBACException,
			UnsupportedModelException, UnsupportedLexicalizationModelException, ProjectInconsistentException,
			InvalidConfigurationException, STPropertyAccessException, IOException,
			ReservedPropertyUpdateException, ProjectUpdateException, STPropertyUpdateException,
			NoSuchConfigurationManager {

		TestRenderingEngineConfiguration rendEngConf = new TestRenderingEngineConfiguration();
		rendEngConf.template = "(\\(${notation}\\) )?${show}";
		VariableDefinition notationVar = new VariableDefinition();
		notationVar.propertyPath = Arrays.asList(
				SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2004/02/skos/core#notation"));
		rendEngConf.variables = ImmutableMap.of("notation", notationVar);
		Project project = ProjectManager.createProject(ProjectConsumer.SYSTEM, "Test", null, Project.SKOS_MODEL,
				Project.SKOS_MODEL, "http://example.org/", false, false, false, new CreateLocal(), "core",
				new PluginSpecification(TEST_REPOSTORY_IMPL_CONFIGURER, null, null, null), null, null, null,
				null, new PluginSpecification(TEST_URI_GENERATOR, null, null, null),
				new PluginSpecification(TEST_RENDERING_ENGINE, null, null,
						STPropertiesManager.storeSTPropertiesToObjectNode(rendEngConf, true)),
				null, null, null, null, null, null, null, false, null, false, false, false, false);
		try {
			Repository repo = new SailRepository(new MemoryStore());
			repo.init();

			STServiceContext stServiceContext = new STServiceContext() {

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
					return project;
				}

				@Override
				public String getExtensionPathComponent() {
					return null;
				}
			};

			QueryBuilder queryBuilder = new QueryBuilder(stServiceContext,
			//@formatter:off
				"PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
				"SELECT ?resource WHERE {\n" +
				"  ?resource a skos:Concept . \n" +
				"}\n" +
				"GROUP BY ?resource\n"
				//@formatter:on
			);

			queryBuilder.processRendering();

			try (RepositoryConnection conn = repo.getConnection()) {
				ValueFactory vf = conn.getValueFactory();

				IRI g = vf.createIRI("http://example.org/");
				IRI personIRI = vf.createIRI("http://example.org/Person");
				conn.add(personIRI, RDF.TYPE, SKOS.CONCEPT, g);
				conn.add(personIRI, SKOS.PREF_LABEL, vf.createLiteral("Person", "en"), g);
				conn.add(personIRI, SKOS.PREF_LABEL, vf.createLiteral("Persona", "it"), g);
				conn.add(personIRI, SKOS.NOTATION, vf.createLiteral("P1"), g);

				IRI animalIRI = vf.createIRI("http://example.org/Animal");
				conn.add(animalIRI, RDF.TYPE, SKOS.CONCEPT, g);
				conn.add(animalIRI, SKOS.PREF_LABEL, vf.createLiteral("Animal", "en"), g);
				conn.add(animalIRI, SKOS.PREF_LABEL, vf.createLiteral("Animale", "it"), g);
				// the notation below had been omitted to test optional nested templates
				// conn.add(animalIRI, SKOS.NOTATION, vf.createLiteral("A1"));

				Collection<AnnotatedValue<Resource>> result = queryBuilder.runQuery(conn);

				assertThat(result, containsInAnyOrder(
						Matchers.both(hasProperty("value", equalTo(personIRI)))
								.and(hasProperty("attributes",
										equalTo(Collections.singletonMap("show",
												vf.createLiteral("(P1) Person (en), Persona (it)"))))),
						Matchers.both(hasProperty("value", equalTo(animalIRI)))
								.and(hasProperty("attributes", equalTo(Collections.singletonMap("show",
										vf.createLiteral("Animal (en), Animale (it)")))))));
			} finally {
				repo.shutDown();
			}
		} finally {
			ProjectManager.disconnectFromProject(ProjectConsumer.SYSTEM, project.getName());
		}

	}

	@Test
	public void testBaseRenderingEngineWithValidation()
			throws ClassNotFoundException, InvalidProjectNameException, ProjectInexistentException,
			ProjectAccessException, ForbiddenProjectAccessException, DuplicatedResourceException,
			ProjectCreationException, WrongPropertiesException, RBACException,
			UnsupportedModelException, UnsupportedLexicalizationModelException, ProjectInconsistentException,
			InvalidConfigurationException, STPropertyAccessException, IOException,
			ReservedPropertyUpdateException, ProjectUpdateException, STPropertyUpdateException,
			NoSuchConfigurationManager {

		TestRenderingEngineConfiguration rendEngConf = new TestRenderingEngineConfiguration();
		rendEngConf.template = "(\\(${notation}\\) )?${show}";
		VariableDefinition notationVar = new VariableDefinition();
		notationVar.propertyPath = Arrays.asList(
				SimpleValueFactory.getInstance().createIRI("http://www.w3.org/2004/02/skos/core#notation"));
		rendEngConf.variables = ImmutableMap.of("notation", notationVar);
		Project project = ProjectManager.createProject(ProjectConsumer.SYSTEM, "Test", null, Project.SKOS_MODEL,
				Project.SKOS_MODEL, "http://example.org/", false, true, false, new CreateLocal(), "core",
				new PluginSpecification(TEST_REPOSTORY_IMPL_CONFIGURER, null, null, null), null, "support",
				new PluginSpecification(TEST_REPOSTORY_IMPL_CONFIGURER, null, null, null), null,
				new PluginSpecification(TEST_URI_GENERATOR, null, null, null),
				new PluginSpecification(TEST_RENDERING_ENGINE, null, null,
						STPropertiesManager.storeSTPropertiesToObjectNode(rendEngConf, true)),
				null, null, null, null, null, null, null, false, null, false, false, false, false);
		try {
			STServiceContext stServiceContext = new STServiceContext() {

				@Override
				public boolean hasContextParameter(String parameter) {
					return false;
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
					return project;
				}

				@Override
				public String getExtensionPathComponent() {
					return null;
				}

				@Override
				public String getContextParameter(String string) {
					return null;
				}
			};

			QueryBuilder queryBuilder = new QueryBuilder(stServiceContext,
			//@formatter:off
				"PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
				"SELECT ?resource WHERE {\n" +
				"  ?resource a skos:Concept . \n" +
				"}\n" +
				"GROUP BY ?resource\n"
				//@formatter:on
			);

			queryBuilder.processRendering();
			RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(project.getRepository());
			try {
				ValueFactory vf = conn.getValueFactory();

				IRI g = vf.createIRI("http://example.org/");
				IRI personIRI = vf.createIRI("http://example.org/Person");
				IRI animalIRI = vf.createIRI("http://example.org/Animal");

				ValidationUtilities.executeWithoutValidation(true, conn, _conn -> {

					conn.add(personIRI, RDF.TYPE, SKOS.CONCEPT, g);
					conn.add(personIRI, SKOS.PREF_LABEL, vf.createLiteral("Person", "en"), g);
					conn.add(personIRI, SKOS.PREF_LABEL, vf.createLiteral("Persona", "it"), g);
					conn.add(personIRI, SKOS.NOTATION, vf.createLiteral("P1"), g);

					conn.add(animalIRI, RDF.TYPE, SKOS.CONCEPT, g);
					conn.add(animalIRI, SKOS.PREF_LABEL, vf.createLiteral("Animal", "en"), g);
					conn.add(animalIRI, SKOS.PREF_LABEL, vf.createLiteral("Animale", "it"), g);
					// the notation below had been omitted to test optional nested templates
					// conn.add(animalIRI, SKOS.NOTATION, vf.createLiteral("A1"));

				});

				conn.remove(personIRI, SKOS.NOTATION, null, g);
				conn.add(personIRI, SKOS.NOTATION, vf.createLiteral("P,1"), g);

				Collection<AnnotatedValue<Resource>> result = queryBuilder.runQuery(conn);

				assertThat(result, containsInAnyOrder(
						Matchers.both(hasProperty("value", equalTo(personIRI)))
								.and(hasProperty("attributes",
										equalTo(Collections.singletonMap("show",
												vf.createLiteral("(P,1) Person (en), Persona (it)"))))),
						Matchers.both(hasProperty("value", equalTo(animalIRI)))
								.and(hasProperty("attributes", equalTo(Collections.singletonMap("show",
										vf.createLiteral("Animal (en), Animale (it)")))))));
			} finally {
				RDF4JRepositoryUtils.releaseConnection(conn, project.getRepository());
			}
		} finally {
			ProjectManager.disconnectFromProject(ProjectConsumer.SYSTEM, project.getName());
		}

	}

}
