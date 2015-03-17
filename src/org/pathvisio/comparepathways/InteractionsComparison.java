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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.pathvisio.core.model.MLine;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.GraphLink.GraphIdContainer;
import org.pathvisio.core.model.PathwayElement.MAnchor;
import org.pathvisio.core.view.VPathway;

/**
 * This class is dedicated to Interaction Comparison and its related methods 
 * @author Praveen Kumar
 */
public class InteractionsComparison 
{
	private PathwaysInfo pathwaysInfo;
	private List<List<PathwayElement>> dataNodeComparisonResults;
	
	private int interactionsCountInPw1, interactionsCountInPw2;
	//getter methods 
	int getInteractionsCountInPw1(){
		return interactionsCountInPw1;
	}
	int getInteractionsCountInPw2(){
		return interactionsCountInPw2;
	}
	
	//constructor
	public InteractionsComparison(PathwaysInfo pathwaysInfo, List<List<PathwayElement>> dataNodeComparisonResults)
	{
		this.dataNodeComparisonResults = dataNodeComparisonResults;
		this.pathwaysInfo = pathwaysInfo;
	}
	
	/**
	 * Returns List of corresponding GraphId strings for the input list of MAnchors 
	 * @param mAnchorList input list of MAnchors
	 */
	private List<String> getAnchorGraphIds(List<MAnchor> mAnchorList)
	{
		if(mAnchorList.isEmpty())
			return null;
		List<String> anchorGraphIds = new ArrayList<String>();
		for(MAnchor mAnchor : mAnchorList)
			anchorGraphIds.add(mAnchor.getGraphId());
		return anchorGraphIds;
	}
	
	/**
	 * Returns a list of unique Datanodes in a pathway, from a one-to-one Datanode comparsion results.
	 * 
	 * @param pathway : either pathway1 or pathway2
	 * @param dataNodeComparisonResults : list containing the results of Datanode comparison. 
	 */
	private List<PathwayElement> prepareUniqueDNsList(Pathway pathway, List<List<PathwayElement>> dataNodeComparisonResults)
	{
		List<PathwayElement> uniqueDNList = new ArrayList<PathwayElement>();
		
		int leftOrRightOfTheMainList = 0; // default left, 1 right
		if(pathway == pathwaysInfo.getPathway2())
			leftOrRightOfTheMainList = 1;
		
		for(List<PathwayElement> eachDNMatch : dataNodeComparisonResults)
		{
			PathwayElement pweDN = eachDNMatch.get(leftOrRightOfTheMainList);
			if( !uniqueDNList.contains( pweDN ) ) 
				uniqueDNList.add(pweDN);	
		}
		
		return uniqueDNList;
	}
	
	/**
	 * prepares a one-to-many list of DN matches from pathway1 to pathway2.
	 * 
	 * @param dataNodeComparisonResults : one-to-one Datanode comparison results.  
	 * 
	 * @return one-to-many Datanode comparison results.
	 */
	private HashMap<PathwayElement, List<PathwayElement>> prepareDNComparisonListMap(List<List<PathwayElement>> dataNodeComparisonResults)
	{
		HashMap<PathwayElement, List<PathwayElement>> mappedDNComparisonResults = new HashMap<PathwayElement, List<PathwayElement>>();
		PathwayElement previousPwe = null, currentPwe = null;
		List<PathwayElement> pweListInPathway2 = null;
		
		for(List<PathwayElement> eachDNMatch : dataNodeComparisonResults)
		{
			currentPwe = eachDNMatch.get(0);
			if(previousPwe != currentPwe)
			{
				if(previousPwe != null)
					mappedDNComparisonResults.put(previousPwe, pweListInPathway2);
				pweListInPathway2 = new ArrayList<PathwayElement>();
				
			}
			pweListInPathway2.add(eachDNMatch.get(1));
			previousPwe = currentPwe;
		}
		mappedDNComparisonResults.put(previousPwe, pweListInPathway2);
		return mappedDNComparisonResults;
	}
	
