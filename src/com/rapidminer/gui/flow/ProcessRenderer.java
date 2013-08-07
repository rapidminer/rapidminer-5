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
package com.rapidminer.gui.flow;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TooManyListenersException;
import java.util.WeakHashMap;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.EventListenerList;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.rapidminer.BreakpointListener;
import com.rapidminer.Process;
import com.rapidminer.ProcessListener;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.ConnectPortToRepositoryAction;
import com.rapidminer.gui.actions.StoreInRepositoryAction;
import com.rapidminer.gui.dnd.AbstractPatchedTransferHandler;
import com.rapidminer.gui.dnd.DragListener;
import com.rapidminer.gui.dnd.OperatorTransferHandler;
import com.rapidminer.gui.dnd.ReceivingOperatorTransferHandler;
import com.rapidminer.gui.dnd.TransferableOperator;
import com.rapidminer.gui.metadata.MetaDataRendererFactoryRegistry;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.tools.PrintingTools;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceMenu;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.ToolTipWindow;
import com.rapidminer.gui.tools.components.ToolTipWindow.TipProvider;
import com.rapidminer.io.process.ProcessXMLFilter;
import com.rapidminer.io.process.ProcessXMLFilterRegistry;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ResultObject;
import com.rapidminer.operator.io.RepositorySource;
import com.rapidminer.operator.learner.tree.DecisionTreeLearner;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.OutputPorts;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.PortException;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.operator.ports.metadata.CollectionMetaData;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataError;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.ClassColorMap;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.ParentResolvingMap;
import com.rapidminer.tools.Tools;

/**
 * This class renders a process graph. It also stores all data about visualization
 * like location and size of the operators. This data is stored in a weak hash map
 * so it should be garbage collected when the operators are removed.
 * 
 * @author Simon Fischer
 */
public class ProcessRenderer extends JPanel implements DragListener {

	private class ProcessRendererDropTarget extends DropTarget {

		private static final long serialVersionUID = 1L;
		private EventListenerList dropTragetListenerList;

		public ProcessRendererDropTarget(Component c, DropTargetListener dropTargetListener) {
			super(c, TransferHandler.COPY_OR_MOVE | TransferHandler.LINK, null);
			try {
				super.addDropTargetListener(dropTargetListener);
			} catch (TooManyListenersException tmle) {}
		}

		public void addDropTargetListener(DropTargetListener dtl) throws TooManyListenersException {
			if (dropTragetListenerList == null) {
				dropTragetListenerList = new EventListenerList();
			}
			dropTragetListenerList.add(DropTargetListener.class, dtl);
		}

		public void removeDropTargetListener(DropTargetListener dtl) {
			if (dropTragetListenerList != null) {
				dropTragetListenerList.remove(DropTargetListener.class, dtl);
			}
		}

		public void dragEnter(DropTargetDragEvent e) {
			super.dragEnter(e);
			if (dropTragetListenerList != null) {
				Object[] listeners = dropTragetListenerList.getListenerList();
				for (int i = listeners.length - 2; i >= 0; i -= 2) {
					if (listeners[i] == DropTargetListener.class) {
						((DropTargetListener) listeners[i + 1]).dragEnter(e);
					}
				}
			}
		}

		public void dragOver(DropTargetDragEvent e) {
			super.dragOver(e);
			if (dropTragetListenerList != null) {
				Object[] listeners = dropTragetListenerList.getListenerList();
				for (int i = listeners.length - 2; i >= 0; i -= 2) {
					if (listeners[i] == DropTargetListener.class) {
						((DropTargetListener) listeners[i + 1]).dragOver(e);
					}
				}
			}
		}

		public void dragExit(DropTargetEvent e) {
			super.dragExit(e);
			setImportDragged(false);
			if (dropTragetListenerList != null) {
				Object[] listeners = dropTragetListenerList.getListenerList();
				for (int i = listeners.length - 2; i >= 0; i -= 2) {
					if (listeners[i] == DropTargetListener.class) {
						((DropTargetListener) listeners[i + 1]).dragExit(e);
					}
				}
			}
		}

		public void drop(DropTargetDropEvent e) {
			super.drop(e);
			setImportDragged(false);
			if (dropTragetListenerList != null) {
				Object[] listeners = dropTragetListenerList.getListenerList();
				for (int i = listeners.length - 2; i >= 0; i -= 2) {
					if (listeners[i] == DropTargetListener.class) {
						((DropTargetListener) listeners[i + 1]).drop(e);
					}
				}
			}
		}

		public void dropActionChanged(DropTargetDragEvent e) {
			super.dropActionChanged(e);
			if (dropTragetListenerList != null) {
				Object[] listeners = dropTragetListenerList.getListenerList();
				for (int i = listeners.length - 2; i >= 0; i -= 2) {
					if (listeners[i] == DropTargetListener.class) {
						((DropTargetListener) listeners[i + 1]).dropActionChanged(e);
					}
				}
			}
		}
	}

	private enum Orientation {
		X_AXIS,
		Y_AXIS
	}

	private static Orientation ORIENTATION = Orientation.X_AXIS;

	private static ParentResolvingMap<Class, Color> IO_CLASS_TO_COLOR_MAP = new ClassColorMap();

	static {
		try {
			IO_CLASS_TO_COLOR_MAP.parseProperties("com/rapidminer/resources/groups.properties", "io.", ".color", OperatorDescription.class.getClassLoader());
		} catch (IOException e) {
			//LogService.getRoot().warning("Cannot load operator group colors.");
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.flow.ProcessRenderer.loading_operator_group_colors_error");
		}
	}

	/**
	 * This method adds the colors of the given property file to the global group colors
	 */
	public static void registerAdditionalGroupColors(String groupProperties, String pluginName, ClassLoader classLoader) {
		SwingTools.registerAdditionalGroupColors(groupProperties, pluginName, classLoader);
	}

	/**
	 * This method adds the colors of the given property file to the io object colors
	 */
	public static void registerAdditionalObjectColors(String groupProperties, String pluginName, ClassLoader classLoader) {
		try {
			IO_CLASS_TO_COLOR_MAP.parseProperties(groupProperties, "io.", ".color", classLoader);
		} catch (IOException e) {
			//LogService.getRoot().warning("Cannot load io object colors for plugin " + pluginName + ".");
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.flow.ProcessRenderer.loading_io_object_colors_error", pluginName);
		}
	}

	private final transient TipProvider tipProvider = new TipProvider() {

		@Override
		public Object getIdUnder(Point point) {
			if (connectingPortSource == null) {
				return hoveringPort;
			} else {
				return null;
			}
		}

		@Override
		public String getTip(Object o) {
			Port port = (Port) o;
			StringBuilder tip = new StringBuilder();

			if (displayedChain instanceof ProcessRootOperator) {
				if (port.getPorts() == displayedChain.getSubprocess(0).getInnerSources()) {
					int index = displayedChain.getSubprocess(0).getInnerSources().getAllPorts().indexOf(port);
					List<String> locations = displayedChain.getProcess().getContext().getInputRepositoryLocations();
					if (index >= 0 && index < locations.size()) {
						String dest = locations.get(index);
						tip.append("Loaded from: ").append(dest).append("<br/>");
					}
				} else if (port.getPorts() == displayedChain.getSubprocess(0).getInnerSinks()) {
					int index = displayedChain.getSubprocess(0).getInnerSinks().getAllPorts().indexOf(port);
					List<String> locations = displayedChain.getProcess().getContext().getOutputRepositoryLocations();
					if (index >= 0 && index < locations.size()) {
						String dest = locations.get(index);
						tip.append("Stored at: ").append(dest).append("<br/>");
					}
				}
			}

			tip.append("<strong>");
			tip.append(port.getSpec());
			tip.append("</strong> (");
			tip.append(port.getName());
			tip.append(")<br/>");
			tip.append("<em>Meta data:</em> ");
			MetaData metaData = port.getMetaData();
			if (metaData != null) {
				if (metaData instanceof ExampleSetMetaData) {
					tip.append(((ExampleSetMetaData) metaData).getShortDescription());
				} else {
					tip.append(metaData.getDescription());
				}
				tip.append("<br/><em>Generated by:</em> ");
				tip.append(metaData.getGenerationHistoryAsHTML());
				tip.append("<br>");
			} else {
				tip.append("-<br/>");
			}
			IOObject data = port.getAnyDataOrNull();
			if (data != null) {
				tip.append("</br><em>Data:</em> ");
				tip.append(data.toString());
				tip.append("<br/>");
			}
			tip.append(port.getDescription());
			if (!port.getErrors().isEmpty()) {
				boolean hasErrors = false;
				boolean hasWarnings = false;
				for (MetaDataError error : port.getErrors()) {
					if (error.getSeverity() == Severity.ERROR)
						hasErrors = true;
					if (error.getSeverity() == Severity.WARNING)
						hasWarnings = true;
				}
				if (hasErrors) {
					tip.append("<br/><strong style=\"color:red\">");
					tip.append(port.getErrors().size());
					tip.append(" error(s):</strong>");
					for (MetaDataError error : port.getErrors()) {
						if (error.getSeverity() == Severity.ERROR) {
							tip.append("<br/> ");
							tip.append(error.getMessage());
						}
					}
				}
				if (hasWarnings) {
					tip.append("<br/><strong style=\"color:#FFA500\">");
					tip.append(port.getErrors().size());
					tip.append(" warnings(s):</strong>");
					for (MetaDataError error : port.getErrors()) {
						if (error.getSeverity() == Severity.WARNING) {
							tip.append("<br/> ");
							tip.append(error.getMessage());
						}
					}
				}
			}
			return tip.toString();
		}

		@Override
		public Component getCustomComponent(Object o) {
			Port hoveringPort = (Port) o;
			MetaData metaData = hoveringPort.getMetaData();
			return MetaDataRendererFactoryRegistry.getInstance().createRenderer(metaData);
//            if (metaData != null && metaData instanceof ExampleSetMetaData) {
//            	//ExampleSetMetaDataTableModel.makeTableForToolTip((ExampleSetMetaData) metaData);
//                return MetaDataRendererFactoryRegistry.getInstance().createRenderer(metaData);
//            } else {
//                return null;
//            }
		}
	};

