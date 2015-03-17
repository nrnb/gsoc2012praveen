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

import javax.swing.JTabbedPane;

import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;

/**
 * The Pathway Comparison plugin's main class.
 * @author Praveen Kumar
 */
public class PathwayComparisonPlugin implements Plugin
{
	@Override
	public void init(PvDesktop pvDesktop)
	{
		ComparePopup comparePopup = new ComparePopup();
		ComparePane comparePane = new ComparePane(pvDesktop.getSwingEngine(), comparePopup);
		JTabbedPane tabbedPane = pvDesktop.getSideBarTabbedPane();
		if(tabbedPane != null) 
		{
			tabbedPane.addTab ("Compare", comparePane);
		}
	}

	@Override
	public void done() 
	{
		// TODO Auto-generated method stub
	}
}
