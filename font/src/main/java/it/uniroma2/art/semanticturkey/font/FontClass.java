package it.uniroma2.art.semanticturkey.font;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FontClass {
	private List<String> fontFileNameList = new ArrayList<>();
	private final String PACKACE_PATH = "it/uniroma2/art/semanticturkey/font/";

	public FontClass() {
		fontFileNameList.add("arial-unicode-ms.ttf");
	}

	public List<String> getFontFileNameList() {
		return fontFileNameList;
	}

	public InputStream getFontFile(String fontFileName) throws IOException {
		String packageAndFile = PACKACE_PATH+fontFileName;
		InputStream inputStream = (new ClassPathResource(packageAndFile, FontClass.class.getClassLoader())).getInputStream();
		return inputStream;
	}
}