	public ResourceAction RENAME_ACTION = new ResourceAction("rename_in_processrenderer") {

		{
			setCondition(OPERATOR_SELECTED, MANDATORY);
		}
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			if (!getSelection().isEmpty()) {
				rename(getSelection().get(0));
			}
		}
	};

	public ResourceAction SELECT_ALL_ACTION = new ResourceAction("select_all") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			selectedOperators.clear();
			for (ExecutionUnit unit : processes) {
				selectedOperators.addAll(unit.getOperators());
			}
			mainFrame.selectOperators(selectedOperators);
			repaint();
		}
	};

	public Action ARRANGE_OPERATORS_ACTION = new ResourceAction(true, "arrange_operators") {

		private static final long serialVersionUID = 4636292007315749350L;

		@Override
		public void actionPerformed(ActionEvent e) {
			for (ExecutionUnit u : processes) {
				autoArrange(u);
			}
		}
	};

	public Action AUTO_FIT_ACTION = new ResourceAction(true, "auto_fit") {

		private static final long serialVersionUID = 3932329413268066576L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			autoFit();
		}
	};

	private final ResourceAction DELETE_SELECTED_CONNECTION = new ResourceAction("delete_selected_connection") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			if (selectedConnectionSource != null) {
				if (selectedConnectionSource.isConnected()) {
					selectedConnectionSource.disconnect();
					repaint();
				}
			} else {
				// don't delete if we have selected surrounding operator in subprocess so the whole subprocess would get deleted
				for (Operator selectedOperator : getSelection()) {
					if (selectedOperator.equals(getDisplayedChain())) {
						return;
					}
				}
				mainFrame.getActions().DELETE_OPERATOR_ACTION.actionPerformed(e);
				setHoveringOperator(null);
				updateCursor();
			}
		}
	};

	private class ChangeSizeAction extends ResourceAction {

		private static final long serialVersionUID = 1L;
		private final int dx, dy;

		private ChangeSizeAction(String key, int dx, int dy) {
			super("processrenderer." + key);
			this.dx = dx;
			this.dy = dy;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (hoveringProcessIndex == -1) {
				return;
			}
			ExecutionUnit unit = processes[hoveringProcessIndex];
			changeProcessSize(unit, dx, dy);
		}
	}

	public final Action INCREASE_PROCESS_LAYOUT_WIDTH_ACTION = new ChangeSizeAction("increase_width", +GRID_WIDTH, 0);
	public final Action DECREASE_PROCESS_LAYOUT_WIDTH_ACTION = new ChangeSizeAction("decrease_width", -GRID_WIDTH, 0);
	public final Action INCREASE_PROCESS_LAYOUT_HEIGHT_ACTION = new ChangeSizeAction("increase_height", 0, +GRID_HEIGHT);
	public final Action DECREASE_PROCESS_LAYOUT_HEIGHT_ACTION = new ChangeSizeAction("decrease_height", 0, -GRID_HEIGHT);

	private final InterpolationMap nameRolloutInterpolationMap = new InterpolationMap(this);

	private static JLabel DUMMY_LABEL = new JLabel();

	private static int OPERATOR_WIDTH = 5 * 16 + 2 * 5; // 5 mini icons + padding
	private static int MIN_OPERATOR_HEIGHT = 60;
	private static int PORT_SIZE = 12;
	private static int PADDING = 10;
	private static int WALL_WIDTH = 25; // 25

	private static int GRID_WIDTH = OPERATOR_WIDTH * 3 / 4;
	private static int GRID_HEIGHT = MIN_OPERATOR_HEIGHT * 3 / 4;
	private static int GRID_X_OFFSET = OPERATOR_WIDTH / 2;
	private static int GRID_Y_OFFSET = MIN_OPERATOR_HEIGHT / 2;
	private static int GRID_AUTOARRANGE_WIDTH = OPERATOR_WIDTH * 3 / 2;
	private static int GRID_AUTOARRANGE_HEIGHT = MIN_OPERATOR_HEIGHT * 3 / 2;

	public static RenderingHints HI_QUALITY_HINTS = new RenderingHints(null);
	public static RenderingHints LOW_QUALITY_HINTS = new RenderingHints(null);

	static {
		HI_QUALITY_HINTS.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		HI_QUALITY_HINTS.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		LOW_QUALITY_HINTS.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		LOW_QUALITY_HINTS.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	}

	private static ImageIcon IMAGE_WARNING = SwingTools.createIcon("16/sign_warning.png");
	private static ImageIcon IMAGE_BREAKPOINT_WITHIN = SwingTools.createIcon("16/breakpoint.png");
	private static ImageIcon IMAGE_BREAKPOINTS = SwingTools.createIcon("16/breakpoints.png");
	private static ImageIcon IMAGE_BREAKPOINT_BEFORE = SwingTools.createIcon("16/breakpoint_up.png");
	private static ImageIcon IMAGE_BREAKPOINT_AFTER = SwingTools.createIcon("16/breakpoint_down.png");
	private static ImageIcon IMAGE_BRANCH = SwingTools.createIcon("16/elements_selection.png");
	private static ImageIcon IMAGE_COMMENT = SwingTools.createIcon("16/document_text.png");

	private static ImageIcon OPERATOR_RUNNING = SwingTools.createIcon("16/bullet_triangle_glass_green.png");
	private static ImageIcon OPERATOR_READY = SwingTools.createIcon("16/bullet_ball_glass_green.png");
	private static ImageIcon OPERATOR_DIRTY = SwingTools.createIcon("16/bullet_ball_glass_yellow.png");
	private static ImageIcon OPERATOR_ERROR_ICON = SwingTools.createIcon("16/bullet_ball_glass_red.png");

	private static final long serialVersionUID = 1L;

	private static Color INNER_COLOR = Color.WHITE;
	private static Color SHADOW_COLOR = Color.LIGHT_GRAY;

	private static Color LINE_COLOR = Color.DARK_GRAY;
	private static Stroke LINE_STROKE = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static Stroke HIGHLIGHT_STROKE = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static Stroke SELECTION_RECT_STROKE = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 5f, new float[] { 2f, 2f }, 0f);
	private static Paint SELECTION_RECT_PAINT = Color.GRAY;
	private static Color PROCESS_TITLE_COLOR = SHADOW_COLOR;
	private static Paint SHADOW_TOP_GRADIENT = new GradientPaint(0, 0, SHADOW_COLOR, PADDING, 0, Color.WHITE);
	private static Paint SHADOW_LEFT_GRADIENT = new GradientPaint(0, 0, SHADOW_COLOR, 0, PADDING, Color.WHITE);

	public static final Color INNER_DRAG_COLOR = RapidMinerGUI.getBodyHighlightColor();
	public static Color LINE_DRAG_COLOR = RapidMinerGUI.getBorderHighlightColor();
	private static Stroke LINE_DRAG_STROKE = new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	private static Paint SHADOW_TOP_DRAG_GRADIENT = new GradientPaint(0, 0, SHADOW_COLOR, PADDING, 0, INNER_DRAG_COLOR);
	private static Paint SHADOW_LEFT_DRAG_GRADIENT = new GradientPaint(0, 0, SHADOW_COLOR, PADDING, 0, INNER_DRAG_COLOR);

	private static Stroke CONNECTION_LINE_STROKE = new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static Stroke CONNECTION_HIGHLIGHT_STROKE = new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static Stroke CONNECTION_COLLECTION_LINE_STROKE = new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static Stroke CONNECTION_COLLECTION_HIGHLIGHT_STROKE = new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	private static Font OPERATOR_FONT = new Font("Dialog", Font.BOLD, 11);

	protected static final int HEADER_HEIGHT = OPERATOR_FONT.getSize() + 7;
	private static final Font PROCESS_FONT = new Font("Dialog", Font.BOLD, 12);
	private static final Font PORT_FONT = new Font("Dialog", Font.PLAIN, 9);
	private static final Color PORT_NAME_COLOR = Color.DARK_GRAY;
	private static final Color PORT_NAME_SELECTION_COLOR = Color.GRAY;
	private static final Color ACTIVE_EDGE_COLOR = SwingTools.RAPID_I_ORANGE;

	private static final Stroke FRAME_STROKE_SELECTED = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke FRAME_STROKE_NORMAL = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	private static final Color FRAME_COLOR_SELECTED = SwingTools.RAPID_I_ORANGE;
	private static final Color FRAME_COLOR_NORMAL = LINE_COLOR;

	private static final int ORIGINAL_TUTORIAL_COMIC_WIDTH = 657;
	private static final int ORIGINAL_TUTORIAL_COMIC_HEIGHT = 464;

	/** The widths of the individual subprocesses. */
	private transient final Map<ExecutionUnit, Dimension> processSizes = new WeakHashMap<ExecutionUnit, Dimension>();

	/** Distances between ports. */
	private transient final Map<Port, Double> portSpacings = new WeakHashMap<Port, Double>();

	/** The displayed processes. */
	private ExecutionUnit[] processes = new ExecutionUnit[0];

	/** Maps operators to their positions (in process coordinate space). */
	private transient final Map<Operator, Rectangle2D> operatorRects = new WeakHashMap<Operator, Rectangle2D>();

	private static int PORT_OFFSET = OPERATOR_FONT.getSize() + 6 + PORT_SIZE;

	private Point currentMousePosition = null;
	private Point mousePositionAtDragStart = null;
	private Point mousePositionAtLastEvaluation = null;

	private boolean hasDragged = false;

	private int pressedMouseButton = 0;

	private boolean connectionDraggingCanceled = false;

	private Rectangle2D selectionRectangle = null;

	private Map<Operator, Rectangle2D> draggedOperatorsOrigins;

	/** Port currently being dragged (within this component only!) */
	private Port draggedPort = null;

	/** Operator selected by a click. */
	private final LinkedList<Operator> selectedOperators = new LinkedList<Operator>();

	/** Operator under the mouse cursor. */
	private Operator hoveringOperator = null;

	/** Port under the mouse cursor. */
	private Port hoveringPort = null;

	/** Source port of the connection currently being created. */
	private Port connectingPortSource = null;

	/** Index of the process under the mouse. */
	private int hoveringProcessIndex = -1;

	/** Operator after which the dropped operator will be added. */
	private Operator dropInsertionPredecessor;

	/** Source port of the connector hit by the drop cursor. */
	private OutputPort hoveringConnectionSource;

	/**
	 * Source port of the connection selected by clicking on it.
	 * 
	 * @see #hoveringConnectionSource
	 */
	private OutputPort selectedConnectionSource;

	private final ProcessPanel processPanel;
	private final MainFrame mainFrame;

	/** Will be <code>true</code> if an operator is dragged from the operator tree or if a repository entry is dragged. */
	private boolean dragStarted = false;

	/** Will be <code>true</code> if canImport of transfer handler has returned <code>true</code>. Will be set to false if mouse has exited the process renderer */
	private boolean importDragged = false;

	/** Indicates if the droptarget could be set. If this is not the case, importDragged will be ignored */
	private boolean dropTargetSet = false;

	private Point mousePositionRelativeToProcess = null;

	private final List<ExtensionButton> subprocessExtensionButtons = new LinkedList<ExtensionButton>();

	private OperatorChain displayedChain;

	private final ReceivingOperatorTransferHandler transferHandler;

	private final FlowVisualizer flowVisualizer = new FlowVisualizer(this);

	private List<ProcessInteractionListener> processInteractionListeners = new LinkedList<ProcessInteractionListener>();

	private BufferedImage[] tutorialComicPanels;

	private BufferedImage tutorialComicPanelToDraw = null;

	public ProcessRenderer(ProcessPanel processPanel, MainFrame mainFrame) {
		new PanningManager(this);

		try {
			tutorialComicPanels = new BufferedImage[7];
			for (int i = 0; i < 5; i++) {
				tutorialComicPanels[i] = ImageIO.read(Tools.getResource("images/tutorial-comic-" + Integer.toString(i) + ".png"));
			}
			tutorialComicPanels[5] = ImageIO.read(Tools.getResource("images/tutorial-comic-" + Integer.toString(1) + "-drop.png"));
			tutorialComicPanels[6] = ImageIO.read(Tools.getResource("images/tutorial-comic-" + Integer.toString(2) + "-drop.png"));
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "Can not load image", e);
		}
		mainFrame.addProcessEditor(new ProcessEditor() {

			private ProcessListener tutorialProcessListener = new ProcessListener() {

				@Override
				public void processStarts(Process process) {}

				@Override
				public void processStartedOperator(Process process, Operator op) {}

				@Override
				public void processFinishedOperator(Process process, Operator op) {}

				@Override
				public void processEnded(Process process) {
					tutorialComicPanelToDraw = null;
				}
			};

			@Override
			public void setSelection(List<Operator> selection) {}

			@Override
			public void processUpdated(Process process) {
				if ((displayedChain instanceof ProcessRootOperator)) {
					int numberOfOperator = displayedChain.getSubprocess(0).getNumberOfOperators();
					tutorialComicPanelToDraw = null;
					RepositorySource golfOperator = null;
					DecisionTreeLearner decisionTreeOperator = null;
					process.getRootOperator().removeProcessListener(tutorialProcessListener);
					switch (numberOfOperator) {
						case 0:
							tutorialComicPanelToDraw = tutorialComicPanels[1];
							break;
						case 1:
							golfOperator = getRetrieveGolfOperator(displayedChain.getSubprocess(0).getOperators());
							if (golfOperator != null) {
								tutorialComicPanelToDraw = tutorialComicPanels[2];
							}
							break;
						case 2:
							golfOperator = getRetrieveGolfOperator(displayedChain.getSubprocess(0).getOperators());
							decisionTreeOperator = getDecisionTreeOperator(displayedChain.getSubprocess(0).getOperators());
							if (golfOperator == null || decisionTreeOperator == null) {
								break;
							}
							if (areCorrectlyConnected(golfOperator, decisionTreeOperator)) {
								tutorialComicPanelToDraw = tutorialComicPanels[4];
								process.getRootOperator().addProcessListener(tutorialProcessListener);
							} else {
								tutorialComicPanelToDraw = tutorialComicPanels[3];
							}

							break;
						default:
							break;
					}
				}
			}

			private boolean areCorrectlyConnected(RepositorySource golfOperator, DecisionTreeLearner decisionTreeOperator) {
				return golfOperator != null && decisionTreeOperator != null &&

						golfOperator.getOutputPorts().getNumberOfConnectedPorts() == 1 &&
						decisionTreeOperator.getInputPorts().getNumberOfConnectedPorts() == 1 &&
						decisionTreeOperator.getOutputPorts().getNumberOfConnectedPorts() == 1 &&

						golfOperator.getOutputPorts().getPortByIndex(0).getDestination().equals(decisionTreeOperator.getInputPorts().getPortByIndex(0));
			}

			private RepositorySource getRetrieveGolfOperator(List<Operator> operators) {
				for (Operator operator : operators) {
					if (operator instanceof RepositorySource) {
						RepositorySource specificOperator = (RepositorySource) operator;
						try {
							if (specificOperator.getParameter(RepositorySource.PARAMETER_REPOSITORY_ENTRY).equals("//Samples/data/Golf")) {
								return specificOperator;
							}
						} catch (UndefinedParameterError e) {
							continue;
						}
					}
				}
				return null;
			}

			private DecisionTreeLearner getDecisionTreeOperator(List<Operator> operators) {
				for (Operator operator : operators) {
					if (operator instanceof DecisionTreeLearner) {
						return (DecisionTreeLearner) operator;
					}
				}
				return null;
			}

			@Override
			public void processChanged(Process process) {}
		});
		ProcessXMLFilterRegistry.registerFilter(new GUIProcessXMLFilter());
		this.mainFrame = mainFrame;
		this.processPanel = processPanel;
		setLayout(null); // for absolute positioning of tipPane

		if (processPanel != null) {
			processPanel.addComponentListener(new ComponentAdapter() {

				@Override
				public void componentResized(ComponentEvent e) {
					super.componentResized(e);
					autoFit();
				}
			});
		}

		transferHandler = new ReceivingOperatorTransferHandler() {

			private static final long serialVersionUID = 7526109471182298215L;

			@Override
			public boolean dropNow(final List<Operator> newOperators, Point loc) {
				setImportDragged(false);
				ProcessRenderer.this.mainFrame.getStatusBar().clearSpecialText();
				if (newOperators.isEmpty()) {
					return true;
				}
				requestFocusInWindow();

				List<Operator> selection = getSelection();
				// if we don't have a loc, we can use the mouse cursor
				if (loc == null) {
					loc = currentMousePosition;
				}

				// determine process to drop to
				int processIndex;
				if (loc != null) {
					processIndex = getProcessIndexUnder(loc.getLocation());
				} else {
					if (selection != null && !selection.isEmpty()) {
						processIndex = Arrays.asList(processes).indexOf(selection.get(0).getExecutionUnit());
						if (processIndex == -1) {
							processIndex = 0;
						}
					} else {
						processIndex = 0;
					}
				}

				try {
					if (processIndex != -1) {
						if (loc != null) {
							// we have a location for the drop/paste
							Operator firstOperator = newOperators.get(0);
							Point dest = toProcessSpace(loc, processIndex);

							// if we drop a single Retrieve operator on an inner source of the root op,
							// we immediately attach the repository location to the port.
							boolean isRoot = displayedChain instanceof ProcessRootOperator;
							boolean dropsSource = firstOperator instanceof RepositorySource;
							if (isRoot && dropsSource && newOperators.size() == 1) {
								if (checkPortUnder(processes[processIndex].getInnerSources(), (int) dest.getX(), (int) dest.getY())) {
									String location = firstOperator.getParameters().getParameterOrNull(RepositorySource.PARAMETER_REPOSITORY_ENTRY);
									int index = hoveringPort.getPorts().getAllPorts().indexOf(hoveringPort);
									displayedChain.getProcess().getContext().setInputRepositoryLocation(index, location);
									return true;
								}
							}

							// calculate operator position
							Point opPosition = new Point((int) dest.getX() - OPERATOR_WIDTH / 2, (int) dest.getY() - MIN_OPERATOR_HEIGHT / 2);

							// snap to grid
							if (isSnapToGrid()) {
								opPosition = snap(opPosition);
							}

							// check if operator overlaps bottom process corner
							int lowestPosition = getHeight() - GRID_AUTOARRANGE_HEIGHT;
							if (opPosition.getY() > lowestPosition) {
								opPosition.setLocation(opPosition.getX(), lowestPosition);
							}

							// check if operator overlaps right process corner
							int rightestPositon = getWidth() - GRID_AUTOARRANGE_WIDTH - PADDING;
							if (opPosition.getX() > rightestPositon) {
								opPosition.setLocation(rightestPositon, opPosition.getY());
							}

							Rectangle2D.Double opPositionDouble = new Rectangle2D.Double(opPosition.getX(), opPosition.getY(), OPERATOR_WIDTH, MIN_OPERATOR_HEIGHT);

							operatorRects.put(firstOperator, opPositionDouble);

							// index at which the first operator is inserted
							int firstInsertionIndex;
							final boolean firstMustBeWired;
							// insert first operator. Possibly insert into connection
							if (hoveringConnectionSource != null &&
									canBeInsertedIntoConnection(firstOperator)) {
								int predecessorIndex = processes[processIndex].getOperators().indexOf(hoveringConnectionSource.getPorts().getOwner().getOperator());
								if (predecessorIndex != -1) {
									firstInsertionIndex = predecessorIndex + 1;
								} else {
									// can happen if dropIntersectsOutputPort is an inner source
									firstInsertionIndex = getDropInsertionIndex(processIndex);
								}
								processes[processIndex].addOperator(firstOperator, firstInsertionIndex);
								insertIntoHoveringConnection(firstOperator);
								firstMustBeWired = false;
							} else {
								firstInsertionIndex = getDropInsertionIndex(processIndex);
								processes[processIndex].addOperator(firstOperator, firstInsertionIndex);
								firstMustBeWired = true;
							}
							// insert the rest (1..n). First, insert, then wire
							for (int i = 1; i < newOperators.size(); i++) {
								Operator newOp = newOperators.get(i);
								processes[processIndex].addOperator(newOp, firstInsertionIndex + i);

								// TODO: inserting operators relative to first operator
							}
							AutoWireThread.autoWireInBackground(newOperators, firstMustBeWired);
						} else {
							// this should not happen, since we should always have a location.
							// Nevertheless, we have a fallback here in case no position for the drop is set.
							for (Operator newOp : newOperators) {
								processes[processIndex].addOperator(newOp);
							}
							AutoWireThread.autoWireInBackground(newOperators, true);
							for (Operator newOp : newOperators) {
								// auto position as a side effect
								getOperatorRect(newOp, true);
							}
						}
						boolean first = true;
						for (Operator op : newOperators) {
							ProcessRenderer.this.selectOperator(op, first);
							first = false;
						}
						dropInsertionPredecessor = null;
						return true;
					} else {
						dropInsertionPredecessor = null;
						return false;
					}
				} catch (RuntimeException e) {
					//LogService.getRoot().log(Level.WARNING, "During drop: " + e, e);
					LogService.getRoot().log(Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(),
									"com.rapidminer.gui.flow.ProcessRenderer.error_during_drop",
									e),
							e);
					throw e;
				}
			}

			@Override
			protected boolean isDropLocationOk(List<Operator> newOperators, Point loc) {
				if (!isEnabled()) {
					return false;
				}
				if (getProcessIndexUnder(loc) == -1) {
					return false;
				} else {
					for (Operator newOperator : newOperators) {
						if (newOperator instanceof OperatorChain) {
							if (displayedChain == newOperator ||
									((OperatorChain) newOperator).getAllInnerOperators().contains(displayedChain)) {
								return false;
							}
						}
					}
					return true;
				}
			}

			@Override
			protected void markDropOver(Point dropPoint) {
				int pid = ProcessRenderer.this.getProcessIndexUnder(dropPoint);
				if (pid != -1) {
					Point processSpace = toProcessSpace(dropPoint, pid);
					hoveringConnectionSource = getPortForConnectorNear(processSpace, processes[pid]);
					setDropInsertionPredecessor(getClosestLeftNeighbour(processSpace, processes[pid]));
				}
				repaint();
			}

			@Override
			protected List<Operator> getDraggedOperators() {
				return getSelection();
			}

			/**
			 * Returns the index at which an operator should be inserted. The operator
			 * is inserted after {@link #dropInsertionPredecessor} or as the last
			 * operator if {@link #dropInsertionPredecessor} is null.
			 */
			private int getDropInsertionIndex(int processIndex) {
				if (dropInsertionPredecessor == null) {
					return processes[processIndex].getOperators().size();
				} else {
					return dropInsertionPredecessor.getExecutionUnit().getOperators().indexOf(dropInsertionPredecessor) + 1;
				}
			}

			@Override
			public boolean canImport(TransferSupport ts) {
				if (ts.isDrop()) {
					int pid = ProcessRenderer.this.getProcessIndexUnder(ts.getDropLocation().getDropPoint());
					if (pid < 0) {
						return false;
					}
				}

				boolean canImport = canImportTransferable(ts.getTransferable());
				canImport &= super.canImport(ts);
				if (ts.isDrop() && dropTargetSet && !dragStarted && canImport && !getImportDragged()) {
					setImportDragged(true);
				}
				return canImport;
			}

			@Override
			protected void dropEnds() {
				setImportDragged(false);
			}

			@Override
			protected Process getProcess() {
				return displayedChain.getProcess();
			}
		};
		setTransferHandler(transferHandler);

		ProcessRendererDropTarget dropTarget;
		try {
			dropTarget = new ProcessRendererDropTarget(this, AbstractPatchedTransferHandler.getDropTargetListener());
			setDropTarget(dropTarget);
			dropTargetSet = true;
		} catch (Exception e) {
			LogService.getRoot().log(Level.INFO, "Drop target could not be loaded. Process panel will not be highlighted when dragging external data on it.");
		}

		addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_BACK_SPACE:
						if (displayedChain != null && displayedChain.getParent() != null) {
							ProcessRenderer.this.mainFrame.selectOperator(displayedChain.getParent());
						}
						e.consume();
						break;
					case KeyEvent.VK_LEFT:
					case KeyEvent.VK_RIGHT:
					case KeyEvent.VK_UP:
					case KeyEvent.VK_DOWN:
						if (e.isControlDown()) {
							changeProcessSize(e, displayedChain.getSubprocess(0));
						} else {
							selectInDirection(e);
						}
						e.consume();
						break;
					case KeyEvent.VK_ESCAPE:
						if (connectingPortSource != null) {
							cancelConnectionDragging();
						}
						break;
					case KeyEvent.VK_ENTER:
						if (!getSelection().isEmpty()) {
							Operator selected = getSelection().get(0);
							if (selected instanceof OperatorChain) {
								ProcessRenderer.this.processPanel.showOperatorChain((OperatorChain) selected);
								ProcessRenderer.this.mainFrame.addViewSwitchToUndo();
							}
						}
						e.consume();
						break;
				}
			}
		});

		((ResourceAction) mainFrame.getActions().TOGGLE_BREAKPOINT[BreakpointListener.BREAKPOINT_AFTER]).addToActionMap(this, WHEN_FOCUSED);
		((ResourceAction) mainFrame.getActions().TOGGLE_ACTIVATION_ITEM).addToActionMap(this, WHEN_FOCUSED);
		SELECT_ALL_ACTION.addToActionMap(this, WHEN_FOCUSED);

		OperatorTransferHandler.addToActionMap(this);
		DELETE_SELECTED_CONNECTION.addToActionMap(this, "delete", WHEN_FOCUSED);

		new ToolTipWindow(tipProvider, this);

		init();
	}

	@Override
	public void addNotify() {
		super.addNotify();
		// we do this here to avoid being overridden by main frame
		RENAME_ACTION.addToActionMap(this, WHEN_FOCUSED);
	}

	protected int getIndex(ExecutionUnit executionUnit) {
		for (int i = 0; i < processes.length; i++) {
			if (processes[i] == executionUnit) {
				return i;
			}
		}
		return -1;
	}

	private void init() {
		addMouseMotionListener(MOUSE_HANDLER);
		addMouseListener(MOUSE_HANDLER);
		setPreferredSize(new Dimension(1000, 440));
		setMinimumSize(new Dimension(100, 100));
		setMaximumSize(new Dimension(2000, 2000));

		// flowVisualizer.installListeners();
	}

	/** Adapts the process size according to the key event. */
	private void changeProcessSize(KeyEvent e, ExecutionUnit unit) {
		// Dimension size = processSizes.get(unit);
		switch (e.getKeyCode()) {
			case KeyEvent.VK_LEFT:
				changeProcessSize(unit, -GRID_WIDTH, 0);
				break;
			case KeyEvent.VK_RIGHT:
				changeProcessSize(unit, +GRID_WIDTH, 0);
				break;
			case KeyEvent.VK_UP:
				changeProcessSize(unit, 0, -GRID_HEIGHT);
				break;
			case KeyEvent.VK_DOWN:
				changeProcessSize(unit, 0, +GRID_HEIGHT);
				break;
		}
	}

	private void changeProcessSize(ExecutionUnit unit, int dx, int dy) {
		if (unit == null) {
			return;
		}
		Dimension size = processSizes.get(unit);
		if (dx == 0) {
			size = new Dimension((int) size.getWidth(), (int) getHeight(unit, true) + dy);
		} else {
			size = new Dimension((int) getWidth(unit, true) + dx, (int) size.getHeight());
		}
		processSizes.put(unit, size);
		balance();
		updateComponentSize();
	}

	/** Starting from the current selection, selects the first operator in the given direction. */
	private void selectInDirection(KeyEvent e) {
		int keyCode = e.getKeyCode();
		if (getSelection().isEmpty()) {
			for (ExecutionUnit unit : processes) {
				if (unit.getNumberOfOperators() > 0) {
					selectOperator(unit.getOperators().get(0), true);
				}
			}
		} else {
			Operator current = getSelection().get(0);
			if (current.getParent() != displayedChain) {
				return;
			}
			Rectangle2D pos = getOperatorRect(current, true);
			ExecutionUnit unit = current.getExecutionUnit();
			if (unit == null) {
				return;
			}
			double smallestDistance = Double.POSITIVE_INFINITY;
			Operator closest = null;
			for (Operator other : unit.getOperators()) {
				Rectangle2D otherPos = getOperatorRect(other, true);
				boolean ok = false;
				switch (keyCode) {
					case KeyEvent.VK_LEFT:
						ok = otherPos.getMinX() < pos.getMinX();
						break;
					case KeyEvent.VK_RIGHT:
						ok = otherPos.getMaxX() > pos.getMaxX();
						break;
					case KeyEvent.VK_UP:
						ok = otherPos.getMinY() < pos.getMinY();
						break;
					case KeyEvent.VK_DOWN:
						ok = otherPos.getMaxY() > pos.getMaxY();
						break;
				}
				if (ok) {
					double dx = otherPos.getCenterX() - pos.getCenterX();
					double dy = otherPos.getCenterY() - pos.getCenterY();
					double dist = dx * dx + dy * dy;
					if (dist < smallestDistance) {
						smallestDistance = dist;
						closest = other;
					}
				}
			}
			if (closest != null) {
				selectOperator(closest, !e.isShiftDown());
			}
		}
	}

	protected void showOperatorChain(OperatorChain op) {
		displayedChain = op;
		ExecutionUnit processes[];
		if (op == null) {
			processes = new ExecutionUnit[0];
		} else {
			processes = new ExecutionUnit[op.getNumberOfSubprocesses()];
			for (int i = 0; i < processes.length; i++) {
				processes[i] = op.getSubprocess(i);
			}
		}
		showProcesses(processes);

		fireDisplayedChainChanged(displayedChain);
		autoFit();
	}

	private void showProcesses(ExecutionUnit[] processes) {
		this.processes = processes;
		setInitialSizes(processes);
		setupExtensionButtons();
		updateComponentSize();
		repaint();
	}

	private void setupExtensionButtons() {
		for (ExtensionButton button : subprocessExtensionButtons) {
			remove(button);
		}
		subprocessExtensionButtons.clear();
		if (displayedChain.areSubprocessesExtendable()) {
			for (int index = 0; index < processes.length; index++) {
				double width = getWidth(processes[index]) + 1;
				Point loc = fromProcessSpace(new Point(0, 0), index);

				if (index == 0) {
					ExtensionButton addButton2 = new ExtensionButton(displayedChain, -1, true);
					addButton2.setBounds((int) (loc.getX() - addButton2.getPreferredSize().getWidth() + 1),
							(int) (loc.getY() - 1),
							(int) addButton2.getPreferredSize().getWidth(),
							(int) addButton2.getPreferredSize().getHeight());
					subprocessExtensionButtons.add(addButton2);
					add(addButton2);
				}

				ExtensionButton addButton = new ExtensionButton(displayedChain, index, true);
				addButton.setBounds((int) (loc.getX() + width),
						(int) (loc.getY() - 1),
						(int) addButton.getPreferredSize().getWidth(),
						(int) addButton.getPreferredSize().getHeight());
				subprocessExtensionButtons.add(addButton);
				add(addButton);

				if (processes.length > 1) {
					ExtensionButton deleteButton = new ExtensionButton(displayedChain, index, false);
					deleteButton.setBounds((int) (loc.getX() + width),
							(int) (loc.getY() + addButton.getHeight() - 1),
							(int) deleteButton.getPreferredSize().getWidth(),
							(int) deleteButton.getPreferredSize().getHeight());
					subprocessExtensionButtons.add(deleteButton);
					add(deleteButton);
				}
			}
		}
	}

	private void updateExtensionButtons() {
		for (ExtensionButton button : subprocessExtensionButtons) {
			int subprocessIndex = button.getSubprocessIndex();
			if (subprocessIndex >= 0) {
				Point loc = fromProcessSpace(new Point(0, 0), subprocessIndex);
				double width = getWidth(processes[subprocessIndex]) + 1;
				button.setBounds((int) (loc.getX() + width),
						(int) loc.getY() + (button.isAdd() ? 0 : button.getHeight()) - 1,
						(int) button.getPreferredSize().getWidth(),
						(int) button.getPreferredSize().getHeight());
			} else {
				Point loc = fromProcessSpace(new Point(0, 0), 0);
				button.setBounds((int) loc.getX() - button.getWidth(),
						(int) loc.getY() + (button.isAdd() ? 0 : button.getHeight()) - 1,
						(int) button.getPreferredSize().getWidth(),
						(int) button.getPreferredSize().getHeight());
			}
		}
	}

	/** Same like {@link #paintComponent(Graphics)} but allows to specify if highlighting and comic tutorial should be drawn */
	public void paintComponent(Graphics graphics, boolean drawHighlighting, boolean drawComicTutorial) {
		super.paintComponent(graphics);
		snapToGrid = !"false".equals(ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_RAPIDMINER_GUI_SNAP_TO_GRID));
		if (draggedOperatorsOrigins != null || connectingPortSource != null) {
			((Graphics2D) graphics).setRenderingHints(LOW_QUALITY_HINTS);
		} else {
			((Graphics2D) graphics).setRenderingHints(HI_QUALITY_HINTS);
		}
		render((Graphics2D) graphics, drawHighlighting, drawComicTutorial);
	}

	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		snapToGrid = !"false".equals(ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_RAPIDMINER_GUI_SNAP_TO_GRID));
		if (draggedOperatorsOrigins != null || connectingPortSource != null) {
			((Graphics2D) graphics).setRenderingHints(LOW_QUALITY_HINTS);
		} else {
			((Graphics2D) graphics).setRenderingHints(HI_QUALITY_HINTS);
		}
		render((Graphics2D) graphics, true, true);
	}

	@SuppressWarnings("unused")
	private void drawComicTutorial(Graphics graphics) {
		if (tutorialComicPanelToDraw != null) {
			Graphics2D translated = (Graphics2D) graphics.create();
			translated.translate(getWidth() / 2 - ORIGINAL_TUTORIAL_COMIC_WIDTH / 2, getHeight() / 2 - ORIGINAL_TUTORIAL_COMIC_HEIGHT / 2);
			translated.drawImage(tutorialComicPanelToDraw, 0, 0, new ImageObserver() {

				@Override
				public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
					return false;
				}
			});
			translated.dispose();
		}
	}

	private void onGolfDataDragged() {
		tutorialComicPanelToDraw = tutorialComicPanels[5];
	}

	private void onDecisionTreeDragged() {
		tutorialComicPanelToDraw = tutorialComicPanels[6];
	}

	/**
	 * Returns the operators rectangle.
	 * 
	 * @param autoPositionIfMissing
	 *            If true and no position has been defined yet, a position will be
	 *            created, either based on the position of adjacent operators or based on the index.
	 */
	public Rectangle2D getOperatorRect(Operator op, boolean autoPositionIfMissing) {
		Rectangle2D rect = operatorRects.get(op);
		if (rect == null && autoPositionIfMissing) {
			// if connected (e.g. because inserted by quick fix), place in the middle
			if (op.getInputPorts().getNumberOfPorts() > 0 &&
					op.getOutputPorts().getNumberOfPorts() > 0 &&
					op.getInputPorts().getPortByIndex(0).isConnected() &&
					op.getOutputPorts().getPortByIndex(0).isConnected()) {

				// to avoid that this method is called again from getPortLocation() we check whether all children know where they are.
				boolean dependenciesOk = true;
				Operator sourceOp = op.getInputPorts().getPortByIndex(0).getSource().getPorts().getOwner().getOperator();
				Operator destOp = op.getOutputPorts().getPortByIndex(0).getDestination().getPorts().getOwner().getOperator();
				dependenciesOk &= sourceOp == displayedChain || operatorRects.containsKey(sourceOp);
				dependenciesOk &= destOp == displayedChain || operatorRects.containsKey(destOp);

				if (dependenciesOk) {
					Point2D sourcePos = getPortLocation(op.getInputPorts().getPortByIndex(0).getSource());
					Point2D destPos = getPortLocation(op.getOutputPorts().getPortByIndex(0).getDestination());
					rect = new Rectangle2D.Double((sourcePos.getX() + destPos.getX()) / 2 - OPERATOR_WIDTH / 2,
							(sourcePos.getY() + destPos.getY()) / 2 - PORT_OFFSET,
							OPERATOR_WIDTH, MIN_OPERATOR_HEIGHT);
					setOperatorRect(op, rect);
				}
			}
			if (rect == null) {
				// otherwise, or, if positions were not known in previous approach, position according to index
				int index = 0;
				ExecutionUnit unit = op.getExecutionUnit();
				if (unit != null) {
					index = unit.getOperators().indexOf(op);
				}
				// rect is added to operatorRects as a side effect
				rect = autoPosition(op, index);
			}
		}
		return rect;
	}

	/** Sets the operators position and increases the process size if necessary. */
	private void setOperatorRect(Operator operator, Rectangle2D rect) {
		if (rect == null) {
			throw new NullPointerException("rect is null");
		}
		operatorRects.put(operator, rect);
		Dimension processSize = processSizes.get(operator.getExecutionUnit());
		if (processSize != null) {
			boolean needsResize = false;
			if (processSize.getWidth() < rect.getMaxX() + PADDING) {
				processSize.setSize(rect.getMaxX() + PADDING, processSize.getHeight());
				needsResize = true;
			}
			if (processSize.getHeight() < rect.getMaxY() + PADDING) {
				processSize.setSize(processSize.getWidth(), rect.getMaxY() + PADDING);
				needsResize = true;
			}
			if (needsResize) {
				updateComponentSize();
			}
		}
		fireOperatorMoved(operator);
	}

	/**
	 * Returns the distance to the next port below this port. Note that
	 * this spacing is additional to the 3/2*PORT_SIZE raster used by {@link #getPortLocation(Port)}.
	 */
	private double getPortSpacing(Port port) {
		Double d = portSpacings.get(port);
		if (d != null) {
			return d;
		} else {
			return 0;
		}
	}

	/** Set spacing and reduce spacing for successor if possible. */
	private double shiftPortSpacing(Port port, double delta) {
		// remember old spacing
		final Ports ports = port.getPorts();
		final int myIndex = ports.getAllPorts().indexOf(port);
		final Double oldD = portSpacings.get(port);
		final double old = oldD == null ? 0 : oldD;

		double newY = old + delta;
		if (isSnapToGrid()) {
			newY = Math.floor(newY / (PORT_SIZE * 3d / 2d)) * (PORT_SIZE * 3 / 2);
		}
		double diff = newY - old;

		if (diff == 0) {
			return 0;
		} else if (diff > 0) {
			// find ports which this port will "push" down
			for (int i = myIndex + 1; i < ports.getNumberOfPorts(); i++) {
				Port other = ports.getPortByIndex(i);
				double otherSpacing = getPortSpacing(other);
				if (otherSpacing < diff) {
					portSpacings.remove(other);
				} else {
					portSpacings.put(other, otherSpacing - diff);
					break;
				}
			}
			// see if it still fits into process frame
			portSpacings.put(port, old + diff);
			Point bottomPortPos = getPortLocation(ports.getPortByIndex(ports.getNumberOfPorts() - 1));
			// if it doesn't, revert
			double height = getHeight(ports.getOwner().getConnectionContext()) - PADDING;
			if (bottomPortPos.getY() > height) {
				double tooMuch = bottomPortPos.getY() - height;
				diff -= tooMuch;
				portSpacings.put(port, old + diff);
			}
			return diff;
		} else if (diff < 0) {
			// find ports which this port will "push" up
			double actuallyRemoved = 0;
			for (int i = myIndex; i >= 0; i--) {
				Port other = ports.getPortByIndex(i);
				double otherSpacing = getPortSpacing(other);
				if (otherSpacing < -diff) {
					actuallyRemoved += getPortSpacing(other);
					portSpacings.remove(other);
				} else {
					portSpacings.put(other, otherSpacing + diff);
					actuallyRemoved = -diff;
					break;
				}
			}
			if (ports.getNumberOfPorts() > myIndex + 1) {
				Port other = ports.getPortByIndex(myIndex + 1);
				portSpacings.put(other, getPortSpacing(other) + actuallyRemoved);
			}
			return -actuallyRemoved;
		} else {
			// cannot happen
			return 0;
		}
	}

	private Point getPortLocation(Port port) {
		if (port.getPorts() == null) {
			return new Point(0, 0);
		}
		Operator op = port.getPorts().getOwner().getOperator();
		int index = port.getPorts().getAllPorts().indexOf(port);
		int addOffset = 0;
		for (int i = 0; i <= index; i++) {
			addOffset += getPortSpacing(port.getPorts().getPortByIndex(i));
		}

		ExecutionUnit process;
		Point point;
		if (op == displayedChain) {
			// this is an inner port
			process = port.getPorts().getOwner().getConnectionContext();
			if (port instanceof OutputPort) {
				point = new Point(0, MIN_OPERATOR_HEIGHT / 2 + PORT_OFFSET + index * PORT_SIZE * 3 / 2 + addOffset);
			} else {
				point = new Point((int) getWidth(process), MIN_OPERATOR_HEIGHT / 2 + PORT_OFFSET + index * PORT_SIZE * 3 / 2 + addOffset);
			}
		} else {
			// this is an outer port of a nested operator
			process = op.getExecutionUnit();
			Rectangle2D opRect = getOperatorRect(op, true);
			if (port instanceof InputPort) {
				point = new Point((int) opRect.getX(), (int) opRect.getY() + PORT_OFFSET + index * PORT_SIZE * 3 / 2 + addOffset);
			} else {
				point = new Point((int) opRect.getMaxX(), (int) opRect.getY() + PORT_OFFSET + index * PORT_SIZE * 3 / 2 + addOffset);
			}
		}
		return point;
	}

	public void renderSubprocess(int index, Graphics2D g, boolean drawHighlighting, boolean drawComicTutorial) {
		double width = getWidth(processes[index]);
		double height = getHeight(processes[index]);
		Shape frame = new Rectangle2D.Double(0, 0, width, height);

		Paint currentInnerColor = INNER_COLOR;
		Paint currentTopGradient = SHADOW_TOP_GRADIENT;
		Paint currentLeftGradient = SHADOW_LEFT_GRADIENT;
		Stroke currentLineStroke = LINE_STROKE;
		Paint currentLineColor = LINE_COLOR;

		if (drawHighlighting && (dragStarted || (dropTargetSet && getImportDragged()))) {
			switch (RapidMinerGUI.getDragHighlighteMode()) {
				case FULL:
					currentInnerColor = INNER_DRAG_COLOR;
					currentTopGradient = SHADOW_TOP_DRAG_GRADIENT;
					currentLeftGradient = SHADOW_LEFT_DRAG_GRADIENT;
				case BORDER:
					currentLineStroke = LINE_DRAG_STROKE;
					currentLineColor = LINE_DRAG_COLOR;
					break;
				default:
					break;
			}
		}

		// background color
		g.setPaint(currentInnerColor);
		g.fill(frame);

		// process title color
		g.setColor(PROCESS_TITLE_COLOR);
		g.setFont(PROCESS_FONT);
		g.drawString(processes[index].getName(), PADDING + 2, PROCESS_FONT.getSize() + PADDING);

		// padding gradients
		g.setPaint(currentTopGradient);
		g.fill(new Rectangle2D.Double(0, 0, PADDING, height));
		GeneralPath top = new GeneralPath();
		int shadowWidth = PADDING;
		top.moveTo(0, 0);
		top.lineTo(width, 0);
		top.lineTo(width, shadowWidth);
		top.lineTo(shadowWidth, shadowWidth);
		top.closePath();
		g.setPaint(currentLeftGradient);
		g.fill(top);

		// frame color
		g.setPaint(currentLineColor);
		g.setStroke(currentLineStroke);

		g.draw(frame);

		if (drawComicTutorial) {
//		drawComicTutorial(g); TODO re-add when it is ready
		}

		// render operators: as a side effect the port locations are stored
		for (Operator operator : processes[index].getOperators()) {
			if (!selectedOperators.contains(operator)) {
				renderOperator(operator, g);
			}
		}
		Iterator<Operator> selectionIterator = selectedOperators.descendingIterator();
		while (selectionIterator.hasNext()) {
			Operator op = selectionIterator.next();
			if (processes[index].getOperators().contains(op)) {
				renderOperator(op, g);
			}
		}

		// render ports
		renderPorts(processes[index].getInnerSources(), null, g, true);
		renderPorts(processes[index].getInnerSinks(), null, g, true);
		renderConnections(processes[index].getInnerSinks(), processes[index].getInnerSources(), g);

		flowVisualizer.render(g, processes[index]);
	}

	private void renderPorts(Ports<? extends Port> ports, Color baseColor, Graphics2D g, boolean enabled) {
		boolean input = ports instanceof InputPorts;
		g.setStroke(LINE_STROKE);

		for (Port port : ports.getAllPorts()) {
			boolean hasError = !port.getErrors().isEmpty();

			Point location = getPortLocation(port);
			double x = location.getX();
			double y = location.getY();

			Shape ellipseTop, ellipseBottom, ellipseBoth;
			int startAngle;
			if (input) {
				startAngle = 90;
				ellipseTop = new Arc2D.Double(new Rectangle2D.Double(x - PORT_SIZE / 2, y - PORT_SIZE / 2, PORT_SIZE, PORT_SIZE),
						startAngle, 90, Arc2D.PIE);
				ellipseBottom = new Arc2D.Double(new Rectangle2D.Double(x - PORT_SIZE / 2, y - PORT_SIZE / 2, PORT_SIZE, PORT_SIZE),
						startAngle + 90, 90, Arc2D.PIE);
				ellipseBoth = new Arc2D.Double(new Rectangle2D.Double(x - PORT_SIZE / 2, y - PORT_SIZE / 2, PORT_SIZE, PORT_SIZE),
						startAngle, 180, Arc2D.PIE);
			} else {
				startAngle = 270;
				ellipseBottom = new Arc2D.Double(new Rectangle2D.Double(x - PORT_SIZE / 2, y - PORT_SIZE / 2, PORT_SIZE, PORT_SIZE),
						startAngle, 90, Arc2D.PIE);
				ellipseTop = new Arc2D.Double(new Rectangle2D.Double(x - PORT_SIZE / 2, y - PORT_SIZE / 2, PORT_SIZE, PORT_SIZE),
						startAngle + 90, 90, Arc2D.PIE);
				ellipseBoth = new Arc2D.Double(new Rectangle2D.Double(x - PORT_SIZE / 2, y - PORT_SIZE / 2, PORT_SIZE, PORT_SIZE),
						startAngle, 180, Arc2D.PIE);
			}

			Color line = Color.DARK_GRAY;
			Color fill = Color.WHITE;

			if (enabled) {
				// What we have
				if (!hasError) {
					fill = getColorFor(port, false, Color.WHITE);
				} else {
					fill = Color.RED;
				}
				g.setColor(fill);

				if (port instanceof OutputPort) {
					g.fill(ellipseBoth);
				}

				// What we want
				if (port instanceof InputPort) {
					g.fill(ellipseTop);
					InputPort inPort = (InputPort) port;
					for (Precondition precondition : inPort.getAllPreconditions()) {
						g.setColor(getColorFor(precondition.getExpectedMetaData()));
						break;
					}
					g.fill(ellipseBottom);
				}

				g.setColor(line);
				if (hoveringPort == port) {
					g.setStroke(HIGHLIGHT_STROKE);
				} else {
					g.setStroke(LINE_STROKE);
				}
				g.draw(ellipseBoth);
			} else {
				g.setColor(fill);
				g.fill(ellipseBoth);
				g.setColor(line);
				g.draw(ellipseBoth);
			}

			g.setFont(PORT_FONT);
			int xt;
			Rectangle2D strBounds = PORT_FONT.getStringBounds(port.getShortName(), g.getFontRenderContext());
			if (input) {
				xt = PORT_SIZE / 2;
			} else {
				xt = -(int) strBounds.getWidth() - 3;
			}

			if (hasError) {
				g.setColor(Color.RED);
				g.setFont(PORT_FONT.deriveFont(Font.BOLD));
			} else {
				if (baseColor == null) {
					if (port == hoveringPort) {
						g.setColor(PORT_NAME_SELECTION_COLOR);
					} else {
						g.setColor(PORT_NAME_COLOR);
					}
				} else {
					if (port == hoveringPort) {
						g.setPaint(baseColor.darker());
					} else {
						g.setPaint(baseColor.darker().darker());
					}
				}
			}

			g.drawString(port.getShortName(), (int) x + xt, (int) (y + strBounds.getHeight() / 2 - 2));
		}
	}

	/** Returns the given icon in an appropriate enabled/disabled state. */
	private ImageIcon getIcon(Operator operator, ImageIcon icon) {
		if (operator.isEnabled() && this.isEnabled()) {
			return icon;
		} else {
			return (ImageIcon) UIManager.getLookAndFeel().getDisabledIcon(DUMMY_LABEL, icon);
		}
	}

	private void renderOperator(Operator operator, Graphics2D g) {
		Rectangle2D frame = getOperatorRect(operator, true);
		double height = Math.max(MIN_OPERATOR_HEIGHT,
				40 + PORT_SIZE * 3 / 2 * Math.max(operator.getInputPorts().getNumberOfPorts(),
						operator.getOutputPorts().getNumberOfPorts()));
		if (frame.getHeight() != height) {
			frame.setRect(frame.getX(), frame.getY(), frame.getWidth(), height);
		}

		final double headerHeight = HEADER_HEIGHT;
		double headerWidth;
		Shape bodyShape;
		double nameRollout = nameRolloutInterpolationMap.getValue(operator);
		if (nameRollout > 0) {
			// if (operator == getHoveringOperator()) {
			Rectangle2D nameBounds = OPERATOR_FONT.getStringBounds(operator.getName(), g.getFontRenderContext());
			headerWidth = nameBounds.getWidth() + 6;
			if (headerWidth > frame.getWidth()) {
				double dif = headerWidth - frame.getWidth();
				headerWidth = frame.getWidth() + nameRollout * dif;
				GeneralPath path = new GeneralPath();
				path.moveTo(frame.getMinX(), frame.getMinY());
				path.lineTo(frame.getMinX() + headerWidth, frame.getMinY());
				path.lineTo(frame.getMinX() + headerWidth, frame.getMinY() + headerHeight);
				path.lineTo(frame.getMaxX(), frame.getMinY() + headerHeight);
				path.lineTo(frame.getMaxX(), frame.getMaxY());
				path.lineTo(frame.getMinX(), frame.getMaxY());
				path.closePath();
				bodyShape = path;
			} else {
				headerWidth = frame.getWidth();
				bodyShape = frame;
			}
		} else {
			headerWidth = frame.getWidth();
			bodyShape = frame;
		}

		// Shadow
		if (!selectedOperators.isEmpty() && operator == selectedOperators.get(0) ||
				dropInsertionPredecessor == operator) {
			Rectangle2D shadow = new Rectangle2D.Double(frame.getX() + 5, frame.getY() + 5, frame.getWidth(), frame.getHeight());
			GeneralPath bottom = new GeneralPath();
			bottom.moveTo(shadow.getX(), frame.getMaxY());
			bottom.lineTo(frame.getMaxX(), frame.getMaxY());
			bottom.lineTo(shadow.getMaxX(), shadow.getMaxY());
			bottom.lineTo(shadow.getMinX(), shadow.getMaxY());
			bottom.closePath();
			g.setPaint(new GradientPaint((float) frame.getX(), (float) frame.getMaxY(), Color.gray, (float) frame.getX(), (float) shadow.getMaxY(), INNER_COLOR));
			g.fill(bottom);

			GeneralPath right = new GeneralPath();
			right.moveTo(frame.getMaxX(), shadow.getMinY());
			right.lineTo(shadow.getMaxX(), shadow.getMinY());
			right.lineTo(shadow.getMaxX(), shadow.getMaxY());
			right.lineTo(frame.getMaxX(), frame.getMaxY());
			right.closePath();
			g.setPaint(new GradientPaint((float) frame.getMaxX(), (float) shadow.getY(), Color.gray, (float) shadow.getMaxX(), (float) shadow.getY(), INNER_COLOR));
			g.fill(right);
		}

		// Frame head
		Color baseColor = SwingTools.getOperatorColor(operator);
		if (!operator.isEnabled() || !this.isEnabled()) {
			baseColor = Color.LIGHT_GRAY;
		}

		g.setPaint(baseColor);
		g.fill(frame);

		// head gradient
		Rectangle2D bar = new Rectangle2D.Double(frame.getX(), frame.getY(), headerWidth, headerHeight);
		Color c0 = new Color(Math.max(baseColor.getRed() - 25, 0), Math.max(baseColor.getGreen() - 25, 0), Math.max(baseColor.getBlue() - 25, 0));
		Color c1 = baseColor;
		Rectangle2D[] regions = splitHorizontalBar(bar, 0.0, 0.2, 0.6);

		GradientPaint gp = new GradientPaint(0.0f, (float) regions[0].getMinY(), c0, 0.0f, (float) regions[0].getMaxX(), c1);
		g.setPaint(gp);
		g.fill(regions[0]);

		gp = new GradientPaint(0.0f, (float) regions[1].getMinY(), c1, 0.0f, (float) regions[1].getMaxY(), Color.WHITE);
		g.setPaint(gp);
		g.fill(regions[1]);

		gp = new GradientPaint(0.0f, (float) regions[2].getMinY(), Color.WHITE, 0.0f, (float) regions[2].getMaxY(), c1);
		g.setPaint(gp);
		g.fill(regions[2]);

		gp = new GradientPaint(0.0f, (float) regions[3].getMinY(), c1, 0.0f, (float) regions[3].getMaxY(), c0.darker());
		g.setPaint(gp);
		g.fill(regions[3]);

		// Frame Body
		g.setPaint(LINE_COLOR);
		g.setStroke(LINE_STROKE);
		if (selectedOperators.contains(operator) || operator == dropInsertionPredecessor) {
			g.setPaint(FRAME_COLOR_SELECTED);
			g.setStroke(FRAME_STROKE_SELECTED);
		} else {
			g.setPaint(FRAME_COLOR_NORMAL);
			g.setStroke(FRAME_STROKE_NORMAL);
		}
		g.draw(bodyShape);

		// Label: Name
		g.setFont(OPERATOR_FONT);
		if (operator.isEnabled()) {
			if (operator == getHoveringOperator()) {
				g.setPaint(baseColor.darker());
			} else if (selectedOperators.contains(operator)) {
				g.setPaint(baseColor.darker());
			} else {
				g.setPaint(baseColor.darker().darker());
			}
		} else {
			g.setPaint(baseColor.darker().darker());
		}
		// if (operator != getHoveringOperator()) {
		g.drawString(fitString(operator.getName(), g, (int) headerWidth - 3), (int) frame.getX() + 4, (int) (frame.getY() + OPERATOR_FONT.getSize() + 1));
		// } else {
		// g.drawString(operator.getName(), (int)frame.getX() + 4, (int)(frame.getY() + OPERATOR_FONT.getSize() + 1));
		// }

		// Icon
		ImageIcon icon = operator.getOperatorDescription().getIcon();
		if (icon != null) {
			if (!operator.isEnabled()) {
				icon = getIcon(operator, icon);
			}
			icon.paintIcon(this, g,
					(int) (frame.getX() + frame.getWidth() / 2 - icon.getIconWidth() / 2),
					(int) (frame.getY() + headerHeight + (height - headerHeight - 10) / 2 - icon.getIconHeight() / 2));
		}

		// Ports
		renderConnections(operator.getInputPorts(), operator.getOutputPorts(), g);
		renderPorts(operator.getInputPorts(), baseColor, g, operator.isEnabled());
		renderPorts(operator.getOutputPorts(), baseColor, g, operator.isEnabled());

		// Small icons
		int iconX = (int) frame.getX() + 3;
		// Dirtyness
		ImageIcon opIcon;
		if (operator.isRunning()) {
			opIcon = OPERATOR_RUNNING;
		} else if (!operator.isDirty()) {
			opIcon = OPERATOR_READY;
		} else if (!operator.getErrorList().isEmpty()) {
			opIcon = OPERATOR_ERROR_ICON;
		} else {
			opIcon = OPERATOR_DIRTY;
		}
		getIcon(operator, opIcon).paintIcon(this, g, iconX, (int) (frame.getY() + frame.getHeight() - opIcon.getIconHeight() - 1));
		iconX += opIcon.getIconWidth() + 1;

		// Errors
		if (!operator.getErrorList().isEmpty()) {
			getIcon(operator, IMAGE_WARNING).paintIcon(this, g, iconX, (int) (frame.getY() + frame.getHeight() - IMAGE_WARNING.getIconHeight() - 1));
		}
		iconX += IMAGE_WARNING.getIconWidth() + 1;

		// Breakpoint
		if (operator.hasBreakpoint()) {
			ImageIcon breakpointIcon;
			if (operator.getNumberOfBreakpoints() == 1) {
				if (operator.hasBreakpoint(BreakpointListener.BREAKPOINT_BEFORE)) {
					breakpointIcon = IMAGE_BREAKPOINT_BEFORE;
				} else if (operator.hasBreakpoint(BreakpointListener.BREAKPOINT_AFTER)) {
					breakpointIcon = IMAGE_BREAKPOINT_AFTER;
				} else {
					breakpointIcon = IMAGE_BREAKPOINT_WITHIN;
				}
			} else {
				breakpointIcon = IMAGE_BREAKPOINTS;
			}
			getIcon(operator, breakpointIcon).paintIcon(this, g, iconX, (int) (frame.getY() + frame.getHeight() - breakpointIcon.getIconHeight() - 1));
		}
		iconX += IMAGE_BREAKPOINTS.getIconWidth() + 1;

		// Comment
		if (operator.getUserDescription() != null && operator.getUserDescription().length() > 0) {
			getIcon(operator, IMAGE_COMMENT).paintIcon(this, g, iconX, (int) (frame.getY() + frame.getHeight() - IMAGE_COMMENT.getIconHeight() - 1));
		}
		iconX += IMAGE_COMMENT.getIconWidth() + 1;

		if (operator instanceof OperatorChain) {
			getIcon(operator, IMAGE_BRANCH).paintIcon(this, g, iconX, (int) (frame.getY() + frame.getHeight() - IMAGE_BRANCH.getIconHeight() - 1));
		}
		iconX += IMAGE_BRANCH.getIconWidth() + 1;
	}

	private Rectangle2D[] splitHorizontalBar(RectangularShape bar, double a, double b, double c) {
		Rectangle2D[] result = new Rectangle2D[4];
		double y0 = bar.getMinY();
		double y1 = Math.rint(y0 + bar.getHeight() * a);
		double y2 = Math.rint(y0 + bar.getHeight() * b);
		double y3 = Math.rint(y0 + bar.getHeight() * c);
		result[0] = new Rectangle2D.Double(bar.getMinX(), bar.getMinY(),
				bar.getWidth(), y1 - y0);
		result[1] = new Rectangle2D.Double(bar.getMinX(), y1, bar.getWidth(),
				y2 - y1);
		result[2] = new Rectangle2D.Double(bar.getMinX(), y2, bar.getWidth(),
				y3 - y2);
		result[3] = new Rectangle2D.Double(bar.getMinX(), y3, bar.getWidth(),
				bar.getMaxY() - y3);
		return result;
	}

	private Color getColorFor(Port port, boolean alwaysDark, Color defaultColor) {
		if (!isEnabled()) {
			return Color.LIGHT_GRAY;
		}
		IOObject data = port.getAnyDataOrNull();
		if (data != null) {
			if (data instanceof IOObjectCollection) {
				return IO_CLASS_TO_COLOR_MAP.get(((IOObjectCollection) data).getElementClass(true));
			} else {
				return IO_CLASS_TO_COLOR_MAP.get(data.getClass());
			}
		} else if (port.getMetaData() != null) {
			MetaData md = port.getMetaData();
			return getColorFor(md);
		} else {
			return defaultColor;
		}
	}

	public static Color getColorFor(MetaData md) {
		if (md == null) {
			return Color.WHITE;
		}
		if (md instanceof CollectionMetaData) {
			MetaData elementMetaDataRecursive = ((CollectionMetaData) md).getElementMetaDataRecursive();
			if (elementMetaDataRecursive != null) {
				return IO_CLASS_TO_COLOR_MAP.get(elementMetaDataRecursive.getObjectClass());
			} else {
				return IO_CLASS_TO_COLOR_MAP.get(IOObject.class);
			}
		} else {
			return IO_CLASS_TO_COLOR_MAP.get(md.getObjectClass());
		}
	}

	private void renderConnections(InputPorts inputPorts, OutputPorts ports, Graphics2D g) {
		if (connectingPortSource != null && mousePositionRelativeToProcess != null && connectingPortSource instanceof InputPort && inputPorts.containsPort((InputPort) connectingPortSource)) {
			g.setColor(ACTIVE_EDGE_COLOR);
			Point2D toLoc = getPortLocation(connectingPortSource);
			toLoc.setLocation(toLoc.getX() - PORT_SIZE, toLoc.getY());
			g.draw(new Line2D.Double(toLoc.getX() + PORT_SIZE / 2, toLoc.getY(), mousePositionRelativeToProcess.getX(), mousePositionRelativeToProcess.getY()));
		}

		for (int i = 0; i < ports.getNumberOfPorts(); i++) {
			OutputPort from = ports.getPortByIndex(i);
			Port to = from.getDestination();
			g.setColor(getColorFor(from, true, Color.LIGHT_GRAY));
			if (to != null) {
				if (from.getMetaData() instanceof CollectionMetaData) {
					if (from == hoveringPort ||
							to == hoveringPort ||
							from == hoveringConnectionSource ||
							from == selectedConnectionSource) {
						g.setStroke(CONNECTION_COLLECTION_HIGHLIGHT_STROKE);
					} else {
						g.setStroke(CONNECTION_COLLECTION_LINE_STROKE);
					}
					g.draw(createConnector(from, to));
					g.setColor(Color.white);
					g.setStroke(LINE_STROKE);
					g.draw(createConnector(from, to));
				} else {
					if (from == hoveringPort ||
							to == hoveringPort ||
							from == hoveringConnectionSource ||
							from == selectedConnectionSource) {
						g.setStroke(CONNECTION_HIGHLIGHT_STROKE);
					} else {
						g.setStroke(CONNECTION_LINE_STROKE);
					}
					// g.draw(new Line2D.Double(portLocations.get(from), portLocations.get(to)));
					g.draw(createConnector(from, to));
				}
			}

			if (connectingPortSource == from && mousePositionRelativeToProcess != null) {
				g.setColor(ACTIVE_EDGE_COLOR);
				Point2D fromLoc = getPortLocation(connectingPortSource);
				g.draw(new Line2D.Double(fromLoc.getX() + PORT_SIZE / 2, fromLoc.getY(), mousePositionRelativeToProcess.getX(), mousePositionRelativeToProcess.getY()));
			}
		}
	}

	/** Renders without highlighting and comic tutorial */
	public void render(Graphics2D graphics) {
		render(graphics, false, false);
	}
	
	public void render(Graphics2D graphics, boolean drawHighlighting, boolean drawComicTutorial) {
		if (processes == null || processes.length == 0) {
			return;
		}

		Graphics2D g = (Graphics2D) graphics.create();
		g.translate(0, -1);
		g.translate(0, PADDING);
		for (int i = 0; i < processes.length; i++) {
			switch (ORIENTATION) {
				case X_AXIS:
					g.translate(WALL_WIDTH, 0);
					break;
				case Y_AXIS:
					g.translate(0, PADDING);
					break;
			}
			renderSubprocess(i, g, drawHighlighting, drawComicTutorial);
			switch (ORIENTATION) {
				case X_AXIS:
					g.translate(getWidth(processes[i]) + WALL_WIDTH, 0);
					break;
				case Y_AXIS:
					g.translate(0, getHeight(processes[i]) + PADDING);
					break;
			}
		}
		g.translate(0, PADDING);

		if (selectionRectangle != null) {
			Graphics2D selG = (Graphics2D) graphics.create();
			selG.setPaint(SELECTION_RECT_PAINT);
			selG.setStroke(SELECTION_RECT_STROKE);
			selG.draw(selectionRectangle);
			selG.dispose();
		}
		g.dispose();
	}

	private final transient MouseAdapter MOUSE_HANDLER = new MouseAdapter() {

		private boolean pressHasSelected = false;

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseMoved(MouseEvent e) {
			currentMousePosition = e.getPoint();
			if (flowVisualizer.isActive()) {
				return;
			}
			if (connectingPortSource != null) {
				repaint();
			}
			updateHoveringState(e);
		}

		private void updateHoveringState(MouseEvent e) {
			hoveringProcessIndex = getProcessIndexUnder(e.getPoint());
			if (hoveringProcessIndex != -1) {
				mousePositionRelativeToProcess = toProcessSpace(e.getPoint(), hoveringProcessIndex);
				int relativeX = (int) mousePositionRelativeToProcess.getX();
				int relativeY = (int) mousePositionRelativeToProcess.getY();

				OutputPort connectionSourceUnderMouse = getPortForConnectorNear(mousePositionRelativeToProcess, processes[hoveringProcessIndex]);
				if (connectionSourceUnderMouse != hoveringConnectionSource) {
					hoveringConnectionSource = connectionSourceUnderMouse;
					repaint();
				}

				// find inner sinks/sources under mouse
				if (checkPortUnder(processes[hoveringProcessIndex].getInnerSinks(), relativeX, relativeY) ||
						checkPortUnder(processes[hoveringProcessIndex].getInnerSources(), relativeX, relativeY)) {
					return;
				}

				// find operator under mouse
				List<Operator> operators = processes[hoveringProcessIndex].getOperators();
				ListIterator<Operator> iterator = operators.listIterator(operators.size());
				while (iterator.hasPrevious()) {
					Operator op = iterator.previous();
					// first, check whether we are over a port
					if (checkPortUnder(op.getInputPorts(), relativeX, relativeY) ||
							checkPortUnder(op.getOutputPorts(), relativeX, relativeY)) {
						return;
					}
					// If not, check operator.
					Rectangle2D rect = getOperatorRect(op, true);
					if (rect.contains(new Point2D.Double(relativeX, relativeY))) {
						if (getHoveringOperator() != op) {
							hoveringPort = null;
							setHoveringOperator(op);
							updateCursor();
							if (getHoveringOperator() instanceof OperatorChain) {
								showStatus(I18N.getGUILabel("processRenderer.displayChain.hover"));
							} else {
								showStatus(I18N.getGUILabel("processRenderer.operator.hover"));
							}
							repaint();
						}
						return;
					}
				}
			}
			if (getHoveringOperator() != null) {
				setHoveringOperator(null);
				updateCursor();
				repaint();
			}
			if (hoveringPort != null) {
				hoveringPort = null;
				updateCursor();
				repaint();
			}
			if (hoveringConnectionSource != null) {
				showStatus(I18N.getGUILabel("processRenderer.connection.hover"));
			} else {
				clearStatus();
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {

			// do nothing if flow visualizer is active
			if (flowVisualizer.isActive()) {
				return;
			}

			if (renameField != null) {
				remove(renameField);
			}

			requestFocus();
			pressHasSelected = false;
			mousePositionAtDragStart = e.getPoint();
			mousePositionAtLastEvaluation = e.getPoint();
			hasDragged = false;
			pressedMouseButton = e.getButton();
			connectionDraggingCanceled = false;

			// Popup will only be triggered if mouse has been released and no dragging was done
			// CAUTION: Mac&Linux / Windows do different popup trigger handling. Because of this the popup trigger has to be checked
			// in mousePressed AND mouseReleased
			if (e.isPopupTrigger()) {
				if (showPopupMenu(e)) {
					return;
				}
			}

			if (e.getButton() == MouseEvent.BUTTON1) {
				if (selectedConnectionSource != hoveringConnectionSource) {
					selectedConnectionSource = hoveringConnectionSource;
					repaint();
				}
			}

			if (e.getButton() == MouseEvent.BUTTON2) {
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				return;
			}

			// disconnect when clicking with alt + left mouse on connection
			if (hoveringConnectionSource != null && e.getButton() == MouseEvent.BUTTON1 && e.isAltDown()) {
				hoveringConnectionSource.disconnect();
			}

			// If mouse pressed while connecting, check if connecting ports should be canceled
			if (connectingPortSource != null) {

				// cancel if right mouse button is pressed
				if (e.getButton() == MouseEvent.BUTTON3) {
					cancelConnectionDragging();
					return;
				}

				// cancel if any button is pressed but not over hovering port
				if (hoveringPort == null) {
					cancelConnectionDragging();
					return;
				}
			}

			updateHoveringState(e);

			if (e.getButton() == MouseEvent.BUTTON1 && hoveringPort != null) {

				// Left mouse button pressed on port with alt pressed -> remove connection
				if (e.isAltDown()) {
					if (hoveringPort instanceof OutputPort) {
						if (((OutputPort) hoveringPort).isConnected()) {
							((OutputPort) hoveringPort).disconnect();
						}
					} else if (hoveringPort instanceof InputPort) {
						if (((InputPort) hoveringPort).isConnected()) {
							((InputPort) hoveringPort).getSource().disconnect();
						}
					}
					repaint();
				} else {
					// Left mouse button pressed on port -> start connecting ports
					if (hoveringPort instanceof OutputPort) {
						if (connectingPortSource != null && connectingPortSource instanceof InputPort) {
							connectConnectingPortSourceWithHoveringPort((InputPort) connectingPortSource, (OutputPort) hoveringPort, hoveringPort);
						} else {
							if (!e.isShiftDown()) {
								connectingPortSource = hoveringPort;
							}
						}
					} else if (hoveringPort instanceof InputPort) {
						if (connectingPortSource != null && connectingPortSource instanceof OutputPort) {
							connectConnectingPortSourceWithHoveringPort((InputPort) hoveringPort, (OutputPort) connectingPortSource, hoveringPort);
						} else {
							if (!e.isShiftDown()) {
								connectingPortSource = hoveringPort;
							}
						}
					}
				}

			} else if (getHoveringOperator() == null) {
				// deselect unless shift is pressed
				if (!e.isShiftDown() && !e.isControlDown()) {
					selectOperator(displayedChain, true);
				}
			}

			if (hoveringPort != null) {
				selectOperator(hoveringPort.getPorts().getOwner().getOperator(), true);
				pressHasSelected = true;
			} else {
				if (getHoveringOperator() == null) {
					if (!e.isShiftDown() && !e.isControlDown()) {
						selectOperator(displayedChain, true);
						pressHasSelected = true;
					}
				}
			}
			if (getHoveringOperator() != null) {
				// control down and reducing selection from {A,B,C} to {A} is delayed to mouseReleased
				if (!e.isControlDown() && !selectedOperators.contains(getHoveringOperator())) {
					selectOperator(getHoveringOperator(), true, e.isShiftDown());
					pressHasSelected = true;
				}
				// start dragging
				draggedOperatorsOrigins = new HashMap<Operator, Rectangle2D>();
				for (Operator op : selectedOperators) {
					if (op.getExecutionUnit() == getHoveringOperator().getExecutionUnit()) {
						draggedOperatorsOrigins.put(op, (Rectangle2D) getOperatorRect(op, false).clone());
					}
				}
				// no rollout during drag
				nameRolloutInterpolationMap.clear();
			} else if (hoveringPort != null) {
				draggedPort = hoveringPort;
			} else if (e.getButton() == MouseEvent.BUTTON1) {
				// start selection
				selectionRectangle = getSelectionRectangle(mousePositionAtDragStart, e.getPoint());
			}
		}

		private void connectConnectingPortSourceWithHoveringPort(InputPort input, OutputPort output, Port hoveringPort) {
			try {
				Operator destOp = input.getPorts().getOwner().getOperator();
				boolean hasConnections = hasConnections(destOp);
				connect(output, input);
				// move directly after source if first connection
				if (!hasConnections) {
					Operator sourceOp = output.getPorts().getOwner().getOperator();
					if (destOp != displayedChain && sourceOp != displayedChain) {
						destOp.getExecutionUnit().moveToIndex(destOp, destOp.getExecutionUnit().getOperators().indexOf(sourceOp) + 1);
					}
				}
			} catch (PortException e1) {
				if (e1.hasRepairOptions()) {

					// calculate popup position
					Point popupPosition = getPortLocation(hoveringPort);

					// take splitted process pane into account and add offset for each process we have to the left of our current one
					if (hoveringPort.getPorts() != null) {
						ExecutionUnit process;
						if (hoveringPort.getPorts().getOwner().getOperator() == displayedChain) {
							// this is an inner port
							process = hoveringPort.getPorts().getOwner().getConnectionContext();
						} else {
							// this is an outer port of a nested operator
							process = hoveringPort.getPorts().getOwner().getOperator().getExecutionUnit();
						}
						// iterate over all processes and add widths of processes to the left
						int counter = 0;
						for (ExecutionUnit unit : processes) {
							if (unit == process) {
								// only add process widths until we have the process which contains the port
								break;
							} else {
								counter++;
								popupPosition = new Point((int) (popupPosition.x + getWidth(unit) + WALL_WIDTH), popupPosition.y);
							}
						}
						// add another wall width as offset if we have multiple processes
						if (counter > 0) {
							popupPosition = new Point((int) (popupPosition.x + WALL_WIDTH), popupPosition.y);
						}
					}

					if (hoveringPort instanceof InputPort) {
						popupPosition.setLocation(popupPosition.getX() + 28, popupPosition.getY() - 2);
					} else {
						popupPosition.setLocation(popupPosition.getX() - 18, popupPosition.getY() - 2);
					}

					e1.showRepairPopup(ProcessRenderer.this, popupPosition);
				} else {
					JOptionPane.showMessageDialog(null, e1.getMessage(), "Cannot connect", JOptionPane.ERROR_MESSAGE);
				}
				repaint();
			} finally {
				cancelConnectionDragging();
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (flowVisualizer.isActive()) {
				return;
			}

			if ((e.getModifiers() & MouseEvent.BUTTON2_MASK) != 0) {
				setCursor(Cursor.getDefaultCursor());
				return;
			}

			if (e.isConsumed()) {
				return;
			}

			// Popup will only be triggered if mouse has been released and no dragging was done
			// CAUTION: Mac&Linux / Windows do different popup trigger handling. Because of this the popup trigger has to be checked
			// in mousePressed AND mouseReleased
			if (e.isPopupTrigger()) {
				if (showPopupMenu(e)) {
					return;
				}
			}

			if (connectingPortSource != null) {

				// cancel if right mouse button is released
				if (e.getButton() == MouseEvent.BUTTON3) {
					cancelConnectionDragging();
				}

				// cancel if any button is released but not over hovering port
				if (hoveringPort == null) {
					cancelConnectionDragging();
				}

				// connect when released over hovering port
				if (e.getButton() == MouseEvent.BUTTON1 && hoveringPort != null && !e.isAltDown()) {
					if (hoveringPort instanceof InputPort && connectingPortSource instanceof OutputPort) {
						connectConnectingPortSourceWithHoveringPort((InputPort) hoveringPort, (OutputPort) connectingPortSource, hoveringPort);
					} else if (hoveringPort instanceof OutputPort && connectingPortSource instanceof InputPort) {
						connectConnectingPortSourceWithHoveringPort((InputPort) connectingPortSource, (OutputPort) hoveringPort, hoveringPort);
					}
				}
			}

			try {
				if (selectionRectangle != null) {
					if (selectionRectangle.getWidth() > 3 && selectionRectangle.getHeight() > 3) {
						int processIndex = getProcessIndexUnder(mousePositionAtDragStart);
						if (processIndex == -1) {
							processIndex = getProcessIndexUnder(e.getPoint());
						}
						if (processIndex == -1) {
							processIndex = getProcessIndexUnder(new Point((int) selectionRectangle.getCenterX(), (int) selectionRectangle.getCenterY()));
						}
						Point offset = toProcessSpace(new Point(0, 0), processIndex);
						if (offset != null) {
							selectionRectangle.setFrame(selectionRectangle.getX() + offset.getX(),
									selectionRectangle.getY() + offset.getY(),
									selectionRectangle.getWidth(),
									selectionRectangle.getHeight());
							if (!e.isShiftDown() && !e.isControlDown() ||
									selectedOperators.size() == 1 && selectedOperators.get(0) == displayedChain) { // if we have only
								// selected the
								// parent, we
								// ignore SHIFT and
								// CTRL
								selectedOperators.clear();
							}
							for (Operator op : processes[processIndex].getOperators()) {
								Rectangle2D opRect = getOperatorRect(op, true);
								if (selectionRectangle.contains(opRect)) {
									selectOperator(op, false);
								}
							}
						}
					}
					selectionRectangle = null;
				} else {
					if (hasDragged && draggedOperatorsOrigins != null && draggedOperatorsOrigins.size() == 1) {
						insertIntoHoveringConnection(getHoveringOperator());
					} else if (!hasDragged && getHoveringOperator() != null && !e.isPopupTrigger() && (e.isControlDown() || selectedOperators.contains(getHoveringOperator()) && !pressHasSelected)) {
						// control and deselection was delayed to mouseReleased
						selectOperator(getHoveringOperator(), !e.isControlDown(), e.isShiftDown());
					}
				}

				if (draggedOperatorsOrigins != null || draggedPort != null) {
					// mainFrame.addToUndoList();
					displayedChain.getProcess().updateNotify();
//					autoFit();
				}
			} finally {
				mousePositionAtDragStart = null;
				draggedPort = null;
				draggedOperatorsOrigins = null;
				hasDragged = false;
			}
			repaint();
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			currentMousePosition = e.getPoint();

			if (flowVisualizer.isActive()) {
				return;
			}

			// Pan viewport
			if ((e.getModifiers() & MouseEvent.BUTTON2_MASK) != 0) {
				if (getParent() instanceof JViewport) {
					JViewport jv = (JViewport) getParent();
					Point p = jv.getViewPosition();
					int newX = p.x - (e.getX() - mousePositionAtDragStart.x);
					int newY = p.y - (e.getY() - mousePositionAtDragStart.y);
					int maxX = getWidth() - jv.getWidth();
					int maxY = getHeight() - jv.getHeight();
					if (newX < 0)
						newX = 0;
					if (newX > maxX)
						newX = maxX;
					if (newY < 0)
						newY = 0;
					if (newY > maxY)
						newY = maxY;
					jv.setViewPosition(new Point(newX, newY));
					return;
				}
			}

			// drag ports
			if (connectingPortSource != null) {
				repaint();

				// We cannot drag when it is an inner sink: dragging means moving the port.
				if (connectingPortSource.getPorts().getOwner().getOperator() == displayedChain && e.isShiftDown()) {
					cancelConnectionDragging();
				}
			}

			// find process in which we are dragging
			hoveringProcessIndex = getProcessIndexUnder(e.getPoint());
			if (hoveringProcessIndex != -1) {
				mousePositionRelativeToProcess = toProcessSpace(e.getPoint(), hoveringProcessIndex);
			}
			hasDragged = true;
			if (draggedOperatorsOrigins != null && !draggedOperatorsOrigins.isEmpty()) {
				ExecutionUnit draggingInSubprocess = draggedOperatorsOrigins.keySet().iterator().next().getExecutionUnit();
				Operator hoveringOperator = getHoveringOperator();
				if (hoveringOperator != null) {
					if (draggedOperatorsOrigins.size() == 1) {
						if (canBeInsertedIntoConnection(hoveringOperator)) {
							int pid = getIndex(draggingInSubprocess);
							Point processSpace = toProcessSpace(e.getPoint(), pid);
							if (processSpace != null) {
								hoveringConnectionSource = getPortForConnectorNear(processSpace, draggingInSubprocess);
							}
						}
					}

					double difX = e.getX() - mousePositionAtDragStart.getX();
					double difY = e.getY() - mousePositionAtDragStart.getY();

					// hoveringOperator is always included in draggedOperators
					if (!draggedOperatorsOrigins.containsKey(hoveringOperator)) {
						draggedOperatorsOrigins.put(hoveringOperator, getOperatorRect(hoveringOperator, false));
					}
					double targetX = draggedOperatorsOrigins.get(hoveringOperator).getX() + difX;
					double targetY = draggedOperatorsOrigins.get(hoveringOperator).getY() + difY;
					if (targetX < 0) {
						targetX = 0;
					}

					if (targetY < 0) {
						targetY = 0;
					}

					// use only hovering operator for snapping
					if (isSnapToGrid()) {
						Point snapped = snap(new Point2D.Double(targetX, targetY));
						targetX = snapped.getX();
						targetY = snapped.getY();
					}

					// now, set difX and difY to shift /after/ snapped and clipped
					difX = targetX - draggedOperatorsOrigins.get(hoveringOperator).getX();
					difY = targetY - draggedOperatorsOrigins.get(hoveringOperator).getY();

					// bound to subprocess
					double unitWidth = getWidth(draggingInSubprocess);
					double unitHeight = getHeight(draggingInSubprocess);
					for (Operator op : draggedOperatorsOrigins.keySet()) {
						Rectangle2D origin = draggedOperatorsOrigins.get(op);
						if (origin.getMaxX() + difX >= unitWidth) {
							difX -= origin.getMaxX() + difX - unitWidth;
						}
						if (origin.getMaxY() + difY >= unitHeight) {
							difY -= origin.getMaxY() + difY - unitHeight;
						}
						if (origin.getMinX() + difX < 0) {
							difX -= origin.getMinX() + difX;
						}
						if (origin.getMinY() + difY < 0) {
							difY -= origin.getMinY() + difY;
						}
					}

					double maxX = 0;
					double maxY = 0;
					// shift
					for (Operator op : draggedOperatorsOrigins.keySet()) {
						Rectangle2D origin = draggedOperatorsOrigins.get(op);
						Rectangle2D opPos = new Rectangle2D.Double(origin.getX() + difX, origin.getY() + difY, origin.getWidth(), origin.getHeight());
						setOperatorRect(op, opPos);
					}
					ensureWidth(draggingInSubprocess, (int) maxX);
					ensureHeight(draggingInSubprocess, (int) maxY);
					repaint();
				}
			} else {
				// ports are draggeable only if they belong to the displayedChain <-> they are inner sinks our sources
				if (isDisplayChainPortDragged() &&
						// furthermore they can only be dragged with left mouse button + shift key pressed
						(pressedMouseButton == MouseEvent.BUTTON1 && e.isShiftDown())) {

					double diff = e.getY() - mousePositionAtLastEvaluation.getY();
					double shifted = shiftPortSpacing(draggedPort, diff);
					mousePositionAtLastEvaluation.setLocation(mousePositionAtLastEvaluation.getX(), mousePositionAtLastEvaluation.getY() + shifted);

					repaint();
				} else if (selectionRectangle != null) {
					selectionRectangle = getSelectionRectangle(mousePositionAtDragStart, e.getPoint());
					repaint();
				} else if (connectingPortSource != null) {
					updateHoveringState(e);
				}
			}
		}

		private boolean isDisplayChainPortDragged() {
			return draggedPort != null && draggedPort.getPorts().getOwner().getOperator() == displayedChain;
		}

		@Override
		public void mouseClicked(MouseEvent e) {

			if (flowVisualizer.isActive()) {
				return;
			}
			requestFocus();

			switch (e.getButton()) {
				case MouseEvent.BUTTON1:
					if (e.getClickCount() == 2) {
						if (getHoveringOperator() != null) {
							if (getHoveringOperator() instanceof OperatorChain) {
								processPanel.showOperatorChain((OperatorChain) getHoveringOperator());
								ProcessRenderer.this.mainFrame.addViewSwitchToUndo();
							}
						}
					}
					repaint();
					break;
				case MouseEvent.BUTTON3:
					if (connectingPortSource != null) {
						cancelConnectionDragging();
						break;
					}
			}

			repaint();

		}

		@Override
		public void mouseExited(MouseEvent e) {
			mainFrame.getStatusBar().clearSpecialText();
		};
	};

	private void selectOperator(Operator op, boolean clear) {
		selectOperator(op, clear, false);
	}

	private void cancelConnectionDragging() {
		connectionDraggingCanceled = true;
		connectingPortSource = null;
		repaint();
	}

	/**
	 * 
	 * @param op
	 *            The operator to add.
	 * @param clear
	 *            If true, clear before adding
	 * @param range
	 *            If true, select interval from last selected operator to op.
	 */
	private void selectOperator(Operator op, boolean clear, boolean range) {
		boolean changed = false;
		if (clear || op == null) {
			if (!selectedOperators.isEmpty()) {
				changed = true;
				if (!range) {
					selectedOperators.clear();
				} else {
					Operator last = null;
					if (!selectedOperators.isEmpty()) {
						last = selectedOperators.getLast();
					}
					selectedOperators.clear();
					if (last != null) {
						selectedOperators.add(last);
					}
				}
			}
		}
		if (range) {
			int lastIndex = -1;
			boolean sameUnit = true;
			if (!selectedOperators.isEmpty()) {
				Operator lastSelected = selectedOperators.getLast();
				if (lastSelected.getExecutionUnit() == null) { // happns is last == Root
					sameUnit = false;
				} else {
					lastIndex = lastSelected.getExecutionUnit().getOperators().indexOf(lastSelected);
					if (lastSelected.getExecutionUnit() != op.getExecutionUnit()) {
						sameUnit = false;
					}
				}
			}
			if (sameUnit) {
				int index = op.getExecutionUnit().getOperators().indexOf(op);
				if (lastIndex < index) {
					for (int i = lastIndex + 1; i <= index; i++) {
						selectedOperators.add(op.getExecutionUnit().getOperators().get(i));
					}
				} else if (lastIndex > index) {
					for (int i = lastIndex - 1; i >= index; i--) {
						selectedOperators.add(op.getExecutionUnit().getOperators().get(i));
					}
				}
			}
		} else {
			boolean contains = selectedOperators.contains(op);
			if (op != null) {
				if (!contains) {
					selectedOperators.add(op);
					changed = true;
				} else if (!clear) {
					selectedOperators.remove(op);
					changed = true;
				}
			}
		}
		if (changed) {
			mainFrame.selectOperators(selectedOperators);
		}
		repaint();
	}

	public List<Operator> getSelection() {
		return selectedOperators;
	}

	public void setSelection(List<Operator> selectedOperators) {
		if (!this.selectedOperators.equals(selectedOperators)) {
			this.selectedOperators.clear();
			this.selectedOperators.addAll(selectedOperators);
			repaint();
		}
	}

	private boolean showPopupMenu(final MouseEvent e) {
		if (connectingPortSource != null || connectionDraggingCanceled) {
			return false;
		}
		JPopupMenu menu = new JPopupMenu();

		// port or not port, that is the question
		if (hoveringPort != null) {
			// add port actions
			final IOObject data = hoveringPort.getAnyDataOrNull();
			if (data != null && data instanceof ResultObject) {
				JMenuItem showResult = new JMenuItem(new ResourceAction(true, "show_port_data", ((ResultObject) data).getName()) {

					private static final long serialVersionUID = -6557085878445788274L;

					@Override
					public void actionPerformed(ActionEvent e) {
						data.setSource(hoveringPort.getPorts().getOwner().getOperator().getName());
						mainFrame.getResultDisplay().showResult(((ResultObject) data));
					}

				});
				menu.add(showResult);
				try {
					String locationString = mainFrame.getProcess().getRepositoryLocation().getAbsoluteLocation();
					menu.add(new StoreInRepositoryAction(data, new RepositoryLocation(locationString.substring(0, locationString.lastIndexOf(RepositoryLocation.SEPARATOR)))));
				} catch (Exception e1) {
					menu.add(new StoreInRepositoryAction(data));
				}
				menu.addSeparator();
			}
			List<QuickFix> fixes = hoveringPort.collectQuickFixes();
			if (!fixes.isEmpty()) {
				JMenu fixMenu = new ResourceMenu("quick_fixes");
				for (QuickFix fix : fixes) {
					fixMenu.add(fix.getAction());
				}
				menu.add(fixMenu);
			}
			if (hoveringPort.isConnected()) {
				menu.add(new ResourceAction(true, "disconnect") {

					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						if (hoveringPort != null) {
							if (hoveringPort.isConnected()) {
								if (hoveringPort instanceof OutputPort) {
									((OutputPort) hoveringPort).disconnect();
								} else {
									((InputPort) hoveringPort).getSource().disconnect();
								}
							}
						}
					}
				});
			}

			if (displayedChain instanceof ProcessRootOperator) {
				if (hoveringPort.getPorts() == displayedChain.getSubprocess(0).getInnerSources() ||
						hoveringPort.getPorts() == displayedChain.getSubprocess(0).getInnerSinks()) {
					menu.add(new ConnectPortToRepositoryAction(hoveringPort));
				}
			}
			firePortMenuWillOpen(menu, hoveringPort);
		} else {
			// add operator actions
			mainFrame.getActions().addToOperatorPopupMenu(menu, RENAME_ACTION);

			// if not hovering on operator, add process panel actions
			if (getHoveringOperator() == null) {
				menu.addSeparator();

				JMenu orderMenu = new ResourceMenu("execution_order");
				orderMenu.add(flowVisualizer.ALTER_EXECUTION_ORDER.createMenuItem());
				orderMenu.add(flowVisualizer.SHOW_EXECUTION_ORDER);
				menu.add(orderMenu);

				JMenu layoutMenu = new ResourceMenu("process_layout");

				layoutMenu.add(new ResourceAction("arrange_operators") {

					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent ae) {
						int index = getProcessIndexUnder(e.getPoint());
						if (index == -1) {
							for (ExecutionUnit u : processes) {
								autoArrange(u);
							}
						} else {
							autoArrange(processes[index]);
						}
					}
				});
				layoutMenu.add(AUTO_FIT_ACTION);
				if (hoveringProcessIndex != -1) {
					layoutMenu.add(INCREASE_PROCESS_LAYOUT_WIDTH_ACTION);
					layoutMenu.add(DECREASE_PROCESS_LAYOUT_WIDTH_ACTION);
					layoutMenu.add(INCREASE_PROCESS_LAYOUT_HEIGHT_ACTION);
					layoutMenu.add(DECREASE_PROCESS_LAYOUT_HEIGHT_ACTION);
				}
				menu.add(layoutMenu);

				menu.addSeparator();
				String name = "Process";
				if (displayedChain.getProcess().getProcessLocation() != null) {
					name = displayedChain.getProcess().getProcessLocation().getShortName();
				}
				menu.add(PrintingTools.makeExportPrintMenu(this, name));
				fireOperatorMenuWillOpen(menu, displayedChain);
			}

			if (getHoveringOperator() != null) {
				boolean first = true;
				for (OutputPort port : getHoveringOperator().getOutputPorts().getAllPorts()) {
					final IOObject data = port.getAnyDataOrNull();
					if (data != null && data instanceof ResultObject) {
						if (first) {
							menu.addSeparator();
							first = false;
						}
						JMenuItem showResult = new JMenuItem(new ResourceAction(true, "show_port_data", ((ResultObject) data).getName()) {

							private static final long serialVersionUID = -6557085878445788274L;

							@Override
							public void actionPerformed(ActionEvent e) {
								data.setSource(getHoveringOperator().getName());
								mainFrame.getResultDisplay().showResult(((ResultObject) data));
							}

						});
						menu.add(showResult);
					}
				}
				fireOperatorMenuWillOpen(menu, getHoveringOperator());
			}
		}

		// show popup
		if (menu.getSubElements().length > 0) {
			menu.show(this, e.getX(), e.getY());
		}
		return true;
	}

	private static enum ConnectorShape {
		LINEAR, ORTHOGONAL, SPLINES;
	};

	private final ConnectorShape connectorShape = ConnectorShape.SPLINES;

	private boolean snapToGrid;

	private final OverviewPanel overviewPanel = new OverviewPanel(this);

	private JTextField renameField = null;

	private BufferedImage tutorialImage;

	private Shape createConnector(Port fromPort, Port toPort) {
		Point2D from = getPortLocation(fromPort);
		Point2D to = getPortLocation(toPort);

		from = new Point2D.Double(from.getX() + PORT_SIZE / 2, from.getY());
		to = new Point2D.Double(to.getX() - PORT_SIZE / 2, to.getY());

		int delta = 10;
		switch (connectorShape) {
			case LINEAR:
				return new Line2D.Double(from, to);
			case ORTHOGONAL: {
				GeneralPath connector = new GeneralPath();
				double sourceIndex = fromPort.getPorts().getAllPorts().indexOf(fromPort);
				double destIndex = -toPort.getPorts().getAllPorts().indexOf(toPort);
				int totalNumPorts = Math.max(fromPort.getPorts().getNumberOfPorts(), toPort.getPorts().getNumberOfPorts());
				double freeSpace = to.getX() - from.getX() - 2 * delta;
				double centerX = (from.getX() + to.getX()) / 2 + 0.5 * freeSpace * ((sourceIndex + destIndex + 1d) / totalNumPorts - 0.5);
				connector.moveTo(from.getX() + 1, from.getY());
				if (to.getX() >= from.getX() + 2 * delta) {
					if (to.getY() != from.getY()) {
						connector.lineTo(centerX, from.getY());
						connector.lineTo(centerX, to.getY());
					}
				} else {
					connector.lineTo(from.getX() + delta, from.getY());
					connector.lineTo(from.getX() + delta, (from.getY() + to.getY()) / 2);
					connector.lineTo(to.getX() - delta, (from.getY() + to.getY()) / 2);
					connector.lineTo(to.getX() - delta, to.getY());
				}
				connector.lineTo(to.getX() - 1, to.getY());
				return connector;
			}
			case SPLINES: {
				GeneralPath connector = new GeneralPath();
				connector.moveTo(from.getX() + 1, from.getY());
				double cx = (from.getX() + to.getX()) / 2;
				double cy = (from.getY() + to.getY()) / 2;
				if (to.getX() >= from.getX() + 2 * delta) {
					connector.curveTo(cx, from.getY(), cx, from.getY(), cx, cy);
					connector.curveTo(cx, to.getY(), cx, to.getY(), to.getX() - 1, to.getY());
				} else {
					connector.curveTo(from.getX() + delta, from.getY(), from.getX() + delta, cy, cx, cy);
					connector.curveTo(to.getX() - delta, cy, to.getX() - delta, to.getY(), to.getX() - 1, to.getY());
				}
				return connector;
			}
			// cannot happen
			default:
				return null;
		}
	}

	private OutputPort getPortForConnectorNear(Point p, ExecutionUnit unit) {
		List<OutputPort> candidates = new LinkedList<OutputPort>();
		candidates.addAll(unit.getInnerSources().getAllPorts());
		for (Operator op : unit.getOperators()) {
			candidates.addAll(op.getOutputPorts().getAllPorts());
		}
		Stroke thickStroke = new BasicStroke(5);
		for (OutputPort port : candidates) {
			if (port.isConnected()) {
				Shape connector = createConnector(port, port.getDestination());
				Shape thick = thickStroke.createStrokedShape(connector);
				if (thick.contains(p)) {
					return port;
				}
			}
		}
		return null;
	}

	// private int getDefaultProcessHeight() {
	// if (ORIENTATION == Orientation.Y_AXIS) {
	// return PROCESS_HEIGHT * 2/3;
	// } else {
	// return PROCESS_HEIGHT;
	// }
	// }

	private double getWidth(ExecutionUnit executionUnit) {
		return getWidth(executionUnit, true);
	}

	private void setInitialSizes(ExecutionUnit[] units) {
		Dimension frameSize;
		if (getParent() instanceof JViewport) {
			frameSize = getParent().getSize();
		} else {
			frameSize = getSize();
		}
		for (ExecutionUnit unit : units) {
			Dimension size = processSizes.get(unit);
			if (size == null) {
				size = new Dimension((int) (frameSize.getWidth() / processes.length - WALL_WIDTH * 2), (int) (frameSize.getHeight() - 2 * PADDING));
				processSizes.put(unit, size);
			}
		}
	}

	/**
	 * @param fill
	 *            If true, use all free space in the parent viewport
	 */
	private double getWidth(ExecutionUnit executionUnit, boolean fill) {
		Dimension size = processSizes.get(executionUnit);
		double width = size.getWidth();
		if (fill) {
			if (getParent() instanceof JViewport) {
				double viewportWidth = ((JViewport) getParent()).getWidth();
				double totalWidth = getTotalWidth(false);
				double free = viewportWidth - totalWidth;
				if (free > 0) {
					switch (ORIENTATION) {
						case Y_AXIS:
							width += free;
							break;
						case X_AXIS:
							width += (int) free / processes.length;
							break;
					}
				}
			}
		}
		return width;
	}

	private double getHeight(ExecutionUnit executionUnit) {
		return getHeight(executionUnit, true);
	}

	public double getTotalHeight() {
		return getTotalHeight(true);
	}

	private double getHeight(ExecutionUnit executionUnit, boolean fill) {
		Dimension size = processSizes.get(executionUnit);
		double height = size.getHeight();
		if (fill) {
			if (getParent() instanceof JViewport) {
				double viewportHeight = ((JViewport) getParent()).getHeight();
				double free = viewportHeight - getTotalHeight(false);
				if (free > 0) {
					switch (ORIENTATION) {
						case X_AXIS:
							height += free;
							break;
						case Y_AXIS:
							height += free / processes.length;
							break;
					}
				}
			}
		}
		return height;
	}

	public double getTotalWidth() {
		return getTotalWidth(true);
	}

	private double getTotalWidth(boolean fill) {
		double width = 0;
		for (ExecutionUnit u : processes) {
			double w = getWidth(u, fill) + 2 * WALL_WIDTH;
			switch (ORIENTATION) {
				case X_AXIS:
					width += w;
					break;
				case Y_AXIS:
					if (w > width) {
						width = w;
					}
					break;
			}
		}
		return width;
	}

	private double getTotalHeight(boolean fill) {
		double height = 0;
		for (ExecutionUnit u : processes) {
			double h = getHeight(u, fill) + 2 * PADDING;
			switch (ORIENTATION) {
				case X_AXIS:
					if (h > height) {
						height = h;
					}
					break;
				case Y_AXIS:
					height += h;
					break;
			}
		}
		return height;
	}

	private void setWidth(ExecutionUnit executionUnit, double width) {
		Dimension old = processSizes.get(executionUnit);
		old.setSize(width, old.getHeight());
		updateComponentSize();
	}

	private void setHeight(ExecutionUnit executionUnit, double height) {
		Dimension old = processSizes.get(executionUnit);
		old.setSize(old.getWidth(), height);
		updateComponentSize();
	}

	private void ensureWidth(ExecutionUnit executionUnit, int width) {
		Dimension old = processSizes.get(executionUnit);
		if (width > old.getWidth()) {
			old.setSize(width, old.getHeight());
			balance();
			updateComponentSize();
		}
	}

	private void ensureHeight(ExecutionUnit executionUnit, int height) {
		Dimension old = processSizes.get(executionUnit);
		if (height > old.getHeight()) {
			old.setSize(old.getWidth(), height);
			balance();
			updateComponentSize();
		}
	}

	private Rectangle2D autoPosition(Operator op, int index) {
		int maxPerRow = (int) Math.max(1, Math.floor(getWidth(op.getExecutionUnit(), true) / GRID_AUTOARRANGE_WIDTH));
		int col = index % maxPerRow;
		int row = index / maxPerRow;
		Rectangle2D old = operatorRects.get(op);

		Rectangle2D rect = new Rectangle2D.Double(col * GRID_AUTOARRANGE_WIDTH + GRID_X_OFFSET, GRID_AUTOARRANGE_HEIGHT * row + GRID_Y_OFFSET,
				old != null ? old.getWidth() : OPERATOR_WIDTH,
				old != null ? old.getHeight() : MIN_OPERATOR_HEIGHT);

		setOperatorRect(op, rect);
		return rect;
	}

	private void autoArrange(ExecutionUnit process) {
		Collection<Operator> sorted = process.getOperators();
		int i = 0;
		for (Operator op : sorted) {
			autoPosition(op, i++);
		}
		autoFit(process, true);
		process.getEnclosingOperator().getProcess().updateNotify();
	}

	private void autoFit(ExecutionUnit process, boolean balance) {
		double w = 0;
		double h = 0;
		for (Operator op : process.getOperators()) {
			Rectangle2D bounds = getOperatorRect(op, true);
			if (bounds.getMaxX() > w) {
				w = bounds.getMaxX();
			}
			if (bounds.getMaxY() > h) {
				h = bounds.getMaxY();
			}
		}
		for (Port port : process.getInnerSources().getAllPorts()) {
			h = Math.max(h, getPortLocation(port).getY());
		}
		for (Port port : process.getInnerSinks().getAllPorts()) {
			h = Math.max(h, getPortLocation(port).getY());
		}

		setWidth(process, (int) (w + 3 * PADDING));
		setHeight(process, (int) (h + 3 * PADDING));
		if (balance) {
			balance();
			repaint();
		}
		updateExtensionButtons();
	}

	private void autoFit() {
		for (ExecutionUnit unit : processes) {
			autoFit(unit, false);
		}
		balance();
		repaint();
		updateExtensionButtons();
	}

	private void balance() {
		switch (ORIENTATION) {
			case X_AXIS:
				balanceHeights();
				break;
			case Y_AXIS:
				balanceWidths();
				break;
		}
	}

	private void balanceHeights() {
		double height = 0;
		for (ExecutionUnit p : processes) {
			double h = getHeight(p);
			if (h > height) {
				height = h;
			}
		}
		for (ExecutionUnit p : processes) {
			setHeight(p, height);
		}
	}

	private void balanceWidths() {
		double width = 0;
		for (ExecutionUnit p : processes) {
			double w = getWidth(p);
			if (w > width) {
				width = w;
			}
		}
		for (ExecutionUnit p : processes) {
			setWidth(p, width);
		}
	}

	private void updateComponentSize() {
		Dimension newSize = new Dimension((int) getTotalWidth(), (int) getTotalHeight());
		updateExtensionButtons();
		if (!newSize.equals(getPreferredSize())) {
			setPreferredSize(newSize);
			revalidate();
		}
	}

	int getProcessIndexUnder(Point2D p) {
		switch (ORIENTATION) {
			case X_AXIS:
				if (p.getY() < PADDING || p.getY() > getTotalHeight()) {
					return -1;
				}
				int xOffset = 0;
				for (int i = 0; i < processes.length; i++) {
					xOffset += WALL_WIDTH;
					int relativeX = (int) p.getX() - xOffset;
					if (relativeX >= 0 && relativeX <= getWidth(processes[i])) {
						return i;
					}
					xOffset += WALL_WIDTH + getWidth(processes[i]);
				}
				break;
			case Y_AXIS:
				if (p.getX() < WALL_WIDTH || p.getX() > WALL_WIDTH + getTotalWidth()) {
					return -1;
				}
				int yOffset = 0;
				for (int i = 0; i < processes.length; i++) {
					int relativeY = (int) p.getY() - yOffset;
					if (relativeY >= 0 && relativeY <= getHeight(processes[i])) {
						return i;
					}
					yOffset += PADDING + getHeight(processes[i]);
				}
				break;
		}
		return -1;
	}

	private Point fromProcessSpace(Point p, int processIndex) {
		switch (ORIENTATION) {
			case X_AXIS:
				double xOffset = 0;
				for (int i = 0; i < processes.length; i++) {
					xOffset += WALL_WIDTH;
					if (i == processIndex) {
						return new Point((int) (p.getX() + xOffset), (int) (p.getY() + PADDING));
					}
					xOffset += WALL_WIDTH + getWidth(processes[i]);
				}
				break;
			case Y_AXIS:
				double yOffset = 0;
				for (int i = 0; i < processes.length; i++) {
					if (i == processIndex) {
						return new Point((int) (p.getX() + WALL_WIDTH), (int) (p.getY() + yOffset + PADDING));
					}
					yOffset += PADDING + getHeight(processes[i]);
				}
				break;
		}
		return null;
	}

	public Point toProcessSpace(Point p, int processIndex) {
		switch (ORIENTATION) {
			case X_AXIS:
				int xOffset = 0;
				for (int i = 0; i < processes.length; i++) {
					xOffset += WALL_WIDTH;
					if (i == processIndex) {
						return new Point((int) p.getX() - xOffset, (int) p.getY() - PADDING);
					}
					xOffset += WALL_WIDTH + getWidth(processes[i]);
				}
				break;
			case Y_AXIS:
				int yOffset = 0;
				for (int i = 0; i < processes.length; i++) {
					if (i == processIndex) {
						return new Point((int) p.getX() - WALL_WIDTH, (int) p.getY() - yOffset - PADDING);
					}
					yOffset += PADDING + getHeight(processes[i]);
				}
				break;
		}
		return null;
	}

	private Operator getClosestLeftNeighbour(Point2D p, ExecutionUnit unit) {
		Operator closest = null;
		double minDist = Double.POSITIVE_INFINITY;
		for (Operator op : unit.getOperators()) {
			Rectangle2D rect = getOperatorRect(op, true);
			if (rect.getMaxX() >= p.getX()) {
				continue;
			}
			double dx = rect.getMaxX() - p.getX();
			double dy = rect.getMaxY() - p.getY();
			double dist = dx * dx + dy * dy;
			if (dist < minDist) {
				minDist = dist;
				closest = op;
			}
		}
		return closest;
	}

	private void showStatus(String msg) {
		RapidMinerGUI.getMainFrame().getStatusBar().setSpecialText(msg);
	}

	private void clearStatus() {
		RapidMinerGUI.getMainFrame().getStatusBar().clearSpecialText();
	}

	private void updateCursor() {
		if (getHoveringOperator() != null || hoveringPort != null) {
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		} else {
			setCursor(Cursor.getDefaultCursor());
		}
	}

	private Rectangle2D getSelectionRectangle(Point dragStart, Point2D e) {
		if (dragStart == null) {
			return null;
		}
		return new Rectangle2D.Double(Math.min(dragStart.getX(), e.getX()),
				Math.min(dragStart.getY(), e.getY()),
				Math.abs(dragStart.getX() - e.getX()),
				Math.abs(dragStart.getY() - e.getY()));
	}

	public void processChanged() {
		autoFit();
	}

	public void processUpdated() {
		if (displayedChain.getNumberOfSubprocesses() != processes.length) {
			showOperatorChain(displayedChain);
		}
		repaint();
	}

	private Point snap(Point2D point) {
		int snappedX = (int) point.getX() - GRID_X_OFFSET;
		int factor = (snappedX + GRID_WIDTH / 2) / GRID_WIDTH;
		snappedX /= GRID_WIDTH;
		snappedX = factor * GRID_WIDTH + GRID_X_OFFSET;

		int snappedY = (int) point.getY() - GRID_Y_OFFSET;
		factor = (snappedY + GRID_HEIGHT / 2) / GRID_HEIGHT;
		snappedY /= GRID_HEIGHT;
		snappedY = factor * GRID_HEIGHT + GRID_Y_OFFSET;

		return new Point(snappedX, snappedY);
	}

	private boolean isSnapToGrid() {
		return snapToGrid;
	}

	/** Abbreviates the string using ... if necessary. */
	private String fitString(String string, Graphics2D g, int maxWidth) {
		Rectangle2D bounds = g.getFont().getStringBounds(string, g.getFontRenderContext());
		if (bounds.getWidth() < maxWidth) {
			return string;
		}
		while (g.getFont().getStringBounds(string + "...", g.getFontRenderContext()).getWidth() > maxWidth) {
			if (string.length() == 0) {
				return "...";
			}
			string = string.substring(0, string.length() - 1);
		}
		return string + "...";
	}

	private boolean canBeInsertedIntoConnection(Operator operator) {
		if (operator == null) {
			return false;
		}
		boolean hasFreeInput = false;
		for (InputPort port : operator.getInputPorts().getAllPorts()) {
			if (!port.isConnected()) {
				hasFreeInput = true;
				break;
			}
		}
		if (!hasFreeInput) {
			return false;
		}
		for (OutputPort port : operator.getOutputPorts().getAllPorts()) {
			if (!port.isConnected()) {
				return true;
			}
		}
		return false;
	}

	private void insertIntoHoveringConnection(Operator operator) {
		if (hoveringConnectionSource == null) {
			return;
		}
		InputPort oldDest = hoveringConnectionSource.getDestination();
		oldDest.lock();
		hoveringConnectionSource.lock();
		try {
			// no IndexOutOfBoundsException since checked above
			InputPort bestInputPort = null;
			MetaData md = hoveringConnectionSource.getMetaData();
			if (md != null) {
				for (InputPort inCandidate : operator.getInputPorts().getAllPorts()) {
					if (!inCandidate.isConnected() && inCandidate.isInputCompatible(md, CompatibilityLevel.PRE_VERSION_5)) {
						bestInputPort = inCandidate;
						break;
					}
				}
			} else {
				for (InputPort inCandidate : operator.getInputPorts().getAllPorts()) {
					if (!inCandidate.isConnected()) {
						bestInputPort = inCandidate;
						break;
					}
				}
			}
			if (bestInputPort != null) {
				hoveringConnectionSource.disconnect();
				connect(hoveringConnectionSource, bestInputPort);
				if (mainFrame.VALIDATE_AUTOMATICALLY_ACTION.isSelected()) {
					hoveringConnectionSource.getPorts().getOwner().getOperator().transformMetaData();
					operator.transformMetaData();
				}

				OutputPort bestOutput = null;
				for (OutputPort outCandidate : operator.getOutputPorts().getAllPorts()) {
					if (!outCandidate.isConnected()) {
						md = outCandidate.getMetaData();
						if (md != null && oldDest.isInputCompatible(md, CompatibilityLevel.PRE_VERSION_5)) {
							bestOutput = outCandidate;
							break;
						}
					}
				}
				if (bestOutput == null) {
					for (OutputPort outCandidate : operator.getOutputPorts().getAllPorts()) {
						if (!outCandidate.isConnected()) {
							bestOutput = outCandidate;
							break;
						}
					}
				}
				if (bestOutput != null) {
					connect(bestOutput, oldDest);
				}
			}
		} finally {
			oldDest.unlock();
			hoveringConnectionSource.unlock();
			hoveringConnectionSource = null;
		}
	}

	public OverviewPanel getOverviewPanel() {
		return overviewPanel;
	}

	@Override
	public void repaint() {
		super.repaint();
		if (overviewPanel != null) {
			overviewPanel.repaint();
		}
	}

	private void rename(final Operator op) {
		int processIndex = getIndex(op.getExecutionUnit());
		if (processIndex == -1) {
			String name = SwingTools.showInputDialog("rename_operator", op.getName());
			if (name != null && name.length() > 0) {
				op.rename(name);
			}
			return;
		}
		renameField = new JTextField(10);
		Rectangle2D rect = getOperatorRect(op, false);
		double rollout = nameRolloutInterpolationMap.getValue(op);
		int width = 0;
		if (rollout > 0) {
			width = (int) OPERATOR_FONT.getStringBounds(op.getName(), ((Graphics2D) getGraphics()).getFontRenderContext()).getWidth();
		}
		width = Math.max(width, OPERATOR_WIDTH);

		// if (width > rect.getWidth()) {
		// rect.setFrame(rect.getX(), rect.getY(), width, rect.getHeight());
		// }
		renameField.setText(op.getName());
		renameField.selectAll();
		Point p = fromProcessSpace(new Point((int) rect.getX(), (int) rect.getY()), processIndex);
		renameField.setBounds((int) p.getX(), (int) p.getY() - 4, width + 1, 21);
		// renameField.setFont(renameField.getFont().deriveFont(11f));
		renameField.setFont(OPERATOR_FONT);
		add(renameField);
		renameField.requestFocus();
		// accepting changes on enter and focus lost
		renameField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (renameField != null) {
					String name = renameField.getText().trim();
					if (name.length() > 0) {
						op.rename(name);
					}
					remove(renameField);
					renameField = null;
					repaint();
				}
			}
		});
		renameField.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {}

			@Override
			public void focusLost(FocusEvent e) {
				if (renameField != null) {
					String name = renameField.getText().trim();
					if (name.length() > 0) {
						op.rename(name);
					}
					remove(renameField);
					renameField = null;
					repaint();
				}
			}
		});
		// ignore changes on escape
		renameField.addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					remove(renameField);
					renameField = null;
					repaint();
				}
			}

			@Override
			public void keyTyped(KeyEvent e) {}
		});

		repaint();
	}

	public void setHoveringOperator(Operator hoveringOperator) {
		if (hoveringOperator != this.hoveringOperator) {
			if (this.hoveringOperator != null) {
				nameRolloutInterpolationMap.rollIn(this.hoveringOperator);
			}
			if (hoveringOperator != null) {
				nameRolloutInterpolationMap.rollOut(hoveringOperator);
			}
		}
		this.hoveringOperator = hoveringOperator;
	}

	public Operator getHoveringOperator() {
		return hoveringOperator;
	}

	private void setDropInsertionPredecessor(Operator closestLeftNeighbour) {
		dropInsertionPredecessor = closestLeftNeighbour;
		if (dropInsertionPredecessor != null) {
			mainFrame.getStatusBar().setSpecialText("Operator will be inserted after " + dropInsertionPredecessor);
		} else {
			mainFrame.getStatusBar().setSpecialText("Operator will be inserted as the last operator in this process.");
		}
	}

	private boolean hasConnections(Operator op) {
		for (Port port : op.getInputPorts().getAllPorts()) {
			if (port.isConnected()) {
				return true;
			}
		}
		for (Port port : op.getOutputPorts().getAllPorts()) {
			if (port.isConnected()) {
				return true;
			}
		}
		return false;
	}

	public FlowVisualizer getFlowVisualizer() {
		return flowVisualizer;
	}

	public OperatorChain getDisplayedChain() {
		return displayedChain;
	}

	/** Connects two operators and enables them. */
	private void connect(OutputPort out, InputPort in) {
		Operator inOp = in.getPorts().getOwner().getOperator();
		if (!inOp.isEnabled()) {
			inOp.setEnabled(true);
		}
		Operator outOp = out.getPorts().getOwner().getOperator();
		if (!outOp.isEnabled()) {
			outOp.setEnabled(true);
		}
		out.connectTo(in);
	}

	/** Adds positions of operators etc. */
	private class GUIProcessXMLFilter implements ProcessXMLFilter {

		/** Adds GUI information to the element. */
		@Override
		public void operatorExported(Operator op, Element opElement) {
			Rectangle2D bounds = getOperatorRect(op, false);
			if (bounds != null) {
				opElement.setAttribute("x", "" + (int) bounds.getX());
				opElement.setAttribute("y", "" + (int) bounds.getY());
				opElement.setAttribute("width", "" + (int) bounds.getWidth());
				opElement.setAttribute("height", "" + (int) bounds.getHeight());
			}
		}

		/** Adds GUI information to the element. */
		@Override
		public void executionUnitExported(ExecutionUnit process, Element element) {
			for (Port port : process.getInnerSources().getAllPorts()) {
				Element spacingElement = element.getOwnerDocument().createElement("portSpacing");
				spacingElement.setAttribute("port", "source_" + port.getName());
				spacingElement.setAttribute("spacing", "" + (int) getPortSpacing(port));
				element.appendChild(spacingElement);
			}
			for (Port port : process.getInnerSinks().getAllPorts()) {
				Element spacingElement = element.getOwnerDocument().createElement("portSpacing");
				spacingElement.setAttribute("port", "sink_" + port.getName());
				spacingElement.setAttribute("spacing", "" + (int) getPortSpacing(port));
				element.appendChild(spacingElement);
			}
		}

		/** Extracts GUI information from the XML element. */
		@Override
		public void operatorImported(Operator op, Element opElement) {
			String x = opElement.getAttribute("x");
			String y = opElement.getAttribute("y");
			String w = opElement.getAttribute("width");
			String h = opElement.getAttribute("height");
			if (x != null && x.length() > 0) {
				try {
					setOperatorRect(op, new Rectangle2D.Double(Double.parseDouble(x),
							Double.parseDouble(y),
							Double.parseDouble(w),
							Double.parseDouble(h)));
				} catch (Exception e) {
					// ignore silently
				}
			}
		}

		/** Extracts GUI information from the XML element. */
		@Override
		public void executionUnitImported(ExecutionUnit process, Element element) {
			NodeList children = element.getChildNodes();
			for (Port port : process.getInnerSources().getAllPorts()) {
				for (int i = 0; i < children.getLength(); i++) {
					if (children.item(i) instanceof Element && "portSpacing".equals(((Element) children.item(i)).getTagName())) {
						Element psElement = (Element) children.item(i);
						if (("source_" + port.getName()).equals(psElement.getAttribute("port"))) {
							try {
								portSpacings.put(port, (double) Integer.parseInt(psElement.getAttribute("spacing")));
							} catch (NumberFormatException e) {}
							break;
						}
					}
				}
			}
			for (Port port : process.getInnerSinks().getAllPorts()) {
				for (int i = 0; i < children.getLength(); i++) {
					if (children.item(i) instanceof Element && "portSpacing".equals(((Element) children.item(i)).getTagName())) {
						Element psElement = (Element) children.item(i);
						if (("sink_" + port.getName()).equals(psElement.getAttribute("port"))) {
							try {
								portSpacings.put(port, (double) Integer.parseInt(psElement.getAttribute("spacing")));
							} catch (NumberFormatException e) {}
							break;
						}
					}
				}
			}

		}
	}

	/**
	 * Checks whether we have a port under the given point (in process space)
	 * and, as a side effect, remembers the {@link #hoveringPort}.
	 */
	private boolean checkPortUnder(Ports<? extends Port> ports, int x, int y) {
		for (Port port : ports.getAllPorts()) {
			Point2D location = getPortLocation(port);
			if (location == null) {
				continue;
			}
			int dx = (int) location.getX() - x;
			int dy = (int) location.getY() - y;
			if (dx * dx + dy * dy < 3 * PORT_SIZE * PORT_SIZE / 2) {
				if (hoveringPort != port) {
					hoveringPort = port;
					if (hoveringPort.getPorts().getOwner().getOperator() == displayedChain) {
						showStatus(I18N.getGUILabel("processRenderer.displayChain.port.hover"));
					} else {
						showStatus(I18N.getGUILabel("processRenderer.operator.port.hover"));
					}
					setHoveringOperator(null);
					updateCursor();
					repaint();
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public void dragStarted(Transferable t) {

		// check if transferable can be imported
		if (!canImportTransferable(t)) {
			return;
		}

		DataFlavor[] transferDataFlavors = t.getTransferDataFlavors();
		dragStarted = true;
		for (DataFlavor flavor : transferDataFlavors) {

			if (flavor == TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR) {
				RepositoryLocation location;
				try {

					// get repository location
					location = (RepositoryLocation) t.getTransferData(flavor);

					// check if golf is dragged
					if (location.getAbsoluteLocation().equals("//Samples/data/Golf")) {
						onGolfDataDragged();
					}
				} catch (UnsupportedFlavorException e) {} catch (IOException e) {}
			}

			if (flavor == TransferableOperator.LOCAL_TRANSFERRED_OPERATORS_FLAVOR) {
				Operator[] operators;
				try {
					operators = (Operator[]) t.getTransferData(flavor);

					// we assume that only one operator can be dragged at a time
					Operator draggedOperator = operators[0];

					// check if decision tree is dragged
					if (draggedOperator instanceof DecisionTreeLearner) {
						onDecisionTreeDragged();
					}
				} catch (UnsupportedFlavorException e) {} catch (IOException e) {}
			}
		}
		repaint();
	}

	@Override
	public void dragEnded() {
		dragStarted = false;
		repaint();
	}

	private void setImportDragged(boolean importDragged) {
		this.importDragged = importDragged;
		repaint();
	}

	private boolean getImportDragged() {
		return importDragged;
	}

	private boolean canImportTransferable(Transferable t) {
		for (DataFlavor flavor : t.getTransferDataFlavors()) {

			// check if folder is being dragged. Folders cannot be dropped on the process panel
			if (flavor == TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR) {
				RepositoryLocation location;
				try {

					// get repository location
					location = (RepositoryLocation) t.getTransferData(flavor);

					// locate entry
					Entry locateEntry = location.locateEntry();

					// if entry is folder, return false
					if (locateEntry instanceof Folder) {
						return false;
					}

				} catch (UnsupportedFlavorException e) {} catch (IOException e) {} catch (RepositoryException e) {}
			}
		}

		return true;
	}
		
	/** Adds a listener that will be informed when the user right-clicks an operator or a port. */
	public void addProcessInteractionListener(ProcessInteractionListener l) {
		processInteractionListeners.add(l);
	}
	
	/**
	 * @see #addProcessInteractionListener(ProcessInteractionListener)
	 */
	public void removeProcessInteractionListener(ProcessInteractionListener l) {
		processInteractionListeners.remove(l);
	}
	
	private void fireOperatorMenuWillOpen(JPopupMenu m, Operator op) {
		List<ProcessInteractionListener> copy = new LinkedList<ProcessInteractionListener>(processInteractionListeners);
		for (ProcessInteractionListener l : copy) {
			l.operatorContextMenuWillOpen(m, op);
		}
	}
	
	private void firePortMenuWillOpen(JPopupMenu m, Port port) {
		List<ProcessInteractionListener> copy = new LinkedList<ProcessInteractionListener>(processInteractionListeners);
		for (ProcessInteractionListener l : copy) {
			l.portContextMenuWillOpen(m, port);
		}
	}

	private void fireOperatorMoved(Operator op) {
		List<ProcessInteractionListener> copy = new LinkedList<ProcessInteractionListener>(processInteractionListeners);
		for (ProcessInteractionListener l : copy) {
			l.operatorMoved(op);
		}
	}

	private void fireDisplayedChainChanged(OperatorChain op) {
		List<ProcessInteractionListener> copy = new LinkedList<ProcessInteractionListener>(processInteractionListeners);
		for (ProcessInteractionListener l : copy) {
			l.displayedChainChanged(op);
		}
	}
}
