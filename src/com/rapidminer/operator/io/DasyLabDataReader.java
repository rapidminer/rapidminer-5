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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;


/**
 * <p>This operator allows to import data from DasyLab files (.DDF) into RapidMiner.
 * Currently only universal format 1 is supported. External files (.DDB) and histogram
 * data are currently not supported.</p>
 * 
 * <p>The parameter <code>timestamp</code> allows to configure whether and what kind of
 * timestamp should be included in the example set. If it is set to <em>relative</em>,
 * the timestamp attribute captures the amount of milliseconds since the file start
 * time. If it is set to <em>absolute</em>, the absolute time is used to timestamp the
 * examples.</p>
 * 
 * @author Tobias Malbrecht
 */
public class DasyLabDataReader extends BytewiseExampleSource {

	public static final String PARAMETER_TIMESTAMP = "timestamp";
	
	public static final String[] PARAMETER_TIMESTAMP_OPTIONS = { "none", "relative", "absolute" };
	
	public static final int TIMESTAMP_NONE = 0;
	
	public static final int TIMESTAMP_RELATIVE = 1;
	
	public static final int TIMESTAMP_ABSOLUTE = 2;
	
	private static final String NOT_YET_IMPLEMENTED_ERROR_MESSAGE = "feature not yet implemented, ";
	
	private static final String FILE_HEADER_STRING = "DTDF";
	
	private static final byte STRING_TERMINATOR_BYTE = 0;
	
	private static final int FILE_TYPE_UNIVERSAL_FORMAT_1 = 1;
	
	private static class Channel {
//		private static final int CONTINUOUS_TIME_DEPENDENT_SIGNAL = 0;
//		
//		private static final int FREQUENCY_DEPENDENT_FULL_BLOCK_SIZE = 10;
//		
//		private static final int FREQUENCY_DEPENDENT_HALF_BLOCK_SIZE = 11;
		
		private static final int HISTOGRAM = 20;
		
		private static final int HISTOGRAM_WITH_TIME_INFORMATION = 21;
		
//		private int number;
//		
//		private int maximumBlockSize;
//		
//		private double sampleDelayTime;

		private int type;
		
//		private int flags;
//		
//		private String unit;
		
		private String name;
	}
	
	public DasyLabDataReader(OperatorDescription description) {
        super(description);
    }

	@Override
	protected String getFileSuffix() {
		return "ddf";
	}

