package it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

/**
 * This class represents the output of a {@link ReformattingExporter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ClosableFormattedResource implements Closeable {

	private File backingFile;
	private byte[] data;
	private String defaultFileExtension;
	private String mimeType;
	private Charset charset;
	private @Nullable String originalFilename;

	public ClosableFormattedResource(File backingFile, String defaultFileExtension, String mimeType,
			Charset charset, String originalFilename) {
		this.backingFile = backingFile;
		this.defaultFileExtension = defaultFileExtension;
		this.mimeType = mimeType;
		this.charset = charset;
		this.originalFilename = originalFilename;
	}

	public ClosableFormattedResource(byte[] data, String defaultFileExtension, String mimeType,
									 Charset charset, String originalFilename) {
		this.data = data;
		this.defaultFileExtension = defaultFileExtension;
		this.mimeType = mimeType;
		this.charset = charset;
		this.originalFilename = originalFilename;
	}
	
	public File getBackingFile() {
		return backingFile;
	}

	public String getDefaultFileExtension() {
		return defaultFileExtension;
	}

	public String getMIMEType() {
		return mimeType;
	}

	public Charset getCharset() {
		return charset;
	}

	public String getOriginalFilename() {
		return originalFilename;
	}

	public void writeTo(OutputStream outputStream) throws IOException {
		if (backingFile != null) {
			FileUtils.copyFile(backingFile, outputStream);
		} else {
			try(ByteArrayInputStream is = new ByteArrayInputStream(data)) {
				IOUtils.copy(is, outputStream);
			}
		}
	}

	public InputStream getInputStream() throws IOException {
		return backingFile != null ? new FileInputStream(backingFile) : new ByteArrayInputStream(data);
	}

	@Override
	public void close() {
		if (backingFile != null) {
			backingFile.delete();
		}
	}

	public static ClosableFormattedResource build(Consumer<OutputStream> outputStreamConsumer,
			String mimeType, String defaultFileExtension, Charset charset, String originalFilename)
			throws IOException {
		File backingFile = File.createTempFile("reformattingexporter", null);
		try (OutputStream os = new FileOutputStream(backingFile)) {
			outputStreamConsumer.accept(os);
		}
		return new ClosableFormattedResource(backingFile, defaultFileExtension, mimeType, charset,
				originalFilename);
	}

}
