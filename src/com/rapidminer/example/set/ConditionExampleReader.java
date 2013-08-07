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
package com.rapidminer.example.set;

import java.util.Iterator;

import com.rapidminer.example.Example;


/**
 * This ExampleReader skips all examples that do not fulfil a specified
 * {@link Condition}.
 * 
 * @deprecated ConditionExampleReaders are now replaced by fixed mappings
 * 
 * @author Simon Fischer, Ingo Mierswa
 *          ingomierswa Exp $
 */
@Deprecated
public class ConditionExampleReader extends AbstractExampleReader {

	/** The example reader that provides a complete example set. */
	private Iterator<Example> parent;

	/** The used condition. */
	private Condition condition;

	/** The example that will be returned by the next invocation of next(). */
	private Example currentExample;

    /** Indicates if the inverted condition should be fulfilled. */
    private boolean inverted = false;
    
	/**
	 * Constructs a new ConditionExampleReader the next() method of which
	 * returns only examples that fulfil a specified condition.
	 */
	public ConditionExampleReader(Iterator<Example> parent, Condition condition, boolean inverted) {
		this.parent = parent;
		this.currentExample = null;
		this.condition = condition;
        this.inverted = inverted;
	}

	public boolean hasNext() {
		while (currentExample == null) {
			if (!parent.hasNext())
				return false;
			Example e = parent.next();
            if (!inverted) {
                if (condition.conditionOk(e))
                    currentExample = e;
            } else {
                if (!condition.conditionOk(e))
                    currentExample = e;                
            }
		}
		return true;
	}

	public Example next() {
		hasNext();
		Example dummy = currentExample;
		currentExample = null;
		return dummy;
	}
}
