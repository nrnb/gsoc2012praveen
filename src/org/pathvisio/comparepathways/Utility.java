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

import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.util.List;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;

import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.preferences.GlobalPreference;
import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.core.view.VPathway;
import org.pathvisio.core.view.VPathwayElement;

/**
 * This is the utility class where all the commonly used functions and constants are defined.
 * This java file also has other supporting non-inner classes: ComparisonResults, PathwaysInfo and CustomTabelModel. 
 * @author Praveen Kumar
 */
public class Utility 
{
	/*private static Utility utility = new Utility();
	// Getter method for getting an instance of this class. No need to create an instance every single time.  
	public static Utility getUtilityInstance() 
	{
		return utility;
	}*/

	/**
	 * Highlights a PathwayElement (a node) in the pathway and can also adjust the scroller 
	 * to focus on to the the highlighted node.
	 * 
	 * @param pwe The {@link PathwayElement} to highlight
	 * @param vPathway The {@link VPathway} object which contains the PathwayElement
	 * @param shouldFocus set this true to also focus (adjust the scrollers) on to the highlighted Node
	 */
	public static void highlightPWE(PathwayElement pwe, VPathway vPathway, boolean shouldFocus) 
	{
		VPathwayElement vPwe = vPathway.getPathwayElementView(pwe);
		vPwe.highlight(Color.GREEN);

		//focus on to the highlighted PathwayElement by adjusting the scroll bars
		if(shouldFocus)
			vPathway.getWrapper().scrollCenterTo((int)vPwe.getVBounds().getCenterX(), 
					(int)vPwe.getVBounds().getCenterY());
	}

	/**
	 * Opens a file-chooser dialog for choosing a pathway (GPML / XML file) to load before comparison 
	 *
	 * @param parent The parent {@link Component} for the dialog
	 * @param labelToUse The {@link String} to use on the dialog's buttons and for the title
	 *
	 * @return The {@link File} picked by the user using this dialog
	 */
	public static File showLoadPathwayDialog(Component parent, String labelToUse)
	{
		//Open file dialog
		JFileChooser jfc = new JFileChooser();
		jfc.setAcceptAllFileFilterUsed(false);
		jfc.setDialogTitle(labelToUse);
		jfc.setDialogType(JFileChooser.OPEN_DIALOG);
		jfc.setCurrentDirectory(
				PreferenceManager.getCurrent().getFile(GlobalPreference.DIR_LAST_USED_OPEN));

		jfc.addChoosableFileFilter(new FileFilter() 
		{
			public boolean accept(File f) 
			{
				if(f.isDirectory())
					return true;
				
				String ext = f.toString().substring(f.toString().lastIndexOf(".")+1);
				if(ext.equalsIgnoreCase("xml") || ext.equalsIgnoreCase("gpml")) 
				{
					return true;
				}
				
				return false;
			}
			
			public String getDescription() 
			{
				return "GPML files (*.gpml, *.xml)";
			}
		});

		int status = jfc.showDialog(parent, labelToUse);
		if(status == JFileChooser.APPROVE_OPTION) 
		{
			File f = jfc.getSelectedFile();
			PreferenceManager.getCurrent().setFile(GlobalPreference.DIR_LAST_USED_OPEN,
					jfc.getCurrentDirectory());
			if(!(f.toString().toUpperCase().endsWith("GPML") || f.toString().toUpperCase().endsWith("XML")))
			{
				f = new File(f.toString() + ".gpml");
			}
			return f;
		}
		return null;
	}
	
