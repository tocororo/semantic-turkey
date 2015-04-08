package it.uniroma2.art.semanticturkey.services.core.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import it.uniroma2.art.owlart.vocabulary.RDFResourceRolesEnum;

public class StatementConsumerProvider {
	private static StatementConsumerProvider instance;

	public static synchronized StatementConsumerProvider getInstace() {
		if (instance == null) {
			instance = new StatementConsumerProvider();
			
		}

		return instance;
	}

	private HashMap<RDFResourceRolesEnum, List<StatementConsumer>> role2template;
	
	public StatementConsumerProvider() {
		TypesStatementConsumer typesStatementConsumer = new TypesStatementConsumer();
		SubClassOfStatementConsumer subClassofStatementConsumer = new SubClassOfStatementConsumer();
		LexicalizationsStatementConsumer lexicalizationsStatementConsumer = new LexicalizationsStatementConsumer();
		BroaderStatementConsumer broaderStatementConsumer = new BroaderStatementConsumer();
		SubPropertyOfStatementConsumer subPropertyOfStatementConsumer = new SubPropertyOfStatementConsumer();
		PropertyFactesStatementConsumer propertyFactesStatementConsumer = new PropertyFactesStatementConsumer();
		DomainsStatementConsumer domainsStatementConsumer = new DomainsStatementConsumer();
		RangesStatementConsumer rangesStatementConsumer = new RangesStatementConsumer();
		TopConceptsStatementConsumer topConceptsStatementConsumer = new TopConceptsStatementConsumer();
		OntologyImportsStatementConsumer ontologyImportsStatementConsumer = new OntologyImportsStatementConsumer();
		OtherPropertiesStatementConsumer otherPropertiesStatementConsumer = new OtherPropertiesStatementConsumer();
		
		
		role2template = new HashMap<RDFResourceRolesEnum, List<StatementConsumer>>();
		role2template.put(RDFResourceRolesEnum.cls, Arrays.asList(typesStatementConsumer, subClassofStatementConsumer, lexicalizationsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.concept, Arrays.asList(typesStatementConsumer, broaderStatementConsumer, lexicalizationsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.property, Arrays.asList(subPropertyOfStatementConsumer, lexicalizationsStatementConsumer, propertyFactesStatementConsumer, domainsStatementConsumer, rangesStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.conceptScheme, Arrays.asList(typesStatementConsumer, lexicalizationsStatementConsumer, topConceptsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.ontology, Arrays.asList(typesStatementConsumer, lexicalizationsStatementConsumer, ontologyImportsStatementConsumer, otherPropertiesStatementConsumer));
		role2template.put(RDFResourceRolesEnum.individual, Arrays.asList(typesStatementConsumer, lexicalizationsStatementConsumer, otherPropertiesStatementConsumer));
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
