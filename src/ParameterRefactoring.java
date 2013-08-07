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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 
 *  This class is used for refactoring RapidMiners operator classes 
 *  in order to ensure, that every parameter reference uses a certain
 *  predefined String variable. 
 * 
 * 	@author Helge Homburg, Tobias Beckers
 */
public class ParameterRefactoring {

    private BufferedReader srcFile;
	
	private BufferedWriter dstFile;
	
	private BufferedWriter logFile;
	
	private boolean isProperty = false;
    
    private boolean containsTODOs = false;
	
	private String file = "";  
	
	private String workingDir, logDir;
	
	private StringBuffer newEntries = new StringBuffer("");
	
	private List<File> files = new LinkedList<File>();
	
	private int currentPosition = 0, nextIndex = 0, numberParameter = 0, numberProperty = 0;
	private int parameterDeclaration = 0, propertyDeclaration = 0; 
	
	/** By adding a parameter or property name to this blacklist it becomes transparent to the 
	  * refactoring methods.*/
	List<String> blackList = Arrays.asList("user.dir", "rapidminer.home", "os.name", "noWordSep",
			"keep_example_set", "min_similarity", "java.class.path", "user.home", "line.separator");	
	
	private static class JAVAFilter implements FilenameFilter {
	    public boolean accept(File dir, String name) {
	        return (name.endsWith(".java"));
	    }
	}
	
	public ParameterRefactoring(String workingDir, String logDir) {		
		this.workingDir = workingDir;
		this.logDir = logDir;
	}	
	
	public void transformFiles() {		
		
		// get all files in workingDir
		getAllFiles(new File(workingDir));
		
		// get current timestamp
		Date dt = new Date();
		
		logFile = openOutputWriter(new File(logDir, "refactor-"+ dt.getTime() +".log"));
		writeToLog("Refactoring started "+ dt +", ");		
		writeToLog("changes were made to the following classes:\n" );
		
		// modify all files in workingDir 
		for (int i = 0; i < files.size(); i++) {
						
			file = "";
			currentPosition = 0; 
			nextIndex = 0;
			containsTODOs = false;
            newEntries = new StringBuffer("");
			
			File currentFile = files.get(i);
			String[] fileName = currentFile.getName().split("\\.");
			String className = fileName[0];
			
			srcFile = openSourceFile(currentFile);
						
			if (srcFile != null) {
				fillBuffer();
			} else {
				System.out.println("Target file not found!");
				break;
			}
			closeSourceFile();
			
			writeToLog("\n--> "+className+" class at \""+currentFile.toString()+"\"\n\n");
			
			// perform substitution for every suitable ParameterType declaration
			int currentIndex = getNextParameterTokenIndex();		
			
			while (currentIndex != -1) {
				replaceParameterToken(currentIndex);
				currentIndex = getNextParameterTokenIndex();				
			}
			
			// write statements
			setDeclarations(className);
			
			checkForNonStandardVariables();
			findOtherStatements();
				
            if (!newEntries.toString().equals("") || containsTODOs) {                
            	String path = currentFile.getParent();
                if (path != null) { 
                    dstFile = openOutputWriter(new File(path, className+".java"));
                } else {
                    break;
                }                
                writeOutput();
            }
		}
		writeToLog("\nSome strings remained unchanged:\n\n\t[parameter]: "+numberParameter+"\t[property]: "+numberProperty);
		writeToLog("\n\t[parameter declaration]: "+parameterDeclaration+"\t[property declaration]: "+propertyDeclaration);
		try {
			logFile.flush();
			logFile.close();
		} catch (IOException e) {			
			e.printStackTrace();
		}
	}
	
