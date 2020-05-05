package it.uniroma2.art.semanticturkey.services.core.sheet2rdf;

import java.io.File;
import java.io.IOException;
import java.util.List;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.structures.SuggOntologyCoda;
import it.uniroma2.art.sheet2rdf.core.Sheet2RDFCore;

public class S2RDFContext {

	private Sheet2RDFCore s2rdfCore;
	private CODACore codaCore;
	private File spreadsheetFile;
	private File pearlFile;
	private List<SuggOntologyCoda> suggestedTriplesCache; 
	
	public S2RDFContext(Sheet2RDFCore s2rdfCore, CODACore codaCore, File spreadsheetFile){
		this.s2rdfCore = s2rdfCore;
		this.spreadsheetFile = spreadsheetFile;
		this.codaCore = codaCore;
	}
	
	public Sheet2RDFCore getSheet2RDFCore() {
		return s2rdfCore;
	}
	
	public void setSheet2RDFCore(Sheet2RDFCore s2rdfCore) {
		this.s2rdfCore = s2rdfCore;
	}
	
	public File getSpreadsheetFile() {
		return spreadsheetFile;
	}
	
	public void setSpreadsheetFile(File excelFile) {
		this.spreadsheetFile = excelFile;
	}
	
	public File getPearlFile() {
		//in case the pearl file has not been set
		//(ex. if is not generated or loaded, but simply written by user)
		if (pearlFile == null){
			try {
				pearlFile = File.createTempFile("pearl", ".pr");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return pearlFile;
	}

	public void setPearlFile(File pearlFile) {
		this.pearlFile = pearlFile;
	}

	public CODACore getCodaCore() {
		return codaCore;
	}

	public void setCodaCore(CODACore codaCore) {
		this.codaCore = codaCore;
	}
	
	public List<SuggOntologyCoda> getCachedSuggestedTriples(){
		return suggestedTriplesCache;
	}
	
	public void setSuggestedTriples(List<SuggOntologyCoda> suggestedTriples){
		this.suggestedTriplesCache = suggestedTriples;
	}
	
	public void close(){
		this.pearlFile.deleteOnExit();
		this.spreadsheetFile.deleteOnExit();
	}
	
}
