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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapperException;
import org.bridgedb.IDMapperStack;
import org.bridgedb.Xref;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.model.DataNodeType;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;

/**This class has all the Datanode comparison logic and is instantiated on every hit of compare button. 
 * @author Prvaeen Kumar
 */
public class PathwayElementComparision 
{
	private PathwaysInfo pathwaysInfo;
	private IDMapperStack bridgeDBIDMapperStack;
	
	// + 1 is for the miscellaneous list which would store all the unmatched DataNodes
	private final static int numberOfDNTypes = DataNodeType.getValues().length + 1; 
		
	/**	Each array element in the variable stores the index corresponding to a list of 
	 * PathwayElements belonging to a particular DataNodeType. Since the same list is used 
	 * to store both Pathway1 and Pathway2 datanodes, an index is needed to mark the separation*/
	private int seperator_dataNodesTypesList[] = new int[numberOfDNTypes]; 

	/** Used to dynamically group the PathwayElements of a particular dataNode Type.
	 *  Each Array element corresponds to a list of PathwayElements belonging to a specific DataNode Type.*/ 
	private List<PathwayElement> array_dataNodeTypesList[] = new ArrayList[numberOfDNTypes];  
	
	/** Each element in the outer ArrayList corresponds to a match and 
	 the inner ArrayList has a list of entries corresponding to a specific match. 
	 The first element in this inner list corresponds to a PathwayElement in Pathway1  
	 and the remaining ones are the respective matches found in Pathway2.*/   
	private final List<List<PathwayElement>> dataNodeComparisonResults =  new ArrayList<List<PathwayElement>>();
	
	private int dnCountInPw1, dnCountInPw2;
	int getDNcountInPw1(){
		return dnCountInPw1;
	}
	
	int getDNcountInPw2(){
		return dnCountInPw2;
	} 
	
	public PathwayElementComparision(PathwaysInfo pathwaysInfo, IDMapperStack bridgeDBIDMapperStack) 
	{
		this.pathwaysInfo = pathwaysInfo;
		this.bridgeDBIDMapperStack = bridgeDBIDMapperStack;
		//generatePathwayCollectionReport("c:/pathways");
		
		for(int i = 0; i < numberOfDNTypes; i++)
			array_dataNodeTypesList[i] = new ArrayList<PathwayElement>();
	}
	
	/** categorizes PathwayElements found in the pathway based on their DataNode Types.
	 *
	 * @param pathway Pathway object on which the categorization of PathwayElements is to be performed.
	 * @param dataNodeTypes Different DataNode types into which the PathwayElements in the Pathway object need to be categorized to.
	 * 
	 * @return count of Datanodes in the pathway*/
	private int categorizePWEBasedOnDNType(Pathway pathway, DataNodeType ...dataNodeTypes ) 
	{
		int dnCountInPw = 0;
		for(PathwayElement pwe: pathway.getDataObjects())
		{
			boolean pweNotMatchingWithDNTypes = true;
			if(pwe.getObjectType().equals(ObjectType.DATANODE)) 
			{	
				dnCountInPw++;
				for(int i= 0; i < numberOfDNTypes-1; i++) 
				{
					if(pwe.getDataNodeType().equals(dataNodeTypes[i].getName())) 
					{
						array_dataNodeTypesList[i].add(pwe);
						pweNotMatchingWithDNTypes = false;
						break;
					} 
				}
				if(pweNotMatchingWithDNTypes)
					array_dataNodeTypesList[numberOfDNTypes-1].add(pwe);
			}
		}
		return dnCountInPw;
	}
	
	/** Gets the unique list of Xref DataSources found in a pathway.
	 *  
	 *  @param pathway The Pathway object in which to look for the Xref DataSources
	 *
	 *  @return Unique list of Xref DataSources found in the pathway*/
	private DataSource[] getUniqueSetOfXrefDataSourcesInPathway(Pathway pathway)
	{
		List<DataSource> result = new ArrayList<DataSource>();
		
		for(Xref xref : pathway.getDataNodeXrefs()) 
		{
			DataSource xrefDataSource = xref.getDataSource();
			if( xrefDataSource != null && !result.contains(xrefDataSource) ) 
			{
				result.add(xrefDataSource);
			}
		}
		if(result.size() > 0)
			return result.toArray(new DataSource[result.size()]);
		else
			return null;
	}
	
	/** Compares two {@link PathwayElement}s based on their Xrefs. Comparison at its lowest level.
	 * @return true if the PathwayElements match.*/
	private boolean compareDataNodesUsingXrefs(PathwayElement pwe1, PathwayElement pwe2, DataSource[] dataSourceArray) 
	{		
		Set<Xref> mappedXrefsForPWE1 = null;
		try {
			mappedXrefsForPWE1 = bridgeDBIDMapperStack.mapID(pwe1.getXref(), dataSourceArray);
		} catch (IDMapperException e) {
			Logger.log.error("IDMapperException Exception while mapping one of the Xrefs in Pathway1 to the DataSources found in Pathways2", e);
			e.printStackTrace();
		}
		if( mappedXrefsForPWE1 != null && mappedXrefsForPWE1.contains(pwe2.getXref()) ) 
		{
			Utility.highlightPWE(pwe1, pathwaysInfo.getVPathway1(), false);
			Utility.highlightPWE(pwe2, pathwaysInfo.getVPathway2(), false);
			return true;
		}
		return false;
	} 
	
