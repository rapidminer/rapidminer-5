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
package com.rapidminer.gui.actions;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileOutputStream;

import javax.swing.JPanel;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.LinkAndBrushChartPanel;
import com.rapidminer.gui.new_plotter.gui.ChartConfigurationPanel;
import com.rapidminer.gui.new_plotter.templates.PlotterTemplate;
import com.rapidminer.gui.plotter.PlotterPanel;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;

/** Action to create a PDF file from the given {@link Component}.
 * 
 * @author Marco Boeck
 *
 */
public class ExportPdfAction extends ResourceAction {

	private PlotterTemplate template;
	private Component component;
	private final String componentName;

	public ExportPdfAction(Component component, String componentName) {
		super(true, "export_pdf", componentName);
		this.component = component;
		this.componentName = componentName;
	}

	public ExportPdfAction(PlotterTemplate template) {
		super(true, "export_pdf", template.getChartType());
		this.componentName = template.getChartType();
		this.template = template;
	}

	private static final long serialVersionUID = 1L;

	@Override
	public void actionPerformed(ActionEvent e) {
		if (component != null) {
			createPdf(component);
			return;
		}
		if (template != null) {
			createPdf(template);
			return;
		}
	}

	/**
	 * Create the PDF from a {@link Component}.
	 * 
	 * @param component
	 */
	private void createPdf(Component component) {
		if (component == null) {
			return;
		}

		// prompt user for pdf location
		File file = promptForPdfLocation();
		if (file == null) {
			return;
		}

		try {
			// create pdf document
			Document document = new Document(PageSize.A4);
			PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
			document.open();
			PdfContentByte cb = writer.getDirectContent();
			createPdfViaTemplate(component, document, cb);
			document.close();
		} catch (Exception e) {
			SwingTools.showSimpleErrorMessage("cannot_export_pdf", e, e.getMessage());
		}
	}

	/**
	 * Create the PDF from a {@link PlotterTemplate}.
	 * 
	 * @param template
	 */
	private void createPdf(PlotterTemplate template) {
		if (template == null) {
			return;
		}

		// prompt user for pdf location
		File file = promptForPdfLocation();
		if (file == null) {
			return;
		}

		try {
			// create pdf document
			Document document = new Document(PageSize.A4);
			PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
			document.open();
			PdfContentByte cb = writer.getDirectContent();
			createPdfViaTemplate(template, document, cb);
			document.close();
		} catch (Exception e) {
			SwingTools.showSimpleErrorMessage("cannot_export_pdf", e, e.getMessage());
		}
	}

	/**
	 * Prompts the user for the location of the .pdf file.
	 * Will append .pdf if file does not end with it.
	 * 
	 * @return
	 */
	private File promptForPdfLocation() {
		// prompt user for pdf location
		File file = SwingTools.chooseFile(RapidMinerGUI.getMainFrame(), "export_pdf", null, false, false, new String[] { "pdf" }, new String[] { "PDF" }, false);
		if (file == null) {
			return null;
		}
		if (!file.getName().endsWith(".pdf")) {
			file = new File(file.getAbsolutePath() + ".pdf");
		}
		// prompt for overwrite confirmation
		if (file.exists()) {
			int returnVal = SwingTools.showConfirmDialog("export_pdf", ConfirmDialog.YES_NO_OPTION, file.getName());
			if (returnVal == ConfirmDialog.NO_OPTION) {
				return null;
			}
		}
		return file;
	}

