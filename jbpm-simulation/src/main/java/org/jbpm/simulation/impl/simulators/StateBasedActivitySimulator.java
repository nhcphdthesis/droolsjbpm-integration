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

package org.jbpm.simulation.impl.simulators;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.drools.core.time.TimeUtils;
import org.jbpm.simulation.ActivitySimulator;
import org.jbpm.simulation.SimulationContext;
import org.jbpm.simulation.SimulationDataProvider;
import org.jbpm.simulation.SimulationEvent;
import org.jbpm.simulation.TimeGenerator;
import org.jbpm.simulation.TimeGeneratorFactory;
import org.jbpm.simulation.impl.events.ActivitySimulationEvent;
import org.jbpm.workflow.core.node.TimerNode;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.kie.api.definition.process.Node;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.api.runtime.process.ProcessInstance;

public class StateBasedActivitySimulator implements ActivitySimulator {

    public SimulationEvent simulate(Object activity, SimulationContext context) {
       NodeInstance stateNode = (NodeInstance) activity;
       System.out.println("Simulate State-based activity: "+stateNode.toString());
       long startTime = context.getClock().getCurrentTime();
       Map<String, Object> metadata = stateNode.getNode().getMetaData();
       
       ProcessInstance pi = stateNode.getProcessInstance();
       Node node = stateNode.getNode();
       String bpmn2NodeId = (String) metadata.get("UniqueId");
       SimulationDataProvider provider = context.getDataProvider();
       
       TimeGenerator timeGenerator=TimeGeneratorFactory.newTimeGenerator(provider.getSimulationDataForNode(node));
       long duration = timeGenerator.generateTime();
       if (node instanceof TimerNode){//is timer event
    	   
       	System.out.println("simulating TimerNode: "+((TimerNode)node).getTimer().toString());
       	long delay = TimeUtils.parseTimeString(((TimerNode)node).getTimer().getDelay());
       	
       	HashMap scheduleTable = new HashMap(); //later we can use this table to store scheduled time and calculate delay dynamically
       	String proc_ist_id_str = String.format("%d",stateNode.getProcessInstance().getId());
       	scheduleTable.put(proc_ist_id_str, DateTime.now());
       	//we can specify the scheduled timeslots, and we can get the time from the pysudo clock, so we can calculate how long a timer need to delay
       	System.out.println(String.format("delay: %d for process instance %s. HashMap has values: %s, current time in PseudoClock: %d, parsed as: %s",
       			delay,
       			proc_ist_id_str,
       			scheduleTable.get(proc_ist_id_str).toString(),
       			context.getClock().getCurrentTime(),DateTimeFormat.longDateTime().print(context.getClock().getCurrentTime())));
       	duration = delay; //2*60*1000;//set to 2min
       }
       System.out.println(String.format("advancing time for state-based activity: %d",duration));
       context.getClock().advanceTime(duration, TimeUnit.MILLISECONDS);
       // set end time for processinstance end time
       context.setMaxEndTime(context.getClock().getCurrentTime());

       String type = (String) provider.getProcessDataForNode(node).get("node.type");

       return new ActivitySimulationEvent(pi.getProcessId(), context.getProcessInstanceId(), node.getName(), bpmn2NodeId, duration,
               startTime, context.getClock().getCurrentTime(), type);
    }

}
