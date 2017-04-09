package it.uniroma2.art.semanticturkey.services.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ProjectionRuleModelNotSet;
import it.uniroma2.art.coda.exception.UnassignableFeaturePathException;
import it.uniroma2.art.coda.structures.ARTTriple;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.constraints.LanguageTaggedString;
import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.constraints.NotLocallyDefined;
import it.uniroma2.art.semanticturkey.customform.CustomForm;
import it.uniroma2.art.semanticturkey.customform.CustomFormException;
import it.uniroma2.art.semanticturkey.customform.CustomFormGraph;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customform.StandardForm;
import it.uniroma2.art.semanticturkey.customform.UpdateTripleSet;
import it.uniroma2.art.semanticturkey.exceptions.CODAException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerationException;
import it.uniroma2.art.semanticturkey.plugin.extpts.URIGenerator;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Selection;
import it.uniroma2.art.semanticturkey.services.annotations.Write;

/**
 * This class provides services for manipulating SKOSXL constructs.
 * @author Tiziano
 *
 */
@STService
public class SKOSXL extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(SKOSXL.class);
	
	@Autowired
	private CustomFormManager cfManager;
	
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	public AnnotatedValue<IRI> createConcept(
			@Optional @NotLocallyDefined IRI newConcept, @Optional @LanguageTaggedString Literal label,
			@Optional @LocallyDefined @Selection Resource broaderConcept, @LocallyDefined IRI conceptScheme,
			@Optional String customFormId, @Optional Map<String, Object> userPromptMap)
					throws URIGenerationException, ProjectInconsistentException, CustomFormException, CODAException {
		
		Model modelAdditions = new LinkedHashModel();
		Model modelRemovals = new LinkedHashModel();
		
		IRI newConceptIRI;
		if (newConcept == null) {
			newConceptIRI = generateConceptIRI(label, conceptScheme);
		} else {
			newConceptIRI = newConcept;
		}
		
		modelAdditions.add(newConceptIRI, RDF.TYPE, org.eclipse.rdf4j.model.vocabulary.SKOS.CONCEPT); 
		if (label != null) {
			IRI xLabelIRI = generateXLabelIRI(newConceptIRI, label, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL);
			modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOSXL.PREF_LABEL, xLabelIRI);
			modelAdditions.add(xLabelIRI, RDF.TYPE, org.eclipse.rdf4j.model.vocabulary.SKOSXL.LABEL);
			modelAdditions.add(xLabelIRI, org.eclipse.rdf4j.model.vocabulary.SKOSXL.LITERAL_FORM, label);
		}
		modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.IN_SCHEME, conceptScheme);
		if (broaderConcept != null) {
			modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.BROADER, broaderConcept);
		} else {
			modelAdditions.add(newConceptIRI, org.eclipse.rdf4j.model.vocabulary.SKOS.TOP_CONCEPT_OF, conceptScheme);
		}

		//CustomForm further info
		RepositoryConnection repoConnection = getManagedConnection();
		CODACore codaCore = getInitializedCodaCore(repoConnection);
		if (customFormId != null && userPromptMap != null) {
			try {
				
				CustomForm cForm = cfManager.getCustomForm(getProject(), customFormId);
				if (cForm.isTypeGraph()){
					CustomFormGraph cfGraph = cForm.asCustomFormGraph();
					StandardForm stdForm = new StandardForm(
							newConceptIRI.stringValue(), label.getLabel(), label.getLanguage().orElse(null));
					UpdateTripleSet updates = cfGraph.executePearlForConstructor(codaCore, userPromptMap, stdForm);
					shutDownCodaCore(codaCore);
					
					for (ARTTriple t : updates.getInsertTriples()){
						modelAdditions.add(t.getSubject(), t.getPredicate(), t.getObject(), getWorkingGraph());
					}
					for (ARTTriple t : updates.getDeleteTriples()){
						modelRemovals.add(t.getSubject(), t.getPredicate(), t.getObject(), getWorkingGraph());
					}
				} else {
					throw new CustomFormException("Cannot execute CustomForm with id '" + cForm.getId()
						+ "' as constructor since it is not of type 'graph'");
				}
			} catch (ProjectionRuleModelNotSet | UnassignableFeaturePathException e){
				throw new CODAException(e);
			} finally {
				shutDownCodaCore(codaCore);
			}
		}
		repoConnection.add(modelAdditions, getWorkingGraph());
		repoConnection.remove(modelRemovals, getWorkingGraph());
		
		AnnotatedValue<IRI> annotatedConcept = new AnnotatedValue<IRI>(newConceptIRI);
		annotatedConcept.setAttribute("role", RDFResourceRolesEnum.concept.name());
		//TODO compute show
		return annotatedConcept; 
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
	 * @param type
	 *            the property used for attaching the label
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateXLabelIRI(IRI lexicalizedResource, Literal lexicalForm, IRI type) throws URIGenerationException {
		Map<String, Value> args = new HashMap<>();
		
		if (lexicalizedResource != null) {
			args.put(URIGenerator.Parameters.lexicalizedResource, lexicalizedResource);
		}

		if (lexicalForm != null) {
			args.put(URIGenerator.Parameters.lexicalForm, lexicalForm);
		}
		
		if (type != null) {
			args.put(URIGenerator.Parameters.type, type);
		}
		
		return generateIRI(URIGenerator.Roles.xLabel, args);
	}
	
	/**
	 * Generates a new URI for a SKOS concept, optionally given its accompanying preferred label and concept
	 * scheme. The actual generation of the URI is delegated to {@link #generateURI(String, Map)}, which in
	 * turn invokes the current binding for the extension point {@link URIGenerator}. In the end, the <i>URI
	 * generator</i> will be provided with the following:
	 * <ul>
	 * <li><code>concept</code> as the <code>xRole</code></li>
	 * <li>a map of additional parameters consisting of <code>label</code> and <code>scheme</code> (each, if
	 * not <code>null</code>)</li>
	 * </ul>
	 * 
	 * @param label
	 *            the preferred label accompanying the concept (can be <code>null</code>)
	 * @param scheme
	 *            the scheme to which the concept is being attached at the moment of its creation (can be
	 *            <code>null</code>)
	 * @return
	 * @throws URIGenerationException
	 */
	public IRI generateConceptIRI(Literal label, IRI scheme) throws URIGenerationException {
		Map<String, Value> args = new HashMap<>();

		if (label != null) {
			args.put(URIGenerator.Parameters.label, label);
		}

		if (scheme != null) {
			args.put(URIGenerator.Parameters.scheme, scheme);
		}

		return generateIRI(URIGenerator.Roles.concept, args);
	}
	
}