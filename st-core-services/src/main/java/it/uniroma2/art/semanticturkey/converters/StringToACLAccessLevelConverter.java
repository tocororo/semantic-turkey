package it.uniroma2.art.semanticturkey.converters;

import it.uniroma2.art.semanticturkey.project.ProjectACL;

import org.springframework.core.convert.converter.Converter;

public class StringToACLAccessLevelConverter implements Converter<String, ProjectACL.AccessLevel> {

	@Override
	public ProjectACL.AccessLevel convert(String accessLevelString) {
		return ProjectACL.AccessLevel.valueOf(accessLevelString);
	}

}
