package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.xlabeldereification;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import it.uniroma2.art.semanticturkey.extension.extpts.rdftransformer.RDFTransformer;
import it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.FilterUtils;

/**
 * 
 *  @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 *
 */
public class XLabelDereificationRDFTransformer implements RDFTransformer {

	private boolean preserveReifiedLabels;

	public XLabelDereificationRDFTransformer(XLabelDereificationRDFTransformerConfiguration config) {
		preserveReifiedLabels = config.preserveReifiedLabels;
	}

	@Override
	public void filter(RepositoryConnection sourceRepositoryConnection,
			RepositoryConnection workingRepositoryConnection, IRI[] graphs) throws RDF4JException {
		IRI[] expandedGraphs = FilterUtils.expandGraphs(workingRepositoryConnection, graphs);
		
		if (expandedGraphs.length == 0) return;
		
		//add the triples regarding the prefLabel/altLabel/hiddenLabel
		// @formatter:off
		String queryUpdate = 
					"PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" + 
					"PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> \n" + 
					"INSERT { \n" +
					"?concept skos:prefLabel ?prefLabelLitForm . \n " +
					"?concept skos:altLabel ?altLabelLitForm . \n" +
					"?concept skos:hiddenLabel ?hiddenLabelLitForm . \n" +
					"} \n " +
					"WHERE { \n" +
					"{ ?concept skosxl:prefLabel ?prefLabel . \n" +
					"?prefLabel skosxl:literalForm ?prefLabelLitForm . }\n" +
					"UNION \n" +
					"{ ?concept skosxl:altLabel ?altLabel . \n" +
					"?altLabel skosxl:literalForm ?altLabelLitForm . }\n" +
					"UNION \n" +
					"{ ?concept skosxl:hiddenLabel ?hiddenLabel . \n" +
					"?hiddenLabel skosxl:literalForm ?hiddenLabelLitForm . }\n" +
					"}";
		// @formatter:on
		Update update = workingRepositoryConnection.prepareUpdate(queryUpdate);
		for(IRI g : expandedGraphs){
			SimpleDataset dataset = new SimpleDataset();
			dataset.setDefaultInsertGraph(g);
			dataset.addDefaultGraph(g);
			update.setDataset(dataset);
			update.execute();
		}
		
		
		//if preserveReifiedLabels is false, delete all the triples having an xlabel as subject or object
		if(!preserveReifiedLabels){
			// @formatter:off
			queryUpdate = 
					"PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" + 
					"PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> \n" + 
					"DELETE { \n"+
					"?label ?prop1 ?value . \n" +
					"?subj ?prop2 ?label . \n" +
					"} \n" +
					"WHERE {\n" +
					
					"{SELECT ?label \n" +
					"WHERE { \n" +
					"{ ?concept skosxl:prefLabel ?label . } \n" +
					"UNION \n" +
					"{ ?concept skosxl:altLabel ?label . } \n" +
					"UNION \n" +
					"{ ?concept skosxl:hiddenLabel ?label . } \n" +
					"}}\n" +
					
					"{?label ?prop1 ?value . } \n" +
					"UNION\n" +
					"{?subj ?prop2 ?label . }\n" +
					"}";
			
			// @formatter:on
			update = workingRepositoryConnection.prepareUpdate(queryUpdate);
			for(IRI g : expandedGraphs){
				SimpleDataset dataset = new SimpleDataset();
				dataset.addDefaultRemoveGraph(g);
				update.setDataset(dataset);
				update.execute();
			}
		}
	}

}
