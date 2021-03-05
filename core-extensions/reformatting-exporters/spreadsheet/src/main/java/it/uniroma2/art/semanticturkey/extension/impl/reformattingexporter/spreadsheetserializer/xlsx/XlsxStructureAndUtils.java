package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.xlsx;

import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures.CollectionInfo;
import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures.ConceptInfo;
import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures.ConceptSchemeInfo;
import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures.HeaderProp;
import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures.HeaderWhole;
import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures.PropInfoAndValues;
import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures.ReifiedValue;
import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures.ResourceInfo;
import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures.SimpleValue;
import it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures.ValueForProp;
import it.uniroma2.art.semanticturkey.utilities.ModelUtilities;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class XlsxStructureAndUtils {

	private Map<String, String> prefixDeclaraionMap;
	private String prefLang = null;


	public File createExcelFile(Map<String, String> prefixDeclaraionMap, List<IRI> topConceptList, Map<IRI, ConceptInfo> conceptIriToConceptInfoMap,
			Map<IRI, ConceptSchemeInfo> schemeIriToConceptSchemeInfoMap, List<IRI> topCollectionList, Map<IRI, CollectionInfo>collectionIriToConceptInfoMap,
			int maxDepth, HeaderWhole headerWhole, boolean isSkosxlLex, List<IRI> prefPropList, List<IRI> altPropList, List<IRI> hiddenPropList,
			List<IRI> notePropList, boolean reifiedNote, String prefLang) throws IOException {

		this.prefixDeclaraionMap = prefixDeclaraionMap;
		this.prefLang = prefLang==null || prefLang.isEmpty() ? "en" : prefLang;

		File tempServerFile = File.createTempFile("spreadsheet", ".xlsx");

		// Create a Workbook
		Workbook workbook = new XSSFWorkbook(); // new HSSFWorkbook() for generating `.xls` file
		// Create a Sheet
		Sheet sheet = workbook.createSheet("Sheet 1");



		int posRow = 0;
		//create the concept part
		//create the headers
		createHeaderPart("CONCEPTS HIERARCHY", posRow, workbook, sheet, maxDepth, headerWhole, isSkosxlLex, reifiedNote, prefPropList, altPropList, hiddenPropList, notePropList);
		posRow = createConceptPartInExcel(sheet, posRow+1,topConceptList, conceptIriToConceptInfoMap, maxDepth, headerWhole, prefPropList, altPropList, hiddenPropList,
				isSkosxlLex, reifiedNote, notePropList);

		// create the scheme part
		//create the headers
		createHeaderPart("CONCEPT SCHEMES", posRow+1, workbook, sheet, maxDepth, headerWhole, isSkosxlLex, reifiedNote, prefPropList, altPropList, hiddenPropList, notePropList);
		posRow = createSchemePartInExcel(sheet, posRow+2, schemeIriToConceptSchemeInfoMap, maxDepth, headerWhole, prefPropList, altPropList, hiddenPropList,
				isSkosxlLex, reifiedNote, notePropList);

		// create the collection part
		//create the headers
		createHeaderPart("COLLECTIONS HIERARCHY", posRow+1, workbook, sheet, maxDepth, headerWhole, isSkosxlLex, reifiedNote, prefPropList, altPropList, hiddenPropList, notePropList);
		posRow = createCollectionPartInExcel(sheet, posRow+2, topCollectionList, collectionIriToConceptInfoMap, maxDepth, headerWhole, prefPropList, altPropList, hiddenPropList,
				isSkosxlLex, reifiedNote, notePropList);

		// Resize all columns to fit the content size
		/*for (int i = 0; i < 6; i++) {
			sheet.autoSizeColumn(i);
		}*/

		// Write the output to a file
		FileOutputStream fileOut = new FileOutputStream(tempServerFile);
		workbook.write(fileOut);
		fileOut.close();

		// Closing the workbook
		//workbook.close();


		return tempServerFile;
	}

	private String toQName(IRI iri){
		String qnameOrIri = ModelUtilities.getQName(iri, prefixDeclaraionMap);
		return  qnameOrIri.equals(iri.stringValue()) ? "<"+iri.stringValue()+">" : qnameOrIri;
	}

	/** HEADERS PART **/

	private void createHeaderPart(String text, int rowPos, Workbook workbook, Sheet sheet, int maxDepth, HeaderWhole headerWhole, boolean isSkosxlLex,
			boolean reifiedNote, List<IRI> prefPropList, List<IRI> altPropList, List<IRI> hiddenPropList, List<IRI> notePropList){
		// Create a Font for styling header cells
		Font headerFont = workbook.createFont();
		//headerFont.setBold(true);
		headerFont.setFontHeightInPoints((short) 14);

		// Create a CellStyle with the font
		CellStyle headerCellStyle = workbook.createCellStyle();
		headerCellStyle.setFont(headerFont);

		// Create  Header Row
		Row headerRow = sheet.createRow(rowPos);

		Cell cell = headerRow.createCell(0);
		cell.setCellValue(text);
		cell.setCellStyle(headerCellStyle);

		//create the headers related to the properties
		int posCol = prefLang.isEmpty() ? maxDepth+1 : maxDepth+2;

		//first of all start with lexicalizations (pref, alt and hidden)
		posCol = prepareHeaderForPossibleReifiedPropValue(prefPropList, headerWhole, isSkosxlLex, headerRow, headerCellStyle, posCol, toQName(SKOSXL.LITERAL_FORM));
		posCol = prepareHeaderForPossibleReifiedPropValue(altPropList, headerWhole, isSkosxlLex, headerRow, headerCellStyle, posCol, toQName(SKOSXL.LITERAL_FORM));
		posCol = prepareHeaderForPossibleReifiedPropValue(hiddenPropList, headerWhole, isSkosxlLex, headerRow, headerCellStyle, posCol, toQName(SKOSXL.LITERAL_FORM));

		//add the rdf:type
		posCol = prepareHeaderForProp(RDF.TYPE, headerWhole, headerRow, headerCellStyle, posCol);

		//add the note (if reified, otherwise the note are created with the other properties)
		if(reifiedNote){
			posCol = prepareHeaderForPossibleReifiedPropValue(notePropList, headerWhole, true, headerRow, headerCellStyle, posCol, toQName(RDF.VALUE));
		}

		//add all other properties
		List<IRI> propToSkip = new ArrayList<>();
		propToSkip.addAll(prefPropList);
		propToSkip.addAll(altPropList);
		propToSkip.addAll(hiddenPropList);
		propToSkip.add(RDF.TYPE);
		for(String prop : headerWhole.getSimplePropNameList()){
			if(skipProp(prop, propToSkip)){
				continue;
				//skip this property
			}
			posCol = prepareHeaderForProp(prop, headerWhole, headerRow, headerCellStyle, posCol);
		}
	}

	private int prepareHeaderForPossibleReifiedPropValue(List<IRI> propList, HeaderWhole headerWhole, boolean isReified, Row headerRow,
			CellStyle headerCellStyle, int posCol, String propForReified){
		Cell cell;
		for(IRI prop : propList){
			HeaderProp headerProp = isReified ? headerWhole.getHeaderReifiedPropFromPropName(toQName(prop)) :
					headerWhole.getHeaderSimplePropFromPropName(toQName(prop));
			if(headerProp == null){
				//this property is not used, so just skip it
				continue;
			}
			for(String lang : headerWhole.getOrderedLangList()){
				String langForHeader = lang.equals(PropInfoAndValues.NO_LANG_TAG) ? "" : "@"+lang;
				//prepare the header(s) for this language
				for(int i=0; i<headerProp.getNumForLang(lang); ++i){
					if (!isReified) {
						//prepare a single header
						cell = headerRow.createCell(posCol++);
						cell.setCellValue(headerProp.getPropName() + langForHeader);
						cell.setCellStyle(headerCellStyle);
					} else {
						//prepare two headers
						cell = headerRow.createCell(posCol++);
						cell.setCellValue(headerProp.getPropName()+langForHeader);
						cell.setCellStyle(headerCellStyle);
						cell = headerRow.createCell(posCol++);
						cell.setCellValue(propForReified);
						cell.setCellStyle(headerCellStyle);
					}
				}
			}

		}
		return posCol;
	}

	private int prepareHeaderForProp(IRI propIri, HeaderWhole headerWhole, Row headerRow,
			CellStyle headerCellStyle, int posCol){
		return prepareHeaderForProp(toQName(propIri), headerWhole, headerRow, headerCellStyle, posCol);
	}

	private int prepareHeaderForProp(String prop, HeaderWhole headerWhole, Row headerRow,
			CellStyle headerCellStyle, int posCol){
		Cell cell;
		HeaderProp headerProp = headerWhole.getHeaderSimplePropFromPropName(prop);
		if(headerProp == null){
			//this property is not used, so just skip it
			return posCol;
		}
		for(String lang : headerWhole.getOrderedLangList()){
			String langForHeader = lang.equals(PropInfoAndValues.NO_LANG_TAG) ? "" : "@"+lang;
			//prepare the header(s) for this language
			for(int i=0; i<headerProp.getNumForLang(lang); ++i){
				//prepare a single header
				cell = headerRow.createCell(posCol++);
				cell.setCellValue(headerProp.getPropName()+langForHeader);
				cell.setCellStyle(headerCellStyle);
			}
		}
		return posCol;
	}

	private boolean skipProp(String prop, List<IRI> propToSkipList){
		for (IRI propToSkip : propToSkipList){
			if (prop.equals(toQName(propToSkip))) {
				return true;
			}
		}
		return false;
	}

	/** RESOURCES PART **/

	/** CONCEPTS PART **/

	private int createConceptPartInExcel(Sheet sheet, int posRow, List<IRI> topConcept, Map<IRI, ConceptInfo> conceptIriToConceptInfoMap, int maxDepth,
			HeaderWhole headerWhole, List<IRI> prefPropList, List<IRI> altPropList, List<IRI> hiddenPropList, boolean isSkosxlLex,
			boolean reifiedNote, List<IRI> notePropList) {
		//iterate over the top element
		for(IRI conceptIri : topConcept){
			ConceptInfo conceptInfo = conceptIriToConceptInfoMap.get(conceptIri);
			posRow = createConceptHierarchyAndInfoInExcel(sheet, posRow, 0, conceptInfo, maxDepth, headerWhole, prefPropList, altPropList,
					hiddenPropList, isSkosxlLex, reifiedNote, notePropList);
		}
		return posRow;
	}

	private int createConceptHierarchyAndInfoInExcel(Sheet sheet, int posRow, int postCol, ConceptInfo conceptInfo, int maxDepth, HeaderWhole headerWhole,
			List<IRI> prefPropList, List<IRI> altPropList, List<IRI> hiddenPropList, boolean isSkosxlLex, boolean reifiedNote, List<IRI> notePropList) {
		Row row = sheet.createRow(posRow++);

		addResourceIriAndSinglePrefLabel(conceptInfo, row, postCol++, prefPropList);

		addSeparator(row, maxDepth);

		//create all the info associated to the excel
		createResourceInfo(conceptInfo, headerWhole, maxDepth, prefPropList, altPropList, hiddenPropList, isSkosxlLex, row, reifiedNote, notePropList);

		for(ConceptInfo narrowerConcept : conceptInfo.getNarrowerList()){
			posRow = createConceptHierarchyAndInfoInExcel(sheet, posRow, postCol, narrowerConcept, maxDepth, headerWhole, prefPropList, altPropList,
					hiddenPropList, isSkosxlLex, reifiedNote, notePropList);
		}
		return posRow;
	}


	/** SCHEMES PART **/

	private int createSchemePartInExcel(Sheet sheet, int posRow, Map<IRI, ConceptSchemeInfo> schemeIriToConceptSchemeInfoMap, int maxDepth,
			HeaderWhole headerWhole, List<IRI> prefPropList, List<IRI> altPropList, List<IRI> hiddenPropList, boolean isSkosxlLex,
			boolean reifiedNote, List<IRI> notePropList) {
		//iterate over the top element
		for(IRI schemeIri : schemeIriToConceptSchemeInfoMap.keySet()){
			ConceptSchemeInfo conceptSchemeInfo = schemeIriToConceptSchemeInfoMap.get(schemeIri);

			Row row = sheet.createRow(posRow++);
			addResourceIriAndSinglePrefLabel(conceptSchemeInfo, row, 0, prefPropList);

			addSeparator(row, maxDepth);

			createResourceInfo(conceptSchemeInfo, headerWhole, maxDepth, prefPropList, altPropList, hiddenPropList, isSkosxlLex, row, reifiedNote, notePropList);
		}
		return posRow;
	}


	/** COLLECTIONS PART **/

	private int createCollectionPartInExcel(Sheet sheet, int posRow, List<IRI> topCollection, Map<IRI, CollectionInfo> collectionIriToCollectionInfoMap, int maxDepth,
			HeaderWhole headerWhole, List<IRI> prefPropList, List<IRI> altPropList, List<IRI> hiddenPropList, boolean isSkosxlLex,
			boolean reifiedNote, List<IRI> notePropList) {
		//iterate over the top element
		for(IRI conceptIri : topCollection){
			CollectionInfo collectionInfo = collectionIriToCollectionInfoMap.get(conceptIri);
			posRow = createCollectionHierarchyAndInfoInExcel(sheet, posRow, 0, collectionInfo, maxDepth, headerWhole, prefPropList, altPropList,
					hiddenPropList, isSkosxlLex, reifiedNote, notePropList);
		}
		return posRow;
	}

	private int createCollectionHierarchyAndInfoInExcel(Sheet sheet, int posRow, int postCol, ResourceInfo resourceInfo, int maxDepth, HeaderWhole headerWhole,
			List<IRI> prefPropList, List<IRI> altPropList, List<IRI> hiddenPropList, boolean isSkosxlLex, boolean reifiedNote, List<IRI> notePropList) {
		Row row = sheet.createRow(posRow++);
		addResourceIriAndSinglePrefLabel(resourceInfo, row, postCol++, prefPropList);

		//create all the info associated to the excel
		createResourceInfo(resourceInfo, headerWhole, maxDepth, prefPropList, altPropList, hiddenPropList, isSkosxlLex, row, reifiedNote, notePropList);

		addSeparator(row, maxDepth);

		if(resourceInfo instanceof CollectionInfo){
			for(ResourceInfo memberCollection : ((CollectionInfo) resourceInfo).getMemeberList()){
				posRow = createCollectionHierarchyAndInfoInExcel(sheet, posRow, postCol, memberCollection, maxDepth, headerWhole, prefPropList, altPropList,
						hiddenPropList, isSkosxlLex, reifiedNote, notePropList);
			}
		}

		return posRow;
	}


	/** GENERIC PART **/

	private void addResourceIriAndSinglePrefLabel(ResourceInfo resourceInfo, Row row, int pos, List<IRI> prefPropList){
		String conceptString = toQName(resourceInfo.getResourceIRI());
		row.createCell(pos).setCellValue(conceptString);
		if(!prefLang.isEmpty()){
			//the input prefLang is not null/empty so try to add a value, add the first value from the list of prefLabelList

			for(IRI prefProp : prefPropList){
				PropInfoAndValues propInfoAndValues = resourceInfo.getPropInfoAndValues(toQName(prefProp));
				if(propInfoAndValues==null){
					continue;
				}
				List<ValueForProp> valueForPropList = propInfoAndValues.getValueForPropListFromLang(prefLang);
				if(valueForPropList == null){
					continue;
				}
				String valueToAdd = null;
				for(ValueForProp valueForProp : valueForPropList){
					if(valueForProp instanceof SimpleValue) {
						valueToAdd = ((SimpleValue) valueForProp).getValue();
						break;
					} else {//(valueForProp instanceof ReifiedValue)
						valueToAdd = ((ReifiedValue)valueForProp).getLiteralValue();
						break;
					}
				}
				if(valueToAdd!=null){
					row.createCell(pos+1).setCellValue(valueToAdd);
				}
			}
		}
	}

	private void addSeparator(Row row, int maxDepth){
		if(prefLang.isEmpty()) {
			row.createCell(maxDepth).setCellValue(":");
		} else {
			row.createCell(maxDepth+1).setCellValue(":");
		}
	}

	private void createResourceInfo(ResourceInfo resourceInfo, HeaderWhole headerWhole, int maxDepth, List<IRI> prefPropList, List<IRI> altPropList,
			List<IRI> hiddenPropList, boolean isSkosxlLex, Row row, boolean reifiedNote, List<IRI> notePropList){
		int posCol = prefLang.isEmpty() ? maxDepth+1 : maxDepth+2;

		//first of all start with lexicalizations (pref, alt and hidden)
		posCol = prepareCellsForPossibleReifiedPropValue(resourceInfo, prefPropList, headerWhole, isSkosxlLex, row, posCol);
		posCol = prepareCellsForPossibleReifiedPropValue(resourceInfo, altPropList, headerWhole, isSkosxlLex, row, posCol);
		posCol = prepareCellsForPossibleReifiedPropValue(resourceInfo, hiddenPropList, headerWhole, isSkosxlLex, row, posCol);

		//add the rdf:type(s)
		posCol = prepareCellsForProp(resourceInfo, RDF.TYPE, headerWhole, row, posCol);

		//add the note (if reified, otherwise the note are created with the other general properties)
		if(reifiedNote){
			posCol = prepareCellsForPossibleReifiedPropValue(resourceInfo, notePropList, headerWhole, true, row, posCol);
		}

		//add all other properties
		List<IRI> propToSkip = new ArrayList<>();
		propToSkip.addAll(prefPropList);
		propToSkip.addAll(altPropList);
		propToSkip.addAll(hiddenPropList);
		propToSkip.add(RDF.TYPE);
		for(String prop : headerWhole.getSimplePropNameList()){
			if(skipProp(prop, propToSkip)){
				continue;
				//skip this property
			}
			posCol = prepareCellsForProp(resourceInfo, prop, headerWhole, row, posCol);
		}
	}


	private int prepareCellsForPossibleReifiedPropValue(ResourceInfo resourceInfo, List<IRI> propList, HeaderWhole headerWhole,
			boolean isReified, Row row, int posCol) {
		Cell cell;
		int mult = isReified ? 2 : 1 ; //2 cells are needed for the reified properties
		for(IRI prop : propList){
			HeaderProp headerProp = isReified ? headerWhole.getHeaderReifiedPropFromPropName(toQName(prop)) :
					headerWhole.getHeaderSimplePropFromPropName(toQName(prop));
			if(headerProp == null){
				//this property is not used, so no cell is dedicated to this property, just skip it
				continue;
			}
			PropInfoAndValues propInfoAndValues = resourceInfo.getPropInfoAndValues(toQName(prop));
			if(propInfoAndValues==null){
				//this property is not used for this ConceptInfo, so just skip it, but remember to increment the value "used" cell
				for(String lang : headerWhole.getOrderedLangList()){
					posCol += headerProp.getNumForLang(lang) * mult;
				}
				continue;
			}
			//this property is used at least once for this ConceptInfo, so fill the desired cell (and skip those cells not having
			// a value for the property_lang)

			for(String lang : headerWhole.getOrderedLangList()){
				int addedCellNum=0;
				List<ValueForProp> valueForPropList = propInfoAndValues.getValueForPropListFromLang(lang);
				if(valueForPropList == null){
					//there are no value for this prop_lang, so just increment the posCol
					posCol += headerProp.getNumForLang(lang) * mult;
					continue;
				}
				//there is at least one value for this prop_lang so create the selected cell(s)
				for (ValueForProp valueForProp : valueForPropList) {
					if (!isReified) {
						// single cell
						cell = row.createCell(posCol++);
						cell.setCellValue(((SimpleValue) valueForProp).getValue());
						++addedCellNum;
					} else {
						if (valueForProp instanceof SimpleValue) {
							//skip this value since it is a SimpleValue stored in a property which is now considered as reified
							continue;
						}
						// two cells
						cell = row.createCell(posCol++);
						cell.setCellValue(((ReifiedValue) valueForProp).getIriValue());
						cell = row.createCell(posCol++);
						cell.setCellValue(((ReifiedValue) valueForProp).getLiteralValue());
						addedCellNum += 2;
					}
				}
				// all values for the prop_lang have been addded, but the posCol may have to be further increment, according to the header
				posCol += (headerProp.getNumForLang(lang)*mult - addedCellNum);
			}
		}
		return posCol;
	}

	private int prepareCellsForProp(ResourceInfo resourceInfo, IRI prop, HeaderWhole headerWhole, Row row,  int posCol){
		return prepareCellsForProp(resourceInfo, toQName(prop), headerWhole, row, posCol);
	}

	private int prepareCellsForProp(ResourceInfo resourceInfo, String prop, HeaderWhole headerWhole, Row row, int posCol){
		Cell cell;
		HeaderProp headerProp = headerWhole.getHeaderSimplePropFromPropName(prop);
		if(headerProp == null){
			//this property is not used, so no cell is dedicated to this property, just skip it
			return posCol;
		}
		PropInfoAndValues propInfoAndValues = resourceInfo.getPropInfoAndValues(prop);
		if(propInfoAndValues==null){
			//this property is not used for this ConceptInfo, so just skip it, but remember to increment the value "used" cell
			for(String lang : headerWhole.getOrderedLangList()){
				posCol += headerProp.getNumForLang(lang);
			}
			return posCol;
		}
		//this property is used at least once for this ConceptInfo, so fill the desired cell (and skip those cells not having
		// a value for the property_lang)
		for(String lang : headerWhole.getOrderedLangList()){
			List<ValueForProp> valueForPropList = propInfoAndValues.getValueForPropListFromLang(lang);
			if(valueForPropList == null){
				//there are no value for this prop_lang, so just increment the posCol
				posCol += headerProp.getNumForLang(lang);
				continue;
			}
			//there is at least one value for this prop_lang so create the selected cell(s)
			int addedValueNum = 0;
			for(int i=0; i<valueForPropList.size(); ++i){
				ValueForProp valueForProp = valueForPropList.get(i);
				if(valueForProp instanceof SimpleValue) {
					// single cell
					cell = row.createCell(posCol++);
					cell.setCellValue(((SimpleValue) valueForProp).getValue());
					++addedValueNum;
				}
			}
			// all values for the prop_lang have been added, but the posCol may have to be further increment, according to the header
			posCol += (headerProp.getNumForLang(lang) - addedValueNum);
		}
		return posCol;
	}
}
