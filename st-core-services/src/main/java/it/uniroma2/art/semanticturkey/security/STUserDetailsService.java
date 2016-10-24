package it.uniroma2.art.semanticturkey.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * @author Tiziano
 * This class handles the user authentication (retrieves authorities for a given username-password pair)
 * It's an alternative solution for STAuthenticationProvider and it allows to use remeber-me feature
 * (that AuthenticationProvider doesn't support)
 * (Referenced in WEB-INF/spring-security.xml)
 */
public class STUserDetailsService implements UserDetailsService {

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
        // Ideally it should be fetched from model and populated instance of
        // #org.springframework.security.core.userdetails.User should be returned from this method
        UserDetails user;
        
        List<GrantedAuthority> grantedAuths;
        if (username.equals("user")) {
            grantedAuths = new ArrayList<>();
            grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));
            user = new User(username, "user", grantedAuths);
        } else if (username.equals("admin")) {
            grantedAuths = new ArrayList<>();
            grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));
            grantedAuths.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            user = new User(username, "admin", grantedAuths);
        } else {
        	throw new UsernameNotFoundException("username " + username+ " not found");
        }
        
        return user;
	}

}
