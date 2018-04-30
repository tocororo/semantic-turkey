package it.uniroma2.art.semanticturkey.services.core;

import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.maple.orchestration.MediationFramework;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

/**
 * This class provides access to the capabilities of <a href="http://art.uniroma2.it/maple/">MAPLE</a>
 * (Mapping Architecture based on Linguistic Evidences).
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class MAPLE extends STServiceAdapter {

	@Autowired
	private MediationFramework mediationFramework;

	@STServiceOperation
	public void test() {
		System.out.println("mediation framework instance: " + mediationFramework);
	}
}
