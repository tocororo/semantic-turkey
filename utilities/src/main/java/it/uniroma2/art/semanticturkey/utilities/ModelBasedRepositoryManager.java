package it.uniroma2.art.semanticturkey.utilities;

import java.io.IOException;

import org.eclipse.rdf4j.http.client.RDF4JProtocolSession;
import org.eclipse.rdf4j.http.client.SharedHttpClientSessionManager;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.UnauthorizedException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.config.RepositoryConfigUtil;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.SystemRepository;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

public interface ModelBasedRepositoryManager {

	public class ModelBasedRepositoryConfig extends RepositoryConfig {
		private Model model;

		public ModelBasedRepositoryConfig(Model model) {
			this.model = model;
		}

		@Override
		public String getID() {
			return Models.getPropertyString(model, getRepositoryNode(), RepositoryConfigSchema.REPOSITORYID).get();
		}

		@Override
		public String getTitle() {
			return Models.getPropertyString(model, getRepositoryNode(), RDFS.LABEL).orElse(null);
		}

		@Override
		public void export(Model model, Resource repositoryNode) {
			Resource originalRepNode = getRepositoryNode();

			model.addAll(RDF4JUtilities.substitute(this.model, originalRepNode, repositoryNode));
		}

		protected Resource getRepositoryNode() {
			Resource originalRepNode = Models
					.subject(this.model.filter(null, RepositoryConfigSchema.REPOSITORYID, null)).get();
			return originalRepNode;
		}

		@Override
		public void parse(Model model, Resource repositoryNode) throws RepositoryConfigException {
			this.model = model;
		}
	}

	Model getRepositoryConfig(String id);

	void addRepositoryConfig(Model model);

	Repository getRepository(String id);

	static class LocalRepositoryManagerAdapter implements ModelBasedRepositoryManager, AutoCloseable {

		private ValueFactory vf;
		private LocalRepositoryManager repMgr;
		private boolean shutdownOnClose;

		public LocalRepositoryManagerAdapter(LocalRepositoryManager repMgr, boolean shutdownOnClose) {
			this.vf = SimpleValueFactory.getInstance();
			this.repMgr = repMgr;
			this.shutdownOnClose = shutdownOnClose;
		}
		
		public LocalRepositoryManagerAdapter(LocalRepositoryManager repMgr) {
			this(repMgr, true);
		}


		@Override
		public Model getRepositoryConfig(String id) {
			Resource repositoryNode = vf.createBNode();
			Model model = new LinkedHashModel();
			repMgr.getRepositoryConfig(id).export(model, repositoryNode);
			return model;
		}

		@Override
		public void addRepositoryConfig(Model model) {
			RepositoryConfig repConfig = RepositoryConfigUtil.getRepositoryConfig(model,
					RepositoryConfigUtil.getRepositoryIDs(model).iterator().next());
			repMgr.addRepositoryConfig(repConfig);
		}

		@Override
		public Repository getRepository(String id) {
			return repMgr.getRepository(id);
		}

		@Override
		public void close() {
			if (shutdownOnClose) {
				repMgr.shutDown();
			}
		}

	}

	static class RemoteRepositoryManagerAdapter implements ModelBasedRepositoryManager, AutoCloseable {

		private SharedHttpClientSessionManager sessionManager;
		private String serverURL;
		private String username;
		private String password;

		public RemoteRepositoryManagerAdapter(String serverURL, String username, String password) {
			this.serverURL = serverURL;
			this.username = username;
			this.password = password;
			sessionManager = new SharedHttpClientSessionManager();
		}

		@Override
		public Model getRepositoryConfig(String id) {
			// based on the sources of RemoteRepositoryManager
			Model model = new LinkedHashModel();
			try (RDF4JProtocolSession protocolSession = sessionManager
					.createRDF4JProtocolSession(serverURL)) {
				protocolSession.setUsernameAndPassword(username, password);

				int serverProtocolVersion = Integer.parseInt(protocolSession.getServerProtocol());
				if (serverProtocolVersion < 10) { // explicit per-repo config endpoint was introduced in
													// Protocol version 10
					protocolSession
							.setRepository(Protocol.getRepositoryLocation(serverURL, SystemRepository.ID));
					protocolSession.getStatements(null, null, null, true, new StatementCollector(model));
				} else {
					protocolSession.setRepository(Protocol.getRepositoryLocation(serverURL, id));
					protocolSession.getRepositoryConfig(new StatementCollector(model));
				}

				return model;
			} catch (IOException | QueryEvaluationException | UnauthorizedException ue) {
				throw new RepositoryException(ue);
			}
		}

		@Override
		public void addRepositoryConfig(Model model) {
			RemoteRepositoryManager repMgr = new RemoteRepositoryManager(serverURL);
			repMgr.setUsernameAndPassword(username, password);
			repMgr.init();
			try {
				Model m = new LinkedHashModel();
				new ModelBasedRepositoryConfig(model).export(m,
						SimpleValueFactory.getInstance().createBNode());
				repMgr.addRepositoryConfig(new ModelBasedRepositoryConfig(model));
			} finally {
				repMgr.shutDown();
			}
		}

		@Override
		public Repository getRepository(String id) {
			HTTPRepository result = new HTTPRepository(serverURL, id);
			result.setHttpClientSessionManager(sessionManager);
			result.setUsernameAndPassword(username, password);
			result.init();
			return result;
		}

		@Override
		public void close() {
			sessionManager.shutDown();
		}

	}
}
