package it.uniroma2.art.semanticturkey.converters;

import it.uniroma2.art.semanticturkey.project.ProjectACL;

import org.springframework.core.convert.converter.Converter;

public class StringToACLLockLevelConverter implements Converter<String, ProjectACL.LockLevel> {

	@Override
	public ProjectACL.LockLevel convert(String lockLevelString) {
		return ProjectACL.LockLevel.valueOf(lockLevelString);
	}

}
