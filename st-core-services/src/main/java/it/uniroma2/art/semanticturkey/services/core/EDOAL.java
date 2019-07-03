package it.uniroma2.art.semanticturkey.services.core;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.OtherPropertiesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.vocabulary.Alignment;

/**
 * This class provides services for manipulating Alignments in the Align API format.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class EDOAL extends STServiceAdapter {

	public static class Correspondence {
		private Collection<AnnotatedValue<Value>> leftEntity;
		private Collection<AnnotatedValue<Value>> rightEntity;
		private Collection<AnnotatedValue<Value>> relation;
		private Collection<AnnotatedValue<Value>> measure;

		public Collection<AnnotatedValue<Value>> getLeftEntity() {
			return leftEntity;
		}

		public Collection<AnnotatedValue<Value>> getRightEntity() {
			return rightEntity;
		}

		public Collection<AnnotatedValue<Value>> getRelation() {
			return relation;
		}

		public Collection<AnnotatedValue<Value>> getMeasure() {
			return measure;
		}
	}

	private static Logger logger = LoggerFactory.getLogger(EDOAL.class);

	@Autowired
	private CustomFormManager cfManager;

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

	@Read
	@STServiceOperation
	public Collection<Correspondence> getCorrespondences(Resource alignment,
			@Optional(defaultValue = "0") int offset, @Optional(defaultValue = "10") int limit) {
		return Collections.emptyList();
	}

}