  /**
	* checks if the line interacts with a DN and an anchor (or) 2 anchors (or) 2 DNs at its ends
	*
	* @param line : the line under consideration.
	* @param pathway : pathway in which the line is present.
	* @param matchingDNsInThePathway : list of DNs in a pathway which match with the DNs in the other pathway.
	*
	* @return -1: if the line fails the above criteria;
	* 0: line has 2 anchors at its ends;
	* 1: one DN and one anchor at its ends;
	* 2: both DNs at its ends
	*/
	private int checkIfLineInteractsWithDNsOrAnchors(MLine line, 
			List<PathwayElement> matchingDNsInThePathway, Pathway pathway)
	{
		int numberOfDns = 0;
		for(int i = 0; i < 2; i++) //i = 0 means the startGraphRef, i = 1: endGraphRef, the 2 count loop is to get both the end-points of the line 
		{
			String endPointGraphRef = (i == 0) ? line.getStartGraphRef(): line.getEndGraphRef();
			
			GraphIdContainer lineEndPoint = pathway.getGraphIdContainer(endPointGraphRef);
			if(lineEndPoint == null || !(lineEndPoint instanceof PathwayElement 
					|| lineEndPoint instanceof MAnchor) ) //If its not a DN or an MAnchor
				return -1;
			else if(lineEndPoint instanceof PathwayElement) 
			{
				PathwayElement pwe = (PathwayElement) lineEndPoint;
					if(pwe.getObjectType().equals(ObjectType.DATANODE))
					{
						if(!matchingDNsInThePathway.contains(pwe))
							return -1;
						else
							numberOfDns++;
					} else //its a PWE but not DN
						return -1; 
			}
		}
		
		return numberOfDns;
	}
	
	/**
	 * categorizes the PWEs into lines that interact with a DN and an anchor (or) 2 anchors (or) 2 DNs at its ends
	 * @param pathway : pathway in which to look for the lines.
	 * @return returns 2 lists of lines. One that has 2 DNs at its ends and the other lines that doesn't.
	 */
	public List<List<MLine>> categorizePWEIntoLinesForInteractionComparison(Pathway pathway)
	{
		List<MLine> linesWithTwoDNs = new ArrayList<MLine>();
		List<MLine> otherLines = new ArrayList<MLine>(); // lines referring to a DN and an anchor (or) two anchors
		List<List<MLine>> resultantLines = new ArrayList<List<MLine>>();
		
		List<PathwayElement> matchingDNsInThePathway = prepareUniqueDNsList(pathway, dataNodeComparisonResults);
		for(PathwayElement pwe: pathway.getDataObjects())
		{
			if(pwe instanceof MLine) 
			{
				MLine eachLine = (MLine)pwe;
				int numberOfDNs = checkIfLineInteractsWithDNsOrAnchors(eachLine, matchingDNsInThePathway, pathway);
				if(numberOfDNs == 2)
					linesWithTwoDNs.add(eachLine);
				else if (numberOfDNs != -1) // either 0 or 1 
					otherLines.add(eachLine);
			}
		}
		resultantLines.add(linesWithTwoDNs);
		resultantLines.add(otherLines);
		return resultantLines;
	}
	
