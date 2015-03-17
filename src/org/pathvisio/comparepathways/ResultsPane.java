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
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.pathvisio.core.model.MLine;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.view.VPathway;
import org.pathvisio.core.view.VPathwayElement;

/**
 * This is the panel which displays the Comparison results. 
 * Its located in the right in comparison pop-up window 
 * @author Praveen Kumar 
 */
public class ResultsPane extends JPanel 
{
	private PathwaysInfo pathwaysInfo;
	
	// '_g' indicates that this variable is globally available in the class
	private List<List<List<PathwayElement>>>  dataNodeComparisonResults_g; 
	private List<List<Set<PathwayElement>>> interactionComparisonResults_g;
	
	private final JSplitPane splitPane = new JSplitPane();
	private final JPanel resultsPanel_DataNode = new JPanel(), resultsPanel_Line = new JPanel();
	private final JCheckBox checkBox_DataNode = new JCheckBox("Hilight matches"), 
			checkBox_Line = new JCheckBox("Hilight matches");
	private JTable resultsTable_DataNode, resultsTable_Line;
	private CustomTabelModel resultsTableModel_DataNode = new CustomTabelModel(),
		resultsTableModel_Line = new CustomTabelModel(); 
	private final JLabel label_DataNode = new JLabel(), label_Line = new JLabel();
	
	/** 
	 * The constructor is called once and the method 'setPaneAndCompare' is called on every compare. 
	 */
	public ResultsPane() 
	{
		checkBox_DataNode.setSelected(true);
		checkBox_DataNode.addActionListener(checkBoxListener);
		checkBox_Line.addActionListener(checkBoxListener);

		resultsPanel_DataNode.setBorder (BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
				"DataNode Comparison Results"));
		resultsPanel_DataNode.setLayout(new BorderLayout());	

