package it.uniroma2.art.semanticturkey.user;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.user.Role.RoleLevel;
import it.uniroma2.art.semanticturkey.vocabulary.UserVocabulary;

public class ProjectUserBindingsRepoHelper {
	
	protected static Logger logger = LoggerFactory.getLogger(ProjectUserBindingsRepoHelper.class);
	
	private static String BINDING_USER = "user";
	private static String BINDING_ROLE = "role";
	private static String BINDING_LANGUAGE = "language";
	private static String BINDING_GROUP = "group";
	private static String BINDING_GROUP_LIMITATION = "groupLimitation";
	private static String BINDING_PROJECT = "project";
	
	private Repository repository;
	
	public ProjectUserBindingsRepoHelper() {
		MemoryStore memStore = new MemoryStore();
		memStore.setPersist(false);
		repository = new SailRepository(memStore);
		repository.init();
	}
	
	public void loadBindingDetails(File bindingDetailsFile) throws RDFParseException, RepositoryException, IOException {
		RepositoryConnection conn = repository.getConnection();
		conn.add(bindingDetailsFile, UserVocabulary.BASEURI, RDFFormat.TURTLE);
		conn.close();
	}
	
	/**
	 * Insert the given ProjectUserBinding into the repository
	 * @param puBinding
	 */
	public void insertBinding(ProjectUserBinding puBinding) {
		String query = "INSERT DATA {"
				+ " _:binding a " + NTriplesUtil.toNTriplesString(UserVocabulary.BINDING) + " ."
				+ " _:binding " + NTriplesUtil.toNTriplesString(UserVocabulary.USER_PROP) + " " 
				+ NTriplesUtil.toNTriplesString(puBinding.getUser().getIRI()) + " ."
				+ " _:binding " + NTriplesUtil.toNTriplesString(UserVocabulary.PROJECT_PROP) + " " 
				+ NTriplesUtil.toNTriplesString(getProjectIRI(puBinding.getProject())) + " .";
		for (Role role : puBinding.getRoles()) {
			query += " _:binding " + NTriplesUtil.toNTriplesString(UserVocabulary.ROLE_PROP) + " " 
					+ NTriplesUtil.toNTriplesString(getRoleIRI(role, puBinding.getProject())) + " .";
		}
		for (String lang : puBinding.getLanguages()) {
			query += " _:binding " + NTriplesUtil.toNTriplesString(UserVocabulary.LANGUAGE_PROP) + " '" + lang + "' .";
		}
		if (puBinding.getGroup() != null) {
			query += " _:binding " + NTriplesUtil.toNTriplesString(UserVocabulary.GROUP_PROP) + " " 
					+ NTriplesUtil.toNTriplesString(puBinding.getGroup().getIRI()) + " ."
					+ " _:binding " + NTriplesUtil.toNTriplesString(UserVocabulary.GROUP_LIMITATIONS_PROP) + " "
					+ NTriplesUtil.toNTriplesString(SimpleValueFactory.getInstance().createLiteral(puBinding.isSubjectToGroupLimitations())) + " .";
		}
		query += " }";
		
		//execute query
		logger.debug(query);
		try (RepositoryConnection conn = repository.getConnection()) {
			Update update = conn.prepareUpdate(query);
			update.execute();
		}
	}
	
	/**
	 * Returns the binding about the given user and project
	 * @return
	 */
	public Collection<ProjectUserBinding> listPUBindings() {
		String query = "SELECT * WHERE {"
				+ " ?binding a " + NTriplesUtil.toNTriplesString(UserVocabulary.BINDING) + " ."
				+ " ?binding " + NTriplesUtil.toNTriplesString(UserVocabulary.USER_PROP) + " ?" + BINDING_USER + " ."
				+ " ?binding " + NTriplesUtil.toNTriplesString(UserVocabulary.PROJECT_PROP) + " ?" + BINDING_PROJECT + " ."
				+ " OPTIONAL { ?binding " + NTriplesUtil.toNTriplesString(UserVocabulary.ROLE_PROP) + " ?" + BINDING_ROLE + " . }"
				+ " OPTIONAL { ?binding " + NTriplesUtil.toNTriplesString(UserVocabulary.LANGUAGE_PROP) + " ?" + BINDING_LANGUAGE + " . }"
				+ " OPTIONAL {"
				+ " ?binding " + NTriplesUtil.toNTriplesString(UserVocabulary.GROUP_PROP) + " ?" + BINDING_GROUP + " ."
				+ " OPTIONAL { ?binding " + NTriplesUtil.toNTriplesString(UserVocabulary.GROUP_LIMITATIONS_PROP) + " ?" + BINDING_GROUP_LIMITATION + " . }"
				+ " }"
				+ " }";
		// execute query
		logger.debug(query);
		TupleQueryResult result = null;
		try (RepositoryConnection conn = repository.getConnection()) {
			TupleQuery tq = conn.prepareTupleQuery(query);
			result = tq.evaluate();
			// collect bindings
			return getPUBindingsFromTupleResult(result);
		} finally {
			if (result != null) {
				result.close();
			}
		}
	}
	
