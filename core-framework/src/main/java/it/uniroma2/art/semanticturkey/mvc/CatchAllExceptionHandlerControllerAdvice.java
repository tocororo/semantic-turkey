package it.uniroma2.art.semanticturkey.mvc;

import java.io.StringWriter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import it.uniroma2.art.semanticturkey.event.annotation.EventListener;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchSettingsManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.settings.core.CoreSystemSettings;
import it.uniroma2.art.semanticturkey.settings.core.ErrorReportingSettings;
import it.uniroma2.art.semanticturkey.settings.core.SemanticTurkeyCoreSettingsManager;
import it.uniroma2.art.semanticturkey.settings.events.SettingsEvent;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.rdf4j.exceptions.ValidationException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.hibernate.validator.method.MethodConstraintViolation;
import org.hibernate.validator.method.MethodConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;
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

	private AtomicReference<ErrorReportingSettings> errorReportingSettings = new AtomicReference<>();

	@Autowired
	protected ExtensionPointManager exptMgr;

	@EventListener
	public void onSettingsUpdated(SettingsEvent event) {
		// skips events not related to the core settings
		if (!Objects.equals(event.getSettingsManager().getId(), SemanticTurkeyCoreSettingsManager.class.getName())) return;

		// skips events not related to the system settings (or their defaults)
		if (!Objects.equals(event.getScope(), Scope.SYSTEM)) return;

		errorReportingSettings.set(null);
	}

	@Nullable
	public ErrorReportingSettings getErrorReportingSettings() {
		return errorReportingSettings.updateAndGet(current -> {
			if (current != null) {
				return current;
			}

			try {
				CoreSystemSettings coreSystemSettings = (CoreSystemSettings) exptMgr.getSettings(null, null, null, SemanticTurkeyCoreSettingsManager.class.getName(), Scope.SYSTEM);
				return coreSystemSettings.errorReporting;
			} catch (STPropertyAccessException|NoSuchSettingsManager e) {
				ExceptionUtils.rethrow(e);
				return null; // never reached
			}
		});
	}

	/**
	 * In st-core-services some methods are secured with @PreAuthorized annotation. When an authenticated user
	 * that has no enough privileges try to access the method, an AccessDeniedException is thrown. This
	 * exception should be handled by STAccessDeniedHandler (a custom AccessDeniedHandler). The following
	 * ExceptionHandler prevent @ExceptionHandler(Exception.class) from catching the AccessDeniedException and
	 * simply re-throw it so that it bubble up untill the STAccessDeniedHandler
	 * 
	 * @param ex
	 * @param request
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public void handleException(AccessDeniedException ex, HttpServletRequest request) {

		logger.error("Exception catched by the Controller Advice", ex);
		throw new AccessDeniedException(ex.getMessage());
	}

	@ExceptionHandler(TransactionSystemException.class)
	public ResponseEntity<String> handleException(TransactionSystemException ex, HttpServletRequest request) {

		logger.error("Exception catched by the Controller Advice", ex);

		Throwable cause = ex.getRootCause();
		if (cause instanceof ValidationException) {
			Model validatioReport = ((ValidationException) cause).validationReportAsModel();

			StringWriter reportWriter = new StringWriter();

			WriterConfig writerConfig = new WriterConfig();
			writerConfig.set(BasicWriterSettings.PRETTY_PRINT, true);
			writerConfig.set(BasicWriterSettings.INLINE_BLANK_NODES, true);

			Rio.write(validatioReport, reportWriter, RDFFormat.TURTLE, writerConfig);

			StringBuilder errorMsg = new StringBuilder();
			errorMsg.append("SHACL validation failed with the following report:\n\n").append(reportWriter);

			ServletUtilities servUtils = ServletUtilities.getService();
			Response stResp = servUtils.createExceptionResponse(extractServicRequestName(request), ex,
					errorMsg.toString(), getErrorReportingSettings());

			return formatResponse(stResp, request);

		} else { // Otherwise, fall back to the default exception handler
			return handleException((Exception) ex, request);
		}
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
		Response stResp = servUtils.createExceptionResponse(extractServicRequestName(request), ex,
				errorMsg.toString(), getErrorReportingSettings());

		return formatResponse(stResp, request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleException(Exception ex, HttpServletRequest request) {
		ServletUtilities servUtils = ServletUtilities.getService();

		logger.error("Exception catched by the Controller Advice", ex);

		Response stResp;

		if (ex instanceof RuntimeException) {
			// if a RuntimeException (i.e., unexpected failure), return an ST error
			stResp = servUtils.createErrorResponse(extractServicRequestName(request), ex, ex.toString(), getErrorReportingSettings());
		} else {
			// otherwise, it is a foreseen failure, therefore return an ST exception
			stResp = servUtils.createExceptionResponse(extractServicRequestName(request), ex, ex.toString(), getErrorReportingSettings());
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
