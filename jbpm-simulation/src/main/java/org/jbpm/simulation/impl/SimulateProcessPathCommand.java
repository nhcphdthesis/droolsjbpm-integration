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

import java.util.ArrayList;
import java.util.List;

import org.drools.core.command.impl.RegistryContext;
import org.drools.core.event.DefaultProcessEventListener;
import org.drools.core.time.SessionPseudoClock;
import org.jbpm.simulation.SimulationContext;
import org.jbpm.simulation.SimulationInfo;
import org.jbpm.simulation.impl.events.ProcessInstanceEndSimulationEvent;
import org.kie.api.command.ExecutableCommand;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.runtime.Context;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;

public class SimulateProcessPathCommand implements ExecutableCommand<KieSession> {

    private static final long serialVersionUID = 3485947845100224769L;

    private String processId;
    private SimulationContext simContext;
    private SimulationPath path;
    
    public SimulateProcessPathCommand(String processId, SimulationContext context, SimulationPath path) {
    	System.out.println("initializing simulateprocessoathcommand for process id: "+processId);
        this.processId = processId;
        this.simContext = context;
        this.path = path;
    }
    
    public KieSession execute(Context context ) {
        
        KieSession session = ((RegistryContext)context).lookup(KieSession.class);
        SystemOutLogger logger = (SystemOutLogger) session.getGlobal("logger");
        if (logger==null) {
        	logger = new SystemOutLogger();
        	//session.setGlobal("logger", logger);
        }
        logger.setLog(true);
        logger.log("inside command with context: "+context.toString());
        session.getEnvironment().set("NodeInstanceFactoryRegistry", SimulationNodeInstanceFactoryRegistry.getInstance());
        simContext.setClock((SessionPseudoClock) session.getSessionClock());
        simContext.setCurrentPath(path);
        SimulationInfo simInfo = simContext.getRepository().getSimulationInfo();
        if (simInfo != null) {
//        	System.out.println("sesson: "+session);
//        	System.out.println("kieBase: "+session.getKieBase());
//        	System.out.println("process: "+session.getKieBase().getProcess(processId));
            simInfo.setProcessName(session.getKieBase().getProcess(processId).getName());
            simInfo.setProcessVersion(session.getKieBase().getProcess(processId).getVersion());
        }
        // reset max end time before starting new instance
        simContext.resetMaxEndTime();
        simContext.getExecutedNodes().clear();
        simContext.incrementProcessInstanceId();
        logger.log("registerng real world start time");
        //simContext.setRealworldStartTime(simContext.getClock().getCurrentTime()); //Hongchao register the start time of all simulations

        long instanceId = -1;
        ProcessInstance pi = null;
        if (path.getSignalName() != null) {
        	logger.log("path.getSignalName list not null");
            final List<ProcessInstance> instances = new ArrayList<ProcessInstance>();
            session.addEventListener(new DefaultProcessEventListener() {
                @Override
                public void beforeProcessStarted(ProcessStartedEvent event) {
                	System.out.println("before processStarted: adding instance "+event.getProcessInstance().getId());
                    instances.add(event.getProcessInstance());
                }
            });
            logger.log("signaling: "+path.getSignalName());
            session.signalEvent(path.getSignalName(), null);
            if (!instances.isEmpty()) {
                pi = instances.get(0);
                instanceId = session.getIdentifier()+pi.getId();
            }

        } else {
        	logger.log("starting process instance: "+processId);
            pi = session.startProcess(processId);
            instanceId = session.getIdentifier()+pi.getId();
            logger.log(String.format("process instance started: %s",pi.toString()));
        }
        logger.log("storing process end event: "+instanceId);
        simContext.getRepository().storeEvent(new ProcessInstanceEndSimulationEvent(processId, instanceId,
                simContext.getStartTime(), simContext.getMaxEndTime(), path.getPathId(),
                pi.getProcessName(), pi.getProcess().getVersion()));

        return session;

    }

}