	/**
	 * Opens a file-chooser dialog, where the user chooses a location and name of the text file 
	 * in which to save the results.  
	 * 
	 * @param parent The parent {@link Component} for the dialog
	 * 
	 * @return The {@link File} chosen by the user using this dialog
	 */
	public static File showSaveResultsDialog(Component parent) 
	{
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setDialogTitle("Save comparision results");
		fileChooser.setCurrentDirectory(
				PreferenceManager.getCurrent().getFile(GlobalPreference.DIR_LAST_USED_SAVE));

		fileChooser.addChoosableFileFilter(new FileFilter(){  
			String description = "Text Files(*.txt)";
			
			public String getDescription() 
			{  
				return description;  
			}  

			public boolean accept(File f) 
			{  
				if(f == null) 
					return false;
				
				if(f.isDirectory()) 
					return true;  
				
				return f.getName().toLowerCase().endsWith(".txt");  
			}
		});  

		int fileChooserResponse = fileChooser.showSaveDialog(parent);
		if(fileChooserResponse == JFileChooser.APPROVE_OPTION) 
		{
			File chosenFile = fileChooser.getSelectedFile();
			if(!chosenFile.getName().contains(".txt")) 
			{
				chosenFile = new File( fileChooser.getSelectedFile( ) + ".txt");
			}
			
			if(chosenFile.exists()) 
			{
				int overwriteDialogResp = JOptionPane.showConfirmDialog(fileChooser, 
						"File: '" + chosenFile.getName() + "' already exits. Do you want to overwrite ?");
				if (overwriteDialogResp == JOptionPane.YES_OPTION) 
				{
				}
				else if (overwriteDialogResp == JOptionPane.NO_OPTION) 
				{
					showSaveResultsDialog(parent);
					return null;
				}				
				else //for if the user clicks on cancel or close button of this dialog
				{
					return null;
				}
			}
			//reaches here in either of the 2 cases: chosenFile does not exist or JOptionPane.YES_OPTION 
			PreferenceManager.getCurrent().setFile(GlobalPreference.DIR_LAST_USED_SAVE,
						fileChooser.getCurrentDirectory());
			return chosenFile;
		} 
		else
			return null;
	}
}

/**
 * It provides a single handle to access the 'Pathway' and 'VPathway' objects
 * corresponding to the 2 pathways. This info is used in comparing the 2 pathways.
 */
class PathwaysInfo
{
	private Pathway theTwoPathwayObjects[];
	private VPathway theTwoVPathwayObjects[];

	private static PathwaysInfo pathwaysInfo = new PathwaysInfo();
	static PathwaysInfo getStaticInstance()
	{
		return pathwaysInfo;
	}

	void setPathwaysInfo(Pathway pathways[], VPathway vPathways[]) 
	{
		theTwoPathwayObjects = pathways;
		theTwoVPathwayObjects = vPathways;
	}
	
	/*void setPathway1(Pathway pathway){
		theTwoPathwayObjects[0] = pathway;
	} 

	void setPathway2(Pathway pathway){
		theTwoPathwayObjects[1] = pathway;
	}

	void setVPathway1(VPathway vPathway){
		theTwoVPathwayObjects[0] = vPathway;
	}

	void setVPathway2(VPathway vPathway){
		theTwoVPathwayObjects[1] = vPathway;
	}*/

	Pathway getPathway1()
	{
		return theTwoPathwayObjects[0];
	}
	Pathway getPathway2()
	{
		return theTwoPathwayObjects[1];
	}
	VPathway getVPathway1()
	{
		return theTwoVPathwayObjects[0];
	}
	VPathway getVPathway2()
	{
		return theTwoVPathwayObjects[1];
	}
}

/**
 * This class stores the comparison results: DataNode and Interaction Comparison, 
 * thus providing a single handle to access both the results.
 */
class ComparisonResults
{
	private List<List<PathwayElement>> dataNodeCompResults;
	private List<List<List<PathwayElement>>> dNCompResultsParsed;
	private List<List<Set<PathwayElement>>> interactionCompResults;

	private static ComparisonResults comparisonResults = new ComparisonResults();
	static ComparisonResults getStaticInstance()
	{
		return comparisonResults;
	}

	void setComparisonResults(List<List<PathwayElement>> dataNodeComparisonResults,
			List<List<Set<PathwayElement>>> interactionComparisonResults)
	{
		this.dataNodeCompResults = dataNodeComparisonResults;
		this.interactionCompResults = interactionComparisonResults;
	}

	void setDNCompResultsParsed(List<List<List<PathwayElement>>> dNCompResultsParsed)
	{
		this.dNCompResultsParsed = dNCompResultsParsed;
	}

	List<List<PathwayElement>> getDNCompResults()
	{
		return dataNodeCompResults;
	}

	List<List<List<PathwayElement>>> getDNCompResultsParsed()
	{
		return dNCompResultsParsed;
	}

	List<List<Set<PathwayElement>>> getInteractionCompResults()
	{
		return interactionCompResults;
	}
}

/**JTables which use this TableModel will have their cells un-editable */
class CustomTabelModel extends DefaultTableModel
{
	@Override
	public boolean isCellEditable(int row, int col) 
	{
		return false;
	}
}