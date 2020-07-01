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

package org.jbpm.simulation.handler;

import java.util.List;

import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.EndEvent;
import org.eclipse.bpmn2.Event;
import org.eclipse.bpmn2.EventDefinition;
import org.eclipse.bpmn2.FlowElement;
import org.eclipse.bpmn2.Gateway;
import org.eclipse.bpmn2.GatewayDirection;
import org.eclipse.bpmn2.IntermediateThrowEvent;
import org.eclipse.bpmn2.ParallelGateway;
import org.eclipse.bpmn2.SequenceFlow;
import org.eclipse.bpmn2.StartEvent;
import org.eclipse.bpmn2.SubProcess;
import org.jbpm.simulation.PathContext;
import org.jbpm.simulation.PathContextManager;
import org.jbpm.simulation.util.BPMN2Utils;
import org.eclipse.bpmn2.IntermediateCatchEvent;
import org.eclipse.bpmn2.TimerEventDefinition;

public class MainElementHandler implements ElementHandler {
    
    

    public boolean handle(FlowElement element, PathContextManager manager) {
        PathContext context = manager.getContextFromStack();
        if (!(element instanceof SubProcess)) {
        	System.out.println("handling element: "+element.getName());
            manager.addToPath(element, context);
        }
        
        List<SequenceFlow> outgoing = getOutgoing(element);
        System.out.println("outgoing: "+outgoing);
        if (outgoing != null && !outgoing.isEmpty()) {
            boolean handled = false;
            if (element instanceof Gateway) {
                Gateway gateway = ((Gateway) element);
                System.out.println("handling gateway: "+gateway.getName());
                if (gateway.getGatewayDirection() == GatewayDirection.DIVERGING) {
                    
                    handled = HandlerRegistry.getHandler(element).handle(element, manager);
                } else {
                    if (gateway instanceof ParallelGateway) {
                        handled = HandlerRegistry.getHandler(element).handle(element, manager);
                    } else {
                        handled = HandlerRegistry.getHandler().handle(element, manager);
                    }
                }
            } else if (element instanceof Activity) {
            	System.out.println("handling activity: "+element.getName());
                handled = HandlerRegistry.getHandler(element).handle(element, manager);
            } else if (element instanceof IntermediateThrowEvent) {
            	System.out.println("handling intermediate event: "+element.getName());
                handled = HandlerRegistry.getHandler(element).handle(element, manager);
            //} else if (element instanceof IntermediateCatchEvent) {//Hongchao
                //handled = HandlerRegistry.getHandler(element).handle(element, manager);
            } else {
            	System.out.println("handling others: "+element.getName());
                handled = HandlerRegistry.getHandler().handle(element, manager);
            }
            
            if (!handled && BPMN2Utils.isAdHoc(element)) {
            	System.out.println("clearing context: ");
                manager.clearCurrentContext();
            }
        } else {
        	System.out.println("else branch for: "+element.getName());
            ElementHandler handelr = HandlerRegistry.getHandler(element);
            if (handelr != null) {
            	System.out.println("handelr != null");
            	System.out.println("handelr type: "+handelr.getClass().getName());
                boolean handled = handelr.handle(element, manager);
                if (!handled) {
                	System.out.println("not handeled, finalizing path");
                    manager.finalizePath();
                }
            } else {
            	System.out.println("handelr is null, finalizing path");
                manager.finalizePath();
            }
        }
        
        return true;

    }
    
    protected List<EventDefinition> getEventDefinitions(FlowElement startAt) {
        List<EventDefinition> throwDefinitions = null;

        if (startAt instanceof IntermediateThrowEvent) {
            throwDefinitions = ((IntermediateThrowEvent) startAt)
                    .getEventDefinitions();

        } else if (startAt instanceof EndEvent) {
            EndEvent end = (EndEvent) startAt;

            throwDefinitions = end.getEventDefinitions();
            System.out.println("End Event definition: "+throwDefinitions);
        } else if (startAt instanceof IntermediateCatchEvent) { //Hongchao
        	for (EventDefinition ed : ((IntermediateCatchEvent) startAt).getEventDefinitions()) {
        		if (ed instanceof TimerEventDefinition) {
        			System.out.println("Philips returning timer event definitions for: "+((IntermediateCatchEvent) startAt).toString());
        			throwDefinitions = ((IntermediateCatchEvent) startAt).getEventDefinitions();
        			System.out.println("Event definition: "+((TimerEventDefinition) ed).toString());
        			break;
        		}
        	}
        }

        return throwDefinitions;
    }
    
    
    protected List<SequenceFlow> getOutgoing(FlowElement element) {
        List<SequenceFlow> outgoing = null;
        if (element instanceof StartEvent) {

            outgoing = ((StartEvent) element).getOutgoing();
        } else if (element instanceof SubProcess) {
            
            SubProcess subProcess = ((SubProcess) element);
            outgoing = subProcess.getOutgoing();
        } else if (element instanceof Event) {
            
            outgoing = ((Event) element).getOutgoing();
        } else if (element instanceof Activity) {

            outgoing = ((Activity) element).getOutgoing();
        } else if (element instanceof EndEvent) {
            
            outgoing = ((EndEvent) element).getOutgoing();
        } else if (element instanceof Gateway) {
            
            Gateway gateway = ((Gateway) element);
            outgoing = gateway.getOutgoing();
        }
        
        return outgoing;
    }
    
    

}
