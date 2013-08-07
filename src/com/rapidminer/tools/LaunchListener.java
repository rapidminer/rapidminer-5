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
package com.rapidminer.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rapidminer.RapidMiner;
import com.rapidminer.io.process.XMLTools;

/** When started for the first time, listens on a given socket.
 *  If started for the second time, contacts this socket and
 *  passes command line options to this socket.
 *  The port number on which we listen is stored in a file in the users
 *  home directory.
 * 
 *  In order to use this class, first try to contact another instance by calling
 *  {@link #sendToOtherInstanceIfUp(String...)}. If true is returned, commands were
 *  sent to the other instance and we can terminate. If false is returned, the other
 *  instance is not running. In that case, call {@link #installListener(RemoteControlHandler)}.
 *  Now, when another instance is started, callbacks are made to the {@link RemoteControlHandler}
 *  passed. Precisely this is done when calling {@link #defaultLaunchWithArguments(String[], RemoteControlHandler)}.
 * 
 * @author Simon Fischer
 *
 */
public class LaunchListener {

    /** Callbacks will be made to this interface when another client contacts us. */
    public static interface RemoteControlHandler {

        /** Callback method called when another client starts. */
        boolean handleArguments(String[] args);

    }

    private static final String FAILED = "<failed/>";

    private static final String UNKNOWN_COMMAND = "<unknown-command/>";

    private static final String REJECTED = "<rejected/>";

    private static final String OK = "<ok/>";

    private static final String HELLO_MESSAGE = "<hi>I am RapidMiner. I understand a bit of XML.</hi>";

    private static final Logger LOGGER = Logger.getLogger(LaunchListener.class.getName());

    private static final LaunchListener INSTANCE = new LaunchListener();

    private RemoteControlHandler handler;

    private LaunchListener() {
    }

    private File getSocketFile() {
        return FileSystemService.getUserConfigFile("socket");
    }

    public static LaunchListener getInstance() {
        return INSTANCE;
    }

    private void installListener(final RemoteControlHandler handler) throws IOException {
        // port 0 = let system assign port
        // backlog 1 = we don't expect simultaneous requests
        final ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLocalHost());
        final int port = serverSocket.getLocalPort();
        final File socketFile = getSocketFile();
        LOGGER.info("Listening for other instances on port "+port+". Writing "+socketFile+".");
        PrintStream socketOut = new PrintStream(socketFile);
        socketOut.println(""+port);
        socketOut.close();
        RapidMiner.addShutdownHook(new Runnable() {
            @Override
            public void run() {
                LOGGER.config("Deleting "+socketFile);
                socketFile.delete();
            }
        });

