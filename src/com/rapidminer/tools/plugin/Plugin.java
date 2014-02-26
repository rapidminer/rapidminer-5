/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2014 by RapidMiner and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapidminer.com
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
package com.rapidminer.tools.plugin;

import java.awt.Frame;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.rapid_i.Launcher;
import com.rapid_i.deployment.update.client.ManagedExtension;
import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMiner.ExecutionMode;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.ProcessRenderer;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.safemode.SafeMode;
import com.rapidminer.gui.templates.BuildingBlock;
import com.rapidminer.gui.tools.SplashScreen;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.gui.tools.dialogs.AboutBox;
import com.rapidminer.io.Base64;
import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.ResourceSource;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.WebServiceTools;

/**
 * <p>
 * The class for RapidMiner plugins. This class is used to encapsulate the .jar file which must be in the
 * <code>lib/plugins</code> subdirectory of RapidMiner. Provides methods for plugin checks, operator registering, and
 * getting information about the plugin.
 * </p>
 * <p>
 * Plugin dependencies must be defined in the form <br />
 * plugin_name1 (plugin_version1) # ... # plugin_nameM (plugin_versionM) < /br> of the manifest parameter
 * <code>Plugin-Dependencies</code>. You must define both the name and the version of the desired plugins and separate
 * them with &quot;#&quot;.
 * </p>
 * 
 * @author Simon Fischer, Ingo Mierswa, Nils Woehler
 */
public class Plugin {

	/**
	 * The name for the manifest entry RapidMiner-Type which can be used to indicate that a jar file is a RapidMiner
	 * plugin.
	 */
	public static final String RAPIDMINER_TYPE = "RapidMiner-Type";

	/** The value for the manifest entry RapidMiner-Type which indicates that a jar file is a RapidMiner plugin. */
	public static final String RAPIDMINER_TYPE_PLUGIN = "RapidMiner_Extension";

	private static final ClassLoader MAJOR_CLASS_LOADER;

	static {
		try {
			MAJOR_CLASS_LOADER = AccessController.doPrivileged(new PrivilegedExceptionAction<ClassLoader>() {

				@Override
				public ClassLoader run() throws Exception {
					return new AllPluginsClassLoader();
				}
			});
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("Cannot create major class loader: " + e.getMessage(), e);
		}
	}

	/**
	 * The jar archive of the plugin which must be placed in the <code>lib/plugins</code> subdirectory of RapidMiner.
	 */
	private final JarFile archive;

	/** The file for this plugin. */
	private final File file;

	/** The class loader based on the plugin file. */
	private PluginClassLoader classLoader;

	/** The name of the plugin. */
	private String name;

	/** The version of the plugin. */
	private String version;

	/** The vendor of the plugin. */
	private String vendor;

	/** The url for this plugin (in WWW). */
	private String url;

	/** The RapidMiner version which is needed for this plugin. */
	private String necessaryRapidMinerVersion = "0";

	/** The plugins and their versions which are needed for this plugin. */
	private final List<Dependency> pluginDependencies = new LinkedList<Dependency>();

	private String extensionId;

	private String pluginInitClassName;

	private String pluginResourceObjects;

	private String pluginResourceOperators;

	private String pluginParseRules;

	private String pluginGroupDescriptions;

	private String pluginErrorDescriptions;

	private String pluginUserErrorDescriptions;

	private String pluginGUIDescriptions;

	private String prefix;

	private boolean disabled = false;

	/** The collection of all plugins. */
	private static final List<Plugin> allPlugins = new LinkedList<Plugin>();

	/** Creates a new plugin based on the plugin .jar file. */
	public Plugin(File file) throws IOException {
		this.file = file;
		this.archive = new JarFile(this.file);
		this.classLoader = makeInitialClassloader();
		Tools.addResourceSource(new ResourceSource(this.classLoader));
		fetchMetaData();

		if (!RapidMiner.getExecutionMode().isHeadless()) {
			RapidMiner.getSplashScreen().addExtension(this);
		}
	}

	/**
	 * This method will create an initial class loader that is only used to
	 * access the manifest.
	 * After the manifest is read, a new class loader will be constructed from
	 * all dependencies.
	 */
	private PluginClassLoader makeInitialClassloader() {
		URL url;
		try {
			url = this.file.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException("Cannot make classloader for plugin: " + e, e);
		}
		final PluginClassLoader cl = new PluginClassLoader(new URL[] { url });
		return cl;
	}

	/**
	 * This method will build the final class loader for this plugin
	 * that contains all class loaders of all plugins this plugin depends on.
	 * 
	 * This must be called after all plugins have been initially loaded.
	 */
	public void buildFinalClassLoader() {
		// add URLs of plugins this plugin depends on
		for (Dependency dependency : this.pluginDependencies) {
			final Plugin other = getPluginByExtensionId(dependency.getPluginExtensionId());
			classLoader.addDependency(other);
		}

	}

	/** Returns the name of the plugin. */
	public String getName() {
		return name;
	}

	/** Returns the version of this plugin. */
	public String getVersion() {
		return version;
	}