	/**
	 * Creates a pdf showing the given {@link Component} via {@link PdfTemplate} usage.
	 * @param component
	 * @param document
	 * @param cb
	 * @throws DocumentException
	 */
	private void createPdfViaTemplate(Component component, Document document,PdfContentByte cb) throws DocumentException {
		PdfTemplate tp = cb.createTemplate(500, PageSize.A4.getHeight()/2);
		Graphics2D g2 = tp.createGraphics(500, PageSize.A4.getHeight()/2);

		// special handling for charts as we only want to export the chart but not the control panel
		// chart cannot be scaled to size of component because otherwise we would break the chart aspect-ratio
		if (component.getClass().isAssignableFrom(JPanel.class)) {
			JPanel panel = (JPanel) component;
			if (panel.getLayout().getClass().isAssignableFrom(CardLayout.class)) {
				for (final Component comp : panel.getComponents()) {
					// iterate over all card components and see if there is a chart which would require special handling
					// if not we don't do anything in this loop and do the standard behavior at the bottom of the method
					if (comp.isVisible() && ChartConfigurationPanel.class.isAssignableFrom(comp.getClass())) {
						final ChartConfigurationPanel chartConfigPanel = (ChartConfigurationPanel) comp;

						// create new LinkAndBrushChartPanel with double buffering set to false to get vector graphic export
						// The real chart has to use double buffering for a) performance and b) zoom rectangle drawing
						LinkAndBrushChartPanel newLaBPanel = new LinkAndBrushChartPanel(chartConfigPanel.getPlotEngine().getChartPanel().getChart(), chartConfigPanel.getPlotEngine().getChartPanel().getWidth(), chartConfigPanel.getPlotEngine().getChartPanel().getHeight(), chartConfigPanel.getPlotEngine().getChartPanel().getMinimumDrawWidth(), chartConfigPanel.getPlotEngine().getChartPanel().getMinimumDrawHeight(), false, false);
						newLaBPanel.setSize(chartConfigPanel.getPlotEngine().getChartPanel().getSize());
						newLaBPanel.setOverlayList(chartConfigPanel.getPlotEngine().getChartPanel().getOverlayList());
						AffineTransform at = new AffineTransform();
						double factor = 500d / chartConfigPanel.getPlotEngine().getChartPanel().getWidth();
						at.scale(factor, factor);
						g2.transform(at);
						newLaBPanel.print(g2);
						g2.dispose();
						document.add(new Paragraph(componentName));
						document.add(Image.getInstance(tp));

						return;
					} else if (comp.isVisible() && PlotterPanel.class.isAssignableFrom(comp.getClass())) {
						// special case for PlotterPanel as the Panel itself is wider than the plotter
						// not having a special case here results in the exported image being too wide (empty space to the left)
						final PlotterPanel plotterPanel = (PlotterPanel) comp;

						AffineTransform at = new AffineTransform();
						double factor = 500d / plotterPanel.getPlotterComponent().getWidth();
						at.scale(factor, factor);
						g2.transform(at);
						plotterPanel.print(g2);
						g2.dispose();
						document.add(new Paragraph(componentName));
						document.add(Image.getInstance(tp));

						return;
					}
				}
			}
		}

		AffineTransform at = new AffineTransform();
		double factor = 500d / component.getWidth();
		at.scale(factor, factor);
		g2.transform(at);
		component.print(g2);
		g2.dispose();
		document.add(new Paragraph(componentName));
		document.add(Image.getInstance(tp));
	}

	/**
	 * Creates a pdf showing the given {@link PlotterTemplate} via {@link PdfTemplate} usage.
	 * @param template
	 * @param document
	 * @param cb
	 * @throws DocumentException
	 */
	private void createPdfViaTemplate(PlotterTemplate template, Document document,PdfContentByte cb) throws DocumentException {
		PdfTemplate tp = cb.createTemplate(500, PageSize.A4.getHeight()/2);
		Graphics2D g2 = tp.createGraphics(500, PageSize.A4.getHeight()/2);
		AffineTransform at = new AffineTransform();
		double factor = 500d / template.getPlotEngine().getChartPanel().getWidth();
		at.scale(factor, factor);
		g2.transform(at);
		template.getPlotEngine().getChartPanel().print(g2);
		g2.dispose();
		document.add(new Paragraph(componentName));
		document.add(Image.getInstance(tp));
	}
}
