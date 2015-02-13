package it.uniroma2.art.semanticturkey.customrange;

import it.uniroma2.art.semanticturkey.resources.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

/**
 * This class is a provider of CustomRange. 
 * @author Tiziano Lorenzetti
 *
 */
public class CustomRangeProvider {
	
	private static final String CUSTOM_RANGE_CONFIG_FILENAME = "customRange.config";
	private static final String CUSTOM_RANGE_FOLDER_NAME = "customRange";
	private static final String CUSTOM_RANGE_ENTRY_FOLDER_NAME = "customRangeEntry";
	
	private Properties customRangeConfig; //contains the mapping between property and CustomRange
	/*
	 * The customRangeConfig should contain property-customRangeName pairs, for example
	 * http://www.w3.org/2008/05/skos-xl#prefLabel			it.uniroma2.art.semanticturkey.skosxlLabel
	 * http://www.w3.org/2008/05/skos-xl#altLabel			it.uniroma2.art.semanticturkey.skosxlLabel
	 * http://www.w3.org/2008/05/skos-xl#hiddenLabel		it.uniroma2.art.semanticturkey.skosxlLabel
	 * http://www.w3.org/2004/02/skos/core#note				it.uniroma2.art.semanticturkey.Note
	 * http://www.w3.org/2004/02/skos/core#definition		it.uniroma2.art.semanticturkey.Note
	 * http://www.w3.org/2004/02/skos/core#example			it.uniroma2.art.semanticturkey.Note
	 */
	
	/**
	 * Initialize the customRange.config map
	 * @param projectName needed to retrieve the folder of the CustomRange xml files into 
	 * SemanticTurkeyData folder
	 * @throws IOException 
	 */
	public CustomRangeProvider() {
		customRangeConfig = new Properties();
		try {
			File customRangeConfigFile = new File(getCustomRangeFolder() , CUSTOM_RANGE_CONFIG_FILENAME);
			FileReader fileReader = new FileReader(customRangeConfigFile);
			customRangeConfig.load(fileReader);
			fileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns all the CustomRange available into the custom range folder of the project
	 * (TODO: or it would be better if get the custom ranges mapped into the customRange.config file)
	 * @return
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 */
	public Collection<CustomRange> loadCustomRanges() throws FileNotFoundException {
		Collection<CustomRange> customRanges = new ArrayList<CustomRange>();
		File[] crFiles = getCustomRangeFolder().listFiles();//get list of files into custom range folder
		for (File f : crFiles){//search for the custom range files
			if (f.getName().endsWith(".xml")){
				customRanges.add(new CustomRange(f));
			}
		}
		return customRanges;
	}
	
	/**
	 * Given a property returns the CustomRange associated to that property. <code>null</code> if no
	 * custom range is specified for that property.
	 * TODO I don't like the null return.
	 * @param property
	 * @return
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 */
	public CustomRange loadCustomRange(String property) throws FileNotFoundException {
		String customRangeName = customRangeConfig.getProperty(property);
		if (customRangeName != null){ //custom range of the given property specified in customRange.config
			File crFolder = CustomRangeProvider.getCustomRangeFolder();
			File[] crFiles = crFolder.listFiles();//get list of files into custom range folder
			for (File f : crFiles){//search for the custom range file with the given name
				if (f.getName().equals(customRangeName+".xml")){
					return new CustomRange(f);
				}
			} //if the CustomRange file specified in customRange.config is not found. 
			throw new FileNotFoundException("Custom range file '" + customRangeName + ".xml' cannot be found in the CustomRange folder. "
					+ " Please, check if it exists in the CustomRange folder or fix the configuration file '" + CUSTOM_RANGE_CONFIG_FILENAME + "'");
		}
		return null;
	}
	
	/**
	 * Returns the CustomRange folder, located under SemanticTurkeyData/ folder
	 * @return
	 */
	public static File getCustomRangeFolder(){
		return new File(Config.getDataDir() + "/" + CUSTOM_RANGE_FOLDER_NAME);
	}
	
	/**
	 * Returns the CustoRangeEntry folder, located under SemanticTurkeyData/custoRange/ folder
	 * @return
	 */
	public static File getCustomRangeEntryFolder(){
		return new File(getCustomRangeFolder() + "/" + CUSTOM_RANGE_ENTRY_FOLDER_NAME);
	}

}
