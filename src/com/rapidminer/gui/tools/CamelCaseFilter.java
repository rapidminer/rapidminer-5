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
package com.rapidminer.gui.tools;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Matches, e.g. BiDi to BinDiscretization
 * 
 * @author Simon Fischer
 * 
 */
public class CamelCaseFilter {

    private final String filterString;
    private Pattern pattern = null;

    /**
     * This is the constructor. Only non-null values might be passed!
     */
    public CamelCaseFilter(String filterString) {
        if (filterString != null && filterString.trim().length() > 0) {
            StringBuilder regexp = new StringBuilder();
            filterString = filterString.trim();
            regexp.append(".*");
            regexp.append(filterString);
            regexp.append(".*|(");
            boolean first = true;
            for (char c : filterString.toCharArray()) {
                if (first || Character.isUpperCase(c) || Character.isDigit(c)) {
                    regexp.append(".*");
                }
                if (c != ' ') {
                    if (first) {
                        regexp.append(Character.toUpperCase(c));
                    } else {
                        regexp.append(c);
                    }
                    first = false;
                }

            }
            regexp.append(".*)");
            try {
                this.pattern = Pattern.compile(regexp.toString());
            } catch (PatternSyntaxException e) {
                this.pattern = null;
                // TODO: maybe other handling than NoOp?
                // can happen only if regexp special chars in filter string
            }
            this.filterString = filterString.toLowerCase();
        } else {
            this.filterString = "";
        }

    }

    public boolean matches(String string) {
        if (string == null) {
            return false;
        } else {
            return string.toLowerCase().contains(filterString) || ((pattern != null) && pattern.matcher(string).matches());
        }
    }

    @Override
    public String toString() {
        if (pattern != null) {
            return pattern.toString();
        } else {
            return filterString;
        }
    }
}
