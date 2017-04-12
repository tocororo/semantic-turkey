package it.uniroma2.art.semanticturkey.rbac;

import java.util.HashMap;
import java.util.Map;

import alice.tuprolog.InvalidTheoryException;

public class RBACManager {
	
	private static Map<String, RBACProcessor> rbacMap = new HashMap<>(); //role-RBACProcessor
	
	private static RBACProcessor initRBACProcessor(String role) throws InvalidTheoryException, TheoryNotFoundException {
		RBACProcessor rbac = new RBACProcessor(role);
		rbacMap.put(role, rbac);
		return rbac;
	}
	
	public static synchronized RBACProcessor getRBACProcessor(String role) throws InvalidTheoryException, TheoryNotFoundException {
		RBACProcessor rbac = rbacMap.get(role);
		if (rbac == null) {
			rbac = initRBACProcessor(role);
		}
		return rbac;
	}

}
