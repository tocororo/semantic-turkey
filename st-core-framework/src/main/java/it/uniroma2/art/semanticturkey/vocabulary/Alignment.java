package it.uniroma2.art.semanticturkey.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class Alignment {
	
	//INRIA default property/Class
	/** http://knowledgeweb.semanticweb.org/heterogeneity/alignment **/
	public static final String URI = "http://knowledgeweb.semanticweb.org/heterogeneity/alignment";
	
	/** http://knowledgeweb.semanticweb.org/heterogeneity/alignment# **/
	public static final String NAMESPACE = URI + "#";
	
	/** http://knowledgeweb.semanticweb.org/heterogeneity/alignment#Alignment **/
	public static final IRI ALIGNMENT;
	/** http://knowledgeweb.semanticweb.org/heterogeneity/alignment#Cell **/
	public static final IRI CELL;
	/** http://knowledgeweb.semanticweb.org/heterogeneity/alignment#Ontology **/
	public static final IRI ONTOLOGY;
	/** http://knowledgeweb.semanticweb.org/heterogeneity/alignment#xml **/
	public static final IRI XML;
	/** http://knowledgeweb.semanticweb.org/heterogeneity/alignment#level **/
	public static final IRI LEVEL;
	/** http://knowledgeweb.semanticweb.org/heterogeneity/alignment#type **/
	public static final IRI TYPE;
	/** http://knowledgeweb.semanticweb.org/heterogeneity/alignment#onto1 **/
	public static final IRI ONTO1;
	/** http://knowledgeweb.semanticweb.org/heterogeneity/alignment#onto2 **/
	public static final IRI ONTO2;
	/** http://knowledgeweb.semanticweb.org/heterogeneity/alignment#location **/
	public static final IRI LOCATION;
	/** http://knowledgeweb.semanticweb.org/heterogeneity/alignment#map **/
	public static final IRI MAP;
	/** http://knowledgeweb.semanticweb.org/heterogeneity/alignment#entity1 **/
	public static final IRI ENTITY1;
	/** http://knowledgeweb.semanticweb.org/heterogeneity/alignment#entity2 **/
	public static final IRI ENTITY2;
	/** http://knowledgeweb.semanticweb.org/heterogeneity/alignment#measure **/
	public static final IRI MEASURE;
	/** http://knowledgeweb.semanticweb.org/heterogeneity/alignment#relation **/
	public static final IRI RELATION;
	/** http://knowledgeweb.semanticweb.org/heterogeneity/alignment#mappingProperty **/
	public static final IRI MAPPING_PROPERTY;
	/** http://knowledgeweb.semanticweb.org/heterogeneity/alignment#status **/
	public static final IRI STATUS;
	/** http://knowledgeweb.semanticweb.org/heterogeneity/alignment#comment **/
	public static final IRI COMMENT;
	
	static {
		ValueFactory vf = SimpleValueFactory.getInstance();
		ALIGNMENT = vf.createIRI(NAMESPACE, "Alignment");
		CELL = vf.createIRI(NAMESPACE, "Cell");
		ONTOLOGY = vf.createIRI(NAMESPACE, "Ontology");
		XML = vf.createIRI(NAMESPACE, "xml");
		LEVEL = vf.createIRI(NAMESPACE, "level");
		TYPE = vf.createIRI(NAMESPACE, "type");
		ONTO1 = vf.createIRI(NAMESPACE, "onto1");
		ONTO2 = vf.createIRI(NAMESPACE, "onto2");
		LOCATION = vf.createIRI(NAMESPACE, "location");
		MAP = vf.createIRI(NAMESPACE, "map");
		ENTITY1 = vf.createIRI(NAMESPACE, "entity1");
		ENTITY2 = vf.createIRI(NAMESPACE, "entity2");
		MEASURE = vf.createIRI(NAMESPACE, "measure");
		RELATION = vf.createIRI(NAMESPACE, "relation");
		MAPPING_PROPERTY = vf.createIRI(NAMESPACE, "mappingProperty");
		STATUS = vf.createIRI(NAMESPACE, "status");
		COMMENT = vf.createIRI(NAMESPACE, "comment");
	}

}
