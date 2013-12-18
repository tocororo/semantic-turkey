package it.uniroma2.art.semanticturkey.aspects.rendering;

import it.uniroma2.art.semanticturkey.plugin.extpts.ShowerInterface;
import it.uniroma2.art.semanticturkey.shower.ShowerManager;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public class STServiceRenderingAdvice implements MethodInterceptor {

	@Autowired
	private ShowerManager showerManager;
	
	@Autowired
	private ApplicationContext apContext;
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		System.out.println("########## Rendering advisor on method: " + invocation.getMethod());
		System.out.println("########## Generic return type is: "
				+ invocation.getMethod().getGenericReturnType());
		System.out.println("apContext = "+apContext);
		
		Object resultObj = invocation.proceed();
		
		//use here the show
		ShowerInterface shower = showerManager.getShowerImpl("pippo");
		shower.apply(resultObj);
		
		return resultObj;
	}

}
