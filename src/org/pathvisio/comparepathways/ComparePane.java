// Pathway Comparison plug-in,
// a PathVisio plug-in for comparing 2 pathways based on Datanodes and the interaction between Datanodes
// Copyright 2006-2011 BiGCaT Bioinformatics
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package org.pathvisio.comparepathways;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.pathvisio.core.Engine;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.MLine;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.util.ProgressKeeper;
import org.pathvisio.core.view.VPathway;
import org.pathvisio.gui.ProgressDialog;
import org.pathvisio.gui.SwingEngine;
import org.pathvisio.gui.view.VPathwaySwing;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * This class defines the UI layout for the 'Compare' tab
 *  and has the necessary methods and Listener definitions
 *  @author Praveen Kumar 
 */
public class ComparePane extends JPanel 
{
	private Engine engine;
	private SwingEngine swingEngine;
	
	private final Pathway pathwayArr[] = new Pathway[2];
	private final VPathway vPathwayArr[] = new VPathway[2];
	
	private final JTextField textField_pathwayPath_arr[] = {new JTextField(), new JTextField()};
	private final JButton button_loadPathway1 = new JButton("Load Pathway1"),
		button_loadPathway2 = new JButton("Load Pathway2"), comparePathwaysButton = new JButton("Compare Pathways"),
		button_saveResults = new JButton("Save Comparison Results");
	private JTable statsTable;
	
	private ComparePopup comparePopup;
	
	public ComparePane(SwingEngine swingEngine, ComparePopup comparePopup)
	{
		this.swingEngine = swingEngine;
		this.engine = swingEngine.getEngine();
		this.comparePopup = comparePopup;
		createComparePanel();
	}
	
	/**Lays out the UI for the 'Compare' tab*/
	private void createComparePanel()
	{
		textField_pathwayPath_arr[0].setBackground(Color.WHITE);
		textField_pathwayPath_arr[0].setEditable(false);
		button_loadPathway1.addActionListener(jButtonsCommonActionListner);
		
		textField_pathwayPath_arr[1].setBackground(Color.WHITE);
		textField_pathwayPath_arr[1].setEditable(false);
		button_loadPathway2.addActionListener(jButtonsCommonActionListner);
	
		comparePathwaysButton.setEnabled(false);
		comparePathwaysButton.addActionListener(jButtonsCommonActionListner);		

		button_saveResults.setEnabled(false);
		button_saveResults.addActionListener(jButtonsCommonActionListner);
		
		FormLayout layout = new FormLayout(
				"4dlu, pref, 4dlu, fill:pref:grow, 4dlu, pref, 4dlu",
				"4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu");
		CellConstraints cc = new CellConstraints();
		Border etch = BorderFactory.createEtchedBorder();
		
		JPanel comparePanel = new JPanel();
		comparePanel.setLayout (layout);
		comparePanel.setBorder (BorderFactory.createTitledBorder (etch, "Compare Pathways"));
		comparePanel.add (textField_pathwayPath_arr[0], cc.xyw(2,6,3));
		comparePanel.add(button_loadPathway1, cc.xy(6,6));
		comparePanel.add(textField_pathwayPath_arr[1], cc.xyw(2,10,3));
		comparePanel.add(button_loadPathway2, cc.xy(6,10));
		comparePanel.add(comparePathwaysButton, cc.xyw(1,12,5, "center, top"));
		comparePanel.add(button_saveResults, cc.xyw(6, 12, 1, "center, top"));
		
		DefaultTableModel dtm = new CustomTabelModel();
		dtm.setDataVector(new String[][]{
				{"Datanodes total in Pathway 1", "  -"},
				{"Datanodes total in Pathway 2", "  -"},
				{"Datanode Matches", "  -"},
				{"Score ", "  -"},
				{"Interactions total in Pathway 1", "  -"},
				{"Interactions total in Pathway 2", "  -"},
				{"Interaction Matches", "  -"},
				{"Score ", "  -"} },
				new String[]{"Property", "Value"});
		statsTable = new JTable(dtm);
		JPanel statsPanel = new JPanel();
		statsPanel.setBorder (BorderFactory.createTitledBorder (etch, "Comparison Statistics"));
		statsPanel.setLayout(new BorderLayout());
		statsPanel.add(statsTable.getTableHeader(),BorderLayout.NORTH);
		statsPanel.add(statsTable);
		
		this.setLayout(new BorderLayout());
		this.add(comparePanel, BorderLayout.NORTH);
		this.add(statsPanel);
	}
	
