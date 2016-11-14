package it.uniroma2.art.semanticturkey.services.core.resourceview;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.customrange.CustomRangeProvider;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.BroadersStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.ClassAxiomsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.DomainsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.InSchemeStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.LabelRelationsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.LexicalizationsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.OntologyImportsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.OtherPropertiesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.PropertyFacetsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.RangesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SKOSCollectionMembersStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SKOSOrderedCollectionMembersStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SubPropertyOfStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.TopConceptOfStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.TypesStatementConsumer;

@Component
public class StatementConsumerProvider {
	private HashMap<RDFResourceRolesEnum, List<StatementConsumer>> role2template;

	@Autowired
	public StatementConsumerProvider(CustomRangeProvider customRangeProvider) {
		TypesStatementConsumer typesStatementConsumer = new TypesStatementConsumer(customRangeProvider);

		ClassAxiomsStatementConsumer classAxiomsStatementConsumer = new ClassAxiomsStatementConsumer(customRangeProvider);

		LexicalizationsStatementConsumer lexicalizationsStatementConsumer = new LexicalizationsStatementConsumer(
				customRangeProvider);
		BroadersStatementConsumer broaderStatementConsumer = new BroadersStatementConsumer(
				customRangeProvider);
		SubPropertyOfStatementConsumer subPropertyOfStatementConsumer = new SubPropertyOfStatementConsumer(
				customRangeProvider);
		PropertyFacetsStatementConsumer propertyFactesStatementConsumer = new PropertyFacetsStatementConsumer(
				customRangeProvider);
		DomainsStatementConsumer domainsStatementConsumer = new DomainsStatementConsumer(customRangeProvider);
		RangesStatementConsumer rangesStatementConsumer = new RangesStatementConsumer(customRangeProvider);
		OntologyImportsStatementConsumer ontologyImportsStatementConsumer = new OntologyImportsStatementConsumer(
				customRangeProvider);
		OtherPropertiesStatementConsumer otherPropertiesStatementConsumer = new OtherPropertiesStatementConsumer(
				customRangeProvider);
		TopConceptOfStatementConsumer topConceptOfStatementConsumer = new TopConceptOfStatementConsumer(
				customRangeProvider);
		InSchemeStatementConsumer inSchemeStatementConsumer = new InSchemeStatementConsumer(
				customRangeProvider);
		SKOSCollectionMembersStatementConsumer skosCollectionMemberStatementConsumer = new SKOSCollectionMembersStatementConsumer(
				customRangeProvider);
		 SKOSOrderedCollectionMembersStatementConsumer skosOrderedCollectionMembersStatementConsumer = new
				 SKOSOrderedCollectionMembersStatementConsumer(customRangeProvider);
		LabelRelationsStatementConsumer labelRelationStatementConsumer = new LabelRelationsStatementConsumer(
				customRangeProvider);

		role2template = new HashMap<RDFResourceRolesEnum, List<StatementConsumer>>();
		role2template.put(RDFResourceRolesEnum.cls,
				Arrays.asList(typesStatementConsumer,
						classAxiomsStatementConsumer, lexicalizationsStatementConsumer,
						otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.concept,
				Arrays.asList(typesStatementConsumer, topConceptOfStatementConsumer,
						inSchemeStatementConsumer, broaderStatementConsumer,
						lexicalizationsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.property,
				Arrays.asList(typesStatementConsumer, subPropertyOfStatementConsumer,
						lexicalizationsStatementConsumer, propertyFactesStatementConsumer,
						domainsStatementConsumer, rangesStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.conceptScheme, Arrays.asList(typesStatementConsumer,
				lexicalizationsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.ontology,
				Arrays.asList(typesStatementConsumer, lexicalizationsStatementConsumer,
						ontologyImportsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.skosCollection,
				Arrays.asList(typesStatementConsumer, lexicalizationsStatementConsumer,
						skosCollectionMemberStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.skosOrderedCollection, Arrays.asList(typesStatementConsumer,
				lexicalizationsStatementConsumer,
				skosOrderedCollectionMembersStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.individual, Arrays.asList(typesStatementConsumer,
				lexicalizationsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.xLabel, Arrays.asList(typesStatementConsumer,
				labelRelationStatementConsumer, otherPropertiesStatementConsumer));
	}

	public List<StatementConsumer> getTemplateForResourceRole(RDFResourceRolesEnum role) {
		if (role.isProperty()) {
			return role2template.get(RDFResourceRolesEnum.property);
		} else if (role.isClass()) {
			return role2template.get(RDFResourceRolesEnum.cls);
		} else {
			List<StatementConsumer> result = role2template.get(role);

			if (result != null) {
				return result;
			} else {
				return role2template.get(RDFResourceRolesEnum.individual);
			}
		}
	}
}
