package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer;

import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ClosableFormattedResource;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ExporterContext;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ReformattingException;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ReformattingExporter;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ReformattingWrongLexModelException;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ReformattingWrongModelException;
import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures.CollectionInfo;
import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures.ConceptInfo;
import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures.ConceptSchemeInfo;
import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures.HeaderWhole;
import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures.PropInfoAndValues;
import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures.ReifiedValue;
import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures.ResourceInfo;
import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures.SimpleValue;
import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures.ValueForProp;
import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.xlsx.XlsxStructureAndUtils;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.utilities.ModelUtilities;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * A {@link ReformattingExporter} that serializes RDF data in Spreadsheet format
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
public class SpreadsheetSerializingExporter implements ReformattingExporter {

	public static class MessageKeys {
		public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.SpreadsheetSerializingExporter";

		public static final String exceptionModel$messsage = keyBase + ".exceptionModel.message";
		public static final String exceptionLexModel$messsage = keyBase + ".exceptionLexModel.message";
	}

	private SpreadsheetSerializingExporterConfiguration config;

	public SpreadsheetSerializingExporter(SpreadsheetSerializingExporterConfiguration config) {
		this.config = config;
	}

	private Map<String, String> prefixDeclaraionMap = new HashMap<>();

	@Override
	public ClosableFormattedResource export(RepositoryConnection sourceRepositoryConnection, IRI[] graphs,
			@Nullable String format, ExporterContext exporterContext) throws IOException, ReformattingException {
		Objects.requireNonNull(format, "Format must be specified");

		boolean reifiedNote = config.reifiedNotes != null && config.reifiedNotes;
		String prefLang = config.language;


		prefixDeclaraionMap = exporterContext.getProject().getOntologyManager().getNSPrefixMappings(false);

		if(!exporterContext.getProject().getModel().equals(Project.SKOS_MODEL)){
			//only SKOS Model should be used
			throw new ReformattingWrongModelException(STMessageSource.getMessage(MessageKeys.exceptionModel$messsage));
		}

		IRI lexModel = exporterContext.getProject().getLexicalizationModel();
		if(!lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL) && !lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)) {
			//only SKOS and SKOSXL lexicalization are supported
			throw new ReformattingWrongLexModelException(STMessageSource.getMessage(MessageKeys.exceptionLexModel$messsage));
		}

		boolean isSkosxlLex = !lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL);

		String query;

		//do a SPARQL query to get all possible lexicalization properties (depending on the lexicalization model and considering also the sub properties) (3)
		List<IRI> prefPropList = new ArrayList<>();
		List<IRI> altPropList = new ArrayList<>();
		List<IRI> hiddenPropList = new ArrayList<>();
		if(lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL)){
			// @formatter:off
			query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
					"\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
					"\nSELECT ?prefLabelProp ?altLabelProp ?hiddenLabelProp " +
					"\nWHERE {" +
					"\n?prefLabelProp rdfs:subPropertyOf* skos:prefLabel ." +
					"\n?altLabelProp rdfs:subPropertyOf* skos:altLabel ." +
					"\n?hiddenLabelProp rdfs:subPropertyOf* skos:hiddenLabel ." +
					"\n}";
		} else { //lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)
			query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
					"\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> "+
					"\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
					"\nSELECT ?prefLabelProp ?altLabelProp ?hiddenLabelProp " +
					"\nWHERE {" +
					"\n?prefLabelProp rdfs:subPropertyOf* skosxl:prefLabel ." +
					"\n?altLabelProp rdfs:subPropertyOf* skosxl:altLabel ." +
					"\n?hiddenLabelProp rdfs:subPropertyOf* skosxl:hiddenLabel ." +
					"\n}";
			// @formatter:on
		}
		TupleQuery  tupleQuery = sourceRepositoryConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
		while(tupleQueryResult.hasNext()){
			BindingSet bindingSet = tupleQueryResult.next();
			Value prefLabelProp = bindingSet.getBinding("prefLabelProp").getValue();
			if(prefLabelProp instanceof IRI && !prefPropList.contains(prefLabelProp)) {
				prefPropList.add((IRI) prefLabelProp);
			}
			Value altLabelProp = bindingSet.getBinding("altLabelProp").getValue();
			if(altLabelProp instanceof IRI && !altPropList.contains(altLabelProp)) {
				altPropList.add((IRI) altLabelProp);
			}
			Value hiddenLabelProp = bindingSet.getBinding("hiddenLabelProp").getValue();
			if(hiddenLabelProp instanceof IRI && !prefPropList.contains(hiddenLabelProp)) {
				hiddenPropList.add((IRI) hiddenLabelProp);
			}
		}

		//do a SPARQL query to get all the subproperties of skos:note (useful only for reified note)
		List<IRI> notePropList = new ArrayList<>();
		if(reifiedNote) {
			// @formatter:off
			query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
					"\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
					"\nSELECT ?noteProp " +
					"\nWHERE {" +
					"\n?noteProp rdfs:subPropertyOf* skos:note ." +
					"\n}";
			// @formatter:on

			tupleQuery = sourceRepositoryConnection.prepareTupleQuery(query);
			tupleQuery.setIncludeInferred(false);
			tupleQueryResult = tupleQuery.evaluate();
			while (tupleQueryResult.hasNext()) {
				BindingSet bindingSet = tupleQueryResult.next();
				Value noteProp = bindingSet.getBinding("noteProp").getValue();
				if (noteProp instanceof IRI) {
					notePropList.add((IRI) noteProp);
				}
			}
		}


		HeaderWhole headerWhole = new HeaderWhole();

		//do several SPARQL queries to prepare the Concepts part
		final List<IRI> topConceptList = new ArrayList<>();
		final Map<IRI, ConceptInfo> conceptIriToConceptInfoMap = new HashMap<>();
		int maxDepth = prepareConceptsPart(sourceRepositoryConnection, topConceptList, prefPropList, altPropList, hiddenPropList, lexModel, isSkosxlLex,
				reifiedNote, notePropList, headerWhole, conceptIriToConceptInfoMap);


		//do a SPARQL query to get all the skos:ConceptScheme and the associated data (special case for reified data, such as SKOSXL label and skos:note)
		final Map<IRI, ConceptSchemeInfo> schemeIriToConceptSchemeInfoMap = new HashMap<>();
		prepareConceptSchemePart(sourceRepositoryConnection, prefPropList, altPropList, hiddenPropList, lexModel, isSkosxlLex, reifiedNote, notePropList,headerWhole,
				schemeIriToConceptSchemeInfoMap);

		//do a SPARQL query to get all the skos:collection and associated data (special case for reified data, such as SKOSXL label and skos:note)
		final List<IRI> topCollectionList = new ArrayList<>();
		final Map<IRI, CollectionInfo> collectionIriToConceptInfoMap = new HashMap<>();
		int depthCollection = prepareCollectionPart(sourceRepositoryConnection, topCollectionList, prefPropList, altPropList, hiddenPropList,
				lexModel, isSkosxlLex, reifiedNote, notePropList, headerWhole, collectionIriToConceptInfoMap);

		maxDepth = Math.max(maxDepth, depthCollection);

		//process the result to construct the data for the Spreadsheet

		//File tempServerFile = File.createTempFile("spreadsheet", ".xlsx");
		//create the file with it content
		XlsxStructureAndUtils xlsxStructureAndUtils = new XlsxStructureAndUtils();

		File tempServerFile = xlsxStructureAndUtils.createExcelFile(prefixDeclaraionMap, topConceptList, conceptIriToConceptInfoMap, schemeIriToConceptSchemeInfoMap,
				topCollectionList, collectionIriToConceptInfoMap, maxDepth, headerWhole, isSkosxlLex, prefPropList, altPropList, hiddenPropList,
				notePropList, reifiedNote, prefLang);

		return new ClosableFormattedResource(tempServerFile, "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				StandardCharsets.UTF_8, null);
	}

	/** CONCEPT PART **/

	private int prepareConceptsPart(RepositoryConnection sourceRepositoryConnection, List<IRI> topConceptList, List<IRI> prefPropList, List<IRI> altPropList,
			List<IRI> hiddenPropList, IRI lexModel, boolean isSkosxlLex, boolean reifiedNote, List<IRI> notePropList, HeaderWhole headerWhole,
			Map<IRI, ConceptInfo> conceptIriToConceptInfoMap){
		//do a SPARQL query to get all the subclasses of skos:Concept (1)
		List<IRI> conceptClassList = new ArrayList<>();
		// @formatter:off
		String query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
				"\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"\nSELECT ?conceptClass " +
				"\nWHERE {" +
				"\n?conceptClass rdfs:subClassOf* skos:Concept ." +
				"\n}";
		// @formatter:on

		TupleQuery tupleQuery = sourceRepositoryConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
		while(tupleQueryResult.hasNext()){
			BindingSet bindingSet = tupleQueryResult.next();
			Value conceptClass = bindingSet.getBinding("conceptClass").getValue();
			if(conceptClass instanceof IRI) {
				conceptClassList.add((IRI) conceptClass);
			}
		}

		// do a SPARQL query to get all the sub properties of skos:broader and skos:narrower
		List<IRI> broaderPropList = new ArrayList<>();
		List<IRI> narrowerPropList = new ArrayList<>();
		// @formatter:off
		query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
				"\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"\nSELECT ?broaderProp ?narrowerProp " +
				"\nWHERE {" +
				"\n?broaderProp rdfs:subPropertyOf* skos:broader ." +
				"\n?narrowerProp rdfs:subPropertyOf* skos:narrower ." +
				"\n}";
		// @formatter:on
		tupleQuery = sourceRepositoryConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQueryResult = tupleQuery.evaluate();
		while(tupleQueryResult.hasNext()){
			BindingSet bindingSet = tupleQueryResult.next();
			Value broaderProp = bindingSet.getBinding("broaderProp").getValue();
			if(broaderProp instanceof IRI && !broaderPropList.contains(broaderProp) && !broaderProp.stringValue().equals("http://www.w3.org/2004/02/skos/core#broadMatch")) {
				broaderPropList.add((IRI) broaderProp);
			}
			Value narrowerProp = bindingSet.getBinding("narrowerProp").getValue();
			if(narrowerProp instanceof IRI && !narrowerPropList.contains(narrowerProp) && !narrowerProp.stringValue().equals("http://www.w3.org/2004/02/skos/core#narrowMatch")) {
				narrowerPropList.add((IRI) narrowerProp);
			}
		}

		// do a SPARQL query to get all concepts not having a broader and not being narrower of any other concept
		// so they can be considered as topConcept

		// @formatter:on
		query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
				"\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"\nSELECT DISTINCT ?concept " +
				"\nWHERE {" +
				addValuesPart("?type", conceptClassList)+
				"\n?concept a ?type ." +
				"\nFILTER(isIRI(?concept)) " +
				"\nFILTER NOT EXISTS {" +
				addValuesPart("?broaderProp", broaderPropList)+
				"\n?concept ?broaderProp ?broaderConcept ." +
				"\n}" +
				"\nFILTER NOT EXISTS {" +
				addValuesPart("?narrowerProp", narrowerPropList)+
				"\n?narrowerConcept ?narrowerProp ?concept ." +
				"\n}" +
				"\n}";
		// @formatter:on
		tupleQuery = sourceRepositoryConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQueryResult = tupleQuery.evaluate();
		while(tupleQueryResult.hasNext()){
			BindingSet bindingSet = tupleQueryResult.next();
			Value concept = bindingSet.getBinding("concept").getValue();
			if(concept instanceof IRI && !topConceptList.contains(concept)) {
				topConceptList.add((IRI) concept);
			}
		}

		// do a SPARQL query to get the hierarchy of the concepts
		Map<IRI, List<IRI>> conceptToNarrowerConceptListMap = new HashMap<>();
		// @formatter:off
		query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
				"\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"\nSELECT ?concept ?broaderConcept ?narrowerConcept" +
				"\nWHERE {" +

				"\n{"+
				addValuesPart("?type", conceptClassList) +
				"\n?concept a ?type ." +
				"\nFILTER(isIRI(?concept)) " +
				addValuesPart("?broaderProp", broaderPropList) +
				"\n?concept ?broaderProp ?broaderConcept ." +
				"\n}" +

				"\nUNION" +

				"\n{"+
				addValuesPart("?type", conceptClassList) +
				"\n?concept a ?type ." +
				addValuesPart("?narrowerProp", narrowerPropList) +
				"\n?narrowerConcept ?narrowerProp ?concept ." +
				"\n}" +

				"\n}";
		// @formatter:on

		tupleQuery = sourceRepositoryConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQueryResult = tupleQuery.evaluate();
		while(tupleQueryResult.hasNext()){
			BindingSet bindingSet = tupleQueryResult.next();
			Value conceptValue = bindingSet.getBinding("concept").getValue();
			if(!(conceptValue instanceof IRI) ) {
				continue;
			}
			if(bindingSet.hasBinding("broaderConcept")){
				Value broaderValue = bindingSet.getBinding("broaderConcept").getValue();
				if (broaderValue instanceof IRI){
					addNarrower(conceptToNarrowerConceptListMap, (IRI)broaderValue, (IRI)conceptValue);
				}
			}
			if(bindingSet.hasBinding("narrowerConcept")){
				Value narrowerValue = bindingSet.getBinding("narrowerConcept").getValue();
				if (narrowerValue instanceof IRI){
					addNarrower(conceptToNarrowerConceptListMap, (IRI)narrowerValue, (IRI)conceptValue);
				}
			}
		}

		//create the hierarchy, starting from the top concept (and also fill the conceptIriToConceptInfoMap)
		int maxDepth = 1;
		for(IRI conceptIri : topConceptList) {
			int depth = createConceptHierarchy(conceptIri, conceptToNarrowerConceptListMap, conceptIriToConceptInfoMap, 1, new TreeSet<>());
			maxDepth = Math.max(maxDepth, depth);
		}

		// do a SPARQL query to get the lexicalization (different query depending on the Lexicalization model)
		List<IRI> propLexList = new ArrayList<>();
		propLexList.addAll(prefPropList);
		propLexList.addAll(altPropList);
		propLexList.addAll(hiddenPropList);
		addLexicalizations(sourceRepositoryConnection, propLexList, conceptClassList, lexModel, isSkosxlLex,
				headerWhole, conceptIriToConceptInfoMap, null, null);

		// do a SPARQL query to get the reified note (if reifiedNote is passed as true)
		if(reifiedNote) {
			addReifiedNotes(sourceRepositoryConnection, conceptClassList, headerWhole, isSkosxlLex, notePropList,
					reifiedNote, conceptIriToConceptInfoMap, null, null);
		}


		// do a SPARQL query to get all the data directly associated to the concepts
		addGenericPropertyValue(sourceRepositoryConnection, conceptClassList, isSkosxlLex, propLexList, headerWhole,
				conceptIriToConceptInfoMap, null, null);

		return maxDepth;
	}



	private void addNarrower(Map<IRI, List<IRI>> conceptToNarrowerConcept, IRI broader, IRI narrower) {
		if(!conceptToNarrowerConcept.containsKey(broader)){
			conceptToNarrowerConcept.put(broader, new ArrayList<>());
		}
		if(!conceptToNarrowerConcept.get(broader).contains(narrower)){
			conceptToNarrowerConcept.get(broader).add(narrower);
		}
	}

	private int createConceptHierarchy(IRI concept, Map<IRI, List<IRI>> conceptToNarrowerConceptMap,
			Map<IRI, ConceptInfo> conceptIriToConceptInfoMap, int currentDepth, TreeSet<String> alreadyAddedConceptList) {
		int maxDepth = currentDepth;
		if(!conceptIriToConceptInfoMap.containsKey(concept)){
			conceptIriToConceptInfoMap.put(concept, new ConceptInfo(concept));
		}
		ConceptInfo conceptInfo = conceptIriToConceptInfoMap.get(concept);

		TreeSet<String> newAlreadyAddedConceptList = new TreeSet<>(alreadyAddedConceptList);
		newAlreadyAddedConceptList.add(toQName(conceptInfo.getResourceIRI()));

		List<IRI> narrowerList = conceptToNarrowerConceptMap.get(concept);
		if(narrowerList!=null) {
			for (IRI narrower : narrowerList) {
				if (!conceptIriToConceptInfoMap.containsKey(narrower)) {
					conceptIriToConceptInfoMap.put(narrower, new ConceptInfo(narrower));
				}
				ConceptInfo narrowerConcept = conceptIriToConceptInfoMap.get(narrower);
				boolean added = conceptInfo.addNarrower(narrowerConcept);

				//if the narrower concept was already added in this hierarchy previously, it means that there is a cycle, so do not continue
				// with this hierarchy (but add as a narrower of the current concept)
				// WARNING: even if this concept is not expanded further, since the ConceptInfo is considered, it means that its
				// narrower concepts are already present in it, so be careful when this structure is analyzed for example to construct the xlsx file
				if (added && !newAlreadyAddedConceptList.contains(toQName(narrower))) {
					int depth = createConceptHierarchy(narrower, conceptToNarrowerConceptMap, conceptIriToConceptInfoMap, currentDepth + 1,
							newAlreadyAddedConceptList);
					maxDepth = Math.max(maxDepth, depth);
				}
			}
		}
		return maxDepth;
	}

	private void addConceptInfo(boolean isSkosxlLex, IRI concept, IRI propLex, Value xLabelValue, Literal label, IRI propNote,
			Value xNoteValue, Value noteValue, IRI prop, Value valueValue, Map<IRI, ConceptInfo> conceptIriToConceptInfoMap, HeaderWhole headerWhole,
			List<IRI> propLexList, List<IRI> propNoteList, boolean reifiedNote){
		//get the ConceptInfo from conceptIriToConceptInfoMap
		ConceptInfo conceptInfo = conceptIriToConceptInfoMap.get(concept);
		if(conceptInfo == null){
			//this in theory should never happen, so just return
			return;
		}
		addResourceInfo(conceptInfo, isSkosxlLex, propLex, xLabelValue, label, propNote, xNoteValue, noteValue, prop, valueValue, headerWhole,
				propLexList, propNoteList, reifiedNote);
	}

	/** SCHEME PART**/

	private void prepareConceptSchemePart(RepositoryConnection sourceRepositoryConnection, List<IRI> prefPropList, List<IRI> altPropList,
			List<IRI> hiddenPropList, IRI lexModel, boolean isSkosxlLex, boolean reifiedNote, List<IRI> notePropList, HeaderWhole headerWhole,
			Map<IRI, ConceptSchemeInfo> schemeIriToConceptSchemeInfoMap){
		//do a SPARQL query to get all the subclasses of skos:Concept (1)
		List<IRI> schemeClassList = new ArrayList<>();
		// @formatter:off
		String query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
				"\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"\nSELECT ?schemeClass " +
				"\nWHERE {" +
				"\n?schemeClass rdfs:subClassOf* skos:ConceptScheme ." +
				"\n}";
		// @formatter:on

		TupleQuery tupleQuery = sourceRepositoryConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
		while(tupleQueryResult.hasNext()){
			BindingSet bindingSet = tupleQueryResult.next();
			Value schemeClass = bindingSet.getBinding("schemeClass").getValue();
			if(schemeClass instanceof IRI) {
				schemeClassList.add((IRI) schemeClass);
			}
		}


		// do a SPARQL query to get the lexicalization (different query depending on the Lexicalization model)
		List<IRI> propLexList = new ArrayList<>();
		propLexList.addAll(prefPropList);
		propLexList.addAll(altPropList);
		propLexList.addAll(hiddenPropList);
		addLexicalizations(sourceRepositoryConnection, propLexList, schemeClassList, lexModel, isSkosxlLex,
				headerWhole, null, schemeIriToConceptSchemeInfoMap, null);

		// do a SPARQL query to get the reified note (if reifiedNote is passed as true)
		if(reifiedNote) {
			addReifiedNotes(sourceRepositoryConnection, schemeClassList, headerWhole, isSkosxlLex, notePropList,
					reifiedNote, null, schemeIriToConceptSchemeInfoMap, null);
		}

		// do a SPARQL query to get all the data directly associated to the concepts
		addGenericPropertyValue(sourceRepositoryConnection, schemeClassList, isSkosxlLex, propLexList, headerWhole,
				null, schemeIriToConceptSchemeInfoMap, null);

	}

	private void addSchemeInfo(boolean isSkosxlLex, IRI scheme, IRI propLex, Value xLabelValue, Literal label, IRI propNote,
			Value xNoteValue, Value noteValue, IRI prop, Value valueValue, Map<IRI, ConceptSchemeInfo> conceptSchemeIriToConceptInfoMap, HeaderWhole headerWhole,
			List<IRI> propLexList, List<IRI> propNoteList, boolean reifiedNote){
		if(!conceptSchemeIriToConceptInfoMap.containsKey(scheme)){
			conceptSchemeIriToConceptInfoMap.put(scheme, new ConceptSchemeInfo(scheme));
		}
		//get the ConceptSchemeInfo from conceptSchemeIriToConceptInfoMap
		ConceptSchemeInfo conceptSchemeInfo = conceptSchemeIriToConceptInfoMap.get(scheme);
		addResourceInfo(conceptSchemeInfo, isSkosxlLex, propLex, xLabelValue, label, propNote, xNoteValue, noteValue, prop, valueValue, headerWhole,
				propLexList, propNoteList, reifiedNote);
	}

	/** COLLECTION PART **/

	private int prepareCollectionPart(RepositoryConnection sourceRepositoryConnection, List<IRI> topCollectionList, List<IRI> prefPropList, List<IRI> altPropList,
			List<IRI> hiddenPropList, IRI lexModel, boolean isSkosxlLex, boolean reifiedNote, List<IRI> notePropList, HeaderWhole headerWhole,
			Map<IRI, CollectionInfo> collectionIriToConceptInfoMap){
		//do a SPARQL query to get all the subclasses of skos:Concept (1)
		List<IRI> collectionClassList = new ArrayList<>();
		// @formatter:off
		String query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
				"\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"\nSELECT ?collectionClass " +
				"\nWHERE {" +
				"\n?collectionClass rdfs:subClassOf* skos:Collection ." +
				"\n}";
		// @formatter:on

		TupleQuery tupleQuery = sourceRepositoryConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
		while(tupleQueryResult.hasNext()){
			BindingSet bindingSet = tupleQueryResult.next();
			Value collectionClass = bindingSet.getBinding("collectionClass").getValue();
			if(collectionClass instanceof IRI) {
				collectionClassList.add((IRI) collectionClass);
			}
		}

		// do a SPARQL query to get all the sub properties of rdfs:member and skos:memberList
		List<IRI> memberPropList = new ArrayList<>();
		List<IRI> memberListPropList = new ArrayList<>();
		// @formatter:off
		query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
				"\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"\nSELECT ?memberProp ?memberListProp " +
				"\nWHERE {" +
				"\n?memberProp rdfs:subPropertyOf* skos:member ." +
				"\n?memberListProp rdfs:subPropertyOf* skos:memberList ." +
				"\n}";
		// @formatter:on
		tupleQuery = sourceRepositoryConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQueryResult = tupleQuery.evaluate();
		while(tupleQueryResult.hasNext()){
			BindingSet bindingSet = tupleQueryResult.next();
			Value memberProp = bindingSet.getBinding("memberProp").getValue();
			if(memberProp instanceof IRI && !memberPropList.contains(memberProp) ) {
				memberPropList.add((IRI) memberProp);
			}
			Value memberListProp = bindingSet.getBinding("memberListProp").getValue();
			if(memberListProp instanceof IRI && !memberListPropList.contains(memberListProp) ) {
				memberListPropList.add((IRI) memberListProp);
			}
		}

		// do a SPARQL query to get all skos:Collection not being as a member of a something nor being in a list of a skos:memberList
		// so they can be considered as top level Collection

		// @formatter:on
		query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
				"\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"\nSELECT DISTINCT ?collection " +
				"\nWHERE {" +
				addValuesPart("?type", collectionClassList) +
				"\n?collection a ?type ." +
				"\nFILTER(isIRI(?collection)) " +

				//rdfs:member part
				"\nFILTER NOT EXISTS {"	+
				addValuesPart("?memberProp", memberPropList) +
				"\n[] ?memberProp ?collection ." +
				"\n}" +

				//skos:memberList part
				"\nFILTER NOT EXISTS {"	+
				addValuesPart("?memberListProp", memberListPropList) +
				"\n[] ?memberProp ?list ." +
				"\n?list rdf:rest*/rdf:first ?collection ." +
				"\n}" +

				"\n}";
		// @formatter:on
		tupleQuery = sourceRepositoryConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQueryResult = tupleQuery.evaluate();
		while(tupleQueryResult.hasNext()){
			BindingSet bindingSet = tupleQueryResult.next();
			Value collection = bindingSet.getBinding("collection").getValue();
			if(collection instanceof IRI && !topCollectionList.contains(collection)) {
				topCollectionList.add((IRI) collection);
			}
		}


		// do two SPARQL query to get the hierarchy of the collections:
		// 1) using rdfs:member (and its subProperties)
		// 2) using skos:memberList (and its subProperties)
		Map<IRI, List<IRI>> collectionToMemberListMap = new HashMap<>();
		// @formatter:off
		query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
				"\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"\nSELECT ?collection ?member" +
				"\nWHERE {" +

				addValuesPart("?type", collectionClassList) +
				"\n?collection a ?type ." +
				"\nFILTER(isIRI(?collection)) " +
				addValuesPart("?memberProp", memberPropList) +
				"\n?collection ?memberProp ?member ." +

				"\n}";
		// @formatter:on

		tupleQuery = sourceRepositoryConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQueryResult = tupleQuery.evaluate();
		while(tupleQueryResult.hasNext()){
			BindingSet bindingSet = tupleQueryResult.next();
			Value collectionValue = bindingSet.getBinding("collection").getValue();
			if(!(collectionValue instanceof IRI) ) {
				continue;
			}
			Value memberValue = bindingSet.getBinding("member").getValue();
			if (memberValue instanceof IRI){
				addMember(collectionToMemberListMap, (IRI)collectionValue, (IRI)memberValue );
			}
		}

		// @formatter:off
		query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
				"\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"\nSELECT ?collection ?member (COUNT(DISTINCT ?mid) AS ?index)" +
				"\nWHERE {" +

				addValuesPart("?type", collectionClassList) +
				"\n?collection a ?type ." +
				"\nFILTER(isIRI(?collection)) " +
				addValuesPart("?memberListProp", memberListPropList) +
				"\n?collection ?memberListProp ?list ." +
				"\n?list rdf:rest* ?mid ." +
				"\n?mid rdf:rest*/rdf:first ?member . " +

				"\n}" +
				"\nGROUP BY ?collection ?member " +
				"\nORDER BY ?collection ?index " ;
		// @formatter:on

		tupleQuery = sourceRepositoryConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQueryResult = tupleQuery.evaluate();
		while(tupleQueryResult.hasNext()){
			BindingSet bindingSet = tupleQueryResult.next();
			Value collectionValue = bindingSet.getBinding("collection").getValue();
			if(!(collectionValue instanceof IRI) ) {
				continue;
			}
			Value memberValue = bindingSet.getBinding("member").getValue();
			if (memberValue instanceof IRI){
				addMember(collectionToMemberListMap, (IRI)collectionValue, (IRI)memberValue );
			}
		}


		//create the hierarchy, starting from the top collections
		int maxDepth = 1;
		for(IRI conceptIri : topCollectionList) {
			int depth = createCollectionHierarchy(conceptIri, collectionToMemberListMap, collectionIriToConceptInfoMap, 1, new TreeSet<>());
			maxDepth = Math.max(maxDepth, depth);
		}


		// do a SPARQL query to get the lexicalization (different query depending on the Lexicalization model)
		List<IRI> propLexList = new ArrayList<>();
		propLexList.addAll(prefPropList);
		propLexList.addAll(altPropList);
		propLexList.addAll(hiddenPropList);
		addLexicalizations(sourceRepositoryConnection, propLexList, collectionClassList, lexModel, isSkosxlLex,
				headerWhole, null, null, collectionIriToConceptInfoMap);

		if(reifiedNote) {
			addReifiedNotes(sourceRepositoryConnection, collectionClassList, headerWhole, isSkosxlLex, notePropList,
					reifiedNote, null, null, collectionIriToConceptInfoMap);
		}


		// do a SPARQL query to get all the data directly associated to the concepts
		addGenericPropertyValue(sourceRepositoryConnection, collectionClassList, isSkosxlLex, propLexList, headerWhole,
				null, null, collectionIriToConceptInfoMap);

		return maxDepth;
	}

	private void addMember(Map<IRI, List<IRI>> collectionToMemberListMap, IRI collection, IRI member) {
		if(!collectionToMemberListMap.containsKey(collection)){
			collectionToMemberListMap.put(collection, new ArrayList<>());
		}
		if(!collectionToMemberListMap.get(collection).contains(member)){
			collectionToMemberListMap.get(collection).add(member);
		}
	}

	private int createCollectionHierarchy(IRI collection, Map<IRI, List<IRI>> collectionToMemberListMap,
			Map<IRI, CollectionInfo> collectionIriToConceptInfoMap, int currentDepth, TreeSet<String> alreadyAddedCollectionSet) {
		int maxDepth = currentDepth;
		if(!collectionIriToConceptInfoMap.containsKey(collection)){
			collectionIriToConceptInfoMap.put(collection, new CollectionInfo(collection));
		}
		CollectionInfo collectionInfo = collectionIriToConceptInfoMap.get(collection);

		TreeSet<String> newAlreadyAddedCollectionSet = new TreeSet<>(alreadyAddedCollectionSet);
		newAlreadyAddedCollectionSet.add(toQName(collectionInfo.getResourceIRI()));

		List<IRI> memberList = collectionToMemberListMap.get(collection);
		if(memberList!=null) {
			for (IRI member : memberList) {
				if (!collectionIriToConceptInfoMap.containsKey(member)) {
					collectionIriToConceptInfoMap.put(member, new CollectionInfo(member));
				}
				CollectionInfo memberCollection = collectionIriToConceptInfoMap.get(member);
				boolean added = collectionInfo.addMember(memberCollection);
				//if the member was already added in this hierarchy previously, it means that there is a cycle, so do not continue
				// with this hierarchy (but add it as a member of the current collection)
				// WARNING: even if this collection is not expanded further, since the CollectionInfo is considered, it means that its
				// members are already present in it, so be careful when this structure is analyzed for example to construct the xlsx file
				if (added && !newAlreadyAddedCollectionSet.contains(toQName(member))) {
					int depth = createCollectionHierarchy(member, collectionToMemberListMap, collectionIriToConceptInfoMap, currentDepth + 1,
							newAlreadyAddedCollectionSet);
					maxDepth = Math.max(maxDepth, depth);
				}
			}
		}
		return maxDepth;
	}

	private void addCollectionInfo(boolean isSkosxlLex, IRI collection, IRI propLex, Value xLabelValue, Literal label, IRI propNote,
			Value xNoteValue, Value noteValue, IRI prop, Value valueValue, Map<IRI, CollectionInfo> collectionIriToConceptInfoMap, HeaderWhole headerWhole,
			List<IRI> propLexList, List<IRI> propNoteList, boolean reifiedNote){
		//get the CollectionInfo from collectionIriToConceptInfoMap
		CollectionInfo collectionInfo = collectionIriToConceptInfoMap.get(collection);
		if(collectionInfo == null){
			//this in theory should never happen, so just return
			return;
		}
		addResourceInfo(collectionInfo, isSkosxlLex, propLex, xLabelValue, label, propNote, xNoteValue, noteValue, prop, valueValue, headerWhole,
				propLexList, propNoteList, reifiedNote);
	}

	/** GENERAL PART**/

	private String addValuesPart(String var, List<IRI> iriList){
		String queryPart = "\nVALUES("+var+") {";
		for(IRI iri : iriList){
			queryPart += " (<"+iri.stringValue()+">)";
		}
		queryPart+="}";
		return queryPart;
	}


	private void addLexicalizations(RepositoryConnection sourceRepositoryConnection, List<IRI> propLexList,
									List<IRI> classList, IRI lexModel, boolean isSkosxlLex, HeaderWhole headerWhole,
									Map<IRI, ConceptInfo> conceptIriToConceptInfoMap,
									Map<IRI, ConceptSchemeInfo> schemeIriToConceptSchemeInfoMap,
									Map<IRI, CollectionInfo> collectionIriToConceptInfoMap) {
		// @formatter:off
		String query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
				"\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> " +
				"\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"\nPREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"\nSELECT ?resource ?type ?propLex ?xlabel ?label " +
				"\nWHERE {" +
				addValuesPart("?type", classList) +
				"\n?resource a ?type ." +
				"\nFILTER(isIRI(?resource)) ";
		//add the part about the lexicalization
		if(lexModel.equals(Project.SKOS_LEXICALIZATION_MODEL)){
			query+= addValuesPart("?propLex", propLexList) +
					"\n?resource ?propLex ?label . " +
					"\nFILTER(isLiteral(?label))";
		} else { //lexModel.equals(Project.SKOSXL_LEXICALIZATION_MODEL)
			query+=addValuesPart("?propLex", propLexList) +
					"\n?resource ?propLex ?xlabel ." +
					"\n?xlabel skosxl:literalForm ?label ." +
					"\nFILTER(isLiteral(?label))";
		}
		query+="\n}";
		// @formatter:on
		TupleQuery tupleQuery = sourceRepositoryConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		try(TupleQueryResult tupleQueryResult = tupleQuery.evaluate()) {
			while (tupleQueryResult.hasNext()) {
				BindingSet bindingSet = tupleQueryResult.next();
				Value resourceValue = bindingSet.getValue("resource");
				if (!(resourceValue instanceof IRI)) {
					//skip this tuple, since the resource is not an IRI (this should never happen)
					continue;
				}
				IRI resource = (IRI) resourceValue;
				IRI propLex = bindingSet.hasBinding("propLex") ? (IRI) bindingSet.getValue("propLex") : null;
				Value xLabelValue = bindingSet.hasBinding("xlabel") ? bindingSet.getValue("xlabel") : null;
				Literal label = bindingSet.hasBinding("label") ? (Literal) bindingSet.getValue("label") : null;
				if (conceptIriToConceptInfoMap!=null) {
					addConceptInfo(isSkosxlLex, resource, propLex, xLabelValue, label, null, null, null, null, null,
							conceptIriToConceptInfoMap, headerWhole, propLexList, null, false);
				} else if (schemeIriToConceptSchemeInfoMap!=null){
					addSchemeInfo(isSkosxlLex, resource, propLex, xLabelValue, label, null, null, null, null, null,
							schemeIriToConceptSchemeInfoMap, headerWhole, propLexList, null, false);
				} else if (collectionIriToConceptInfoMap!=null){
					addCollectionInfo(isSkosxlLex, resource, propLex, xLabelValue, label, null, null, null, null, null,
							collectionIriToConceptInfoMap, headerWhole, propLexList, null, false);
				}

			}
		}
	}

	private void addReifiedNotes(RepositoryConnection sourceRepositoryConnection, List<IRI> classList,
								 HeaderWhole headerWhole, boolean isSkosxlLex,
								 List<IRI> notePropList, boolean reifiedNote,
								 Map<IRI, ConceptInfo> conceptIriToConceptInfoMap,
								 Map<IRI, ConceptSchemeInfo> schemeIriToConceptSchemeInfoMap,
								 Map<IRI, CollectionInfo> collectionIriToConceptInfoMap){
		if(!reifiedNote){
			//there are no reified note, so just return
			return;
		}
		// @formatter:off
		String query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
				"\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> " +
				"\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"\nPREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"\nSELECT ?resource ?type ?propNote ?xNote ?note  " +
				"\nWHERE {" +
				addValuesPart("?type", classList) +
				"\n?resource a ?type ." +
				"\nFILTER(isIRI(?resource)) "+
				addValuesPart("?propNote", notePropList) +
				"\n?resource ?propNote ?xNote ." +
				"\n?xNote rdf:value ?note ." +
				"\nFILTER(isLiteral(?note))" +
				"\n}";
		// @formatter:on
		TupleQuery tupleQuery = sourceRepositoryConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		try(TupleQueryResult tupleQueryResult = tupleQuery.evaluate()) {
			while(tupleQueryResult.hasNext()) {
				BindingSet bindingSet = tupleQueryResult.next();
				Value resourceValue = bindingSet.getValue("resource");
				if(!(resourceValue instanceof IRI)){
					//skip this tuple, since the resource is not an IRI (this should never happen)
					continue;
				}
				IRI resource = (IRI) resourceValue;
				IRI propNote = (IRI) bindingSet.getValue("propNote") ;
				Value xNoteValue =  bindingSet.getValue("xNote") ;
				Value noteValue = bindingSet.getValue("note") ;
				if (conceptIriToConceptInfoMap != null) {
					addConceptInfo(isSkosxlLex, resource, null, null, null, propNote, xNoteValue, noteValue, null, null,
							conceptIriToConceptInfoMap, headerWhole, null, notePropList, true);
				} else if (schemeIriToConceptSchemeInfoMap!=null){
					addSchemeInfo(isSkosxlLex, resource, null, null, null, propNote, xNoteValue, noteValue, null, null,
							schemeIriToConceptSchemeInfoMap, headerWhole, null, notePropList, true);
				} else if (collectionIriToConceptInfoMap!=null){
					addCollectionInfo(isSkosxlLex, resource, null, null, null, propNote, xNoteValue, noteValue, null, null,
							collectionIriToConceptInfoMap, headerWhole, null, notePropList, true);
				}

			}
		}
	}

	private void addGenericPropertyValue(RepositoryConnection sourceRepositoryConnection, List<IRI> classList,
										 boolean isSkosxlLex, List<IRI> propLexList, HeaderWhole headerWhole,
										 Map<IRI, ConceptInfo> conceptIriToConceptInfoMap,
										 Map<IRI, ConceptSchemeInfo> schemeIriToConceptSchemeInfoMap,
										 Map<IRI, CollectionInfo> collectionIriToConceptInfoMap){
		// @formatter:off
		String query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" +
				"\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> " +
				"\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"\nPREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"\nSELECT ?resource ?type ?prop ?value  " +
				"\nWHERE {" +
				addValuesPart("?type", classList) +
				"\n?resource a ?type ." +
				"\nFILTER(isIRI(?resource)) " +
				"\n?resource ?prop ?value ." +
				"\n}";
		// @formatter:on
		TupleQuery tupleQuery = sourceRepositoryConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		try(TupleQueryResult tupleQueryResult = tupleQuery.evaluate()) {
			while (tupleQueryResult.hasNext()) {
				BindingSet bindingSet = tupleQueryResult.next();
				Value resourceValue = bindingSet.getValue("resource");
				if (!(resourceValue instanceof IRI)) {
					//skip this tuple, since the concept is not an IRI (this should never happen since it is also
					// filtered in the query)
					continue;
				}
				IRI resource = (IRI) resourceValue;
				IRI prop = (IRI) bindingSet.getValue("prop");
				Value valueValue = bindingSet.getValue("value");
				if (conceptIriToConceptInfoMap != null) {
					addConceptInfo(isSkosxlLex, resource, null, null, null, null, null, null,
							prop, valueValue, conceptIriToConceptInfoMap, headerWhole, propLexList, null, false);
				} else if (schemeIriToConceptSchemeInfoMap!=null){
					addSchemeInfo(isSkosxlLex, resource, null, null, null, null, null, null,
							prop, valueValue, schemeIriToConceptSchemeInfoMap, headerWhole, propLexList, null, false);
				} else if (collectionIriToConceptInfoMap!=null){
					addCollectionInfo(isSkosxlLex, resource, null, null, null, null, null, null,
							prop, valueValue, collectionIriToConceptInfoMap, headerWhole, propLexList, null, false);
				}
			}
		}
	}

	private void addResourceInfo(ResourceInfo resourceInfo, boolean isSkosxlLex, IRI propLex, Value xLabelValue, Literal label, IRI propNote,
			Value xNoteValue, Value noteValue, IRI prop, Value valueValue, HeaderWhole headerWhole,
			List<IRI> propLexList, List<IRI> propNoteList, boolean reifiedNote){
		int count;
		//now add the value associated to the current ConceptInfo
		//add the type
		//if(type!=null) {
		//	addPropAndValue(resourceInfo, headerWhole, RDF.TYPE, new SimpleValue(toQName(type)), PropInfoAndValues.NO_LANG_TAG);
		//}

		//add the lexicalization
		if(propLex != null ) {
			String lang = label.getLanguage().isPresent() ? label.getLanguage().get() : PropInfoAndValues.NO_LANG_TAG;
			if (isSkosxlLex) {
				//since it is a skosxl lexicalization, consider propLex, xLabelValue and labelValue
				ReifiedValue reifiedValue;
				if (xLabelValue instanceof IRI) {
					reifiedValue = new ReifiedValue(toQName((IRI) xLabelValue), literalToNT(label));
					addPropAndValue(resourceInfo, headerWhole, propLex, reifiedValue, lang);
				} else if (xLabelValue instanceof BNode){ // BNODE
					reifiedValue = new ReifiedValue(toQName((BNode) xLabelValue), literalToNT(label));
					addPropAndValue(resourceInfo, headerWhole, propLex, reifiedValue, lang);
				}
			} else {
				//since it is a skos lexicalization, consider only propLex, and labelValue and leave xLabelValue
				addPropAndValue(resourceInfo, headerWhole, propLex, new SimpleValue(literalToNT(label)), lang);
			}
		}

		//add the reified note (if any)
		if(xNoteValue != null){
			String lang;
			String noteValueString;
			if(noteValue instanceof  Literal) {
				lang = ((Literal) noteValue).getLanguage().isPresent() ? ((Literal) noteValue).getLanguage().get() : PropInfoAndValues.NO_LANG_TAG;
				noteValueString = literalToNT((Literal) noteValue);
			} else {
				lang = PropInfoAndValues.NO_LANG_TAG;
				noteValueString = (noteValue instanceof IRI) ? toQName((IRI) noteValue) : noteValue.stringValue();
			}
			ReifiedValue reifiedValue;
			if (xNoteValue instanceof IRI) {
				reifiedValue = new ReifiedValue(toQName((IRI) xNoteValue), noteValueString);
				addPropAndValue(resourceInfo, headerWhole, propNote, reifiedValue, lang);
			} else if (xNoteValue instanceof BNode) {
				reifiedValue = new ReifiedValue(toQName((BNode) xNoteValue), noteValueString);
				addPropAndValue(resourceInfo, headerWhole, propNote, reifiedValue, lang);
			}
		}


		//add the other properties (avoid adding the skosxl labels if isSkosxlLex is true )

		if(prop!=null && valueValue!=null) {
			String valueValueString;
			String langValue = PropInfoAndValues.NO_LANG_TAG; // set to NO_LANG_TAG which can be changed later
			if (valueValue instanceof IRI) {
				valueValueString = toQName((IRI) valueValue);
			} else if (valueValue instanceof Literal) {
				valueValueString = literalToNT((Literal) valueValue);
				langValue = ((Literal) valueValue).getLanguage().isPresent() ? ((Literal) valueValue).getLanguage().get() : PropInfoAndValues.NO_LANG_TAG;
			} else { // BNODE
				valueValueString = toQName((BNode) valueValue);
			}
			// check if the prop and valueValue should be added
			boolean addPropValue = true;
			if(prop.equals(RDF.TYPE)) {
				// add the RDF.TYPE only if the valueValue is an IRI
				if (!(valueValue instanceof IRI)) {
					addPropValue = false;
				}
			}
			if (isSkosxlLex && propLexList.contains(prop)) {
				//do not add this property-value since it belong to the lexicalization part (SKOSXL)
				addPropValue = false;
			}
			if (reifiedNote && propNoteList.contains(prop) && !(valueValue instanceof Literal)) {
				//check if this property_value was already added as being part of a reified note value
				PropInfoAndValues propInfoAndValues = resourceInfo.getPropInfoAndValues(toQName(prop));
				if(propInfoAndValues!=null) {
					for (String lang : propInfoAndValues.getLangTagList()) {
						for (ValueForProp valueForProp : propInfoAndValues.getValueForPropListFromLang(lang)) {
							if (valueForProp instanceof ReifiedValue && ((ReifiedValue) valueForProp).getIriValue().equals(valueValueString)) {
								//this value as already being added as the IRI/Bnode of reified note, so do not add it as a SimpleValue
								addPropValue = false;
							}
						}
					}
				}
			}
			if (addPropValue) {
				addPropAndValue(resourceInfo, headerWhole, prop, new SimpleValue(valueValueString), langValue);
			}
		}
	}

	private String toQName(IRI iri){
		String qnameOrIri = ModelUtilities.getQName(iri, prefixDeclaraionMap);
		return  qnameOrIri.equals(iri.stringValue()) ? "<"+iri.stringValue()+">" : qnameOrIri;
	}

	private String toQName(BNode bNode) {
		return "_:"+bNode.stringValue();
	}

	private String literalToNT(Literal literal){
		return NTriplesUtil.toNTriplesString(literal);
	}

	private void addPropAndValue(ResourceInfo resourceInfo, HeaderWhole headerWhole, IRI prop, ValueForProp valueForProp, String lang){
		int count = resourceInfo.addPropWithValuesForProp(toQName(prop), lang, valueForProp);
		if(valueForProp instanceof SimpleValue) {
			headerWhole.updateNumForLangInSimpleProp(toQName(prop), lang, count);
		} else {
			headerWhole.updateNumForLangInReifiedProp(toQName(prop), lang, count);
		}
	}

}
