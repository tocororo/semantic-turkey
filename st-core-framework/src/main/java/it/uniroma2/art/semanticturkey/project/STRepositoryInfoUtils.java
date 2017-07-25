package it.uniroma2.art.semanticturkey.project;

import java.util.Objects;
import java.util.Optional;

import it.uniroma2.art.semanticturkey.project.STRepositoryInfo.SearchStrategies;

public abstract class STRepositoryInfoUtils {
	/**
	 * Returns the appropriate value in {@link SearchStrategies} with fallback to
	 * {@link SearchStrategies#REGEX}.
	 * 
	 * @param repoInfo
	 * @return
	 */
	public static SearchStrategies getSearchStrategy(Optional<STRepositoryInfo> repoInfo) {
		return repoInfo.map(STRepositoryInfo::getSearchStrategy).filter(Objects::nonNull)
				.orElse(SearchStrategies.REGEX);
	}
}