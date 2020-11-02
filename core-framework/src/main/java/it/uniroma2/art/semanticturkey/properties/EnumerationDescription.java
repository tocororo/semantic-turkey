package it.uniroma2.art.semanticturkey.properties;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * Describes an enumeration.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class EnumerationDescription {
	private final List<String> values;
	private final boolean open;

	public EnumerationDescription(Collection<String> values, boolean open) {
		this.values = ImmutableList.copyOf(values);
		this.open = open;
	}

	public EnumerationDescription(String[] values, boolean open) {
		this(Arrays.asList(values), open);
	}

	public List<String> getValues() {
		return values;
	}

	public boolean isOpen() {
		return open;
	}

	public static EnumerationDescription fromAnnotation(Enumeration annot) {
		return new EnumerationDescription(Arrays.asList(annot.value()), annot.open());
	}
}
