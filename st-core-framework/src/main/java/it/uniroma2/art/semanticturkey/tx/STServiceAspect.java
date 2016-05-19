package it.uniroma2.art.semanticturkey.tx;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Queue;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;

import it.uniroma2.art.semanticturkey.utilities.ReflectionUtilities;

@Aspect
public class STServiceAspect implements Ordered {
	
	private int order = Ordered.LOWEST_PRECEDENCE;
	
	@Override
	public int getOrder() {
		return order;
	}
	
	public void setOrder(int order) {
		this.order = order;
	}

	private static final ThreadLocal<Queue<STServiceInvocaton>> serviceInvocations = ThreadLocal.withInitial(LinkedList::new);
	
	@Pointcut("target(it.uniroma2.art.semanticturkey.services.STServiceAdapter2) && (execution(@it.uniroma2.art.semanticturkey.services.annotations.Read public * *(..)) || execution(@it.uniroma2.art.semanticturkey.services.annotations.Write public * *(..)))")
	public void stServiceMethod() {};
	
	@Before("stServiceMethod()")
	public void beforeServiceInvocation(JoinPoint joinPoint) throws Throwable {
		Signature signature = joinPoint.getSignature();
		Object[] args = joinPoint.getArgs();

		Method method = ReflectionUtilities.getMethodByName(signature.getDeclaringType(), signature.getName());
		
		STServiceInvocaton serviceInvocation = STServiceInvocaton.create(method, args);
		
		System.out.println("New service invocation");
		System.out.println(serviceInvocation);
		
		serviceInvocations.get().add(serviceInvocation);
	}
	
	@After("stServiceMethod()")
	public void afterServiceInvocation() throws Throwable {
		STServiceInvocaton serviceInvocation = serviceInvocations.get().remove();
		
		System.out.println("Exiting service invocation");
		System.out.println(serviceInvocation);
	}

	public static STServiceInvocaton getCurrentServiceInvocation() {
		return serviceInvocations.get().peek();
	}

}
