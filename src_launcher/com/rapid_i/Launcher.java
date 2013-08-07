
package com.rapid_i;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 * This class launches RapidMiner by calling com.rapidminer.gui.RapidMinerGUI
 * reflectively. Before doing so, it tries to update rapid miner by looking into
 * the "update" folder.
 * 
 * @author Simon Fischer
 * 
 */
public class Launcher {

	private static final Logger LOGGER = Logger.getLogger(Launcher.class.getName());

	/** The name of the property indicating the home directory of RapidMiner. */
	public static final String PROPERTY_RAPIDMINER_HOME = "rapidminer.home";

	private static final int RELAUNCH_EXIT_CODE = 2;

	public static void ensureRapidMinerHomeSet() {
		String home = System.getProperty(PROPERTY_RAPIDMINER_HOME);
		if (home != null) {
			LOGGER.info(PROPERTY_RAPIDMINER_HOME + " is '" + home + "'.");
		} else {
			LOGGER.info("Property " + PROPERTY_RAPIDMINER_HOME + " is not set. Guessing.");
			String classpath = System.getProperty("java.class.path");
			String pathComponents[] = classpath.split(File.pathSeparator);
			boolean found = false;
			for (int i = 0; i < pathComponents.length; i++) {
				String path = pathComponents[i].trim();
				if (path.endsWith("rapidminer.jar") || path.endsWith("launcher.jar")) {
					File jar = new File(path).getAbsoluteFile();
					String message = "Trying parent directory of '" + jar + "'...";
					File dir = jar.getParentFile();
					if (dir != null) {
						dir = dir.getParentFile();
						if (dir != null) {
							message += "gotcha!";
							found = true;
							System.setProperty(PROPERTY_RAPIDMINER_HOME, dir.getAbsolutePath());
						} else {
							message += "failed";
						}
					} else {
						message += "failed";
					}
					LOGGER.log(Level.INFO, message);
				}
			}

			if (!found) {
				String message = "Trying base directory of classes (build) '";
				URL url = Launcher.class.getClassLoader().getResource(".");
				if (url != null) {
					try {
						File dir = new File(new URI(url.toString()));
						if (dir.exists()) {
							dir = dir.getParentFile();
							message += dir + "'...";
							if (dir != null) {
								message += "gotcha!";
								try {
									System.setProperty(PROPERTY_RAPIDMINER_HOME, dir.getCanonicalPath());
								} catch (IOException e) {
									System.setProperty(PROPERTY_RAPIDMINER_HOME, dir.getAbsolutePath());
								}
							} else {
								message += "failed";
							}
						} else {
							message += "failed";
						}
					} catch (Throwable e) {
						// important: not only URI Syntax Exception since the program must not crash in any case!!!
						// For example: RapidNet integration as applet into Siebel would cause problem with new File(...)
						message += "failed";
					}
				} else {
					message += "failed";
				}
				LOGGER.log(Level.INFO, message);
			}
		}
	}

	private static JProgressBar bar;
	private static JFrame dialog;