	private Collection<ProjectUserBinding> getPUBindingsFromTupleResult(TupleQueryResult result) {
		// collect puBindings
		Collection<ProjectUserBinding> list = new ArrayList<>();
		tupleLoop: while (result.hasNext()) {
			BindingSet tuple = result.next();

			IRI projIRI = (IRI) tuple.getValue(BINDING_PROJECT);
			AbstractProject project = getProjectFromIRI(projIRI);
			if (project == null) { //there is a binding that references a no more existing project
				logger.warn("Invalid binding: IRI " + projIRI.stringValue() + " references to a not existing project."
						+ " Project-User bindings with this project will be ignored");
				continue; // => binding ignored
			}
			
			IRI userIRI = (IRI) tuple.getValue(BINDING_USER);
			try {
				STUser user = UsersManager.getUser(userIRI);

				ProjectUserBinding puBinding = new ProjectUserBinding(project, user);

				Role role = null;
				if (tuple.getBinding(BINDING_ROLE) != null) {
					role = getRoleFromIRI((IRI) tuple.getValue(BINDING_ROLE));
				}
				String lang = null;
				if (tuple.getBinding(BINDING_LANGUAGE) != null) {
					lang = tuple.getValue(BINDING_LANGUAGE).stringValue();
				}
				// Check if the current tuple is about a binding already fetched (and so it differs just for role or lang)
				for (ProjectUserBinding b : list) {
					if (b.getProject().getName().equals(project.getName()) && b.getUser().getIRI().equals(user.getIRI())) {
						// binding already in list => add the role or the lang to it
						if (role != null) {
							b.addRole(role);
						}
						if (lang != null) {
							b.addLanguage(lang);
						}
						continue tupleLoop;
					}
				}
				//if it reach this point, current binding was not already fetched so add the role to the binding
				if (role != null) {
					puBinding.addRole(role);
				}
				if (lang != null) {
					puBinding.addLanguage(lang);
				}

				UsersGroup group;
				if (tuple.getBinding(BINDING_GROUP) != null) {
					group = UsersGroupsManager.getGroupByIRI((IRI) tuple.getBinding(BINDING_GROUP).getValue());
					puBinding.assignGroup(group);
					if (tuple.getBinding(BINDING_GROUP_LIMITATION) != null) {
						puBinding.setSubjectToGroupLimitations(Boolean.parseBoolean(tuple.getBinding(BINDING_GROUP_LIMITATION).getValue().stringValue()));
					}
				}

				list.add(puBinding);
			} catch (UserException e) { //a binding references a no more existing user
				logger.warn("Invalid binding: IRI " + userIRI.stringValue() + " references to a not existing user."
						+ " Project-User bindings with this user will be ignored");
			}
		}
		return list;
	}
	
	/**
	 * Serializes the binding in the given file
	 * @param file
	 * @throws IOException
	 */
	public void saveBindingDetailsFile(File file) throws IOException {
		try (RepositoryConnection conn = repository.getConnection()) {
			RepositoryResult<Statement> stats = conn.getStatements(null, null, null, false);
			Model model = Iterations.addAll(stats, new LinkedHashModel());
			try (FileOutputStream out = new FileOutputStream(file)) {
				RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, out);
				writer.startRDF();
				for (Statement st : model) {
					writer.handleStatement(st);
				}
				writer.endRDF();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void shutDownRepository() {
		repository.shutDown();
	}
	
	private IRI getProjectIRI(AbstractProject project) {
		return SimpleValueFactory.getInstance().createIRI(UserVocabulary.PROJECTSBASEURI, encodeProjectName(project));
	}
	
	private IRI getRoleIRI(Role role, AbstractProject project) {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();
		if (role.getLevel() == RoleLevel.project) {
			return vf.createIRI(UserVocabulary.ROLESBASEURI, encodeProjectName(project) + "/" + role.getName());
		} else { //system level
			return vf.createIRI(UserVocabulary.ROLESBASEURI, role.getName());
		}
	}
	
	private AbstractProject getProjectFromIRI(IRI projectIRI) {
		try {
			String projectName = "";
			try {
				projectName = URLDecoder.decode(projectIRI.getLocalName(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return ProjectManager.getProjectDescription(projectName);
		} catch (InvalidProjectNameException | ProjectInexistentException | ProjectAccessException e) {
			logger.warn("Invalid binding: IRI " + projectIRI.stringValue()
				+ " references to a not existing project");
			return null;
		}
	}
	
	private Role getRoleFromIRI(IRI roleIRI) {
		String roleString = roleIRI.stringValue(); 
		String projAndRole = roleString.substring(UserVocabulary.ROLESBASEURI.length());
		try {
			projAndRole = URLDecoder.decode(projAndRole, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		int separatorIdx = projAndRole.indexOf("/");
		if (separatorIdx == -1) { //no separator => uri doesn't contain the project name => system role
			return new Role(projAndRole, RoleLevel.system);
		} else {
			String roleName = projAndRole.substring(separatorIdx + 1);
			return new Role(roleName, RoleLevel.project);
		}
		
	}
	
	public static String encodeProjectName(AbstractProject project) {
		String encodedProjName = "";
		try {
			encodedProjName = URLEncoder.encode(project.getName(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return encodedProjName;
	}

}
