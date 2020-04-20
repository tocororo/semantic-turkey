package it.uniroma2.art.semanticturkey.rendering;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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
import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.impl.ExtensionPointManagerImpl;
import it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.PredefinedRepositoryImplConfigurer;
import it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.PredefinedRepositoryImplConfigurerFactory;
import it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined.RDF4JPersistentInMemorySailConfigurerConfiguration;
import it.uniroma2.art.semanticturkey.extension.impl.search.regex.RegexSearchStrategy;
import it.uniroma2.art.semanticturkey.extension.impl.search.regex.RegexSearchStrategyFactory;
import it.uniroma2.art.semanticturkey.ontology.STEnviroment;
import it.uniroma2.art.semanticturkey.plugin.PluginManager;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnloadablePluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.configuration.UnsupportedPluginConfigurationException;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.SKOSRenderingEngineFactory;
import it.uniroma2.art.semanticturkey.plugin.impls.rendering.conf.SKOSRenderingEngineConfiguration;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.NativeTemplateBasedURIGeneratorFactory;
import it.uniroma2.art.semanticturkey.plugin.impls.urigen.conf.NativeTemplateBasedURIGeneratorConfiguration;
import it.uniroma2.art.semanticturkey.project.CreateLocal;
import it.uniroma2.art.semanticturkey.project.ForbiddenProjectAccessException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.rbac.RBACException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STRequest;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import it.uniroma2.art.semanticturkey.user.ProjectUserBindingsManager;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.validation.ValidationUtilities;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class BaseRenderingEngineTest {

	@Rule
	public STEnviroment stEnv = new STEnviroment(false);

	@Before
	public void tearUp() {
		SecurityContextImpl securityContext = new SecurityContextImpl();
		securityContext.setAuthentication(
				new TestingAuthenticationToken(new STUser("admin@vocbench.com", "admin", null, null), null));
		SecurityContextHolder.setContext(securityContext);
	}

	@Test
	public void testBaseRenderingEngine() throws ClassNotFoundException, InvalidProjectNameException,
			ProjectInexistentException, ProjectAccessException, ForbiddenProjectAccessException,
			DuplicatedResourceException, ProjectCreationException, UnsupportedPluginConfigurationException,
			UnloadablePluginConfigurationException, WrongPropertiesException,
			RBACException, UnsupportedModelException, UnsupportedLexicalizationModelException,
			ProjectInconsistentException, InvalidConfigurationException, STPropertyAccessException,
			IOException, ReservedPropertyUpdateException, ProjectUpdateException {

		// initializes custom forms
		(new CustomFormManager()).init();

		// loads PU bindings
		ProjectUserBindingsManager.loadPUBindings();
		PluginManager.setDirectAccessTest(true);
		PluginManager.setTestPluginFactoryImpls(Arrays.asList(new NativeTemplateBasedURIGeneratorFactory(),
				new SKOSRenderingEngineFactory()));

		ProjectManager.setExtensionPointManager(new ExtensionPointManagerImpl() {
			@Override
			public ExtensionFactory<?> getExtension(String componentID) throws NoSuchExtensionException {
				if (componentID.equals(PredefinedRepositoryImplConfigurer.class.getName())) {
					return new PredefinedRepositoryImplConfigurerFactory();
				} else if (componentID.equals(RegexSearchStrategy.class.getName())) {
					return new RegexSearchStrategyFactory();
				} else {
					throw new IllegalArgumentException("Unsupported extension: " + componentID);
				}
			}
		});
		Properties renderingEngineConfiguration = new Properties();
		renderingEngineConfiguration.setProperty("template", "(\\(${notation}\\) )?${show}");
		renderingEngineConfiguration.setProperty("variables",
				"{\"notation\" : {\"propertyPath\" : [\"<http://www.w3.org/2004/02/skos/core#notation>\"]}}");

		Project project = ProjectManager.createProject(ProjectConsumer.SYSTEM, "Test", Project.SKOS_MODEL,
				Project.SKOS_MODEL, "http://example.org/", false, false, false, new CreateLocal(), "core",
				new PluginSpecification(PredefinedRepositoryImplConfigurer.class.getName(),
						RDF4JPersistentInMemorySailConfigurerConfiguration.class.getName(), null,
						JsonNodeFactory.instance.objectNode()),
				null, null, null, null,
				new PluginSpecification(NativeTemplateBasedURIGeneratorFactory.class.getName(),
						NativeTemplateBasedURIGeneratorConfiguration.class.getName(), null,
						JsonNodeFactory.instance.objectNode()),
				new PluginSpecification(SKOSRenderingEngineFactory.class.getName(),
						SKOSRenderingEngineConfiguration.class.getName(), renderingEngineConfiguration,
						JsonNodeFactory.instance.objectNode()),
				null, null, new String[] { "resource" }, null, null, null, null, null, null, false, null,false);
		try {
			Repository repo = new SailRepository(new MemoryStore());
			repo.initialize();

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
			ProjectCreationException, UnsupportedPluginConfigurationException,
			UnloadablePluginConfigurationException, WrongPropertiesException,
			RBACException, UnsupportedModelException, UnsupportedLexicalizationModelException,
			ProjectInconsistentException, InvalidConfigurationException, STPropertyAccessException,
			IOException, ReservedPropertyUpdateException, ProjectUpdateException {

		// initializes custom forms
		(new CustomFormManager()).init();

		// loads PU bindings
		ProjectUserBindingsManager.loadPUBindings();
		PluginManager.setDirectAccessTest(true);
		PluginManager.setTestPluginFactoryImpls(Arrays.asList(new NativeTemplateBasedURIGeneratorFactory(),
				new SKOSRenderingEngineFactory()));

		ProjectManager.setExtensionPointManager(new ExtensionPointManagerImpl() {
			@Override
			public ExtensionFactory<?> getExtension(String componentID) throws NoSuchExtensionException {
				if (componentID.equals(PredefinedRepositoryImplConfigurer.class.getName())) {
					return new PredefinedRepositoryImplConfigurerFactory();
				} else if (componentID.equals(RegexSearchStrategy.class.getName())) {
					return new RegexSearchStrategyFactory();
				} else {
					throw new IllegalArgumentException("Unsupported extension: " + componentID);
				}
			}
		});
		Properties renderingEngineConfiguration = new Properties();
		renderingEngineConfiguration.setProperty("template", "(\\(${notation}\\) )?${show}");
		renderingEngineConfiguration.setProperty("variables",
				"{\"notation\" : {\"propertyPath\" : [\"<http://www.w3.org/2004/02/skos/core#notation>\"]}}");

		Project project = ProjectManager.createProject(ProjectConsumer.SYSTEM, "Test", Project.SKOS_MODEL,
				Project.SKOS_MODEL, "http://example.org/", false, true, false, new CreateLocal(), "core",
				new PluginSpecification(PredefinedRepositoryImplConfigurer.class.getName(),
						RDF4JPersistentInMemorySailConfigurerConfiguration.class.getName(), null,
						JsonNodeFactory.instance.objectNode()),
				null, "support",
				new PluginSpecification(PredefinedRepositoryImplConfigurer.class.getName(),
						RDF4JPersistentInMemorySailConfigurerConfiguration.class.getName(), null,
						JsonNodeFactory.instance.objectNode()),
				null,
				new PluginSpecification(NativeTemplateBasedURIGeneratorFactory.class.getName(),
						NativeTemplateBasedURIGeneratorConfiguration.class.getName(), null,
						JsonNodeFactory.instance.objectNode()),
				new PluginSpecification(SKOSRenderingEngineFactory.class.getName(),
						SKOSRenderingEngineConfiguration.class.getName(), renderingEngineConfiguration,
						JsonNodeFactory.instance.objectNode()),
				null, null, new String[] { "resource" }, null, null, null, null, null, null, false, null,false);
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
