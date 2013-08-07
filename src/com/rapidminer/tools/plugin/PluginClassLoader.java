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
package com.rapidminer.tools.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;

import com.rapidminer.RapidMiner;

/**
 * The class loader for a plugin (extending URLClassLoader). Since a plugin
 * might depend on other plugins the URLs of these plugins are also added to the
 * current class loader.
 * 
 * @author Ingo Mierswa
 */
public class PluginClassLoader extends URLClassLoader {

    private ArrayList<Plugin> parentPlugins = new ArrayList<Plugin>();

    /**
     * This constructor is for plugins that only depend on the core.
     * 
     * @param urls
     *            These URLs will be used for class building.
     */
    public PluginClassLoader(URL[] urls) {
        super(urls, RapidMiner.class.getClassLoader());
    }

    @Deprecated
    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    /**
     * This method can be used if for a plugin already is known which parent plugins are needed.
     * Otherwise you can use the standard constructor and add the Dependencies later using {@link #addDependency(Plugin)}.
     * 
     * @param urls
     * @param parentPlugins
     */
    public PluginClassLoader(URL[] urls, Plugin... parentPlugins) {
        super(urls, RapidMiner.class.getClassLoader());

        for (Plugin plugin : parentPlugins)
            this.parentPlugins.add(plugin);

    }

    /**
     * This adds another parent plugin to the list of dependencies.
     */
    public void addDependency(Plugin parentPlugin) {
        parentPlugins.add(parentPlugin);
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c = null;
        try {
            c = super.loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            // ClassNotFoundException thrown if class not found
            // from the urls registered nor the core class loader
        }
        if (c == null) {
            for (Plugin plugin : parentPlugins) {
                try {
                    return plugin.getClassLoader().loadClass(name, resolve);
                } catch (ClassNotFoundException e) {
                    // ClassNotFoundException thrown if class not found
                    // from the parent extension
                }
            }
        }
        if (c == null) {
            // If still not found, then invoke findClass in order
            // to find the class.
            c = findClass(name);
        }
        // if no class found during findClass an Exception is thrown anyway
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    @Override
    public URL getResource(String name) {
        URL url = super.getResource(name);

        for (Plugin parentPlugin : parentPlugins) {
            url = parentPlugin.getClassLoader().getResource(name);
            if (url != null)
                break;
        }

        if (url == null) {
            url = findResource(name);
        }
        return url;
    }

    @Override
    public String toString() {
        return "PluginClassLoader (" + Arrays.asList(getURLs()) + ")";
    }
}
