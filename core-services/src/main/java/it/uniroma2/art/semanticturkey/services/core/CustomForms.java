package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.ConverterException;
import it.uniroma2.art.coda.exception.RDFModelNotSetException;
import it.uniroma2.art.coda.exception.parserexception.PRParserException;
import it.uniroma2.art.coda.pearl.model.ConverterMention;
import it.uniroma2.art.coda.pearl.model.GraphElement;
import it.uniroma2.art.coda.pearl.model.GraphStruct;
import it.uniroma2.art.coda.pearl.model.OptionalGraphStruct;
import it.uniroma2.art.coda.pearl.model.PlaceholderStruct;
import it.uniroma2.art.coda.pearl.model.ProjectionRule;
import it.uniroma2.art.coda.pearl.model.ProjectionRulesModel;
import it.uniroma2.art.coda.pearl.model.annotation.Annotation;
import it.uniroma2.art.coda.pearl.model.annotation.AnnotationDefinition;
import it.uniroma2.art.coda.pearl.model.annotation.param.ParamValueDouble;
import it.uniroma2.art.coda.pearl.model.annotation.param.ParamValueInteger;
import it.uniroma2.art.coda.pearl.model.annotation.param.ParamValueInterface;
import it.uniroma2.art.coda.pearl.model.annotation.param.ParamValueIri;
import it.uniroma2.art.coda.pearl.model.annotation.param.ParamValueLiteral;
import it.uniroma2.art.coda.pearl.model.annotation.param.ParamValueString;
import it.uniroma2.art.coda.pearl.model.graph.GraphSingleElemPlaceholder;
import it.uniroma2.art.coda.pearl.model.graph.GraphSingleElemUri;
import it.uniroma2.art.coda.pearl.model.graph.GraphSingleElement;
import it.uniroma2.art.coda.pearl.parser.antlr4.AntlrParserRuntimeException;
import it.uniroma2.art.coda.provisioning.ComponentProvisioningException;
import it.uniroma2.art.semanticturkey.customform.BrokenCFStructure;
import it.uniroma2.art.semanticturkey.customform.CustomForm;
import it.uniroma2.art.semanticturkey.customform.CustomFormException;
import it.uniroma2.art.semanticturkey.customform.CustomFormGraph;
import it.uniroma2.art.semanticturkey.customform.CustomFormLevel;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.customform.CustomFormParseException;
import it.uniroma2.art.semanticturkey.customform.CustomFormXMLHelper;
import it.uniroma2.art.semanticturkey.customform.DuplicateIdException;
import it.uniroma2.art.semanticturkey.customform.FormCollection;
import it.uniroma2.art.semanticturkey.customform.FormsMapping;
import it.uniroma2.art.semanticturkey.customform.UserPromptStruct;
import it.uniroma2.art.semanticturkey.data.nature.NatureRecognitionOrchestrator;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Modified;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.resourceview.PredicateObjectsList;
import it.uniroma2.art.semanticturkey.services.core.resourceview.PredicateObjectsListSection;
import it.uniroma2.art.semanticturkey.services.core.resourceview.ResourceViewSection;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@STService
public class CustomForms extends STServiceAdapter {

	private static final Logger logger = LoggerFactory.getLogger(CustomForms.class);

	private final String ANN_DEF_PATH = "/it/uniroma2/art/semanticturkey/customform/annDef.pr";

	//Annotations
	private final String ANN_RANGE = "Range";
	private final String ANN_RANGELIST = "RangeList";
	private final String ANN_ROLE = "Role";
	private final String ANN_DATAONEOF = "DataOneOf";
	private final String ANN_OBJECTONEOF = "ObjectOneOf";
	private final String ANN_COLLECTION = "Collection";


	/**
	 * Returns a description of a graph created through a CustomForm. The description is based on the
	 * userPrompt fields of the CF that generated the graph. If no CustomFormGraph is found for the given
	 * custom form bearing resources, returns an empty description.
	 * 
	 * @param project
	 * @param codaCore
	 * @param cfManager
	 * @param repoConnection
	 * @param resource
	 * @param customFormBearingResources
	 * @param includeInferred
	 * @return
	 * @throws RDFModelNotSetException
	 * @throws PRParserException
	 */
	public static Map<String, ResourceViewSection> getResourceFormPreviewHelper(Project project,
			CODACore codaCore, CustomFormManager cfManager, RepositoryConnection repoConnection,
			Resource resource, Set<IRI> customFormBearingResources, boolean includeInferred)
			throws PRParserException {

		Map<String, ResourceViewSection> rv = new LinkedHashMap<>();

		// try to identify the CF which has generated the graph
		CustomFormGraph cfGraph = cfManager.getCustomFormGraphSeed(project, codaCore, repoConnection,
				resource, customFormBearingResources, includeInferred);
		if (cfGraph != null) {
			// retrieves, through a query, the values of those placeholders filled through a userPrompt in
			// the CustomForm
			Map<String, Value> promptValuesMap = new LinkedHashMap<>();
			String graphSection = cfGraph.getGraphSectionAsString(codaCore, true);
			Map<String, String> phUserPromptMap = cfGraph.getRelevantFormPlaceholders(codaCore);
			if (!phUserPromptMap.isEmpty()) {
				String bindings = "";
				for (String ph : phUserPromptMap.keySet()) {
					bindings += "?" + ph + " ";
				}
				String query = "SELECT " + bindings + " WHERE { " + graphSection + " }";
				logger.debug("query " + query);
				TupleQuery tq = repoConnection.prepareTupleQuery(query);
				tq.setIncludeInferred(includeInferred);
				tq.setBinding(cfGraph.getEntryPointPlaceholder(codaCore).substring(1), resource);
				try (TupleQueryResult result = tq.evaluate()) {
					// iterate over the results and create a map <userPrompt, value>
					while (result.hasNext()) {
						BindingSet bindingSet = result.next();
						for (Entry<String, String> phUserPrompt : phUserPromptMap.entrySet()) {
							String phId = phUserPrompt.getKey();
							String userPrompt = phUserPrompt.getValue();
							if (bindingSet.hasBinding(phId)) {
								promptValuesMap.put(userPrompt, bindingSet.getValue(phId));
							}
						}
					}
				}
			}

			// fill the predicate object list structure

			SimpleValueFactory vf = SimpleValueFactory.getInstance();

			Map<IRI, AnnotatedValue<IRI>> propMap = new LinkedHashMap<>();
			Multimap<IRI, AnnotatedValue<?>> valueMultiMap = HashMultimap.create();

			IRI cfNameProp = vf.createIRI(repoConnection.getNamespace(""), "@metadata-customFormName");

			AnnotatedValue<IRI> CFNameAnnotatedProp = new AnnotatedValue<>(cfNameProp);
			CFNameAnnotatedProp.setAttribute("role", RDFResourceRole.property.toString());
			CFNameAnnotatedProp.setAttribute("explicit", true);
			CFNameAnnotatedProp.setAttribute("show", "Custom form name");

			propMap.put(cfNameProp, CFNameAnnotatedProp);

			AnnotatedValue<Literal> cfName = new AnnotatedValue<>(vf.createLiteral(cfGraph.getName()));

			valueMultiMap.put(cfNameProp, cfName);
			
			for (Entry<String, Value> promptValue : promptValuesMap.entrySet()) {
				// create a "fake" predicate to represent the userPrompt label
				IRI predResource = vf.createIRI(repoConnection.getNamespace(""), promptValue.getKey());

				AnnotatedValue<IRI> annotatedPredicate = new AnnotatedValue<>(predResource);
				annotatedPredicate.setAttribute("role", RDFResourceRole.property.toString());
				annotatedPredicate.setAttribute("explicit", true);
				annotatedPredicate.setAttribute("show", promptValue.getKey());

				propMap.put(predResource, annotatedPredicate);

				AnnotatedValue<?> annotatedObject = new AnnotatedValue<>(promptValue.getValue());
				// I don't know how to retrieve role and other attributes
				annotatedObject.setAttribute("explicit", true);
				if (promptValue.getValue() instanceof Literal) {
					annotatedObject.setAttribute("show",
							NTriplesUtil.toNTriplesString(promptValue.getValue()));
				} else {
					annotatedObject.setAttribute("show", promptValue.getValue().stringValue());
				}
				valueMultiMap.put(predResource, annotatedObject);
			}

			// use a PredicateObjectList (although this is not a pred-obj list, but a "userPrompt"-value)
			// to exploit the serialization
			PredicateObjectsList predicateObjectsList = new PredicateObjectsList(propMap, valueMultiMap);
			rv.put("formBasedPreview", new PredicateObjectsListSection(predicateObjectsList));
		}
		return rv;
	}

