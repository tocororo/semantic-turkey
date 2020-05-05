package it.uniroma2.art.semanticturkey.changetracking.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for the W3C PROVenance Interchange Vocabulary.
 *
 * @see <a href="https://www.w3.org/ns/prov#">W3C PROVenance Interchange</a>
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PROV {

	/**
	 * The PROV namespace: http://www.w3.org/ns/prov#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/prov#";

	/**
	 * Recommended prefix for the W3C PROVenance Interchange namespace: "prov"
	 */
	public static final String PREFIX = "prov";

	/**
	 * An immutable {@link Namespace} constant that represents the W3C PROVenance Interchange namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	// Classes

	/** prov:Accept */
	public static final IRI ACCEPT;

	/** prov:Activity */
	public static final IRI ACTIVITY;

	/** prov:ActivityInfluence */
	public static final IRI ACTIVITY_INFLUENCE;

	/** prov:Agent */
	public static final IRI AGENT;

	/** prov:AgentInfluence */
	public static final IRI AGENT_INFLUENCE;

	/** prov:Association */
	public static final IRI ASSOCIATION;

	/** prov:Attribution */
	public static final IRI ATTRIBUTION;

	/** prov:Bundle */
	public static final IRI BUNDLE;

	/** prov:Collection */
	public static final IRI COLLECTION;

	/** prov:Communication */
	public static final IRI COMMUNICATION;

	/** prov:Contribute */
	public static final IRI CONTRIBUTE;

	/** prov:Contributor */
	public static final IRI CONTRIBUTOR;

	/** prov:Copyright */
	public static final IRI COPYRIGHT;

	/** prov:Create */
	public static final IRI CREATE;

	/** prov:Creator */
	public static final IRI CREATOR;

	/** prov:Delegation */
	public static final IRI DELEGATION;

	/** prov:Derivation */
	public static final IRI DERIVATION;

	/** prov:Dictionary */
	public static final IRI DICTIONARY;

	/** prov:DirectQueryService */
	public static final IRI DIRECT_QUERY_SERVICE;

	/** prov:EmptyCollection */
	public static final IRI EMPTY_COLLECTION;

	/** prov:EmptyDictionary */
	public static final IRI EMPTY_DICTIONARY;

	/** prov:End */
	public static final IRI END;

	/** prov:Entity */
	public static final IRI ENTITY;

	/** prov:EntityInfluence */
	public static final IRI ENTITY_INFLUENCE;

	/** prov:Generation */
	public static final IRI GENERATION;

	/** prov:Influence */
	public static final IRI INFLUENCE;

	/** prov:Insertion */
	public static final IRI INSERTION;

	/** prov:InstantaneousEvent */
	public static final IRI INSTANTANEOUS_EVENT;

	/** prov:Invalidation */
	public static final IRI INVALIDATION;

	/** prov:KeyEntityPair */
	public static final IRI KEY_ENTITY_PAIR;

	/** prov:Location */
	public static final IRI LOCATION;

	/** prov:Modify */
	public static final IRI MODIFY;

	/** prov:Organization */
	public static final IRI ORGANIZATION;

	/** prov:Person */
	public static final IRI PERSON;

	/** prov:Plan */
	public static final IRI PLAN;

	/** prov:PrimarySource */
	public static final IRI PRIMARY_SOURCE;

	/** prov:Publish */
	public static final IRI PUBLISH;

	/** prov:Publisher */
	public static final IRI PUBLISHER;

	/** prov:Quotation */
	public static final IRI QUOTATION;

	/** prov:Removal */
	public static final IRI REMOVAL;

	/** prov:Replace */
	public static final IRI REPLACE;

	/** prov:Revision */
	public static final IRI REVISION;

	/** prov:RightsAssignment */
	public static final IRI RIGHTS_ASSIGNMENT;

	/** prov:RightsHolder */
	public static final IRI RIGHTS_HOLDER;

	/** prov:Role */
	public static final IRI ROLE;

	/** prov:ServiceDescription */
	public static final IRI SERVICE_DESCRIPTION;

	/** prov:SoftwareAgent */
	public static final IRI SOFTWARE_AGENT;

	/** prov:Start */
	public static final IRI START;

	/** prov:Submit */
	public static final IRI SUBMIT;

	/** prov:Usage */
	public static final IRI USAGE;

	// Properties

	/** prov:actedOnBehalfOf */
	public static final IRI ACTED_ON_BEHALF_OF;

	/** prov:activity */
	public static final IRI HAS_ACTIVITY;

	/** prov:agent */
	public static final IRI HAS_AGENT;

	/** prov:alternateOf */
	public static final IRI ALTERNATE_OF;

	/** prov:aq */
	public static final IRI AQ;

	/** prov:asInBundle */
	public static final IRI AS_IN_BUNDLE;

	/** prov:atLocation */
	public static final IRI AT_LOCATION;

	/** prov:atTime */
	public static final IRI AT_TIME;

	/** prov:category */
	public static final IRI CATEGORY;

	/** prov:component */
	public static final IRI COMPONENT;

	/** prov:constraints */
	public static final IRI CONSTRAINTS;

	/** prov:definition */
	public static final IRI DEFINITION;

	/** prov:derivedByInsertionFrom */
	public static final IRI DERIVED_BY_INSERTION_FROM;

	/** prov:derivedByRemovalFrom */
	public static final IRI DERIVED_BY_REMOVAL_FROM;

	/** prov:describesService */
	public static final IRI DESCRIBES_SERVICE;

	/** prov:dictionary */
	public static final IRI HAS_DICTIONARY;

	/** prov:dm */
	public static final IRI DM;

	/** prov:editorialNote */
	public static final IRI EDITORIAL_NOTE;

	/** prov:editorsDefinition */
	public static final IRI EDITORS_DEFINITION;

	/** prov:endedAtTime */
	public static final IRI ENDED_AT_TIME;

	/** prov:entity */
	public static final IRI HAS_ENTITY;

	/** prov:generated */
	public static final IRI GENERATED;

	/** prov:generatedAtTime */
	public static final IRI GENERATED_AT_TIME;

	/** prov:hadActivity */
	public static final IRI HAD_ACTIVITY;

	/** prov:hadDictionaryMember */
	public static final IRI HAD_DICTIONARY_MEMBER;

	/** prov:hadGeneration */
	public static final IRI HAD_GENERATION;

	/** prov:hadMember */
	public static final IRI HAD_MEMBER;

	/** prov:hadPlan */
	public static final IRI HAD_PLAN;

	/** prov:hadPrimarySource */
	public static final IRI HAD_PRIMARY_SOURCE;

	/** prov:hadRole */
	public static final IRI HAD_ROLE;

	/** prov:hadUsage */
	public static final IRI HAD_USAGE;

	/** prov:has_anchor */
	public static final IRI HAS_ANCHOR;

	/** prov:has_provenance */
	public static final IRI HAS_PROVENANCE;

	/** prov:has_query_service */
	public static final IRI HAS_QUERY_SERVICE;

	/** prov:influenced */
	public static final IRI INFLUENCED;

	/** prov:influencer */
	public static final IRI INFLUENCER;

	/** prov:insertedKeyEntityPair */
	public static final IRI INSERTED_KEY_ENTITY_PAIR;

	/** prov:invalidated */
	public static final IRI INVALIDATED;

	/** prov:invalidatedAtTime */
	public static final IRI INVALIDATED_AT_TIME;

	/** prov:inverse */
	public static final IRI INVERSE;

	/** prov:mentionOf */
	public static final IRI MENTION_OF;

	/** prov:n */
	public static final IRI N;

	/** prov:order */
	public static final IRI ORDER;

	/** prov:pairEntity */
	public static final IRI PAIR_ENTITY;

	/** prov:pairKey */
	public static final IRI PAIR_KEY;

	/** prov:pingback */
	public static final IRI PINGBACK;

	/** prov:provenanceUriTemplate */
	public static final IRI PROVENANCE_URI_TEMPLATE;

	/** prov:qualifiedAssociation */
	public static final IRI QUALIFIED_ASSOCIATION;

	/** prov:qualifiedAttribution */
	public static final IRI QUALIFIED_ATTRIBUTION;

	/** prov:qualifiedCommunication */
	public static final IRI QUALIFIED_COMMUNICATION;

	/** prov:qualifiedDelegation */
	public static final IRI QUALIFIED_DELEGATION;

	/** prov:qualifiedDerivation */
	public static final IRI QUALIFIED_DERIVATION;

	/** prov:qualifiedEnd */
	public static final IRI QUALIFIED_END;

	/** prov:qualifiedForm */
	public static final IRI QUALIFIED_FORM;

	/** prov:qualifiedGeneration */
	public static final IRI QUALIFIED_GENERATION;

	/** prov:qualifiedInfluence */
	public static final IRI QUALIFIED_INFLUENCE;

	/** prov:qualifiedInsertion */
	public static final IRI QUALIFIED_INSERTION;

	/** prov:qualifiedInvalidation */
	public static final IRI QUALIFIED_INVALIDATION;

	/** prov:qualifiedPrimarySource */
	public static final IRI QUALIFIED_PRIMARY_SOURCE;

	/** prov:qualifiedQuotation */
	public static final IRI QUALIFIED_QUOTATION;

	/** prov:qualifiedRemoval */
	public static final IRI QUALIFIED_REMOVAL;

	/** prov:qualifiedRevision */
	public static final IRI QUALIFIED_REVISION;

	/** prov:qualifiedStart */
	public static final IRI QUALIFIED_START;

	/** prov:qualifiedUsage */
	public static final IRI QUALIFIED_USAGE;

	/** prov:removedKey */
	public static final IRI REMOVED_KEY;

	/** prov:sharesDefinitionWith */
	public static final IRI SHARES_DEFINITION_WITH;

	/** prov:specializationOf */
	public static final IRI SPECIALIZATION_OF;

	/** prov:startedAtTime */
	public static final IRI STARTED_AT_TIME;

	/** prov:todo */
	public static final IRI TODO;

	/** prov:unqualifiedForm */
	public static final IRI UNQUALIFIED_FORM;

	/** prov:used */
	public static final IRI USED;

	/** prov:value */
	public static final IRI VALUE;

	/** prov:wasAssociatedWith */
	public static final IRI WAS_ASSOCIATED_WITH;

	/** prov:wasAttributedTo */
	public static final IRI WAS_ATTRIBUTED_TO;

	/** prov:wasDerivedFrom */
	public static final IRI WAS_DERIVED_FROM;

	/** prov:wasEndedBy */
	public static final IRI WAS_ENDED_BY;

	/** prov:wasGeneratedBy */
	public static final IRI WAS_GENERATED_BY;

	/** prov:wasInfluencedBy */
	public static final IRI WAS_INFLUENCED_BY;

	/** prov:wasInformedBy */
	public static final IRI WAS_INFORMED_BY;

	/** prov:wasInvalidatedBy */
	public static final IRI WAS_INVALIDATED_BY;

	/** prov:wasQuotedFrom */
	public static final IRI WAS_QUOTED_FROM;

	/** prov:wasRevisionOf */
	public static final IRI WAS_REVISION_OF;

	/** prov:wasStartedBy */
	public static final IRI WAS_STARTED_BY;

	static {
		ValueFactory vf = SimpleValueFactory.getInstance();

		ACCEPT = vf.createIRI(NAMESPACE, "Accept");
		ACTIVITY = vf.createIRI(NAMESPACE, "Activity");
		ACTIVITY_INFLUENCE = vf.createIRI(NAMESPACE, "ActivityInfluence");
		AGENT = vf.createIRI(NAMESPACE, "Agent");
		AGENT_INFLUENCE = vf.createIRI(NAMESPACE, "AgentInfluence");
		ASSOCIATION = vf.createIRI(NAMESPACE, "Association");
		ATTRIBUTION = vf.createIRI(NAMESPACE, "Attribution");
		BUNDLE = vf.createIRI(NAMESPACE, "Bundle");
		COLLECTION = vf.createIRI(NAMESPACE, "Collection");
		COMMUNICATION = vf.createIRI(NAMESPACE, "Communication");
		CONTRIBUTE = vf.createIRI(NAMESPACE, "Contribute");
		CONTRIBUTOR = vf.createIRI(NAMESPACE, "Contributor");
		COPYRIGHT = vf.createIRI(NAMESPACE, "Copyright");
		CREATE = vf.createIRI(NAMESPACE, "Create");
		CREATOR = vf.createIRI(NAMESPACE, "Creator");
		DELEGATION = vf.createIRI(NAMESPACE, "Delegation");
		DERIVATION = vf.createIRI(NAMESPACE, "Derivation");
		DICTIONARY = vf.createIRI(NAMESPACE, "Dictionary");
		DIRECT_QUERY_SERVICE = vf.createIRI(NAMESPACE, "DirectQueryService");
		EMPTY_COLLECTION = vf.createIRI(NAMESPACE, "EmptyCollection");
		EMPTY_DICTIONARY = vf.createIRI(NAMESPACE, "EmptyDictionary");
		END = vf.createIRI(NAMESPACE, "End");
		ENTITY = vf.createIRI(NAMESPACE, "Entity");
		ENTITY_INFLUENCE = vf.createIRI(NAMESPACE, "EntityInfluence");
		GENERATION = vf.createIRI(NAMESPACE, "Generation");
		INFLUENCE = vf.createIRI(NAMESPACE, "Influence");
		INSERTION = vf.createIRI(NAMESPACE, "Insertion");
		INSTANTANEOUS_EVENT = vf.createIRI(NAMESPACE, "InstantaneousEvent");
		INVALIDATION = vf.createIRI(NAMESPACE, "Invalidation");
		KEY_ENTITY_PAIR = vf.createIRI(NAMESPACE, "KeyEntityPair");
		LOCATION = vf.createIRI(NAMESPACE, "Location");
		MODIFY = vf.createIRI(NAMESPACE, "Modify");
		ORGANIZATION = vf.createIRI(NAMESPACE, "Organization");
		PERSON = vf.createIRI(NAMESPACE, "Person");
		PLAN = vf.createIRI(NAMESPACE, "Plan");
		PRIMARY_SOURCE = vf.createIRI(NAMESPACE, "PrimarySource");
		PUBLISH = vf.createIRI(NAMESPACE, "Publish");
		PUBLISHER = vf.createIRI(NAMESPACE, "Publisher");
		QUOTATION = vf.createIRI(NAMESPACE, "Quotation");
		REMOVAL = vf.createIRI(NAMESPACE, "Removal");
		REPLACE = vf.createIRI(NAMESPACE, "Replace");
		REVISION = vf.createIRI(NAMESPACE, "Revision");
		RIGHTS_ASSIGNMENT = vf.createIRI(NAMESPACE, "RightsAssignment");
		RIGHTS_HOLDER = vf.createIRI(NAMESPACE, "RightsHolder");
		ROLE = vf.createIRI(NAMESPACE, "Role");
		SERVICE_DESCRIPTION = vf.createIRI(NAMESPACE, "ServiceDescription");
		SOFTWARE_AGENT = vf.createIRI(NAMESPACE, "SoftwareAgent");
		START = vf.createIRI(NAMESPACE, "Start");
		SUBMIT = vf.createIRI(NAMESPACE, "Submit");
		USAGE = vf.createIRI(NAMESPACE, "Usage");

		ACTED_ON_BEHALF_OF = vf.createIRI(NAMESPACE, "actedOnBehalfOf");
		HAS_ACTIVITY = vf.createIRI(NAMESPACE, "activity");
		HAS_AGENT = vf.createIRI(NAMESPACE, "agent");
		ALTERNATE_OF = vf.createIRI(NAMESPACE, "alternateOf");
		AQ = vf.createIRI(NAMESPACE, "aq");
		AS_IN_BUNDLE = vf.createIRI(NAMESPACE, "asInBundle");
		AT_LOCATION = vf.createIRI(NAMESPACE, "atLocation");
		AT_TIME = vf.createIRI(NAMESPACE, "atTime");
		CATEGORY = vf.createIRI(NAMESPACE, "category");
		COMPONENT = vf.createIRI(NAMESPACE, "component");
		CONSTRAINTS = vf.createIRI(NAMESPACE, "constraints");
		DEFINITION = vf.createIRI(NAMESPACE, "definition");
		DERIVED_BY_INSERTION_FROM = vf.createIRI(NAMESPACE, "derivedByInsertionFrom");
		DERIVED_BY_REMOVAL_FROM = vf.createIRI(NAMESPACE, "derivedByRemovalFrom");
		DESCRIBES_SERVICE = vf.createIRI(NAMESPACE, "describesService");
		HAS_DICTIONARY = vf.createIRI(NAMESPACE, "dictionary");
		DM = vf.createIRI(NAMESPACE, "dm");
		EDITORIAL_NOTE = vf.createIRI(NAMESPACE, "editorialNote");
		EDITORS_DEFINITION = vf.createIRI(NAMESPACE, "editorsDefinition");
		ENDED_AT_TIME = vf.createIRI(NAMESPACE, "endedAtTime");
		HAS_ENTITY = vf.createIRI(NAMESPACE, "entity");
		GENERATED = vf.createIRI(NAMESPACE, "generated");
		GENERATED_AT_TIME = vf.createIRI(NAMESPACE, "generatedAtTime");
		HAD_ACTIVITY = vf.createIRI(NAMESPACE, "hadActivity");
		HAD_DICTIONARY_MEMBER = vf.createIRI(NAMESPACE, "hadDictionaryMember");
		HAD_GENERATION = vf.createIRI(NAMESPACE, "hadGeneration");
		HAD_MEMBER = vf.createIRI(NAMESPACE, "hadMember");
		HAD_PLAN = vf.createIRI(NAMESPACE, "hadPlan");
		HAD_PRIMARY_SOURCE = vf.createIRI(NAMESPACE, "hadPrimarySource");
		HAD_ROLE = vf.createIRI(NAMESPACE, "hadRole");
		HAD_USAGE = vf.createIRI(NAMESPACE, "hadUsage");
		HAS_ANCHOR = vf.createIRI(NAMESPACE, "has_anchor");
		HAS_PROVENANCE = vf.createIRI(NAMESPACE, "has_provenance");
		HAS_QUERY_SERVICE = vf.createIRI(NAMESPACE, "has_query_service");
		INFLUENCED = vf.createIRI(NAMESPACE, "influenced");
		INFLUENCER = vf.createIRI(NAMESPACE, "influencer");
		INSERTED_KEY_ENTITY_PAIR = vf.createIRI(NAMESPACE, "insertedKeyEntityPair");
		INVALIDATED = vf.createIRI(NAMESPACE, "invalidated");
		INVALIDATED_AT_TIME = vf.createIRI(NAMESPACE, "invalidatedAtTime");
		INVERSE = vf.createIRI(NAMESPACE, "inverse");
		MENTION_OF = vf.createIRI(NAMESPACE, "mentionOf");
		N = vf.createIRI(NAMESPACE, "n");
		ORDER = vf.createIRI(NAMESPACE, "order");
		PAIR_ENTITY = vf.createIRI(NAMESPACE, "pairEntity");
		PAIR_KEY = vf.createIRI(NAMESPACE, "pairKey");
		PINGBACK = vf.createIRI(NAMESPACE, "pingback");
		PROVENANCE_URI_TEMPLATE = vf.createIRI(NAMESPACE, "provenanceUriTemplate");
		QUALIFIED_ASSOCIATION = vf.createIRI(NAMESPACE, "qualifiedAssociation");
		QUALIFIED_ATTRIBUTION = vf.createIRI(NAMESPACE, "qualifiedAttribution");
		QUALIFIED_COMMUNICATION = vf.createIRI(NAMESPACE, "qualifiedCommunication");
		QUALIFIED_DELEGATION = vf.createIRI(NAMESPACE, "qualifiedDelegation");
		QUALIFIED_DERIVATION = vf.createIRI(NAMESPACE, "qualifiedDerivation");
		QUALIFIED_END = vf.createIRI(NAMESPACE, "qualifiedEnd");
		QUALIFIED_FORM = vf.createIRI(NAMESPACE, "qualifiedForm");
		QUALIFIED_GENERATION = vf.createIRI(NAMESPACE, "qualifiedGeneration");
		QUALIFIED_INFLUENCE = vf.createIRI(NAMESPACE, "qualifiedInfluence");
		QUALIFIED_INSERTION = vf.createIRI(NAMESPACE, "qualifiedInsertion");
		QUALIFIED_INVALIDATION = vf.createIRI(NAMESPACE, "qualifiedInvalidation");
		QUALIFIED_PRIMARY_SOURCE = vf.createIRI(NAMESPACE, "qualifiedPrimarySource");
		QUALIFIED_QUOTATION = vf.createIRI(NAMESPACE, "qualifiedQuotation");
		QUALIFIED_REMOVAL = vf.createIRI(NAMESPACE, "qualifiedRemoval");
		QUALIFIED_REVISION = vf.createIRI(NAMESPACE, "qualifiedRevision");
		QUALIFIED_START = vf.createIRI(NAMESPACE, "qualifiedStart");
		QUALIFIED_USAGE = vf.createIRI(NAMESPACE, "qualifiedUsage");
		REMOVED_KEY = vf.createIRI(NAMESPACE, "removedKey");
		SHARES_DEFINITION_WITH = vf.createIRI(NAMESPACE, "sharesDefinitionWith");
		SPECIALIZATION_OF = vf.createIRI(NAMESPACE, "specializationOf");
		STARTED_AT_TIME = vf.createIRI(NAMESPACE, "startedAtTime");
		TODO = vf.createIRI(NAMESPACE, "todo");
		UNQUALIFIED_FORM = vf.createIRI(NAMESPACE, "unqualifiedForm");
		USED = vf.createIRI(NAMESPACE, "used");
		VALUE = vf.createIRI(NAMESPACE, "value");
		WAS_ASSOCIATED_WITH = vf.createIRI(NAMESPACE, "wasAssociatedWith");
		WAS_ATTRIBUTED_TO = vf.createIRI(NAMESPACE, "wasAttributedTo");
		WAS_DERIVED_FROM = vf.createIRI(NAMESPACE, "wasDerivedFrom");
		WAS_ENDED_BY = vf.createIRI(NAMESPACE, "wasEndedBy");
		WAS_GENERATED_BY = vf.createIRI(NAMESPACE, "wasGeneratedBy");
		WAS_INFLUENCED_BY = vf.createIRI(NAMESPACE, "wasInfluencedBy");
		WAS_INFORMED_BY = vf.createIRI(NAMESPACE, "wasInformedBy");
		WAS_INVALIDATED_BY = vf.createIRI(NAMESPACE, "wasInvalidatedBy");
		WAS_QUOTED_FROM = vf.createIRI(NAMESPACE, "wasQuotedFrom");
		WAS_REVISION_OF = vf.createIRI(NAMESPACE, "wasRevisionOf");
		WAS_STARTED_BY = vf.createIRI(NAMESPACE, "wasStartedBy");
	}
}