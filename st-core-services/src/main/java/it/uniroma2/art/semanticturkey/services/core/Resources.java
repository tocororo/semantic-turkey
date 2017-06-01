package it.uniroma2.art.semanticturkey.services.core;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import it.uniroma2.art.semanticturkey.constraints.LocallyDefined;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Subject;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
//import it.uniroma2.art.semanticturkey.utilities.SPARQLHelp;
import it.uniroma2.art.semanticturkey.vocabulary.OWL2Fragment;


@STService
public class Resources extends STServiceAdapter {
	
	private static Logger logger = LoggerFactory.getLogger(Resources.class);
	
	@STServiceOperation
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#subject)+ ', values)', 'U')")
	public void updateTriple(@Subject Resource subject, IRI property, Value value, Value newValue){
		logger.info("request to update a triple");
		RepositoryConnection repoConnection = getManagedConnection();
		
		String query = "DELETE DATA {GRAPH ?g {?subject ?property ?value .}}" +
				"INSERT DATA {GRAPH ?g {?subject ?property ?newValue .} } ;" ;
		Update update = repoConnection.prepareUpdate(query);
		update.setBinding("g", getWorkingGraph());
		update.setBinding("subject", subject);
		update.setBinding("property", property);
		update.setBinding("value", value);
		update.setBinding("newValue", newValue);
		update.execute();
		
	}
	
	@STServiceOperation
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#subject)+ ', values)', 'D')")
	public void removeValue(@LocallyDefined @Subject Resource subject, @LocallyDefined IRI property, Value value){
		getManagedConnection().remove(subject, property, value, getWorkingGraph());
	}
	
	
	@STServiceOperation
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf(' +@auth.typeof(#subject)+ ', values)', 'C')")
	public void addValue(@LocallyDefined @Subject Resource subject, @LocallyDefined IRI property, Value value){
		getManagedConnection().add(subject, property, value, getWorkingGraph());
	}
	
	
	@STServiceOperation
	@Write
	public void setDeprecated(@LocallyDefined @Subject IRI resource){
		RepositoryConnection conn = getManagedConnection();
		Literal literalTrue = conn.getValueFactory().createLiteral("true",XMLSchema.BOOLEAN);
		conn.add(resource, OWL2Fragment.DEPRECATED, literalTrue, getWorkingGraph());
	}

}
