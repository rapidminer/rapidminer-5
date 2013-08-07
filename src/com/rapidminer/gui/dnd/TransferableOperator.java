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

package com.rapidminer.gui.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;

import com.rapidminer.operator.Operator;
import com.rapidminer.repository.RepositoryLocation;

/** Provides a transferable wrapper for Operators in order to drag-n-drop them in the
 *  Process-Tree. 
 *
 *  @see com.rapidminer.gui.operatortree.OperatorTree
 *  @author Helge Homburg
 */
public class TransferableOperator implements Transferable {

	public static final DataFlavor LOCAL_TRANSFERRED_OPERATORS_FLAVOR = 
			new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + Operator.class.getName(), "transferedOperatorArray");
	
	public static final DataFlavor LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR = 
			new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + RepositoryLocation.class.getName(), "transferedOperatorArray");

	private static final DataFlavor[] DATA_FLAVORS = {
			TransferableOperator.LOCAL_TRANSFERRED_OPERATORS_FLAVOR,
			DataFlavor.stringFlavor
			//TransferableOperator.TRANSFERRED_OPERATOR_FLAVOR         
	};

	private final Operator[] transferedOperators;

	public TransferableOperator(Operator[] operators) {
		this.transferedOperators = operators;
	}

	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if (flavor.equals(LOCAL_TRANSFERRED_OPERATORS_FLAVOR)) {
			return this.transferedOperators;
		}
		if (flavor.equals(DataFlavor.stringFlavor)) {
			StringBuilder b = new StringBuilder();
			for (Operator op : transferedOperators) {
				b.append(op.getXML(false));
			}
			return b.toString();
//		} else if (flavor.equals(XML_SERIALIZED_TRANSFERRED_OPERATORS_FLAVOR)) {
//			return new ByteArrayInputStream(transferedOperators[0].getXML(false).getBytes("UTF-8"));
		} else {
			throw new UnsupportedFlavorException(flavor);
		}
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return (Arrays.asList(DATA_FLAVORS).contains(flavor));
	}

	public DataFlavor[] getTransferDataFlavors() {
		return DATA_FLAVORS;
	}

	protected Operator[] getOperators() {
		return this.transferedOperators;
	}
}
