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
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to add the missing image tags in the GUI Manual
 * document (they are missing due to the usage of pdflatex instead of latex).
 * 
 * @author Tobias Beckers, Christian Lohmann, Marcin Skirzynski
 */
public class HTMLImageAdder {
	
	private RandomAccessFile htmlraf;
	private DataOutputStream out;
	private static String fLINK = "\\[width=[0-9.]*\\]([a-zA-Z0-9_]*.png)";
	private static String fFRAGMENT = "<img src=\"$1\">";
	
	public HTMLImageAdder(String pathHtmlFileIn) {
		File htmlFile = new File(pathHtmlFileIn);
		File fileOut = new File(htmlFile.getParent(), "guimanualtemp.html");
		
		try {
			htmlraf = new RandomAccessFile(htmlFile, "r");
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			out = new DataOutputStream(new
			        BufferedOutputStream(new FileOutputStream(fileOut)));	
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		correct();
	}
	
	public void correct() {
		String result;
		String tmp;
		try {
			while(htmlraf.getFilePointer() != htmlraf.length()) {
				tmp = htmlraf.readLine();
				result = replaceLinks(tmp);	
					for(int i=0;i<(result+"\n").length();i++) {
						out.write((byte)(result+"\n").charAt(i));						
					}	
			}
			out.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static String replaceLinks(String aHtmlTextWithLinks){
		Pattern pattern = Pattern.compile(fLINK);
		Matcher matcher = pattern.matcher(aHtmlTextWithLinks);
		return matcher.replaceAll(fFRAGMENT);	
	}
	
	public static void main(String[] args) {
		new HTMLImageAdder(args[0]);	
	}
}
