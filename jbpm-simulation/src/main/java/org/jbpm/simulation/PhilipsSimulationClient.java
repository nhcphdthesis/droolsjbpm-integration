package org.jbpm.simulation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.drools.core.impl.EnvironmentFactory;
import org.drools.core.impl.KnowledgeBaseFactory;
import org.drools.core.io.impl.InputStreamResource;
import org.drools.core.time.SessionPseudoClock;
import org.jbpm.bpmn2.core.Definitions;
import org.jbpm.simulation.PathFinder;
import org.jbpm.simulation.PathFinderFactory;
import org.jbpm.simulation.SimulationContext;
import org.jbpm.simulation.SimulationContextFactory;
import org.jbpm.simulation.converter.SimulationFilterPathFormatConverter;
import org.jbpm.simulation.impl.HardCodedSimulationDataProvider2;
import org.jbpm.simulation.impl.SimulationNodeInstanceFactoryRegistry;
import org.jbpm.simulation.impl.SimulationPath;
import org.jbpm.simulation.impl.SystemOutLogger;
import org.jbpm.simulation.impl.WorkingMemorySimulationRepository;
import org.jbpm.simulation.util.BPMN2Utils;
import org.kie.api.KieBase;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;

public class PhilipsSimulationClient {

	public static void main(String[] args) {
		//testSimulation();
        runPhilipsSimulation();
	}

	protected static void runPhilipsSimulation() {
		SystemOutLogger logger = new SystemOutLogger();
        logger.setLog(true);
		InputStream bpmn2stream = PhilipsSimulationClient.class.getResourceAsStream("/org.jbpm.CathPathEventSim.v1.0.bpmn2");//CathPathEventSim
        
        try {
			System.out.println(bpmn2stream==null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        InputStreamResource bpmn2resource = new InputStreamResource(bpmn2stream);
        String bpmn2Container=readResourceContent(bpmn2resource);
        
        logger.log("bpmn2: "+bpmn2Container);
        
        //directly use simulation runner as in Workbench
        String[] rules = new String[1];
        rules[0]="onevent.simulation.rules.drl";
        
        //org.eclipse.bpmn2.Definitions definitions = BPMN2Utils.getDefinitions(bpmn2stream);
		String processId="com.philips.CathpathEventSim";
		int numberOfAllInstances=10;
		long interval=1000*60*60;
		
		SimulationRepository repo = SimulationRunner.runSimulation(processId, bpmn2Container, numberOfAllInstances, interval, rules);
		
		if (repo instanceof WorkingMemorySimulationRepository) {
			WorkingMemorySimulationRepository wmrepo =  (WorkingMemorySimulationRepository)repo;
			wmrepo.close();
        }
	}
	
	public static void testSimulation() {
		InputStream bpmn2stream = PhilipsSimulationClient.class.getResourceAsStream("/BasicWF.v1.0.bpmn2");
        PathFinder finder = PathFinderFactory.getInstance(bpmn2stream);
        System.out.println(finder);
        List<SimulationPath> paths = finder.findPaths(new SimulationFilterPathFormatConverter());
        SimulationContext context = SimulationContextFactory.newContext(new HardCodedSimulationDataProvider2() //todo: replace with BPMN2SimulationDataProvider (see test)
        , new WorkingMemorySimulationRepository(true, "printOutRule.drl"));
        
        
        for (SimulationPath path : paths) {
            
            context.setCurrentPath(path);
            KieSession session = createSession("BasicWF.v1.0.bpmn2");
            
            context.setClock((SessionPseudoClock) session.getSessionClock());
            // set start date to current time
            context.getClock().advanceTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            
            session.startProcess("src.main.resources.BasicWF");
            System.out.println("#####################################");
        }
	}
	
    public static KieSession createSession(String process) {
        KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        
        builder.add(ResourceFactory.newClassPathResource(process), ResourceType.BPMN2);
        
        KieBase kbase = builder.newKieBase();
        KieSessionConfiguration config = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        config.setOption(ClockTypeOption.get("pseudo") );
        KieSession session = kbase.newKieSession(config, EnvironmentFactory.newEnvironment());
        session.getEnvironment().set("NodeInstanceFactoryRegistry", SimulationNodeInstanceFactoryRegistry.getInstance());
        
        return session;
    }
    protected static String readResourceContent(Resource resource) {
        StringBuilder contents = new StringBuilder();
        BufferedReader reader = null;
 
        try {
            reader = new BufferedReader(resource.getReader());
            String text = null;
 
            // repeat until all lines is read
            while ((text = reader.readLine()) != null) {
                contents.append(text);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return contents.toString();
    }
}
