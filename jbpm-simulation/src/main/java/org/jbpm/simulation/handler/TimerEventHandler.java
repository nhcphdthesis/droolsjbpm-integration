package org.jbpm.simulation.handler;

import java.util.List;

import org.eclipse.bpmn2.FlowElement;
import org.eclipse.bpmn2.SequenceFlow;
import org.jbpm.simulation.PathContextManager;

public class TimerEventHandler extends DefaultElementHandler {
	public boolean handle(FlowElement element, PathContextManager manager) {
        List<SequenceFlow> outgoing = getOutgoing(element);
        if (outgoing.isEmpty()) {
            return false;
        }
        for (SequenceFlow seqFlow : outgoing) {
            FlowElement target = seqFlow.getTargetRef();
            manager.addToPath(seqFlow, manager.getContextFromStack());
            super.handle(target, manager);
        }
        return true;
    }
}
