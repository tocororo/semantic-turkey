package it.uniroma2.art.semanticturkey.utilities;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.iteration.IterationWrapper;

/**
 * An {@link IterationWrapper} that allows to capture elements of interest inside the iteration.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 * @param <E>
 * @param <X>
 */
public class IterationSpy<E, X extends Exception> extends IterationWrapper<E, X> {

	private Predicate<E> matcher;
	private Consumer<E> sink;

	public IterationSpy(Iteration<? extends E, ? extends X> iter, Predicate<E> matcher, Consumer<E> sink) {
		super(iter);
		this.matcher = matcher;
		this.sink = sink;
	}

	@Override
	public E next() throws X {
		E rv = super.next();
		if (matcher.test(rv)) {
			sink.accept(rv);
		}

		return rv;
	}

}
