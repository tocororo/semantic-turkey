package it.uniroma2.art.semanticturkey.customrange;

import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.osgi.bundle.CODAOSGiFactory;

@Component
public class CODACoreProvider {
	
	private CODACore codaCore;
	
	protected CODACoreProvider() {
		// This constructor is only required for the generation of a CGLIB proxy
	}
	
	@Autowired
	public CODACoreProvider(CODAOSGiFactory codaFactory, BundleContext context){
		this.codaCore = codaFactory.getInstance(context);
	}

	public CODACore getCODACore(){
		return this.codaCore;
	}
}
