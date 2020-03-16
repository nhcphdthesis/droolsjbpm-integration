package org.jbpm.simulation.impl.simulators;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jbpm.simulation.SimulationContext;
import org.jbpm.simulation.SimulationDataProvider;
import org.jbpm.simulation.SimulationEvent;
import org.jbpm.simulation.TimeGenerator;
import org.jbpm.simulation.TimeGeneratorFactory;
import org.jbpm.simulation.impl.BPMN2SimulationDataProvider;
import org.jbpm.simulation.impl.events.ActivitySimulationEvent;
import org.kie.api.definition.process.Node;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.api.runtime.process.ProcessInstance;

//Hongchao Philips introduce timer simulator as a mechanism to allow scheduling. 
public class TimerEventSimulator extends EventSimulator {
	public SimulationEvent simulate(Object activity, SimulationContext context) {
        NodeInstance eventNodeInstance = (NodeInstance) activity;
        System.out.println("similate event activity: "+eventNodeInstance.toString());
        long startTime = context.getClock().getCurrentTime();
        Map<String, Object> metadata = eventNodeInstance.getNode().getMetaData();
        
        ProcessInstance pi = eventNodeInstance.getProcessInstance();
        Node node = eventNodeInstance.getNode();
        String bpmn2NodeId = (String) metadata.get("UniqueId");
        SimulationDataProvider provider = context.getDataProvider();
        
        long scheduledTime; 
        if (provider instanceof BPMN2SimulationDataProvider) {
        	String scheduleString = ((BPMN2SimulationDataProvider)provider).getScheduledTime("1");
        	System.out.println("Reading schedule: "+scheduleString);
        	scheduledTime= startTime+Long.parseLong(scheduleString);
        }else {
        	System.out.println("Schedule not found, using default");
        	scheduledTime= startTime+5000;//Hongchao: this scheduledTime can be read from a schedule table.
        }
        
        //TimeGenerator timeGenerator=TimeGeneratorFactory.newTimeGenerator(provider.getSimulationDataForNode(node));
        long duration = scheduledTime-startTime;//timeGenerator.generateTime();//Hongchao wait for the timer to fire event
        System.out.println(String.format("[Waiting until scheculed time] advancing clock for timer event activity %s with duration %d", eventNodeInstance.toString(),duration));
        context.getClock().advanceTime(duration, TimeUnit.MILLISECONDS);
        // set end time for processinstance end time
        context.setMaxEndTime(context.getClock().getCurrentTime());

        String type = (String) provider.getProcessDataForNode(node).get("node.type");

        return new ActivitySimulationEvent(pi.getProcessId(), context.getProcessInstanceId(), node.getName(), bpmn2NodeId, duration,
                startTime, context.getClock().getCurrentTime(), "schedule-timer");
    }
}
