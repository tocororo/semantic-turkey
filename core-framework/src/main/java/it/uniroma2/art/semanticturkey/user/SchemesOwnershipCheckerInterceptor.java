package it.uniroma2.art.semanticturkey.user;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Deleted;
import it.uniroma2.art.semanticturkey.services.annotations.Modified;
import it.uniroma2.art.semanticturkey.services.annotations.SchemeAssignment;
import it.uniroma2.art.semanticturkey.services.support.STServiceContextUtils;

public class SchemesOwnershipCheckerInterceptor  implements MethodInterceptor {
	
	@Autowired
	private STServiceContext stServiceContext;

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		
		STUser user = UsersManager.getLoggedUser();
		Project project = stServiceContext.getProject();
		UsersGroup group = ProjectUserBindingsManager.getPUBinding(user, project).getGroup();
		
		if (!user.isAdmin() && group != null) { //Do the checks only if user is not the admin and belongs to a group

			List<IRI> assignedSchemes = null; //list of schemes annotated with @SchemeAssignemnt
			IRI modifiedArg = null; //resource annotated with @Modified
			IRI deletedArg = null; //resource annotated with @Deleted
			
			Method method = invocation.getMethod();
			Object[] args = invocation.getArguments();
			
			Parameter[] params = method.getParameters();
			for (int i = 0; i < params.length; i++) {
				//if the parameter has the @SchemeAssignment annotation and the related argument is an IRI or a list of IRI
				if (params[i].getAnnotation(SchemeAssignment.class) != null) {
					if (args[i] instanceof List) {
						assignedSchemes = (List<IRI>) args[i];
					} else if (args[i] instanceof IRI) {
						assignedSchemes = Arrays.asList((IRI)args[i]);
					}
					break;
				}
				//if the parameter has the @Modified annotation and the related argument is an IRI
				if (params[i].getAnnotation(Modified.class) != null && args[i] instanceof IRI) {
					modifiedArg = (IRI) args[i];
				}
				//if the parameter has the @Modified annotation and the related argument is an IRI
				if (params[i].getAnnotation(Deleted.class) != null && args[i] instanceof IRI) {
					deletedArg = (IRI) args[i];
				}
			}
			
			if (assignedSchemes != null) {
				if (!ProjectGroupBindingsManager.hasUserOwnershipOfSchemes(UsersManager.getLoggedUser(), stServiceContext.getProject(), assignedSchemes, false)) {
					String errorMsg;
					if (assignedSchemes.size() == 1) {
						throw OperationOnResourceDeniedException.missingSchemeOwnership(group.getShortName());
					} else {
						throw OperationOnResourceDeniedException.missingSchemesOwnership(group.getShortName());
					}
				}
			}
			if (modifiedArg != null) {
				//check if the modifiedArg is a scheme (or a concept that belongs to a scheme) which the user has the ownership
				
//				String query = "SELECT DISTINCT ?scheme WHERE {\n"
//						+ "{\n"
//						+ "?inSchemeProp " + NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF) + "* " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " .\n"
//						+ NTriplesUtil.toNTriplesString(modifiedArg) + " ?inSchemeProp ?scheme .\n"
//						+ "} UNION {\n"
//						+ "?schemeClass " + NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF) + "* " + NTriplesUtil.toNTriplesString(SKOS.CONCEPT_SCHEME) + " .\n"
//						+ NTriplesUtil.toNTriplesString(modifiedArg) + " a ?schemeClass .\n"
//						+ "BIND(" + NTriplesUtil.toNTriplesString(modifiedArg) + " as ?scheme) .\n"
//						+ "}\n"
//						+ "}";

//				String conceptCase = "concept";
//				String schemeCase = "scheme";
//				String xLabelConcept = "xLabelConcept";
//				String xLabelScheme = "xLabelScheme";
//				String noteConcept = "noteConcept";
//				String noteScheme = "noteScheme";
				
				String query = "SELECT DISTINCT ?scheme WHERE {\n"
						//modifiedArg is a concept?
						+ "{\n"
						+ "	?inSchemeProp " + NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF) + "* " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " .\n"
						+ NTriplesUtil.toNTriplesString(modifiedArg) + " ?inSchemeProp ?scheme .\n"
//				        + "BIND('" + conceptCase + "' as ?type) .\n"
				        + "} UNION {\n"
				        //modifiedArg is a conceptScheme?
						+ "?schemeClass " + NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF) + "* " + NTriplesUtil.toNTriplesString(SKOS.CONCEPT_SCHEME) + " .\n"
						+ NTriplesUtil.toNTriplesString(modifiedArg) + " a ?schemeClass .\n"
						+ "BIND(" + NTriplesUtil.toNTriplesString(modifiedArg) + " as ?scheme) .\n"
//				        + "BIND('" + schemeCase + "' as ?type) .\n"
				        + "} UNION {\n"
				        //modifiedArg is a xLabel of a concept?
				        + "?xLabelClass " + NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF) + "* " + NTriplesUtil.toNTriplesString(SKOSXL.LABEL) + " .\n"
				        + NTriplesUtil.toNTriplesString(modifiedArg) + " a ?xLabelClass .\n"
				        + "?concept ?skosxlPred " + NTriplesUtil.toNTriplesString(modifiedArg) + " .\n"
				        + "?inSchemeProp " + NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF) + "* " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " .\n"
						+ "?concept ?inSchemeProp ?scheme .\n"
