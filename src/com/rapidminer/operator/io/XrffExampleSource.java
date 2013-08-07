/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2013 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.operator.io;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.file.FileInputPortHandler;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.PortProvider;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;


/**
 * <p>This operator can read XRFF files known from Weka.
 * The XRFF (eXtensible attribute-Relation File Format) is an XML-based extension of the ARFF format
 * in some sense similar to the original RapidMiner file format for attribute description files (.aml).</p>
 * 
 * <p>Here you get a small example for the IRIS dataset represented as XRFF file:</p>
 *
 * <pre>
 * &lt;?xml version="1.0" encoding="utf-8"?&gt;
 * &lt;dataset name="iris" version="3.5.3"&gt;
 *  &lt;header&gt;
 *     &lt;attributes&gt;
 *        &lt;attribute name="sepallength" type="numeric"/&gt;
 *        &lt;attribute name="sepalwidth" type="numeric"/&gt;
 *        &lt;attribute name="petallength" type="numeric"/&gt;
 *        &lt;attribute name="petalwidth" type="numeric"/&gt;
 *        &lt;attribute class="yes" name="class" type="nominal"&gt;
 *           &lt;labels&gt;
 *              &lt;label&gt;Iris-setosa&lt;/label&gt;
 *              &lt;label&gt;Iris-versicolor&lt;/label&gt;
 *              &lt;label&gt;Iris-virginica&lt;/label&gt;
 *           &lt;/labels&gt;
 *        &lt;/attribute&gt;
 *     &lt;/attributes&gt;
 *  &lt;/header&gt;
 *
 *  &lt;body&gt;
 *     &lt;instances&gt;
 *        &lt;instance&gt;
 *           &lt;value&gt;5.1&lt;/value&gt;
 *           &lt;value&gt;3.5&lt;/value&gt;
 *           &lt;value&gt;1.4&lt;/value&gt;
 *           &lt;value&gt;0.2&lt;/value&gt;
 *           &lt;value&gt;Iris-setosa&lt;/value&gt;
 *        &lt;/instance&gt;
 *        &lt;instance&gt;
 *           &lt;value&gt;4.9&lt;/value&gt;
 *           &lt;value&gt;3&lt;/value&gt;
 *           &lt;value&gt;1.4&lt;/value&gt;
 *           &lt;value&gt;0.2&lt;/value&gt;
 *           &lt;value&gt;Iris-setosa&lt;/value&gt;
 *        &lt;/instance&gt;
 *        ...
 *     &lt;/instances&gt;
 *  &lt;/body&gt;
 * &lt;/dataset&gt;
 * </pre>
 * 
 * <p>Please note that the sparse XRFF format is currently not supported, please use one of the 
 * other options for sparse data files provided by RapidMiner.</p>
 *
 * <p>Since the XML representation takes up considerably more space since the data is wrapped
 * into XML tags, one can also compress the data via gzip. RapidMiner automatically recognizes a file 
 * being gzip compressed, if the file's extension is .xrff.gz instead of .xrff.</p>
 *
 * <p>Similar to the native RapidMiner data definition via .aml and almost arbitrary data files, the XRFF 
 * format contains some additional features. Via the class="yes" attribute in the attribute 
 * specification in the header, one can define which attribute should used as a prediction label 
 * attribute. Although the RapidMiner terminus for such classes is &quot;label&quot; instead of 
 * &quot;class&quot; we support the terminus class in order to not break compatibility with
 * original XRFF files.</p>
 *  
 * <p>Please note that loading attribute weights is currently not supported, please use
 * the other RapidMiner operators for attribute weight loading and writing for this
 * purpose.</p>
 *
 * <p>Instance weights can be defined via a weight XML attribute in each instance tag. 
 * By default, the weight is 1. Here's an example:</p>
 *
 * <pre>
 * &lt;instance weight="0.75"&gt;
 *  &lt;value&gt;5.1&lt;/value&gt;
 *  &lt;value&gt;3.5&lt;/value&gt;
 *  &lt;value&gt;1.4&lt;/value&gt;
 *  &lt;value&gt;0.2&lt;/value&gt;
 *  &lt;value&gt;Iris-setosa&lt;/value&gt;
 * &lt;/instance&gt;
 * </pre>
 * 
 * <p>Since the XRFF format does not support id attributes one have to use one of the RapidMiner
 * operators in order to change on of the columns to the id column if desired. This has to be done
 * after loading the data.</p>
 * 
 * @rapidminer.index xrff
 * @author Ingo Mierswa
 */
public class XrffExampleSource extends AbstractExampleSource {

	/** The parameter name for &quot;The path to the data file.&quot; */
	public static final String PARAMETER_DATA_FILE = "data_file";

