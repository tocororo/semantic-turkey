package it.uniroma2.art.semanticturkey.converters;

import it.uniroma2.art.semanticturkey.project.ProjectConsumer;
import it.uniroma2.art.semanticturkey.project.ProjectManager;

import org.springframework.core.convert.converter.Converter;

public class StringToProjectConsumerConverter implements Converter<String, ProjectConsumer> {

	@Override
	public ProjectConsumer convert(String consumerName) {			
		return ProjectManager.getProjectConsumer(consumerName);
	}

}
