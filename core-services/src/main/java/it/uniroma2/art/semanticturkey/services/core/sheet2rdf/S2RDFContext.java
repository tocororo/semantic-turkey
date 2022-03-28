package it.uniroma2.art.semanticturkey.services.core.sheet2rdf;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.structures.SuggOntologyCoda;
import it.uniroma2.art.sheet2rdf.core.Sheet2RDFCore;

public class S2RDFContext {

    private Sheet2RDFCore s2rdfCore;
    private CODACore codaCore;

    private Map<String, File> sheetPearlMap;
    private Map<String, List<SuggOntologyCoda>> sheetSuggestedTriplesMap; //cache of suggested triples for each sheet

    public S2RDFContext(Sheet2RDFCore s2rdfCore, CODACore codaCore) {
        this.s2rdfCore = s2rdfCore;
        this.codaCore = codaCore;
        this.sheetPearlMap = new HashMap<>();
        this.sheetSuggestedTriplesMap = new HashMap<>();
    }

    public Sheet2RDFCore getSheet2RDFCore() {
        return s2rdfCore;
    }

    public File getPearlFile(String sheetName) {
        File pearlFile = sheetPearlMap.get(sheetName);
        //in case the pearl file has not been set
        //(ex. if is not generated or loaded, but simply written by user)
        if (pearlFile == null) {
            try {
                pearlFile = File.createTempFile("pearl", ".pr");
                sheetPearlMap.put(sheetName, pearlFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return pearlFile;
    }

    public void setPearlFile(String sheetName, File pearlFile) {
        sheetPearlMap.put(sheetName, pearlFile);
    }

    public CODACore getCodaCore() {
        return codaCore;
    }

    public void setCodaCore(CODACore codaCore) {
        this.codaCore = codaCore;
    }

    public List<SuggOntologyCoda> getCachedSuggestedTriples(String sheetName) {
        return this.sheetSuggestedTriplesMap.get(sheetName);
    }

    public void setSuggestedTriples(String sheetName, List<SuggOntologyCoda> suggestedTriples) {
        this.sheetSuggestedTriplesMap.put(sheetName, suggestedTriples);
    }

    public void close() {
        for (File pearlFile : this.sheetPearlMap.values()) {
            pearlFile.deleteOnExit();
        }
        if (s2rdfCore.getSpreadsheetFile() != null) {
            s2rdfCore.getSpreadsheetFile().deleteOnExit();
        }
    }

}
