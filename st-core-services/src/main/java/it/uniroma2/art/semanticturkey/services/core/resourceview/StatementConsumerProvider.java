package it.uniroma2.art.semanticturkey.services.core.resourceview;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.uniroma2.art.semanticturkey.customform.CODACoreProvider;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.BroadersStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.ClassAxiomsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.ConstituentsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.DenotationsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.PropertyDisjointWithStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.DomainsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.EquivalentPropertyStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.EvokedLexicalConcepts;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.FormBasedPreviewStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.InSchemeStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.LabelRelationsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.LexicalFormsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.LexicalSensesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.LexicalizationsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.OntologyImportsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.OtherPropertiesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.PropertyChainStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.PropertyFacetsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.RDFSMembersStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.RangesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.FormRepresentationsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SKOSCollectionMembersStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SKOSNotesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SKOSOrderedCollectionMembersStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SubPropertyOfStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SubtermsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.TopConceptOfStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.TypesStatementConsumer;

@Component
public class StatementConsumerProvider {
	private HashMap<RDFResourceRole, List<StatementConsumer>> role2template;

	@Autowired
	public StatementConsumerProvider(CustomFormManager customFormManager,
			ObjectFactory<CODACoreProvider> codaProvider) {
		TypesStatementConsumer typesStatementConsumer = new TypesStatementConsumer(customFormManager);

		ClassAxiomsStatementConsumer classAxiomsStatementConsumer = new ClassAxiomsStatementConsumer(
				customFormManager);

		LexicalizationsStatementConsumer lexicalizationsStatementConsumer = new LexicalizationsStatementConsumer(
				customFormManager);
		BroadersStatementConsumer broaderStatementConsumer = new BroadersStatementConsumer(customFormManager);
		EquivalentPropertyStatementConsumer equivalentPropertyStatementConsumer = new EquivalentPropertyStatementConsumer(
				customFormManager);
		PropertyDisjointWithStatementConsumer propertyDisjointWithStatementConsumer = new PropertyDisjointWithStatementConsumer(
				customFormManager);
		SubPropertyOfStatementConsumer subPropertyOfStatementConsumer = new SubPropertyOfStatementConsumer(
				customFormManager);
		PropertyChainStatementConsumer propertyChainStatementConsumer = new PropertyChainStatementConsumer(
				customFormManager);
		PropertyFacetsStatementConsumer propertyFacetsStatementConsumer = new PropertyFacetsStatementConsumer(
				customFormManager);
		DomainsStatementConsumer domainsStatementConsumer = new DomainsStatementConsumer(customFormManager);
		RangesStatementConsumer rangesStatementConsumer = new RangesStatementConsumer(customFormManager);
		OntologyImportsStatementConsumer ontologyImportsStatementConsumer = new OntologyImportsStatementConsumer(
				customFormManager);
		OtherPropertiesStatementConsumer otherPropertiesStatementConsumer = new OtherPropertiesStatementConsumer(
				customFormManager);
		TopConceptOfStatementConsumer topConceptOfStatementConsumer = new TopConceptOfStatementConsumer(
				customFormManager);
		InSchemeStatementConsumer inSchemeStatementConsumer = new InSchemeStatementConsumer(
				customFormManager);
		SKOSCollectionMembersStatementConsumer skosCollectionMemberStatementConsumer = new SKOSCollectionMembersStatementConsumer(
				customFormManager);
		SKOSOrderedCollectionMembersStatementConsumer skosOrderedCollectionMembersStatementConsumer = new SKOSOrderedCollectionMembersStatementConsumer(
				customFormManager);
		LabelRelationsStatementConsumer labelRelationStatementConsumer = new LabelRelationsStatementConsumer(
				customFormManager);
		SKOSNotesStatementConsumer skosNotesStatementConsumer = new SKOSNotesStatementConsumer(
				customFormManager);
		LexicalFormsStatementConsumer lexicalFormsStatementConsumer = new LexicalFormsStatementConsumer(
				customFormManager);
		LexicalSensesStatementConsumer lexicalSensesStatementConsumer = new LexicalSensesStatementConsumer(
				customFormManager);
		DenotationsStatementConsumer denotationsStatementConsumer = new DenotationsStatementConsumer(
				customFormManager);
		EvokedLexicalConcepts evokedLexicalConcepts = new EvokedLexicalConcepts(customFormManager);
		FormBasedPreviewStatementConsumer formBasedPreview = new FormBasedPreviewStatementConsumer(
				customFormManager, codaProvider);
		SubtermsStatementConsumer subtermsStatementConsumer = new SubtermsStatementConsumer(
				customFormManager);
		ConstituentsStatementConsumer constituentsStatementConsumer = new ConstituentsStatementConsumer(
				customFormManager);

		FormRepresentationsStatementConsumer representationsStatementConsumer = new FormRepresentationsStatementConsumer(
				customFormManager);

		RDFSMembersStatementConsumer rdfsMembersStatementConsumer = new RDFSMembersStatementConsumer(
				customFormManager);

		role2template = new HashMap<>();
		role2template.put(RDFResourceRole.cls,
				Arrays.asList(typesStatementConsumer, formBasedPreview, classAxiomsStatementConsumer,
						lexicalizationsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.concept,
				Arrays.asList(typesStatementConsumer, formBasedPreview, topConceptOfStatementConsumer,
						inSchemeStatementConsumer, broaderStatementConsumer, lexicalizationsStatementConsumer,
						skosNotesStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.property,
				Arrays.asList(typesStatementConsumer, formBasedPreview, equivalentPropertyStatementConsumer,
						subPropertyOfStatementConsumer, propertyChainStatementConsumer,
						propertyFacetsStatementConsumer, propertyDisjointWithStatementConsumer,
						domainsStatementConsumer, rangesStatementConsumer, lexicalizationsStatementConsumer,
						otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.objectProperty,
				Arrays.asList(typesStatementConsumer, formBasedPreview, equivalentPropertyStatementConsumer,
						subPropertyOfStatementConsumer, propertyChainStatementConsumer,
						propertyFacetsStatementConsumer, propertyDisjointWithStatementConsumer,
						domainsStatementConsumer, rangesStatementConsumer, lexicalizationsStatementConsumer,
						otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.datatypeProperty,
				Arrays.asList(typesStatementConsumer, formBasedPreview, equivalentPropertyStatementConsumer,
						subPropertyOfStatementConsumer, propertyFacetsStatementConsumer,
						propertyDisjointWithStatementConsumer, domainsStatementConsumer,
						rangesStatementConsumer, lexicalizationsStatementConsumer,
						otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.annotationProperty,
				Arrays.asList(typesStatementConsumer, formBasedPreview, subPropertyOfStatementConsumer,
						domainsStatementConsumer, rangesStatementConsumer, lexicalizationsStatementConsumer,
						otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.ontologyProperty,
				Arrays.asList(typesStatementConsumer, formBasedPreview, subPropertyOfStatementConsumer,
						domainsStatementConsumer, rangesStatementConsumer, lexicalizationsStatementConsumer,
						otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.conceptScheme,
				Arrays.asList(typesStatementConsumer, formBasedPreview, lexicalizationsStatementConsumer,
						skosNotesStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.ontology,
				Arrays.asList(typesStatementConsumer, formBasedPreview, lexicalizationsStatementConsumer,
						ontologyImportsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.skosCollection,
				Arrays.asList(typesStatementConsumer, formBasedPreview, lexicalizationsStatementConsumer,
						skosNotesStatementConsumer, skosCollectionMemberStatementConsumer,
						otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.skosOrderedCollection,
				Arrays.asList(typesStatementConsumer, formBasedPreview, lexicalizationsStatementConsumer,
						skosNotesStatementConsumer, skosOrderedCollectionMembersStatementConsumer,
						otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.individual, Arrays.asList(typesStatementConsumer, formBasedPreview,
				lexicalizationsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.xLabel,
				Arrays.asList(typesStatementConsumer, formBasedPreview, labelRelationStatementConsumer,
						skosNotesStatementConsumer, otherPropertiesStatementConsumer));

		role2template.put(RDFResourceRole.ontolexLexicalEntry, Arrays.asList(typesStatementConsumer,
				lexicalFormsStatementConsumer, subtermsStatementConsumer, constituentsStatementConsumer,
				rdfsMembersStatementConsumer, formBasedPreview, lexicalSensesStatementConsumer,
				denotationsStatementConsumer, evokedLexicalConcepts, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.ontolexForm, Arrays.asList(typesStatementConsumer,
				representationsStatementConsumer, formBasedPreview, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.ontolexLexicalSense,
				Arrays.asList(typesStatementConsumer, otherPropertiesStatementConsumer));

	}

	public List<StatementConsumer> getTemplateForResourceRole(RDFResourceRole role) {
		if (role.isProperty()) {
			if (RDFResourceRole.subsumes(RDFResourceRole.objectProperty, role)) {
				return role2template.get(RDFResourceRole.objectProperty);
			} else if (RDFResourceRole.subsumes(RDFResourceRole.datatypeProperty, role)) {
				return role2template.get(RDFResourceRole.datatypeProperty);
			} else if (RDFResourceRole.subsumes(RDFResourceRole.ontologyProperty, role)) {
				return role2template.get(RDFResourceRole.ontologyProperty);
			} else if (RDFResourceRole.subsumes(RDFResourceRole.annotationProperty, role)) {
				return role2template.get(RDFResourceRole.annotationProperty);
			} else {
				return role2template.get(RDFResourceRole.property);
			}
		} else {
			List<StatementConsumer> result = role2template.get(role);

			if (result != null) {
				return result;
			} else {
				return role2template.get(RDFResourceRole.individual);
			}
		}
	}
}