	/**
	 * Finds out all the interactions in a given pathway. 
	 * assumptions : only the first and the last points of a line are considered.
	 * 
	 * @param pathway: pathway in which to look for the interactions.
	 * 
	 * @return List of interactions in the pathway. 
	 * An interaction consists of interaction partners (i.e PathwayElements, which can be an MLine or Datanode).
	 * Thus the interaction is represented as Set<PathwayElement>. 
	 */   
	public List< Set<PathwayElement> > findOutInteractionsInPathway(Pathway pathway)
	{
		List< Set<PathwayElement> > interactionsListInPathway = new ArrayList<Set<PathwayElement>>();
		
		List<List<MLine>> probableInteractionLines = categorizePWEIntoLinesForInteractionComparison(pathway);
		List<MLine> linesWithTwoDns = probableInteractionLines.get(0);
		List<MLine> linesWithDnsAndAnchors = probableInteractionLines.get(1);

		List<MLine> resultantLines = new ArrayList<MLine>();
		resultantLines.addAll(linesWithDnsAndAnchors);
		resultantLines.addAll(linesWithTwoDns);
		
		List<PathwayElement> linesNoLongerNeeded = new ArrayList<PathwayElement>();
		
		for(MLine eachRootLine : linesWithTwoDns)
		{
			if(linesNoLongerNeeded.contains(eachRootLine))
				continue;
			linesNoLongerNeeded.add(eachRootLine);
			
			// contains the interaction partners. This will be added to the main list which maintains the interactions list
			Set<PathwayElement> possibleInteractionPartnerListWithThisLine = new HashSet<PathwayElement>();  
			Set<String> anchorsPresentInThePossibleInteraction = new HashSet<String>(); // for simplicity represent the anchors by the string IDs
			
			//add the line and its partners to the list
			possibleInteractionPartnerListWithThisLine
				.add((PathwayElement) pathway.getGraphIdContainer(eachRootLine.getStartGraphRef()));
			possibleInteractionPartnerListWithThisLine
				.add((PathwayElement) pathway.getGraphIdContainer(eachRootLine.getEndGraphRef()));
			possibleInteractionPartnerListWithThisLine.add(eachRootLine);
			
			//these are the anchors present on the line itself
			List<String> eachLineAnchorGraphIds = getAnchorGraphIds(eachRootLine.getMAnchors());
			if(eachLineAnchorGraphIds != null)
				anchorsPresentInThePossibleInteraction.addAll(eachLineAnchorGraphIds); // the new lines present can connect to these anchors.
			else {// if there are no anchors on the Root Line, Interaction consists of just the two DNs and the line connecting them
				interactionsListInPathway.add(possibleInteractionPartnerListWithThisLine);
				continue;
			}
			
			for(int j=0 ; j < resultantLines.size() ;j++)
			{
				PathwayElement allOtherLine = resultantLines.get(j);
				if(linesNoLongerNeeded.contains(allOtherLine)) // if the line is already part of the interaction, then skip 
					continue;
				if(anchorsPresentInThePossibleInteraction.isEmpty()) // if this Anchors List is empty, then no other lines can be part of this interaction
					break;
				
				boolean lineAndAnchorInteract = false;
				List<String> anchorsToRemove = new ArrayList<String>();
				//anchors to which the lines' end-points may refer to 
				String startGraphRefId_otherLine = allOtherLine.getStartGraphRef();
				GraphIdContainer lineEndPoint1 = pathway.getGraphIdContainer(startGraphRefId_otherLine);
				if(lineEndPoint1 instanceof MAnchor)
					if(anchorsPresentInThePossibleInteraction.contains(startGraphRefId_otherLine)){
						lineAndAnchorInteract = true;
						anchorsToRemove.add(startGraphRefId_otherLine);
					}
				
				String endGraphRefId_otherLine = allOtherLine.getEndGraphRef();
				GraphIdContainer lineEndPoint2 = pathway.getGraphIdContainer(endGraphRefId_otherLine);
				if(lineEndPoint2 instanceof MAnchor)
					if(anchorsPresentInThePossibleInteraction.contains(endGraphRefId_otherLine)){
						lineAndAnchorInteract = true;
						anchorsToRemove.add(endGraphRefId_otherLine);
					}
				
				//these are the anchors which are present on the line itself
				List<String> allOtherLineAnchorGraphIds = getAnchorGraphIds(allOtherLine.getMAnchors());
				if(allOtherLineAnchorGraphIds != null)
				{
					for (String anchorGraphId : allOtherLineAnchorGraphIds)
					{
						if(anchorsPresentInThePossibleInteraction.contains(anchorGraphId))
						{
							lineAndAnchorInteract = true;
							anchorsToRemove.add(anchorGraphId); // as it cannot be removed directly while the list is in a loop operation
							break;
						}
					}
				}
				
				if(lineAndAnchorInteract)
				{
					j = -1; // to start over the loop; as it may contain lines that might have elements present in the latest element-comparison-list
										
					if(allOtherLineAnchorGraphIds != null)
						anchorsPresentInThePossibleInteraction.addAll(allOtherLineAnchorGraphIds);
					
					if(lineEndPoint1 instanceof PathwayElement)
						possibleInteractionPartnerListWithThisLine.add((PathwayElement)lineEndPoint1);
					else // if not DN, then its an MAnchor
						anchorsPresentInThePossibleInteraction.add(startGraphRefId_otherLine);
					
					if(lineEndPoint2 instanceof PathwayElement)
						possibleInteractionPartnerListWithThisLine.add((PathwayElement)lineEndPoint2);
					else
						anchorsPresentInThePossibleInteraction.add(endGraphRefId_otherLine);
				
					possibleInteractionPartnerListWithThisLine.add(allOtherLine);
					linesNoLongerNeeded.add(allOtherLine);
				}
				anchorsPresentInThePossibleInteraction.removeAll(anchorsToRemove);
			}
			
			interactionsListInPathway.add(possibleInteractionPartnerListWithThisLine);
		}
		return interactionsListInPathway;
	}
	
