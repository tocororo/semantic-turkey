package it.uniroma2.art.semanticturkey.showvoc;

import it.uniroma2.art.semanticturkey.user.Role;
import it.uniroma2.art.semanticturkey.user.Role.RoleLevel;

public class ShowVocConstants {

	public static final class ShowVocRole {
		public static final Role PUBLIC = new Role("showvoc_public", RoleLevel.system);
		public static final Role PRISTINE = new Role("showvoc_pristine", RoleLevel.system);
		public static final Role STAGING = new Role("showvoc_staging", RoleLevel.system);
	}

	public static final String SHOWVOC_VISITOR_EMAIL = "public@showvoc.eu";
	public static final String SHOWVOC_VISITOR_PWD = "showvoc";

}
