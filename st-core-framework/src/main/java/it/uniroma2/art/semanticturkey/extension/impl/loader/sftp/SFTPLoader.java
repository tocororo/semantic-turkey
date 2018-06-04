package it.uniroma2.art.semanticturkey.extension.impl.loader.sftp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.security.PublicKey;
import java.util.AbstractMap.SimpleImmutableEntry;

import org.apache.commons.io.IOUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.subsystem.sftp.SftpClient;
import org.apache.sshd.client.subsystem.sftp.SftpClient.OpenMode;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.digest.Digest;

import it.uniroma2.art.semanticturkey.extension.extpts.loader.FormattedResourceTarget;
import it.uniroma2.art.semanticturkey.extension.extpts.loader.Loader;
import it.uniroma2.art.semanticturkey.extension.extpts.loader.StreamTargetingLoader;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ClosableFormattedResource;

/**
 * Implementation of the {@link Loader} extension point that uses the SFTP protocol. This implementation can
 * load data into a {@link FormattedResourceTarget}.
 * 
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SFTPLoader implements StreamTargetingLoader {

	private SFTPLoderConfiguration conf;

	public SFTPLoader(SFTPLoderConfiguration conf) {
		this.conf = conf;
	}

	@Override
	public void load(FormattedResourceTarget target) throws IOException {
		try (SshClient client = SshClient.setUpDefaultClient()) {
			client.start();

			try (ClientSession session = client.connect(conf.username, conf.host, conf.port)
					.verify(conf.timeout).getSession()) {
				session.setServerKeyVerifier(
						(ClientSession clientSession, SocketAddress remoteAddress, PublicKey serverKey) -> {
							try {
								SimpleImmutableEntry<Boolean, String> rv = KeyUtils.checkFingerPrint(
										conf.serverKeyFingerprint, (Digest) null, serverKey);
								return rv.getKey();
							} catch (Exception e) {
								return false;
							}
						});
				if (conf.password != null) {
					session.addPasswordIdentity(conf.password);
				}
				session.auth().verify(conf.timeout);

				SftpClient sftpClient = session.getSftpClientFactory().createSftpClient(session, null);

				try (BufferedInputStream is = new BufferedInputStream(
						sftpClient.read(conf.sourcePath, OpenMode.Read))) {
					File backingFile = File.createTempFile("loadRDF", null);
					try (FileOutputStream os = new FileOutputStream(backingFile)) {
						IOUtils.copy(is, os);
					}
					target.setTargetFormattedResource(
							new ClosableFormattedResource(backingFile, null, null, null));
				}
			}
		}
	}
}
