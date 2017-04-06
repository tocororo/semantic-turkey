package it.uniroma2.art.semanticturkey.services.core;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.provisioning.ConverterContractDescription;
import it.uniroma2.art.coda.provisioning.ParameterDescription;
import it.uniroma2.art.coda.provisioning.SignatureDescription;
import it.uniroma2.art.owlart.exceptions.UnavailableResourceException;
import it.uniroma2.art.semanticturkey.customform.CODACoreProvider;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInconsistentException;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

@STService
public class CODA extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(CODA.class);

	@STServiceOperation
	@Read
	public JsonNode listConverterContracts()
			throws UnavailableResourceException, ProjectInconsistentException {
		CODACore codaCore = null;
		try {
			codaCore = getInitializedCodaCore(getManagedConnection());
			
			ArrayNode covnertersArrayNode = JsonNodeFactory.instance.arrayNode();
			
			for (ConverterContractDescription aDescr : codaCore.listConverterContracts()) {
				
				ObjectNode convDescriptionNode = JsonNodeFactory.instance.objectNode();
				
				convDescriptionNode.set("uri", JsonNodeFactory.instance.textNode(aDescr.getContractURI()));
				convDescriptionNode.set("name", JsonNodeFactory.instance.textNode(aDescr.getContractName()));
				convDescriptionNode.set("description", JsonNodeFactory.instance.textNode(aDescr.getContractDescription()));
				convDescriptionNode.set("rdfCapability", JsonNodeFactory.instance.textNode(aDescr.getRDFCapability().name()));
				
				Set<IRI> datatypes = aDescr.getDatatypes();
				ArrayNode datatypesArrayNode = JsonNodeFactory.instance.arrayNode();
				for (IRI dt : datatypes) {
					datatypesArrayNode.add(JsonNodeFactory.instance.textNode(dt.stringValue()));
				}
				convDescriptionNode.set("datatypes", datatypesArrayNode);
				
				Collection<SignatureDescription> signatureDescriptions = aDescr.getSignatureDescriptions();
				ArrayNode signaturesArrayNode = JsonNodeFactory.instance.arrayNode();
				for (SignatureDescription sd : signatureDescriptions) {
					ObjectNode signatureNode = JsonNodeFactory.instance.objectNode();
					List<ParameterDescription> paramDescriptions = sd.getParameterDescriptions();
					signatureNode.set("returnType", JsonNodeFactory.instance.textNode(sd.getReturnTypeDescription().getName()));
					signatureNode.set("featurePathRequiredLevel", JsonNodeFactory.instance.textNode(sd.getFeaturePathRequirementLevel().name()));
					
					ArrayNode parametersArrayNode = JsonNodeFactory.instance.arrayNode();
					for (ParameterDescription pd : paramDescriptions) {
						ObjectNode paramNode = JsonNodeFactory.instance.objectNode();
						paramNode.set("name", JsonNodeFactory.instance.textNode(pd.getName()));
						paramNode.set("type", JsonNodeFactory.instance.textNode(pd.getTypeDescription().getName()));
						paramNode.set("description", JsonNodeFactory.instance.textNode(pd.getHTMLDescription()));
						parametersArrayNode.add(paramNode);
					}
					signatureNode.set("params", parametersArrayNode);
					signaturesArrayNode.add(signatureNode);
				}
				convDescriptionNode.set("signatures", signaturesArrayNode);
				
				covnertersArrayNode.add(convDescriptionNode);
			}
			
			return covnertersArrayNode;
		} finally {
			if (codaCore != null) {
				shutDownCodaCore(codaCore);
			}
		}
	}

}
