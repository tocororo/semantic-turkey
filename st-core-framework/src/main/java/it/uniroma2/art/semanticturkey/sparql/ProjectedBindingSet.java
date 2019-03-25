package it.uniroma2.art.semanticturkey.sparql;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.AbstractBindingSet;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

import com.google.common.collect.BiMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

public class ProjectedBindingSet extends AbstractBindingSet implements BindingSet {

	private static final long serialVersionUID = 1L;

	private BindingSet baseBindingSet;

	private BiMap<String, String> variableSubstitutionMapping;

	public ProjectedBindingSet(BindingSet baseBindingSet, BiMap<String, String> variableSubstitutionMapping) {
		this.baseBindingSet = baseBindingSet;
		this.variableSubstitutionMapping = variableSubstitutionMapping;
	}

	@Override
	public Iterator<Binding> iterator() {
		final Iterator<Binding> it = Iterators.filter(baseBindingSet.iterator(),
				b -> variableSubstitutionMapping.containsKey(b.getName()));
		return new Iterator<Binding>() {

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public Binding next() {
				Binding baseBinding = it.next();
				String projectedName = variableSubstitutionMapping.get(baseBinding.getName());
				return new SimpleBinding(projectedName, baseBinding.getValue());
			}
		};
	}

	@Override
	public Set<String> getBindingNames() {
		return variableSubstitutionMapping.values();
	}

	@Override
	public Binding getBinding(String bindingName) {
		String baseBindingName = variableSubstitutionMapping.inverse().get(bindingName);
		Binding baseBinding = baseBindingSet.getBinding(baseBindingName);
		return baseBinding != null ? new SimpleBinding(bindingName, baseBinding.getValue()) : null;
	}

	@Override
	public boolean hasBinding(String bindingName) {
		String baseBindingName = variableSubstitutionMapping.inverse().get(bindingName);
		Binding baseBinding = baseBindingSet.getBinding(baseBindingName);
		return baseBinding != null;
	}

	@Override
	public Value getValue(String bindingName) {
		String baseBindingName = variableSubstitutionMapping.inverse().get(bindingName);
		Binding baseBinding = baseBindingSet.getBinding(baseBindingName);
		return baseBinding != null ? baseBinding.getValue() : null;
	}

	@Override
	public int size() {
		return Sets.intersection(baseBindingSet.getBindingNames(), variableSubstitutionMapping.keySet())
				.size();
	}
}
