package it.uniroma2.art.semanticturkey.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import it.uniroma2.art.owlart.exceptions.ModelUpdateException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.services.ServiceSpecies;

/**
 * An {@link HandlerInterceptor} that implements connection management for legacy (e.g. Cls in
 * st-core-services) and new-style services (e.g. Resource View in st-core-services). Such services should not
 * be confused with newer new-style services, which are being introduced.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class LegacyAndNewStyleServiceConnectioManagementHandlerInterceptor implements HandlerInterceptor {

	private static final Logger logger = LoggerFactory
			.getLogger(LegacyAndNewStyleServiceConnectioManagementHandlerInterceptor.class);

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		logger.debug("pre handle: " + request);

		if (shouldManage(handler)) {

			logger.debug("either legacy or new-style service");

			String projectName = request.getParameter("ctx_project");

			if (projectName != null) {

				logger.debug("uses project");

				Project project = ProjectManager.getProject(projectName);

				if (project == null) {
					logger.warn("Project not open: " + projectName);
					return true;
				}

				project.createModelAndBoundToThread();
			} else {
				logger.debug("does not use project");
			}
		}
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		logger.debug("post handle: " + request);

		if (shouldManage(handler)) {

			logger.debug("either legacy or new-style service");

			String projectName = request.getParameter("ctx_project");

			if (projectName != null) {

				logger.debug("uses project");

				Project project = ProjectManager.getProject(projectName);

				if (project == null) {
					logger.warn(
							"Could not unbound model from thread because after service handling the project is close: "
									+ projectName);
					return;
				}

				project.unbindModelFromThread();
			} else {
				logger.debug("does not use project");
			}

		}
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) throws Exception {
		// We expect that the model has already been unbound from the thread. However, in case of abrupt
		// termination of a handler, the method postHandle is not invoked. Thus, we repeat the release here. 
		if (shouldManage(handler)) {
			String projectName = request.getParameter("ctx_project");
			if (projectName != null) {
				Project project = ProjectManager.getProject(projectName);
				if (project != null) {
					try {
						project.unbindModelFromThread();
					} catch (ModelUpdateException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Checks whether the given {@code handler} is either an old-style or a new-style service.
	 * 
	 * @param handler
	 * @return
	 */
	public static boolean shouldManage(Object handler) {
		if (handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;
			Object handlingBean = handlerMethod.getBean();

			if (handlingBean instanceof IntrospectableController) {
				ServiceSpecies serviceSpecies = ((IntrospectableController) handlingBean).getServiceSpecies();
				if (serviceSpecies == ServiceSpecies.OldStyle || serviceSpecies == ServiceSpecies.NewStyle) {
					return true;
				}
			}

		}

		return false;
	}
}
