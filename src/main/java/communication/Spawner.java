/*
 * Copyright (c) 2015.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package communication;

import agent.FactorGraphAgent;
import kernel.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ffiorett on 7/10/15.
 * Spawns the problem agents and creates dependencies, according to their constraint graph.
 */
public class Spawner {

    private List<AgentState> spawnedAgentStates;
    private HashMap<String, DCOPagent> yellowPages;
    DCOPInstance dcopInstance = null;

    public Spawner(DCOPInstance instance) {
        dcopInstance = instance;
        spawnedAgentStates = new ArrayList<>();
        yellowPages = new HashMap<>();

        for (AgentState agt : instance.getDCOPAgents()) {
            spawnedAgentStates.add(agt);
        }

        DCOPinfo.nbAgents = spawnedAgentStates.size();
        DCOPinfo.nbConstraints = instance.getDCOPConstraints().size();
    }

    /**
     * Creates the AKKA Agent System, as well as a watcher. It spawns all DCOP agents, and it
     * creates their dependencies.
     */
    public void spawn(List<Object> algParameters) {

        // Spawn the Statistics collector Deamon
        final ComAgent statsCollector = new StatisticsDeamon(spawnedAgentStates.size());
        statsCollector.start();
        DCOPinfo.cycleTickerDeamon = new CycleTickerDeamon(spawnedAgentStates);


        // Spawns agents and start the DCOP algorithm
        for (AgentState agtState : spawnedAgentStates) {

//            for (AgentState neighbor : agtState.getNeighbors()) {
//                DCOPagent neighborAgt = yellowPages.get(neighbor.getName());
//                neighbor.setComAgent(neighborAgt);
//            }

            DCOPagent agt = AgentFactory.create(statsCollector, agtState, algParameters);
            DCOPinfo.agentsRef.put(agt.getId(), agt);
            yellowPages.put(agtState.getName(), agt);

            agt.start();
        }

        // Save leader AgentRef
        String leaderName = spawnedAgentStates.get(0).getName();
        DCOPinfo.leaderAgent = DCOPinfo.agentsRef.get((long)0);
        assert (DCOPinfo.leaderAgent.getId() == 0);

        // Links Agent Neighbors as ComAgent objects.
        for (AgentState agtState : this.spawnedAgentStates) {
            ComAgent actor = yellowPages.get(agtState.getName());
            agtState.setComAgent(actor);
            for (AgentState neighbor : agtState.getNeighbors()) {
                DCOPagent neighborAgt = yellowPages.get(neighbor.getName());
                actor.tell(new Message.RegisterNeighbor(neighborAgt, neighborAgt.getId()), ComAgent.noSender());
            }
            // Link Leader to each agent (used if needed by algorithm)
            DCOPagent agt = yellowPages.get(leaderName);
            actor.tell(new Message.RegisterLeader(yellowPages.get(leaderName)), ComAgent.noSender());
        }

        // Wait some time for discovery phase
        try {Thread.sleep(DCOPinfo.nbAgents * 50);} catch (InterruptedException e) {e.printStackTrace();}

        constructOrdering();

        // Signals start to all agents
        for (AgentState agtState : this.spawnedAgentStates) {
            DCOPagent actor = yellowPages.get(agtState.getName());
            actor.tell(new Message.StartSignal(), ComAgent.noSender());
        }

        // System awaits termination
        try {
            for (DCOPagent agt : yellowPages.values()) {
                agt.join();
            }
            statsCollector.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Collection<DCOPagent> getSpawnedAgents() {
        return yellowPages.values();
    }

    /**
     * This function construct an ordering of the DCOP agents.
     * At the end of this function all agents will know their position in their ordering and their neighbors.
     */
    private void constructOrdering() {
        if (DCOPinfo.leaderAgent instanceof FactorGraphAgent)
        {
            FactorGraph a = new FactorGraph(dcopInstance);
        }
    }

}
