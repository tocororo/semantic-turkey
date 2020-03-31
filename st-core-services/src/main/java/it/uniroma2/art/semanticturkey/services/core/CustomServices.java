package it.uniroma2.art.semanticturkey.services.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.customservice.CustomServiceHandlerMapping;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.STService;

@STService
public class CustomServices extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(CustomServices.class);

	@Autowired
	private CustomServiceHandlerMapping customServiceMapping;

}