	/**
	 * Removes a reified resource according to the CustomFormGraph that generated it
	 * 
	 * @param subject
	 * @param predicate
	 * @param resource
	 * @return
	 * @throws PRParserException
	 * @throws RDFModelNotSetException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#subject)+ ')', 'D')")
	public void removeReifiedResource(@Modified IRI subject, IRI predicate, IRI resource)
			throws PRParserException {
		RepositoryConnection repoConnection = getManagedConnection();
		removeReifiedValue(repoConnection, subject, predicate, resource);
	}

	@STServiceOperation
	@Read
	public JsonNode executeURIConverter(String converter, @Optional String value)
			throws ComponentProvisioningException, ConverterException {
		String result;
		CODACore codaCore = getInitializedCodaCore(getManagedConnection());
		if (value != null) {
			result = codaCore.executeURIConverter(converter, value).stringValue();
		} else {
			result = codaCore.executeURIConverter(converter).stringValue();
		}
		shutDownCodaCore(codaCore);
		return JsonNodeFactory.instance.textNode(result);
	}

	@STServiceOperation
	@Read
	public JsonNode executeLiteralConverter(String converter, String value, @Optional String datatype,
			@Optional String lang)
			throws ComponentProvisioningException, ConverterException {
		CODACore codaCore = getInitializedCodaCore(getManagedConnection());
		if (datatype != null && datatype.equals(""))
			datatype = null;
		if (lang != null && lang.equals(""))
			lang = null;
		String result = codaCore.executeLiteralConverter(converter, value, datatype, lang).stringValue();
		shutDownCodaCore(codaCore);

		return JsonNodeFactory.instance.textNode(result);
	}

	/*
	 * Custom Form mechanism management services
	 */

	// ========== Form Collection =================

	/**
	 * Returns the serialization of the FormCollection with the given id This serialization doesn't contain
	 * the effective forms, it contains just the id(s) of the forms.
	 * 
	 * @param id
	 * @return
	 * @throws CustomFormException
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('cform(formCollection, form)', 'R')")
	public JsonNode getFormCollection(String id) throws CustomFormException {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode formCollNode = jsonFactory.objectNode();
		FormCollection formColl = cfManager.getFormCollection(getProject(), id);
		if (formColl != null) {
			formCollNode.set("id", jsonFactory.textNode(formColl.getId()));

			ArrayNode formsColl = jsonFactory.arrayNode();
			for (CustomForm form : formColl.getForms()) {
				ObjectNode formNode = jsonFactory.objectNode();
				formNode.set("id", jsonFactory.textNode(form.getId()));
				formNode.set("level", jsonFactory.textNode(form.getLevel().toString()));
				formsColl.add(formNode);
			}
			formCollNode.set("forms", formsColl);

			ArrayNode suggestionsColl = jsonFactory.arrayNode();
			for (IRI sugg : formColl.getSuggestions()) {
				ObjectNode suggNode = jsonFactory.objectNode();
				suggNode.set("@id", jsonFactory.textNode(sugg.stringValue()));
				suggNode.set("nature", jsonFactory
						.textNode(NatureRecognitionOrchestrator.computeNature(sugg, getManagedConnection())));
				suggestionsColl.add(suggNode);
			}
			formCollNode.set("suggestions", suggestionsColl);

			return formCollNode;
		} else {
			throw new CustomFormException("FormCollection with id " + id + " not found");
		}
	}

	/**
	 * Returns all the FormCollections IDs available at system and (current) project level
	 * 
	 * @return
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('cform(formCollection, form)', 'R')")
	public JsonNode getAllFormCollections() {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode formCollArray = jsonFactory.arrayNode();
		Collection<FormCollection> formCollections = cfManager.getFormCollections(getProject());
		// form collections at system level
		for (FormCollection formColl : formCollections) {
			ObjectNode formCollNode = jsonFactory.objectNode();
			formCollNode.set("id", jsonFactory.textNode(formColl.getId()));
			formCollNode.set("level", jsonFactory.textNode(formColl.getLevel().toString()));
			formCollArray.add(formCollNode);
		}
		return formCollArray;
	}

	/**
	 * Creates an empty FormCollection (without CustomForms related)
	 * 
	 * @param id
	 * @return
	 * @throws DuplicateIdException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('cform(formCollection, form)', 'C')")
	public void createFormCollection(String id) throws DuplicateIdException {
		cfManager.createFormCollection(getProject(), id);
	}

	/**
	 * Creates a new FormCollection cloning an existing one
	 * 
	 * @param sourceId
	 *            id of the FC to clone
	 * @param targetId
	 *            id of the FC to create
	 * @return
	 * @throws CustomFormException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('cform(formCollection, form)', 'C')")
	public void cloneFormCollection(String sourceId, String targetId) throws CustomFormException {
		// look for FC at project level
		FormCollection sourceFC = cfManager.getFormCollection(getProject(), sourceId);
		if (sourceFC == null) { // if doesn't exist, look for it at system level
			throw new CustomFormException(
					"Impossible to clone '" + sourceId + "'. A FormCollection with this ID doesn't exists");
		}
		FormCollection newFC = cfManager.createFormCollection(getProject(), targetId);
		newFC.setForms(sourceFC.getForms());
		newFC.setSuggestions(sourceFC.getSuggestions());
		cfManager.updateFormCollection(getProject(), newFC, sourceFC.getForms(), sourceFC.getSuggestions());
	}

	/**
	 * Exports the {@link FormCollection} with the given id
	 * 
	 * @param oRes
	 * @param id
	 * @throws CustomFormException
	 * @throws IOException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('cform(formCollection, form)', 'R')")
	public void exportFormCollection(HttpServletResponse oRes, String id)
			throws CustomFormException, IOException {
		FormCollection formCollection = cfManager.getFormCollection(getProject(), id);
		if (formCollection == null) {
			throw new CustomFormException(
					"Impossible to export '" + id + "'. A FormCollection with this ID doesn't exists");
		}
		File tempServerFile = File.createTempFile("cfExport", ".xml");
		try {
			formCollection.save(tempServerFile);
			oRes.setHeader("Content-Disposition", "attachment; filename=" + id + ".xml");
			oRes.setContentType("application/xml");
			oRes.setContentLength((int) tempServerFile.length());
			try (InputStream is = new FileInputStream(tempServerFile)) {
				IOUtils.copy(is, oRes.getOutputStream());
			}
			oRes.flushBuffer();
		} finally {
			tempServerFile.delete();
		}
	}

	/**
	 * Imports a {@link FormCollection}
	 * 
	 * @param newId
	 *            id of the new FormCollection that will be created. If not provided, the created FC will have
	 *            the same ID of the loaded FC
	 * @throws IOException
	 * @throws CustomFormException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('cform(formCollection, form)', 'C')")
	public void importFormCollection(MultipartFile inputFile, @Optional String newId)
			throws IOException, CustomFormException {
		// create a temp file (in karaf data/temp folder) to copy the received file
		File tempServerFile = File.createTempFile("cfImport", inputFile.getOriginalFilename());
		try {
			inputFile.transferTo(tempServerFile);
			try {
				Collection<BrokenCFStructure> brokenCFS = new ArrayList<>();
				FormCollection parsedFormColl = CustomFormXMLHelper.parseAndCreateFormCollection(
						tempServerFile, cfManager.getCustomForms(getProject()), CustomFormLevel.project,
						brokenCFS);
				if (brokenCFS.size() != 0) {
					// if the form collection contains some error (reference to missing CF or duplicate ID)
					// throws an exception
					throw new CustomFormException("Failed to load the input FormCollection. Reason: "
							+ brokenCFS.iterator().next().getReason());
				}
				String newFormCollId;
				if (newId != null) {
					if (newId.contains(" ") || newId.trim().isEmpty()) { // check if ID is valid
						throw new CustomFormException("The provided ID '" + newId + "' is not valid."
								+ " It must not contain whitespaces");
					}
					newFormCollId = newId;
				} else {
					newFormCollId = parsedFormColl.getId();
				}
				FormCollection newFormColl = cfManager.createFormCollection(getProject(), newFormCollId);
				cfManager.updateFormCollection(getProject(), newFormColl, parsedFormColl.getForms(),
						parsedFormColl.getSuggestions());
			} catch (CustomFormParseException e) {
				throw new CustomFormException("Failed to parse the input file, it may contain some errors.");
			}
		} finally {
			tempServerFile.delete();
		}
	}

	/**
	 * Deletes the FormCollection with the given id
	 * 
	 * @param id
	 * @return
	 * @throws CustomFormException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('cform(formCollection, form)', 'D')")
	public void deleteFormCollection(String id) throws CustomFormException {
		FormCollection formColl = cfManager.getProjectFormCollection(getProject(), id);
		if (formColl == null) {
			throw new CustomFormException("FormCollection with ID '" + id + "' doesn't exist.");
		}
		cfManager.deleteFormCollection(getProject(), formColl);
	}

	/**
	 * Removes a CustomForm from an existing FormCollection
	 * 
	 * @param formCollectionId
	 * @param customFormIds
	 * @param suggestions
	 * @return
	 * @throws CustomFormException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('cform(formCollection, form)', 'U')")
	public void updateFromCollection(String formCollectionId, List<String> customFormIds,
			List<IRI> suggestions) throws CustomFormException {
		FormCollection formColl = cfManager.getProjectFormCollection(getProject(), formCollectionId);
		if (formColl == null) {
			throw new CustomFormException("FormCollection with ID " + formCollectionId + " doesn't exist");
		}
		List<CustomForm> customForms = new ArrayList<>();
		for (String cfId : customFormIds) {
			CustomForm cf = cfManager.getCustomForm(getProject(), cfId);
			if (cf == null) {
				throw new CustomFormException("CustomForm with ID " + cfId + " doesn't exist");
			}
			customForms.add(cf);
		}
		cfManager.updateFormCollection(getProject(), formColl, customForms, suggestions);
	}

	// ========== Custom Form =================

	/**
	 * Returns all the CustomForms available
	 * 
	 * @return
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('cform(form)', 'R')")
	public JsonNode getAllCustomForms() {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode customFormArrayNode = jsonFactory.arrayNode();
		Collection<CustomForm> cForms = cfManager.getCustomForms(getProject());
		for (CustomForm cf : cForms) {
			ObjectNode customFormNode = jsonFactory.objectNode();
			customFormNode.set("id", jsonFactory.textNode(cf.getId()));
			customFormNode.set("level", jsonFactory.textNode(cf.getLevel().toString()));
			customFormArrayNode.add(customFormNode);
		}
		return customFormArrayNode;
	}

	/**
	 * Returns the FormCollection (with available CustomForms to use as constructor) linked to the given
	 * resource. This method doesn't check if the resource is a class or a property (it wouldn't have sense to
	 * use it with properties since Properties.getRange() already list the CF to use for custom ranges).
	 * 
	 * @param resource
	 * @return
	 * @throws CustomFormException
	 */
	@STServiceOperation
	public JsonNode getCustomConstructors(IRI resource) {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		// Collection<CustomFormGraph> cForms = cfManager.getAllCustomFormGraphs(getProject(), resource);

		ObjectNode formCollNode = jsonFactory.objectNode();
		FormCollection formColl = cfManager.getFormCollection(getProject(), resource);
		if (formColl != null) {
			formCollNode.set("id", jsonFactory.textNode(formColl.getId()));
			ArrayNode formsArrayNode = jsonFactory.arrayNode();
			Collection<CustomFormGraph> cForms = formColl.getGraphForms();
			for (CustomForm customForm : cForms) {
				ObjectNode formObjectNode = jsonFactory.objectNode();
				formObjectNode.set("id", jsonFactory.textNode(customForm.getId()));
				formObjectNode.set("name", jsonFactory.textNode(customForm.getName()));
				formObjectNode.set("type", jsonFactory.textNode(customForm.getType()));
				formObjectNode.set("description", jsonFactory.textNode(customForm.getDescription()));
				formsArrayNode.add(formObjectNode);

			}
			formCollNode.set("forms", formsArrayNode);
		}
		return formCollNode;
	}