	private void replaceParameterToken(int pos) {
		
		String key = "[parameter]";
		
		// get the parameter name		
		int startOfName = file.indexOf('"', pos);		
		int endOfName = file.indexOf('"', startOfName + 1);		
		String name = file.substring(startOfName, endOfName + 1);
		
		// get the parameter description
		int startOfDescription = file.indexOf('"', endOfName + 1);
		int endOfDescription = file.indexOf('"', startOfDescription +1);
		boolean isEndOfDescription = false;
		while (!isEndOfDescription) {
			String indicator = file.substring(endOfDescription - 1, endOfDescription + 1);			
			if (indicator.equals("\\\"")) {								
				endOfDescription = file.indexOf('"', endOfDescription +1);
			} else {
				isEndOfDescription = true;
			}
		}
		String descriptionName = file.substring(startOfDescription, endOfDescription + 1);		
		
		if (!this.isProperty) {
			// construct new parameter declaration
			String comment = "\n\t/** The parameter name for &quot;"+descriptionName.substring(1, descriptionName.length() - 1)+"&quot; */";
			newEntries.append(comment);
			String newName = "PARAMETER_"+file.substring(startOfName + 1, endOfName).toUpperCase().trim();
			String fullStatement = "\n\tpublic static final String "+newName+" = "+name+";\n";
			newEntries.append(fullStatement);		
			
			// replace the old string by the new parameter statement			
			Pattern nameReference = Pattern.compile("([Ligset]*Parameter[Set]*[^\\(]*\\(\\s*)"+name);
			Matcher findNameReference = nameReference.matcher(file);	
			file = findNameReference.replaceAll("$1"+newName);
			
			Pattern parameterTypeDeclaration = Pattern.compile("(new\\s+ParameterType[^\\(]*\\(\\s*)"+name);
			Matcher findDeclaration = parameterTypeDeclaration.matcher(file);
			file = findDeclaration.replaceAll("$1"+newName);
		
		} else {
			key = "[property]";			
			
			// construct new parameter declaration
			String comment = "\n\t/** The property name for &quot;"+descriptionName.substring(1, descriptionName.length() - 1)+"&quot; */";
			newEntries.append(comment);			
			String newName = "PROPERTY_"+name.substring(1, name.length() - 1).toUpperCase().replace('.', '_').trim();			
			String fullStatement = "\n\tpublic static final String "+newName+" = "+name+";\n";
			newEntries.append(fullStatement);			
			
			Pattern nameReference = Pattern.compile("([gs]+etProperty[^\\(]*\\(\\s*)"+name);
			Matcher findNameReference = nameReference.matcher(file);		
			file = findNameReference.replaceAll("$1"+newName);		
			
			Pattern parameterTypeDeclaration = Pattern.compile("(Property\\s*\\(\\s*new\\s+ParameterType[^\\(]*\\(\\s*)"+name);
			Matcher findDeclaration = parameterTypeDeclaration.matcher(file);
			file = findDeclaration.replaceAll("$1"+newName);
			
			
		}
		
		// look for further occurrences of the old string		
		int currentIndex = findOccurranceOf(name, 0);
		while (currentIndex >= 0) {			
			writeToLog("\t"+key+" "+name+"\t occurs at position "+currentIndex+" (line "+getLineNumber(currentIndex)+")\n");
			currentIndex = findOccurranceOf(name, currentIndex + 1);			
		}
	}
	
	private int getNextParameterTokenIndex() {
		
		// find next the occurance of a ParameterType declaration and move 
		// the pointer to the starting position of the parameter name string		
		int nextParameterIndex = -1, nextPropertyIndex = -1;
		boolean isRegular = false;
		
		try {
			Pattern parameterTypeDeclaration = Pattern.compile("(new\\s+ParameterType[^\\(]*\\(\\s*)([A-Z\"])");
			Matcher findParameterDeclaration = parameterTypeDeclaration.matcher(file);						
			findParameterDeclaration.find(currentPosition);		
			if (!findParameterDeclaration.group(2).equals("\"")) {
				isRegular = true;				
			}
			nextParameterIndex = findParameterDeclaration.end(1) - 1;			
		} catch (RuntimeException e) {
			nextParameterIndex = -2;				
		}	
		
		try {			
			Pattern propertyTypeDeclaration = Pattern.compile("Property\\s*\\(\\s*(new\\s+ParameterType[^\\(]*\\(\\s*)([A-Z\"])");
			Matcher findPropertyDeclaration = propertyTypeDeclaration.matcher(file);			
			findPropertyDeclaration.find(currentPosition);			
			nextPropertyIndex = findPropertyDeclaration.end(1) - 1;
			if (!findPropertyDeclaration.group(2).equals("\"")) {
				isRegular = true;
				nextIndex = nextPropertyIndex;				
			}
		} catch (RuntimeException e) {
			nextPropertyIndex = -3;						
		}
		
		if (nextParameterIndex == -2 && nextPropertyIndex == -3) {
			return -1;
		}
		
		if (nextParameterIndex == nextPropertyIndex) {
			this.isProperty = true;			
		} else {
			this.isProperty = false;
		}
		
		nextIndex = nextParameterIndex;
		
		// find the ending bracket of the current declaration in order to
		// ignore any inner declaration of ParameterType
		Character currentChar = Character.valueOf('c');
		int currentIndex = nextIndex + 1, numberOfOpenBrackets = 1, numberOfClosedBrackets = 0;
		boolean notAllBracketsClosed = true;
		
		while (notAllBracketsClosed) {
			
			currentChar = file.charAt(currentIndex);
			
			// skip a comment
			if (currentChar.equals('"')) {
				currentIndex++;
				currentChar = file.charAt(currentIndex);
				while (!currentChar.equals('"')) {
					currentIndex++;
					currentChar = file.charAt(currentIndex);
				}				
			}			
			
			// count all brackets
			if (currentChar.equals('(')) {
				numberOfOpenBrackets += 1;
			}			
			if (currentChar.equals(')')) {
				numberOfClosedBrackets += 1;
			}
			
			if (numberOfClosedBrackets == numberOfOpenBrackets) {
				notAllBracketsClosed = false;
			}
			
			currentIndex++;
		}	
		
		this.currentPosition = currentIndex;

		// if the current declaration already uses a constant for reference, go ahead 
		// to the next possible occurrance of a parameter type declaration.
		if (isRegular) {
			return getNextParameterTokenIndex();
		}
		return nextIndex;
	}	
	