//				        + "BIND('" + xLabelConcept + "' as ?type) .\n"
				        + "} UNION {\n"
				        //modifiedArg is a xLabel of a conceptScheme?
				        + "?xLabelClass " + NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF) + "* " + NTriplesUtil.toNTriplesString(SKOSXL.LABEL) + " .\n"
				        + NTriplesUtil.toNTriplesString(modifiedArg) + " a ?xLabelClass .\n"
				        + "?scheme ?skosxlPred " + NTriplesUtil.toNTriplesString(modifiedArg) + " .\n"
				        + "?schemeClass " + NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF) + "* " + NTriplesUtil.toNTriplesString(SKOS.CONCEPT_SCHEME) + " .\n"
						+ "?scheme a ?schemeClass .\n"
//				        + "BIND('" + xLabelScheme + "' as ?type) .\n"
				        + "} UNION {\n"
				        //modifiedArg is a reified note of a concept?
				        + "?notePred " + NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF) + "* " + NTriplesUtil.toNTriplesString(SKOS.NOTE) + " .\n"
				     	+ "?concept ?notePred " + NTriplesUtil.toNTriplesString(modifiedArg) + " .\n"
				        + "?inSchemeProp " + NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF) + "* " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " .\n"
						+ "?concept ?inSchemeProp ?scheme .\n"
//				        + "BIND('" + noteConcept + "' as ?type) .\n"
				        + "} UNION {\n"
				        //modifiedArg is a reified note of a conceptScheme?
				        + "?notePred " + NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF) + "* " + NTriplesUtil.toNTriplesString(SKOS.NOTE) + " .\n"
				        + "?scheme ?notePred " + NTriplesUtil.toNTriplesString(modifiedArg) + " .\n"
				        + "?schemeClass " + NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF) + "* " + NTriplesUtil.toNTriplesString(SKOS.CONCEPT_SCHEME) + " .\n"
						+ "?scheme a ?schemeClass .\n"
//				        + "BIND('" + noteScheme + "' as ?type) .\n"
				        + "}\n"
				        + "}";
				
				RepositoryConnection repoConnection = STServiceContextUtils.getRepostory(stServiceContext).getConnection();
				TupleQueryResult results = repoConnection.prepareTupleQuery(query).evaluate();
				List<IRI> schemes = new ArrayList<IRI>();
				while (results.hasNext()) {
					BindingSet bs = results.next();
					IRI scheme = (IRI) bs.getValue("scheme");
					schemes.add(scheme);
				}
				if (!schemes.isEmpty()) {
					//do the check of the ownership only if schemes is not empty (it means that the modified resource belongs to a scheme or it's a scheme)
					if (!ProjectGroupBindingsManager.hasUserOwnershipOfSchemes(UsersManager.getLoggedUser(), stServiceContext.getProject(), schemes, true)) {
						throw OperationOnResourceDeniedException.resourceModificationForbidden(modifiedArg, group.getShortName());
					}
				}
			}
			if (deletedArg != null) {
				//check if the deletedArg is a scheme (or a concept that belongs to a scheme) which the user has the ownership
				String query = "SELECT DISTINCT ?scheme WHERE {\n"
						+ "{\n"
						+ "?inSchemeProp " + NTriplesUtil.toNTriplesString(RDFS.SUBPROPERTYOF) + "* " + NTriplesUtil.toNTriplesString(SKOS.IN_SCHEME) + " .\n"
						+ NTriplesUtil.toNTriplesString(deletedArg) + " ?inSchemeProp ?scheme .\n"
						+ "} UNION {\n"
						+ "?schemeClass " + NTriplesUtil.toNTriplesString(RDFS.SUBCLASSOF) + "* " + NTriplesUtil.toNTriplesString(SKOS.CONCEPT_SCHEME) + " .\n"
						+ NTriplesUtil.toNTriplesString(deletedArg) + " a ?schemeClass .\n"
						+ "BIND(" + NTriplesUtil.toNTriplesString(deletedArg) + " as ?scheme) .\n"
						+ "}\n"
						+ "}";
				RepositoryConnection repoConnection = STServiceContextUtils.getRepostory(stServiceContext).getConnection();
				TupleQueryResult results = repoConnection.prepareTupleQuery(query).evaluate();
				List<IRI> schemes = new ArrayList<IRI>();
				while (results.hasNext()) {
					BindingSet bs = results.next();
					IRI scheme = (IRI) bs.getValue("scheme");
					schemes.add(scheme);
				}
				if (!schemes.isEmpty()) {
					//do the check of the ownership only if schemes is not empty (it means that the deleted resource belongs to a scheme or it's a scheme)
					if (!ProjectGroupBindingsManager.hasUserOwnershipOfSchemes(UsersManager.getLoggedUser(), stServiceContext.getProject(), schemes, false)) {
						throw OperationOnResourceDeniedException.resourceDeletionForbidden(deletedArg, group.getShortName());
					}
				}
			}
		} //else if user doesn't belong to any group, he can modify any concept
		
		
		return invocation.proceed();
	}

}
