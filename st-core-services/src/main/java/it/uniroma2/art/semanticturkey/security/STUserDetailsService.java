package it.uniroma2.art.semanticturkey.security;

import it.uniroma2.art.semanticturkey.user.UserException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersManager;

/**
 * @author Tiziano
 * This class handles the user authentication.
 * It's an alternative solution for STAuthenticationProvider and it allows to use remember-me feature
 * (that AuthenticationProvider doesn't support)
 * (Referenced in WEB-INF/spring-security.xml)
 */
public class STUserDetailsService implements UserDetailsService {
	
	//TODO this component seems to be initialized twice (PostCostruct is called twice). Why? this is declared just in spring-security.xml
//	@PostConstruct
//	public void postCostr() {
//		System.out.println("@PostConstruct STUserDetailsService");
//	}
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		STUser user = null;
		try {
			return UsersManager.getUser(username);
		} catch (UserException e) {
			throw new UsernameNotFoundException("User with e-mail address '" + username + "' not found");
		}
	}
	
}