	private static boolean updateGUI(File rmHome, File updateZip, File updateScript) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {

				@Override
				public void run() {
					dialog = new JFrame("Updating RapidMiner");
					bar = new JProgressBar(0, 1000);
					dialog.setLayout(new BorderLayout());
					dialog.add(new JLabel("Updating RapidMiner"), BorderLayout.NORTH);
					dialog.add(bar, BorderLayout.CENTER);
					dialog.pack();
					dialog.setLocationRelativeTo(null);
					dialog.setVisible(true);
				}
			});
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Cannot show update dialog.", e);
			return false;
		}
		boolean success = true;
		if (updateZip != null) {
			try {
				success &= updateDiffZip(rmHome, updateZip, true);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Update from " + updateZip + " failed: " + e, e);
				JOptionPane.showMessageDialog(dialog, "Update from " + updateZip + " failed: " + e, "Update Failed", JOptionPane.ERROR_MESSAGE);
				success = false;
			}
		}
		if (updateScript != null) {
			try {
				success &= executeUpdateScript(rmHome, new FileInputStream(updateScript), true);
				updateScript.delete();
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Update script " + updateScript + " failed: " + e, e);
				JOptionPane.showMessageDialog(dialog, "Update from " + updateScript + " failed: " + e, "Update Failed", JOptionPane.ERROR_MESSAGE);
				success = false;
			}
		}
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				dialog.dispose();
			}
		});
		return success;
	}

	private static boolean executeUpdateScript(File rmHome, InputStream in, boolean gui) throws IOException {
		try {
			Set<String> toDelete = new HashSet<String>();
			BufferedReader updateReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			String line;
			while ((line = updateReader.readLine()) != null) {
				String[] split = line.split(" ", 2);
				if (split.length != 2) {
					LOGGER.warning("Ignoring unparseable update script entry: " + line);
				}
				if ("DELETE".equals(split[0])) {
					toDelete.add(split[1].trim());
				} else {
					LOGGER.warning("Ignoring unparseable update script entry: " + line);
				}
			}
			for (String string : toDelete) {
				File file = new File(rmHome, string);
				LOGGER.info("DELETE " + file);
				file.delete();
			}
			return true;
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Cannot read update script: " + e, e);
			if (gui) {
				JOptionPane.showMessageDialog(dialog, "Cannot read update script: " + e, "Update Failed", JOptionPane.ERROR_MESSAGE);
			}
			return false;
		} finally {
			try {
				in.close();
			} catch (IOException e) {}
		}
	}

	/** Unzips the given file, applying the update script defined in META-INF/UPDATE. */
	private static boolean updateDiffZip(File rmHome, File updateZip, boolean gui) {
		//try {
		LOGGER.info("Updating using update file " + updateZip);
		ZipFile zip = null;
		try {
			zip = new ZipFile(updateZip);
		} catch (Exception e1) {
			LOGGER.log(Level.SEVERE, "Update file corrupt: " + e1, e1);
			if (gui) {
				JOptionPane.showMessageDialog(dialog, "Update file corrupt: " + e1, "Update Failed", JOptionPane.ERROR_MESSAGE);
			}
			return false;
		} finally {
			if (zip != null) {
				try {
					zip.close();
				} catch (IOException e) {}
			}
		}

		final int size = zip.size();
		Enumeration<? extends ZipEntry> enumeration = zip.entries();
		int i = 0;
		while (enumeration.hasMoreElements()) {
			i++;
			ZipEntry entry = enumeration.nextElement();
			if (entry.isDirectory()) {
				continue;
			}
			String name = entry.getName();
			if (!"META-INF/UPDATE".equals(name)) {
				if (name.startsWith("rapidminer/")) {
					name = name.substring("rapidminer/".length());
				}
				File dest = new File(rmHome, name);
				try {
					LOGGER.info("UPDATE " + dest);
					File parent = dest.getParentFile();
					if ((parent != null) && !parent.exists()) {
						parent.mkdirs();
					}
					final int fi = i;
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							bar.setValue(fi * 1000 / size);
						}
					});
					byte[] buf = new byte[10 * 1024];
					int length;
					InputStream in = zip.getInputStream(entry);
					OutputStream out = new FileOutputStream(dest);
					while ((length = in.read(buf)) != -1) {
						out.write(buf, 0, length);
					}
					out.close();
					in.close();
				} catch (Exception e2) {
					LOGGER.log(Level.WARNING, "Updating " + dest + " failed: " + e2, e2);
					if (gui) {
						JOptionPane.showMessageDialog(dialog, "Updating " + dest + " failed: " + e2, "Update Failed", JOptionPane.ERROR_MESSAGE);
					}
					return false;
				}

			}
		}

		ZipEntry updateEntry = zip.getEntry("META-INF/UPDATE");
		if (updateEntry != null) {
			try {
				executeUpdateScript(rmHome, zip.getInputStream(updateEntry), gui);
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Cannot read update script: " + e, e);
				if (gui) {
					JOptionPane.showMessageDialog(dialog, "Cannot read update script: " + e, "Update Failed", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		try {
			zip.close();
		} catch (IOException e1) {
			LOGGER.log(Level.WARNING, "Cannot close update file: " + enumeration, enumeration);
		}
		try {
			if (updateZip.delete()) {
				return true;
			} else {
				JOptionPane.showMessageDialog(dialog, "Could not delete update file " + updateZip + ". Probably you do not have administrator permissions. Please delete this file manually.", "Update Failed", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(dialog, "Could not delete update file " + updateZip + ". Probably you do not have administrator permissions. Please delete this file manually.", "Update Failed", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	public static void main(String[] args) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException,
			IllegalAccessException, InvocationTargetException, ZipException, IOException {
		ensureRapidMinerHomeSet();
		LOGGER.info("Launching RapidMiner, platform " + getPlatform());
		String rapidMinerHomeProperty = System.getProperty(PROPERTY_RAPIDMINER_HOME);
		if (rapidMinerHomeProperty == null) {
			LOGGER.info("RapidMiner HOME is not set. Ignoring potential update installation. (If that happens, you weren't able to download updates anyway.)");
		} else {
			File rmHome = new File(rapidMinerHomeProperty);
			File updateDir = new File(rmHome, "update");

			File updateScript = new File(updateDir, "UPDATE");
			if (!updateScript.exists()) {
				updateScript = null;
			}
			File[] updates = updateDir.listFiles(new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					return pathname.getName().startsWith("rmupdate");
				}
			});
			File updateZip = null;
			if (updates != null) {
				switch (updates.length) {
					case 0:
						break;
					case 1:
						updateZip = updates[0];
						break;
					default:
						LOGGER.warning("Multiple updates found: " + Arrays.toString(updates) + ". Ignoring all.");
				}
			}
			if ((updateZip != null) || (updateScript != null)) {
				if (updateGUI(rmHome, updateZip, updateScript)) {
					relaunch();
					return; // not reached
				}
			}
		}
		Class<?> rapidMinerClass = Class.forName("com.rapidminer.gui.RapidMinerGUI");
		Method main = rapidMinerClass.getMethod("main", String[].class);
		main.invoke(null, new Object[] { args });
	}

	public static void relaunch() {
		LOGGER.info("RapidMiner will be restarted...");
		System.exit(RELAUNCH_EXIT_CODE);
	}

	public static String getPlatform() {
		String version = Launcher.class.getPackage().getImplementationVersion();
		if (version == null) {
			//return "ANY";
			return null;
		} else {
			return version.split("-")[1];
		}

	}

	public static String getLongVersion() {
		String version = Launcher.class.getPackage().getImplementationVersion();
		if (version == null) {
			LOGGER.info("Implementation version not set.");
			return "?.?.?";
		} else {
			return version.split("-")[0];
		}
	}

	public static String getShortVersion() {
		String version = getLongVersion();
		int lastDot = version.lastIndexOf('.');
		if (lastDot != -1) {
			return version.substring(0, lastDot);
		} else {
			// does not happen
			return version;
		}
	}

	public static boolean isDevelopmentBuild() {
		String platform = getPlatform();
		return (platform == null) || "${platform}".equals(platform);
	}

}
