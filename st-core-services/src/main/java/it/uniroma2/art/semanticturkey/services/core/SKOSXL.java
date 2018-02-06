package it.uniroma2.art.semanticturkey.services.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import it.uniroma2.art.semanticturkey.constraints.LanguageTaggedString;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.AlreadyExistingLiteralFormForResourceException;
import it.uniroma2.art.semanticturkey.exceptions.PrefAltLabelClashException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.DisplayName;
import it.uniroma2.art.semanticturkey.services.annotations.Modified;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;

/**
 * This class provides services for manipulating SKOSXL constructs.
 * 
 * TODO: here there are: generateConceptIRI, generateConceptSchemeIRI, generateCollectionIRI
 * that are implemented also in SKOS service class.
 * Whould be better move this method in a STSKOSServiceAdapter (that extends STServiceAdapter)? 
 * 
 * @author Tiziano
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 *
 */
@STService
public class SKOSXL extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(SKOSXL.class);
	
	public static enum XLabelCreationMode {
		bnode, uri
	};
	
	/**
	 * Generates a new URI for a SKOSXL Label, based on the provided mandatory parameters. The actual
	 * generation of the URI is delegated to {@link #generateURI(String, Map)}, which in turn invokes the
	 * current binding for the extension point {@link URIGenerator}. In the end, the <i>URI generator</i> will
	 * be provided with the following:
	 * <ul>
	 * <li><code>xLabel</code> as the <code>xRole</code></li>
	 * <li>a map of additional parameters consisting of <code>lexicalForm</code>,
	 * <code>lexicalizedResource</code> and <code>type</code> (each, if not <code>null</code>)</li>
	 * </ul>
	 * 
	 * All arguments should be not <code>null</code>, but in the end is the specific implementation of the
	 * extension point that would complain about the absence of one of these theoretically mandatory
	 * parameters.
	 * 
	 * @param lexicalForm
	 *            the textual content of the label
	 * @param lexicalizedResource
	 *            the resource to which the label will be attached to
	 * @param lexicalizationProperty
	 *            the property used for attaching the label
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateXLabelIRI(Resource lexicalizedResource, Literal lexicalForm,
			IRI lexicalizationProperty) throws URIGenerationException {
		Map<String, Value> args = new HashMap<>();

		if (lexicalizedResource != null) {
			args.put(URIGenerator.Parameters.lexicalizedResource, lexicalizedResource);
		}

		if (lexicalForm != null) {
			args.put(URIGenerator.Parameters.lexicalForm, lexicalForm);
		}

		if (lexicalizationProperty != null) {
			args.put(URIGenerator.Parameters.lexicalizationProperty, lexicalizationProperty);
		}

		return generateIRI(URIGenerator.Roles.xLabel, args);
	}
	
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#concept)+ ', lexicalization)', 'R')")
	public Collection<AnnotatedValue<Resource>> getPrefLabel(@LocallyDefined IRI concept, @Optional(defaultValue="*") String lang){
		Collection<AnnotatedValue<Resource>> resultsList = new ArrayList<>();
		
		RepositoryConnection repoConnection = getManagedConnection();
		
		// @formatter:off
		String query = "SELECT ?prefLabel ?label" +
				"\nWHERE{" + 
				"?concept "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL)+" ?prefLabel ." +
				"?prefLabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" ?label ." +
				"\n}";
		// @formatter:on
		TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQuery.setBinding("concept", concept);
		try(TupleQueryResult queryResult = tupleQuery.evaluate()){
			BindingSet bindingSet = queryResult.next();
			Resource prefLabel = (Resource) bindingSet.getBinding("prefLabel").getValue();
			Literal label = (Literal) bindingSet.getBinding("label").getValue();
			if(lang.equals("*") || (label.getLanguage().isPresent() && label.getLanguage().get().equals(lang))){
				AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>(prefLabel);
				annotatedValue.setAttribute("role", RDFResourceRole.xLabel.name());
				if(label.getLanguage().isPresent()){
					annotatedValue.setAttribute("lang", label.getLanguage().get());
				}
				annotatedValue.setAttribute("show", label.getLabel());
				resultsList.add(annotatedValue);
			}
		}
		return resultsList;
	}
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#concept)+ ', lexicalization)', 'R')")
	public Collection<AnnotatedValue<Resource>> getAltLabels(@LocallyDefined IRI concept, @Optional(defaultValue="*") String lang){
		Collection<AnnotatedValue<Resource>> resultsList = new ArrayList<>();
		
		RepositoryConnection repoConnection = getManagedConnection();
		
		// @formatter:off
		String query = "SELECT ?prefLabel ?label" +
				"\nWHERE{" + 
				"?concept "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL)+" ?prefLabel ." +
				"?prefLabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" ?label ." +
				"\n}";
		// @formatter:on
		TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQuery.setBinding("concept", concept);
		try(TupleQueryResult queryResult = tupleQuery.evaluate()){
			BindingSet bindingSet = queryResult.next();
			Resource prefLabel = (Resource) bindingSet.getBinding("prefLabel").getValue();
			Literal label = (Literal) bindingSet.getBinding("label").getValue();
			if(lang.equals("*") || (label.getLanguage().isPresent() && label.getLanguage().get().equals(lang))){
				AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>(prefLabel);
				annotatedValue.setAttribute("role", RDFResourceRole.xLabel.name());
				if(label.getLanguage().isPresent()){
					annotatedValue.setAttribute("lang", label.getLanguage().get());
				}
				annotatedValue.setAttribute("show", label.getLabel());
				resultsList.add(annotatedValue);
			}
		}
		return resultsList;
	}
	
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#concept)+ ', lexicalization)', 'R')")
	public Collection<AnnotatedValue<Resource>> getHiddenLabels(@LocallyDefined IRI concept, @Optional(defaultValue="*") String lang){
		Collection<AnnotatedValue<Resource>> resultsList = new ArrayList<>();
		
		RepositoryConnection repoConnection = getManagedConnection();
		
		// @formatter:off
		String query = "SELECT ?prefLabel ?label" +
				"\nWHERE{" + 
				"?concept "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.HIDDEN_LABEL)+" ?prefLabel ." +
				"?prefLabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" ?label ." +
				"\n}";
		// @formatter:on
		TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQuery.setBinding("concept", concept);
		try(TupleQueryResult queryResult = tupleQuery.evaluate()){
			BindingSet bindingSet = queryResult.next();
			Resource prefLabel = (Resource) bindingSet.getBinding("prefLabel").getValue();
			Literal label = (Literal) bindingSet.getBinding("label").getValue();
			if(lang.equals("*") || (label.getLanguage().isPresent() && label.getLanguage().get().equals(lang))){
				AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<Resource>(prefLabel);
				annotatedValue.setAttribute("role", RDFResourceRole.xLabel.name());
				if(label.getLanguage().isPresent()){
					annotatedValue.setAttribute("lang", label.getLanguage().get());
				}
				annotatedValue.setAttribute("show", label.getLabel());
				resultsList.add(annotatedValue);
			}
		}
		return resultsList;
	}
	
	/**
	 * this service adds an alternative label to a concept 
	 * 
	 * 
	 * @param concept
	 * @param literal
	 * @param mode
	 *            bnode or uri: if uri a URI generator is used to create the URI for the xlabel
	 * @return
	 * @throws URIGenerationException 
	 * @throws AlreadyExistingLiteralFormForResourceException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#concept)+ ', lexicalization)', '{lang: ''' +@auth.langof(#literal)+ '''}', 'C')")
	@DisplayName("add alternative label")
	public void addAltLabel(@LocallyDefined @Modified IRI concept, Literal literal, XLabelCreationMode mode) 
			throws URIGenerationException, AlreadyExistingLiteralFormForResourceException {
		RepositoryConnection repoConnection = getManagedConnection();
		checkIfAddAltLabelIsPossible(repoConnection, literal, concept);
		Model modelAdditions = new LinkedHashModel();
		
		Resource xlabel;
		if (mode == XLabelCreationMode.bnode) {
			xlabel = repoConnection.getValueFactory().createBNode();
		} else{
			xlabel = generateXLabelIRI(concept, literal, org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL);
		}
		modelAdditions.add(repoConnection.getValueFactory().createStatement(concept, 
				org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL, xlabel));
		modelAdditions.add(repoConnection.getValueFactory().createStatement(xlabel, 
				RDF.TYPE, org.eclipse.rdf4j.model.vocabulary.SKOSXL.LABEL));
		modelAdditions.add(repoConnection.getValueFactory().createStatement(xlabel, 
				org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM, literal));
		repoConnection.add(modelAdditions, getWorkingGraph());
	}
	
	
	/**
	 * this service adds an hidden label to a concept 
	 * 
	 * 
	 * @param concept
	 * @param literal
	 * @param mode
	 *            bnode or uri: if uri a URI generator is used to create the URI for the xlabel
	 * @return
	 * @throws URIGenerationException 
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#concept)+ ', lexicalization)', '{lang: ''' +@auth.langof(#literal)+ '''}', 'C')")
	@DisplayName("add hidden label")
	public void addHiddenLabel(@LocallyDefined @Modified IRI concept, @LanguageTaggedString Literal literal, 
			XLabelCreationMode mode) throws URIGenerationException {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		
		Resource xlabel;
		if (mode == XLabelCreationMode.bnode) {
			xlabel = repoConnection.getValueFactory().createBNode();
		} else{
			xlabel = generateXLabelIRI(concept, literal, org.eclipse.rdf4j.model.vocabulary.SKOSXL.HIDDEN_LABEL);
		}
		modelAdditions.add(repoConnection.getValueFactory().createStatement(concept, 
				org.eclipse.rdf4j.model.vocabulary.SKOSXL.HIDDEN_LABEL, xlabel));
		modelAdditions.add(repoConnection.getValueFactory().createStatement(xlabel, 
				RDF.TYPE, org.eclipse.rdf4j.model.vocabulary.SKOSXL.LABEL));
		modelAdditions.add(repoConnection.getValueFactory().createStatement(xlabel, 
				org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM, literal));
		
		repoConnection.add(modelAdditions, getWorkingGraph());
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	// TODO @PreAuthorize
	public void prefToAtlLabel(@LocallyDefined @Modified IRI concept, @LocallyDefined Resource xlabel){
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		modelRemovals.add(repoConnection.getValueFactory().createStatement(concept, 
				org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL, xlabel));
		modelAdditions.add(repoConnection.getValueFactory().createStatement(concept, 
				org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL, xlabel));
		
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	// TODO @PreAuthorize
	public void altToPrefLabel(@LocallyDefined @Modified IRI concept, @LocallyDefined Resource xlabel){
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		//check if there is already a prefLabel for the given language (take it from the input xlabel)
		// @formatter:off
		String query = "select ?oldPrefLabel " +
					"\nWHERE{" +
					"\n?concept "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL)+"?xlabel ."+
					"\n?xlabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" ?label ."+
					"\nBIND(lang(?label) AS ?lang)"+
					"\n?concept "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL)+" ?oldPrefLabel ."+
					"\n?oldPrefLabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" ?oldLabel ."+
					"\nFILTER(lang(oldLabel) = ?lang)"+
					"\n}";
		// @formatter:on
		TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQuery.setBinding("concept", concept);
		tupleQuery.setBinding("xlabel", xlabel);
		try(TupleQueryResult tupleQueryResult = tupleQuery.evaluate()){
			if(tupleQueryResult.hasNext()){
				//if there is already a prefLabel, change it to altLabel
				Resource oldPrefLabel = (Resource) tupleQueryResult.next().getValue("oldPrefLabel");
				modelRemovals.add(repoConnection.getValueFactory().createStatement(concept, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL, oldPrefLabel));
				modelAdditions.add(repoConnection.getValueFactory().createStatement(concept, org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL, oldPrefLabel));
			}
			modelRemovals.add(repoConnection.getValueFactory().createStatement(concept,org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL, xlabel));
			modelAdditions.add(repoConnection.getValueFactory().createStatement(concept, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL, xlabel));
		}
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#concept)+ ', lexicalization)', '{lang: ''' +@auth.langof(#literal)+ '''}', 'C')")
	@DisplayName("set preferred label")
	public void setPrefLabel(@LocallyDefined @Modified IRI concept, @LanguageTaggedString Literal literal,
			XLabelCreationMode mode, @Optional(defaultValue="true") boolean checkExistingAltLabel) 
					throws URIGenerationException, AlreadyExistingLiteralFormForResourceException, PrefAltLabelClashException{
		RepositoryConnection repoConnection = getManagedConnection();
		if(checkExistingAltLabel) {
			checkIfPrefAltLabelClash(repoConnection, literal, concept);
		}
		checkIfAddPrefLabelIsPossible(repoConnection, literal, concept, false);
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		String lang = literal.getLanguage().get();
		
		//check if there is already a prefLabel for the given language (take it from the input literal)
		// @formatter:off
		String query = "select ?oldPrefLabel " +
					"\nWHERE{" +
					"\n?concept "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL)+" ?oldPrefLabel ."+
					"\n?oldPrefLabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" ?oldLabel ."+
					"\nFILTER(lang(?oldLabel) = \""+lang+"\")"+
					"\n}";
		// @formatter:on
		TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		tupleQuery.setBinding("concept", concept);
		try(TupleQueryResult tupleQueryResult = tupleQuery.evaluate()){
			if(tupleQueryResult.hasNext()){
				//if there is already a prefLabel, change it to altLabel
				Resource oldPrefLabel = (Resource) tupleQueryResult.next().getValue("oldPrefLabel");
				modelRemovals.add(repoConnection.getValueFactory().createStatement(concept, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL, oldPrefLabel));
				modelAdditions.add(repoConnection.getValueFactory().createStatement(concept, org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL, oldPrefLabel));
			}
			
			Resource xlabel;
			if (mode == XLabelCreationMode.bnode) {
				xlabel = repoConnection.getValueFactory().createBNode();
			} else{
				xlabel = generateXLabelIRI(concept, literal, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL);
			}
			modelAdditions.add(repoConnection.getValueFactory().createStatement(concept, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL, xlabel));
			modelAdditions.add(repoConnection.getValueFactory().createStatement(xlabel, RDF.TYPE, org.eclipse.rdf4j.model.vocabulary.SKOSXL.LABEL));
			modelAdditions.add(repoConnection.getValueFactory().createStatement(xlabel, org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM, literal));
		}
		
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#concept)+ ', lexicalization)', '{lang: ''' +@auth.langof(#xlabel)+ '''}', 'D')")
	public void removePrefLabel(@LocallyDefined @Modified IRI concept, @LocallyDefined Resource xlabel){
		RepositoryConnection repoConnection = getManagedConnection();
		
		// @formatter:off
		String query = "DELETE {" +
				"\nGRAPH ?g {?xlabel ?p1 ?o1 . }"+
				"\nGRAPH ?g {?s2 ?p2 ?xlabel . }"+
				"\n}" + 
				"\nWHERE{" +
				"GRAPH ?g {?concept "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL)+" ?xlabel .}" +
				
				"{GRAPH ?g {?xlabel ?p1 ?o1 .}}" +
				"\nUNION" +
				"{GRAPH ?g {?s2 ?p2 ?xlabel  .}}" +
				
				"\n}";
		// @formatter:on
		Update update = repoConnection.prepareUpdate(query);
		update.setBinding("concept", concept);
		update.setBinding("xlabel", xlabel);
		update.setBinding("g", getWorkingGraph());
		update.execute();
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#concept)+ ', lexicalization)', '{lang: ''' +@auth.langof(#xlabel)+ '''}', 'D')")
	public void removeAltLabel(@LocallyDefined @Modified IRI concept, @LocallyDefined Resource xlabel){
		RepositoryConnection repoConnection = getManagedConnection();
		
		// @formatter:off
		String query = "DELETE {" +
				"\nGRAPH ?g {?xlabel ?p1 ?o1 . }"+
				"\nGRAPH ?g {?s2 ?p2 ?xlabel . }"+
				"\n}" + 
				"\nWHERE{" +
				"GRAPH ?g {?concept "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL)+" ?xlabel .}" +
				
				"{GRAPH ?g {?xlabel ?p1 ?o1 .}}" +
				"\nUNION" +
				"{GRAPH ?g {?s2 ?p2 ?xlabel  .}}" +
				
				"\n}";
		// @formatter:on
		Update update = repoConnection.prepareUpdate(query);
		update.setBinding("concept", concept);
		update.setBinding("xlabel", xlabel);
		update.setBinding("g", getWorkingGraph());
		update.execute();
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#concept)+ ', lexicalization)', '{lang: ''' +@auth.langof(#xlabel)+ '''}', 'D')")
	public void removeHiddenLabel(@LocallyDefined @Modified IRI concept, @LocallyDefined Resource xlabel){
		RepositoryConnection repoConnection = getManagedConnection();
		
		// @formatter:off
		String query = "DELETE {" +
				"\nGRAPH ?g {?xlabel ?p1 ?o1 . }"+
				"\nGRAPH ?g {?s2 ?p2 ?xlabel . }"+
				"\n}" + 
				"\nWHERE{" +
				"GRAPH ?g {?concept "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.HIDDEN_LABEL)+" ?xlabel .}" +
				
				"{GRAPH ?g {?xlabel ?p1 ?o1 .}}" +
				"\nUNION" +
				"{GRAPH ?g {?s2 ?p2 ?xlabel  .}}" +
				
				"\n}";
		// @formatter:on
		Update update = repoConnection.prepareUpdate(query);
		update.setBinding("concept", concept);
		update.setBinding("xlabel", xlabel);
		update.setBinding("g", getWorkingGraph());
		update.execute();
	}
	
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	// TODO @PreAuthorize
	public void changeLabelInfo(@LocallyDefined @Modified Resource xlabel, @LanguageTaggedString Literal literal){
		RepositoryConnection repoConnection = getManagedConnection();
		
		// @formatter:off
		String query = 
				"DELETE {"+
				"\nGRAPH ?g {?xlabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" ?oldLiteral .}" +
				"\n}"+
						
				"INSERT {"+
				"\nGRAPH ?g {?xlabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" "+NTriplesUtil.toNTriplesString(literal)+" .}" +
				"\n}"+
				
				"\nWHERE{"+
				"?s ?p ?xlabel ." +
				"\nOPTIONAL{" +
				"\nGRAPH ?g {?xlabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" ?oldLiteral .}" +
				"\n}" +
				"\n}";
		// @formatter:on
		Update update = repoConnection.prepareUpdate(query);
		update.setBinding("g", getWorkingGraph());
		update.setBinding("xlabel", xlabel);
		update.execute();
		
	}
	
	public static void checkIfAddPrefLabelIsPossible(RepositoryConnection repoConnection, Literal newLabel, 
			Resource resource, boolean newResource) throws AlreadyExistingLiteralFormForResourceException{
		//see if there is no other resource that has a prefLabel with the same Literal or that the resource 
		// to which the Literal will be added has not already an alternative label with the input
		String query = "ASK {"+
				"\n{?resource "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL)+" "+
					"?xlabel ."+
				"\n?xlabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" "+
					NTriplesUtil.toNTriplesString(newLabel)+" . }"+
				"\nUNION"+
				"\n{"+NTriplesUtil.toNTriplesString(resource)+" "+
					NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL)+" "+
					"?xlabel ."+
				"\n?xlabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" "+
					NTriplesUtil.toNTriplesString(newLabel)+" . }";	
				//see the type to check
		query+="\n}";
		
		BooleanQuery booleanQuery = repoConnection.prepareBooleanQuery(query);
		booleanQuery.setIncludeInferred(false);
		boolean check = booleanQuery.evaluate();
		if(check){
			String text;
			if(!newResource) {
				text = "prefLabel "+NTriplesUtil.toNTriplesString(newLabel)+" cannot be created since either "
					+ "there is already a resource with the same prefLabel or this resource has already an altLabel "
					+ "with the same value";
			} else {
				text = "prefLabel "+NTriplesUtil.toNTriplesString(newLabel)+" cannot be created since "
						+ "there is already a resource with the same prefLabel";
			}
			throw new AlreadyExistingLiteralFormForResourceException(text);
		}
	}
	
	public static void checkIfPrefAltLabelClash(RepositoryConnection repoConnection, Literal newLabel, 
			Resource resource) throws PrefAltLabelClashException{
		//see if there is no other resource that has a altLabel with the same Literal 
		String query = "ASK {"+
				"\n?resource "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL)+" "+
					"?xlabel ."+
				"\n?xlabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" "+
					NTriplesUtil.toNTriplesString(newLabel)+" . "+
				"\n}";
		
		BooleanQuery booleanQuery = repoConnection.prepareBooleanQuery(query);
		booleanQuery.setIncludeInferred(false);
		boolean check = booleanQuery.evaluate();
		if(check){
			String text = "WARNING: prefLabel "+NTriplesUtil.toNTriplesString(newLabel)+" cannot be created since "
					+ "there is already a resource with the same altLabel.";
			throw new PrefAltLabelClashException(text);
		}
	}
	
	public static void checkIfAddAltLabelIsPossible(RepositoryConnection repoConnection, Literal newLabel, 
			Resource resource) throws AlreadyExistingLiteralFormForResourceException{
		//see if the resource to which the Literal will be added has not already a pref label or an  
		// alternative label with the input
		String query = "ASK {"+
				"\n{"+NTriplesUtil.toNTriplesString(resource)+" "+
					NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL)+" "+
					"?xlabel ."+
				"\n?xlabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" "+
					NTriplesUtil.toNTriplesString(newLabel)+" . }"+
				"\nUNION"+
				"\n{"+NTriplesUtil.toNTriplesString(resource)+" "+
					NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL)+" "+
				"?xlabel ."+
					"\n?xlabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" "+
					NTriplesUtil.toNTriplesString(newLabel)+" . }";	
				//see the type to check
		query+="\n}";
		
		BooleanQuery booleanQuery = repoConnection.prepareBooleanQuery(query);
		booleanQuery.setIncludeInferred(false);
		if(booleanQuery.evaluate()){
			String text = "altLabel "+NTriplesUtil.toNTriplesString(newLabel)+" cannot be created since this "
					+ "resource already has a prefLabel or an altLabel with the same value";
			throw new AlreadyExistingLiteralFormForResourceException(text);
		}
	}
	
}