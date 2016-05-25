package it.uniroma2.art.semanticturkey.tx;

import java.lang.reflect.Method;
import java.util.Arrays;

import com.google.common.base.Objects;

/**
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class STServiceInvocaton {

	public static STServiceInvocaton create(Method method, Object[] args) {
		return new STServiceInvocaton(method, args);
	}

	private Method method;
	private Object[] arguments;

	protected STServiceInvocaton(Method method, Object[] arguments) {
		this.method = method;
		this.arguments = arguments;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(STServiceInvocaton.class).add("method", method).add("args", Arrays.toString(arguments))
				.toString();
	}

	public Object getMethod() {
		return method;
	}
	
	public Object[] getArguments() {
		return arguments;
	}
}