	/** Returns the necessary RapidMiner version. */
	public String getNecessaryRapidMinerVersion() {
		return necessaryRapidMinerVersion;
	}

	/**
	 * Returns the class name of the plugin init class
	 */
	public String getPluginInitClassName() {
		return pluginInitClassName;
	}

	public String getPluginParseRules() {
		return pluginParseRules;
	}

	public String getPluginGroupDescriptions() {
		return pluginGroupDescriptions;
	}

	public String getPluginErrorDescriptions() {
		return pluginErrorDescriptions;
	}

	public String getPluginUserErrorDescriptions() {
		return pluginUserErrorDescriptions;
	}

	public String getPluginGUIDescriptions() {
		return pluginGUIDescriptions;
	}

	/**
	 * Returns the resource identifier of the xml file specifying the operators
	 */
	public String getPluginResourceOperators() {
		return pluginResourceOperators;
	}

	/**
	 * Returns the resource identifier of the IO Object descriptions.
	 */
	public String getPluginResourceObjects() {
		return pluginResourceObjects;
	}

	/** Returns the plugin dependencies of this plugin. */
	public List getPluginDependencies() {
		return pluginDependencies;
	}

	/**
	 * Returns the class loader of this plugin. This class loader should be used in cases where Class.forName(...)
	 * should be used, e.g. for implementation finding in all classes (including the core and the plugins).
	 */
	public PluginClassLoader getClassLoader() {
		return this.classLoader;
	}

	/**
	 * Returns the class loader of this plugin. This class loader should be used in cases where Class.forName(...)
	 * should find a class explicitly defined in this plugin jar.
	 */
	public ClassLoader getOriginalClassLoader() {
		try {
			// this.archive = new JarFile(this.file);
			final URL url = new URL("file", null, this.file.getAbsolutePath());
			return AccessController.doPrivileged(new PrivilegedExceptionAction<ClassLoader>() {

				@Override
				public ClassLoader run() throws Exception {
					return new URLClassLoader(new URL[] { url }, Plugin.class.getClassLoader());
				}
			});

		} catch (IOException e) {
			return null;
		} catch (PrivilegedActionException e) {
			return null;
		}
	}

	/** Checks the RapidMiner version and plugin dependencies. */
	private boolean checkDependencies(List plugins) {
		if (RapidMiner.getLongVersion().compareTo(necessaryRapidMinerVersion) < 0)
			return false;
		if (pluginDependencies.size() > 1) {
			throw new UnsupportedOperationException("Only one dependent plugin allowed!");
		}
		// other plugins
		Iterator i = pluginDependencies.iterator();
		while (i.hasNext()) {
			Dependency dependency = (Dependency) i.next();
			if (!dependency.isFulfilled(plugins))
				return false;
		}
		// all ok
		return true;
	}

