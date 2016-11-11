package it.uniroma2.art.semanticturkey.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.uniroma2.art.semanticturkey.user.UserPermissionsEnum;
import it.uniroma2.art.semanticturkey.user.UserRolesEnum;

public class PermissionManager {
	
	private static Map<UserRolesEnum, List<UserPermissionsEnum>> rolePermissionMap;
	static {
		rolePermissionMap = new HashMap<UserRolesEnum, List<UserPermissionsEnum>>();
		List<UserPermissionsEnum> adminCapabilities = new ArrayList<UserPermissionsEnum>();
		adminCapabilities.add(UserPermissionsEnum.CAPABILITY_1);
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
	
	public static List<UserPermissionsEnum> getPermissionsForRole(UserRolesEnum role) {
		return rolePermissionMap.get(role);
	}
	
	public static boolean isCapabilityCoveredByRole(UserPermissionsEnum capabililty, UserRolesEnum role) {
		return rolePermissionMap.get(role).contains(capabililty);
	}

}
