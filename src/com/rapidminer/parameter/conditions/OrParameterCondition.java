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
package com.rapidminer.parameter.conditions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.w3c.dom.Element;

import com.rapidminer.io.process.XMLTools;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.tools.XMLException;

/**
 * 
 * @author Sebastian Land
 *
 */
public class OrParameterCondition extends ParameterCondition {
	public static final String ELEMENT_CONDITIONS = "SubConditions";
	public static final String ELEMENT_CONDITION = "Condition";
    private static final String ATTRIBUTE_CONDITION_CLASS = "condition-class";
	
	private ParameterCondition[] conditions;

	public OrParameterCondition(Element element) throws XMLException {
		super(element);
		// get all condition xml-elements
        Element conditionsElement = XMLTools.getChildElement(element, ELEMENT_CONDITIONS, true);
        Collection<Element> conditionElements = XMLTools.getChildElements(conditionsElement, ELEMENT_CONDITION);
        conditions = new ParameterCondition[conditionElements.size()];
        
        // iterate over condition xml-elements
        int idx = 0;
        for (Element conditionElement : conditionElements) {
        	// try to construct a condition object
            try {
                String className = conditionElement.getAttribute(ATTRIBUTE_CONDITION_CLASS);
                Class<?> conditionClass = Class.forName(className);
                Constructor<?> constructor = conditionClass.getConstructor(Element.class);
                conditions[idx] = ((ParameterCondition) constructor.newInstance(conditionElement));
            } catch (ClassNotFoundException e) {
                throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CONDITION_CLASS, e);
            } catch (IllegalArgumentException e) {
                throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CONDITION_CLASS, e);
            } catch (InstantiationException e) {
                throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CONDITION_CLASS, e);
            } catch (IllegalAccessException e) {
                throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CONDITION_CLASS, e);
            } catch (InvocationTargetException e) {
                throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CONDITION_CLASS, e);
            } catch (SecurityException e) {
                throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CONDITION_CLASS, e);
            } catch (NoSuchMethodException e) {
                throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CONDITION_CLASS, e);
            }
        	++idx;
        }
	}

	public OrParameterCondition(ParameterHandler parameterHandler, boolean becomeMandatory, ParameterCondition... conditions) {
		super(parameterHandler, becomeMandatory);
		this.conditions = conditions;
	}

	public OrParameterCondition(ParameterHandler parameterHandler,
			String conditionParameter, boolean becomeMandatory, ParameterCondition... conditions) {
		super(parameterHandler, conditionParameter, becomeMandatory);
		this.conditions = conditions;
	}

	@Override
	public boolean isConditionFullfilled() {
		for (int i = 0; i < conditions.length; ++i) {
			if (conditions[i].isConditionFullfilled()) {
				return true;
			}
		}
		return false;
	}

	
	@Override
    public void getDefinitionAsXML(Element element) {
        Element conditionsElement = XMLTools.addTag(element, ELEMENT_CONDITIONS);
        for (int i = 0; i < conditions.length; ++i) {
        	Element conditionElement = XMLTools.addTag(conditionsElement, ELEMENT_CONDITION);
        	ParameterCondition condition = conditions[i];
            condition.getDefinitionAsXML(conditionElement);
            conditionElement.setAttribute(ATTRIBUTE_CONDITION_CLASS, condition.getClass().getName());
        }
    }
}