	//<---------------Listeners start--------------->
	/**
	 * common ActionListener for the  JButtons: 'Compare', 'Load Pathway1', 'Load Pathway2' 
	 * and 'Save Comparison Results'
	 */
	private ActionListener jButtonsCommonActionListner = new ActionListener()
	{
		public void actionPerformed (ActionEvent ae) 
		{
			JButton button = (JButton)ae.getSource();
			// handling "Load Pathway" buttons' events	
			if(button == button_loadPathway1 || button == button_loadPathway2)
			{
				handleLoadPathwayAction(button);
			}
			// handling "Compare" button events
			else if (button == comparePathwaysButton)
			{
				handleComparePathwayAction();
			}
			
			else if (button == button_saveResults)
			{
				handleSaveCompResultsAction();				
			}
		}
	};
	
	//<------------------Listeners end------------------->
	
	/**	
	 * loads {@link Pathway} (the Data Model) and 
	 * {@link VPathway} (the Graphics corresponding to the Data Model) objects through
	 * a custom fileChooser method defined in {@link Utility} class 
	 * 
	 * @param button The {@link JButton} on which the click event occurred, can be either 'Load Pathway 1' or 'Load Pathway 2' button
	 */
	private void handleLoadPathwayAction(JButton button) 
	{
		File pathwayFile = Utility.showLoadPathwayDialog(swingEngine.getApplicationPanel(), button.getText());
		if(pathwayFile == null) /* if the user cancels the "Load Pathway" dialog, do not proceed further*/
			return;
		
		byte pathwayIndex = (button == button_loadPathway1 ) ? (byte) 0: (byte)1;
		boolean loadPathwaySuccess = loadPathway(pathwayIndex, pathwayFile);
		
		if(loadPathwaySuccess)
		{
			//pathField related settings on loading a pathway
			textField_pathwayPath_arr[pathwayIndex].setText(pathwayFile.getAbsolutePath());
			textField_pathwayPath_arr[pathwayIndex].setToolTipText(pathwayFile.getAbsolutePath());
			textField_pathwayPath_arr[pathwayIndex].setCaretPosition(0);
			comparePopup.getInternalFrameArr()[pathwayIndex].setTitle( "Pathway#" + (pathwayIndex+1) + " ( " + pathwayFile.getName() + " )" );
		} else 
		{
			//reset the pathField to default state
			textField_pathwayPath_arr[pathwayIndex].setText(null);
			textField_pathwayPath_arr[pathwayIndex].setToolTipText(null);
		}
		
		// Compare Button is enabled only after both the pathways are loaded 
		if(vPathwayArr[0] != null && vPathwayArr[1] != null)
			comparePathwaysButton.setEnabled(true);
		else
			comparePathwaysButton.setEnabled(false);
	}
	
	/** 
	 * handles click event on 'Compare Pathways' button. 
	 * Pops-up a progress-bar while it does pathways comparison in the background.   
	 * 
	 * @returns true if pathway-comparison succeeds 
	 */
	private boolean handleComparePathwayAction()
	{
		//If BridgeDB IDMapperStack is empty, then do not proceed further
		if(swingEngine.getGdbManager().getCurrentGdb().getSize() == 0)
		{
			JOptionPane.showMessageDialog(null, "Please select a biological database first.", 
					"Note", JOptionPane.PLAIN_MESSAGE);
			return false;
		}
		
		final ProgressKeeper progressKeeper = new ProgressKeeper();
		final ProgressDialog progressDialog = new ProgressDialog(null,"Comparison plug-in", progressKeeper, false, true);
		
		SwingWorker<ComparisonResults, Void> swingWorker = new SwingWorker<ComparisonResults, Void>() 
		{
			protected ComparisonResults doInBackground() 
			{
				progressKeeper.setTaskName("Comparing Pathway");
				//reset already highlighted nodes in both pathways before comparison
				vPathwayArr[0].resetHighlight();
				vPathwayArr[1].resetHighlight();
				
				//populating pathway 1 and 2's info related to Pathway and VPathway object 
				//in a single class object 'PathwaysInfo' and communicating this to other classes.  
				PathwaysInfo pathwaysInfo = PathwaysInfo.getStaticInstance();
				pathwaysInfo.setPathwaysInfo(pathwayArr, vPathwayArr); 
				
				PathwayElementComparision pwElemComp = new PathwayElementComparision(pathwaysInfo, swingEngine.getGdbManager().getCurrentGdb());
				List<List<PathwayElement>> dataNodeCompResults = pwElemComp.compareDataNodes();
				
				InteractionsComparison interactionsComp = new InteractionsComparison(pathwaysInfo, dataNodeCompResults);
				List<List<Set<PathwayElement>>> interactionCompResults = interactionsComp.compareInteractions();
				
				//Populating the statistics table with data from comparison results
				//datanode comparison
				DecimalFormat decFormat = new DecimalFormat("#.##");
				statsTable.getModel().setValueAt("  " + pwElemComp.getDNcountInPw1(), 0, 1);
				statsTable.getModel().setValueAt("  " + pwElemComp.getDNcountInPw2(), 1, 1);
				statsTable.getModel().setValueAt("  " + dataNodeCompResults.size(), 2, 1);
				statsTable.getModel().setValueAt("  " + decFormat.format
						( dataNodeCompResults.size() * 2.0 / (pwElemComp.getDNcountInPw1() + pwElemComp.getDNcountInPw2()) ) , 3, 1);
				
				//interactions comparison
				statsTable.getModel().setValueAt("  " + interactionsComp.getInteractionsCountInPw1(), 4, 1);
				statsTable.getModel().setValueAt("  " + interactionsComp.getInteractionsCountInPw2(), 5, 1);
				statsTable.getModel().setValueAt("  " + interactionCompResults.size(), 6, 1);
				statsTable.getModel().setValueAt("  " + decFormat.format
						( interactionCompResults.size() * 2.0 / (interactionsComp.getInteractionsCountInPw1() + interactionsComp.getInteractionsCountInPw2()) ) , 7, 1);
				
				ComparisonResults.getStaticInstance().setComparisonResults(dataNodeCompResults, interactionCompResults);
				
				progressKeeper.finished();
				
				if(!button_saveResults.isEnabled())
				{
					button_saveResults.setEnabled(true);
				}
				return ComparisonResults.getStaticInstance();
			}
		};

		swingWorker.execute();
		progressDialog.setVisible(true);
		try 
		{
			ComparisonResults comparisonResults = swingWorker.get();
			comparePopup.displayPathwayComparisonInNewWindow(PathwaysInfo.getStaticInstance(), comparisonResults);
			return true;
		} catch (ExecutionException e){
			swingEngine.handleConverterException("Exception during Pathway comparison", null, e.getCause());
			return false;
		} catch (InterruptedException e){
			swingEngine.handleConverterException("Pathway comparison was cancelled or interrupted", null, e);
			return false;
		}
	}
	
