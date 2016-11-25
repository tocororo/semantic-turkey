package it.uniroma2.art.semanticturkey.user;

import java.util.ArrayList;
import java.util.Collection;

public class STRole {
	
	private String name;
	private Collection<UserCapabilitiesEnum> capabilities;
	
	public STRole(String name) {
		this.name = name;
		this.capabilities = new ArrayList<>(); 
	}
	
	public STRole(String name, Collection<UserCapabilitiesEnum> capabilities) {
		this.name = name;
		this.capabilities = capabilities;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String id) {
		this.name = id;
	}
	
	public Collection<UserCapabilitiesEnum> getCapabilities() {
		return this.capabilities;
	}
	
	public void setCapabilities(Collection<UserCapabilitiesEnum> capabilities) {
		this.capabilities = capabilities;
	}
	
	public void addCapability(UserCapabilitiesEnum capability) {
		if (!this.capabilities.contains(capability)) {
			this.capabilities.add(capability);
		}
	}
	
	public void removeCapability(UserCapabilitiesEnum capability) {
		this.capabilities.remove(capability);
	}
	
	public boolean isCapabilityCovered(UserCapabilitiesEnum capability) {
		return this.capabilities.contains(capability);
	}
	
	public boolean areCapabilitiesCovered(Collection<UserCapabilitiesEnum> capabilities) {
		return this.capabilities.containsAll(capabilities);
	}

}
