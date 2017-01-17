package it.uniroma2.art.semanticturkey.customrange;

import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.osgi.bundle.CODAOSGiFactory;

@Component
public class CODACoreProvider {
	
	private CODACore codaCore;
	private CODAOSGiFactory codaFactory;
	private BundleContext context;
	
	protected CODACoreProvider() {
		// This constructor is only required for the generation of a CGLIB proxy
	}
	
	@Autowired
	public CODACoreProvider(CODAOSGiFactory codaFactory, BundleContext context){
		this.codaFactory = codaFactory;
		this.context = context;
	}

	public CODACore getCODACore(){
		codaCore = codaFactory.getInstance(context);
		codaCore.setGlobalContractBinding("http://art.uniroma2.it/coda/contracts/randIdGen",
				"http://semanticturkey.uniroma2.it/coda/converters/randIdGen");
		return codaCore;
	}
}
