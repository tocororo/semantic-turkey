package it.uniroma2.art.semanticturkey.extension.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.extension.ExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.ExtensionPoint;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchExtensionPointException;
import it.uniroma2.art.semanticturkey.resources.Scope;

public class ExtensionPointManagerImpl implements ExtensionPointManager {

	@Autowired
	private BundleContext context;
	private ServiceTracker extensionPointTracker;
	private ServiceTracker extensionFactoryTracker;

	@PostConstruct
	public void init() {
		extensionPointTracker = new ServiceTracker(context, ExtensionPoint.class.getName(), null);
		extensionFactoryTracker = new ServiceTracker(context, ExtensionFactory.class.getName(), null);

		extensionPointTracker.open();
		extensionFactoryTracker.open();
	}

	@PreDestroy
	public void destroy() {
		if (extensionPointTracker != null) {
			extensionPointTracker.close();
		}
		try {
		} finally {
			if (extensionFactoryTracker != null) {
				extensionFactoryTracker.close();
			}
		}
	}

	@Override
	public Collection<ExtensionPoint> getExtensionPoints(Scope... scopes) {
		Collection<ExtensionPoint> rv = new ArrayList<>();
		Set<Scope> filter = new HashSet<>();
		Arrays.stream(scopes.length == 0 ? Scope.values() : scopes).forEach(filter::add);
		for (Object expt : extensionPointTracker.getServices()) {
			ExtensionPoint expt2 = ((ExtensionPoint) expt);
			if (filter.contains(expt2.getScope())) {
				rv.add(expt2);
			}
		}
		return rv;
	}

	@Override
	public ExtensionPoint getExtensionPoint(String identifier) throws NoSuchExtensionPointException {
		for (Object expt : extensionPointTracker.getServices()) {
			ExtensionPoint expt2 = ((ExtensionPoint) expt);

			if (expt2.getInterface().getName().equals(identifier)) {
				return expt2;
			}
		}

		throw new NoSuchExtensionPointException("Unrecognized extension point: " + identifier);
	}

}