	@Override
	protected ExampleSet readStream(InputStream inputStream, DataRowFactory dataRowFactory) throws IOException, UndefinedParameterError {
		int timestampMode = getParameterAsInt(PARAMETER_TIMESTAMP);
		
		byte[] buffer = new byte[500];
		int readBytes = -1;
		
		// header "DTDF",0x0D
		read(inputStream, buffer, 5);
		if (!extractString(buffer, 0, 4).equals(FILE_HEADER_STRING)) {
			throw new IOException(GENERIC_ERROR_MESSAGE);
		}

		// data file description string
		StringBuffer stringBuffer = new StringBuffer();
		for ( ; ; ) {
    		byte readByte = (byte) (0x000000FF & inputStream.read());
    		if (readByte == -1) {
    			throw new IOException(GENERIC_ERROR_MESSAGE);
    		}
    		if (readByte == STRING_TERMINATOR_BYTE) {
    			break;
    		}
    		stringBuffer.append((char) readByte);
    	}

//		// parse file description string
//		String[] descriptionStrings = stringBuffer.toString().split(new String(LINE_FEED_SEQUENCE));
//		for (int i = 0; i < descriptionStrings.length; i++) {
//			if (descriptionStrings[i].contains("=")) {
//				String[] keyValuePair = descriptionStrings[i].split("=");
//			}
//		}
		
		read(inputStream, buffer, 3);
		if (!extractString(buffer, 1, 2).equals("IN")) {
			throw new IOException(GENERIC_ERROR_MESSAGE);
		}
		
		// header size
		read(inputStream, buffer, 2);
		
		// file type
		read(inputStream, buffer, 2);
		int fileType = extract2ByteInt(buffer, 0, true);
		
		// version number
		read(inputStream, buffer, 2);

		// size of second global header
		read(inputStream, buffer, 2);
		
		// size of channel header
		read(inputStream, buffer, 2);

		// size of block header
		read(inputStream, buffer, 2);
		
		// data is stored in a separate file
		read(inputStream, buffer, 2);
		boolean separateFile = extract2ByteInt(buffer, 0, true)	== 1;
		if (separateFile) {
			throw new IOException(NOT_YET_IMPLEMENTED_ERROR_MESSAGE + "separate files not allowed");
		}
		
		// number of channels
		read(inputStream, buffer, 2);
		
		// time delay between samples in seconds
		read(inputStream, buffer, 8);
		
		// datum
		read(inputStream, buffer, 4);
		
		if (fileType != FILE_TYPE_UNIVERSAL_FORMAT_1) {
			throw new IOException(NOT_YET_IMPLEMENTED_ERROR_MESSAGE + "file types other than universal format 1 not supported");
		}

		// size of header
		read(inputStream, buffer, 2);
			
		// number of channels
		read(inputStream, buffer, 2);
		int numberOfChannels = extract2ByteInt(buffer, 0, true);
			
		// multiplexed?
		read(inputStream, buffer, 2);
			
		// number of channels collected on each input channel
		read(inputStream, buffer, 32);

		Channel[] channels = new Channel[numberOfChannels];
		for (int i = 0; i < numberOfChannels; i++) {
			Channel channel = new Channel();
				
			// size of channel header (2-byte int)
			read(inputStream, buffer, 2);
				
			// channel number (2-byte int)
			read(inputStream, buffer, 2);
				
			// maximum block size (2-byte int)
			read(inputStream, buffer, 2);
				
			// time delay between samples (double)
			read(inputStream, buffer, 8);
				
			// channel type
			read(inputStream, buffer, 2);
			channel.type = extract2ByteInt(buffer, 0, true);
			if (channel.type == Channel.HISTOGRAM ||
				channel.type == Channel.HISTOGRAM_WITH_TIME_INFORMATION) {
				throw new IOException(NOT_YET_IMPLEMENTED_ERROR_MESSAGE + "histogram data not supported");
			}
				
			// channel flags (2-byte int)
			read(inputStream, buffer, 2);
				
			// unused
			read(inputStream, buffer, 16);
				
			// channel unit
			readBytes = read(inputStream, buffer, (char) 0);
//			if (readBytes != -1) {
//				channel.unit = extractString(buffer, 0, readBytes);
//			}
				
			// channel name
			readBytes = read(inputStream, buffer, (char) 0);
			if (readBytes != -1) {
				channel.name = extractString(buffer, 0, readBytes);
			}
				
			channels[i] = channel;
		}
			
		read(inputStream, buffer, 4);
		if (!extractString(buffer, 0, 4).equals("DATA")) {
			throw new IOException(GENERIC_ERROR_MESSAGE);
		}
			
		ArrayList<Attribute> attributes = new ArrayList<Attribute>(numberOfChannels);
		switch (timestampMode) {
		case TIMESTAMP_NONE:
			break;
		case TIMESTAMP_RELATIVE:
			attributes.add(AttributeFactory.createAttribute("timestamp", Ontology.REAL));
			break;
		case TIMESTAMP_ABSOLUTE:
			attributes.add(AttributeFactory.createAttribute("timestamp", Ontology.DATE_TIME));
			break;
		}
		for (int i = 0; i < numberOfChannels; i++) {
			Attribute attribute = AttributeFactory.createAttribute(channels[i].name, Ontology.REAL);
			attributes.add(attribute);
		}
		
		// number of bytes in channel
		read(inputStream, buffer, 2);
		
		// start datum
		read(inputStream, buffer, 4);
		double startTime = (long) extractInt(buffer, 0, true) * 1000;
		
		// unused
		read(inputStream, buffer, 8);

		MemoryExampleTable table = new MemoryExampleTable(attributes);
		
		HashMap<Double, Double[]> valuesMap = new HashMap<Double, Double[]>();
		HashMap<Double, Integer> counterMap = new HashMap<Double, Integer>();
		
		boolean eof = false;
		while (!eof) {
			readBytes = inputStream.read(buffer, 0, 20);
			if (readBytes != 20) {
				eof = true;
				break;
			}
			
			int channelNr = extract2ByteInt(buffer, 0, true);
			double time = extractDouble(buffer, 2, true);
			double delay = extractDouble(buffer, 10, true);
			int blockSize = extract2ByteInt(buffer, 18, true);
			
			for (int i = 0; i < blockSize; i++) {
				readBytes = inputStream.read(buffer, 20, 4);
				if (readBytes != 4) {
					eof = true;
					break;
				}
				double value = extractFloat(buffer, 20, true);

				Double[] values = null; 
				if (!valuesMap.containsKey(time)) {
					counterMap.put(time, 1);
					values = new Double[timestampMode == TIMESTAMP_NONE ? numberOfChannels : numberOfChannels + 1];
					for (int j = 1; j < values.length; j++) {
						values[j] = Double.NaN;
					}
					valuesMap.put(time, values);
				} else {
					Integer counter = counterMap.get(time) + 1;
					counterMap.put(time, counter);
					values = valuesMap.get(time);
				}
				
				if (values != null) {
					switch (timestampMode) {
					case TIMESTAMP_NONE:
						values[channelNr] = value;
						break;
					case TIMESTAMP_RELATIVE:
						values[0] = (double) (long) (time * 1000);
						values[channelNr + 1] = value;
						break;
					case TIMESTAMP_ABSOLUTE:
						values[0] = (double) ((long) startTime + (long) (time * 1000));
						values[channelNr + 1] = value;
						break;
					}
				}
				
				if (counterMap.get(time) == numberOfChannels) {
					table.addDataRow(dataRowFactory.create(valuesMap.get(time), attributes.toArray(new Attribute[attributes.size()])));
					counterMap.remove(time);
					valuesMap.remove(time);
				}
				
				time += delay;
			}
		}
		
		inputStream.close();
		ExampleSet exampleSet = table.createExampleSet();
		if (timestampMode != TIMESTAMP_NONE) {
			exampleSet.getAttributes().setId(attributes.get(0));
		}
		return exampleSet;
	}
	
	   @Override
	public List<ParameterType> getParameterTypes() {
	        List<ParameterType> types = super.getParameterTypes();
	        types.add(new ParameterTypeCategory(PARAMETER_TIMESTAMP, "Specifies whether to include an absolute timestamp, a timestamp relative to the beginning of the file (in seconds) or no timestamp at all.", PARAMETER_TIMESTAMP_OPTIONS, 2));
	        return types;
	   }
}
