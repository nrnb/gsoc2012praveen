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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.pathvisio.core.model.PathwayElement;

/** 
 * This class has the the Comparison pop-up window code, basically related to its UI layout  
 * @author Praveen Kumar
 */
public class ComparePopup 
{
	private static final int INTERNAL_FRAMES_WIDTH = 600;
	static final int INTERNAL_FRAMES_HEIGHT = 650;
	private static final int OUTER_FRAME_WIDTH = INTERNAL_FRAMES_WIDTH * 2;
	private static final int OUTER_FRAME_HEIGHT = INTERNAL_FRAMES_HEIGHT;
	
	private final JScrollPane jScrollPaneArr[] = {new JScrollPane(), new JScrollPane()};
	private final JInternalFrame internalFrameArr[] = {new JInternalFrame(), new JInternalFrame()};
    //Getter Methods below
	JScrollPane[] getjScrollPaneArr() 
	{
		return jScrollPaneArr;
	}

	JInternalFrame[] getInternalFrameArr() 
	{
		return internalFrameArr;
	}
	
	private final JSplitPane pathwaysSplitPane = new JSplitPane(),outerSplitPane = new JSplitPane();
	private JFrame mainFrame;

	/** Constructs the Frame structure necessary to display the loaded pathways in 'compare-mode' in a pop-up window. 
	 * 
	 * @param pathwaysInfo Pathway and VPatwhay information related to a given pathway is contained inside in PathwaysInfo object
	 * @param comparisonResults The comparison results which are to be displayed inside the "Results" pane
	 */
	void displayPathwayComparisonInNewWindow(PathwaysInfo pathwaysInfo, ComparisonResults comparisonResults) 
	{
		if(mainFrame != null) 
		{
			//Reset resultsPane and print comparison results on it
			ResultsPane resultsPane = (ResultsPane) outerSplitPane.getRightComponent();
			resultsPane.initializePaneAndPrintReults(comparisonResults, pathwaysInfo);
			adjustSplitPaneDividerPosition();
			resetScrollBars();
			
			/*bringing mainFrame to top and then disabling 'always top' property back to normal*/
			mainFrame.setAlwaysOnTop(true);
			mainFrame.setAlwaysOnTop(false);
			mainFrame.setState(Frame.NORMAL); // maximizes the window if its in a minimized state 
			
		} else {
			
			/*	JFrame's contentPane has DesktopPane as its container -->
			 *  DesktopPane has an outer JSplitPane component, 
			 *  This outer SplitPane separates the ResultsPane and the inner JSplitpane (which separates the 2 pathways)  -->
			 *  The inner JSplitPane has JInternalFrames as its left and right components -->
			 *  The JInternalFrames have JScrollPanes as their inner component -->
			 *  The JScrollPanes have VPathway as their inner component*/
			
			/*jScrollPane1.getHorizontalScrollBar().addAdjustmentListener(scrollBarAdjustMentListener);
			jScrollPane1.getVerticalScrollBar().addAdjustmentListener(scrollBarAdjustMentListener);
			jScrollPane2.getHorizontalScrollBar().addAdjustmentListener(scrollBarAdjustMentListener);
			jScrollPane2.getVerticalScrollBar().addAdjustmentListener(scrollBarAdjustMentListener);*/
			
			//The JInternalFrames have JScrollPanes as their inner component
			internalFrameArr[0].setSize(INTERNAL_FRAMES_WIDTH,INTERNAL_FRAMES_HEIGHT);
			internalFrameArr[0].add(jScrollPaneArr[0]);
			internalFrameArr[0].setVisible(true);
			//removeMouseListener(internalFrameArr[0]);
			
			internalFrameArr[1].setSize(INTERNAL_FRAMES_WIDTH,INTERNAL_FRAMES_HEIGHT);
			internalFrameArr[1].add(jScrollPaneArr[1]);
			internalFrameArr[1].setVisible(true);
			//removeMouseListener(internalFrameArr[1]);
			
			//setup inner splitPane which contains both the internal frames containing the pathways
			pathwaysSplitPane.setLeftComponent(internalFrameArr[0]);
			pathwaysSplitPane.setRightComponent(internalFrameArr[1]);
			pathwaysSplitPane.setVisible(true);
			
			//Creating ResultsPane in which the comparison results would be shown
			ResultsPane resultsPane = new ResultsPane();
			resultsPane.initializePaneAndPrintReults(comparisonResults, pathwaysInfo);
			
			//setup outer splitPane which separates pathwaySplitpane and resultsPane 
			outerSplitPane.setLeftComponent(pathwaysSplitPane);
			outerSplitPane.setRightComponent(resultsPane);
			outerSplitPane.setVisible(true);
			
			//DesktopPane has the outer JSplitPane component
			JDesktopPane desktopPane = new JDesktopPane();
			//desktopPane.setSize(OUTER_FRAME_WIDTH, OUTER_FRAME_HEIGHT);
			desktopPane.setLayout(new BoxLayout(desktopPane, BoxLayout.Y_AXIS));
			desktopPane.add(outerSplitPane);
			desktopPane.setVisible(true);
			
			//JFrame's contentPane has DesktopPane as its container
			mainFrame = new JFrame("Pathway Comparison");
			mainFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			mainFrame.addComponentListener(mainFrameResizeListener);
			mainFrame.setContentPane(desktopPane);
			
			//place the mainFrame at the center of the screen
			Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
			mainFrame.setBounds(((screenResolution.width - OUTER_FRAME_WIDTH)>>1), ((screenResolution.height - OUTER_FRAME_HEIGHT) >> 1), 
					OUTER_FRAME_WIDTH, OUTER_FRAME_HEIGHT);
			adjustSplitPaneDividerPosition();
			//mainFrame.setVisible(true);
		}
		
		mainFrame.setVisible(true);
	}
	
