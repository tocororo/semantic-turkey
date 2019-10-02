package it.uniroma2.art.semanticturkey.config.contribution;

import it.uniroma2.art.semanticturkey.extension.SystemScopedConfigurableComponent;

public class ContributionStore implements SystemScopedConfigurableComponent<StoredContributionConfiguration> {

	@Override
	public String getId() {
		return ContributionStore.class.getName();
	}
}