	/** Collects all meta data of the plugin from the manifest file. */
	private void fetchMetaData() {
		try {
			java.util.jar.Attributes atts = archive.getManifest().getMainAttributes();
			name = getValue(atts, "Implementation-Title");

			if (name == null) {
				name = archive.getName();
			}
			version = getValue(atts, "Implementation-Version");
			if (version == null)
				version = "";

			url = getValue(atts, "Implementation-URL");
			vendor = getValue(atts, "Implementation-Vendor");

			prefix = getValue(atts, "Namespace");
			extensionId = getValue(atts, "Extension-ID");
			pluginInitClassName = getValue(atts, "Initialization-Class");
			pluginResourceObjects = getDescriptorResource("IOObject-Descriptor", false, false, atts);
			pluginResourceOperators = getDescriptorResource("Operator-Descriptor", false, true, atts);
			pluginParseRules = getDescriptorResource("ParseRule-Descriptor", false, false, atts);
			pluginGroupDescriptions = getDescriptorResource("Group-Descriptor", false, false, atts);

			pluginErrorDescriptions = getDescriptorResource("Error-Descriptor", false, true, atts);
			pluginUserErrorDescriptions = getDescriptorResource("UserError-Descriptor", false, true, atts);
			pluginGUIDescriptions = getDescriptorResource("GUI-Descriptor", false, true, atts);

			necessaryRapidMinerVersion = getValue(atts, "RapidMiner-Version");
			if (necessaryRapidMinerVersion == null) {
				necessaryRapidMinerVersion = "0";
			}
			String dependencies = getValue(atts, "Plugin-Dependencies");
			if (dependencies == null)
				dependencies = "";
			addDependencies(dependencies);

			RapidMiner.splashMessage("loading_plugin", name);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getValue(java.util.jar.Attributes atts, String key) {
		String result = atts.getValue(key);
		if (result == null) {
			return null;
		} else {
			result = result.trim();
			if (result.isEmpty()) {
				return null;
			} else {
				return result;
			}
		}

	}

	private String getDescriptorResource(String typeName, boolean mandatory, boolean isBundle, java.util.jar.Attributes atts) throws IOException {
		String value = getValue(atts, typeName);
		if (value == null) {
			if (mandatory) {
				throw new IOException("Manifest attribute '" + typeName + "' is not defined.");
			} else {
				return null;
			}
		} else {
			if (isBundle) {
				return toResourceBundleIdentifier(value);
			} else {
				return toResourceIdentifier(value);
			}
		}
	}

	private String toResourceBundleIdentifier(String value) {
		if (value.startsWith("/"))
			value = value.substring(1);
		if (value.endsWith(".properties")) {
			value = value.substring(0, value.length() - 11);
		}
		return value;
	}

	/**
	 * Removes leading slash if present.
	 */
	private String toResourceIdentifier(String value) {
		if (value.startsWith("/"))
			value = value.substring(1);
		return value;
	}

	/** Register plugin dependencies. */
	private void addDependencies(String dependencies) {
		pluginDependencies.addAll(Dependency.parse(dependencies));
	}

	public void registerOperators() {
		if (disabled) {
			//LogService.getRoot().warning("Plugin " + getName() + " disabled due to previous errors. Not registering operators.");
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.plugin.Plugin.registring_operators_error", getName());
		}
		InputStream in = null;
		// trying normal plugins
		if (pluginResourceOperators != null) {
			URL operatorsURL = this.classLoader.getResource(pluginResourceOperators);
			if (operatorsURL == null) {
				//LogService.getRoot().log(Level.WARNING, "Operator descriptor '" + pluginResourceOperators + "' does not exist in '" + archive.getName() + "'!");
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.tools.plugin.Plugin.operators_description_not_existing",
						new Object[] { pluginResourceOperators, archive.getName() });
				return;
			} else {
				// register operators
				try {
					in = operatorsURL.openStream();
				} catch (IOException e) {
					//LogService.getRoot().log(Level.WARNING, "Cannot read operator descriptor '" + operatorsURL + "' from '" + archive.getName() + "'!", e);
					LogService.getRoot().log(Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(),
									"com.rapidminer.tools.plugin.Plugin.operator_descriptor_reading_error",
									operatorsURL, archive.getName()),
							e);
					return;
				}
			}
		} else if (pluginInitClassName != null) {
			//LogService.getRoot().info("No operator descriptor specified for plugin " + getName() + ". Trying plugin initializtation class " + pluginInitClassName + ".");
			LogService.getRoot().log(Level.INFO,
					"com.rapidminer.tools.plugin.Plugin.operator_descriptor_not_specified",
					new Object[] { getName(), pluginInitClassName });
			// if no operators.xml found: Try via PluginInit method getOperatorStream()
			try {
				// important: here the combined class loader has to be used
				Class<?> pluginInitator = Class.forName(pluginInitClassName, false, getClassLoader());
				Method registerOperatorMethod = pluginInitator.getMethod("getOperatorStream", new Class[] { ClassLoader.class });
				in = (InputStream) registerOperatorMethod.invoke(null, new Object[] { getClassLoader() });
			} catch (ClassNotFoundException e) {} catch (SecurityException e) {} catch (NoSuchMethodException e) {} catch (IllegalArgumentException e) {} catch (IllegalAccessException e) {} catch (InvocationTargetException e) {}
		}
		if (in != null) {
			OperatorService.registerOperators(archive.getName(), in, this.classLoader, this);
		} else {
			//LogService.getRoot().warning("No operator descriptor defined for: " + getName());
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.tools.plugin.Plugin.operator_descriptor_not_defined",
					getName());
		}
	}

	/**
	 * Register all things delivered with this plugin.
	 * 
	 * @throws PluginException
	 */
	public void registerDescriptions() throws PluginException {
		// registering settings for internationalization
		if (pluginErrorDescriptions != null)
			I18N.registerErrorBundle(ResourceBundle.getBundle(pluginErrorDescriptions, Locale.getDefault(), this.classLoader));
		if (pluginGUIDescriptions != null)
			I18N.registerGUIBundle(ResourceBundle.getBundle(pluginGUIDescriptions, Locale.getDefault(), this.classLoader));
		if (pluginUserErrorDescriptions != null)
			I18N.registerUserErrorMessagesBundle(ResourceBundle.getBundle(pluginUserErrorDescriptions, Locale.getDefault(), this.classLoader));

		// registering renderers
		if (pluginResourceObjects != null) {
			URL resource = this.classLoader.getResource(pluginResourceObjects);
			if (resource != null) {
				RendererService.init(name, resource, this.classLoader);
			} else {
				throw new PluginException("Cannot find io object descriptor '" + pluginResourceObjects + "' for plugin " + getName() + ".");
			}
		}

		// registering parse rules
		if (pluginParseRules != null) {
			URL resource = this.classLoader.getResource(pluginParseRules);
			if (resource != null) {
				XMLImporter.importParseRules(resource, this);
			} else {
				throw new PluginException("Cannot find parse rules '" + pluginParseRules + "' for plugin " + getName() + ".");
			}
		}

		// registering colors
		if (pluginGroupDescriptions != null) {
			ProcessRenderer.registerAdditionalObjectColors(pluginGroupDescriptions, name, classLoader);
			ProcessRenderer.registerAdditionalGroupColors(pluginGroupDescriptions, name, classLoader);
		}
	}

	/**
	 * Returns a list of building blocks. If this plugin does not define any building blocks, an empty list will be
	 * returned.
	 */
	public List<BuildingBlock> getBuildingBlocks() {
		List<BuildingBlock> result = new LinkedList<BuildingBlock>();

		URL url = null;
		try {
			url = new URL("file", null, this.file.getAbsolutePath());
		} catch (MalformedURLException e1) {
			//LogService.getRoot().log(Level.WARNING, "Cannot load plugin building blocks. Skipping...", e1);
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.plugin.Plugin.loading_plugin_building_blocks_error_skipping"),
					e1);
		}
		if (url != null) {
			// TODO: Check why we have to build an independentLoader? If yes: Use doPriviledged.
			// ClassLoader independentLoader = new PluginClassLoader(new URL[] { url });
			// URL bbDefinition = independentLoader.getResource(Tools.RESOURCE_PREFIX + "buildingblocks.txt");
			URL bbDefinition = classLoader.getResource(Tools.RESOURCE_PREFIX + "buildingblocks.txt");
			if (bbDefinition != null) {
				BufferedReader in = null;
				try {
					in = new BufferedReader(new InputStreamReader(bbDefinition.openStream()));

					String line = null;
					while ((line = in.readLine()) != null) {
						URL bbURL = this.classLoader.getResource(Tools.RESOURCE_PREFIX + line);
						BufferedReader bbIn = null;
						try {
							bbIn = new BufferedReader(new InputStreamReader(WebServiceTools.openStreamFromURL(bbURL)));
							result.add(new BuildingBlock(bbIn, BuildingBlock.PLUGIN_DEFINED));
						} catch (IOException e) {
							//LogService.getRoot().log(Level.WARNING, "Cannot load plugin building blocks. Skipping...", e);
							LogService.getRoot().log(Level.WARNING,
									I18N.getMessage(LogService.getRoot().getResourceBundle(),
											"com.rapidminer.tools.plugin.Plugin.loading_plugin_building_blocks_error_skipping"),
									e);
						} finally {
							if (bbIn != null) {
								bbIn.close();
							}
						}
					}
				} catch (IOException e) {
					//LogService.getRoot().log(Level.WARNING, "Cannot load plugin building blocks.", e);
					LogService.getRoot().log(Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(),
									"com.rapidminer.tools.plugin.Plugin.loading_plugin_building_blocks_error"),
							e);
				} finally {
					if (in != null) {
						try {
							in.close();
						} catch (IOException e) {
							//LogService.getRoot().log(Level.WARNING, "Cannot close stream to plugin building blocks.", e);
							LogService.getRoot().log(Level.WARNING,
									I18N.getMessage(LogService.getRoot().getResourceBundle(),
											"com.rapidminer.tools.plugin.Plugin.closing_plugin_building_blocks_error"),
									e);
						}
					}
				}
			}
		}
		return result;
	}

	/** Creates the about box for this plugin. */
	public AboutBox createAboutBox(Frame owner) {
		ClassLoader simpleClassLoader = makeInitialClassloader();
		String about = "";
		try {
			URL url = simpleClassLoader.getResource("META-INF/ABOUT.NFO");
			if (url != null)
				about = Tools.readTextFile(new InputStreamReader(url.openStream()));
		} catch (Exception e) {
			//LogService.getRoot().log(Level.WARNING, "Error reading ABOUT.NFO for plugin " + getName(), e);
			I18N.getMessage(ResourceBundle.getBundle("com.rapidminer.resources.i18n.LogMessages"), "com.rapidminer.tools.I18N.plugin_warning1", Level.WARNING, getName(), e);
		}
		Image productLogo = null;
		try {
			InputStream imageIn = simpleClassLoader.getResourceAsStream("META-INF/icon.png");
			productLogo = ImageIO.read(imageIn);
		} catch (Exception e) {
			//LogService.getRoot().log(Level.WARNING, "Error reading icon.png for plugin " + getName(), e);
			I18N.getMessage(ResourceBundle.getBundle("com.rapidminer.resources.i18n.LogMessages"), "com.rapidminer.tools.I18N.plugin_warning2", Level.WARNING, getName(), e);
		}
		return new AboutBox(owner, name, version, "Vendor: " + (vendor != null ? vendor : "unknown"), url, about, true, productLogo);
	}

	/** Scans the directory for jar files and calls {@link #registerPlugins(List, boolean)} on the list of files. */
	private static void findAndRegisterPlugins(File pluginDir, boolean showWarningForNonPluginJars) {
		List<File> files = new LinkedList<File>();
		if (pluginDir == null) {
			//LogService.getRoot().warning("findAndRegisterPlugins called with null directory.");
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.plugin.Plugin.findandregisterplugins_called_with_null_directory");
			return;
		}
		if (!(pluginDir.exists() && pluginDir.isDirectory())) {
			//LogService.getRoot().config("Plugin directory " + pluginDir + " does not exist.");
			LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.plugin.Plugin.plugin_dir_not_existing", pluginDir);
		} else {
			//LogService.getRoot().config("Scanning plugins in " + pluginDir + ".");
			LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.plugin.Plugin.scanning_for_plugins", pluginDir);
			files.addAll(Arrays.asList(pluginDir.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".jar");
				}
			})));
		}
		registerPlugins(files, showWarningForNonPluginJars);
	}

	/** Makes {@link Plugin} s from all files and adds them to {@link #allPlugins}.
	 * After all Plugins are loaded, they must be assigend their final class loader.
	 *  */
	private static void registerPlugins(List<File> files, boolean showWarningForNonPluginJars) {
		List<Plugin> newPlugins = new LinkedList<Plugin>();
		for (File file : files) {
			try {
				JarFile jarFile = new JarFile(file);
				Manifest manifest = jarFile.getManifest();
				Attributes attributes = manifest.getMainAttributes();
				if (RAPIDMINER_TYPE_PLUGIN.equals(attributes.getValue(RAPIDMINER_TYPE))) {
					final Plugin plugin = new Plugin(file);
					final Plugin conflict = getPluginByExtensionId(plugin.getExtensionId(), newPlugins);
					if (conflict == null) {
						newPlugins.add(plugin);
					} else {
						// keep plugin with higher version number
						VersionNumber newVersion = new VersionNumber(plugin.getVersion());
						VersionNumber conflictVersion = new VersionNumber(conflict.getVersion());
						if (newVersion != null && conflictVersion != null) {
							if (newVersion.compareTo(conflictVersion) > 0) {
								LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.plugin.Plugin.duplicate_plugin_definition_higher_version",
										new Object[] { plugin.getExtensionId(), file, conflict.file });
								newPlugins.remove(conflict);
								newPlugins.add(plugin);
							}
						} else {
							//LogService.getRoot().warning("Duplicate plugin definition for plugin " + plugin.getExtensionId() + " in " + conflict.file + " and " + file + ". Keeping the first.");
							LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.plugin.Plugin.duplicate_plugin_definition",
									new Object[] { plugin.getExtensionId(), conflict.file, file });
						}
					}
				} else {
					if (showWarningForNonPluginJars)
						//LogService.getRoot().warning("The jar file '" + jarFile.getName() + "' does not contain an entry '" + RAPIDMINER_TYPE + "' in its manifest and will therefore not be loaded (if this file actually is a plugin updating the plugin file might help).");
						LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.plugin.Plugin.jar_file_does_not_contain_entry",
								new Object[] { jarFile.getName(), RAPIDMINER_TYPE });
				}
			} catch (Throwable e) {
				//LogService.getRoot().log(Level.WARNING, "Cannot load plugin '" + file + "': " + e.getMessage(), e);
				LogService.getRoot().log(
						Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.tools.plugin.Plugin.plugin_loading_error", file, e.getMessage()), e);
			}
		}
		for (Plugin newPlugin : newPlugins) {
			LogService.getRoot().log(Level.INFO, "Register plugin: " + newPlugin.getName());
			Plugin oldPlugin = getPluginByExtensionId(newPlugin.getExtensionId(), allPlugins);
			if (oldPlugin == null) {
				allPlugins.add(newPlugin);
			} else {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.plugin.Plugin.duplicate_plugin_definition",
						new Object[] { newPlugin.getExtensionId(), oldPlugin.getFile(), newPlugin.getFile() });
			}
		}
	}

	@Override
	public String toString() {
		return name + " " + version + " (" + archive.getName() + ") depending on " + pluginDependencies;
	}

	/**
	 * Finds all plugins in lib/plugins directory and initializes them.
	 */
	private static void registerAllPluginDescriptions() {
		Iterator<Plugin> i = allPlugins.iterator();
		while (i.hasNext()) {
			Plugin plugin = i.next();
			if (!plugin.checkDependencies(allPlugins)) {
				//LogService.getRoot().warning("Cannot register operators from '" + plugin.getName() + "': Dependencies not fulfilled! This plugin needs a RapidMiner version " + plugin.getNecessaryRapidMinerVersion() + " and the following plugins:" + Tools.getLineSeparator() + plugin.getPluginDependencies());
				LogService.getRoot().log(
						Level.WARNING,
						"com.rapidminer.tools.plugin.Plugin.registring_operators_error",
						new Object[] { plugin.getName(), plugin.getNecessaryRapidMinerVersion(), Tools.getLineSeparator(),
								plugin.getPluginDependencies() });
				plugin.disabled = true;
				i.remove();
			}
		}

		if (allPlugins.size() > 0) {
			i = allPlugins.iterator();
			while (i.hasNext()) {
				Plugin plugin = i.next();
				try {
					plugin.registerDescriptions();
				} catch (Exception e) {
					//LogService.getRoot().log(Level.WARNING, "Error initializing plugin: " + e, e);
					LogService.getRoot().log(
							Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(),
									"com.rapidminer.tools.plugin.Plugin.plugin_initializing_error", e), e);
					i.remove();
					plugin.disabled = true;
				}
			}
		}
	}

	/**
	 * This method will check all needed dependencies of all currently registered
	 * plugin files and will build the final class loaders for the extensions containing all
	 * dependencies.
	 */
	public static void finalizePluginLoading() {
		// building final class loader with all dependent extensions
		LinkedList<Plugin> queue = new LinkedList<Plugin>(allPlugins);
		HashSet<Plugin> initialized = new HashSet<Plugin>();
		// now initialized every extension that's dependencies are fulfilled as long as we find another per round
		boolean found = false;
		while (found || !queue.isEmpty() && initialized.isEmpty()) {
			found = false;
			Iterator<Plugin> iterator = queue.iterator();
			while (iterator.hasNext()) {
				Plugin plugin = iterator.next();
				boolean dependenciesMet = true;
				for (Dependency dependency : plugin.pluginDependencies) {
					Plugin dependencyPlugin = getPluginByExtensionId(dependency.getPluginExtensionId());
					if (dependencyPlugin == null) {
						// if we cannot find dependency plugin: Don't load this one, instead remove it and post error
						allPlugins.remove(plugin);
						iterator.remove();
						//LogService.getRoot().log(Level.SEVERE, "Cannot load extension '" + plugin.extensionId + "': Depends on '" + dependency.getPluginExtensionId() + "' which cannot be found!");
						LogService.getRoot().log(
								Level.SEVERE,
								"com.rapidminer.tools.plugin.Plugin.loading_extension_error",
								new Object[] { plugin.extensionId, dependency.getPluginExtensionId() });
						found = true;
						dependenciesMet = false;
						break; //break this loop: Nothing to check
					} else {
						dependenciesMet &= initialized.contains(dependencyPlugin);
					}
				}

				// if we have all dependencies met: Load final class loader
				if (dependenciesMet) {
					plugin.buildFinalClassLoader();
					initialized.add(plugin);
					iterator.remove();

					// then we have one more extension that is initialized, next round might find more
					found = true;
				}
			}

		}
	}

	/**
	 * Registers all operators from the plugins previously found by a call of registerAllPluginDescriptions
	 */
	public static void registerAllPluginOperators() {
		for (Plugin plugin : allPlugins) {
			plugin.registerOperators();
		}
	}

	/** Returns a class loader which is able to load all classes (core _and_ all plugins). */
	public static ClassLoader getMajorClassLoader() {
		return MAJOR_CLASS_LOADER;
	}

	/** Returns the collection of all plugins. */
	public static List<Plugin> getAllPlugins() {
		return allPlugins;
	}

	/** Returns the plugin with the given extension id. */
	public static Plugin getPluginByExtensionId(String name) {
		return getPluginByExtensionId(name, allPlugins);
	}

	/** Returns the plugin with the given extension id. */
	private static Plugin getPluginByExtensionId(String name, List<Plugin> plugins) {
		Iterator<Plugin> i = plugins.iterator();
		while (i.hasNext()) {
			Plugin plugin = i.next();
			if (name.equals(plugin.getExtensionId()))
				return plugin;
		}
		return null;
	}

	/**
	 * This method will try to invoke the method void initGui(MainFrame) of PluginInit class of every plugin.
	 */
	public static void initPluginGuis(MainFrame mainframe) {
		callPluginInitMethods("initGui", new Class[] { MainFrame.class }, new Object[] { mainframe }, false);
	}

	/**
	 * This method will try to invoke the public static method initPlugin() of the class com.rapidminer.PluginInit for
	 * arbitrary initializations of the plugins. It is called directly after registering the plugins.
	 */
	public static void initPlugins() {
		callPluginInitMethods("initPlugin", new Class[] {}, new Object[] {}, false);
	}

	public static void initPluginUpdateManager() {
		callPluginInitMethods("initPluginManager", new Class[] {}, new Object[] {}, false);
	}

	public static void initFinalChecks() {
		callPluginInitMethods("initFinalChecks", new Class[] {}, new Object[] {}, false);
	}

	public static void initPluginTests() {
		callPluginInitMethods("initPluginTests", new Class[] {}, new Object[] {}, false);
	}

	private static void callPluginInitMethods(String methodName, Class[] arguments, Object[] argumentValues, boolean useOriginalJarClassLoader) {
		List<Plugin> plugins = getAllPlugins();

		for (Iterator<Plugin> iterator = plugins.iterator(); iterator.hasNext();) {
			Plugin plugin = iterator.next();
			if (!plugin.callInitMethod(methodName, arguments, argumentValues, useOriginalJarClassLoader)) {
				iterator.remove();
			}
		}
	}

	/**
	 * @return true if everything went well, false if a fatal error occurred. The plugin should be unregistered in this case.
	 */
	private boolean callInitMethod(String methodName, Class[] arguments, Object[] argumentValues, boolean useOriginalJarClassLoader) {
		if (pluginInitClassName == null) {
			return true;
		}
		try {
			ClassLoader classLoader;
			if (useOriginalJarClassLoader) {
				classLoader = getOriginalClassLoader();
			} else {
				classLoader = getClassLoader();
			}
			Class<?> pluginInitator = Class.forName(pluginInitClassName, false, classLoader);
			Method initMethod;
			try {
				initMethod = pluginInitator.getMethod(methodName, arguments);
			} catch (NoSuchMethodException e) {
				return true;
			}
			initMethod.invoke(null, argumentValues);
			return true;
		} catch (Throwable e) {
			//LogService.getRoot().log(Level.WARNING, "Plugin initializer " + pluginInitClassName + "." + methodName + " of Plugin " + getName() + " causes an error: " + e.getMessage(), e);
			LogService.getRoot().log(
					Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.plugin.Plugin.plugin_initializer_error", pluginInitClassName, methodName, getName(), e
									.getMessage()), e);
			return false;
		}
	}

	public static void initPluginSplashTexts(SplashScreen splashScreen) {
		if (!RapidMiner.getExecutionMode().isHeadless()) {
			callPluginInitMethods("initSplashTexts", new Class[] { SplashScreen.class }, new Object[] { splashScreen }, false);
		}
	}

	public static void initAboutTexts(Properties properties) {
		callPluginInitMethods("initAboutTexts", new Class[] { Properties.class }, new Object[] { properties }, false);
	}

	public boolean showAboutBox() {
		if (pluginInitClassName == null) {
			return true;
		}
		try {
			Class<?> pluginInitator = Class.forName(pluginInitClassName, false, getClassLoader());
			Method initGuiMethod = pluginInitator.getMethod("showAboutBox", new Class[] {});
			Boolean showAboutBox = (Boolean) initGuiMethod.invoke(null, new Object[] {});
			return showAboutBox.booleanValue();
		} catch (ClassNotFoundException e) {} catch (SecurityException e) {} catch (NoSuchMethodException e) {} catch (IllegalArgumentException e) {} catch (IllegalAccessException e) {} catch (InvocationTargetException e) {}
		return true;
	}

	/**
	 * Initializes all plugins if {@link RapidMiner#PROPERTY_RAPIDMINER_INIT_PLUGINS} is set. Plugins are searched for
	 * in the directory specified by {@link RapidMiner#PROPERTY_RAPIDMINER_INIT_PLUGINS_LOCATION} or, if this is not
	 * set, in the RapidMiner/lib/plugins directory.
	 */
	public static void initAll() {
		// only load managed extensions if execution modes indicates
		if (RapidMiner.getExecutionMode().isLoadingManagedExtensions())
			ManagedExtension.init();

		String loadPluginsString = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_INIT_PLUGINS);
		boolean loadPlugins = Tools.booleanValue(loadPluginsString, true);
		SafeMode safeMode = RapidMinerGUI.getSafeMode();
		boolean isSafeMode = false;
		if (safeMode != null) {
			isSafeMode = safeMode.isSafeMode();
		}
		if (loadPlugins && !isSafeMode) {
			File webstartPluginDir;
			if (RapidMiner.getExecutionMode() == ExecutionMode.WEBSTART) {
				webstartPluginDir = updateWebstartPluginsCache();
			} else {
				webstartPluginDir = null;
			}

			File pluginDir = null;
			String pluginDirString = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_INIT_PLUGINS_LOCATION);
			if (pluginDirString != null && !pluginDirString.isEmpty()) {
				pluginDir = new File(pluginDirString);
			}

			if (pluginDir == null) {
				try {
					pluginDir = getPluginLocation();
				} catch (IOException e) {
					LogService.getRoot().log(
							Level.WARNING,
							"com.rapidminer.tools.plugin.Plugin.no_properties_set",
							new Object[] { RapidMiner.PROPERTY_RAPIDMINER_INIT_PLUGINS, Launcher.PROPERTY_RAPIDMINER_HOME });
				}
			}

			if (webstartPluginDir != null) {
				findAndRegisterPlugins(webstartPluginDir, true);
			}
			if (pluginDir != null) {
				findAndRegisterPlugins(pluginDir, true);
			}
			registerPlugins(ManagedExtension.getActivePluginJars(), true);

			registerAllPluginDescriptions();
			finalizePluginLoading();
			initPlugins();
		} else {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.plugin.Plugin.plugins_skipped");
		}
	}

	/** Updates plugins from the server and returns a cache directory containing the jar files. */
	private static File updateWebstartPluginsCache() {
		// We hash the home URL to a directory name, so we don't have special characters.
		final String homeUrl = System.getProperty(RapidMiner.PROPERTY_HOME_REPOSITORY_URL);
		String dirName;
		try {
			final byte[] md5hash = MessageDigest.getInstance("MD5").digest(homeUrl.getBytes());
			dirName = Base64.encodeBytes(md5hash);
		} catch (NoSuchAlgorithmException e) {
			//LogService.getRoot().log(Level.WARNING, "Failed to hash remote url: " + e, e);
			LogService.getRoot().log(
					Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.plugin.Plugin.hashing_remote_url_error", e), e);
			return null;
		}

		File cacheDir = new File(ManagedExtension.getUserExtensionsDir(), dirName);
		cacheDir.mkdirs();
		File readmeFile = new File(cacheDir, "README.txt");
		try {
			Tools.writeTextFile(readmeFile,
					"This directory contains plugins downloaded from RapidAnalytics instance \n" +
							"  " + homeUrl + ".\n" +
							"These plugins are only used if RapidMiner is started via WebStart from this \n" +
							"server. You can delete the directory if you no longer need the cached plugins.");
		} catch (IOException e1) {
			//LogService.getRoot().log(Level.WARNING, "Failed to create file " + readmeFile + ": " + e1, e1);
			LogService.getRoot().log(
					Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.plugin.Plugin.creating_file_error", readmeFile, e1), e1);
		}

		Document pluginsDoc;
		try {
			URL pluginsListUrl = new URL(homeUrl + "/RAWS/dependencies/resources.xml");
			pluginsDoc = XMLTools.parse(pluginsListUrl.openStream());
		} catch (Exception e) {
			//LogService.getRoot().log(Level.WARNING, "Failed to load extensions list from server: " + e, e);
			LogService.getRoot().log(
					Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.plugin.Plugin.loading_extensions_list_error", e), e);
			return null;
		}

		Set<File> cachedFiles = new HashSet<File>();
		NodeList pluginElements = pluginsDoc.getElementsByTagName("extension");
		boolean errorOccurred = false;
		for (int i = 0; i < pluginElements.getLength(); i++) {
			Element pluginElem = (Element) pluginElements.item(i);
			String pluginName = pluginElem.getTextContent();
			String pluginVersion = pluginElem.getAttribute("version");
			File pluginFile = new File(cacheDir, pluginName + "-" + pluginVersion + ".jar");
			cachedFiles.add(pluginFile);
			if (pluginFile.exists()) {
				//LogService.getRoot().log(Level.CONFIG, "Found extension on server: " + pluginName + ". Local cache exists.");
				LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.plugin.Plugin.extension_found_cache_exists", pluginName);
			} else {
				//LogService.getRoot().log(Level.CONFIG, "Found extension on server: " + pluginName + ". Downloading to local cache.");
				LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.plugin.Plugin.extension_found_downloading", pluginName);
				try {
					URL pluginUrl = new URL(homeUrl + "/RAWS/dependencies/plugins/" + pluginName);
					Tools.copyStreamSynchronously(WebServiceTools.openStreamFromURL(pluginUrl), new FileOutputStream(pluginFile), true);
				} catch (Exception e) {
					//LogService.getRoot().log(Level.WARNING, "Failed to download extension from server: " + e, e);
					LogService.getRoot().log(
							Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(),
									"com.rapidminer.tools.plugin.Plugin.downloading_extension_error", e), e);
					errorOccurred = true; // Don't clear unknown files in this case.
				}
			}
		}
		// clear out of date cache files unless error occurred
		if (!errorOccurred) {
			for (File file : cacheDir.listFiles()) {
				if (file.getName().equals("README.txt")) {
					continue;
				}
				if (!cachedFiles.contains(file)) {
					//LogService.getRoot().log(Level.CONFIG, "Deleting obsolete file " + file + " from extension cache.");
					LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.plugin.Plugin.deleting_obsolete_file", file);
					file.delete();
				}
			}
		}
		return cacheDir;
	}

	/** Specifies whether plugins should be initialized on startup. */
	public static void setInitPlugins(boolean init) {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_INIT_PLUGINS, Boolean.toString(init));
	}

	/** Specifies a directory to scan for plugins. */
	public static void setPluginLocation(String directory) {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_INIT_PLUGINS_LOCATION, directory);
	}

	/** Returns the prefix to be used in the operator keys (namespace). This is also used for the Wiki URL. */
	public String getPrefix() {
		return this.prefix;
	}

	public JarFile getArchive() {
		return archive;
	}

	public File getFile() {
		return file;
	}

	public String getExtensionId() {
		return extensionId;
	}

	/** Returns the directory where plugin files are expected. */
	public static File getPluginLocation() throws IOException {
		String locationProperty = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_INIT_PLUGINS_LOCATION);
		if (locationProperty == null || locationProperty.isEmpty()) {
			return FileSystemService.getLibraryFile("plugins");
		} else {
			return new File(locationProperty);
		}
	}

	/**
	 * This returns the Icon of the extension or null if not present.
	 */
	public ImageIcon getExtensionIcon() {
		URL iconURL = classLoader.findResource("META-INF/icon.png");
		if (iconURL != null)
			return new ImageIcon(iconURL);
		return null;
	}

	/** <strong>Experimental method.</strong>
	 *  Unregisters this plugin, all of its {@link Operator}s, and calls tearDown() and optionally tearDownGUI(MainFrame) on the
	 *  {@link #pluginInitClassName}. Finally, removes the plugin from {@link #allPlugins}. 
	 */
	public void tearDown() {
		OperatorService.unregisterAll(this);
		if (!RapidMiner.getExecutionMode().isHeadless()) {
			callInitMethod("tearDownGUI", new Class[] { MainFrame.class }, new Object[] { RapidMinerGUI.getMainFrame() }, false);
		}
		callInitMethod("tearDown", new Class[0], new Object[0], false);
		allPlugins.remove(this);
	}
}
