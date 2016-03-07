package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelCreationException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.exceptions.UnsupportedRDFFormatException;
import it.uniroma2.art.owlart.io.RDFFormat;
import it.uniroma2.art.owlart.models.RDFModel;
import it.uniroma2.art.semanticturkey.generation.annotation.GenerateSTServiceController;
import it.uniroma2.art.semanticturkey.generation.annotation.RequestMethod;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseREPLY;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Element;

@GenerateSTServiceController
@Validated
@Component
@Controller //just for saveRDF service
public class InputOutput extends STServiceAdapter {
	
	protected static Logger logger = LoggerFactory.getLogger(InputOutput.class);
	
	/**
	 * This method returns into the response content an RDF serialization of the model
	 * @param oRes
	 * @param ext
	 * @param format
	 * @param allNGs
	 * @throws IOException
	 * @throws ModelAccessException
	 * @throws UnsupportedRDFFormatException
	 */
	@RequestMapping(value = "it.uniroma2.art.semanticturkey/st-core-services/InputOutput/saveRDF", 
			method = org.springframework.web.bind.annotation.RequestMethod.GET)
	public void saveRDF(HttpServletResponse oRes,
			@RequestParam(value="ext", required=false) String ext,
			@RequestParam(value="format", required=false) String format,
			@RequestParam(value = "allNGs", defaultValue = "false") boolean allNGs) 
			throws IOException, ModelAccessException, UnsupportedRDFFormatException {
		
		File tempServerFile;
		RDFFormat rdfFormat = RDFFormat.parseFormat(format);
		if (rdfFormat == null){ //format not provided or unparsable
			if (ext == null){
				rdfFormat = RDFFormat.RDFXML; //default format
				ext = RDFFormat.getFormatExtensions(rdfFormat)[0];
			} else { //ext provided -> guess format
				rdfFormat = RDFFormat.guessRDFFormatFromFile(new File("file." + ext));
				if (rdfFormat == null) { //the given ext is not valid -> default format
					rdfFormat = RDFFormat.RDFXML; //default format
					ext = RDFFormat.getFormatExtensions(rdfFormat)[0];
				}
			}
		} else { //valid format provided
			if (ext == null) {
				ext = RDFFormat.getFormatExtensions(rdfFormat)[0];
			} else { //check consistency between required format and ext
				String[] extForFormat = RDFFormat.getFormatExtensions(rdfFormat);
				if (!Arrays.asList(extForFormat).contains(ext)){//ext isn't compatible with format -> infer ext
					ext = RDFFormat.getFormatExtensions(rdfFormat)[0];
				}
			}
		}
			
		tempServerFile = File.createTempFile("save", "."+ext);
		
		RDFModel model = getOWLModel();
		if (allNGs)
			model.writeRDF(tempServerFile, rdfFormat, getUserNamedGraphs());
		else
			model.writeRDF(tempServerFile, rdfFormat, getWorkingGraph());
		
		oRes.setHeader("Content-Disposition", "attachment; filename=save." + ext);
		oRes.setHeader("Access-Control-Allow-Origin", "*");
		FileInputStream is = new FileInputStream(tempServerFile);
		IOUtils.copy(is, oRes.getOutputStream());
		oRes.setContentType(rdfFormat.getMIMEType());
		oRes.setContentLength((int) tempServerFile.length());
		oRes.flushBuffer();
		is.close();
	}
	
	/**
	 * answers with an ack on the result of loading the ontModel from a local file.
	 * 
	 * <Tree type="load_ontModel"> <result level="ok"/> //oppure "failed" <msg content="bla bla bla"/> </Tree>
	 * 
	 * 
	 * HINT for CLIENT: always launch a getNamespaceMappings, getOntologyImports after a setOntologyImports
	 * because an imported ontology may contain other prefix mappings to be imported
	 * @throws UnsupportedRDFFormatException 
	 * @throws ModelUpdateException 
	 * @throws ModelAccessException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * 
	 */
	@GenerateSTServiceController (method = RequestMethod.POST)
	public Response loadRDF(MultipartFile inputFile, String baseUri, @Optional String formatName) 
			throws FileNotFoundException, IOException, ModelAccessException, ModelUpdateException, UnsupportedRDFFormatException {
		
		//create a temp file (in karaf data/temp folder) to copy the received file 
		File inputServerFile = File.createTempFile("loadRDF", inputFile.getOriginalFilename());
		inputFile.transferTo(inputServerFile);
		
		RDFFormat rdfFormat = null;
		if (formatName != null) {
			logger.debug("name for rdf format to be used in loading RDF has been provided by the user: "
					+ formatName);
			rdfFormat = RDFFormat.parseFormat(formatName);
		}
		if (rdfFormat == null) {
			logger.debug("guessing format from extension of file to be loaded: " + formatName);
			rdfFormat = RDFFormat.guessRDFFormatFromFile(inputServerFile);
		}
		if (rdfFormat == null) {
			return logAndSendException("rdf file format cannot be established neither from file extension nor from user preference (if it has been provided)");
		}
		getProject().getOntologyManager().loadOntologyData(inputServerFile, baseUri, rdfFormat, getWorkingGraph());

		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element element = XMLHelp.newElement(dataElement, "result");
		element.setAttribute("level", "ok");
		element = XMLHelp.newElement(dataElement, "msg");
		element.setAttribute("content", "ontModel loaded");
		return response;
	}
	
	/**
	 * answers with an ack on the result of loading the ontModel from a local file.
	 * 
	 * <Tree type="clear_ontModel"> <result level="ok"/> //oppure "failed" <msg content="bla bla bla"/>
	 * </Tree>
	 * 
	 * 
	 * HINT for CLIENT: always launch a getNamespaceMappings, getOntologyImports after a setOntologyImports
	 * because an imported ontology may contain other prefix mappings to be imported
	 * @throws ModelUpdateException 
	 * @throws ModelCreationException 
	 * 
	 */
	@GenerateSTServiceController
	public Response clearData() throws ModelCreationException, ModelUpdateException {
		getProject().getOntologyManager().clearData();
		
		XMLResponseREPLY response = createReplyResponse(RepliesStatus.ok);
		Element dataElement = response.getDataElement();
		Element element = XMLHelp.newElement(dataElement, "result");
		element.setAttribute("level", "ok");
		element = XMLHelp.newElement(dataElement, "msg");
		element.setAttribute("content", "rdf data cleared");
		return response;
	}
	
}