	/** The parameter name for &quot;The (case sensitive) name of the id attribute&quot; */
	public static final String PARAMETER_ID_ATTRIBUTE = "id_attribute";

	/** The parameter name for &quot;Determines, how the data is represented internally.&quot; */
	public static final String PARAMETER_DATAMANAGEMENT = "datamanagement";

	/** The parameter name for &quot;Character that is used as decimal point.&quot; */
	public static final String PARAMETER_DECIMAL_POINT_CHARACTER = "decimal_point_character";

	/** The parameter name for &quot;The fraction of the data set which should be read (1 = all; only used if sample_size = -1)&quot; */
	public static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";

	/** The parameter name for &quot;The exact number of samples which should be read (-1 = use sample ratio; if not -1, sample_ratio will not have any effect)&quot; */
	public static final String PARAMETER_SAMPLE_SIZE = "sample_size";
	
	private InputPort fileInputPort = getInputPorts().createPort("file");
	private FileInputPortHandler filePortHandler = new FileInputPortHandler(this, fileInputPort, PARAMETER_DATA_FILE);

	public XrffExampleSource(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		String idName = getParameterAsString(PARAMETER_ID_ATTRIBUTE);

		Attribute label = null;
		Attribute id = null;
		Attribute weight = null;
		boolean instanceWeightsUsed = false;

		MemoryExampleTable table = null;
		try {
			Document document = null;
			try {
				document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(filePortHandler.openSelectedFile());
			} catch (SAXException e1) {
				throw new IOException(e1.getMessage());
			} catch (ParserConfigurationException e1) {
				throw new IOException(e1.getMessage());
			}

			Element datasetElement = document.getDocumentElement();
			if (!datasetElement.getTagName().equals("dataset")) {
				throw new IOException("Outer tag of XRFF file must be <dataset>.");
			}

			// read attribute meta data
			Element headerElement = retrieveSingleNode(datasetElement, "header");
			Element attributesElement = retrieveSingleNode(headerElement, "attributes");

			List<Attribute> attributeList = new LinkedList<Attribute>();
			NodeList attributes = attributesElement.getChildNodes();
			for (int i = 0; i < attributes.getLength(); i++) {
				Node node = attributes.item(i);
				if (node instanceof Element) {
					Element attribute= (Element)node;
					String tagName = attribute.getTagName();
					if (!tagName.equals("attribute"))
						throw new IOException("Only tags <attribute> are allowed inside <attributes>, was " + tagName);

					String name =  attribute.getAttribute("name");
					if (name == null)
						throw new IOException("The tag <attribute> needs a 'name' attribute.");
					String classAttribute =  attribute.getAttribute("class");
					boolean isClass = classAttribute != null && classAttribute.equals("yes");
					String valueType = attribute.getAttribute("type");
					if (valueType == null)
						throw new IOException("The tag <attribute> needs a 'type' attribute.");

					Attribute att = createAttribute(name, valueType);
					if (att.isNominal()) {
						Element labelsElement = retrieveSingleNode(attribute, "labels", false);
						if (labelsElement != null) {
							NodeList labels = labelsElement.getChildNodes();
							for (int j = 0; j < labels.getLength(); j++) {
								Node labelNode = labels.item(j);
								if (labelNode instanceof Element) {
									String labelTagName = labelNode.getNodeName();
									if (!labelTagName.equals("label"))
										throw new IOException("Only tags <label> are allowed inside <labels>, was " + labelTagName);

									String labelValue = labelNode.getTextContent();
									att.getMapping().mapString(labelValue);
								}
							}   
						}
					}

					if (isClass)
						label = att;

					if ((idName != null) && (name.equals(idName)))
						id = att;

					attributeList.add(att);
				}
			}

			// create weight attribute for instance weights 
			// remove this later on if no instance weights were defined
			weight = AttributeFactory.createAttribute("weight", Ontology.REAL);
			attributeList.add(weight);

			// read data
			table = new MemoryExampleTable(attributeList);
			DataRowFactory factory = new DataRowFactory(getParameterAsInt(PARAMETER_DATAMANAGEMENT), getParameterAsString(PARAMETER_DECIMAL_POINT_CHARACTER).charAt(0));
			Attribute[] attributeArray = new Attribute[attributeList.size()];
			attributeList.toArray(attributeArray);
			Element bodyElement = retrieveSingleNode(datasetElement, "body");
			Element instancesElement = retrieveSingleNode(bodyElement, "instances");
			NodeList instances = instancesElement.getChildNodes();
			int maxRows = getParameterAsInt(PARAMETER_SAMPLE_SIZE);
			double sampleProb = getParameterAsDouble(PARAMETER_SAMPLE_RATIO);
			RandomGenerator random = RandomGenerator.getRandomGenerator(this);
			int counter = 0;

			for (int i = 0; i < instances.getLength(); i++) {
				Node node = instances.item(i);
				if (node instanceof Element) {
					Element instance = (Element)node;
					String tagName = instance.getTagName();
					if (!tagName.equals("instance"))
						throw new IOException("Only tags <instance> are allowed inside <instances>, was " + tagName);

					NodeList values = instance.getChildNodes();
					int elementCount = 0;
					for (int j = 0; j < values.getLength(); j++) {
						if (values.item(j) instanceof Element) {
							elementCount++;

						}
					}
					if (elementCount != attributeList.size() - 1) { // -1 because of the add. weight att
						throw new IOException("Number of values must be the same than the number of attributes.");
					}
					String[] valueArray = new String[attributeList.size()];
					int index = 0;
					for (int j = 0; j < values.getLength(); j++) {
						Node valueNode = values.item(j);
						if (valueNode instanceof Element) {
							Element valueElement = (Element)valueNode;
							String valueTagName = valueElement.getTagName();
							if (!valueTagName.equals("value"))
								throw new IOException("Only tags <value> are allowed inside <instance>, was " + valueTagName);

							valueArray[index++] = valueNode.getTextContent();
						}
					}

					String weightString = instance.getAttribute("weight");
					if ((weightString != null) && (weightString.length() > 0)) {
						valueArray[valueArray.length - 1] = weightString;
						instanceWeightsUsed = true;
					} else {
						valueArray[valueArray.length - 1] = "1.0";
					}

					if ((maxRows > -1) && (counter >= maxRows))
						break;

					counter++;

					if (maxRows == -1) {
						if (random.nextDouble() > sampleProb)
							continue;
					}

					table.addDataRow(factory.create(valueArray, attributeArray));
				}
			}   
		} catch (IOException e) {
			throw new UserError(this, 302, filePortHandler.getSelectedFileDescription(), e.getMessage());
		}

		ExampleSet result = table.createExampleSet(label, weight, id);
		if (!instanceWeightsUsed) {
			result.getAttributes().remove(weight);
			result.getExampleTable().removeAttribute(weight);
		}

		return result;
	}

