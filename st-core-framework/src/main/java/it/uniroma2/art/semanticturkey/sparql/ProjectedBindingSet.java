package it.uniroma2.art.semanticturkey.sparql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.AbstractBindingSet;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.CollectionIteration;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterators;

public class ProjectedBindingSet extends AbstractBindingSet implements BindingSet {

	private static final long serialVersionUID = 1L;

	private BindingSet baseBindingSet;

	private BiMap<String, String> variableMapping;

	public ProjectedBindingSet(BindingSet baseBindingSet, BiMap<String, String> variableMapping) {
		this.baseBindingSet = baseBindingSet;
		this.variableMapping = variableMapping;
	}

	@Override
	public Iterator<Binding> iterator() {
		final Iterator<Binding> it = Iterators.filter(baseBindingSet.iterator(),
				b -> variableMapping.containsKey(b.getName()));
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
		return baseBinding != null ? new SimpleBinding(bindingName, baseBinding.getValue()) : null;
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
}
