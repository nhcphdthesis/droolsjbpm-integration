/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.jbpm.simulation.impl.ht;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.random.RandomData;
import org.apache.commons.math3.random.RandomDataImpl;
import org.jbpm.simulation.SimulationContext;
import org.jbpm.simulation.SimulationDataProvider;
import org.jbpm.simulation.TimeGenerator;
import org.jbpm.simulation.TimeGeneratorFactory;
import org.jbpm.simulation.util.SimulationConstants;
import org.jbpm.simulation.util.SimulationUtils;
import org.kie.api.definition.process.Node;

public class StaffPoolImpl implements StaffPool {
    
    private Map<String, Object> properties;
    //Hongchao introduce roles in staff pool
    private String role;
	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	private int poolSize;
	private long duration;
	private List<Long> allocatedTill = new ArrayList<Long>();
	private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
	//default working hours set to eight hours
	private long workingHours = ( 8 * 60 * 60 * 1000);
	private long poolCapacity = 0;
	
	private long performedWork = 0;
	
	private RandomData randomizer = new RandomDataImpl();
	
	private double resourceCost;
	
	private TimeUnit elementTimeUnit;
	
	private TimeGenerator timeGenerator;
	
	private Map<String, List<Long>> allocatedRanges = new HashMap<String, List<Long>>();
	private RangeChain rangeChain = new RangeChain();

	public StaffPoolImpl(String processId, Node element, double simulationDuration) {
	    
	    SimulationDataProvider provider = SimulationContext.getContext().getDataProvider();
	    
	    properties = provider.getSimulationDataForNode(element);
	    
	    if (SimulationContext.getContext().isTestFeatureEnabled()) {
	    	System.out.println("before assigning staff pool, checking element name: "+element.getName());
	    }
	    
	    timeGenerator=TimeGeneratorFactory.newTimeGenerator(properties);
		
		this.elementTimeUnit = SimulationUtils.getTimeUnit(properties);
		this.poolSize = (int)SimulationUtils.asDouble(properties.get(SimulationConstants.STAFF_AVAILABILITY));
		
		this.duration = timeGenerator.generateTime();
		String workingHoursRange = (String) properties.get("StaffPoolImpl working.hours.range");
		System.out.println(String.format("StaffPoolImpl input working hours range: %s", workingHoursRange));
		if (workingHoursRange != null) {
		    
		    String[] ranges = workingHoursRange.split(",");
		    
		    for (String range : ranges) {
		        String[] rangeElems = range.split("-");
		        rangeChain.addRange(new Range(Integer.parseInt(rangeElems[0]), Integer.parseInt(rangeElems[1]), poolSize));
		    }
		    
		} else {
    		long workingHoursOpt = 12;//(long)SimulationUtils.asDouble(properties.get(SimulationConstants.WORKING_HOURS));
    		System.out.println(String.format("StaffPoolImpl workingHoursOpt: %d",workingHoursOpt));
    		if (workingHoursOpt > 0) {
    			this.workingHours = timeUnit.convert(workingHoursOpt, TimeUnit.HOURS);
    		}
    		int startHour = 8;
    		System.out.println(String.format("StaffPoolImpl adding default working hours range, poolSize: %d",poolSize));
    		rangeChain.addRange(new Range(startHour, (int) (startHour+workingHoursOpt), poolSize));//Hongchao: changed default working hours
		}
		this.poolCapacity = poolSize * this.workingHours;
		
		// if simulation is estimated to more than one day multiply working hours by that factor
		if (simulationDuration > 1) {
			this.poolCapacity = (long) (this.poolCapacity * simulationDuration);
		}
		
		this.resourceCost = SimulationUtils.asDouble(properties.get(SimulationConstants.COST_PER_TIME_UNIT));
		
		
	}
	
	
	protected long allocate(long startTime, long duration) {
		//Hongchao: offset start time to fix the "current time vs. expected start time" problem
		long offsetStartTime = startTime - SimulationContext.getContext().getStartOffset();
		System.out.println(String.format("StaffPoolImpl Allocating resource in StaffPoolImp with start time: %d, duration: %d, offset startTime: %d",startTime,duration,offsetStartTime));
		performedWork += duration;
	    
	    return rangeChain.allocateWork(offsetStartTime, duration);
	}
	

	public long allocate(long startTime) {
		
		return allocate(startTime, this.duration);
	}
	
	public long allocate(long startTime, Node element) {

		long duration = this.duration = timeGenerator.generateTime();
		
		return allocate(startTime, duration);
	}
	
	public double getResourceUtilization() {
        if (poolCapacity == 0) {
            return 0;
        }
        System.out.println(String.format("getting resource utilization. performed work: %d, pool capacity: %d", performedWork,poolCapacity));
		return performedWork * 100 / poolCapacity;
	}

	/* (non-Javadoc)
	 * @see org.onebpm.simulation.engine.api.StaffPool#getResourceCost()
	 */
	public double getResourceCost() {
		
		return this.resourceCost;
	}
	
	
	public TimeUnit getElementTimeUnit() {
		return elementTimeUnit;
	}
	
	protected List<Long> findAllocatedRange(long stringTime) {
	    
	    Calendar c = Calendar.getInstance();
	    c.setTimeInMillis(stringTime);
	    
	    int hour = c.get(Calendar.HOUR_OF_DAY);
	    
	    Set<String> ranges = this.allocatedRanges.keySet();
	    
	    for (String range : ranges) {
	        
	        String[] elems = range.split("-");
	        
	        int lower = Integer.parseInt(elems[0]);
	        int upper = Integer.parseInt(elems[1]);
	        
	        if (hour >= lower && hour <= upper) {
	            return this.allocatedRanges.get(range);
	        }
	        
	    }
	    
	    return null;
	    
	}
}
