package it.uniroma2.art.semanticturkey.services.core;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.RecognitionException;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.w3c.dom.Element;

import it.uniroma2.art.owlart.exceptions.ManchesterParserException;
import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.io.RDFNodeSerializer;
import it.uniroma2.art.owlart.model.ARTBNode;
import it.uniroma2.art.owlart.model.ARTNode;
import it.uniroma2.art.owlart.model.ARTStatement;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.NodeFilters;
import it.uniroma2.art.owlart.model.syntax.manchester.ManchesterClassInterface;
import it.uniroma2.art.owlart.model.syntax.manchester.ManchesterParser;
import it.uniroma2.art.owlart.models.OWLModel;
import it.uniroma2.art.owlart.navigation.ARTStatementIterator;
import it.uniroma2.art.owlart.vocabulary.OWL;
import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.owlart.vocabulary.RDFS;
import it.uniroma2.art.semanticturkey.exceptions.ManchesterSyntaxException;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.ontology.utilities.RDFXMLHelp;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFNodeFactory;
import it.uniroma2.art.semanticturkey.ontology.utilities.STRDFResource;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

@GenerateSTServiceController
@Validated
@Component
public class ManchesterHandler extends STServiceAdapter {

	@GenerateSTServiceController
	public Response getAllDLExpression(ARTURIResource classUri,
			@Optional(defaultValue = "true") boolean usePrefixes,
			@Optional(defaultValue = "true") boolean useUppercaseSyntax) throws ModelAccessException {

		OWLModel model = getOWLModel();

		List<ARTBNode> equivalentClassList = new ArrayList<ARTBNode>();
		List<ARTBNode> subClassList = new ArrayList<ARTBNode>();

		ARTStatementIterator iter = model.listStatements(classUri, NodeFilters.ANY, NodeFilters.ANY, false,
				getWorkingGraph());
		while (iter.hasNext()) {
			ARTStatement artStatement = iter.next();
			// check if the statement has a predicate RDFS.Res.SUBCLASSOF or OWL.Res.EQUIVALENTCLASS
			// remember that the restriction is ALWAYS a bnode
			ARTURIResource predicate = artStatement.getPredicate();
			ARTNode object = artStatement.getObject();
			if (predicate.equals(RDFS.Res.SUBCLASSOF) && object.isBlank()) {
				subClassList.add(object.asBNode());
			} else if (predicate.equals(OWL.Res.EQUIVALENTCLASS) && object.isBlank()) {
				equivalentClassList.add(object.asBNode());
			}
		}
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();

		// now take all the bnode in both lists and get the associted manchester expression
		Element collElem = XMLHelp.newElement(dataElement, "collection");
		for (ARTBNode artNode : equivalentClassList) {
			String manchExpr = getSingleManchExpression(artNode, model, null, usePrefixes, useUppercaseSyntax);
			Element equivalentClassElem = XMLHelp.newElement(collElem, "equivalentClass");
			equivalentClassElem.setAttribute("bnode", artNode.getNominalValue());
			equivalentClassElem.setAttribute("expression", manchExpr);
		}
		for (ARTBNode artNode : subClassList) {
			String manchExpr = getSingleManchExpression(artNode, model, null, usePrefixes, useUppercaseSyntax);
			Element equivalentClassElem = XMLHelp.newElement(collElem, "subClass");
			equivalentClassElem.setAttribute("bnode", artNode.getNominalValue());
			equivalentClassElem.setAttribute("expression", manchExpr);
		}

		return response;
	}

	@GenerateSTServiceController
	public Response getExpression(ARTBNode artNode, @Optional(defaultValue = "true") boolean usePrefixes,
			@Optional(defaultValue = "true") boolean useUppercaseSyntax) throws ModelAccessException {

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();

		OWLModel model = getOWLModel();
		List<ARTStatement> tripleList = new ArrayList<ARTStatement>();

		String manchExpr = getSingleManchExpression(artNode, model, tripleList, usePrefixes,
				useUppercaseSyntax);

		Element manchExprElem = XMLHelp.newElement(dataElement, "MachesterExpression");
		manchExprElem.setAttribute("value", manchExpr);

		return response;

	}

	private String getSingleManchExpression(ARTBNode artNode, OWLModel model, List<ARTStatement> tripleList,
			boolean usePrefixes, boolean useUppercaseSyntax) throws ModelAccessException {
		ManchesterClassInterface manchClassFromBNode = model.getManchClassFromBNode(artNode, model,
				NodeFilters.MAINGRAPH, tripleList);
		String manchExpr = manchClassFromBNode.getManchExpr(usePrefixes, useUppercaseSyntax);
		return manchExpr;
	}

