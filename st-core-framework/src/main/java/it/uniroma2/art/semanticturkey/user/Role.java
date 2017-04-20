package it.uniroma2.art.semanticturkey.user;

public class Role {
	
	public static enum RoleLevel { system, project };
	
	private String name;
	private RoleLevel level;
	
	public Role(String name, RoleLevel level) {
		this.name = name;
		this.level = level;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public RoleLevel getLevel() {
		return level;
	}

	public void setLevel(RoleLevel level) {
		this.level = level;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
	    if (other == this) return true;
	    if (!(other instanceof Role))return false;
	    Role otherRole = (Role)other;
	    return (otherRole.getName().equals(this.name) && otherRole.getLevel() == this.level);
	}
	
	@Override
	public String toString() {
		return this.name + "(" + this.level + ")";
	}

}
