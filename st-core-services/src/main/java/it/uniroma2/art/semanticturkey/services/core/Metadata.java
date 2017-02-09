package it.uniroma2.art.semanticturkey.services.core;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import it.uniroma2.art.semanticturkey.exceptions.ProjectUpdateException;
import it.uniroma2.art.semanticturkey.ontology.ImportStatus;
import it.uniroma2.art.semanticturkey.ontology.OntologyManager;
import it.uniroma2.art.semanticturkey.ontology.OntologyManagerException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

/**
 * This class provides services for manipulating metadata associated with a project.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * @author <a href="mailto:stellato@uniroma2.it">Armando Stellato</a>
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
@STService
public class Metadata extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Metadata.class);

	/**
	 * Returns the base URI associated with the project
	 * 
	 * @return
	 */
	@STServiceOperation
	public String getBaseURI() {
		return getOntologyManager().getBaseURI();
	}

	/**
	 * Returns the default namespace associated with the project
	 * 
	 * @return
	 */
	@STServiceOperation
	@Read
	public String getDefaultNamespace() {
		return getManagedConnection().getNamespace("");
	}

	/**
	 * Sets the default namespace associated with the project
	 * 
	 * @param namespace
	 * @throws ProjectUpdateException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	// @Write Project.setDefaultNamespace uses an internal/dedicate connection
	public void setDefaultNamespace(String namespace) throws ProjectUpdateException {
		getProject().setDefaultNamespace(namespace);
	}

	/**
	 * Returns the prefix declarations in the managed ontology.
	 * 
	 * @return
	 * @throws OntologyManagerException
	 */
	@STServiceOperation
	public Collection<PrefixMapping> getNamespaceMappings() throws OntologyManagerException {
		Map<String, String> allMappings = getOntologyManager().getNSPrefixMappings(false);
		Map<String, String> explicitMappings = getOntologyManager().getNSPrefixMappings(true);

		return allMappings.entrySet().stream().map(entry -> new PrefixMapping(entry.getKey(),
				entry.getValue(), explicitMappings.containsKey(entry.getKey()))).collect(toList());
	}

	/**
	 * Expands a qname into its full form, by substituting a namespace for its prefix.
	 * 
	 * @param qname
	 * @return
	 * @throws IllegalArgumentException
	 *             if the provided qname has problems such as missing colon or undefined prefix
	 */
	@STServiceOperation
	@Read
	public String expandQName(String qname) throws IllegalArgumentException {
		String[] parts = qname.split(":");

		if (parts.length == 0) {
			throw new IllegalArgumentException("The provided value does not contain a colon: " + qname);
		}

		if (parts.length == 1) {
			return getManagedConnection().getNamespace("") + qname;
		} else {
			String prefix = parts[0];

			String ns = getManagedConnection().getNamespace(prefix);

			if (ns == null) {
				throw new IllegalArgumentException("Prefixed not defined: " + prefix);
			}

			return ns + parts[1];
		}
	}

	/**
	 * Represents an individual prefix mapping.
	 *
	 */
	public static class PrefixMapping {
		private final String prefix;
		private final String namespace;
		private final boolean explicit;

		public PrefixMapping(String prefix, String namespace, boolean explicit) {
			this.prefix = prefix;
			this.namespace = namespace;
			this.explicit = explicit;
		}

		/**
		 * Returns the prefix
		 * 
		 * @return
		 */
		public String getPrefix() {
			return prefix;
		}

		/**
		 * Returns the namespace
		 * 
		 * @return
		 */
		public String getNamespace() {
			return namespace;
		}

		/**
		 * Tells whether this prefix mapping was explicitly set through the {@link OntologyManager}
		 * 
		 * @return
		 */
		public boolean isExplicit() {
			return explicit;
		}
	}

	/**
	 * Returns the named graphs in the managed ontology.
	 * 
	 * @return
	 * @throws RepositoryException
	 */
	@STServiceOperation
	@Read
	public Collection<AnnotatedValue<org.eclipse.rdf4j.model.Resource>> getNamedGraphs()
			throws RepositoryException {
		return Iterations.stream(getManagedConnection().getContextIDs())
				.map(AnnotatedValue<org.eclipse.rdf4j.model.Resource>::new).collect(toList());
	}

	/**
	 * Returns the hierarchy formed by the imports. Cyclic imports are identified and reported in the response
	 */
	@STServiceOperation
	@Read
	public Collection<OntologyImport> getImports() throws RepositoryException {
		RepositoryConnection conn = getManagedConnection();

		IRI ont = conn.getValueFactory().createIRI(getBaseURI());
		Set<IRI> importsBranch = new HashSet<>();

		logger.debug("listing ontology imports");
		return getImportsHelper(conn, ont, importsBranch);
	}

	/**
	 * Represents an individual ontology import
	 *
	 */
	@JsonSerialize(using = OntologyImport.OntologyImportSerializer.class)
	public static class OntologyImport {
		private IRI ontology;
		private String status;
		private String localfile;
		private Collection<OntologyImport> imports;

		public OntologyImport(IRI ontology, String status, @Nullable String localfile,
				@Nullable Collection<OntologyImport> imports) {
			this.ontology = ontology;
			this.status = status;
			this.localfile = localfile;
			this.imports = imports;
		}

		public IRI getOntology() {
			return ontology;
		}

		public String getStatus() {
			return status;
		}

		public Optional<String> getLocalfile() {
			return Optional.ofNullable(localfile);
		}

		public Optional<Collection<OntologyImport>> getImports() {
			return Optional.ofNullable(imports);
		}

		public static class OntologyImportSerializer extends StdSerializer<OntologyImport> {

			private static final long serialVersionUID = 1L;

			public OntologyImportSerializer() {
				this(null);
			}

			public OntologyImportSerializer(Class<OntologyImport> t) {
				super(t);
			}

			@Override
			public void serialize(OntologyImport value, JsonGenerator gen, SerializerProvider provider)
					throws IOException {
				gen.writeStartObject();

				gen.writeStringField("@id", value.getOntology().stringValue());
				gen.writeStringField("status", value.getStatus());

				if (value.getLocalfile().isPresent()) {
					gen.writeStringField("localfile", value.getLocalfile().get());
				}

				if (value.getImports().isPresent()) {
					Collection<OntologyImport> importsOfImports = value.getImports().get();

					if (!importsOfImports.isEmpty()) {
						gen.writeArrayFieldStart("imports");
						for (OntologyImport recursiveImport : value.getImports().get()) {
							gen.writeObject(recursiveImport);
						}
						gen.writeEndArray();
					}
				}

				gen.writeEndObject();
			}

		}
	}

	private Collection<OntologyImport> getImportsHelper(RepositoryConnection conn, IRI ont,
			Set<IRI> importsBranch) throws RepositoryException {
		Collection<OntologyImport> rv = new ArrayList<>();

		try (RepositoryResult<Statement> imports = conn.getStatements(ont, OWL.IMPORTS, null, false)) {
			while (imports.hasNext()) {
				Value importedOntValue = imports.next().getObject();

				if (!(importedOntValue instanceof IRI))
					continue;

				IRI importedOnt = (IRI) importedOntValue;
				logger.debug("\timport: " + importedOnt);

				String status;
				String localfile = null;
				Collection<OntologyImport> importsOfImporteddOntology = null;

				if (importsBranch.contains(importedOnt)) {
					status = "loop";
				} else {
					ImportStatus importStatus = getOntologyManager()
							.getImportStatus(importedOnt.stringValue());
					if (importStatus != null) {
						ImportStatus.Values statusValue = importStatus.getValue();
						if (statusValue == ImportStatus.Values.LOCAL) {
							localfile = importStatus.getCacheFile().getLocalName();
						}
						status = statusValue.toString();

					} else {
						status = ImportStatus.Values.NULL.toString();
					}

					Set<IRI> newImportsBranch = new HashSet<>(importsBranch);
					newImportsBranch.add(importedOnt);

					importsOfImporteddOntology = getImportsHelper(conn, importedOnt, newImportsBranch);
				}

				OntologyImport importedOntologyElem = new OntologyImport(importedOnt, status, localfile,
						importsOfImporteddOntology);
				rv.add(importedOntologyElem);
			}
		}

		return rv;
	}

	protected OntologyManager getOntologyManager() {
		return getProject().getNewOntologyManager();
	}
}
