package it.uniroma2.art.semanticturkey.sparql;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

import com.google.common.collect.BiMap;
import com.google.common.collect.Iterators;

public class ProjectedBindingSet implements BindingSet {

	private static final long serialVersionUID = 1L;

	private BindingSet baseBindingSet;

	private BiMap<String, String> variableMapping;
	
	
	public ProjectedBindingSet(BindingSet baseBindingSet, BiMap<String, String> variableMapping) {
		this.baseBindingSet = baseBindingSet;
		this.variableMapping = variableMapping;
	}

	@Override
	public Iterator<Binding> iterator() {
		final Iterator<Binding> it = Iterators.filter(baseBindingSet.iterator(), b -> variableMapping.containsKey(b.getName()));
		return new Iterator<Binding>() {

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public Binding next() {
				Binding baseBinding = it.next();
				String projectedName = variableMapping.get(baseBinding.getName());
				return new SimpleBinding(projectedName, baseBinding.getValue());
			}
		};
	}

	@Override
	public Set<String> getBindingNames() {
		return variableMapping.values();
	}

	@Override
	public Binding getBinding(String bindingName) {
		String baseBindingName = variableMapping.inverse().get(bindingName);
		Binding baseBinding = baseBindingSet.getBinding(baseBindingName);
		return new SimpleBinding(bindingName, baseBinding.getValue());
	}

	@Override
	public boolean hasBinding(String bindingName) {
		String baseBindingName = variableMapping.inverse().get(bindingName);
		Binding baseBinding = baseBindingSet.getBinding(baseBindingName);
		return baseBinding != null;
	}

	@Override
	public Value getValue(String bindingName) {
		String baseBindingName = variableMapping.inverse().get(bindingName);
		Binding baseBinding = baseBindingSet.getBinding(baseBindingName);
		return baseBinding != null ? baseBinding.getValue() : null;
	}

	@Override
	public int size() {
		return baseBindingSet.size();
	}

	@Override
	public int hashCode() {
		return Objects.hash(baseBindingSet, variableMapping);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		
		if (obj == this) {
			return true;
		}
		
		if (!(obj instanceof BindingSet)) {
			return false;
		}
		
		BindingSet objBS = (BindingSet)obj;
		
		Set<String> bindingNames = getBindingNames();
	
		if (!bindingNames.equals(objBS)) {
			return false;
		}
		
		return bindingNames.stream().allMatch(bn -> Objects.equals(objBS.getBinding(bn), getBinding(bn)));
	}
}
