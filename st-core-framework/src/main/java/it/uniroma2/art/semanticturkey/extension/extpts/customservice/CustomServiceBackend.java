package it.uniroma2.art.semanticturkey.extension.extpts.customservice;

import java.lang.reflect.InvocationHandler;

import it.uniroma2.art.semanticturkey.config.customservice.OperationDefintion;
import it.uniroma2.art.semanticturkey.extension.Extension;

public interface CustomServiceBackend extends Extension {

	boolean isWrite();
	
	InvocationHandler createInvocationHandler(OperationDefintion operationDefinition);

}