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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

import it.uniroma2.art.semanticturkey.utilities.ReflectionUtilities;

/**
 * Aspect wrapping the execution of a service method with appropriate bookkeeping logic.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
@Aspect
public class STServiceAspect implements Ordered {
	
	private static final Logger logger = LoggerFactory.getLogger(STServiceAspect.class);
	
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
		
		logger.debug("Begin of service invocation: {}", serviceInvocation);
		
		serviceInvocations.get().add(serviceInvocation);
	}
	
	@After("stServiceMethod()")
	public void afterServiceInvocation() throws Throwable {
		STServiceInvocaton serviceInvocation = serviceInvocations.get().remove();
		
		logger.debug("End of service invocation: {}", serviceInvocation);
	}

	public static STServiceInvocaton getCurrentServiceInvocation() {
		return serviceInvocations.get().peek();
	}

}
