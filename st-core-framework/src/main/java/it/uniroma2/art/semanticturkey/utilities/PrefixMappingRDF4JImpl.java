package it.uniroma2.art.semanticturkey.utilities;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import com.google.common.base.Objects;

import it.uniroma2.art.owlart.exceptions.ModelAccessException;
import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.owlart.model.ARTNodeFactory;
import it.uniroma2.art.owlart.model.ARTURIResource;
import it.uniroma2.art.owlart.model.impl.ARTNodeFactoryImpl;
import it.uniroma2.art.owlart.models.PrefixMapping;

/**
 * Implementation of OWL ART {@link PrefixMapping} based on an RD4j {@link RepositoryConnection}.
 * 
 * Based on code found in {@link it.uniroma2.art.owlart.models.RDFNodeSerializerTest}.
 */
public class PrefixMappingRDF4JImpl implements PrefixMapping {

	private RepositoryConnection conn;

	private ARTNodeFactory nf = new ARTNodeFactoryImpl();

	public PrefixMappingRDF4JImpl(RepositoryConnection conn) {
		this.conn = conn;
	}

	@Override
	public String expandQName(String qname) throws ModelAccessException {
		if (qname == null)
			throw new IllegalAccessError("qname to be expanded cannot be null!");
		if (maybeIsURI(qname))
			return qname;
		String[] parts = qname.split(":");
		if (parts.length == 1)
			return getNSForPrefix("") + qname;
		else
			return getNSForPrefix(parts[0]) + parts[1];
	}

	private boolean maybeIsURI(String input) {
		if (input.contains("#") || input.contains("/"))
			return true;
		return false;
	}

	@Override
	public String getQName(String uri) throws ModelAccessException {
		ARTURIResource realURI = nf.createURIResource(uri);
		String namespace = realURI.getNamespace();
		String prefix = getPrefixForNS(namespace);
		if (prefix == null)
			return uri;
		else
			return prefix + ":" + realURI.getLocalName();
	}

	@Override
	public Map<String, String> getNamespacePrefixMapping() throws ModelAccessException {
		return QueryResults.stream(conn.getNamespaces())
				.collect(Collectors.toMap(n -> n.getPrefix(), n -> n.getName(), (x, y) -> y));
	}

	@Override
	public String getNSForPrefix(String prefix) throws ModelAccessException {
		return conn.getNamespace(prefix);
	}

	@Override
	public String getPrefixForNS(String namespace) throws ModelAccessException {
		return QueryResults.stream(conn.getNamespaces()).filter(n -> Objects.equal(n.getName(), namespace))
				.map(n -> n.getPrefix()).findAny().orElse(null);
	}

	@Override
	public void setNsPrefix(String namespace, String prefix) throws ModelUpdateException {
		conn.setNamespace(prefix, namespace);
	}

	@Override
	public void removeNsPrefixMapping(String namespace) throws ModelUpdateException {
		List<String> prefixes = QueryResults.stream(conn.getNamespaces())
				.filter(n -> Objects.equal(n.getName(), namespace)).map(n -> n.getPrefix())
				.collect(Collectors.toList());
		for (String p : prefixes) {
			conn.removeNamespace(p);
		}
	}

}
