package it.uniroma2.art.semanticturkey.project;

import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.Nullable;

public class RepositoryLocation {
	private URL serverURL;

	public RepositoryLocation(@Nullable URL serverURL) {
		this.serverURL = serverURL;
	}

	public static RepositoryLocation fromRepositoryAccess(RepositoryAccess repositoryAccess) {
		URL serverURL = null;

		if (repositoryAccess.isRemote()) {
			serverURL = ((RemoteRepositoryAccess) repositoryAccess).getServerURL();
		}

		return new RepositoryLocation(serverURL);
	}

	@Override
	public String toString() {
		if (serverURL == null) {
			return "local";
		}

		return "remote:" + serverURL.toString();
	}

	public static RepositoryLocation fromString(String serialized) {
		if ("local".equals(serialized)) {
			return new RepositoryLocation(null);
		} else if (serialized.startsWith("remote:")) {
			try {
				return new RepositoryLocation(new URL(serialized.substring("remote:".length())));
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException(e);
			}
		} else {
			throw new IllegalArgumentException("Unknown repository location: " + serialized);
		}
	}

	public RepositoryAccess toRepositoryAccess() {
		if (serverURL == null) {
			return new CreateLocal();
		} else {
			return new CreateRemote(serverURL, null, null);
		}
	}

}
