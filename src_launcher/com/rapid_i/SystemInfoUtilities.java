/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2012 by Rapid-I and the contributors
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

package com.rapid_i;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class to get system information.
 * 
 * @author Nils Woehler
 */
public class SystemInfoUtilities {

	private static final int BITS_TO_BYTES_FACTOR = 1024;
	private static Logger LOGGER = Logger.getLogger(SystemInfoUtilities.class.getSimpleName());

	public enum Arch {
		THIRTY_TWO, // 32bit
		SIXTY_FOUR, // 64 bit
	}

	public enum OperatingSystem {
		WINDOWS,
		OSX,
		UNIX,
		SOLARIS,
		OTHER,
	}

	private static OperatingSystemMXBean getOperatingSystemBean() {
		return ManagementFactory.getOperatingSystemMXBean();
	}

	public static int getNumberOfProcessors() {
		return getOperatingSystemBean().getAvailableProcessors();
	}

	public static Integer getJavaVersion() {
		return Integer.parseInt(System.getProperty("java.version").
				substring(0, 3).replace(".", ""));
	}

	public static Arch getJVMArchitecture() {
		if (getOperatingSystemBean().getArch().contains("64")) {
			return Arch.SIXTY_FOUR;
		} else {
			return Arch.THIRTY_TWO;
		}
	}

	/** 
	* Returns total physical memory in MB. 
	* If it is run in a 32-bit JVM it may return only a maximum of 4GB even if more memory is available.
	* 
	* @throws IOException if something goes wrong
	*/
	public static Long getTotalPhysicalMemorySize() throws IOException {
		OperatingSystemMXBean operatingSystemBean = getOperatingSystemBean();

		long memory = 0L;

		// if the system bean is an implementation by sun, we are almost done
		try {
			memory = ((com.sun.management.OperatingSystemMXBean)
					operatingSystemBean)
							.getTotalPhysicalMemorySize();
		} catch (Throwable t) { // fallback because sun implementation is not available
			switch (getOperatingSystem()) {
				case OSX:
					memory = readOSXTotalMemory();
					break;
				case WINDOWS:
					memory = readWindowsTotalMemory();
					break;
				case UNIX:
					memory = readUnixTotalMemory();
					break;
				case SOLARIS:
					memory = readSolarisTotalMemory();
					break;
				default:
					memory = readOtherTotalMemory();
					break;
			}
		}

		memory /= BITS_TO_BYTES_FACTOR; // kbyte
		memory /= BITS_TO_BYTES_FACTOR; // mbyte
		return memory;
	}

	private static String executeMemoryInfoProcess(String... command) throws IOException {
		ProcessBuilder procBuilder = new ProcessBuilder(command);
		Process process = procBuilder.start();

		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		try {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().isEmpty()) {
					continue;
				} else {
					return line;
				}
			}
		} catch (IOException e1) {
			throw e1;
		} finally {
			br.close();
		}
		throw new IOException("Could not read memory process output for command " + command);
	}

	/**
	 * @return total memory in bytes
	 */
	private static Long readWindowsTotalMemory() throws IOException {
		String[] command = "wmic OS get TotalVisibleMemorySize /Value".split(" ");
		String line = executeMemoryInfoProcess(command); // Output should be something like 'TotalVisibleMemorySize=8225260'
		return Long.parseLong(line.substring(line.indexOf("=") + 1)) * BITS_TO_BYTES_FACTOR; // convert it to bytes
	}

	/**
	 * @return total memory in bytes
	 */
	private static Long readOSXTotalMemory() throws IOException {
		String[] command = "sysctl -a | grep hw.memsize".split(" ");
		String line = executeMemoryInfoProcess(command);
		return Long.parseLong(line.substring(line.indexOf(":") + 2));
	}

	/**
	 * @return total memory in bytes
	 */
	private static Long readSolarisTotalMemory() throws IOException {  //TODO test if this works
		String[] command = "prtconf | grep Memory".split(" ");
		String line = executeMemoryInfoProcess(command); // output should be something like 'Memory size: 8192 Megabytes'
		line = line.substring(line.indexOf(":") + 2); // shorten output to '8192 Megabytes'
		line = line.substring(0, line.indexOf(" ")); // shorten to just '8192'
		return Long.parseLong(line) * BITS_TO_BYTES_FACTOR * BITS_TO_BYTES_FACTOR;
	}

	/**
	 * @return total memory in bytes
	 */
	private static Long readUnixTotalMemory() throws IOException {
		String[] command = "grep MemTotal /proc/meminfo".split(" ");
		String line = executeMemoryInfoProcess(command); // should output something like 'MemTotal:       12297204 kB'
		line = line.substring(line.indexOf(":") + 1).trim(); // shorten to '12297204 kB'
		line = line.substring(0, line.indexOf(" ")); // shorten to just '12297204'
		return Long.parseLong(line) * BITS_TO_BYTES_FACTOR;
	}

	/**
	 * @return total memory in bytes
	 */
	private static Long readOtherTotalMemory() throws IOException {
		throw new IOException("Not yet implemented"); //TODO implement
	}

	public static String getOperatingSystemName() {
		return getOperatingSystemBean().getName();
	}

	public static String getOperatingSystemVersion() {
		return getOperatingSystemBean().getVersion();
	}

	public static OperatingSystem getOperatingSystem() {
		String systemName = getOperatingSystemName().toLowerCase();
		if (isWindows(systemName)) {
			return OperatingSystem.WINDOWS;
		} else if (isMac(systemName)) {
			return OperatingSystem.OSX;
		} else if (isUnix(systemName)) {
			return OperatingSystem.UNIX;
		} else if (isSolaris(systemName)) {
			return OperatingSystem.SOLARIS;
		} else {
			return OperatingSystem.OTHER;
		}
	}

	private static boolean isWindows(String osName) {
		return (osName.indexOf("win") >= 0);
	}

	private static boolean isMac(String osName) {
		return (osName.indexOf("mac") >= 0);
	}

	private static boolean isUnix(String osName) {
		return (osName.indexOf("nix") >= 0 || osName.indexOf("nux") >= 0 || osName.indexOf("aix") > 0);
	}

	private static boolean isSolaris(String osName) {
		return (osName.indexOf("sunos") >= 0);
	}

	public static void logEnvironmentInfos() {
		LOGGER.log(Level.INFO, "Operating system: " + getOperatingSystemName() + ", Version: " + getOperatingSystemVersion());
		LOGGER.log(Level.INFO, "Number of logical processors: " + getNumberOfProcessors());
		LOGGER.log(Level.INFO, "Java version: " + getJavaVersion());
		LOGGER.log(Level.INFO, "JVM Architecture: " + getJVMArchitecture());
		try {
			LOGGER.log(Level.INFO, "Maxmimum physical memory available for JVM: " + getTotalPhysicalMemorySize() + "mb");
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Could not detect total physical memory.");
		}
	}

	/**
	* @param args
	*/
	public static void main(String[] args) {
		logEnvironmentInfos();
	}

}
