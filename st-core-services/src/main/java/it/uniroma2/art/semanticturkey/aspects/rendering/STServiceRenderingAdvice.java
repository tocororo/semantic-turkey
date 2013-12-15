package it.uniroma2.art.semanticturkey.aspects.rendering;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class STServiceRenderingAdvice implements MethodInterceptor {

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		System.out.println("########## Rendering advisor on method: " + invocation.getMethod());
		System.out.println("########## Generic return type is: "
				+ invocation.getMethod().getGenericReturnType());
		return invocation.proceed();
	}

}
