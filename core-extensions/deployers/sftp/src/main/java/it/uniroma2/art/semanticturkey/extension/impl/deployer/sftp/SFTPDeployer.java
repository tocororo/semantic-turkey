package it.uniroma2.art.semanticturkey.extension.impl.deployer.sftp;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.security.PublicKey;
import java.util.AbstractMap.SimpleImmutableEntry;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.subsystem.sftp.SftpClient;
import org.apache.sshd.client.subsystem.sftp.SftpClient.OpenMode;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.digest.Digest;

import it.uniroma2.art.semanticturkey.extension.extpts.deployer.Deployer;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.FormattedResourceSource;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.StreamSourcedDeployer;

/**
 * Implementation of the {@link Deployer} extension point that uses t. This implementation can deploy data
 * provided by a {@link FormattedResourceSource}.
 * 
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class SFTPDeployer implements StreamSourcedDeployer {

	private SFTPDeployerConfiguration conf;

	public SFTPDeployer(SFTPDeployerConfiguration conf) {
		this.conf = conf;
	}

	@Override
	public void deploy(FormattedResourceSource source) throws IOException {
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

				try (BufferedOutputStream os = new BufferedOutputStream(
						sftpClient.write(conf.destinationPath, OpenMode.Write, OpenMode.Create))) {
					source.getSourceFormattedResource().writeTo(os);
				}
			}
		}
	}
}