	private void checkForNonStandardVariables() {
		
		boolean parameterMatch = true, propertyMatch = true;
        int nextParameterIndex = 0;  int nextPropertyIndex = 0;        
        
		while (parameterMatch || propertyMatch) {
			try {                
				Pattern parameterTypeDeclaration = Pattern.compile("[^y]\\s*\\(\\s*new\\s+ParameterType[^\\(]*\\(\\s*([^P\\s,]\\w*\\.?[^P\\s,.]\\w*\\s*),");
				Matcher findParameterDeclaration = parameterTypeDeclaration.matcher(file);				
				findParameterDeclaration.find(nextParameterIndex);		
				nextParameterIndex = findParameterDeclaration.start(1);				
				String parameterStatement = file.substring(nextParameterIndex, findParameterDeclaration.end(1));
				writeToLog("\t"+"[parameter declaration]"+" "+parameterStatement+"\t occurs at position "+nextParameterIndex+" (line "+getLineNumber(nextParameterIndex)+")\n");
                int todoPosition = getPositionOfNextLineFeed(nextParameterIndex);
                String start = file.substring(0, todoPosition);
                String end = file.substring(todoPosition);
                file = start + " // TODO [parameter declaration]" + end;
				parameterDeclaration++;
                containsTODOs = true;
			} catch (RuntimeException e) {				
				parameterMatch = false;
			}	
			
			try {               
				Pattern propertyTypeDeclaration = Pattern.compile("Property\\s*\\(\\s*new\\s+ParameterType[^\\(]*\\(\\s*([^P\\s,]\\w*\\.?[^P\\s,.]\\w*\\s*),");
				Matcher findPropertyDeclaration = propertyTypeDeclaration.matcher(file);			
				findPropertyDeclaration.find(nextPropertyIndex);						
				nextPropertyIndex = findPropertyDeclaration.start(1);
				String propertyStatement = file.substring(nextPropertyIndex, findPropertyDeclaration.end(1));
				writeToLog("\t"+"[property declaration]"+" "+propertyStatement+"\t occurs at position "+nextPropertyIndex+" (line "+getLineNumber(nextPropertyIndex)+")\n");
                int todoPosition = getPositionOfNextLineFeed(nextPropertyIndex);
                String start = file.substring(0, todoPosition);
                String end = file.substring(todoPosition);
                file = start + " // TODO [property declaration]" + end;
				propertyDeclaration++;
                containsTODOs = true;
			} catch (RuntimeException e) {
				propertyMatch = false;
			}
		}
	}
	
	private void findOtherStatements() {
		// look for further statements using strings for reference instead of string variables
		boolean parameterMatch = true, propertyMatch = true;		
        int parameterIndex = 0; int propertyIndex = 0;
        
		while(parameterMatch || propertyMatch) {
			
			try {                
				Pattern reference = Pattern.compile("([Ligset]*)Parameter[\\w]*\\(\\s*\"([^\\)\"]*)\"\\s*\\)");
				Matcher findReference = reference.matcher(file);
				findReference.find(parameterIndex);
				parameterIndex = findReference.start(1);
				String parameter = file.substring(findReference.start(2), findReference.end(2));
				if (blackList.contains(parameter)) {
					parameterIndex = findReference.end(2);
				} else {
					writeToLog("\t"+"[parameter]"+" \""+parameter+"\"\t occurs at position "+parameterIndex+" (line "+getLineNumber(parameterIndex)+")\n");
					parameterIndex = findReference.end(2);
	                int todoPosition = getPositionOfNextLineFeed(parameterIndex);
	                String start = file.substring(0, todoPosition);
	                String end = file.substring(todoPosition);
	                file = start + " // TODO [parameter]" + end;				
					numberParameter++;
	                containsTODOs = true;
	            }
			} catch (RuntimeException e) {
				parameterMatch = false;		
			}
				
			try {                
				Pattern reference = Pattern.compile("([gs])+etProperty[\\w]*\\(\\s*\"([^\\)\"]*)\"\\s*\\)");
				Matcher findReference = reference.matcher(file);
				findReference.find(propertyIndex);
				propertyIndex = findReference.start(1);
				String property = file.substring(findReference.start(2), findReference.end(2));
				if (blackList.contains(property)) {
					propertyIndex = findReference.end(2);
				} else {
					writeToLog("\t"+"[property]"+" "+property+"\t occurs at position "+propertyIndex+" (line "+getLineNumber(propertyIndex)+")\n");
	                propertyIndex = findReference.end(2);
	                int todoPosition = getPositionOfNextLineFeed(propertyIndex);
	                String start = file.substring(0, todoPosition);
	                String end = file.substring(todoPosition);
	                file = start + " // TODO [property]" + end;				
					numberProperty++;
	                containsTODOs = true;
	            }
			} catch (RuntimeException e) {
				propertyMatch = false;		
			}									
		}			
	}
	
