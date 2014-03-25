package it.uniroma2.art.semanticturkey.mvc;

import it.uniroma2.art.semanticturkey.servlet.ServletUtilities;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseERROR;
import it.uniroma2.art.semanticturkey.servlet.XMLResponseEXCEPTION;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * ControllerAdvice that handles the exception thrown by Semantic Turkey services (published using the MVC
 * framework). The HTTP status is 200 OK (at the communication protocol level), while the response body is an
 * XML representation of the error (at the application level).
 */
@ControllerAdvice
public class CatchAllExceptionHandlerControllerAdvice {

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<String> exceptionHandler(RuntimeException e) {
		XMLResponseERROR response = ServletUtilities.getService().createErrorResponse("", e.getMessage());
		return new ResponseEntity<String>(response.getResponseContent(), HttpStatus.OK);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> exceptionHandler(Exception e) {
		XMLResponseEXCEPTION response = ServletUtilities.getService().createExceptionResponse("",
				e.getMessage());
		return new ResponseEntity<String>(response.getResponseContent(), HttpStatus.OK);
	}
}
