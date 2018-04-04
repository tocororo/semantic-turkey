package it.uniroma2.art.semanticturkey.services.core.resourceview;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.BroadersStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.ClassAxiomsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.DenotationsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.DomainsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.InSchemeStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.LabelRelationsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.LexicalFormsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.LexicalSensesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.LexicalizationsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.OntologyImportsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.OtherPropertiesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.PropertyFacetsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.RangesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SKOSCollectionMembersStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SKOSNotesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SKOSOrderedCollectionMembersStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SubPropertyOfStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.TopConceptOfStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.TypesStatementConsumer;

@Component
public class StatementConsumerProvider {
	private HashMap<RDFResourceRole, List<StatementConsumer>> role2template;

	@Autowired
	public StatementConsumerProvider(CustomFormManager customFormManager) {
		TypesStatementConsumer typesStatementConsumer = new TypesStatementConsumer(customFormManager);

		ClassAxiomsStatementConsumer classAxiomsStatementConsumer = new ClassAxiomsStatementConsumer(
				customFormManager);

		LexicalizationsStatementConsumer lexicalizationsStatementConsumer = new LexicalizationsStatementConsumer(
				customFormManager);
		BroadersStatementConsumer broaderStatementConsumer = new BroadersStatementConsumer(customFormManager);
		SubPropertyOfStatementConsumer subPropertyOfStatementConsumer = new SubPropertyOfStatementConsumer(
				customFormManager);
		PropertyFacetsStatementConsumer propertyFactesStatementConsumer = new PropertyFacetsStatementConsumer(
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

		role2template = new HashMap<RDFResourceRole, List<StatementConsumer>>();
		role2template.put(RDFResourceRole.cls,
				Arrays.asList(typesStatementConsumer, classAxiomsStatementConsumer,
						lexicalizationsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.concept,
				Arrays.asList(typesStatementConsumer, topConceptOfStatementConsumer,
						inSchemeStatementConsumer, broaderStatementConsumer, lexicalizationsStatementConsumer,
						skosNotesStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.property,
				Arrays.asList(typesStatementConsumer, subPropertyOfStatementConsumer,
						propertyFactesStatementConsumer, domainsStatementConsumer, rangesStatementConsumer,
						lexicalizationsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.conceptScheme,
				Arrays.asList(typesStatementConsumer, lexicalizationsStatementConsumer,
						skosNotesStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.ontology,
				Arrays.asList(typesStatementConsumer, lexicalizationsStatementConsumer,
						ontologyImportsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.skosCollection,
				Arrays.asList(typesStatementConsumer, lexicalizationsStatementConsumer,
						skosNotesStatementConsumer, skosCollectionMemberStatementConsumer,
						otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.skosOrderedCollection,
				Arrays.asList(typesStatementConsumer, lexicalizationsStatementConsumer,
						skosNotesStatementConsumer, skosOrderedCollectionMembersStatementConsumer,
						otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.individual, Arrays.asList(typesStatementConsumer,
				lexicalizationsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRole.xLabel,
				Arrays.asList(typesStatementConsumer, labelRelationStatementConsumer,
						skosNotesStatementConsumer, otherPropertiesStatementConsumer));

		role2template.put(RDFResourceRole.ontolexLexicalEntry,
				Arrays.asList(typesStatementConsumer, lexicalFormsStatementConsumer,
						lexicalSensesStatementConsumer, denotationsStatementConsumer,
						otherPropertiesStatementConsumer));

	}

	public List<StatementConsumer> getTemplateForResourceRole(RDFResourceRole role) {
		if (role.isProperty()) {
			return role2template.get(RDFResourceRole.property);
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
