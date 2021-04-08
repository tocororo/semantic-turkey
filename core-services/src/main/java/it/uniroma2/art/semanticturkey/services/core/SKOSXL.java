package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.semanticturkey.constraints.LanguageTaggedString;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.SubClassOf;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.exceptions.PrefAltLabelClashException;
import it.uniroma2.art.semanticturkey.exceptions.PrefPrefLabelClashException;
import it.uniroma2.art.semanticturkey.extension.extpts.urigen.URIGenerationException;
import it.uniroma2.art.semanticturkey.extension.extpts.urigen.URIGenerator;
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
import it.uniroma2.art.semanticturkey.services.annotations.logging.TermCreation;
import it.uniroma2.art.semanticturkey.services.annotations.logging.TermCreation.Facets;
import it.uniroma2.art.semanticturkey.services.aspects.ResourceLevelChangeMetadataSupport;
import it.uniroma2.art.semanticturkey.services.core.SKOS.MessageKeys;
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
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private static final Logger logger = LoggerFactory.getLogger(SKOSXL.class);
	
	public enum XLabelCreationMode {
		bnode, uri
	}
	
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
				AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<>(prefLabel);
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
				"?concept "+ NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL)+" ?prefLabel ." +
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
				AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<>(prefLabel);
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
				AnnotatedValue<Resource> annotatedValue = new AnnotatedValue<>(prefLabel);
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
	 * @throws PrefPrefLabelClashException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#concept)+ ', lexicalization)', '{lang: ''' +@auth.langof(#literal)+ '''}', 'C')")
	@DisplayName("add alternative label")
	@TermCreation(label="literal", concept="concept", facet=Facets.ALT_LABEL)
	public void addAltLabel(@LocallyDefined @Modified IRI concept, @LanguageTaggedString Literal literal,
			@Optional @LocallyDefined @SubClassOf(superClassIRI = "http://www.w3.org/2008/05/skos-xl#Label") IRI labelCls,
			XLabelCreationMode mode) 
			throws URIGenerationException, PrefAltLabelClashException {
		RepositoryConnection repoConnection = getManagedConnection();
		checkIfAddAltLabelIsPossible(repoConnection, literal, concept);
		Model modelAdditions = new LinkedHashModel();
		
		Resource xlabel;
		if (mode == XLabelCreationMode.bnode) {
			xlabel = repoConnection.getValueFactory().createBNode();
		} else{
			xlabel = generateXLabelIRI(concept, literal, org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL);
			ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addCreatedResource(xlabel,
					RDFResourceRole.xLabel); // set created for versioning
		}
		if (labelCls == null) {
			labelCls = org.eclipse.rdf4j.model.vocabulary.SKOSXL.LABEL;
		}
		modelAdditions.add(repoConnection.getValueFactory().createStatement(concept, 
				org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL, xlabel));
		modelAdditions.add(repoConnection.getValueFactory().createStatement(xlabel, RDF.TYPE, labelCls));
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
	@TermCreation(label="literal", concept="concept", facet=Facets.HIDDEN_LABEL)
	public void addHiddenLabel(@LocallyDefined @Modified IRI concept, @LanguageTaggedString Literal literal, 
			@Optional @LocallyDefined @SubClassOf(superClassIRI = "http://www.w3.org/2008/05/skos-xl#Label") IRI labelCls,
			XLabelCreationMode mode) throws URIGenerationException {
		RepositoryConnection repoConnection = getManagedConnection();
		Model modelAdditions = new LinkedHashModel();
		
		Resource xlabel;
		if (mode == XLabelCreationMode.bnode) {
			xlabel = repoConnection.getValueFactory().createBNode();
		} else{
			xlabel = generateXLabelIRI(concept, literal, org.eclipse.rdf4j.model.vocabulary.SKOSXL.HIDDEN_LABEL);
			ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addCreatedResource(xlabel,
					RDFResourceRole.xLabel); // set created for versioning
		}
		if (labelCls == null) {
			labelCls = org.eclipse.rdf4j.model.vocabulary.SKOSXL.LABEL;
		}
		modelAdditions.add(repoConnection.getValueFactory().createStatement(concept, 
				org.eclipse.rdf4j.model.vocabulary.SKOSXL.HIDDEN_LABEL, xlabel));
		modelAdditions.add(repoConnection.getValueFactory().createStatement(xlabel, RDF.TYPE, labelCls));
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
	@TermCreation(label="literal", concept="concept", facet=Facets.PREF_LABEL)
	public void setPrefLabel(@LocallyDefined @Modified IRI concept, @LanguageTaggedString Literal literal,
			 @Optional @LocallyDefined @SubClassOf(superClassIRI = "http://www.w3.org/2008/05/skos-xl#Label") IRI labelCls,
			 XLabelCreationMode mode, @Optional(defaultValue="true") boolean checkExistingAltLabel,
			 @Optional(defaultValue="true") boolean checkExistingPrefLabel)
					throws URIGenerationException, PrefPrefLabelClashException, PrefAltLabelClashException{
		RepositoryConnection repoConnection = getManagedConnection();
		if(checkExistingPrefLabel){
			List<IRI> conceptSchemeList = SKOS.getAllSchemesForConcept(concept, repoConnection);
			checkIfAddPrefLabelIsPossible(repoConnection, literal, false, conceptSchemeList);
		}
		if(checkExistingAltLabel) {
			checkIfPrefAltLabelClash(repoConnection, literal, concept);
		}
		

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
			while(tupleQueryResult.hasNext()){
				//if there is already a prefLabel, change it to altLabel (do for all prefLabels)
				Resource oldPrefLabel = (Resource) tupleQueryResult.next().getValue("oldPrefLabel");
				modelRemovals.add(repoConnection.getValueFactory().createStatement(concept, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL, oldPrefLabel));
				modelAdditions.add(repoConnection.getValueFactory().createStatement(concept, org.eclipse.rdf4j.model.vocabulary.SKOSXL.ALT_LABEL, oldPrefLabel));
			}
			
			Resource xlabel;
			if (mode == XLabelCreationMode.bnode) {
				xlabel = repoConnection.getValueFactory().createBNode();
			} else{
				xlabel = generateXLabelIRI(concept, literal, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL);
				ResourceLevelChangeMetadataSupport.currentVersioningMetadata().addCreatedResource(xlabel,
						RDFResourceRole.xLabel); // set created for versioning
			}
			
			if (labelCls == null) {
				labelCls = org.eclipse.rdf4j.model.vocabulary.SKOSXL.LABEL;
			}
			modelAdditions.add(repoConnection.getValueFactory().createStatement(concept, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL, xlabel));
			modelAdditions.add(repoConnection.getValueFactory().createStatement(xlabel, RDF.TYPE, labelCls));
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
													 boolean newResource, List<IRI> conceptSchemes)
					throws PrefPrefLabelClashException {
		//see if there is no other resource that has a prefLabel with the same Literal in the same Scheme
		// @formatter:off
		String query = "SELECT ?resource {"+
				"\n?resource "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL)+" "+
					"?xlabel ."+
				"\n?xlabel "+NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM)+" "+
					NTriplesUtil.toNTriplesString(newLabel)+" .";
		//if at least one concept scheme is passed, filter the ?resource to that scheme(s)
		if(conceptSchemes!=null && conceptSchemes.size()>0) {
			query+="\n?subPropInScheme "+
					NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.RDFS.SUBPROPERTYOF)+"* "+
					NTriplesUtil.toNTriplesString(org.eclipse.rdf4j.model.vocabulary.SKOS.IN_SCHEME)+" .";
			if(conceptSchemes.size()==1) {
				//since it is a single scheme, there is no need to use the FILTER, check check the triple
				query+="\n?resource ?subPropInScheme "+NTriplesUtil.toNTriplesString(conceptSchemes.get(0))+" .";
			}else {
				//since there are at least two schemes, use the filter
				boolean first=true;
				query+="\n?resource ?subPropInScheme ?scheme ."+
						"\nFILTER(";
				for(IRI scheme : conceptSchemes) {
					if(!first) {
						query+= " || ";
					}
					first = false;
					query+="?scheme="+NTriplesUtil.toNTriplesString(scheme);
				}
				query+=")";
			}
		}
		query+="\n}" +
		"\nLIMIT 1";
		// @formatter:on
		logger.debug("query: " + query);

		TupleQuery tupleQuery = repoConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
		if(tupleQueryResult.hasNext()){
			Value otherResource = tupleQueryResult.next().getValue("resource");
			throw new PrefPrefLabelClashException(
					newResource ? MessageKeys.exceptionUnableToAddPrefLabelForNew$message
							: MessageKeys.exceptionUnableToAddPrefLabel$message,
					new Object[] { NTriplesUtil.toNTriplesString(newLabel),
							NTriplesUtil.toNTriplesString(otherResource) });
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
			throw new PrefAltLabelClashException(MessageKeys.exceptionAltLabelClash$message,
					new Object[] { NTriplesUtil.toNTriplesString(newLabel) });
		}
	}
	
	public static void checkIfAddAltLabelIsPossible(RepositoryConnection repoConnection, Literal newLabel, 
			Resource resource) throws PrefAltLabelClashException {
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
			throw new PrefAltLabelClashException(
					MessageKeys.exceptionUnableToAddAltLabel$message,
					new Object[] { NTriplesUtil.toNTriplesString(newLabel) });
		}
	}
	
}