		resultsPanel_Line.setBorder (BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
				"Interaction Comparison Results"));
		resultsPanel_Line.setLayout(new BorderLayout());	
		
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		splitPane.setTopComponent(resultsPanel_DataNode);
		splitPane.setBottomComponent(resultsPanel_Line);
		splitPane.setDividerLocation(ComparePopup.INTERNAL_FRAMES_HEIGHT >> 1);
		splitPane.setEnabled(false);

		this.setLayout(new BorderLayout());
		this.addComponentListener(resultsPaneResizeLisetner);
		this.add(splitPane);
	}

	/** 
	 * This method is called on every compare. It reinitializes the PathwaysInfo and results info;
	 * and prints the comparison results inside this panel. 
	 * 
	 * @param comparisonResults Data passed from ComparePopup to this class.  
	 * @param pathwaysInfo Data passed from ComparePopup to this class.
	 */
	public void initializePaneAndPrintReults(ComparisonResults comparisonResults, PathwaysInfo pathwaysInfo)
	{
		this.pathwaysInfo = pathwaysInfo;
		this.dataNodeComparisonResults_g = prepManyToManyDNComparisonListMap(comparisonResults.getDNCompResults());
		this.interactionComparisonResults_g = comparisonResults.getInteractionCompResults();
		
		ComparisonResults.getStaticInstance().setDNCompResultsParsed(dataNodeComparisonResults_g);
		
		checkBox_DataNode.setSelected(true);
		checkBox_Line.setSelected(false);
		
		String[][] formattedDNcomparisonResults = parseDNcomparisonResultsIntoTableFormat(dataNodeComparisonResults_g);
		resultsTable_DataNode = populateResultsPanel(resultsPanel_DataNode, formattedDNcomparisonResults);
		
		String[][] formattedInteractionComparsionResults = parseInteractionComparisonResultsIntoTableFormat(
				interactionComparisonResults_g);
		resultsTable_Line = populateResultsPanel(resultsPanel_Line, formattedInteractionComparsionResults);
	}
	
	/**
	 * Returns list of keys which map to the same value.
	 *
	 * @param map : one-to-many map which has these keys and values. 
	 * @param value : value for which the list of keys are to be found.
	 */
	private List<PathwayElement> getKeysFromValue(Map<PathwayElement, List<PathwayElement>> map, List<PathwayElement> value)
	{
	    List<PathwayElement> keysList = new ArrayList<PathwayElement>();
	    for(PathwayElement key : map.keySet())
	    {
	      if(map.get(key).equals(value)) 
	      { 
	        keysList.add(key); 
	      } 
	    } 
	    return keysList;
	}
	
	/**
	 * prepares a many-to-many list of DN matches from pathway1 to pathway2.
	 * @param dataNodeComparisonResults : input list of one-to-one Datanode comparison results.
	 * @return a many-to-many list of DN matches from pathway1 to pathway2.
	 */
	private List<List<List<PathwayElement>>> prepManyToManyDNComparisonListMap(List<List<PathwayElement>> dataNodeComparisonResults)
	{
		List<List<List<PathwayElement>>> manyToManyDNList = new ArrayList<List<List<PathwayElement>>>(); 
		if(dataNodeComparisonResults.isEmpty())
			return manyToManyDNList;
		
		PathwayElement previousPwe = null, currentPwe = null;
		List<PathwayElement> pweListInPathway2 = null;
		HashMap<PathwayElement, List<PathwayElement>> oneToManyDNCompResults = new HashMap<PathwayElement, List<PathwayElement>>();
		for(List<PathwayElement> eachDNMatch : dataNodeComparisonResults)
		{
			currentPwe = eachDNMatch.get(0);
			if(previousPwe != currentPwe)
			{
				if(previousPwe != null)
					oneToManyDNCompResults.put(previousPwe, pweListInPathway2);
				pweListInPathway2 = new ArrayList<PathwayElement>();
				
			}
			pweListInPathway2.add(eachDNMatch.get(1));
			previousPwe = currentPwe;
		}
		oneToManyDNCompResults.put(previousPwe, pweListInPathway2); // one-to-many map ready
		
		HashMap<List<PathwayElement>, List<PathwayElement>> manyToManymappedDNComparisonResults = new HashMap<List<PathwayElement>, List<PathwayElement>>();
		List<List<PathwayElement>>valuesAlreadyKeyed = new ArrayList<List<PathwayElement>>();
		for(List<PathwayElement> eachValueInOneToManyMap : oneToManyDNCompResults.values())
		{
			if(valuesAlreadyKeyed.contains(eachValueInOneToManyMap))
			{
				continue;
			}
			List<PathwayElement> keysMappedToSameValues = getKeysFromValue(oneToManyDNCompResults, eachValueInOneToManyMap);
			if(keysMappedToSameValues.size() > 1)
				valuesAlreadyKeyed.add(eachValueInOneToManyMap);
			manyToManymappedDNComparisonResults.put(keysMappedToSameValues, eachValueInOneToManyMap); // many-to-many map ready
		}
		
		//converting many-to-many map to many-to-many list
		//System.out.println("------ many to many map ------");
		for(Map.Entry<List<PathwayElement>, List<PathwayElement>> mapEntry : manyToManymappedDNComparisonResults.entrySet())
		{
			//System.out.println(mapEntry);
			List<List<PathwayElement>> eachMapEntry = new ArrayList<List<PathwayElement>>();
			eachMapEntry.add(mapEntry.getKey());
			eachMapEntry.add(mapEntry.getValue());
			manyToManyDNList.add(eachMapEntry);
		}
		
		return manyToManyDNList;
	}

	/**
	 * Converts the Datanode Comparison results (in list format) into a format required by JTable (Object / String [][]).
	 *
	 * @param manyToManymappedDNComparisonResults : input list of many-to-many DN comparison results. 
	 * @return converted Datanode Comparison results in a 2-D String array format.
	 */
	private String[][] parseDNcomparisonResultsIntoTableFormat(List<List<List<PathwayElement>>> manyToManymappedDNComparisonResults) 
	{
		if(manyToManymappedDNComparisonResults.isEmpty()) 
		{
			return null;
		}
		
		String[][] formattedDNcomparisonResults = new String[manyToManymappedDNComparisonResults.size()][2];
		StringBuilder stringBuilder = new StringBuilder("");
		List<String> uniqueDNlabelsList = new ArrayList<String>();
		List<Integer> countOfUniqueLabels = new ArrayList<Integer>();
		
		int rowIndex = 0;
		for(List<List<PathwayElement>> eachManytoManyEntry : manyToManymappedDNComparisonResults)
		{
			for(int i=0; i < 2; i++){
				stringBuilder.delete(0, stringBuilder.length());
				uniqueDNlabelsList.clear();
				countOfUniqueLabels.clear();
				for(PathwayElement pwe : eachManytoManyEntry.get(i))
				{
					if(! uniqueDNlabelsList.contains(pwe.getTextLabel()) )
					{
						uniqueDNlabelsList.add(pwe.getTextLabel());
						countOfUniqueLabels.add(Integer.valueOf(1));
						//stringBuilder.append(", " + pwe.getTextLabel());
					} else {
						int indexOfUniqueLabel = uniqueDNlabelsList.indexOf(pwe.getTextLabel());
						int indexValue = countOfUniqueLabels.get(indexOfUniqueLabel).intValue();
						countOfUniqueLabels.set(indexOfUniqueLabel, Integer.valueOf((++indexValue)));
					}
				}
				for(int j = 0; j < uniqueDNlabelsList.size(); j++)
				{
					stringBuilder.append(", " + uniqueDNlabelsList.get(j));
					if(countOfUniqueLabels.get(j).intValue() > 1)
						stringBuilder.append(" {" + countOfUniqueLabels.get(j) + "}");
				}
				formattedDNcomparisonResults[rowIndex][i] = stringBuilder.substring(2);
			}
			rowIndex++;
		}
		
		return formattedDNcomparisonResults;
	}
	
	/**
	 * Converts the Interaction Comparison results (in list format) into a format required by JTable (Object / String[][] ).
	 *
	 * @param interactionComparisonResults : input list of many-to-many interaction comparison results. 
	 * @return converted interaction-comparison results in a 2-D String array format.
	 */
	private String[][] parseInteractionComparisonResultsIntoTableFormat(List<List<Set<PathwayElement>>> interactionComparisonResults)
	{
		if(interactionComparisonResults.isEmpty())
			return null;
		
		String[][] formattedInteractionComparisonResults = new String[interactionComparisonResults.size()][2];
		for(int i = 0; i < interactionComparisonResults.size(); i++)
		{
			for(int j = 0; j < interactionComparisonResults.get(i).size(); j++)
			{
				Set<PathwayElement> eachInteraction = interactionComparisonResults.get(i).get(j);
				StringBuilder textLabelsOfDNsInInteraction = new StringBuilder();
				for(PathwayElement pwe : eachInteraction)
					if(!(pwe instanceof MLine))
						textLabelsOfDNsInInteraction.append(", " + pwe.getTextLabel());
				formattedInteractionComparisonResults[i][j] = textLabelsOfDNsInInteraction.toString().substring(1); 
			}
		}
		return formattedInteractionComparisonResults;
	}
	
	/**
	 * populates Results Panel ('DataNode Comparison Results' and 'Interaction Comparison Results' panel)
	 * with checkboxes, JTable, JLabel etc.
	 * 
	 * @param panel : the panel which has to be populated.
	 * @param dataForTable : data which is to be shown in a JTable inside the panel. 
	 * @return JTable contained inside the populated panel.
	 */
	private JTable populateResultsPanel(JPanel panel, String dataForTable[][]) 
	{
		JTable resultsTable; 
		CustomTabelModel resultsTableModel; 
		JCheckBox checkBox; 
		JLabel label;
		
		if (panel == resultsPanel_DataNode)
		{
			resultsTable = resultsTable_DataNode;
			resultsTableModel = resultsTableModel_DataNode;
			checkBox = checkBox_DataNode;
			label = label_DataNode;
		
		} else {
			resultsTable = resultsTable_Line;
			resultsTableModel = resultsTableModel_Line;
			checkBox = checkBox_Line;
			label = label_Line;
		}
		
		panel.removeAll();
		if(dataForTable == null) 
		{
			panel.add(setupNoMatchFoundLabel()); 
		
		} else {
			if(resultsTable == null) // creates a new JTable only for the first compare and uses the same for subsequent compares
			{
				resultsTable =  createJTableToolTipReady(resultsTableModel);
				resultsTable.setVisible(true);
				resultsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);			
				resultsTable.addMouseListener(resultsTableMouseClickListener);
				resultsTable.addKeyListener(resultsTableReturnKeyListener);
			} 
			resultsTableModel.setDataVector(dataForTable, new String[]{"Pathway 1", "Pathway 2"});
			resultsTable.setModel(resultsTableModel);
			label.setText(resultsTable.getRowCount() + " Results found ");
			panel.add(checkBox, BorderLayout.NORTH);
			panel.add (new JScrollPane(resultsTable), BorderLayout.CENTER);
			panel.add (label, BorderLayout.SOUTH);	
		}			 
		return resultsTable;
	}

	/**
	 * Creates a JTable with tool-tip set for every cell in advance. 
	 * 
	 * @param resultsTableModel : decides what tool-tips to set (interaction / datanode comparison) based on the table model.
	 * @return a new instance of a JTable with tool-tips set for every cell. 
	 */
	private JTable createJTableToolTipReady(final CustomTabelModel resultsTableModel)
	{
		return new JTable()
		{
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
			{
				Component c = super.prepareRenderer(renderer, row, column);
				if (c instanceof JComponent) 
				{
					if(resultsTableModel == resultsTableModel_DataNode) // display PWE's xref in tooltip
					{
						StringBuilder xRefToolTip = new StringBuilder("");
						for(PathwayElement pwe : dataNodeComparisonResults_g.get(row).get(column))
							xRefToolTip.append(", " + pwe.getXref().toString());
						((JComponent) c).setToolTipText(xRefToolTip.substring(2));
					}
					else if(resultsTableModel == resultsTableModel_Line) // display the cell text
						((JComponent) c).setToolTipText(getValueAt(row, column).toString());
					
				}
				return c;
			}
		};
	}

	/**
	 * This does the actual highlighting and focuses the scrollers on to the pathway nodes. 
	 * 
	 * @param match : A matching entity in both the pathways. Could be a matching interaction or PathwayElement. 
	 * @param focus : tells whether to focus on to the entity.  
	 */
	private void highlightMatchingEntityInPWs(Object match, boolean focus) 
	{
		for(int i = 0; i < 2; i++)
		{
			// varibles below  store the min and max of X & Y coordinates of the center of framing Rectangles of all the Datanodes
			double dnMinX = 0, dnMaxX = 0, dnMinY = 0, dnMaxY = 0;  
			boolean onlyOnceFlag = true;
		
			VPathway vPathway = ( i == 0 ) ? pathwaysInfo.getVPathway1() : pathwaysInfo.getVPathway2();

			Collection<PathwayElement> entityInAPathway = ((List<Collection<PathwayElement>>)match).get(i);
			if(entityInAPathway.size() == 1)
			{
				Utility.highlightPWE(entityInAPathway.iterator().next(), vPathway, focus);
				continue;
			}
				
			for(PathwayElement pwe : entityInAPathway)
			{
				Utility.highlightPWE(pwe, vPathway, false);
				
				//calculation dnMinX, dnMaxX, dnMinY and dnMaxY 
				if(focus && ! (pwe instanceof MLine)) { // if its a DataNode
					VPathwayElement vPwe = vPathway.getPathwayElementView(pwe);
					double currentDNsleftX = vPwe.getVBounds().getMinX(),
							currentDNsRightX = vPwe.getVBounds().getMaxX(),
							currentDNsBottomY = vPwe.getVBounds().getMinY(),
							currentDNsTopY = vPwe.getVBounds().getMaxY();
					if(onlyOnceFlag) 
					{
						onlyOnceFlag = false;
						dnMinX = currentDNsleftX;
						dnMaxX = currentDNsRightX;
						dnMinY = currentDNsBottomY;
						dnMaxY = currentDNsTopY;
					} else 
					{  
						if(currentDNsleftX < dnMinX) 
							dnMinX = currentDNsleftX;
						else if (currentDNsRightX > dnMaxX)  
							dnMaxX = currentDNsRightX;
						if(currentDNsBottomY < dnMinY) 
							dnMinY = currentDNsBottomY;
						else if(currentDNsTopY > dnMaxY) 
							dnMaxY = currentDNsTopY;
					}
				}
			}
			
			if(focus)  //scrolls to a mid-location of the Datanodes present in the extreme left, right, top and bottom.  
			{
				double widhthBwHorzDNs = dnMaxX - dnMinX, heightBwVerticalDFs = dnMaxY - dnMinY;
				double viewPortWidth = vPathway.getWrapper().getViewRect().getWidth(), 
						viewPortHeight = vPathway.getWrapper().getViewRect().getHeight();
				double zoomFactor = 1.0;
				if(viewPortWidth < widhthBwHorzDNs || viewPortHeight < heightBwVerticalDFs)
				{
					zoomFactor = Math.min( viewPortWidth / widhthBwHorzDNs, viewPortHeight / heightBwVerticalDFs) - 0.10; // extra zoom-out buffer of 10%
					vPathway.setPctZoom(100 * zoomFactor);
				}
				
				vPathway.getWrapper().scrollCenterTo( (int) (zoomFactor * (dnMinX + dnMaxX) / 2), (int) (zoomFactor * (dnMinY + dnMaxY) / 2) );
			}
		}
	}
	
	/*private void highlightMatchingDNsInPWs(List<List<PathwayElement>> chosenDNMatch, boolean focus) {
		for(PathwayElement pwe : chosenDNMatch.get(0)){
			Utility.highlightPWE(pwe, pathwaysInfo.getVPathway1(), focus);
		}
		
		for(PathwayElement pwe : chosenDNMatch.get(1)){
			Utility.highlightPWE(pwe, pathwaysInfo.getVPathway2(), focus);
		}
	}*/
	
	/**
	 * highlights the chosen comparison match (i.e a pair of Datanodes / interaction) in both the pathways.
	 * @param resultsTable : The JTable on which the row is chosen by the user.
	 */
	private void highlightChosenRowInBothPathways(JTable resultsTable) 
	{
		pathwaysInfo.getVPathway1().resetHighlight();
		pathwaysInfo.getVPathway2().resetHighlight();

		pathwaysInfo.getVPathway1().setPctZoom(100);
		pathwaysInfo.getVPathway2().setPctZoom(100);
		
		int selectedRowForHighlight = resultsTable.getSelectedRow();
				
		if(resultsTable == resultsTable_DataNode)
		{
			highlightMatchingEntityInPWs(dataNodeComparisonResults_g.get(selectedRowForHighlight), true);
		}
		else if (resultsTable == resultsTable_Line)
		{
			highlightMatchingEntityInPWs(interactionComparisonResults_g.get(selectedRowForHighlight), true);
		}

		//commenting out the code below as it was based on the assumption that 
		//each match could have contained multiple PathwayElements from Pathway2 corresponding to a PathwayElement in Pathway1  
		/*String column2String = (String)resultsTable_DataNode.getValueAt(resultsTable_DataNode.getSelectedRow(), 1);
		if(column2String.contains(",")){
			for(String graphIdString : column2String.split(","))
				highlightPWElement(vp2, graphIdString);
		} else{
			highlightPWElement(vp2, column2String);
		}*/
	}
	
	/**
	 * Highlights all the matching Interactions or Datanodes in both the pathways 
	 * depending on the checkbox checked by the user. 
	 * 
	 * @param checkbox : The checkbox which is selected by the user.
	 */
	private void highlightAllMatches(JCheckBox checkbox) 
	{
		pathwaysInfo.getVPathway1().resetHighlight();
		pathwaysInfo.getVPathway2().resetHighlight();
		
		pathwaysInfo.getVPathway1().setPctZoom(100);
		pathwaysInfo.getVPathway2().setPctZoom(100);
		
		resultsTable_Line.getSelectionModel().clearSelection();
		resultsTable_DataNode.getSelectionModel().clearSelection();
		
		if(checkbox.isSelected()) 
		{
			if(checkbox == checkBox_DataNode) 
			{
				for(List<List<PathwayElement>> dNMatach : dataNodeComparisonResults_g) 
				{
					highlightMatchingEntityInPWs(dNMatach, false);
				}
			}
			else if (checkbox == checkBox_Line)
			{
				for(List<Set<PathwayElement>> eachInteeraction : interactionComparisonResults_g)
				{
					highlightMatchingEntityInPWs(eachInteeraction, false);
				}
			}
		}
	}

	/**
	 * Sets up a "No match found" JLabel instance with s custom font and alignment. 
	 * @return "No match found" JLabel instance.  
	 */
	private static JLabel setupNoMatchFoundLabel() 
	{
		JLabel noMatchFoundLabel = new JLabel("No match found");
		noMatchFoundLabel.setFont(new Font("Arial", Font.BOLD, 13));			
		noMatchFoundLabel.setHorizontalAlignment(JLabel.CENTER);
		noMatchFoundLabel.setVerticalAlignment(JLabel.CENTER);
		return noMatchFoundLabel;
	}

	//<!-------------------- Listeners start-------------->
	/**Resize Listener for this panel*/
	private final ComponentAdapter resultsPaneResizeLisetner = new ComponentAdapter() 
	{
		@Override
		public void componentResized(ComponentEvent e)
		{
			splitPane.setDividerLocation( ( (JPanel)e.getComponent() ).getHeight() >> 1);
		}
	};

	/**Mouse Listener for the JTables inside 'Results' Panel (specifically handling the mouse click on the JTable rows)*/
	private final MouseAdapter resultsTableMouseClickListener = new MouseAdapter() 
	{
		public void mouseClicked(MouseEvent mouseEvent)	
		{
			checkBox_DataNode.setSelected(false);
			checkBox_Line.setSelected(false);
			
			if(mouseEvent.getSource() == resultsTable_DataNode && !interactionComparisonResults_g.isEmpty())
			    resultsTable_Line.getSelectionModel().clearSelection();
			else if(mouseEvent.getSource() == resultsTable_Line && !dataNodeComparisonResults_g.isEmpty()) 
			    resultsTable_DataNode.getSelectionModel().clearSelection();
			
			highlightChosenRowInBothPathways((JTable)mouseEvent.getSource());
		}
	};

	/**Key Listener (specifically handling the 'Return' key) for the JTables in 'Results' Panel*/
	private final KeyAdapter resultsTableReturnKeyListener = new KeyAdapter() 
	{		
		@Override
		public void keyPressed(KeyEvent keyEvent) 
		{
			if(keyEvent.getKeyCode() == KeyEvent.VK_ENTER) 
			{
				//This is to bypass the natural behavior of the return key 
				//(hitting return key moved the selection one step down)
				keyEvent.consume();
				checkBox_DataNode.setSelected(false);
				checkBox_Line.setSelected(false);
				highlightChosenRowInBothPathways((JTable)keyEvent.getSource());		
			} 
		}
	};

	/**
	 * Listener for the check-boxes present in the Results panel. 
	 * Clicking on the check boxes highlights all the corresponding matches in both the pathways 
	 */
	private final ActionListener checkBoxListener = new ActionListener() 
	{		
		@Override
		public void actionPerformed(ActionEvent e) 
		{
			if(e.getSource() == checkBox_DataNode) 
			{
				checkBox_Line.setSelected(false);
			} 
			else if(e.getSource() == checkBox_Line) 
			{
				checkBox_DataNode.setSelected(false);
			}
			highlightAllMatches((JCheckBox)e.getSource());
		}
	};
}
