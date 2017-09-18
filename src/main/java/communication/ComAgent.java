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

import java.util.*;

/**
 * Created by ffiorett on 7/17/15.
 * This is an abstract class, which describes a basic "communicating agent", that is, an
 * agent which is able to communicate over the network with other agents.
 * Each DCOP agent should extend from this class or one of its subclasses,
 * as it contains the messageStatistics.
 */
public abstract class ComAgent {

    private long agentID; // ID of the agent starting from 0
    private List<ComAgent> neighborsRef;
    private HashMap<Long, ComAgent> neigbhorRefByID;
    private ComAgent leaderRef = null;
    private AgentStatistics agentStatistics;
    private Queue<TrackableObject> mailbox;
    protected static int HEATUP = -1;
    protected static int RUNNING = 0;
    protected static int STOPPED = 1;
    protected static int TERMINATED = 2;
    private int state;

    String name;

    public ComAgent(String name, long agentID) {
        this.name = name;
        this.agentID = agentID;
        this.neighborsRef = new ArrayList<ComAgent>();
        this.neigbhorRefByID = new HashMap<Long, ComAgent>();
        this.agentStatistics = new AgentStatistics();
        this.mailbox = new LinkedList<>();
        agentStatistics.getStopWatch().start();
        this.state = HEATUP;
    }

    public void run() {
        if (state == HEATUP) {
            // preStart only if first time
            preStart();
            state = RUNNING;
        }

        if (!terminationCondition()) {
            if (state == RUNNING) {
                processMail();
            }
        } else {
            // enter here only when terminated
            preStop();
        }
    }

    public String getName() {
        return name;
    }

    protected abstract boolean terminationCondition();

    protected abstract void preStart();

    protected abstract void preStop();

    protected void onReceive(Object message, ComAgent sender) {
        if (message instanceof BasicMessage) {
            // Save statistics info
            agentStatistics.getStopWatch().suspend();
            long recvSimTime = ((BasicMessage) message).getSimulatedNanoTime();
            agentStatistics.getStopWatch().updateTimeIfFaster(recvSimTime);
            agentStatistics.getStopWatch().resume();            // Resumes Simulated Time (if suspended)
        } else if (message instanceof Message.RegisterNeighbor) {
            Message.RegisterNeighbor actorNeighbor = (Message.RegisterNeighbor) message;
            if (actorNeighbor.getAgentRef() != getSelf()) {
                neighborsRef.add(actorNeighbor.getAgentRef());
                neigbhorRefByID.put(actorNeighbor.getAgentID(), actorNeighbor.getAgentRef());
            }
        } else if (message instanceof  Message.RegisterLeader) {
            Message.RegisterLeader leaderMsg = (Message.RegisterLeader) message;
            if (leaderMsg.getAgentRef() != getSelf()) {
                leaderRef = leaderMsg.getAgentRef();
            }
        }
    }

    /**
     * Sends a message to this agent.
     * @param message The message to be sent
     * @param sender  The sender of the message
     */
    public void tell(Object message, ComAgent sender) {
        try {
            String sName = sender == null ? "none" : sender.getName();
            //System.out.println(sName + " sending " + message.toString() + " to " + getName());
            mailbox.add(new TrackableObject(message, sender));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Mirror of tell function for BasicMessage objects.
     * It is used to update the statistics.
     * @param message The message to be sent
     * @param sender  The sender of the message
     */
    public void tell(BasicMessage message, ComAgent sender) {
        sender.getAgentStatistics().getStopWatch().suspend();
        if (message.isTrackable())
            message.setSimulatedNanoTime(sender.getAgentStatistics().getStopWatch().getNanoTime());
        try {
            mailbox.add(new TrackableObject(message, sender));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (message.isTrackable())
            sender.getAgentStatistics().incrSentMessages();
        sender.getAgentStatistics().getStopWatch().resume();
    }


    /**
     * Awaits while checking if the message queue is non empty. In which case, it process the message.
     * todo: Ensure agent does not send message to itself.
     * todo: In case this is not possible, we need to count the number of messages when we enter in this function,
     * todo: and process only such count of messages.
     */
    public void processMail() {
        agentStatistics.getStopWatch().resume();
        while (!mailbox.isEmpty()) {
            try {
                TrackableObject to = mailbox.remove();
                onReceive(to.getObject(), to.getTrack());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        agentStatistics.getStopWatch().suspend();
    }

    public ComAgent getSelf() {
        return this;
    }

    /**
     * Overrides the Thread ID method to return the agent ID
     */
    public long getId() {
        return agentID;
    }

    public int getAgtState() {
        return this.state;
    }

    public void setAgtState(int state) {
        if (this.state == RUNNING && state == STOPPED) {
            agentStatistics.getStopWatch().suspend();
        } else if (this.state == STOPPED && state == RUNNING) {
            agentStatistics.getStopWatch().resume();
        }
        this.state = state;
    }

    public static ComAgent noSender() {
        return null;
    }

    /**
     * @return The agent's neighbors references.
     */
    public List<ComAgent> getNeighborsRef() {
        return neighborsRef;
    }

    public int getNbNeighbors() {
        return neighborsRef.size();
    }

    public ComAgent getNeigbhorRefByID(long agentID) {
        return neigbhorRefByID.get(agentID);
    }

    public ComAgent getLeaderRef() {
        return leaderRef;
    }

    public boolean isLeader() {
        return leaderRef == null;
    }

    /**
     * @return The agent statistcs.
     */
    public AgentStatistics getAgentStatistics() {
        return agentStatistics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComAgent)) return false;

        ComAgent comAgent = (ComAgent) o;

        if (agentID != comAgent.agentID) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (agentID ^ (agentID >>> 32));
    }

    @Override
    public String toString() {
        return getName();
    }
}
