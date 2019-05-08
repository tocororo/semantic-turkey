package it.uniroma2.art.semanticturkey.services.core;

import java.io.IOException;
import java.time.Instant;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.history.SupportRepositoryUtils;
import it.uniroma2.art.semanticturkey.services.tracker.STServiceTracker;

/**
 * This class provides services related to the blacklist.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class Blacklist extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Blacklist.class);

	private static final String DEFAULT_PAGE_SIZE = "100";

	@Autowired
	private STServiceTracker stServiceTracker;

	@STServiceOperation(method=RequestMethod.POST)
	@Read
	// @PreAuthorize ... todo
	public void clearBlacklist() {
		IRI blacklistGraph = SupportRepositoryUtils.obtainBlacklistGraph(getManagedConnection());

		Repository supportRepository = getProject().getRepositoryManager().getRepository("support");
		try (RepositoryConnection conn = supportRepository.getConnection()) {
			conn.clear(blacklistGraph);
		}
	}

	@STServiceOperation
	@Read
	// @PreAuthorize ... todo
	public void downloadBlacklist(HttpServletResponse oRes,
			@Optional(defaultValue = "TURTLE") RDFFormat format)
			throws RepositoryException, RDFHandlerException, UnsupportedRDFormatException, IOException {
		IRI blacklistGraph = SupportRepositoryUtils.obtainBlacklistGraph(getManagedConnection());

		Repository supportRepository = getProject().getRepositoryManager().getRepository("support");

		oRes.setHeader("Content-Disposition", "attachment; filename=blacklist-" + Instant.now().toString()
				+ "." + format.getDefaultFileExtension());
		oRes.setContentType(format.getDefaultMIMEType());

		try (RepositoryConnection conn = supportRepository.getConnection()) {
			conn.export(Rio.createWriter(format, oRes.getOutputStream()), blacklistGraph);
		}

	}

}