	/** loops through DataNodeType list and compares Pathway1 DataNodes with Pathway2's. 
	 * The actual datanode comparison happens here.
	 * 
	 * @return The result after comparing the datanodes in the 2 pathways*/
	private List<List<PathwayElement>> compareBasedOnDataNodeType() 
	{		
 		DataSource[] arrayOfDataSourcesInPathway = getUniqueSetOfXrefDataSourcesInPathway(pathwaysInfo.getPathway2());
		if(arrayOfDataSourcesInPathway == null)
		{
			return dataNodeComparisonResults;
		}
		for(int index_DNType = 0; index_DNType < numberOfDNTypes; index_DNType++)
		{
			//skip the comparison against this DataNodeType if there are no PathwayElements corresponding to it in either Pathway1 or Pathway2 
			if(seperator_dataNodesTypesList[index_DNType] == 0 || 
					seperator_dataNodesTypesList[index_DNType] == array_dataNodeTypesList[index_DNType].size()) 
				continue;
			
			//comparing Pathway1's DataNodes of a certain Type with Pathway2's DataNodes of the same type
			for(int i = 0; i < seperator_dataNodesTypesList[index_DNType]; i++) 
			{
				PathwayElement pwe1 = array_dataNodeTypesList[index_DNType].get(i);
				Xref pwe1Xref = pwe1.getXref();
				//Xref.getDataSource() returns null when attribute Database = "" (inside the Xref tag) in the gpml file
				if(pwe1Xref.getId().trim().equals("") || pwe1Xref.getDataSource() == null) 
					continue;
				for(int j = seperator_dataNodesTypesList[index_DNType]; j < array_dataNodeTypesList[index_DNType].size(); j++) 
				{
					PathwayElement pwe2 = array_dataNodeTypesList[index_DNType].get(j);
					Xref pwe2Xref = pwe2.getXref();
					if(pwe2Xref.getId().trim().equals("") || pwe2Xref.getDataSource() == null)
						continue;
					if(compareDataNodesUsingXrefs(pwe1, pwe2, arrayOfDataSourcesInPathway))
					{
						//array_dataNodeTypesList[index_DNType].remove(j);
						List<PathwayElement> eachMatch = new ArrayList<PathwayElement>();
						eachMatch.add(pwe1);
						eachMatch.add(pwe2);
						dataNodeComparisonResults.add(eachMatch);
						//break;
					}
				}
			}
		}
		return dataNodeComparisonResults;
	}
	
	/**The main compare method where it first categorizes the PathwayElements and then compares them. 
	 * This method calls other class methods to do the comparison. This is the comparison method at the highest level.
	 *
	 * @return The result after comparing the datanodes in the 2 pathways*/
	public List<List<PathwayElement>> compareDataNodes() 
	{
		dnCountInPw1 = categorizePWEBasedOnDNType(pathwaysInfo.getPathway1(), DataNodeType.getValues());
		
		/* Setting the index which marks the separation between Pathway 1 and Pathway 2 DataNodes. 
		   This index indicates the beginning of the list associated to DataNodes belonging to Pathway2*/
		for(int index_DNType = 0; index_DNType < numberOfDNTypes; index_DNType++)
		{
			seperator_dataNodesTypesList[index_DNType] = array_dataNodeTypesList[index_DNType].size();
		}
		
		dnCountInPw2 = categorizePWEBasedOnDNType(pathwaysInfo.getPathway2(), DataNodeType.getValues());
		
		//getXrefListsOfPathwaysToCompare();
		return compareBasedOnDataNodeType();
	}
	