	// common AdjustmentListener for JScrollPanes which contain pathways
	/*private AdjustmentListener scrollBarAdjustMentListener = new AdjustmentListener() {		
		@Override
		public void adjustmentValueChanged(AdjustmentEvent e) {
			switch(e.getAdjustable().getOrientation()) {
				case Adjustable.HORIZONTAL: //horizontal scroll-bar adjustment
					jScrollPane1.getHorizontalScrollBar().setValue(e.getValue());
					jScrollPane2.getHorizontalScrollBar().setValue(e.getValue());
					break;
				case Adjustable.VERTICAL: //vertical scroll-bar adjustment
					jScrollPane1.getVerticalScrollBar().setValue(e.getValue());
					jScrollPane2.getVerticalScrollBar().setValue(e.getValue());
					break;
			}
		}
	};*/
	
	/**Resize Listener for the component mainFrame (JFrame)*/
	private final ComponentAdapter mainFrameResizeListener = new ComponentAdapter() 
	{
		@Override
		public void componentResized(ComponentEvent e)
		{
			adjustSplitPaneDividerPosition();
		}
	};
	
	/**This resets the SplitPanes' divider positions */
	private void adjustSplitPaneDividerPosition() 
	{
		//setting pathwaysSplitPane divider-location to 40% of main frame width
		pathwaysSplitPane.setDividerLocation(40 * mainFrame.getWidth() / 100);
				
		//setting outerSplitpane divider-location to 80% of main frame width
		outerSplitPane.setDividerLocation(80 * mainFrame.getWidth() / 100);
	}
	
	/**This resets the horizontal and vertical scrollBars' positions to zero */
	private void resetScrollBars() 
	{
		jScrollPaneArr[0].getHorizontalScrollBar().setValue(0);
		jScrollPaneArr[1].getHorizontalScrollBar().setValue(0);
		jScrollPaneArr[0].getVerticalScrollBar().setValue(0);
		jScrollPaneArr[1].getVerticalScrollBar().setValue(0);
	}
	
	/** This is to block the JInternalFrames' movement inside the split-pane when dragging mouse over it
	 * @param internalFrame The {@link JInternalFrame} for which to remove the mouse listener*/
    private void removeMouseListener(JInternalFrame internalFrame) 
    {
            for(MouseListener listener : ((javax.swing.plaf.basic.BasicInternalFrameUI) internalFrame.getUI()).getNorthPane().getMouseListeners())
            {
            	((javax.swing.plaf.basic.BasicInternalFrameUI) internalFrame.getUI()).getNorthPane().removeMouseListener(listener);
            }
    }
    
}
