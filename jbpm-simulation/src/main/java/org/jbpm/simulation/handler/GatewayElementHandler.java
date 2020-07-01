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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipse.bpmn2.ExclusiveGateway;
import org.eclipse.bpmn2.FlowElement;
import org.eclipse.bpmn2.InclusiveGateway;
import org.eclipse.bpmn2.ParallelGateway;
import org.eclipse.bpmn2.SequenceFlow;
import org.jbpm.simulation.PathContext;
import org.jbpm.simulation.PathContext.Type;
import org.jbpm.simulation.PathContextManager;

public class GatewayElementHandler extends MainElementHandler {
    
    private PathContextManager manager;

    public boolean handle(FlowElement element, PathContextManager manager) {
        
        this.manager = manager;
        if (element instanceof ExclusiveGateway) {
            handleExclusiveGateway(getOutgoing(element));
            return true;
        } else if (element instanceof InclusiveGateway) {
            handleInclusiveGateway(getOutgoing(element));
            return true;
        } else if (element instanceof ParallelGateway) {
            handleParallelGateway(getOutgoing(element));
            return true;
        } else {
            throw new UnsupportedOperationException("Not supported element to handle " + element.eClass().getName());
        }

    }

    protected void handleExclusiveGateway(List<SequenceFlow> outgoing) {
        List<PathContext> locked = new ArrayList<PathContext>();
        Stack<PathContext> contextsAtThisNode = manager.getContextsFromStack();

        for (PathContext contextAtThisNode : contextsAtThisNode) {

            for (SequenceFlow seqFlow : outgoing) {

                FlowElement target = seqFlow.getTargetRef();
                if (!contextAtThisNode.getVisitedSplitPoint().contains(seqFlow)) {
                    PathContext separatePath = manager.cloneGiven(contextAtThisNode);
                    separatePath.addVisitedSplitPoint(seqFlow);
                    manager.addToPath(seqFlow, separatePath);
                    super.handle(target, manager);
                    separatePath.setLocked(true);
                    locked.add(separatePath);
                }
            }
        }
        // unlock
        for (PathContext ctx : locked) {
            ctx.setLocked(false);
        }
    }

    protected void handleInclusiveGateway(List<SequenceFlow> outgoing) {
        // firstly cover simple xor based - number of paths is equal to number
        // of outgoing
        handleExclusiveGateway(outgoing);
        Type currentType = manager.getContextFromStack().getType();
        manager.getContextFromStack().setType(Type.ROOT);

        // next cover all combinations of paths
        if (outgoing.size() > 2) {
            List<SequenceFlow> copy = new ArrayList<SequenceFlow>(outgoing);
            List<SequenceFlow> andCombination = null;
            for (SequenceFlow flow : outgoing) {

                // first remove one that we currently processing as that is not
                // a combination
                copy.remove(flow);
                PathContext contextAtThisNode = manager.cloneGivenWithoutPush(manager.getContextFromStack());
                for (SequenceFlow copyFlow : copy) {
                    manager.cloneGiven(contextAtThisNode);


                    andCombination = new ArrayList<SequenceFlow>();
                    andCombination.add(flow);
                    andCombination.add(copyFlow);

                    handleParallelGateway(andCombination);
                }
            }
        }
        manager.getContextFromStack().setType(Type.ROOT);
        // lastly cover and based - is single path that goes through all at the
        // same time
        handleParallelGateway(outgoing);
        manager.getContextFromStack().setType(currentType);

    }

    protected void handleParallelGateway(List<SequenceFlow> outgoing) {
        PathContext context = manager.getContextFromStack();
        boolean canBeFinished = context.isCanBeFinished();
        System.out.println("getting canBeFinished in handling Parallel gateway: "+canBeFinished);
        System.out.println("setting canBeFinished to false");
        context.setCanBeFinished(false);
        manager.addAllToPath(outgoing, context);
        int counter = 0;
        System.out.println("iterating outgoing flows for element");
        for (SequenceFlow seqFlow : outgoing) {
            counter++;
            FlowElement target = seqFlow.getTargetRef();
            System.out.println("target element: "+target.getName());
            if (counter == outgoing.size()) {
            	System.out.println("counter == outgoing.size(), meaning we have processed all the branches of this parallel gateway split");
                if (manager.getPaths().size() == 1) {
                	System.out.println("manager.getPaths().size() == 1");
                	System.out.println("setting canBeFinished to: "+canBeFinished);
                    //context.setCanBeFinished(canBeFinished); //Hongchao 2020-0701 this is the original code. 
                	//When there are multiple parallel gateway split-join combinations, the "canBeFinishedCounter" in context does not update correctly. 
                	//This is because the code continues to add to the counter while subtracting from the counter should be the correct behavior when all branches have been executed. 
                	//To reproduce the problem, create a model with sequential split-join parallel gateway combinations, then in the Workbench use "paths": the simulator will fail to find paths because it cannot correctly finish traversing the paths. 
                	//as a fix, put TRUE to this function rather than using the saved boolean value
                	context.setCanBeFinished(true); //Hongchao 2020-0701 fix parallelgateway converging problem

                } else {
                    Iterator<PathContext> it = manager.getPaths().iterator();

                    while (it.hasNext()) {
                        PathContext pathContext = (PathContext) it.next();
                        if (pathContext.getType() == Type.ACTIVE) {
                        	System.out.println("pathContext.getType() == Type.ACTIVE");
                            pathContext.setCanBeFinished(canBeFinished);
                        }
                    }
                }
            }
            
            super.handle(target, manager);
        }
        // finalize paths if there are any to cover scenario when there was not converging parallel gateway
        if (canBeFinished) {

            for (SequenceFlow seqFlow : outgoing) {
                manager.addToPath(seqFlow, context);
                manager.addToPath(seqFlow.getTargetRef(), context);
            }
            manager.finalizePathOnLeave();
            
        }
    }
}
