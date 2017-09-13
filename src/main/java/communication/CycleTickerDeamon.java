package communication;

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;
import kernel.AgentState;
import kernel.AgentView;
import kernel.Constants;
import kernel.DCOPinfo;
import sun.management.Agent;

import java.util.*;

/**
 * Created by nando on 6/2/17.
 */
public class CycleTickerDeamon /*extends ComAgent*/ {

    int currentCycle;
    int nbAgents;
    BitSet agentsTerminatedCurrentCycle;

    public CycleTickerDeamon(List<AgentState> spawnedAgentStates) {
        currentCycle = 0;
        nbAgents = spawnedAgentStates.size();
        agentsTerminatedCurrentCycle = new BitSet(spawnedAgentStates.size());
    }

    public synchronized void terminateAgentCycle(ComAgent agent) {
        //System.out.println(agent.getName() + " Terminating current cycle");
        AgentView agentView = DCOPinfo.agentsRef.get(agent.getId()).getAgentView();
        agent.getAgentStatistics().updateIterationStats(agentView);
        // System.out.println(agent.getAgentStatistics().getSolutionValue());
        agent.setAgtState(ComAgent.STOPPED);
        agentsTerminatedCurrentCycle.set((int)agent.getId());
        if (agentsTerminatedCurrentCycle.cardinality() == nbAgents)
        {
            // compute cost
            System.out.println("Cycle: " + currentCycle);
            //DCOPinfo.leaderAgent.getAgentStatistics().setSolutionCostIter(getProblemCost());
            currentCycle ++;
            agentsTerminatedCurrentCycle.clear();

            // start new cycle by sending each message
            for (DCOPagent agt : DCOPinfo.agentsRef.values()) {
                agt.setAgtState(ComAgent.RUNNING);
                agt.tell(new Message.StartNewCycle(), ComAgent.noSender());
            }
        }
    }


    public double getProblemCost() {
        double cost = 0;
        for (DCOPagent agt : DCOPinfo.agentsRef.values()) {
            double val = agt.getAgentView().getEvaluator().evaluate();
            if (Constants.isInf(val)) {
                return Constants.infinity;
            }
            cost += val;
        }
        return cost;
    }

    public int getCurrentCycle() {
        return currentCycle;
    }
}