	@GenerateSTServiceController
	public Response removeExpression(ARTURIResource classUri, String exprType, ARTNode artNode)
			throws ModelAccessException, ModelUpdateException {

		OWLModel model = getOWLModel();

		if (!artNode.isBlank()) {
			// this should never happen
			return createReplyFAIL("the input artNode should be a BNode");
		}

		XMLResponseREPLY resp = createReplyResponse(RepliesStatus.ok);
		List<ARTStatement> tripleList = new ArrayList<ARTStatement>();
		model.getManchClassFromBNode(artNode.asBNode(), model, getWorkingGraph(), tripleList);
		// remove all the triple
		for (ARTStatement artStatement : tripleList) {
			model.deleteStatement(artStatement, NodeFilters.MAINGRAPH);
		}
		// remove the triple linking the bnode to the mainClass
		if (exprType.contains("subClass")) {
			model.deleteTriple(classUri, RDFS.Res.SUBCLASSOF, artNode.asBNode(), NodeFilters.MAINGRAPH);
		} else { // equivalent class
			model.deleteTriple(classUri, OWL.Res.EQUIVALENTCLASS, artNode.asBNode(), NodeFilters.MAINGRAPH);
		}

		return resp;
	}

	@GenerateSTServiceController
	public Response checkExpression(String manchExpr) {
		try {
			ManchesterParser.parse(manchExpr, getOWLModel());
		} catch (RecognitionException | ModelAccessException | ManchesterParserException e) {
			return createReplyFAIL("the expression : " + manchExpr + " is not valid");

		}
		XMLResponseREPLY resp = createReplyResponse(RepliesStatus.ok);

		return resp;

	}

	@GenerateSTServiceController
	public Response createRestriction(ARTURIResource classUri, ARTURIResource exprType, String manchExpr)
			throws ManchesterSyntaxException, ModelUpdateException {
		OWLModel model = getOWLModel();

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();

//		if (!exprType.equals(RDFS.Res.SUBCLASSOF) && !exprType.equals(OWL.Res.EQUIVALENTCLASS)) {
//			throw new ManchesterSyntaxException("the exprType is " + exprType.getURI() + " should be either "
//					+ RDFS.Res.SUBCLASSOF.getNominalValue() + " or " + OWL.Res.EQUIVALENTCLASS.getURI());
//		}

		try {
			ManchesterClassInterface manchesterClassInterface = ManchesterParser.parse(manchExpr, model);
			List<ARTStatement> statList = new ArrayList<ARTStatement>();
			ARTBNode bnode = model.parseManchesterExpr(manchesterClassInterface, statList).asBNode();

			Element collElem = XMLHelp.newElement(dataElement, "collection");
			// add all the statement to the maingraph
			for (ARTStatement stat : statList) {
				model.addStatement(stat, NodeFilters.MAINGRAPH);
				Element tripleElem = XMLHelp.newElement(collElem, "triple");
				XMLHelp.newElement(tripleElem, "subject", RDFNodeSerializer.toNT(stat.getSubject()));
				XMLHelp.newElement(tripleElem, "predicate", RDFNodeSerializer.toNT(stat.getPredicate()));
				XMLHelp.newElement(tripleElem, "object", RDFNodeSerializer.toNT(stat.getObject()));
			}

			// add the subClass o equivalentClass property between the main ClassURI and the new BNode
			Element infoClassType;
			model.addTriple(classUri, exprType, bnode, NodeFilters.MAINGRAPH);
			infoClassType = XMLHelp.newElement(dataElement, exprType.getLocalName());
			dataElement.appendChild(infoClassType);

			STRDFResource stBNode = STRDFNodeFactory.createSTRDFResource(model, bnode,
					RDFResourceRolesEnum.undetermined,
					servletUtilities.checkWritable(model, bnode, getWorkingGraph()), false);
			RDFXMLHelp.addRDFNode(infoClassType, stBNode);
		} catch (RecognitionException | ModelAccessException e) {
			throw new ManchesterSyntaxException("Syntax problem with the expression : " + manchExpr);
		} catch (ModelUpdateException e) {
			throw new ManchesterSyntaxException("ModelUpdateException : " + manchExpr);
			// throw new ModelUpdateException(e);
		} catch (ManchesterParserException e) {
			throw new ManchesterSyntaxException("Syntax problem with the expression : " + manchExpr);
		}
		return response;

	}

}
