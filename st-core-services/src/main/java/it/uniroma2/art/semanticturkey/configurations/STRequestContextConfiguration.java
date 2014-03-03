package it.uniroma2.art.semanticturkey.configurations;

import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.http.STServiceHTTPContext;

import org.springframework.context.annotation.Bean;

// this was one attempt at a Spring configuration for ST through Java code. We disabled it and started to work
// with XML

// @Configuration
public class STRequestContextConfiguration {
	/* Maybe this bean definition should be moved to core framework somehow */
	@Bean
	public STServiceContext stservicecontext() {
		return new STServiceHTTPContext();
	}
}