	private Element retrieveSingleNode(Element element, String nodeName) throws IOException {
		return retrieveSingleNode(element, nodeName, true);
	}

	private Element retrieveSingleNode(Element element, String nodeName, boolean exceptionOnFail) throws IOException {
		NodeList headerElements = element.getElementsByTagName(nodeName);
		if (headerElements.getLength() == 0) {
			if (exceptionOnFail)
				throw new IOException("A dataset must define a <"+nodeName+"> section for attribute meta data description.");
			else
				return null;
		}
		if (headerElements.getLength() > 1) {
			if (exceptionOnFail)
				throw new IOException("A dataset must not define more than one <"+nodeName+"> section.");
			else
				return null;
		}

		return (Element)headerElements.item(0);

	}

	private Attribute createAttribute(String name, String type) {
		int valueType = Ontology.NOMINAL;
		if (type.toLowerCase().equals("numeric")) {
			valueType = Ontology.NUMERICAL;
		} else if (type.toLowerCase().equals("real")) {
			valueType = Ontology.REAL;
		} else if (type.toLowerCase().equals("integer")) {
			valueType = Ontology.INTEGER;
		} else if (type.toLowerCase().equals("string")) {
			valueType = Ontology.STRING;
		} else if (type.toLowerCase().equals("date")) {
			valueType = Ontology.DATE;
		}
		return AttributeFactory.createAttribute(name, valueType);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(FileInputPortHandler.makeFileParameterType(this, PARAMETER_DATA_FILE, "Name of the Xrff file to read the data from.", "xrff", new PortProvider() {
			@Override
			public Port getPort() {			
				return fileInputPort;
			}
		}));
		types.add(new ParameterTypeString(PARAMETER_ID_ATTRIBUTE, "The (case sensitive) name of the id attribute"));
		types.add(new ParameterTypeCategory(PARAMETER_DATAMANAGEMENT, "Determines, how the data is represented internally.", DataRowFactory.TYPE_NAMES, DataRowFactory.TYPE_DOUBLE_ARRAY));
		types.add(new ParameterTypeString(PARAMETER_DECIMAL_POINT_CHARACTER, "Character that is used as decimal point.", "."));
		ParameterType type = new ParameterTypeDouble(PARAMETER_SAMPLE_RATIO, "The fraction of the data set which should be read (1 = all; only used if sample_size = -1)", 0.0d, 1.0d, 1.0d);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeInt(PARAMETER_SAMPLE_SIZE, "The exact number of samples which should be read (-1 = use sample ratio; if not -1, sample_ratio will not have any effect)", -1, Integer.MAX_VALUE, -1));

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
}