	/**
	 * Returns the serialization of the CustomForm with the given id
	 * 
	 * @param id
	 * @return
	 * @throws CustomFormException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('cform(form)', 'R')")
	public JsonNode getCustomForm(String id) throws CustomFormException {
		CustomForm cf = cfManager.getCustomForm(getProject(), id); // at project level
		if (cf != null) {
			JsonNodeFactory jsonFactory = JsonNodeFactory.instance;

			ObjectNode customFormNode = jsonFactory.objectNode();
			customFormNode.set("id", jsonFactory.textNode(cf.getId()));
			customFormNode.set("name", jsonFactory.textNode(cf.getName()));
			customFormNode.set("type", jsonFactory.textNode(cf.getType()));
			customFormNode.set("description", jsonFactory.textNode(cf.getDescription()));
			customFormNode.set("ref", jsonFactory.textNode(cf.getRef()));
			return customFormNode;
		} else {
			throw new CustomFormException(
					"CustomForm with id " + id + " not found in project " + getProject().getName());
		}
	}

	/**
	 * Returns a serialization representing the form of the CustomForm with the given id
	 * 
	 * @param id
	 * @return
	 * @throws PRParserException
	 * @throws RDFModelNotSetException
	 * @throws CustomFormException
	 */
	@STServiceOperation
	@Read
	// This service requires no authorization since the UI representation of a CF is necessary for prompting data
	public JsonNode getCustomFormRepresentation(String id) throws PRParserException, RDFModelNotSetException, CustomFormException {
		CustomForm cForm = cfManager.getCustomForm(getProject(), id);
		if (cForm != null) {
			CODACore codaCore = getInitializedCodaCore(getManagedConnection());
			Collection<UserPromptStruct> form = cForm.getForm(codaCore);
			shutDownCodaCore(codaCore);
			JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
			ArrayNode fieldListNode = jsonFactory.arrayNode();
			for (UserPromptStruct formEntry : form) {
				ObjectNode formEntryNode = jsonFactory.objectNode();
				formEntryNode.set("placeholderId", jsonFactory.textNode(formEntry.getPlaceholderId()));
				formEntryNode.set("featureName", jsonFactory.textNode(formEntry.getFeatureName()));
				formEntryNode.set("userPrompt", jsonFactory.textNode(formEntry.getUserPromptName()));
				formEntryNode.set("type", jsonFactory.textNode(formEntry.getRdfType()));
				formEntryNode.set("mandatory", jsonFactory.booleanNode(formEntry.isMandatory()));

				ConverterMention converter = formEntry.getConverter();
				ObjectNode converterNode = jsonFactory.objectNode();
				converterNode.set("uri", jsonFactory.textNode(converter.getURI()));

				// for special case langString, specify the converter argument too
				if (formEntry.getConverterArgPhId() != null) { //converter argument is a placeholder
					String phLangId = formEntry.getConverterArgPhId();
					/*
					 * the language placeholder (arguments of langString converter) is already added to the
					 * xml as formEntry element since in PEARL it must be defined before it's used as
					 * argument, so remove the element from formEntry and add it as argument of converter xml
					 * element
					 */
					for (JsonNode entry : fieldListNode) {
						if (entry.get("placeholderId").textValue().equals(phLangId)) {
							ObjectNode convArgNode = jsonFactory.objectNode();
							convArgNode.set("userPrompt",
									jsonFactory.textNode(entry.get("userPrompt").textValue()));
							converterNode.set("arg", convArgNode);
							break;
						}
					}
				} else if (formEntry.getConverterArgLangTag() != null) { //converter argument is a string (langTag)
					ObjectNode convArgNode = jsonFactory.objectNode();
					convArgNode.set("lang", jsonFactory.textNode(formEntry.getConverterArgLangTag()));
					converterNode.set("arg", convArgNode);
				}
				formEntryNode.set("converter", converterNode);

				if (formEntry.isLiteral()) {
					if (formEntry.hasDatatype()) {
						formEntryNode.set("datatype", jsonFactory.textNode(formEntry.getLiteralDatatype()));
					}
					if (formEntry.hasLanguage()) {
						formEntryNode.set("lang", jsonFactory.textNode(formEntry.getLiteralLang()));
					}
				}

				ArrayNode annotationsNode = jsonFactory.arrayNode();
				List<Annotation> annotationList = formEntry.getAnnotations();
				for (Annotation ann: formEntry.getAnnotations()) {
					ObjectNode annNode = jsonFactory.objectNode();
					String annName = ann.getName();
					annNode.set("name", jsonFactory.textNode(annName));
					/*
					 * Handle the known annotation params:
					 * - value: for most of the annotations
					 * - min & max: for List annotation
					 */
					Map<String, List<ParamValueInterface>> paramMap = ann.getParamMap();
					for (String paramName: paramMap.keySet()) {
						List<ParamValueInterface> paramValueList = ann.getParamValueList(paramName);
						ArrayNode paramValuesNode = jsonFactory.arrayNode();
						for (ParamValueInterface v : paramValueList) {
							if (v instanceof ParamValueInteger) {
								paramValuesNode.add(Integer.parseInt(v.toString()));
							} else if (v instanceof ParamValueDouble) {
								paramValuesNode.add(Double.parseDouble(v.toString()));
							} else {
								paramValuesNode.add(v.toString());
							}
						}
						annNode.set(paramName, paramValuesNode);
					}
					annotationsNode.add(annNode);
				}
				formEntryNode.set("annotations", annotationsNode);

				fieldListNode.add(formEntryNode);
			}
			return fieldListNode;
		} else {
			throw new CustomFormException(
					"CustomForm with id " + id + " not found in project " + getProject().getName());
		}
	}

