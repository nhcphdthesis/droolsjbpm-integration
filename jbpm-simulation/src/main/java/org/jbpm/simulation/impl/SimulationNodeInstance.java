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

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jbpm.simulation.ActivitySimulator;
import org.jbpm.simulation.SimulationContext;
import org.jbpm.simulation.SimulationEvent;
import org.jbpm.workflow.instance.impl.NodeInstanceImpl;
import org.kie.api.definition.process.Connection;
import org.kie.api.definition.process.Node;
import org.kie.api.runtime.process.NodeInstance;

public class SimulationNodeInstance extends NodeInstanceImpl {

    private static final long serialVersionUID = -1965605499505300424L;

    @Override
    public void internalTrigger(NodeInstance from, String type) {
    	System.out.println("Simulation node instance internal trigger of"+this.getNodeName()+" from: " +from.getNodeName());
        SimulationContext context = SimulationContext.getContext();
        System.out.println("node type: "+getNode().getClass().toString());
        ActivitySimulator simulator = context.getRegistry().getSimulator(getNode());
        
        SimulationEvent event = simulator.simulate(this, context);
        
        context.getRepository().storeEvent(event);
        long thisNodeCurrentTime = context.getClock().getCurrentTime();
        
        List<Connection> outgoing = getNode().getOutgoingConnections().get(org.jbpm.workflow.core.Node.CONNECTION_DEFAULT_TYPE);
        System.out.println("number of outgoing connections: "+outgoing.size());
        for (Connection conn : outgoing) {
        	System.out.println("conn: "+(String)(conn.getMetaData().get("UniqueId"))+", for node id: "+from.getNodeName());
            if (context.getCurrentPath().getSequenceFlowsIds().contains(conn.getMetaData().get("UniqueId"))) {
                // handle loops
                if (context.isLoopLimitExceeded((String) conn.getMetaData().get("UniqueId"))) {
                    continue;
                }
                //Hongchao: problematic here. if the gateway does not go to finish branch (i.e. the condition of the connection is not satisfied), this conneciton should not be activated. 
                context.addExecutedNode((String) conn.getMetaData().get("UniqueId"));
                System.out.println("executed node: "+(String) conn.getMetaData().get("UniqueId"));
                System.out.println("triggering connection in SNI: "+conn.toString());
                triggerConnection(conn);
                // reset clock to the value of this node
                System.out.println("advancing clock in simulation node instance of node: "+from.getNodeName());
                context.getClock().advanceTime((thisNodeCurrentTime - context.getClock().getCurrentTime()), TimeUnit.MILLISECONDS);
            }
        }
        long currentNodeId = getNodeId();
        // handle boundary events
        for (String boundaryEvent : context.getCurrentPath().getBoundaryEventIds()) {
            
            Node boundaryEventNode = null;
            for (Node node : getNode().getNodeContainer().getNodes()) {
                
                if (node.getMetaData().get("UniqueId").equals(boundaryEvent) && 
                        node.getMetaData().get("AttachedTo").equals(getNode().getMetaData().get("UniqueId"))) {
                   
                    boundaryEventNode = node;
                    break;
                }
            }
            if (boundaryEventNode != null) {
                NodeInstance next = ((org.jbpm.workflow.instance.NodeInstanceContainer) getNodeInstanceContainer()).getNodeInstance(boundaryEventNode);
                setNodeId(boundaryEventNode.getId());
                this.trigger(next, org.jbpm.workflow.core.Node.CONNECTION_DEFAULT_TYPE);
            }
        }
        setNodeId(currentNodeId);
    }

}
