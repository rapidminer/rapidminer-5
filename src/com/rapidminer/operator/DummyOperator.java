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
package com.rapidminer.operator;

import java.util.Collections;
import java.util.logging.Level;

import com.rapid_i.deployment.update.client.UpdateDialog;
import com.rapid_i.deployment.update.client.UpdateManager;
import com.rapidminer.RapidMiner;
import com.rapidminer.deployment.client.wsimport.PackageDescriptor;
import com.rapidminer.deployment.client.wsimport.UpdateService;
import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.InputPortExtender;
import com.rapidminer.operator.ports.OutputPortExtender;
import com.rapidminer.operator.ports.quickfix.AbstractQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;

/** This operator cannot be executed. It is merely used by a {@link XMLImporter} to
 *  create a dummy operator that acts as a placeholder for an operator contained in
 *  a plugin that is not installed.
 *  
 * @author Simon Fischer
 *
 */
public class DummyOperator extends Operator {

	private InputPortExtender inExtender = new InputPortExtender("in", getInputPorts());
	private OutputPortExtender outExtender = new OutputPortExtender("out", getOutputPorts());
	
	private String replaces;

	private QuickFix installFix = null;
	
	public DummyOperator(OperatorDescription description) {
		super(description);
		inExtender.start();
		outExtender.start();
	}
	
	@Override
	protected void performAdditionalChecks() {
		super.performAdditionalChecks();
		if (installFix != null) {
			addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(), Collections.singletonList(installFix), "dummy_operator", getReplaces()));
		} else {
			addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(), "dummy_operator", getReplaces()));
		}
	}
	
	private String getRequiredPluginPrefix() {
		if (getReplaces() == null) {
			return null;
		}
		if (getReplaces().startsWith("W-")) {
			return "weka";
		}
		if (getReplaces().indexOf(':') != -1) {
			return getReplaces().substring(0, getReplaces().indexOf(':'));
		}
		return null;
	}

	public void doWork() throws UserError {
		throw new UserError(this, 151, getName(), getReplaces());
	}

	public void setReplaces(String replaces) {
		this.replaces = replaces;
		if (replaces != null) {
			installFix = new AbstractQuickFix(10, true, "install_extension", getExtensionName()) {
				@Override
				public void apply() {
					UpdateDialog.showUpdateDialog(false, getExtensionId());
				}
			};
		} else {
			installFix = null;
		}
	}

	public String getReplaces() {
		return replaces;
	}
	
	private String getExtensionId() {
		try {
			UpdateService service = UpdateManager.getService();
			String extensionId = service.getRapidMinerExtensionForOperatorPrefix(getRequiredPluginPrefix());
			if (extensionId == null) {
				return getRequiredPluginPrefix();
			} else {
				return extensionId;
			}
		} catch (Exception e) {
			//LogService.getRoot().log(Level.WARNING, "Cannot connect to update service: "+e, e);
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
					"com.rapidminer.operator.DummyOperator.connecting_to_update_service_error", 
					e),
					e);

			return getRequiredPluginPrefix();
		}
	}
	
	private String getExtensionName() {
		if (RapidMiner.getExecutionMode().isHeadless()) {
			return getRequiredPluginPrefix();
		}
		try {
			UpdateService service = UpdateManager.getService();
			String extensionId = service.getRapidMinerExtensionForOperatorPrefix(getRequiredPluginPrefix());
			if (extensionId == null) {
				return getRequiredPluginPrefix();
			} else {
				String latest = service.getLatestVersion(extensionId, "ANY");
				PackageDescriptor desc = service.getPackageInfo(extensionId, latest, "ANY");
				if(desc == null) {
					return getRequiredPluginPrefix();
				}
				return desc.getName();
			}
		} catch (Exception e) {
			//LogService.getRoot().log(Level.WARNING, "Cannot connect to update service: "+e, e);
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
					"com.rapidminer.operator.DummyOperator.connecting_to_update_service_error", 
					e),
					e);
			return getRequiredPluginPrefix();
		}
	}
}