	/**
	 * custom loading of {@link Pathway} and {@link VPathway} objects from the input GPML file: 
	 * Constructing a Pathway object from GPML file and then modeling VPathway from this Pathway object. 
	 * 
	 * @param pathwayIndex This index indicates which pathway information (0: Pathway1, 1: Pathway2) to use
	 * @param pathwayFile This is the gpml/xml pathway file picked through the file-chooser dialog. 
	 * It is used in constructing the Pathway object.
	 * 
	 * @return Returns true if the file chosen from the file-chooser dialog 
	 * loaded up the Pathway and VPathway objects successfully
	 */
	private boolean loadPathway(final byte pathwayIndex, final File pathwayFile) 
	{
		pathwayArr[pathwayIndex] = new Pathway();
		vPathwayArr[pathwayIndex] = null;
		
		final ProgressKeeper progressKeeper = new ProgressKeeper();
		final ProgressDialog progressDialog = new ProgressDialog(null,"Comparison plug-in", progressKeeper, false, true);
		SwingWorker<Boolean, Void> swingWorker = new SwingWorker<Boolean, Void>() 
		{
			protected Boolean doInBackground() 
			{
				progressKeeper.setTaskName("Loading Pathway");
				try 
				{
					pathwayArr[pathwayIndex].readFromXml(pathwayFile, false);
					VPathwaySwing wrapper = new VPathwaySwing(comparePopup.getjScrollPaneArr()[pathwayIndex]);
					vPathwayArr[pathwayIndex] = wrapper.createVPathway();
					vPathwayArr[pathwayIndex].fromModel(pathwayArr[pathwayIndex]);
					return true;
					
				} catch (ConverterException e) {
					swingEngine.handleConverterException(e.getMessage(), null, e);
					return false;
				
				} finally {
					progressKeeper.finished();
				}
			}
		};

		swingWorker.execute();
		progressDialog.setVisible(true);
		try 
		{
			return swingWorker.get();
		} catch (ExecutionException e){
			swingEngine.handleConverterException("Exception occured during Pathway conversion into GPML", null, e.getCause());
			return false;
		} catch (InterruptedException e) {
			swingEngine.handleConverterException("Pathway conversion was cancelled or interrupted", null, e);
			return false;
		}
	}
	