	/**
	 * This service is thought to create a custom form from client. To be useful is necessary a proper UI for
	 * client support (e.g. a wizard) This is a POST because ref and description could be quite long for a GET
	 * parameter
	 * 
	 * @param type
	 * @param id
	 * @param name
	 * @param description
	 * @param ref
	 * @return
	 * @throws DuplicateIdException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('cform(form)', 'C')")
	public void createCustomForm(String type, String id, String name, String description, String ref) throws DuplicateIdException {
		// avoid proliferation of new line in saved pearl (carriage return character "\r" are added to ref
		// when calling this service
		if (type.equalsIgnoreCase(CustomForm.Types.node.toString())) {
			cfManager.createCustomFormNode(getProject(), id, name, description, ref);
		} else {
			cfManager.createCustomFormGraph(getProject(), id, name, description, ref);
		}
	}

	/**
	 * Creates a new CustomForm cloning an existing one
	 * 
	 * @param sourceId
	 *            id of the CF to clone
	 * @param targetId
	 *            id of the CF to create
	 * @return
	 * @throws CustomFormException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('cform(form)', 'C')")
	public void cloneCustomForm(String sourceId, String targetId) throws CustomFormException {
		CustomForm sourceCF = cfManager.getCustomForm(getProject(), sourceId);
		if (sourceCF == null) {
			throw new CustomFormException(
					"Impossible to clone '" + sourceId + "'. A CustomForm with this ID doesn't exists");
		}
		if (sourceCF.isTypeGraph()) {
			String ref = sourceCF.getRef();
			// replace in pearl rule the namespace
			ref = ref.replace(sourceId, targetId);
			// and the rule ID
			String sourceRuleId = "id:" + sourceId.substring(0, sourceId.lastIndexOf("."));
			String targetRuleId = "id:" + targetId.substring(0, sourceId.lastIndexOf("."));
			ref = ref.replace(sourceRuleId, targetRuleId);
			cfManager.createCustomFormGraph(getProject(), targetId, sourceCF.getName(),
					sourceCF.getDescription(), ref);
		} else { // type "node"
			cfManager.createCustomFormNode(getProject(), targetId, sourceCF.getName(),
					sourceCF.getDescription(), sourceCF.getRef());
		}
	}

	/**
	 * Exports the {@link CustomForm} with the given id
	 * 
	 * @param oRes
	 * @param id
	 * @throws CustomFormException
	 * @throws IOException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('cform(form)', 'R')")
	public void exportCustomForm(HttpServletResponse oRes, String id)
			throws CustomFormException, IOException {
		CustomForm customForm = cfManager.getCustomForm(getProject(), id);
		if (customForm == null) {
			throw new CustomFormException(
					"Impossible to export '" + id + "'. A CustomForm with this ID doesn't exists");
		}
		File tempServerFile = File.createTempFile("cfExport", ".xml");
		try {
			customForm.save(tempServerFile);
			oRes.setHeader("Content-Disposition", "attachment; filename=" + id + ".xml");
			oRes.setContentType("application/xml");
			oRes.setContentLength((int) tempServerFile.length());
			try (InputStream is = new FileInputStream(tempServerFile)) {
				IOUtils.copy(is, oRes.getOutputStream());
			}
			oRes.flushBuffer();
		} finally {
			tempServerFile.delete();
		}
	}

	/**
	 * Imports a {@link CustomForm}
	 * 
	 * @param newId
	 *            id of the new CustomForm that will be created. If not provided, the created CF will have the
	 *            same ID of the loaded CF
	 * @throws IOException
	 * @throws CustomFormException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('cform(form)', 'C')")
	public void importCustomForm(MultipartFile inputFile, @Optional String newId)
			throws IOException, CustomFormException {
		// create a temp file (in karaf data/temp folder) to copy the received file
		File tempServerFile = File.createTempFile("cfImport", inputFile.getOriginalFilename());
		try {
			inputFile.transferTo(tempServerFile);
			try {
				CustomForm parsedCustomForm = CustomFormXMLHelper.parseAndCreateCustomForm(tempServerFile);
				String newCustomFormId;
				String newRef;
				if (newId != null) {
					if (newId.contains(" ") || newId.trim().isEmpty()) { // check if ID is valid
						throw new CustomFormException("The provided ID '" + newId + "' is not valid."
								+ " It must not contain whitespaces");
					}
					newCustomFormId = newId;
					if (parsedCustomForm.isTypeGraph()) {
						newRef = parsedCustomForm.getRef();
						// replace in pearl rule the id
						newRef = newRef.replace(parsedCustomForm.getId(), newCustomFormId);
						// and the rule ID
						String sourceRuleId = "id:" + parsedCustomForm.getId().substring(0, parsedCustomForm.getId().lastIndexOf("."));
						String targetRuleId = "id:" + newCustomFormId.substring(0, newCustomFormId.lastIndexOf("."));
						newRef = newRef.replace(sourceRuleId, targetRuleId);
					} else { // node
						newRef = parsedCustomForm.getRef();
					}
				} else { // keep the old id
					newCustomFormId = parsedCustomForm.getId();
					newRef = parsedCustomForm.getRef();
				}
				if (parsedCustomForm.isTypeGraph()) {
					cfManager.createCustomFormGraph(getProject(), newCustomFormId,
							parsedCustomForm.getName(), parsedCustomForm.getDescription(), newRef);
				} else { // type "node"
					cfManager.createCustomFormNode(getProject(), newCustomFormId,
							parsedCustomForm.getName(), parsedCustomForm.getDescription(), newRef);
				}
			} catch (CustomFormParseException e) {
				throw new CustomFormException("Failed to parse the input file, it may contain some errors.");
			}
		} finally {
			tempServerFile.delete();
		}
	}

	/**
	 * Deletes the CustomForm with the given id. Removes also the CustomForm from the FormCollection(s) that
	 * contain it.
	 * 
	 * @param id
	 * @param deleteEmptyColl
	 *            if true deletes FormCollection that are left empty after the deletion
	 * @return
	 * @throws CustomFormException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('cform(form)', 'D')")
	public void deleteCustomForm(String id, @Optional(defaultValue = "false") boolean deleteEmptyColl)
			throws CustomFormException {
		CustomForm cf = cfManager.getProjectCustomForm(getProject(), id);
		if (cf == null) {
			throw new CustomFormException(
					"CustomForm with id " + id + " not found in project " + getProject().getName());
		}
		cfManager.deleteCustomForm(getProject(), cf, deleteEmptyColl);
	}

	/**
	 * Given the id of a CustomForm tells if it belong to a FormCollection. Useful as pre-check when it's
	 * deleting a CustomForm
	 * 
	 * @param id
	 * @return
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('cform(formCollection, form)', 'R')")
	public JsonNode isFormLinkedToCollection(String id) {
		// Since this is used before to delete a CustomForm, look only at project level, since is not possible
		// to delete at system level
		Collection<FormCollection> formCollections = cfManager.getProjectFormCollections(getProject());
		for (FormCollection formColl : formCollections) {
			if (formColl.getFormsId().contains(id)) {
				return JsonNodeFactory.instance.booleanNode(true);
			}
		}
		return JsonNodeFactory.instance.booleanNode(false);
	}

	/**
	 * Updates a CustomForm. Allows to change the name, the description, the ref and eventually (if the
	 * CustomForm is graph) the showProp of the CustomForm with the given id. It doesn't allow to change the
	 * type.
	 * 
	 * @param id
	 * @param name
	 * @param description
	 * @param ref
	 * @return
	 * @throws CustomFormException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('cform(form)', 'U')")
	public void updateCustomForm(String id, String name, String description, String ref) throws CustomFormException {
		CustomForm cf = cfManager.getProjectCustomForm(getProject(), id);
		if (cf == null) {
			throw new CustomFormException(
					"CustomForm with id " + id + " not found in project " + getProject().getName());
		}
		// avoid proliferation of new line in saved pearl (carriage return character "\r" are added to ref
		// when calling this service
		ref = ref.replace("\r", "");
		if (cf.isTypeGraph()) {
			cfManager.updateCustomFormGraph(getProject(), cf.asCustomFormGraph(), name, description, ref);
		} else {
			cfManager.updateCustomFormNode(getProject(), cf.asCustomFormNode(), name, description, ref);
		}
	}

	/**
	 * Tries to validate a pearl code.
	 * 
	 * @param pearl
	 *            rule to be parsed, it should be a whole pearl rule if the CustomForm is type graph or a
	 *            converter if type node
	 * @param formType
	 *            tells if the CF is type "node" or "graph". Determines also the nature of the pearl parameter
	 * @return
	 * @throws RDFModelNotSetException
	 * @throws CustomFormException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	public JsonNode validatePearl(String pearl, String formType) {
		JsonNodeFactory jf = JsonNodeFactory.instance;
		ObjectNode respNode = jf.objectNode();
		boolean pearlValid;
		String details = null;

		CODACore codaCore = getInitializedCodaCore(getManagedConnection());
		if (formType.equals(CustomForm.Types.graph.toString())) {
			try {
				//parse the pearl by adding first the PR of the annotations def, then the provided pearl
				codaCore.addProjectionRuleModel(CustomFormGraph.getAnnotationPearlStream());
				codaCore.addProjectionRuleModel(new ByteArrayInputStream(pearl.getBytes(StandardCharsets.UTF_8)), true);
				//if not exception is raised, the pearl is valid
				pearlValid = true;
			} catch (PRParserException e) {
				pearlValid = false;
				details = "Invalid pearl rule: " + e.getErrorAsString();
			} catch (AntlrParserRuntimeException e) {
				pearlValid = false;
				details = "Invalid pearl rule: " + e.getMsg();
			} finally {
				shutDownCodaCore(codaCore);
			}
		} else { // type node
			try {
				codaCore.parseProjectionOperator(pearl, Collections.emptyMap());
				shutDownCodaCore(codaCore);
				pearlValid = true;
			} catch (PRParserException e) {
				pearlValid = false;
				details = "Invalid projection operator";
			} catch (AntlrParserRuntimeException e) {
				pearlValid = false;
				details = "Invalid projection operator: " + e.getMsg();
			} finally {
				shutDownCodaCore(codaCore);
			}
		}
		respNode.set("valid", jf.booleanNode(pearlValid));
		respNode.set("details", jf.textNode(details));
		return respNode;
	}

	// ============== Forms Mapping ===================

	/**
	 * Returns all the resource-FormCollections mapping defined in the CustomForm configuration of the project
	 * 
	 * @return
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('cform(form, mapping)', 'R')")
	public JsonNode getCustomFormConfigMap() {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode cfcArrayNode = jsonFactory.arrayNode();
		Collection<FormsMapping> formMappings = cfManager.getProjectFormMappings(getProject());
		for (FormsMapping mapping : formMappings) {
			ObjectNode cfcNode = jsonFactory.objectNode();
			cfcNode.set("resource", jsonFactory.textNode(mapping.getResource()));

			ObjectNode formCollNode = jsonFactory.objectNode();
			formCollNode.set("id", jsonFactory.textNode(mapping.getFormCollection().getId()));
			formCollNode.set("level",
					jsonFactory.textNode(mapping.getFormCollection().getLevel().toString()));
			cfcNode.set("formCollection", formCollNode);

			cfcNode.set("replace", jsonFactory.booleanNode(mapping.getReplace()));
			cfcArrayNode.add(cfcNode);
		}
		return cfcArrayNode;
	}

	/**
	 * Adds a FormCollection to a resource, creating a FormsMapping
	 * 
	 * @param resource
	 * @param formCollId
	 * @param replace
	 * @return
	 * @throws CustomFormException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('cform(form, mapping)', 'C')")
	public void addFormsMapping(IRI resource, String formCollId,
			@Optional(defaultValue = "false") boolean replace) throws CustomFormException {
		FormCollection formColl = cfManager.getFormCollection(getProject(), formCollId);
		if (formColl == null) {
			throw new CustomFormException("FormCollection with ID " + formCollId + " doesn't exist");
		}
		cfManager.addFormsMapping(getProject(), resource, formColl, replace);
	}

	/**
	 * Remove the mapping between the given resource and the FormCollection linked to it
	 * 
	 * @param resource
	 * @return
	 * @throws CustomFormException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('cform(form, mapping)', 'D')")
	public void removeFormCollectionOfResource(IRI resource) {
		cfManager.removeFormsMapping(getProject(), resource);
	}

	/**
	 * Update the replace attribute of a FormCollectionMapping mapping for the given property
	 * 
	 * @param resource
	 * @param replace
	 * @return
	 * @throws CustomFormException
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('cform(formCollection, form)', 'U')")
	public void updateReplace(IRI resource, boolean replace) throws CustomFormException {
		cfManager.setReplace(getProject(), resource, replace);
	}

	// ================================================

	@STServiceOperation
	@PreAuthorize("@auth.isAuthorized('cform(form)', 'R')")
	public JsonNode getBrokenCustomForms() {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ArrayNode bcfArrayNode = jsonFactory.arrayNode();
		Collection<BrokenCFStructure> brokenCFS = cfManager.getBrokenCustomForms(getProject());
		for (BrokenCFStructure bcf : brokenCFS) {
			ObjectNode bcfNode = jsonFactory.objectNode();
			bcfNode.set("id", jsonFactory.textNode(bcf.getId()));
			bcfNode.set("type", jsonFactory.textNode(bcf.getType()));
			CustomFormLevel level = bcf.getLevel();
			if (level != null) {
				bcfNode.set("level", jsonFactory.textNode(bcf.getLevel().name()));
			} else {
				bcfNode.set("level", jsonFactory.textNode("---"));
			}
			bcfNode.set("file", jsonFactory.textNode(bcf.getFile().getName()));
			bcfNode.set("reason", jsonFactory.textNode(bcf.getReason()));
			bcfArrayNode.add(bcfNode);
		}
		return bcfArrayNode;
	}

	// ===================================================

	/**
	 * Update the PEARL part of a Custom Form according to some inferences by adding the needed annotations
	 * @param cfId the id of the Custom Form tu update
	 * @param prId the Optional Id of the Projection Rule in the Custom Form to update. If no id is specified, then all
	 *                Projection Rules are analyzed
	 * @param save set to true to save the updated PEARL in the desired Custom Form
	 * @return
	 * @throws CustomFormException
	 * @throws PRParserException
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAuthorized('cform(form)', 'U')")
	@Read
	public JsonNode updateCustomFormWithAnnotations(String cfId, @Optional String prId,
			@Optional(defaultValue = "true") boolean save) throws CustomFormException,
			PRParserException, IOException {
		CustomForm cf = cfManager.getCustomForm(getProject(), cfId); // at project level
		if (cf != null) {

			String ref = cf.getRef();

			RepositoryConnection repoConnection = getManagedConnection();
			CODACore codaCore = getInitializedCodaCore(repoConnection);
			codaCore.initialize(repoConnection);

			InputStream annDefStream = CustomFormGraph.class.getResourceAsStream(ANN_DEF_PATH);

			InputStream refStream = new ByteArrayInputStream(ref.getBytes());

			List<InputStream> inputStreamList = new ArrayList<>();
			inputStreamList.add(annDefStream);
			inputStreamList.add(refStream);

			codaCore.setAllProjectionRulelModelFromInputStreamList(inputStreamList);

			ProjectionRulesModel projectionRulesModel = codaCore.getProjRuleModel();

			List<String> annDefToExludeList = new ArrayList<>();
			annDefToExludeList.add("Memoized");
			annDefToExludeList.add("Confidence");
			annDefToExludeList.addAll(getAnnotationsDefFromFile(ANN_DEF_PATH));

			for (String currPrId : projectionRulesModel.getProjRule().keySet()){
				if(prId!=null && !prId.isEmpty() && !prId.equals(currPrId)){
					//this rule should not be analized
					continue;
				}
				ProjectionRule projectionRule = projectionRulesModel.getProjRuleFromId(currPrId);
				//add the annotations to the PEARL model
				addAnnRangeOrRole(projectionRule, projectionRulesModel, getManagedConnection());
				addAnnOneOf(projectionRule, projectionRulesModel, getManagedConnection());
				addAnnListMax(projectionRule, projectionRulesModel, getManagedConnection());
			}

			String modelAsString = projectionRulesModel.getModelAsString(annDefToExludeList);

			if (save) {
				//update the PEARL
				if (cf.isTypeGraph()) {
					cfManager.updateCustomFormGraph(getProject(), cf.asCustomFormGraph(), cf.getName(),
							cf.getDescription(), modelAsString);
				} else {
					cfManager.updateCustomFormNode(getProject(), cf.asCustomFormNode(), cf.getName(),
							cf.getDescription(), modelAsString);
				}

			}

			JsonNodeFactory jsonFactory = JsonNodeFactory.instance;

			ObjectNode customFormNode = jsonFactory.objectNode();
			customFormNode.set("id", jsonFactory.textNode(cf.getId()));
			customFormNode.set("name", jsonFactory.textNode(cf.getName()));
			customFormNode.set("type", jsonFactory.textNode(cf.getType()));
			customFormNode.set("description", jsonFactory.textNode(cf.getDescription()));
			customFormNode.set("ref", jsonFactory.textNode(modelAsString));

			return customFormNode;


		} else {
			throw new CustomFormException(
					"CustomForm with id " + cfId + " not found in project " + getProject().getName());
		}

	}


	/**
	 * Enrich the PEARL according to some inferences by adding the needed annotations
	 * @param cfPearl the PEARL text to enrich
	 * @return
	 * @throws PRParserException
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Read
	public String inferPearlAnnotations(String cfPearl) throws PRParserException, IOException {
		List<String> annDefToExludeList = new ArrayList<>();
		annDefToExludeList.add("Memoized");
		annDefToExludeList.add("Confidence");
		annDefToExludeList.addAll(getAnnotationsDefFromFile(ANN_DEF_PATH));

		RepositoryConnection repoConnection = getManagedConnection();
		CODACore codaCore = getInitializedCodaCore(repoConnection);
		codaCore.initialize(repoConnection);

		InputStream annDefStream = CustomFormGraph.class.getResourceAsStream(ANN_DEF_PATH);

		InputStream refStream = new ByteArrayInputStream(cfPearl.getBytes());

		List<InputStream> inputStreamList = new ArrayList<>();
		inputStreamList.add(annDefStream);
		inputStreamList.add(refStream);

		codaCore.setAllProjectionRulelModelFromInputStreamList(inputStreamList);

		ProjectionRulesModel projectionRulesModel = codaCore.getProjRuleModel();

		for(String currPrId : projectionRulesModel.getProjRule().keySet()){
			ProjectionRule projectionRule = projectionRulesModel.getProjRuleFromId(currPrId);
			//add the annotations to the PEARL model
			addAnnRangeOrRole(projectionRule, projectionRulesModel, getManagedConnection());
			addAnnOneOf(projectionRule, projectionRulesModel, getManagedConnection());
			addAnnListMax(projectionRule, projectionRulesModel, getManagedConnection());
			addAnnListMin(projectionRule, projectionRulesModel, getManagedConnection());
		}

		return projectionRulesModel.getModelAsString(annDefToExludeList);
	}


	private List<String> getAnnotationsDefFromFile(java.lang.String filePath) throws IOException {
		List<java.lang.String> annDefList = new ArrayList<>();

		InputStream inputStream = CustomFormGraph.class.getClassLoader().getResourceAsStream(filePath);

		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			java.lang.String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().startsWith("Annotation")) {
					java.lang.String annName = line.trim().split(" ")[1];
					if (annName.endsWith("{")) {
						annName = annName.substring(0, annName.length() - 1);

					}
					annDefList.add(annName);
				}
			}
		}
		return annDefList;
	}

	private void addAnnRangeOrRole(ProjectionRule projectionRule, ProjectionRulesModel projectionRulesModel, RepositoryConnection managedConnection) {
		AnnotationDefinition rangeAnnDef = projectionRulesModel.getAnnotationDefinition(ANN_RANGE);
		AnnotationDefinition rangeListAnnDef = projectionRulesModel.getAnnotationDefinition(ANN_RANGELIST);
		AnnotationDefinition roleAnnDef = projectionRulesModel.getAnnotationDefinition(ANN_ROLE);

		//iterate over each placeholder, if such placeholder is an IRI and does not already have ANN_RANGE or ANN_RANGELIST then
		//check if in the graph section there are any triples of the type  "PLACEHOLDER a CLASS"
		for(String plcName : projectionRule.getPlaceholderMap().keySet()){
			PlaceholderStruct placeholderStruct = projectionRule.getPlaceholderMap().get(plcName);
			if(!placeholderStruct.getRDFType().equals("uri")){
				//it not a placeholder hosting a URI, so just skip it
				continue;
			}
			//check if it alreday has the annotation ANN_RANGE or ANN_RANGELIST
			boolean skipAnnRangeAndRangeList = false;
			boolean skipAnnRole = false;
			for(Annotation annotation : placeholderStruct.getAnnotationList()){
				if(annotation.getName().equals(ANN_RANGE) || annotation.getName().equals(ANN_RANGELIST)){
					skipAnnRangeAndRangeList = true;
				}
				if(annotation.getName().equals(ANN_ROLE) ){
					skipAnnRole = true;
				}
			}
			//if both skipAnnRangeAndRangeList and skipAnnRole are true, just skip this placeholder
			if(skipAnnRangeAndRangeList && skipAnnRole ){
				continue;
			}

			List<IRI> classIriList = new ArrayList<>();
			List<String> roleList = new ArrayList<>();
			//now iterate over the triples in the graph section to search for "PLACEHOLDER a CLASS" for this placeholder
			Collection<GraphElement> graphElemList = projectionRule.getInsertGraphList();
			for(GraphElement graphElement : graphElemList){
				if(graphElement instanceof OptionalGraphStruct){
					continue;
				}
				GraphStruct graphStruct = (GraphStruct) graphElement;
				GraphSingleElement subj = graphStruct.getSubject();
				if(!(subj instanceof  GraphSingleElemPlaceholder) || !subj.getValueAsString().substring(1).equals(plcName)){
					//the subject of this triples is either not a placeholder or it is a different placeholder, so skip this triple
					continue;
				}
				if(graphStruct.getPredicate().getValueAsString().equals(NTriplesUtil.toNTriplesString(RDF.TYPE))){
					GraphSingleElement obj = graphStruct.getObject();
					if(obj instanceof GraphSingleElemUri) {
						//check if obj is a resource representing a specific role (only if skipAnnRole is false)
						List<String> roleListTemp = skipAnnRole ? new ArrayList<>() : getRoleFromIri(((GraphSingleElemUri) obj).getURI(), managedConnection);
						if(roleListTemp.isEmpty()) {
							//it not a role, so add it to the classIriList
							if(!skipAnnRangeAndRangeList) {
								classIriList.add(SimpleValueFactory.getInstance().createIRI(((GraphSingleElemUri) obj).getURI()));
							}
						} else {
							//it is a role, so add it to roleList
							roleList.addAll(roleListTemp);
						}
					}
				}
			}
			//add the ANN_RANGE or ANN_RANGELIST
			if(classIriList.size() == 1){
				//user ANN_RANGE
				//add the new annotation
				Annotation newAnnotaiton = new Annotation(ANN_RANGE, rangeAnnDef);
				List<ParamValueInterface> paramValueInterfaceList = new ArrayList<>();
				paramValueInterfaceList.add(new ParamValueIri(classIriList.get(0)));
				newAnnotaiton.addParams("value", paramValueInterfaceList);
				placeholderStruct.getAnnotationList().add(newAnnotaiton);
			} else if(classIriList.size()>1){
				//there are more than one value, so use ANN_RAGELIST
				Annotation newAnnotaiton = new Annotation(ANN_RANGELIST, rangeListAnnDef);
				List<ParamValueInterface> paramValueInterfaceList = new ArrayList<>();
				for (IRI classIri : classIriList) {
					paramValueInterfaceList.add(new ParamValueIri(classIri));
				}
				newAnnotaiton.addParams("value", paramValueInterfaceList);
				placeholderStruct.getAnnotationList().add(newAnnotaiton);
			}

			//add the ANN_ROLE
			if(roleList.size()>0){
				//there are more than one value, so use ANN_RAGELIST
				Annotation newAnnotaiton = new Annotation(ANN_ROLE, roleAnnDef);
				List<ParamValueInterface> paramValueInterfaceList = new ArrayList<>();
				for (String role : roleList) {
					paramValueInterfaceList.add(new ParamValueString(role));
				}
				newAnnotaiton.addParams("value", paramValueInterfaceList);
				placeholderStruct.getAnnotationList().add(newAnnotaiton);
			}
		}
	}

	private List<String> getRoleFromIri(String uri, RepositoryConnection managedConnection) {
		List<String> roleList = new ArrayList<>();

		String query =
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
				+"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+"PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
				+"PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>\n"
				+"PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
				+"SELECT ?rt \n"
				+ "WHERE {\n"
				+ "<"+uri+"> rdfs:subClassOf* $st .\n";

		//part taken from NatureRecognitionOrchestrator
		query += NatureRecognitionOrchestrator.complexIfPartForNature("?st", "?rt")
				+ "\n}";

		TupleQuery tupleQuery  = managedConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		try(TupleQueryResult queryResult = tupleQuery.evaluate()){
			while(queryResult.hasNext()){
				String role = queryResult.next().getValue("rt").stringValue();
				if(!roleList.contains(role) && !role.equals(RDFResourceRole.individual.name())){
					roleList.add(role);
				}
			}
		}
		return roleList;
	}



	private void addAnnOneOf(ProjectionRule projectionRule, ProjectionRulesModel projectionRulesModel, RepositoryConnection managedConnection) {
		AnnotationDefinition objectOneOfAnnDef = projectionRulesModel.getAnnotationDefinition(ANN_OBJECTONEOF);
		AnnotationDefinition dataOneOfAnnDef = projectionRulesModel.getAnnotationDefinition(ANN_DATAONEOF);

		//iterate over each placeholder, if such placeholder is an IRI and does not already have ANN_DATAONEOF or ANN_OBJECTONEOF then
		//check if in the graph section there are any triples of the type  "SUBJ PROP PLACEHOLDER" and in the repository
		// there is :
		// PROP rdf:range OBJ
		// OBJ owl:oneOf LIST

		for(String plcName : projectionRule.getPlaceholderMap().keySet()) {
			PlaceholderStruct placeholderStruct = projectionRule.getPlaceholderMap().get(plcName);
			if (!placeholderStruct.getRDFType().equals("uri")) {
				//it not a placeholder hosting a URI, so just skip it
				continue;
			}
			boolean skipAnnDataOneOf = false;
			boolean skipAnnObjectOneOf = false;
			for (Annotation annotation : placeholderStruct.getAnnotationList()) {
				if (annotation.getName().equals(ANN_DATAONEOF)) {
					skipAnnDataOneOf = true;
				}
				if (annotation.getName().equals(ANN_OBJECTONEOF)) {
					skipAnnObjectOneOf = true;
				}
			}

			List<Value> oneOfList = new ArrayList<>();
			//now iterate over the triples in the graph section to search for "SUBJ PROP PLACEHOLDER"  for this placeholder
			Collection<GraphElement> graphElemList = projectionRule.getInsertGraphList();
			for (GraphElement graphElement : graphElemList) {
				if (graphElement instanceof OptionalGraphStruct) {
					continue;
				}
				GraphStruct graphStruct = (GraphStruct) graphElement;
				GraphSingleElement obj = graphStruct.getObject();
				if (!(obj instanceof GraphSingleElemPlaceholder) || !obj.getValueAsString().substring(1).equals(plcName)) {
					//the object of this triples is either not a placeholder or it is a different placeholder, so skip this triple
					continue;
				}
				//do not consider the case where PROP is RDF.TYPE
				if (!graphStruct.getPredicate().getValueAsString().equals(NTriplesUtil.toNTriplesString(RDF.TYPE))) {
					GraphSingleElement pred = graphStruct.getPredicate();
					if (pred instanceof GraphSingleElemUri) {
						oneOfList.addAll(getOneOfListFromPred(((GraphSingleElemUri) pred).getURI(), managedConnection));
					}
				}
			}
			//if oneOfList is not empty, check if all its element are URI or Literal and, if their relative skip is not true,
			// create the appropriate annotation
			if(oneOfList.isEmpty()){
				//list is empty, nothing to do
				continue;
			}
			boolean allUri = true, allLiteral = true;
			for(Value value : oneOfList){
				if(value instanceof IRI){
					allLiteral = false;
				} else if(value instanceof Literal) {
					allUri = false;
				} else {
					allLiteral = false;
					allUri = false;
				}
			}
			if(allUri && !skipAnnObjectOneOf) {
				Annotation newAnnotaiton = new Annotation(ANN_OBJECTONEOF, objectOneOfAnnDef);
				List<ParamValueInterface> paramValueInterfaceList = new ArrayList<>();
				for (Value value : oneOfList) {
					paramValueInterfaceList.add(new ParamValueIri(((IRI)value)));
				}
				newAnnotaiton.addParams("value", paramValueInterfaceList);
				placeholderStruct.getAnnotationList().add(newAnnotaiton);
			} else if (allLiteral && !skipAnnDataOneOf) {
				Annotation newAnnotaiton = new Annotation(ANN_DATAONEOF, dataOneOfAnnDef);
				List<ParamValueInterface> paramValueInterfaceList = new ArrayList<>();
				for (Value value : oneOfList) {
					paramValueInterfaceList.add(new ParamValueLiteral(((Literal)value)));
				}
				newAnnotaiton.addParams("value", paramValueInterfaceList);
				placeholderStruct.getAnnotationList().add(newAnnotaiton);
			}
		}
	}

	private Collection<Value> getOneOfListFromPred(String uri, RepositoryConnection managedConnection) {
		//search for:
		// URI rdf:range OBJ
		// OBJ owl:oneOf LIST
		List<Value> oneOfList = new ArrayList<>();
		String query =
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
				+ "PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>\n"
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
				+ "SELECT ?value \n"
				+ "WHERE {\n"
				+ "<"+uri+"> rdfs:range ?obj .\n"
				+ "?obj owl:oneOf ?list .\n"
				+ "?list rdf:rest*/rdf:first ?value .\n"
				+"}";
		TupleQuery tupleQuery  = managedConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		try(TupleQueryResult queryResult = tupleQuery.evaluate()){
			while(queryResult.hasNext()){
				Value value = queryResult.next().getValue("value");
				if(!oneOfList.contains(value)){
					oneOfList.add(value);
				}
			}
		}

