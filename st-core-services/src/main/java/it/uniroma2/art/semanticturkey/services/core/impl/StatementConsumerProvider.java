package it.uniroma2.art.semanticturkey.services.core.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;

@Component(value="StatementConsumerProviderOLD")
public class StatementConsumerProvider {
	private HashMap<RDFResourceRolesEnum, List<StatementConsumer>> role2template;
	
	@Autowired
	public StatementConsumerProvider(CustomFormManager customFormManager) {
		TypesStatementConsumer typesStatementConsumer = new TypesStatementConsumer();
		ClassAxiomsStatementConsumer classAxiomsStatementConsumer = new ClassAxiomsStatementConsumer();
		
		LexicalizationsStatementConsumer lexicalizationsStatementConsumer = new LexicalizationsStatementConsumer();
		BroaderStatementConsumer broaderStatementConsumer = new BroaderStatementConsumer();
		SubPropertyOfStatementConsumer subPropertyOfStatementConsumer = new SubPropertyOfStatementConsumer();
		PropertyFactesStatementConsumer propertyFactesStatementConsumer = new PropertyFactesStatementConsumer();
		DomainsStatementConsumer domainsStatementConsumer = new DomainsStatementConsumer();
		RangesStatementConsumer rangesStatementConsumer = new RangesStatementConsumer();
		OntologyImportsStatementConsumer ontologyImportsStatementConsumer = new OntologyImportsStatementConsumer();
		OtherPropertiesStatementConsumer otherPropertiesStatementConsumer = new OtherPropertiesStatementConsumer(customFormManager);
		TopConceptOfStatementConsumer topConceptOfStatementConsumer = new TopConceptOfStatementConsumer();
		InSchemeStatementConsumer inSchemeStatementConsumer = new InSchemeStatementConsumer();
		SKOSCollectionMemberStatementConsumer skosCollectionMemberStatementConsumer = new SKOSCollectionMemberStatementConsumer();
		SKOSOrderedCollectionMemberStatementConsumer skosOrderedCollectionMemberStatementConsumer = new SKOSOrderedCollectionMemberStatementConsumer();
		LabelRelationStatementConsumer labelRelationStatementConsumer = new LabelRelationStatementConsumer();

		role2template = new HashMap<RDFResourceRolesEnum, List<StatementConsumer>>();
		role2template.put(RDFResourceRolesEnum.cls, Arrays.asList(typesStatementConsumer, classAxiomsStatementConsumer, lexicalizationsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.concept, Arrays.asList(typesStatementConsumer, topConceptOfStatementConsumer, inSchemeStatementConsumer, broaderStatementConsumer, lexicalizationsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.property, Arrays.asList(typesStatementConsumer, subPropertyOfStatementConsumer, lexicalizationsStatementConsumer, propertyFactesStatementConsumer, domainsStatementConsumer, rangesStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.conceptScheme, Arrays.asList(typesStatementConsumer, lexicalizationsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.ontology, Arrays.asList(typesStatementConsumer, lexicalizationsStatementConsumer, ontologyImportsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.skosCollection, Arrays.asList(typesStatementConsumer, lexicalizationsStatementConsumer, skosCollectionMemberStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.skosOrderedCollection, Arrays.asList(typesStatementConsumer, lexicalizationsStatementConsumer, skosOrderedCollectionMemberStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.individual, Arrays.asList(typesStatementConsumer, lexicalizationsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.xLabel, Arrays.asList(typesStatementConsumer, labelRelationStatementConsumer, otherPropertiesStatementConsumer));
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