        Thread listenerThread = new Thread("Launch-Listener") {
            @Override
            public void run() {
                LaunchListener.this.handler = handler;
                while (true) {
                    Socket client;
                    try {
                        client = serverSocket.accept();
                        // We don't spawn another thread here.
                        // Assume no malicious client and communication is quick.
                        talkToSecondClient(client);
                    } catch (IOException e) {
                        //LogService.getRoot().log(Level.WARNING, "Error accepting socket connection: "+e, e);
            			LogService.getRoot().log(Level.WARNING,
            					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
            					"com.rapidminer.tools.LaunchListener.accepting_socket_connection_error", 
            					e),
            					e);
                    }
                }
            }
        };
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void talkToSecondClient(Socket client) {
        try {
            LOGGER.info("Second client launched.");
            PrintStream out = new PrintStream(client.getOutputStream());
            out.println(HELLO_MESSAGE);
            Document doc = XMLTools.parse(client.getInputStream());
            LOGGER.config("Read XML document from other client: ");
            final String command = doc.getDocumentElement().getTagName();
            if ("args".equals(command)) {
                NodeList argsElems = doc.getDocumentElement().getElementsByTagName("arg");
                List<String> args = new LinkedList<String>();
                for (int i = 0; i < argsElems.getLength(); i++) {
                    args.add(argsElems.item(i).getTextContent());
                }
                if (handler != null) {
                    LOGGER.config("Handling <args> command from other client.");
                    try {
                        if (handler.handleArguments(args.toArray(new String[args.size()]))) {
                            out.println(OK);
                        } else {
                            out.println(REJECTED);
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error executing remote control command: "+e, e);
                        out.println(FAILED);
                    }
                } else {
                    LOGGER.warning("Other client sent <args> command, but I don't have a handler installed.");
                    out.println(FAILED);
                }
            } else {
                out.println(UNKNOWN_COMMAND);
                LOGGER.warning("Unknown command from second client: <"+command+">.");
            }
            //			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            //			String line;
            //			while ((line = in.readLine()) != null) {
            //				LOGGER.info("Other client says: "+line);
            //				out.println("You said: "+line);
            //			}
            client.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to talk to client: "+e, e);
        } catch (SAXException e) {
            LOGGER.log(Level.WARNING, "I don't understand what the other client is trying to say: "+e, e);
        }
    }

    private Socket getOtherInstance() {
        File socketFile = getSocketFile();
        if (!socketFile.exists()) {
            LOGGER.config("Socket file "+socketFile+" does not exist. Assuming I am the first instance.");
            return null;
        }
        int port;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(socketFile));
            String portStr = in.readLine();
            port = Integer.parseInt(portStr);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to read socket file '"+socketFile+"': "+ e, e);
            return null;
        } finally {
            try {
                in.close();
            } catch (IOException e) { }
        }
        LOGGER.config("Checking for running instance on port "+port+".");
        try {
            return new Socket("localhost", port);
        } catch (UnknownHostException e) {
            LOGGER.config("Name localhost cannot be resolved. Assuming we are the first instance.");
            return null;
        } catch (IOException e) {
            LOGGER.config("Got exception "+e+". Assuming we are the first instance.");
            return null;
        }
    }

    //	private boolean isOtherInstanceUp() {
    //		final Socket other = getOtherInstance();
    //		if (other != null) {
    //			boolean isRM;
    //			try {
    //				BufferedReader in = new BufferedReader(new InputStreamReader(other.getInputStream()));
    //				isRM = readHelloMessage(in);
    //				other.close();
    //			} catch (IOException e) {
    //				LOGGER.log(Level.WARNING, "Failed to other instance: "+e, e);
    //				return false;
    //			}
    //			return isRM;
    //		} else {
    //			return false;
    //		}
    //	}

    private boolean readHelloMessage(BufferedReader in) throws IOException {
        boolean isRM;
        String line = in.readLine();
        if (HELLO_MESSAGE.equals(line)) {
            LOGGER.config("Found other RapidMiner instance.");
            isRM = true;
        } else {
            LOGGER.config("Read unknown string from other instance: "+line);
            isRM = false;
        }
        return isRM;
    }

    private boolean sendToOtherInstanceIfUp(String ... args) {
        final Socket other = getOtherInstance();
        if (other == null) {
            return false;
        }
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(other.getInputStream()));
            boolean isRM = readHelloMessage(in);
            if (!isRM) {
                return false;
            } else {
                LOGGER.config("Sending arguments to other RapidMiner instance: "+Arrays.toString(args));
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                Element root = doc.createElement("args");
                doc.appendChild(root);
                for (String arg : args) {
                    Element argElem = doc.createElement("arg");
                    argElem.setTextContent(arg);
                    root.appendChild(argElem);
                }
                XMLTools.stream(doc, other.getOutputStream(), null);
                return true;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to talk to other instance: "+e, e);
            return false;
        } catch (ParserConfigurationException e) {
            LOGGER.log(Level.WARNING, "Cannot create XML document: "+e, e);
            return false;
        } catch (XMLException e) {
            LOGGER.log(Level.WARNING, "Cannot create XML document: "+e, e);
            return false;
        } finally {
            try {
                other.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to close socket: "+e, e);
            }
        }
    }

    /** Sends the arguments to the other client, if up.
     * @return true if other client is not up, so we must continue launching our APP.
     * */
    public static boolean defaultLaunchWithArguments(String[] args, RemoteControlHandler handler) throws IOException {
        //LogService.getRoot();
        ParameterService.init();
        if (!getInstance().sendToOtherInstanceIfUp(args)) {
            getInstance().installListener(handler);
            return true;
        } else {
            return false;
        }
    }
}