    private int getPositionOfNextLineFeed(int currentPos) {
        int index = file.indexOf('\n', currentPos);
        if (index >= 0) {
            return index;
        } else {
            return file.length() - 1;
        }
    }
    
	private void setDeclarations(String className) {
				
		Pattern headOfClass = Pattern.compile("(class\\s+"+className+"\\s[\\w\\s,]*\\{\\s*)\n");
		Matcher findClassDeclaration = headOfClass.matcher(file);		
		if (newEntries.length() > 0) {			
			file = findClassDeclaration.replaceFirst("$1"+newEntries.toString()+"\n");			
		}			
	}
	
	private int findOccurranceOf(String name, int pos) {
		int nextIndex = -1;
		try {
			Pattern parameterTypeDeclaration = Pattern.compile("(\\()\\s*"+name+"\\s*\\)");
			Matcher findParameterDeclaration = parameterTypeDeclaration.matcher(file);						
			findParameterDeclaration.find(pos);		
			nextIndex = findParameterDeclaration.end(1) + 1;			
		} catch (RuntimeException e) {
			return -1;				
		}
		return nextIndex;
	}
	
	private int getLineNumber(int pos) {
		int lineNumber = 0;
		int currentIndex = 0;		
		while (currentIndex <= pos) {
			currentIndex = file.indexOf("\n", currentIndex + 1);
			lineNumber++;
		}		
		return lineNumber;	
	}
	
	private void writeOutput() {
		try {
			dstFile.write(file);
			dstFile.flush();
			dstFile.close();
		} catch (IOException e) {			
			e.printStackTrace();
		}		
	}
	
	private void writeToLog(String logOutput) {
		
		try {
			logFile.write(logOutput);
		} catch (IOException e) {			
			e.printStackTrace();
		}		
	}
	
	private BufferedWriter openOutputWriter(File path) {
		
		BufferedWriter newFile = null;
		try {
			newFile = new BufferedWriter(new FileWriter(path));			
		} catch (IOException e) {			
			e.printStackTrace();
		}		
		return newFile;
	}
	
	private BufferedReader openSourceFile(File path) {
		
		BufferedReader file = null;
		try {
			file = new BufferedReader(new FileReader(path));			
		} catch (IOException e) {			
			e.printStackTrace();
		}
		return file;
	}
	
	private void closeSourceFile() {
		try {
			srcFile.close();
		} catch (IOException e) {		
			e.printStackTrace();
		}
	}	
	
	private void fillBuffer() {
		try {
			while (srcFile.ready()) {
				file += srcFile.readLine() + "\n";
			}
		} catch (IOException e) {			
			e.printStackTrace();
		}
		try {
			srcFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void getAllFiles(File root) {	
		if (root.isFile()) {
			files.add(root);
		}
		if (root.isDirectory()) {
			// get all java files
			FilenameFilter filter = new JAVAFilter();
			File[] innerFiles = root.listFiles(filter);
			for (int i = 0; i < innerFiles.length; i++) {
				files.add(innerFiles[i]);
			}
			// apply getAllFiles() recursively to all inner dirctories
			File[] innerDirectories = root.listFiles();
			for (int i = 0; i < innerDirectories.length; i++) {
				if (innerDirectories[i].isDirectory()) {
					getAllFiles(innerDirectories[i]);
				}
			}
		}
	}
	
	public static void main(String[] args) {
		
		if (args[0] != null && args[1] != null) {		
			ParameterRefactoring pM = new ParameterRefactoring(args[0], args[1]);
			pM.transformFiles();
		} else {
			System.out.println("Please specify two directories (working and logging directory) " +
							   "to perform parameter refactoring.");
		}
		
	}
	
}
