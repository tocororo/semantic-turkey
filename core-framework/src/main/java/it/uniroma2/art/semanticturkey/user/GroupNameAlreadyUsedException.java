package it.uniroma2.art.semanticturkey.user;

public class GroupNameAlreadyUsedException extends UsersGroupException {

	private static final long serialVersionUID = -7826958539875735439L;

	public GroupNameAlreadyUsedException(String name) {
		super(GroupNameAlreadyUsedException.class.getName() + ".message", new Object[] { name });
	}

}
