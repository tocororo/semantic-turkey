package it.uniroma2.art.semanticturkey.extension.impl.repositoryimplconfigurer.predefined;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.config.AbstractSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;

public class ModelBasedSailImplConfig extends AbstractSailImplConfig {
	private Model m;
	private Resource implNode;

	@Override
	public void parse(Model m, Resource implNode) throws SailConfigException {
		super.parse(m, implNode);
		this.m = new LinkedHashModel(m);
		this.implNode = implNode;
	}

	@Override
	public Resource export(Model m) {
		Resource newImplNode = super.export(m);
		SimpleValueFactory vf = SimpleValueFactory.getInstance();
		
		for(Statement s : this.m) {
			Resource newSubj = s.getSubject().equals(implNode) ? newImplNode : s.getSubject();
			IRI newPred = s.getPredicate();
			Value newObj = s.getObject().equals(implNode) ? newImplNode : s.getObject();
			
			m.add(vf.createStatement(newSubj, newPred, newObj));
		}
		
		return newImplNode;
	}
}
