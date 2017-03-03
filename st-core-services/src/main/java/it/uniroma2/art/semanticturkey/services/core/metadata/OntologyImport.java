package it.uniroma2.art.semanticturkey.services.core.metadata;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Represents an individual ontology import
 *
 */
@JsonSerialize(using = OntologyImport.OntologyImportSerializer.class)
public class OntologyImport {
	public static enum Statuses {
		OK, FAILED, LOOP
	};

	private IRI ontology;
	private OntologyImport.Statuses status;
	private Collection<OntologyImport> imports;

	public OntologyImport(IRI ontology, OntologyImport.Statuses status, @Nullable Collection<OntologyImport> imports) {
		this.ontology = ontology;
		this.status = status;
		this.imports = imports;
	}

	public IRI getOntology() {
		return ontology;
	}

	public OntologyImport.Statuses getStatus() {
		return status;
	}

	public java.util.Optional<Collection<OntologyImport>> getImports() {
		return java.util.Optional.ofNullable(imports);
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
			gen.writeStringField("status", value.getStatus().toString());

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

	public static Collection<OntologyImport> fromImportFailures(Set<IRI> failedImports) {
		return failedImports.stream().map(iri -> new OntologyImport(iri, Statuses.FAILED, null))
				.collect(toList());
	}
}