	/**
	 * compares the interactions from pathway1 against pathway2's using other methods like 'findOutInteractionsInPathway()'. 
	 * An Interaction from pathway1 is compared to an interaction in pathway2 using just the DNs for now.
	 * i.e if all the DNs in the 2 interactions being compared are the same, then the interactions are considered to be the same. 
	 * And before the comparison begins, the count of interaction partners (lines and datanodes) in the 2 pathways are first checked 
	 * to see if they are the same, only then the comparison is made.  
	 * 
	 * @return List of matching interactions in the 2 pathways. 
	 * Outer List represents the list of Matches. Inner List has the a pair of matching interactions. 
	 * Its first element represents an interaction in Pathway1 and the 2nd element an interaction in Pathway2.
	 * Set<PathwayElement> indicates the interaction partners in a interaction.     
	 */
	public List<List<Set<PathwayElement>>> compareInteractions()
	{
		//not using an array of HashSet here because there seems to be some problem with array of collection in Java 
		List<List<Set<PathwayElement>>> matchingInteractionsList = new ArrayList<List<Set<PathwayElement>>>();
		if(dataNodeComparisonResults.size() == 0) // no need to check for interactions
			return matchingInteractionsList;
		
		HashMap <PathwayElement, List<PathwayElement>> mappedDNcomparisonList = prepareDNComparisonListMap(dataNodeComparisonResults);
		/*for(Map.Entry<PathwayElement, ArrayList<PathwayElement>> mapEntry : mappedDNcomparisonList.entrySet()){
			System.out.println(mapEntry);
		}*/
		
		List<Set<PathwayElement>> interactionsListInPathway1 = findOutInteractionsInPathway(pathwaysInfo.getPathway1());
		interactionsCountInPw1 = interactionsListInPathway1.size();
		//System.out.println("interaction list in pathway1 --- ");
		//printResults(interactionsListInPathway1, pathwaysInfo.getVPathway1());
		
		List<Set<PathwayElement>> interactionsListInPathway2 = findOutInteractionsInPathway(pathwaysInfo.getPathway2());
		interactionsCountInPw2 = interactionsListInPathway2.size();
		//System.out.println("interaction list in pathway2 --- ");
		//printResults(interactionsListInPathway2, pathwaysInfo.getVPathway2());
		
		//comparing the interactions found in pathway 1 and 2
		for(Set<PathwayElement> eachInteractionPw1 : interactionsListInPathway1)
		{
			for(Set<PathwayElement> eachInteractionPw2 : interactionsListInPathway2)
			{
				if(eachInteractionPw1.size() != eachInteractionPw2.size()) //since the count of interacting partners are not same, skip this interaction
					continue;
				
				boolean interactionMatchFound = true; 
				for(PathwayElement eachPWEInPW1 : eachInteractionPw1)
				{
					if(eachPWEInPW1 instanceof MLine) // do not use Lines for now, only DN comparison
						continue;
				
					boolean pweInPw1Matches = false;
					List<PathwayElement> listOfPWEsMapped = mappedDNcomparisonList.get(eachPWEInPW1);
					for(PathwayElement eachPWEInPw2 : eachInteractionPw2)
					{
						if(eachPWEInPw2 instanceof MLine) 
							continue;
						if(listOfPWEsMapped.contains(eachPWEInPw2))
						{
							pweInPw1Matches = true;
							break;
						}
					}
					if(!pweInPw1Matches){ // i.e one of the DNs in the interaction from pathway1 did not match any of the DNs in an interaction from Pathway2. Thus discard the interaction from pathways 2 
						interactionMatchFound = false;
						break;
					}
				}
				if(interactionMatchFound)
				{
					List<Set<PathwayElement>> oneSetOfMatchingInteraction = new ArrayList<Set<PathwayElement>>();
					oneSetOfMatchingInteraction.add(eachInteractionPw1);
					oneSetOfMatchingInteraction.add(eachInteractionPw2);
					matchingInteractionsList.add(oneSetOfMatchingInteraction);
					//System.out.println("interaction Match");
				}
			}
		}
		return matchingInteractionsList;
	}
	
	/**
	 * Can be used to print interaction comparison results on to the console. 
	 * @param interactionsListInPathway : interaction comparison results which are to be printed.
	 */
	private void printResults(List<Set<PathwayElement>> interactionsListInPathway)
	{
		int interactionIndex = 0;
		for(Set<PathwayElement> eachInteraction : interactionsListInPathway)
		{
			System.out.println(eachInteraction);
			/*if(interactionIndex == 0){ // highlight only a particular interaction in the list
				vPathway.resetHighlight();
				for(PathwayElement pwe: eachInteraction)
					Utility.highlightPWE(pwe, vPathway, false);
			}*/
			interactionIndex ++;
		}
	}
}

