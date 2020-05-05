package it.uniroma2.art.semanticturkey.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public abstract class ADMSFragment {

	/** The ADMS namespace: http://www.w3.org/ns/adms# */
	public static final String NAMESPACE = "http://www.w3.org/ns/adms#";

	/**
	 * Recommended prefix for the OWL2 namespace: "adms"
	 */
	public static final String PREFIX = "adms";

	/**
	 * An immutable {@link Namespace} constant that represents the ADMS namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	
	public static final IRI ASSETCLASS;
	public static final IRI IDENTIFIERCLASS;
	public static final IRI ASSETDISTRIBUTIONCLASS;
	public static final IRI IDENTIFIER;
	public static final IRI VERSIONNOTES;
	public static final IRI SCHEMEAGENCY;
	public static final IRI STATUS;
	public static final IRI INTEROPERABILITYLEVEL;
	public static final IRI INCLUDEDASSET;
	public static final IRI SAMPLE;
	public static final IRI TRANSLATION;
	public static final IRI PREV;
	public static final IRI LAST;
	public static final IRI NEXT;
	

	static {
		ValueFactory vf = SimpleValueFactory.getInstance();
		
		ASSETCLASS = vf.createIRI(NAMESPACE, "Asset");
		IDENTIFIERCLASS = vf.createIRI(NAMESPACE, "Identifier");
		ASSETDISTRIBUTIONCLASS = vf.createIRI(NAMESPACE, "AssetDistribution");
		IDENTIFIER = vf.createIRI(NAMESPACE, "identifier");
		VERSIONNOTES = vf.createIRI(NAMESPACE, "versionNotes");
		SCHEMEAGENCY = vf.createIRI(NAMESPACE, "schemeAgency");
		STATUS = vf.createIRI(NAMESPACE, "status");
		INTEROPERABILITYLEVEL = vf.createIRI(NAMESPACE, "interoperabilityLevel");
		INCLUDEDASSET = vf.createIRI(NAMESPACE, "includedAsset");
		SAMPLE = vf.createIRI(NAMESPACE, "sample");
		TRANSLATION = vf.createIRI(NAMESPACE, "translation");
		PREV = vf.createIRI(NAMESPACE, "prev");
		LAST = vf.createIRI(NAMESPACE, "last");
		NEXT = vf.createIRI(NAMESPACE, "next");
				 
	}
}