	/**
	 * Handles click event on the button 'Save Comparison Results' and saves pathway-comparison results.  
	 * A file chooser dialog pops-up to allow the user to choose 
	 * the name and location of the files in which to save the data. 
	 */
	private void handleSaveCompResultsAction()
	{
		List<List<List<PathwayElement>>> dnCompResultsParsed = ComparisonResults.getStaticInstance().getDNCompResultsParsed();
		List<List<Set<PathwayElement>>> interactionCompResults = ComparisonResults.getStaticInstance().getInteractionCompResults();
		
		if(dnCompResultsParsed.isEmpty() && interactionCompResults.isEmpty()) 
		{
			JOptionPane.showMessageDialog(null, " Comparison Results empty ", 
					"Pathway Comparison plug-in", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		File chosenFile = Utility.showSaveResultsDialog(swingEngine.getApplicationPanel());
		if(chosenFile == null)
			return;
		
		final String DELIMITER_BW_DN = ", ";
		//final String DELIMITER_BW_INTERACTION = ", ";
		final String DELIMITER_BW_MATCHES = " <---> ";
		
		try 
		{
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(chosenFile));
			bufferedWriter.append("Datanode Comparison Reuslts: to the left are Datanode-Labels in Pathway1 and to the right are their matching counterparts in Pathway2");
			bufferedWriter.newLine();
			bufferedWriter.append("----------------------------");
			bufferedWriter.newLine();
			
			button_saveResults.setEnabled(false);
			
			if(dnCompResultsParsed.isEmpty())
			{
				bufferedWriter.append("None of the Datanodes match");
				bufferedWriter.newLine();
			}
			
			StringBuilder stringBuilder = new StringBuilder();
			for(List<List<PathwayElement>> eachDNMatch : dnCompResultsParsed)
			{
				stringBuilder.delete(0, stringBuilder.length());
				for(PathwayElement eachDN : eachDNMatch.get(0))
				{
					stringBuilder.append( DELIMITER_BW_DN + eachDN.getTextLabel() + 
							" (" + eachDN.getXref().toString() + ")" );	
				}
				stringBuilder.append(DELIMITER_BW_MATCHES);
				for(PathwayElement eachDN : eachDNMatch.get(1))
				{
					stringBuilder.append(eachDN.getTextLabel() + 
							" (" + eachDN.getXref().toString() + ")"  + DELIMITER_BW_DN);
				}
				bufferedWriter.append(stringBuilder.substring(2,stringBuilder.length()-2));
				bufferedWriter.newLine();
			}
			
			bufferedWriter.newLine();
			bufferedWriter.append("Interaction Comparison Reuslts: Datanodes are represented by their text-labels and MLine(<Line-style>) indicates that its a PathwayElement of type MLine");
			bufferedWriter.newLine();
			bufferedWriter.append("-------------------------------");
			bufferedWriter.newLine();
			
			if(interactionCompResults.isEmpty())
			{
				bufferedWriter.append("None of the Interactions match");
			}
			
			for(List<Set<PathwayElement>> eachInteractionMatch : interactionCompResults)
			{
				stringBuilder.delete(0, stringBuilder.length());
				for(PathwayElement eachDnOrMLine : eachInteractionMatch.get(0))
				{
					if(! (eachDnOrMLine instanceof MLine) )
					{
						stringBuilder.append(DELIMITER_BW_DN + eachDnOrMLine.getTextLabel() +
								" (" + eachDnOrMLine.getXref().toString() + ")");
					} else 
					{
						stringBuilder.append(DELIMITER_BW_DN + "MLine(" + eachDnOrMLine.getLineStyle() + ")");
					}
				}
				
				stringBuilder.append(DELIMITER_BW_MATCHES);
				for(PathwayElement eachDnOrMLine : eachInteractionMatch.get(1))
				{
					if(! (eachDnOrMLine instanceof MLine) )
					{
						stringBuilder.append(eachDnOrMLine.getTextLabel() +
								" (" + eachDnOrMLine.getXref().toString() + ")" + DELIMITER_BW_DN );
					} else 
					{
						stringBuilder.append("MLine(" + eachDnOrMLine.getLineStyle() + ")" + DELIMITER_BW_DN);
					}
				}
				bufferedWriter.append(stringBuilder.substring(DELIMITER_BW_DN.length(), stringBuilder.length() - DELIMITER_BW_DN.length()));
				bufferedWriter.newLine();
			}
			
			bufferedWriter.close();
			
			//dialog which pops up after saving the comparison results in a text file  
			Object[] options = { "Ok", "Open saved file" };
			int selection = JOptionPane.showOptionDialog(null, "Comparison Results saved in '" + chosenFile + "'",
					"Info", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
			if (selection != JOptionPane.OK_OPTION)
            {
				Runtime.getRuntime().exec("notepad.exe " + chosenFile);
            }
			
			button_saveResults.setEnabled(true);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			JOptionPane.showMessageDialog(null, "Error while saving Comparison Results: " + e.getMessage(), 
					"Pathway Comparison plug-in", JOptionPane.ERROR_MESSAGE);
			button_saveResults.setEnabled(true);
			e.printStackTrace();
		}
	}
}
