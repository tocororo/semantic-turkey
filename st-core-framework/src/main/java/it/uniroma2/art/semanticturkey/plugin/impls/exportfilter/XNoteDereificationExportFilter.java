package it.uniroma2.art.semanticturkey.plugin.impls.exportfilter;

import java.util.List;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.plugin.extpts.ExportFilter;
import it.uniroma2.art.semanticturkey.plugin.impls.exportfilter.conf.XNoteDereificationExportFilterConfiguration;

/**
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
public class XNoteDereificationExportFilter implements ExportFilter {

	private boolean preserveReifiedNotes;

	public XNoteDereificationExportFilter(XNoteDereificationExportFilterConfiguration config) {
		preserveReifiedNotes = config.preserveReifiedNotes;
	}

	@Override
	public void filter(RepositoryConnection sourceRepositoryConnection,
			RepositoryConnection workingRepositoryConnection, IRI[] graphs) throws RDF4JException {
		
		IRI[] expandedGraphs = FilterUtils.expandGraphs(workingRepositoryConnection, graphs);
		
		List<Resource> contextList = QueryResults.asList(workingRepositoryConnection.getContextIDs());
		
		
		
		// consider the skos:Note and all its subproperties for the derification.
		// these properties can have both IRI/Bnode or Literal value, consider just the concepts having a URI
		// or a Bnode (since the ones having a different value, is already de-reified)
		// consider the value of the property vale rdf:value
		
		//NEW
		// @formatter:off
		String queryUpdate = 
					"PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" + 
					//"PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> \n" + 
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" + 
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" + 
					"INSERT { \n" +
					"GRAPH ?g{ ?concept ?propNote ?value . } \n " +
					"} \n" +
					"WHERE {\n" +
					"?propNote rdfs:subPropertyOf* skos:note . \n" + 
					"GRAPH ?g {?concept ?propNote ?reifiedNote . \n" + 
					"?reifiedNote rdf:value ?value . } \n" +
					"}";
		// @formatter:on
		Update update = workingRepositoryConnection.prepareUpdate(queryUpdate);
		SimpleDataset dataset = new SimpleDataset();
		//add the defaults graphs (used outside the GRAPH sections of the query)
		for(Resource iri : contextList){
			if(iri instanceof IRI){
				dataset.addDefaultGraph((IRI) iri);
			}
		}
		//add the named graphs (used in the GRAPH sections of the query: WHERE and INSERT)
		for(IRI iri : expandedGraphs){
			dataset.addNamedGraph(iri);
		}
		update.setDataset(dataset);
		
		//execute the query
		update.execute();
		
		
		
		//if preserveReifiedNotes is false, delete all the triples having the reified note as subject or object
		if(!preserveReifiedNotes){
			// @formatter:off
			queryUpdate = 
					"PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" + 
					//"PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> \n" + 
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" + 
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" + 
					"DELETE { \n"+
					"GRAPH ?g {?reifiedNote ?prop1 ?value . \n" +
					"?subj ?prop2 ?reifiedNote . } \n" +
					"} \n" +
					"WHERE {\n" +
					
					"{SELECT ?reifiedNote \n" +
					"WHERE { \n" +
					"?propNote rdfs:subPropertyOf* skos:note. \n" + 
					"GRAPH ?g {?concept ?propNote ?reifiedNote . \n" + 
					"?reifiedNote rdf:value ?value . } \n" +
					"}}\n" +
					
					"{GRAPH ?g {?reifiedNote ?prop1 ?value . } } \n" +
					"UNION\n" +
					"{GRAPH ?g {?subj ?prop2 ?reifiedNote . } }\n" +
					"}";
			// @formatter:on
			update = workingRepositoryConnection.prepareUpdate(queryUpdate);
			dataset = new SimpleDataset();
			//add the defaults graphs (used outside the GRAPH sections of the query)
			for(Resource iri : contextList){
				if(iri instanceof IRI){
					dataset.addDefaultGraph((IRI) iri);
				}
			}
			//add the named graphs (used in the GRAPH sections of the query: WHERE and INSERT)
			for(IRI iri : expandedGraphs){
				dataset.addNamedGraph(iri);
			}
			update.setDataset(dataset);
			
			//execute the query
			update.execute();
		}
	}

}
