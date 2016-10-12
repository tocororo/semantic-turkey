package it.uniroma2.art.semanticturkey.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * @author Tiziano
 * This class handles the user authentication (retrieves authorities for a given username-password pair)
 * (Referenced in WEB-INF/spring-security.xml)
 */
public class STAuthenticationProvider implements AuthenticationProvider {
	

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String name = authentication.getName();
        String password = authentication.getCredentials().toString();
        
        List<GrantedAuthority> grantedAuths;
        Authentication auth = null;
        
        //just one demo user
        if (name.equals("admin") && password.equals("admin")) {
            grantedAuths = new ArrayList<>();
            grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));
            auth = new UsernamePasswordAuthenticationToken(name, password, grantedAuths);
        } 
//        else if (name.equals("admin") && password.equals("password")) {
//            grantedAuths = new ArrayList<>();
//            grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));
//            grantedAuths.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
//            auth = new UsernamePasswordAuthenticationToken(name, password, grantedAuths);
//        }
        return auth;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class);
	}

}
