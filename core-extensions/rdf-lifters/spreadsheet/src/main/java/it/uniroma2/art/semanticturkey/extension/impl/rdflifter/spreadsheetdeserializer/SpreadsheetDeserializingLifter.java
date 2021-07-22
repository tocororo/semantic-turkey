package it.uniroma2.art.semanticturkey.extension.impl.rdflifter.spreadsheetdeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.rio.RDFHandler;

import it.uniroma2.art.semanticturkey.extension.extpts.rdflifter.LifterContext;
import it.uniroma2.art.semanticturkey.extension.extpts.rdflifter.LiftingException;
import it.uniroma2.art.semanticturkey.extension.extpts.rdflifter.RDFLifter;
import it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter.ClosableFormattedResource;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

/**
 * An {@link RDFLifter} that deserializes RDF data from a spreadsheet created by the provided exporter in ST
 * 
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
public class SpreadsheetDeserializingLifter implements RDFLifter {

	private final Map<String, String> prefixToNamespaceMap = new HashMap<>();

	// used to keep track che property name for a given column number (and if such property should be used in a
	// reified construct, meaning tha the subject of such property is not the main element of the row but the element
	// of the previous cell)
	private final Map<Integer, PropertyName> posToPropertyNameMap = new HashMap<>();

	@Override
	public void lift(ClosableFormattedResource sourceFormattedResource, String format,
			RDFHandler targetRDFHandler, LifterContext lifterContext) throws LiftingException, IOException {

		try {
			//this model will contain all the triples extracted from the spreadsheet
			Model model = new LinkedHashModel();

			Workbook workbook = WorkbookFactory.create(sourceFormattedResource.getInputStream());
			//read the second sheet, containing the prefix-mapping
			Sheet sheet2 = workbook.getSheetAt(1);
			readPrefixed(sheet2);

			// now focus on the first sheet
			Sheet sheet = workbook.getSheetAt(0);

			// read the excel file and process the Concepts, Schemes and Collections part, use the separator to
			// identify the different parts:
			// - concept hierarchy / scheme URI / collection hierarchy
			// - lexicalizations
			// - types
			// - reified notes (this part can be empty)
			// - generic properties

			//start with the headers
			// all these "start_*" values represent the colPos where the "::" are, so a pos before the actual stating
			// of the part
			int start_col_lexicalization=0, start_col_type=0, start_col_reifiedNote=0, start_pos_genProp=0, afterLastCol;
			int posCol = 0;
			Row row = sheet.getRow(0);
			while(true){
				Cell cell = row.getCell(++posCol);
				//String cellTextx = row.getCell(++posCol).getStringCellValue();
				if( (cell == null || cell.getStringCellValue().isEmpty()) && start_pos_genProp!=0){
					afterLastCol = posCol;
					break;
				}
				if(cell == null) {
					continue;
				}
				String cellValue = cell.getStringCellValue().trim();
				if (cellValue.equals("::")) {
					//found a separator
					if (start_col_lexicalization==0) {
						start_col_lexicalization = posCol;
					} else if (start_col_type==0) {
						start_col_type = posCol;
					} else if (start_col_reifiedNote==0) {
						start_col_reifiedNote = posCol;
					} else { // start_pos_genProp==0
						start_pos_genProp = posCol;
					}
				} else if (start_col_lexicalization!=0){
					// the concept hierarchy has been passed, so now the headers are about the properties names
					// (and reified used properties)
					if (start_pos_genProp!=0) {
						//we are in the generic property part, so no reified values
						PropertyName propertyName = new PropertyName(cellValue, false);
						posToPropertyNameMap.put(posCol, propertyName);
					} else if (start_col_reifiedNote!=0){
						// we are in the reified note, so the rdf:value is a reified for the previous column
						IRI prop = toIRI(cellValue);
						PropertyName propertyName;
						if(prop.equals(RDF.VALUE)){
							propertyName = new PropertyName(cellValue, true);
						} else {
							propertyName = new PropertyName(cellValue, false);
						}
						posToPropertyNameMap.put(posCol, propertyName);
					} else if (start_col_type!=0){
						// we are in the type part, no reified properties
						PropertyName propertyName = new PropertyName(cellValue, false);
						posToPropertyNameMap.put(posCol, propertyName);
					} else {
						//we are in the lexicalizations, if a skosxl:literalForm is found, then it is reified
						IRI prop = toIRI(cellValue);
						PropertyName propertyName;
						if(prop.equals(SKOSXL.LITERAL_FORM)){
							propertyName = new PropertyName(cellValue, true);
						} else {
							propertyName = new PropertyName(cellValue, false);
						}
						posToPropertyNameMap.put(posCol, propertyName);
					}
				}
			}

			int posRow = 0;
			//Concepts part (CONCEPTS HIERARCHY)
			posRow = processConceptSection(sheet, posRow, start_col_lexicalization, start_col_type, start_col_reifiedNote,
					start_pos_genProp, afterLastCol, model);

			//Schemes part (CONCEPT SCHEMES)
			posRow = processSchemeSection(sheet, ++posRow, start_col_lexicalization, start_col_type, start_col_reifiedNote,
					start_pos_genProp, afterLastCol, model);

			//Collection part (COLLECTIONS HIERARCHY)
			processCollectionSection(sheet, ++posRow, start_col_lexicalization, start_col_type, start_col_reifiedNote,
					start_pos_genProp, afterLastCol, model);


			for (Statement stmt : model) {
				targetRDFHandler.handleStatement(stmt);
			}

		} catch (InvalidFormatException e) {
			throw new LiftingException(e);
		}
	}


	private int processConceptSection(Sheet sheet, int posRow, int start_col_lexicalization, int start_col_type,
									  int start_col_reifiedNote, int start_pos_genProp, int afterLastCol, Model model) {
		Row row;
		//iterate over each row and the first not empty/null cell found is the URI (or qname) of the concept
		while(true){
			//iterate over each row
			String concept = null;
			boolean foundData = false;
			//get the concept IRI/qname
			row = sheet.getRow(++posRow);

			//a analyze each each cell of this row (by using the divisions of the various parts)

			// search for the concept IRI/qname
			if(row == null){
				// it is an empty row, so this section is terminated
				break;
			}
			for(int posCol=0; posCol<start_col_lexicalization; ++posCol) {
				Cell cell = row.getCell(posCol);
				if (cell != null && !cell.getStringCellValue().isEmpty()) {
					// the first not empty cell was found, this is the concept IRI/qname
					concept = cell.getStringCellValue().trim();
					foundData = true;
					break;
				}
			}
			if(!foundData){
				// no data was found at this row, so it means that the concept part was all processed,
				// so stop processing this part
				break;
			}


			// process the concepts lexicalization
			processLexicalizationCol(row, start_col_lexicalization, start_col_type, (Resource) createValue(concept),
					model);

			// process the concept type(s)
			processTypeCol(row, start_col_type, start_col_reifiedNote, (Resource) createValue(concept), model);

			// process the concept reified note, if any
			processReiNoteCol(row, start_col_reifiedNote, start_pos_genProp, (Resource) createValue(concept), model);

			// process the concept generic properties
			processGenPropCol(row, start_pos_genProp, afterLastCol, (Resource) createValue(concept), model);
		}
		return posRow;
	}

	private int processSchemeSection(Sheet sheet, int posRow, int start_col_lexicalization, int start_col_type,
									 int start_col_reifiedNote, int start_pos_genProp, int afterLastCol, Model model) {
		Row row;
		//iterate over each row and the first not empty/null cell found is the URI (or qname) of the concept
		while(true){
			//iterate over each row
			String scheme = null;
			boolean foundData = false;
			//get the concept IRI/qname
			row = sheet.getRow(++posRow);

			//a analyze each each cell of this row (by using the divisions of the various parts)

			// search for the scheme IRI/qname , in this section the scheme uri can only be in the first cell of the row
			if(row == null){
				// it is an empty row, so this section is terminated
				break;
			}
			Cell cell = row.getCell(0);
			if(cell != null && !cell.getStringCellValue().isEmpty()){
				// the first not empty cell was found, this is the concept IRI/qname
				scheme = cell.getStringCellValue().trim();
				foundData = true;
			}
			if(!foundData){
				// no data was found at this row, so it means that the concept part was all processed,
				// so stop processing this part
				break;
			}


			// process the concepts lexicalization
			processLexicalizationCol(row, start_col_lexicalization, start_col_type, (Resource) createValue(scheme),
					model);

			// process the concept type(s)
			processTypeCol(row, start_col_type, start_col_reifiedNote, (Resource) createValue(scheme), model);

			// process the concept reified note, if any
			processReiNoteCol(row, start_col_reifiedNote, start_pos_genProp, (Resource) createValue(scheme), model);

			// process the concept generic properties
			processGenPropCol(row, start_pos_genProp, afterLastCol, (Resource) createValue(scheme), model);
		}
		return posRow;
	}

	private int processCollectionSection(Sheet sheet, int posRow, int start_col_lexicalization, int start_col_type,
										 int start_col_reifiedNote, int start_pos_genProp, int afterLastCol, Model model){
		Row row;
		//iterate over each row and the first not empty/null cell found is the URI (or qname) of the collection
		while(true){
			//iterate over each row
			String collection = null;
			boolean foundData = false;
			//get the concept IRI/qname
			row = sheet.getRow(++posRow);

			//a analyze each each cell of this row (by using the divisions of the various parts)

			// search for the collection IRI/qname
			if(row == null){
				// it is an empty row, so this section is terminated
				break;
			}
			int posCol; // it is external of the for since it is used also later
			for(posCol=0; posCol<start_col_lexicalization; ++posCol) {
				Cell cell = row.getCell(posCol);
				if (cell != null && !cell.getStringCellValue().isEmpty()) {
					// the first not empty cell was found, this is the concept IRI/qname
					collection = cell.getStringCellValue().trim();
					foundData = true;
					break;
				}
			}
			if(!foundData){
				// no data was found at this row, so it means that the concept part was all processed,
				// so stop processing this part
				break;
			}

			// process the concepts lexicalization
			processLexicalizationCol(row, start_col_lexicalization, start_col_type, (Resource) createValue(collection),
					model);

			// process the concept type(s)
			processTypeCol(row, start_col_type, start_col_reifiedNote, (Resource) createValue(collection), model);

			// process the concept reified note, if any
			processReiNoteCol(row, start_col_reifiedNote, start_pos_genProp, (Resource) createValue(collection), model);

			// process the concept generic properties
			processGenPropCol(row, start_pos_genProp, afterLastCol, (Resource) createValue(collection), model);

			// see if this element has a value for the property skos:memberList, in this case it means that is has
			// a list in it, so create a list (using such value, which normally is a bnode) and place is such RDF List
			// all the other element directly under it in the hierarchy
			Resource listRes = getValueOfMemberList(row, afterLastCol);
			if(listRes!=null) {
				List<Resource> memberOfOrdCollList = new ArrayList<>();
				int posRowMember = posRow;
				int posColMember = posCol+1;
				// a list is present, so take all the element under it
				while(true){
					Row rowMember = sheet.getRow(++posRowMember);
					if(rowMember==null){
						// no more row to consider
						break;
					}
					Cell cellSameCol = rowMember.getCell(posCol);
					if(cellSameCol!=null && !cellSameCol.getStringCellValue().isEmpty()){
						// this row contains a resource that is at the same level of the ordered collection
						break;
					}
					Cell cellMember = rowMember.getCell(posColMember);
					if(cellMember==null || cellMember.getStringCellValue().isEmpty()) {
						// this row does not contain a member of this ordered collection
						break;
					}
					// the cellMember contains a resource that belong to the ordered collection
					memberOfOrdCollList.add((Resource) createValue(cellMember.getStringCellValue()));
				}
				// now create the RDF list containing all the resources
				model.add((Resource) createValue(collection), SKOS.MEMBER_LIST, listRes);

				Resource prevList = listRes;
				Resource currentList = prevList;
				for(Resource first : memberOfOrdCollList){
					if(currentList == null){
						currentList = SimpleValueFactory.getInstance().createBNode();
						model.add(prevList,RDF.REST, currentList);
					}
					model.add(currentList, RDF.TYPE, RDF.LIST);
					model.add(currentList, RDF.FIRST, first);
					//update the currentList and prefList
					prevList = currentList;
					currentList = null;
				}
				//add the RDF.NULL
				model.add(prevList, RDF.REST, RDF.NIL);
			}
		}
		return posRow;
	}

	/********************************************************/
	private void processLexicalizationCol(Row row, int start_col_lexicalization, int start_col_type,
										  Resource resource, Model model) {
		processPossibleReifiedCol(row, start_col_lexicalization, start_col_type, resource, model, SKOSXL.LABEL);
	}

	private void processTypeCol(Row row, int start_col_type, int start_col_reifiedNote, Resource resource, Model model){
		processNotReifiedCol(row, start_col_type, start_col_reifiedNote, resource, model);
	}

	private void processReiNoteCol(Row row, int start_col_reifiedNote, int start_pos_genProp, Resource resource, Model model){
		processPossibleReifiedCol(row, start_col_reifiedNote, start_pos_genProp, resource, model, null);
	}

	private void processGenPropCol(Row row, int start_pos_genProp, int afterLastCol, Resource resource, Model model){
		processNotReifiedCol(row, start_pos_genProp, afterLastCol, resource, model);
	}

	private void processNotReifiedCol(Row row, int start, int end, Resource resource, Model model){
		for(int i=start+1; i<end; ++i){
			Cell cell = row.getCell(i);
			if(cell != null && !cell.getStringCellValue().isEmpty()){
				PropertyName propertyName = posToPropertyNameMap.get(i);
				model.add(resource, propertyName.getPropertyIri(),
						createValue(cell.getStringCellValue()));
			}
		}
	}

	private void processPossibleReifiedCol(Row row, int start, int end, Resource resource, Model model,
										   IRI classForReified) {
		for(int i=start+1; i<end; ++i){
			Cell cell = row.getCell(i);
			if(cell != null && !cell.getStringCellValue().isEmpty()){
				PropertyName propertyName = posToPropertyNameMap.get(i);
				if(propertyName.isFromReified) {
					// it is a reified lexicalization, so the subject of the triple is not the concept but the
					// value of the previous cell
					Cell cellPast = row.getCell(i-1);

					model.add((Resource) createValue(cellPast.getStringCellValue()), propertyName.getPropertyIri(),
							createValue(cell.getStringCellValue()));
					if(classForReified!=null){
						model.add((Resource) createValue(cellPast.getStringCellValue()), RDF.TYPE, classForReified);
					}
				} else {
					model.add(resource, propertyName.getPropertyIri(),
							createValue(cell.getStringCellValue()));
				}
			}
		}
	}

	private Resource getValueOfMemberList(Row row, int afterLastCol) {
		//get the position of the skos:memberList
		for(int i=0; i<afterLastCol; ++i){
			PropertyName propertyName = posToPropertyNameMap.get(i);
			if(propertyName==null){
				continue;
			}
			if(propertyName.getPropertyIri().equals(SKOS.MEMBER_LIST)) {
				Cell cell = row.getCell(i);
				if(cell != null && !cell.getStringCellValue().isEmpty()) {
					return (Resource) createValue(cell.getStringCellValue());
				}
			}
		}
		// no Resource was found
		return null;
	}

	/********************************************************/

	private void readPrefixed(Sheet sheet) {
		int posRow = 1;
		while(true){
			Row row = sheet.getRow(posRow++);
			if(row == null || row.getCell(1) == null || row.getCell(1).getStringCellValue().isEmpty()){
				break;
			}
			String prefix="", namespace;
			namespace = row.getCell(1).getStringCellValue();
			if(!namespace.endsWith("#") && !namespace.endsWith("/")){
				namespace += "#";
			}
			if(row.getCell(0) != null && !row.getCell(0).getStringCellValue().isEmpty()) {
				prefix = row.getCell(0).getStringCellValue();
			}
			prefixToNamespaceMap.put(prefix, namespace);
		}
	}

	private IRI toIRI(String iriOrQName) {
		// this property can have at the end the @LANG_TAG, so it should be removed
		if(iriOrQName.contains("@")) {
			iriOrQName = iriOrQName.substring(0, iriOrQName.lastIndexOf("@"));
		}
		return createIRI(iriOrQName);
	}

	private IRI createIRI(String qnameOrIri){
		// if the qnameOrIri starts with < then it is already an IRI
		if(qnameOrIri.startsWith("<")) {
			String iriString = qnameOrIri.substring(1, qnameOrIri.lastIndexOf(">"));
			return SimpleValueFactory.getInstance().createIRI(iriString);
		}
		//the qnameOrIri is a quname, so extract the prefix and local name
		String prefix = qnameOrIri.split(":")[0].trim();
		String localname = qnameOrIri.split(":")[1].trim();
		String namespace = prefixToNamespaceMap.get(prefix);
		return SimpleValueFactory.getInstance().createIRI(namespace+localname);
	}

	private Value createValue(String value) {
		// manage the qname case
		if(isQName(value)){
			return createIRI(value);
		}
		// be careful of the case of literal with a datatype being a qname
		if(value.startsWith("\"")){
			String datatype = value.substring(value.lastIndexOf("^")+1); // TODO test it
			if(isQName(datatype)){
				value = value.substring(0, value.lastIndexOf("^")) +
						NTriplesUtil.toNTriplesString(createIRI(datatype));
			}
		}
		return NTriplesUtil.parseValue(value, SimpleValueFactory.getInstance());
	}

	private boolean isQName(String qname){
		if(!qname.startsWith("<") && !qname.startsWith("\"") && !qname.startsWith("_:") &&
				qname.contains(":")) {
			return true;
		}
		return false;

	}



	public class PropertyName {
		private IRI propertyIri;
		boolean isFromReified;

		public PropertyName(String propertyQNameOrIRI, boolean isFromReified) {
			// this property can have at the end the @LANG_TAG, so it should be removed
			if(propertyQNameOrIRI.contains("@")) {
				propertyQNameOrIRI = propertyQNameOrIRI.substring(0, propertyQNameOrIRI.lastIndexOf("@"));
			}
			this.propertyIri = createIRI(propertyQNameOrIRI);
			this.isFromReified = isFromReified;
		}

		public IRI getPropertyIri() {
			return propertyIri;
		}

		public boolean isFromReified() {
			return isFromReified;
		}
	}
}
