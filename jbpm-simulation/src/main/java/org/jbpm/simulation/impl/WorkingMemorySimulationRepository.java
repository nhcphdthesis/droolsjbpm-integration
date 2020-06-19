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

package org.jbpm.simulation.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jbpm.simulation.AggregatedSimulationEvent;
import org.jbpm.simulation.SimulationEvent;
import org.jbpm.simulation.impl.events.AggregatedActivitySimulationEvent;
import org.jbpm.simulation.impl.events.AggregatedEndEventSimulationEvent;
import org.jbpm.simulation.impl.events.AggregatedProcessSimulationEvent;
import org.jbpm.simulation.impl.events.HumanTaskActivitySimulationEvent;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;



public class WorkingMemorySimulationRepository extends InMemorySimulationRepository {

    private KieSession ksession;
    private boolean fireRulesOnStore = false;
    
    public WorkingMemorySimulationRepository() {
        
    }
    
    public WorkingMemorySimulationRepository(String... rules) {
        this(false, rules);
    }
    
    public WorkingMemorySimulationRepository(Resource... rules) {
        this(false, rules);
    }
    
    public WorkingMemorySimulationRepository(boolean fireRulesOnStore, Resource... rules) {
        this.fireRulesOnStore = fireRulesOnStore;
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        
        for (Resource path : rules) {
        	System.out.println(String.format("adding resource rule: %s", path.toString()));
            kbuilder.add(path, ResourceType.DRL);
        }
        if (kbuilder.hasErrors()) {
            throw new RuntimeException("Error while building knowledge base: " + kbuilder.getErrors());
        }
        
        this.ksession = kbuilder.newKieBase().newKieSession();
        try {
            // register global for aggregated events
            ksession.setGlobal("logger", new SystemOutLogger());
            ksession.setGlobal("simulation", new ArrayList<AggregatedActivitySimulationEvent>());
            ksession.setGlobal("summary", new ArrayList<AggregatedActivitySimulationEvent>());            
            AggregatedProcessSimulationEvent init = new AggregatedProcessSimulationEvent("", 0, 0, 0);
            List processOnlyList = new ArrayList<AggregatedSimulationEvent>();
            processOnlyList.add(init);
            ksession.setGlobal("processEventsOnly", processOnlyList);
        } catch (Exception e) {
            // catch it as there could be no simulation global declared
        }
    }
    
    public WorkingMemorySimulationRepository(boolean fireRulesOnStore, String... rules) {
        this.fireRulesOnStore = fireRulesOnStore;
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        
        for (String path : rules) {
        	System.out.println(String.format("adding rule: %s", path));
            kbuilder.add(ResourceFactory.newClassPathResource(path), ResourceType.DRL);
        }
        if (kbuilder.hasErrors()) {
            throw new RuntimeException("Error while building knowledge base: " + kbuilder.getErrors());
        }
        
        this.ksession = kbuilder.newKieBase().newKieSession();
        try {
            ksession.setGlobal("logger", new SystemOutLogger());
            // register global for aggregated events
            ksession.setGlobal("simulation", new ArrayList<AggregatedActivitySimulationEvent>());
        } catch (Exception e) {
            // catch it as there could be no simulation global declared
        }
    }
    
    public void storeEvent(SimulationEvent event) {
        super.storeEvent(event);
        System.out.println(String.format("storing event in WMSR. session: %s, event: %s", ksession.toString(),event.toString()));
        ksession.insert(event);
        if (fireRulesOnStore) {
        	System.out.println("firing rules when storing event");
            ksession.fireAllRules();
        }
    }

    public void fireAllRules() {
    	System.out.println("firing rules by calling");
        ksession.fireAllRules();
    }
    
    public KieSession getSession() {
        return this.ksession;
    }
    
    public List<AggregatedSimulationEvent> getAggregatedEvents() {
        return (List<AggregatedSimulationEvent>) this.ksession.getGlobal("simulation");
    }
    
    public Object getGlobal(String globalName) {
        return  this.ksession.getGlobal(globalName);
    }

    @Override
    public void close() {
    	//Hongchao, outputs events
    	System.out.println("outputing event log in WM close(): ");
//    	for (SimulationEvent e : this.getEvents()) {
//    		System.out.println(e.toString());
//    	}
    	DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    	try (
                BufferedWriter writer = Files.newBufferedWriter(Paths.get("./simulation_log.csv"));

                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                        .withHeader("process-instance-id", "event-name",  "startTime","endTime","event-id","event-type"));
    			
            ) {
    		for (SimulationEvent e : this.getEvents()) {
    			String event_name="";
    			String event_id="";
    			String event_type=e.getType();
    			if (event_type ==null) {
    				event_type="unknown";
    			}
    			String startTime="";
    			String endTime = "";
    			boolean output=true;
    			switch (event_type) {
    			case "userTask":
    				HumanTaskActivitySimulationEvent hte = (HumanTaskActivitySimulationEvent)e;
    				event_name = hte.getActivityName();
    				event_id = hte.getActivityId();
    				startTime=dateFormat.format(new Date(e.getStartTime()));
    				endTime = dateFormat.format(new Date(e.getEndTime()));
    				break;
    			case "startEvent":
    				event_name="startEvent";
    				startTime=dateFormat.format(new Date(e.getStartTime()));
    				endTime=startTime;
    				break;
    			case "endEvent":
    				event_name="endEvent";
    				endTime = dateFormat.format(new Date(e.getEndTime()));
    				startTime=endTime;
    				break;
    			case "process-instance":
    				output=false;
    				break;
    			default:
    				output=false;
    				break;
    			}
        		if(output) {
        			csvPrinter.printRecord(e.getProcessInstanceId(), event_name, startTime,endTime,event_id,event_type);
        		}
        	}
                
                //csvPrinter.printRecord("2", "Satya Nadella", "CEO", "Microsoft");

                csvPrinter.flush();     
                csvPrinter.close();
            } catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        super.close();
        this.ksession.dispose();
    }
    
    
}
