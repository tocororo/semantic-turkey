package it.uniroma2.art.semanticturkey.services.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import it.uniroma2.art.semanticturkey.exceptions.shacl.SHACLMultipleTargetShapeFromClassIRIException;
import it.uniroma2.art.semanticturkey.exceptions.shacl.SHACLGenericException;
import it.uniroma2.art.semanticturkey.exceptions.shacl.SHACLNoTargetShapeFromClassIriException;
import it.uniroma2.art.semanticturkey.exceptions.shacl.SHACLNotEnabledException;
import it.uniroma2.art.semanticturkey.exceptions.shacl.SHACLTargetShapeNotExistingException;
import it.uniroma2.art.semanticturkey.ontology.OntologyManagerException;
import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.config.InvalidConfigurationException;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionException;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ReformattingException;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;

/**
 * This class provides services for manipulating SHACL constructs.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
@STService
public class SHACL extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(SHACL.class);

	private static final String SH_NAMESPACE = "http://www.w3.org/ns/shacl#";

	private static final String SH_NODESHAPE = SH_NAMESPACE+"NodeShape";

	private static final String SH_MAXCOUNT = SH_NAMESPACE + "maxCount";
	private static final String SH_MINCOUNT = SH_NAMESPACE + "minCount";
	private static final String SH_IN = SH_NAMESPACE + "in";
	private static final String SH_TARGETCLASS = SH_NAMESPACE +"targetClass";
	private static final String SH_PROPERTY = SH_NAMESPACE+"property";
	private static final String SH_PATH = SH_NAMESPACE+"path";
	private static final String SH_CLASS = SH_NAMESPACE+"class";
	private static final String SH_HASVALUE = SH_NAMESPACE+"hasValue";
	private static final String SH_DATATYPE = SH_NAMESPACE + "datatype";


	private static final String FEATPATH_MAIN = "stdForm/resource";

	//Annotations
	private static final String ANN_COLLECTION = "@Collection";
	private static final String ANN_RANGE = "@Range";
	private static final String ANN_OBJECTONEOF = "@ObjectOneOf";
	private static final String ANN_DATAONEOF = "@DataOneOf";

	//User Prompt for the CF PEARL
	final String USERPROMT = "userPrompt/";

	/**
	 * Loads SHACL shapes into the SHACL Shape Graph associated with the contextual project. Existing shapes
	 * are deleted by default, but this behavior can be overridden.
	 * 
	 * @param shapesFile
	 * @param fileFormat
	 * @param clearExisting
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(shacl)', 'CU')")
	public void loadShapes(MultipartFile shapesFile, RDFFormat fileFormat,
			@Optional(defaultValue = "false") boolean clearExisting) throws IOException {
		File inputServerFile = File.createTempFile("loadShapes", shapesFile.getOriginalFilename());
		try {
			shapesFile.transferTo(inputServerFile);
			RepositoryConnection con = getManagedConnection();
			if (clearExisting) {
				con.clear(RDF4J.SHACL_SHAPE_GRAPH);
			}
			con.add(inputServerFile, null, fileFormat, RDF4J.SHACL_SHAPE_GRAPH);
		} finally {
			FileUtils.deleteQuietly(inputServerFile);
		}
	}

	/**
	 * Exports the shapes currently stored in the SHACL Shape Graph associated with the contextual project.
	 * The output format is by default pretty printed TURTLE, but this behavior can be overridden. For the
	 * configuration options, please see
	 * {@link it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.rdfserializer.RDFSerializingExporter}
	 * 
	 * @param oRes
	 * @param rdfFormat
	 * @param exporterConfiguration
	 * @throws ReformattingException
	 * @throws IOException
	 * @throws InvalidConfigurationException
	 * @throws STPropertyAccessException
	 * @throws WrongPropertiesException
	 * @throws NoSuchExtensionException
	 * @throws IllegalArgumentException
	 */
	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(shacl)', 'R')")
	public void exportShapes(HttpServletResponse oRes, @Optional(defaultValue = "TURTLE") RDFFormat rdfFormat,
			@Optional(defaultValue = "{\"prettyPrint\": true, \"inlineBlankNodes\": true}") ObjectNode exporterConfiguration)
			throws IllegalArgumentException, NoSuchExtensionException, WrongPropertiesException,
			STPropertyAccessException, InvalidConfigurationException, IOException, ReformattingException {
		ObjectNode exporterConfigurationJson = STPropertiesManager.createObjectMapper()
				.valueToTree(exporterConfiguration);
		PluginSpecification reformattingExporterSpec = new PluginSpecification(
				"it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.rdfserializer.RDFSerializingExporter",
				null, null, exporterConfigurationJson);
		Export.formatAndThenDownloadOrDeploy(exptManager, stServiceContext, oRes,
				new IRI[] { RDF4J.SHACL_SHAPE_GRAPH }, false, rdfFormat.getName(), null,
				getManagedConnection(), reformattingExporterSpec);
	}

	/**
	 * Delete existing shapes. This operation clears the SHACL Shape Graph associated with the contextual
	 * project.
	 * 
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(shacl)', 'D')")
	public void clearShapes() throws IOException {
		getManagedConnection().clear(RDF4J.SHACL_SHAPE_GRAPH);
	}


	@STServiceOperation(method = RequestMethod.POST)
	@Read
	//@PreAuthorize("@auth.isAuthorized('rdf(shacl)', 'R')") //TODO
	public JsonNode extractCFfromShapeFile(IRI classIri, MultipartFile shapesFile, @Optional RDFFormat fileFormat,
			@Optional IRI targetShape) throws IOException, OntologyManagerException, SHACLGenericException {
		//if fileForma is null, try to guess it
		if(fileFormat==null) {
			fileFormat = Rio.getParserFormatForFileName(shapesFile.getOriginalFilename())
					.orElseThrow(() -> new OntologyManagerException(
							"Could not match a parser for file name: " + shapesFile.getOriginalFilename()));
		}
		//create a temporary repository
		//create a temp file (in karaf data/temp folder) to copy the received file
		String fileName = shapesFile.getOriginalFilename();
		File serverShaclFile = File.createTempFile("shacl", fileName.substring(fileName.lastIndexOf(".")));
		BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(serverShaclFile));
		byte[] bytes = shapesFile.getBytes();
		stream.write(bytes);
		stream.close();

		Repository rep = new SailRepository(new MemoryStore());
		rep.init();
		String pearlFileText="";
		try(RepositoryConnection connection = rep.getConnection();) {
			connection.add(serverShaclFile, getProject().getBaseURI(), fileFormat);

			//prepare the prefix-namespace map
			Map<String, String> prefixToNamespaceMap = new HashMap<>();
			RepositoryResult<Namespace> namespaceRepositoryResult = connection.getNamespaces();
			while (namespaceRepositoryResult.hasNext()) {
				Namespace namespace = namespaceRepositoryResult.next();
				prefixToNamespaceMap.put(namespace.getPrefix(), namespace.getName());
			}
			//generate the propInInfoMap
			Map<String, PropInfo> propToPropInfoMap = generatePropInfoMap(rep, targetShape, classIri);
			//generate the content of the PEARL file
			pearlFileText = generatePearlFileString(propToPropInfoMap, prefixToNamespaceMap, classIri);
		} finally {
			rep.shutDown();
		}
		//return the content of the PEARL file in a JsonNode as a text
		return JsonNodeFactory.instance.textNode(pearlFileText);
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Read
	//@PreAuthorize("@auth.isAuthorized('rdf(shacl)', 'R')") //TODO
	public JsonNode extractCFfromShapesGraph(IRI classIri, @Optional IRI targetShape) throws SHACLGenericException {
		if(!getProject().isSHACLEnabled()){
			throw new SHACLNotEnabledException();
		}

		//prepare the namespace map
		Map <String, String> prefixToNamespaceMap = new HashMap<>();
		RepositoryResult<Namespace> namespaceRepositoryResult = getManagedConnection().getNamespaces();
		while(namespaceRepositoryResult.hasNext()) {
			Namespace namespace = namespaceRepositoryResult.next();
			prefixToNamespaceMap.put(namespace.getPrefix(), namespace.getName());
		}
		//get all the triples related to the SHACL graph

		String pearlFileText = "";
		RepositoryConnection projConn = getManagedConnection();

		Repository tempRep = new SailRepository(new MemoryStore());
		tempRep.init();
		try(RepositoryConnection conn = tempRep.getConnection()) {
			projConn.export(new RDFInserter(conn), RDF4J.SHACL_SHAPE_GRAPH);
			//generate the propInInfoMap
			Map<String, PropInfo> propToPropInfoMap = generatePropInfoMap(tempRep, targetShape, classIri);
			//generate the content of the PEARL file
			pearlFileText = generatePearlFileString(propToPropInfoMap, prefixToNamespaceMap, classIri);
		} finally {
			tempRep.shutDown();
		}


		//return the content of the PEARL file in a JsonNode as a text
		return JsonNodeFactory.instance.textNode(pearlFileText);
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Read
	//@PreAuthorize("@auth.isAuthorized('rdf(shacl)', 'R')") //TODO
	public JsonNode extractCFfromShapeURL(IRI classIri, String shape, @Optional IRI targetShape,
			@Optional RDFFormat fileFormat) throws IOException, OntologyManagerException, SHACLGenericException {
		//if fileForma is null, try to guess it
		if(fileFormat==null) {
			fileFormat = Rio.getParserFormatForFileName(shape)
					.orElseThrow(() -> new OntologyManagerException(
							"Could not match a parser for file name: " + shape));
		}
		URL url = new URL(shape);
		Repository rep = new SailRepository(new MemoryStore());
		rep.init();
		String pearlFileText = "";
		try(RepositoryConnection connection = rep.getConnection()) {
			connection.add(url, getProject().getBaseURI(), fileFormat);

			//prepare the namespace map
			Map<String, String> prefixToNamespaceMap = new HashMap<>();
			RepositoryResult<Namespace> namespaceRepositoryResult = connection.getNamespaces();
			while (namespaceRepositoryResult.hasNext()) {
				Namespace namespace = namespaceRepositoryResult.next();
				prefixToNamespaceMap.put(namespace.getPrefix(), namespace.getName());
			}
			//generate the propInInfoMap
			Map<String, PropInfo> propToPropInfoMap = generatePropInfoMap(rep, targetShape, classIri);
			//generate the content of the PEARL file
			pearlFileText = generatePearlFileString(propToPropInfoMap, prefixToNamespaceMap, classIri);
		} finally {
			rep.shutDown();
		}
		//return the content of the PEARL file in a JsonNode as a text
		return JsonNodeFactory.instance.textNode(pearlFileText);
	}

	private Map<String, PropInfo> generatePropInfoMap(Repository rep, IRI targetShapeIRI, IRI classIRI) throws SHACLGenericException {
		Map<String, PropInfo> propToPropInfoMap = new HashMap<>();
		try(RepositoryConnection conn = rep.getConnection()) {
			String query;
			//perform some checks before getting the data
			if (targetShapeIRI != null) {
				//check that the targetShapeIRI exists
				query = "\nSELECT * " +
						"\nWHERE{" +
						"\n" + NTriplesUtil.toNTriplesString(targetShapeIRI) + " a <" + SH_NODESHAPE + "> ." +
						"\n" + NTriplesUtil.toNTriplesString(targetShapeIRI) + " a ?type ." +
						"\n}";
				try (TupleQueryResult tupleQueryResult = conn.prepareTupleQuery(query).evaluate()) {
					if (!tupleQueryResult.hasNext()) {
						throw new SHACLTargetShapeNotExistingException(targetShapeIRI);
					}
				}
			} else {
				query = "\nSELECT ?nodeShape " +
						"\nWHERE{" +
						"\n ?nodeShape <" + SH_TARGETCLASS + "> " + NTriplesUtil.toNTriplesString(classIRI) + " ."  +
						"\n}";
				try (TupleQueryResult tupleQueryResult = conn.prepareTupleQuery(query).evaluate()) {
					if (!tupleQueryResult.hasNext()) {
						throw new SHACLNoTargetShapeFromClassIriException(classIRI);
					}
					List<IRI> targetShapeIriList = new ArrayList<>();
					while (tupleQueryResult.hasNext()) {
						targetShapeIriList.add((IRI) tupleQueryResult.next().getValue("nodeShape"));
					}
					if (targetShapeIriList.size() > 1) {
						throw new SHACLMultipleTargetShapeFromClassIRIException(classIRI, targetShapeIriList);
					}
				}
			}

			//use the classIRI and targetShapeIRI (if present)
			//perform a describe (so all list will be taken with a single query)
			query = //"PREFIX sh: <http://www.w3.org/ns/shacl#>" +
					//"\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
					"\nDESCRIBE ?nodeShape " +
							"\nWHERE {";
			if (targetShapeIRI != null) {
				query += "\nBIND(" + NTriplesUtil.toNTriplesString(targetShapeIRI) + " AS ?nodeShape)";
			} else {
				query += "\nBIND(" + NTriplesUtil.toNTriplesString(classIRI) + " AS ?targetClass) " +
						"\n?nodeShape <" + SH_TARGETCLASS + "> ?targetClass ." +
						"\n?nodeShape a <" + SH_NODESHAPE + "> .";
			}
			query += "\n}";

			//logger.debug("query: \n" + query);
			GraphQuery graphQuery = conn.prepareGraphQuery(query);
			graphQuery.setIncludeInferred(false);
			GraphQueryResult graphQueryResult = graphQuery.evaluate();

			//create a temporary repository to host these triples so they can be access in the easiest way possible
			SailRepository repTemp = new SailRepository(new MemoryStore());
			repTemp.init();
			SailRepositoryConnection connTemp = repTemp.getConnection();
			while (graphQueryResult.hasNext()) {
				Statement statement = graphQueryResult.next();
				connTemp.add(statement);
			}

			//now get the data from the tempRepository to construct the needed structure
			SimpleValueFactory svf = SimpleValueFactory.getInstance();
			IRI currentNodeShapeIRI = null;
			if (targetShapeIRI != null) {
				currentNodeShapeIRI = targetShapeIRI;
			} else {
				RepositoryResult<Statement> repositoryResult = connTemp.getStatements(null, RDF.TYPE, svf.createIRI(SH_NODESHAPE));
				if (repositoryResult.hasNext()) {
					currentNodeShapeIRI = (IRI) repositoryResult.next().getSubject();
				}
			}
			//now get all the SH_PROPERTY construct
			RepositoryResult<Statement> repositoryResultPropertyList = connTemp.getStatements(currentNodeShapeIRI, svf.createIRI(SH_PROPERTY), null);
			while (repositoryResultPropertyList.hasNext()) {
				BNode bnode = (BNode) repositoryResultPropertyList.next().getObject();
				//get the sh:path which is the property this construct is using
				RepositoryResult<Statement> rsPath = connTemp.getStatements(bnode, svf.createIRI(SH_PATH), null);
				//RepositoryResult<Statement> rsPath = connTemp.getStatements(bnode, null, null);
				if (rsPath == null || !rsPath.hasNext()) {
					//it has no path, so just skip this construct
					continue;
				}
				Value obj = rsPath.next().getObject();
				if (!(obj instanceof IRI)) {
					//it is not a IRI, so it is a bnode and, at the moment, this is not supported
					continue;
				}
				IRI path = (IRI) obj;
				if (propToPropInfoMap.containsKey(path.stringValue())) {
					//TODO there is already a structure for this property, this should not happen, decide what to do
					continue;
				}

				//now check the other info of such constuct
				int sh_minCount = -1;
				int sh_maxCount = -1;
				IRI sh_class = null;
				Value sh_hasValue = null;
				List<Value> sh_in = new ArrayList();
				IRI sh_datatype = null;
				boolean isLiteral = false;
				RepositoryResult<Statement> rsAll = connTemp.getStatements(bnode, null, null);
				while (rsAll.hasNext()) {
					Statement statement = rsAll.next();
					IRI pred = statement.getPredicate();
					obj = statement.getObject();
					//check the pred and decide what to do
					switch (pred.stringValue()) {
						case SH_CLASS:
							if (obj instanceof IRI && sh_class == null) {
								sh_class = (IRI) obj;
							} else {
								//TODO error, decide what to do
							}
							break;
						case SH_HASVALUE:
							if (sh_hasValue == null) {
								sh_hasValue = obj;
							} else {
								//TODO error, decide what to do
							}
							break;
						case SH_IN:
							if (obj instanceof BNode && sh_in.isEmpty()) {
								//the predicate is a list, so get all the value of the list
								sh_in.addAll(getListValues((BNode) obj, connTemp));
								isLiteral = areLiterals(sh_in);
							} else {
								//TODO error, decide what to do
							}
							break;
						case SH_MINCOUNT:
							if (obj instanceof Literal && sh_minCount == -1) {
								try {
									sh_minCount = Integer.parseInt(obj.stringValue());
								} catch (NumberFormatException e) {
									//TODO error, decide what to do
								}
							} else {
								//TODO error, decide what to do
							}
							break;
						case SH_MAXCOUNT:
							if (obj instanceof Literal && sh_maxCount == -1) {
								try {
									sh_maxCount = Integer.parseInt(obj.stringValue());
								} catch (NumberFormatException e) {
									//TODO error, decide what to do
								}
							} else {
								//TODO error, decide what to do
							}
							break;
						case SH_DATATYPE:
							if (obj instanceof IRI && sh_datatype == null) {
								sh_datatype = (IRI) obj;
								isLiteral = true;
							} else {
								//TODO error, decide what to do
							}
							break;
						default:
							//the SH_PROPERTY is not supported, so skip it
							continue;
					}
				}
				PropInfo propInfo = new PropInfo(path, sh_minCount, sh_maxCount, sh_class, sh_hasValue, sh_in, sh_datatype, isLiteral);
				propToPropInfoMap.put(path.stringValue(), propInfo);
			}
		}
		return propToPropInfoMap;
	}

	private String generatePearlFileString(Map<String, PropInfo> propToPropInfoMap, Map <String, String>prefixToNamespaceMap, IRI classIRI) {
		StringBuffer sb = new StringBuffer();
		//create the prefix part
		for(String prefix : prefixToNamespaceMap.keySet()){
			sb.append("prefix "+prefix+": <"+prefixToNamespaceMap.get(prefix)+">");
			sb.append("\n");
		}

		String className = classIRI.getLocalName();
		String classNameInst = className.toLowerCase()+"_inst";
		//now create the rule
		sb.append("\n");
		sb.append("rule it.uniroma2.art." + className + " id:" + className + " { ");
		sb.append("\n");

		//the nodes part
		sb.append("\tnodes = {");
		sb.append("\n");
		//write the main element
		//sb.append("\t\t" + classNameInst + "\turi\t" + className + " .");
		sb.append("\t\t" + classNameInst + "\turi\t" + FEATPATH_MAIN + " .");
		sb.append("\n");
		//iterate over the property
		for(String prop : propToPropInfoMap.keySet()){
			sb.append("\n");
			String propName = propToPropInfoMap.get(prop).getPropIRI().getLocalName();
			PropInfo propInfo = propToPropInfoMap.get(prop);
			if(propInfo.getSh_hasValue()!=null){
				//since this prop SH_HASVALUE, its placeholder will not be generated, so just skip it
				continue;
			}
			if(propInfo.getMinCount()!=-1 || propInfo.getMaxCount()!=-1){
				//the Annotation ANN_COLLECTION is needed
				String min = propInfo.getMinCount()!=-1 ? "min="+propInfo.getMinCount() : "";
				String max = propInfo.getMaxCount()!=-1 ? "max="+propInfo.getMaxCount() : "";
				sb.append("\t\t"+ ANN_COLLECTION +"("+min+ (!min.isEmpty() && !max.isEmpty() ? ", " : "") + max+")");
				sb.append("\n");
			}
			if(propInfo.getSh_class()!=null){
				//the Annotation ANN_RANGE is needed
				sb.append("\t\t"+ANN_RANGE+"("+getQName(propInfo.getSh_class(), prefixToNamespaceMap)+")");
				sb.append("\n");
			}
			if(propInfo.getSh_in()!=null && !propInfo.getSh_in().isEmpty()){
				//Depending on the type of values, either the Annotation
				//check if the values are all IRI or all Literal
				boolean allIRI=true, allLiteral=true;
				for(Value value : propInfo.getSh_in()){
					if(value instanceof IRI) {
						allLiteral = false;
					} else { // it is a Literal
						allIRI = false;
					}
				}
				if(!allIRI && !allLiteral) {
					//TODO there was an error, some are Literal and some are IRI, decide what to do
				}
				String allValues = "";
				for(Value value : propInfo.getSh_in()){
					allValues+= getQNameOrNTLiteral(value, prefixToNamespaceMap)+", ";
				}
				allValues = allValues.substring(0, allValues.length()-2); // to remove the last ", "
				sb.append( "\t\t"+(allIRI ? ANN_OBJECTONEOF : ANN_DATAONEOF)+"({"+allValues+"})");
				sb.append("\n");
			}
			//now add the node definition
			String literalConv = "literal"+(propInfo.getSh_datatype()!=null ? "^^"+getQName(propInfo.getSh_datatype(), prefixToNamespaceMap) : "");
			sb.append("\t\t"+propName+"\t"+(propInfo.isLiteral() ? literalConv : "uri")+"\t"+USERPROMT+propName+" .");
			sb.append("\n");
		}
		sb.append("\t}");
		sb.append("\n");

		//the graph part
		String clasNamePlch = "$"+classNameInst;
		sb.append("\tgraph = {");
		sb.append("\n");
		//write the main element
		sb.append("\t\t" + clasNamePlch + "\ta\t" + getQName(classIRI, prefixToNamespaceMap) + " .");
		sb.append("\n");
		for(String prop : propToPropInfoMap.keySet()){
			String propName = propToPropInfoMap.get(prop).getPropIRI().getLocalName();
			String propPlchOrValue = "$"+propName;
			PropInfo propInfo = propToPropInfoMap.get(prop);
			if(propInfo.getSh_hasValue()!=null){
				//has a specific value
				propPlchOrValue = getQNameOrNTLiteral(propInfo.getSh_hasValue(), prefixToNamespaceMap);
			}
			sb.append("\t\t"+clasNamePlch+"\t"+getQName(propInfo.getPropIRI(), prefixToNamespaceMap)+"\t"+propPlchOrValue+" .");
			sb.append("\n");

		}

		sb.append("\t}");
		sb.append("\n");

		sb.append("}");
		sb.append("\n");
        return replaceTab(sb.toString());
	}

	private String replaceTab(String text){
		return text.replaceAll("\t", "    ");
	}

	private List<Value> getListValues(BNode bnode,  SailRepositoryConnection connTemp){
		List<Value> valuesList = new ArrayList<>();
		RepositoryResult<Statement> repositoryResult = connTemp.getStatements(bnode, RDF.FIRST, null);
		if(repositoryResult.hasNext()){
			valuesList.add(repositoryResult.next().getObject());
		}
		repositoryResult = connTemp.getStatements(bnode, RDF.REST, null);
		if(repositoryResult.hasNext()){
			Value rest = repositoryResult.next().getObject();
			if(!rest.equals(RDF.NIL)){
				valuesList.addAll(getListValues((BNode) rest, connTemp));
			}
		}
		return valuesList;
	}

	private boolean areLiterals(List<Value> valueList){
		for(Value value : valueList){
			if(value instanceof  IRI){
				return false;
			}
		}
		return true;
	}

	private String getQNameOrNTLiteral(Value value, Map <String, String>prefixToNamespaceMap){
		if(value instanceof IRI){
			return getQName((IRI) value, prefixToNamespaceMap);
		} else {
			//it is a Literal
			return NTriplesUtil.toNTriplesString(value);
		}

	}

	private String getQName(IRI iri, Map <String, String>prefixToNamespaceMap){
		String namespace = iri.getNamespace();
		if(prefixToNamespaceMap.values().contains(namespace)) {
			for (String prefix : prefixToNamespaceMap.keySet()) {
				if(prefixToNamespaceMap.get(prefix).equals(namespace)){
					return prefix+":"+iri.getLocalName();
				}
			}
		} else {
			return NTriplesUtil.toNTriplesString(iri);
		}
		return NTriplesUtil.toNTriplesString(iri);
	}


	class PropInfo {
		private IRI propIRI;
		private int minCount; //-1 means no min
		private int maxCount; //-1 means no max
		private IRI sh_class;
		private Value sh_hasValue;
		private List<Value> sh_in;
		private IRI sh_datatype;
		private boolean isLiteral=false;

		public PropInfo(IRI propIRI, int minCount, int maxCount, IRI sh_class, Value sh_hasValue, List<Value> sh_in, IRI sh_datatype, boolean isLiteral) {
			this.propIRI = propIRI;
			this.minCount = minCount;
			this.maxCount = maxCount;
			this.sh_class = sh_class;
			this.sh_hasValue = sh_hasValue;
			this.sh_in = sh_in;
			this.sh_datatype = sh_datatype;
			this.isLiteral = isLiteral;
		}

		public IRI getPropIRI() {
			return propIRI;
		}

		public int getMinCount() {
			return minCount;
		}

		public int getMaxCount() {
			return maxCount;
		}

		public IRI getSh_class() {
			return sh_class;
		}

		public Value getSh_hasValue() {
			return sh_hasValue;
		}

		public List<Value> getSh_in() {
			return sh_in;
		}

		public IRI getSh_datatype() {
			return sh_datatype;
		}

		public boolean isLiteral() {
			return isLiteral;
		}
	}
}
