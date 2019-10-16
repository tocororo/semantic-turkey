package it.uniroma2.art.semanticturkey.pmki;

import it.uniroma2.art.semanticturkey.user.Role;
import it.uniroma2.art.semanticturkey.user.Role.RoleLevel;

public class PmkiConstants {

	public static final class PmkiRole {
		public static final Role PUBLIC = new Role("pmki_public", RoleLevel.system);
		public static final Role PRISTINE = new Role("pmki_pristine", RoleLevel.system);
		public static final Role STAGING = new Role("pmki_staging", RoleLevel.system);
	}

	public static final String PMKI_VISITOR_EMAIL = "pmki@pmki.eu";
	public static final String PMKI_VISITOR_PWD = "pmki";

}
