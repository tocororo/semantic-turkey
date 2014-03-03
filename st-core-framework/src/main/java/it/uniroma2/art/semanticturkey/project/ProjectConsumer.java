package it.uniroma2.art.semanticturkey.project;

/**
 * This interface identifies "environments" in which projects may be open and their data be consumed. All
 * {@link Project}s are valid {@link ProjectConsumer}s, in that, within an open project, it is possible to
 * open other projects to compare/use their data in combination with their own data. There exists a special
 * consumer, called {@link ProjectConsumer#SYSTEM} that represents the system environment, when a project is
 * opened as a primary project.
 * 
 * @author Manuel Fiorelli &lt;fiorelli@info.uniroma2.it&gt;
 * @author Armando Stellato &lt;stellato@info.uniroma2.it&gt;
 * @author Andrea Turbati &lt;turbati@info.uniroma2.it&gt;
 * 
 */
public interface ProjectConsumer {

	/**
	 * SYSTEM is the default project consumer
	 */
	public static ProjectConsumer SYSTEM = new ProjectConsumer() {

		private String name = "SYSTEM";
		
		public String getName() {
			return name;
		}

		// a SYSTEM instance should always be this static field, so the equals implementation just checks for
		// identity
		public boolean equals(Object o) {

			if (this == o) {
				return true;
			}

			return false;
		}

		public int hashCode() {
			return getName().hashCode();
		}
		
		public String toString() {
			return getName();
		}

	};

	public String getName();

}