		return oneOfList;
	}

	private void addAnnListMax(ProjectionRule projectionRule, ProjectionRulesModel projectionRulesModel, RepositoryConnection managedConnection) {
		AnnotationDefinition listAnnDef = projectionRulesModel.getAnnotationDefinition(ANN_COLLECTION);

		//iterate over each placeholder, if such placeholder is an IRI and does not already have ANN_COLLECTION then
		//check if in the graph section there are any triples of the type  "SUBJ PROP PLACEHOLDER" and in the repository
		// there is :
		// PROP a owl:FunctionalProperty

		for(String plcName : projectionRule.getPlaceholderMap().keySet()) {
			PlaceholderStruct placeholderStruct = projectionRule.getPlaceholderMap().get(plcName);
			if (!placeholderStruct.getRDFType().equals("uri")) {
				//it not a placeholder hosting a URI, so just skip it
				continue;
			}
			boolean skipAnnList = false;
			for (Annotation annotation : placeholderStruct.getAnnotationList()) {
				if (annotation.getName().equals(ANN_COLLECTION)) {
					skipAnnList = true;
					break;
				}
			}

			if(skipAnnList){
				continue;
			}
			boolean isPropFunct = false;

			//now iterate over the triples in the graph section to search for "SUBJ PROP PLACEHOLDER"  for this placeholder
			Collection<GraphElement> graphElemList = projectionRule.getInsertGraphList();
			for (GraphElement graphElement : graphElemList) {
				if (graphElement instanceof OptionalGraphStruct) {
					continue;
				}
				GraphStruct graphStruct = (GraphStruct) graphElement;
				GraphSingleElement obj = graphStruct.getObject();
				if (!(obj instanceof GraphSingleElemPlaceholder) || !obj.getValueAsString().substring(1).equals(plcName)) {
					//the object of this triples is either not a placeholder or it is a different placeholder, so skip this triple
					continue;
				}
				//do not consider the case where PROP is RDF.TYPE
				if (!graphStruct.getPredicate().getValueAsString().equals(NTriplesUtil.toNTriplesString(RDF.TYPE))) {
					GraphSingleElement pred = graphStruct.getPredicate();
					if (pred instanceof GraphSingleElemUri) {
						if(isPropFunct(((GraphSingleElemUri) pred).getURI(), managedConnection)){
							isPropFunct = true;
						}
					}
				}
			}
			// create the appropriate annotation, if isPropFunct is true
			if(isPropFunct) {
				Annotation newAnnotaiton = new Annotation(ANN_COLLECTION, listAnnDef);
				List<ParamValueInterface> paramValueInterfaceList = new ArrayList<>();
				paramValueInterfaceList.add(new ParamValueInteger(1));
				newAnnotaiton.addParams("max", paramValueInterfaceList);
				placeholderStruct.getAnnotationList().add(newAnnotaiton);
			}
		}
	}

	private boolean isPropFunct(String uri, RepositoryConnection managedConnection) {
		//search for:
		// URI rdf:range OBJ
		// OBJ owl:oneOf LIST
		String query =
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
						+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
						+ "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
						+ "PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>\n"
						+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
						+ "SELECT ?type \n"
						+ "WHERE {\n"
						+ "<"+uri+"> a owl:FunctionalProperty .\n"
						+ "<"+uri+"> a ?type .\n"
						+"} \n" +
						"LIMIT 1";
		TupleQuery tupleQuery  = managedConnection.prepareTupleQuery(query);
		tupleQuery.setIncludeInferred(false);
		try(TupleQueryResult queryResult = tupleQuery.evaluate()){
			return queryResult.hasNext();
		}
	}

	private void addAnnListMin(ProjectionRule projectionRule, ProjectionRulesModel projectionRulesModel, RepositoryConnection managedConnection){
		AnnotationDefinition listAnnDef = projectionRulesModel.getAnnotationDefinition(ANN_COLLECTION);

		//iterate over each placeholder, if such placeholder does not already have ANN_COLLECTION then
		//check if it is not a mandatory placeholder, if so, set min=0

		for(String plcName : projectionRule.getPlaceholderMap().keySet()) {
			PlaceholderStruct placeholderStruct = projectionRule.getPlaceholderMap().get(plcName);
			boolean isMandatory = placeholderStruct.isMandatoryInGraphSection();
			if(isMandatory){
				//this is a mandatory placeholder, so just skip it
				continue;
			}
			boolean skipAnnList = false;
			for (Annotation annotation : placeholderStruct.getAnnotationList()) {
				if (annotation.getName().equals(ANN_COLLECTION)) {
					skipAnnList = true;
					break;
				}
			}

			if(skipAnnList){
				continue;
			}

			// create the appropriate annotation, with min=0
			Annotation newAnnotaiton = new Annotation(ANN_COLLECTION, listAnnDef);
			List<ParamValueInterface> paramValueInterfaceList = new ArrayList<>();
			paramValueInterfaceList.add(new ParamValueInteger(0));
			newAnnotaiton.addParams("min", paramValueInterfaceList);
			placeholderStruct.getAnnotationList().add(newAnnotaiton);
		}

	}


}
