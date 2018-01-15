package it.uniroma2.art.semanticturkey.project;

/**
 * Describes the creation of new local repositories inside the project.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class CreateLocal extends RepositoryAccess {

	@Override
	public boolean isLocal() {
		return true;
	}

	@Override
	public boolean isRemote() {
		return false;
	}

	@Override
	public boolean isCreation() {
		return true;
	}
}
