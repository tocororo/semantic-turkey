package it.uniroma2.art.semanticturkey.aspects;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RDFModelProvisioning {

	@Pointcut("execution(* it.uniroma2.art.semanticturkey.services.STService.getOWLModel(..))")
	public void getOWLModel() {}

}
