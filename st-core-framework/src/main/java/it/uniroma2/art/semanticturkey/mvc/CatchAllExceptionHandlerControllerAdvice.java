package it.uniroma2.art.semanticturkey.mvc;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.validator.method.MethodConstraintViolation;
import org.hibernate.validator.method.MethodConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.w3c.dom.Document;

import it.uniroma2.art.semanticturkey.constraints.MsgInterpolationVariables;
import it.uniroma2.art.semanticturkey.servlet.JSONResponse;
import it.uniroma2.art.semanticturkey.servlet.Response;
import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.utilities.XMLHelp;

/**
 * ControllerAdvice that handles the exception thrown by Semantic Turkey services (published using the MVC
 * framework). The HTTP status is 200 OK (at the communication protocol level), while the response body is an
 * XML representation of the error (at the application level).
 */
@ControllerAdvice
public class CatchAllExceptionHandlerControllerAdvice {

	private static Logger logger = LoggerFactory.getLogger(CatchAllExceptionHandlerControllerAdvice.class);
	
	/**
	 * In st-core-services some methods are secured with @PreAuthorized annotation. 
	 * When an authenticated user that has no enough privileges try to access the method, 
	 * an AccessDeniedException is thrown. This exception should be handled by 
	 * STAccessDeniedHandler (a custom AccessDeniedHandler).
	 * The following ExceptionHandler prevent @ExceptionHandler(Exception.class) from catching the
	 * AccessDeniedException and simply re-throw it so that it bubble up untill the STAccessDeniedHandler
	 * @param ex
	 * @param request
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public void handleException(AccessDeniedException ex, HttpServletRequest request) {
		
		logger.error("Exception catched by the Controller Advice", ex);
		throw new AccessDeniedException(ex.getMessage());
	}
	
	@ExceptionHandler(MethodConstraintViolationException.class)
	public ResponseEntity<String> handleException(MethodConstraintViolationException ex,
			HttpServletRequest request) {
		
		logger.error("Exception catched by the Controller Advice", ex);
		
		ServletUtilities servUtils = ServletUtilities.getService();

		StringBuilder errorMsg = new StringBuilder();
		for (MethodConstraintViolation<?> viol : ex.getConstraintViolations()) {
			errorMsg.append(viol.getMessage().replace(MsgInterpolationVariables.invalidParamValuePlaceHolder,
					"" + viol.getInvalidValue()));
			errorMsg.append("\n\n");
		}

		// Despite MethodConstraintViolationException being a RuntimeException, return an ST exception
		Response stResp = servUtils.createExceptionResponse(extractServicRequestName(request),
				errorMsg.toString());

		return formatResponse(stResp, request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleException(Exception ex, HttpServletRequest request) {
		ServletUtilities servUtils = ServletUtilities.getService();

		logger.error("Exception catched by the Controller Advice", ex);
		
		Response stResp;

		if (ex instanceof RuntimeException) {
			// if a RuntimeException (i.e., unexpected failure), return an ST error
			stResp = servUtils.createErrorResponse(extractServicRequestName(request), ex.toString());
		} else {
			// otherwise, it is a foreseen failure, therefore return an ST exception
			stResp = servUtils.createExceptionResponse(extractServicRequestName(request), ex.toString());
		}
		return formatResponse(stResp, request);
	}

	private ResponseEntity<String> formatResponse(Response stResp, HttpServletRequest request) {
		HttpHeaders responseHeaders = new HttpHeaders();

		String contentType;
		String respContent = null;

		// 1) we could write to output stream directly (see original ST or LegacyServiceController)
		// 2) actually JSON is not handled here! create a format-agnostic response and then serializers
		if (stResp instanceof JSONResponse) {
			contentType = "application/json";
			respContent = stResp.getResponseContent();
		} else {
			contentType = "application/xml";
			respContent = XMLHelp.XML2String((Document) stResp.getResponseObject(), true);
		}
		responseHeaders.set("Content-Type", contentType + "; charset=UTF-8");
		return new ResponseEntity<String>(respContent, responseHeaders, HttpStatus.OK);
	}

	private static String extractServicRequestName(HttpServletRequest request) {
		String path = request.getServletPath();
		String[] pathElements = path.split("/");

		return pathElements[pathElements.length - 1];
	}

}
