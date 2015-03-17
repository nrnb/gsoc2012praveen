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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.pathvisio.desktop.plugin.Plugin;

/**
 * OSGi Activator for the Pathway Comparison plug-in. 
 * Every PathVisio plugin has to register itself within the the OSGi framework through the Activator class. 
 * The Activator is called automatically from the OSGi framework when the bundle is started. 
 * @author Praveen Kumar
 */
public class Activator implements BundleActivator 
{
	private PathwayComparisonPlugin pwCompPlugin; 
	
	/**
	 * This overridden method registers the plug-in with the Plugin-Manager/OSGi framework.
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	 public void start(BundleContext context) throws Exception 
	 {
		pwCompPlugin = new PathwayComparisonPlugin();
	    context.registerService(Plugin.class.getName(), pwCompPlugin, null);
	 }

	/**
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	 public void stop(BundleContext context) throws Exception 
	 {
		pwCompPlugin.done();	 
	 }
}
