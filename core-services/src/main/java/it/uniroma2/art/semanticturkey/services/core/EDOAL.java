package it.uniroma2.art.semanticturkey.services.core;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.http.config.HTTPRepositoryConfig;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.data.nature.TripleScopes;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.plugin.extpts.RenderingEngine;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.STLocalRepositoryManager;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.rendering.BaseRenderingEngine;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.resourceview.AbstractStatementConsumer;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.vocabulary.Alignment;

/**
 * This class provides services for manipulating Alignments in the Align API format.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class EDOAL extends STServiceAdapter {

	public static class Correspondence {
		private Resource identity;
		private Collection<AnnotatedValue<Value>> leftEntity;
		private Collection<AnnotatedValue<Value>> rightEntity;
		private Collection<AnnotatedValue<Value>> relation;
		private Collection<AnnotatedValue<Value>> measure;

		public Resource getIdentity() {
			return identity;
		}

		public void setIdentity(Resource identity) {
			this.identity = identity;
		}

		public Collection<AnnotatedValue<Value>> getLeftEntity() {
			return leftEntity;
		}

		public void setLeftEntity(Collection<AnnotatedValue<Value>> leftEntity) {
			this.leftEntity = leftEntity;
		}

		public Collection<AnnotatedValue<Value>> getRightEntity() {
			return rightEntity;
		}

		public void setRightEntity(Collection<AnnotatedValue<Value>> rightEntity) {
			this.rightEntity = rightEntity;
		}

		public Collection<AnnotatedValue<Value>> getRelation() {
			return relation;
		}

		public void setRelation(Collection<AnnotatedValue<Value>> relation) {
			this.relation = relation;
		}

		public Collection<AnnotatedValue<Value>> getMeasure() {
			return measure;
		}

		public void setMeasure(Collection<AnnotatedValue<Value>> measure) {
			this.measure = measure;
		}
	}

	private static Logger logger = LoggerFactory.getLogger(EDOAL.class);

	private static Pattern variablePattern = Pattern.compile("(?:\\?|\\$)([a-zA-Z\\d]+)\\b");

	@Autowired
	private CustomFormManager cfManager;

	/**
	 * Returns information about the aligned projects
	 * 
	 * @return
	 */
	@STServiceOperation
	public Pair<String, String> getAlignedProjects() {
		Project thisProject = getProject();

		String leftDataset = thisProject.getProperty(Project.LEFT_DATASET_PROP);
		String rightDataset = thisProject.getProperty(Project.RIGHT_DATASET_PROP);

		return ImmutablePair.of(leftDataset, rightDataset);
	}

	/**
	 * Returns the align:Alignment resources defined in the current project
	 * 
	 * @param superClass
	 * @param numInst
	 * @return
	 * @throws ProjectInexistentException
	 * @throws InvalidProjectNameException
	 * @throws ProjectAccessException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	public BNode createAlignment()
			throws ProjectAccessException, InvalidProjectNameException, ProjectInexistentException {
		Project thisProject = getProject();
		Project leftDataset = ProjectManager.getProject(thisProject.getProperty(Project.LEFT_DATASET_PROP),
				false);
		Project rightDataset = ProjectManager.getProject(thisProject.getProperty(Project.RIGHT_DATASET_PROP),
				false);

		ValueFactory vf = getManagedConnection().getValueFactory();
		Update update = getManagedConnection().prepareUpdate(
		//@formatter:off
			"PREFIX align: <http://knowledgeweb.semanticweb.org/heterogeneity/alignment#>\n" +
			"INSERT {\n" +
			"  ?alignment a align:Alignment ;\n" +
			"    align:onto1 ?onto1 ;\n" +
			"    align:onto2 ?onto2 ;\n" +
			"  .\n" +
			"}\n" +
			"WHERE {\n" +
			"}\n"
			//@formatter:on
		);

		SimpleDataset dataset = new SimpleDataset();
		dataset.setDefaultInsertGraph((IRI) getWorkingGraph());

		update.setDataset(dataset);
		update.setBinding("onto1", vf.createIRI(leftDataset.getBaseURI()));
		update.setBinding("onto2", vf.createIRI(rightDataset.getBaseURI()));
		BNode alignmentNode = vf.createBNode();
		update.setBinding("alignment", alignmentNode);

		update.execute();
		
		return alignmentNode;
	}

	/**
	 * Returns the align:Alignment resources defined in the current project
	 * 
	 * @param superClass
	 * @param numInst
	 * @return
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<Resource>> getAlignments() {
		QueryBuilder qb = createQueryBuilder(
		//@formatter:off
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +  
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
			"PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>\n" +
			"PREFIX align: <http://knowledgeweb.semanticweb.org/heterogeneity/alignment#>\n" + 
			"SELECT ?resource\n" + 
			"       (COUNT(?cell) as ?attr_correspondences)\n" + 
			"	   (MIN(?onto1T) as ?attr_leftDataset)\n" + 
			"	   (MIN(?onto2T) as ?attr_rightDataset)\n" +
			generateNatureSPARQLSelectPart() +
			" WHERE {\n" + 
			"  ?resource a align:Alignment .\n" + 
			"  OPTIONAL {\n" + 
			"    ?resource align:map ?cell\n" + 
			"  }\n" + 
			"  OPTIONAL {\n" + 
			"    ?resource align:onto1 ?onto1T\n" + 
			"  }\n" + 
			"  OPTIONAL {\n" + 
			"    ?resource align:onto2 ?onto2T\n" + 
			"  }\n" + 
			generateNatureSPARQLWherePart("?resource") +
			"}\n" + 
			"GROUP BY ?resource "
			//@formatter:on
		);
		return qb.runQuery();
	}

	/**
	 * Adds a new correspondence (i.e. an align:cell) to the provided <code>alignment</code>.
	 * 
	 * @param alignment
	 * @param leftEntity
	 * @param rightEntity
	 * @param relation
	 * @param measure
	 */
	@Write
	@STServiceOperation(method = RequestMethod.POST)
	public void createCorrespondence(Resource alignment, IRI leftEntity, IRI rightEntity, String relation,
			float measure) {
		Update update = getManagedConnection().prepareUpdate(
		//@formatter:off
			"PREFIX align: <http://knowledgeweb.semanticweb.org/heterogeneity/alignment#>\n" + 
			"INSERT {\n" +
			"  GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {\n" +
			"    ?alignment align:map [\n" + 
			"      a align:Cell ;\n" + 
			"        align:entity1 ?leftEntity ;\n" + 
			"        align:entity2 ?rightEntity ;\n" + 
			"        align:relation ?relation ;\n" + 
			"        align:measure ?measure\n" + 
			"    ] .\n" +
			"  }\n" +
			"} WHERE {\n" + 
			"}"
			//@formatter:on
		);

		update.setBinding("alignment", alignment);

		update.setBinding("leftEntity", leftEntity);
		update.setBinding("rightEntity", rightEntity);
		update.setBinding("relation", SimpleValueFactory.getInstance().createLiteral(relation));
		update.setBinding("measure", SimpleValueFactory.getInstance().createLiteral(measure));

		update.execute();
	}

	/**
	 * Sets the <code>align:entity1</code> of the provided <code>correspondence</code>.
	 * 
	 * @param correspondence
	 * @param entity
	 */
	@Write
	@STServiceOperation(method = RequestMethod.POST)
	public void setLeftEntity(Resource correspondence, IRI entity) {
		replacePropertyValueInternal(correspondence, Alignment.ENTITY1, entity);
	}

	/**
	 * Sets the <code>align:entity2</code> of the provided <code>correspondence</code>.
	 * 
	 * @param correspondence
	 * @param entity
	 */
	@Write
	@STServiceOperation(method = RequestMethod.POST)
	public void setRightEntity(Resource correspondence, IRI entity) {
		replacePropertyValueInternal(correspondence, Alignment.ENTITY2, entity);
	}

	/**
	 * Sets the <code>align:relation</code> of the provided <code>correspondence</code>.
	 * 
	 * @param correspondence
	 * @param entity
	 */
	@Write
	@STServiceOperation(method = RequestMethod.POST)
	public void setRelation(Resource correspondence, String relation) {
		replacePropertyValueInternal(correspondence, Alignment.RELATION,
				SimpleValueFactory.getInstance().createLiteral(relation));
	}

	/**
	 * Sets the <code>align:relation</code> of the provided <code>correspondence</code>.
	 * 
	 * @param correspondence
	 * @param entity
	 */
	@Write
	@STServiceOperation(method = RequestMethod.POST)
	public void setMeasure(Resource correspondence, float measure) {
		replacePropertyValueInternal(correspondence, Alignment.MEASURE,
				SimpleValueFactory.getInstance().createLiteral(measure));
	}

	/**
	 * Sets the <code>align:relation</code> of the provided <code>correspondence</code>.
	 * 
	 * @param correspondence
	 * @param entity
	 */
	@Write
	@STServiceOperation(method = RequestMethod.POST)
	public void deleteCorrespondence(Resource correspondence) {
		Resource workingGraph = getWorkingGraph();
		getManagedConnection().remove((Resource) null, null, correspondence, workingGraph);
		getManagedConnection().remove(correspondence, null, null, workingGraph);
	}

	public void replacePropertyValueInternal(Resource correspondence, IRI prop, Value value)
			throws RepositoryException, MalformedQueryException, UpdateExecutionException {
		Update update = getManagedConnection().prepareUpdate(
		//@formatter:off
			"DELETE {\n" +
			"  GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {\n" +
			"    ?correspondence ?prop ?oldValue .\n" +
			"  }\n" +
			"}\n" +
			"INSERT {\n" +
			"  GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {\n" +
			"    ?correspondence ?prop ?value .\n" +
			"  }\n" +
			"}\n" +
			"WHERE {\n" +
			"  OPTIONAL {\n" +
			"    GRAPH " + NTriplesUtil.toNTriplesString(getWorkingGraph()) + " {\n" +
			"      ?correspondence ?prop ?oldValue .\n" +
			"    }\n" +
			"  }\n" +
			"}"
			//@formatter:on
		);

		update.setBinding("correspondence", correspondence);
		update.setBinding("prop", prop);
		update.setBinding("value", value);

		update.execute();
	}

	/**
	 * Returns the correspondences in an alignment
	 * 
	 * @param alignment
	 * @param page
	 * @param pageSize
	 *            if less than or equal to zero, then all correspondences go into one page
	 * @return
	 * @throws ProjectAccessException
	 * @throws InvalidProjectNameException
	 * @throws ProjectInexistentException
	 * @throws IndexingLanguageNotFound
	 */
	@Read
	@STServiceOperation
	public Collection<Correspondence> getCorrespondences(Resource alignment,
			@Optional(defaultValue = "0") int page, @Optional(defaultValue = "10") int pageSize)
			throws ProjectAccessException, InvalidProjectNameException, ProjectInexistentException,
			IndexingLanguageNotFound {
		Project thisProject = getProject();
		Project leftDataset = ProjectManager.getProject(thisProject.getProperty(Project.LEFT_DATASET_PROP),
				false);
		Project rightDataset = ProjectManager.getProject(thisProject.getProperty(Project.RIGHT_DATASET_PROP),
				false);

		RepositoryConfig coreRepositoryConfig = leftDataset.getRepositoryManager()
				.getRepositoryConfig(Project.CORE_REPOSITORY);
		RepositoryImplConfig repoImpl = STLocalRepositoryManager
				.getUnfoldedRepositoryImplConfig(coreRepositoryConfig);
		if (!(repoImpl instanceof HTTPRepositoryConfig)) {
			throw new IllegalStateException(String.format(
					"The project '%s' is not backed by a remote triple store", leftDataset.getName()));
		}

		IRI leftRepoEndpoint = SimpleValueFactory.getInstance()
				.createIRI(((HTTPRepositoryConfig) repoImpl).getURL());

		String queryString =
		//@formatter:off
			"prefix align: <http://knowledgeweb.semanticweb.org/heterogeneity/alignment#>\n" + 
			"select ?x\n" + 
			"       (GROUP_CONCAT(CONCAT(STR(?g1),\"|=|\",STR(?entity1));separator=\"|_|\") as ?entity1B)\n" + 
			"       (GROUP_CONCAT(CONCAT(STR(?g2),\"|=|\",STR(?entity2));separator=\"|_|\") as ?entity2B)\n" + 
			"       (GROUP_CONCAT(CONCAT(STR(?g3),\"|=|\",STR(?relation));separator=\"|_|\") as ?relationB)\n" + 
			"       (GROUP_CONCAT(CONCAT(STR(?g4),\"|=|\",STR(?measure));separator=\"|_|\") as ?measureB)\n" + 
			"       (MIN(LCASE(?valueIndex)) as ?index) \n" + 
			"{\n" + 
			"    ?x a align:Cell .\n" + 
			"    graph ?g1 {\n" + 
			"       ?x align:entity1 ?entity1 .\n" + 
			"    }\n" + 
			"    graph ?g2 {\n" + 
			"       ?x align:entity2 ?entity2 .\n" + 
			"    }\n" + 
			"    graph ?g3 {\n" + 
			"       ?x align:relation ?relation .\n" + 
			"    }\n" + 
			"    graph ?g4 {\n" + 
			"       ?x align:measure ?measure .\n" + 
			"    }\n" + 
			"    ?al align:map ?x.\n" +
			"    FILTER(sameTerm(?al, ?alignment))\n" +
			"    service " + NTriplesUtil.toNTriplesString(leftRepoEndpoint) + " {\n" + 
			"      optional {\n" + 
			computeIndexingGraphPattern(leftDataset) +
			"      }\n" + 
			"    }\n" + 
			"}\n" + 
			"group by ?x\n" + 
			"having BOUND(?x)\n" +
			"order by ASC(?index) ?x\n" +
			"offset " + (page * pageSize) + "\n"+
			(pageSize <= 0 ? "" : "limit " + pageSize + "\n")
			//@formatter:on
		;

		TupleQuery query = getManagedConnection().prepareTupleQuery(queryString);
		query.setBinding("alignment", alignment);
		return QueryResults.stream(query.evaluate()).map(bs -> {
			Correspondence corr = new Correspondence();
			corr.setIdentity((Resource) bs.getValue("x"));
			corr.setLeftEntity(processStringiedObjectList(bs.getValue("entity1B").stringValue(), null));
			corr.setRightEntity(processStringiedObjectList(bs.getValue("entity2B").stringValue(), null));
			corr.setRelation(
					processStringiedObjectList(bs.getValue("relationB").stringValue(), XMLSchema.STRING));
			corr.setMeasure(
					processStringiedObjectList(bs.getValue("measureB").stringValue(), XMLSchema.FLOAT));
			return corr;
		}).collect(toList());
	}

	private String computeIndexingGraphPattern(Project keyProject) throws IndexingLanguageNotFound {
		RenderingEngine renderingEngine = keyProject.getRenderingEngine();
		if (renderingEngine instanceof BaseRenderingEngine) {
			// a BaseRenderingEngine can provide a label pattern using variables ?resource and ?labelInternal
			StringBuilder sb = new StringBuilder();
			((BaseRenderingEngine) renderingEngine).getGraphPatternInternal(sb);
			Matcher m = variablePattern.matcher(sb.toString());
			StringBuffer renamedPattern = new StringBuffer();
			while (m.find()) {
				String varName = m.group(1);

				String renamedVar;

				switch (varName) {
				case "resource":
					renamedVar = "entity1";
					break;
				case "labelInternal":
					renamedVar = "valueIndex";
					break;
				default:
					renamedVar = "proc_0_" + varName;
				}
				m.appendReplacement(renamedPattern, "?" + renamedVar);
			}

			m.appendTail(renamedPattern);

			String userLanguages;

			try {
				userLanguages = STPropertiesManager.getPUSetting(STPropertiesManager.PREF_LANGUAGES,
						keyProject, UsersManager.getLoggedUser(), RenderingEngine.class.getName());
			} catch (IllegalStateException | STPropertyAccessException e) {
				throw new IndexingLanguageNotFound(
						"Unable to find user languages for project \"" + keyProject + "\"", e);
			}

			if (userLanguages == null || userLanguages.isEmpty()) {
				throw new IndexingLanguageNotFound("Empty user languages for project \"" + keyProject + "\"");
			}

			if (userLanguages.equals("*")) {
				throw new IndexingLanguageNotFound(
						"No specific user language configured for the project \"" + keyProject + "\"");
			}

			String indexingLanguage = userLanguages.split(",")[0];

			renamedPattern.append(
					"\nFILTER(lang(?valueIndex) = \"" + RenderUtils.escape(indexingLanguage) + "\")\n");

			return renamedPattern.toString();
		} else {
			logger.warn("Unsupported rendering engine {} on the key project: {}",
					renderingEngine.getClass().getName(), keyProject.getName());
			return "";
		}
	}

	private Collection<AnnotatedValue<Value>> processStringiedObjectList(String input, IRI datatype) {
		Collection<AnnotatedValue<Value>> rv = new ArrayList<>();
		Multimap<String, String> object2graphs = HashMultimap.create();
		for (String graphObjectPair : (input.split("\\|_\\|"))) {
			String[] splits = graphObjectPair.split("\\|=\\|");
			if (splits.length != 2)
				continue;

			String graph = splits[0];
			String object = splits[1];

			object2graphs.put(object, graph);
		}

		for (String object : object2graphs.keys()) {
			Value value;
			if (datatype == null) {
				value = SimpleValueFactory.getInstance().createIRI(object);
			} else {
				value = SimpleValueFactory.getInstance().createLiteral(object, datatype);
			}

			Set<Resource> graphs = object2graphs.get(object).stream()
					.map(s -> SimpleValueFactory.getInstance().createIRI(s)).collect(Collectors.toSet());

			TripleScopes tripleScope = AbstractStatementConsumer.computeTripleScope(graphs,
					getWorkingGraph());

			Map<String, Value> attributes = new HashMap<>();
			attributes.put("tripleScope",
					SimpleValueFactory.getInstance().createLiteral(tripleScope.toString()));
			AnnotatedValue<Value> annotValue = new AnnotatedValue<>(value, attributes);
			rv.add(annotValue);

		}
		return rv;
	}

}