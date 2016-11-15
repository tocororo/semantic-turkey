package it.uniroma2.art.semanticturkey.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import it.uniroma2.art.semanticturkey.user.UserPermissionsEnum;
import it.uniroma2.art.semanticturkey.user.UserRolesEnum;

public class PermissionManager {
	
	private static Map<UserRolesEnum, List<UserPermissionsEnum>> rolePermissionMap;
	static {
		rolePermissionMap = new HashMap<UserRolesEnum, List<UserPermissionsEnum>>();
		List<UserPermissionsEnum> adminCapabilities = new ArrayList<UserPermissionsEnum>();
		adminCapabilities.add(UserPermissionsEnum.CAPABILITY_ADMIN);
		adminCapabilities.add(UserPermissionsEnum.CAPABILITY_2);
		adminCapabilities.add(UserPermissionsEnum.CAPABILITY_3);
		adminCapabilities.add(UserPermissionsEnum.CAPABILITY_4);
		rolePermissionMap.put(UserRolesEnum.ROLE_ADMIN, adminCapabilities);
		List<UserPermissionsEnum> userCapabilities = new ArrayList<UserPermissionsEnum>();
		userCapabilities.add(UserPermissionsEnum.CAPABILITY_5);
		userCapabilities.add(UserPermissionsEnum.CAPABILITY_6);
		userCapabilities.add(UserPermissionsEnum.CAPABILITY_7);
		rolePermissionMap.put(UserRolesEnum.ROLE_USER, userCapabilities);
	}
	
	public static Collection<UserPermissionsEnum> getPermissionsForRole(UserRolesEnum role) {
		return rolePermissionMap.get(role);
	}
	
	public static Collection<UserPermissionsEnum> getPermissionsForRoles(Collection<UserRolesEnum> roles) {
		Collection<UserPermissionsEnum> permissions = new ArrayList<>();
		for (UserRolesEnum r : roles) {
			for (UserPermissionsEnum p : rolePermissionMap.get(r)) {
				if (!permissions.contains(p)) {
					permissions.add(p);
				}
			}
		}
		return permissions;
	}
	
	public static Collection<GrantedAuthority> getAuthoritiesForRole(UserRolesEnum role) {
		Collection<GrantedAuthority> authorities = new ArrayList<>();
		for (UserPermissionsEnum p : rolePermissionMap.get(role)) {
			authorities.add(new SimpleGrantedAuthority(p.name()));
		}
		return authorities;
	}
	
	public static Collection<GrantedAuthority> getAuthoritiesForRoles(Collection<UserRolesEnum> roles) {
		Collection<GrantedAuthority> authorities = new ArrayList<>();
		for (UserPermissionsEnum p : getPermissionsForRoles(roles)) {
			GrantedAuthority auth = new SimpleGrantedAuthority(p.name());
			if (!authorities.contains(auth)) {
				authorities.add(auth);
			}
		}
		return authorities;
	}
	
	public static boolean isCapabilityCoveredByRole(UserPermissionsEnum capabililty, UserRolesEnum role) {
		return rolePermissionMap.get(role).contains(capabililty);
	}

}
