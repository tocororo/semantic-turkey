package it.uniroma2.art.semanticturkey.extension.extpts.customservice;

import java.lang.reflect.InvocationHandler;

import it.uniroma2.art.semanticturkey.extension.Extension;

public interface CustomServiceBackend extends Extension {

	boolean isWrite();
	
	InvocationHandler createInvocationHandler();

}