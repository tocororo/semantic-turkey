package it.uniroma2.art.semanticturkey.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import it.uniroma2.art.semanticturkey.services.ServiceSpecies;

/**
 * An {@link HandlerInterceptor} that stores the current request in a thread-local store.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class RequestListenerHandlerInterceptor implements HandlerInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(RequestListenerHandlerInterceptor.class);

	private static ThreadLocal<HttpServletRequest> requestHolder = new ThreadLocal<>();

	public static HttpServletRequest getRequest() {
		return requestHolder.get();
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		ResolvedRequestHolder.setRequest(request);
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		// nothing to do
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) throws Exception {
		ResolvedRequestHolder.removeRequest();
	}
}
