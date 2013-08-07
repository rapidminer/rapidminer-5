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
package com.rapidminer.operator.learner.meta;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.lazy.AttributeBasedVotingLearner;
import com.rapidminer.tools.OperatorService;


/**  
 * This class uses n+1 inner learners and generates n different models
 * by using the last n learners. The predictions of these n models are
 * taken to create n new features for the example set, which is finally
 * used to serve as an input of the first inner learner.  
 * 
 * @author Ingo Mierswa, Helge Homburg
 */
public class Vote extends AbstractStacking {
    
	public Vote(OperatorDescription description) {
		super(description, "Base Learner");
	}		

	@Override
	public String getModelName() {
		return "Vote Model";
	}

	@Override
	public boolean keepOldAttributes() {
		return false;
	}

	@Override
	protected ExecutionUnit getBaseModelLearnerProcess() {
		return getSubprocess(0);
	}

	@Override
	protected Model getStackingModel(ExampleSet stackingLearningSet) throws OperatorException {
		try {
			return OperatorService.createOperator(AttributeBasedVotingLearner.class).doWork(stackingLearningSet);
		} catch (OperatorCreationException e) {
			throw new OperatorException(getName() + ": Not possible to create vote operator.");
		}
	}
}