	/*private boolean compareDataNodesUsingGraphIds(PathwayElement pwe1,PathwayElement pwe2) {		
		if( pwe1.getGraphId().equals(pwe2.getGraphId()) ) {
			Utility.highlightPWE(pwe1, pathwaysInfo.getVPathway1(), false);
			Utility.highlightPWE(pwe2, pathwaysInfo.getVPathway2(), false);
			return true;
		}
		return false;
	}
	
	BridgeDB Test comparisons below
	private void getXrefListsOfPathwaysToCompare() {
		try {
			Class.forName("org.bridgedb.webservice.bridgerest.BridgeRest");
			BioDataSource.init();	
			IDMapper mapper = BridgeDb.connect ("idmapper-bridgerest:http://webservice.bridgedb.org/Human");	
			List<Xref> src1 = pathwaysInfo.getPathway1().getDataNodeXrefs();
			List<Xref> src2 = pathwaysInfo.getPathway2().getDataNodeXrefs();
			List<Xref> xrefList1 = new ArrayList<Xref>();
			List<Xref> xrefList2 = new ArrayList<Xref>();

			System.out.println(":::::::::::::::::;ENTREZ_GENE xrefs pathway1::::::::::::");
			for(Xref xref : src1) {
				if(xref.getDataSource() != null && xref.getDataSource().equals(BioDataSource.ENTREZ_GENE)) {
					xrefList1.add(xref);
				}
			}
			System.out.println(":::::::::::::::::;ENTREZ_GENE xrefs pathway2::::::::::::");
			for(Xref xref : src2) {
				if(xref.getDataSource() != null && xref.getDataSource().equals(BioDataSource.ENTREZ_GENE)) {
					xrefList2.add(xref);
				}
			}

			Map<Xref, Set<Xref>> dest1 = mapper.mapID(xrefList1, BioDataSource.ENTREZ_GENE);
			Map<Xref, Set<Xref>> dest2 = mapper.mapID(xrefList2, BioDataSource.ENTREZ_GENE);
			
			loopXrefMap(dest1, "pathway1");
			loopXrefMap(dest2, "pathway2");
			
			compareXrefs(dest1, dest2);
		}
		catch (Exception e) {
			e.printStackTrace();	
		} 
	}
	
	private void loopXrefMap(Map<Xref, Set<Xref>> map,String pathwayName) {
		System.out.println("::::::::::::::::::::::::::"+pathwayName+":::::::::::::::::::::::::::::");
		for(Map.Entry<Xref, Set<Xref>> mapEntry : map.entrySet()) {
			System.out.println("Xref id = "+mapEntry.getKey().getId());
			for(Xref xref : mapEntry.getValue()) {
				System.out.println("mapper xref = "+xref.getURN());
			}
		}
	}
		
	private void compareXrefs(Map<Xref, Set<Xref>> dest1,Map<Xref, Set<Xref>> dest2) {
		ArrayList<Xref> total1 = new ArrayList<Xref>();
		ArrayList<Xref> total2 = new ArrayList<Xref>();
		for (Map.Entry<Xref,Set<Xref>> entry : dest1.entrySet()) {
			total1.add((Xref) entry.getValue().toArray()[0]);
		}
		
		for (Map.Entry<Xref,Set<Xref>> entry : dest2.entrySet()) {
			total2.add((Xref) entry.getValue().toArray()[0]);
		}
		System.out.println("total1 = "+total1);
		System.out.println("total2 = "+total2);
		total1.retainAll(total2);
		System.out.println("total1 size = "+total1.size());
		for(Xref xref : total1) {
			System.out.println("match found :::"+xref.getId());
		}
	}
	
	private static void compareBasicBridgeDbExample() {
		try {
			Class.forName("org.bridgedb.webservice.bridgerest.BridgeRest");
			BioDataSource.init();	
			IDMapper mapper = BridgeDb.connect ("idmapper-bridgerest:http://webservice.bridgedb.org/Human");	
			Xref src = new Xref ("2950", BioDataSource.ENTREZ_GENE);	
			Set<Xref> dests = mapper.mapID(src, BioDataSource.ENSEMBL_HUMAN);
			System.out.println (src.getURN() + " maps to:");
			for (Xref dest : dests)
				System.out.println(" " + dest.getURN());
			}
		catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	//Report for if there are any gpml files in a folder which have lines whose end-points are graphrefs to anchors 
	private void generatePathwayCollectionReport(String path) {	
		File folder = new File(path);
		if(folder.isDirectory()) {
			File[] gpmlFiles = folder.listFiles();
			for(File file : gpmlFiles) {
				Pathway pathway = null;
				List<String> anchorIDsListInFile = new ArrayList<String>();
				try {
					pathway = new Pathway();
					pathway.readFromXml(file, false);
				} catch (ConverterException e) {
					System.out.println("GPML file failed to load");
					e.printStackTrace();
					continue;
				}
				
				for(PathwayElement line : pathway.getDataObjects()) {
					if(line.getObjectType().equals(ObjectType.LINE)) {
						for(MAnchor mAnchor : line.getMAnchors())
							if(mAnchor.getGraphId() != null)
								anchorIDsListInFile.add(mAnchor.getGraphId());
					}
				}
				
				for(PathwayElement line : pathway.getDataObjects()) {
					if(line.getObjectType().equals(ObjectType.LINE)) {
						if(anchorIDsListInFile.contains(line.getStartGraphRef())
								&& anchorIDsListInFile.contains(line.getEndGraphRef())){
							System.out.println(":::::::::::::::::::::file name:::"+file.getName());
							System.out.println(line.getGraphId());
							System.out.println();
						}
							
					}
				}
			}
		}
	}*/
}
