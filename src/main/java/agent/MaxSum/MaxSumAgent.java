package agent.MaxSum;

import agent.FactorGraphAgent;
import communication.BasicMessage;
import communication.ComAgent;
import communication.FactorNode;
import communication.VariableNode;
import kernel.AgentState;
import kernel.Commons;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by nandofioretto on 5/15/17.
 */
public class MaxSumAgent extends FactorGraphAgent {

    private int nbCycles = Integer.MAX_VALUE;
    private double convergenceDelta = 0.001;

    // key: variableNode ID, value = maxSumNode
    private Map<Long, MaxSumVariableNode> variableNodes;
    private Map<Long, MaxSumFactorNode> factorNodes;

    // Message counts (MAKE THIS A CLASS Msg Manager)
    private int nbRecvFmsgs = 0;
    int totalNbVneibgbors = 0;
    private int nbRecvVmsgs = 0;
    int totalNbFneibgbors = 0;

    // key = varID; value = variableNodes index associated to that variable ID
    private Map<Long, Integer> mapVarPos;

    public MaxSumAgent(ComAgent statsCollector, AgentState agentState, List<Object> parameters) {
        super(statsCollector, agentState);
        // Check argument:
        assert (parameters.size() == 1);
        this.nbCycles = (int) parameters.get(0);

        variableNodes = new TreeMap<>();
        factorNodes = new TreeMap<>();
        mapVarPos = new HashMap<>();
    }

    @Override
    protected boolean terminationCondition() {
        return getCurrentCycle() >= nbCycles;
    }

    @Override
    protected void onStart() {
        // Assign variable value to 0
        getAgentActions().setVariableValue(0);

        // Initialize MaxSumVariableNodes
        for (VariableNode vnode : getVariableNodes()) {
            variableNodes.put(vnode.getID(), new MaxSumVariableNode(vnode));
            int vIdx = findVariableID(vnode.getVariable().getID());
            mapVarPos.put(vnode.getID(), vIdx);
            //totalNbFneibgbors += vnode.getNeighbors().size();
            totalNbFneibgbors += vnode.getNbNotOwnedNeighbors();
        }
        System.out.println(getName() + " num VariableNode's neighbors: " + totalNbFneibgbors);

        // Initialize MaxSumFactorNodes
        for (FactorNode fnode : getFactorNodes()) {
            factorNodes.put(fnode.getID(), new MaxSumFactorNode(fnode));
            //totalNbVneibgbors += fnode.getNeighbors().size(); //fnode.getNbNotOwnedNeighbors();
            totalNbVneibgbors += fnode.getNbNotOwnedNeighbors();
        }
        System.out.println(getName() + " num FactorNode's neighbors: " + totalNbVneibgbors);

        // start cycling
        super.onStart();
    }


    @Override
    protected void onReceive(Object message, ComAgent sender) {
        super.onReceive(message, sender);

        if (message instanceof VnodeToFnodeMessage) {
            VnodeToFnodeMessage msg = (VnodeToFnodeMessage)message;
            long fnodeId = msg.getFactorNodeID();
            long vNodeId = msg.getVariableNodeID();
            factorNodes.get(fnodeId).copyCostTable(msg.getTable(), vNodeId);
            nbRecvVmsgs ++;

            System.out.println(getName() + " received " + message.toString()
                    + "  # V->F msg recv: " + nbRecvVmsgs + " / " + totalNbVneibgbors
                    + "  # F->V msg recv: " + nbRecvFmsgs + " / " + totalNbFneibgbors);

            if (nbRecvVmsgs == totalNbVneibgbors && nbRecvFmsgs == totalNbFneibgbors) {
                terminateCycle();
            }
        }
        else if (message instanceof FnodeToVnodeMessage) {
            FnodeToVnodeMessage msg = (FnodeToVnodeMessage)message;
            long fNodeId = msg.getFactorNodeID();
            long vNodeId = msg.getVariableNodeID();
            variableNodes.get(vNodeId).copyCostTable(msg.getTable(), fNodeId);
            nbRecvFmsgs ++;

            System.out.println(getName() + " received " + message.toString()
                    + "  # V->F msg recv: " + nbRecvVmsgs + " / " + totalNbVneibgbors
                    + "  # F->V msg recv: " + nbRecvFmsgs + " / " + totalNbFneibgbors);

            if (nbRecvVmsgs == totalNbVneibgbors && nbRecvFmsgs == totalNbFneibgbors) {
                terminateCycle();
            }
        }
    }

    @Override
    protected void cycle() {

        // Send messages: VarNode -> FuncNode
        for (MaxSumVariableNode vnode : variableNodes.values()) {
            for (FactorNode fnode : vnode.getNeighbors()) {
                double[] table = vnode.getTable(fnode);
                if (fnode.getOwner().equals(this)) {
                    factorNodes.get(fnode.getID()).copyCostTable(table, vnode.getID());
                } else {
                    MaxSumAgent.VnodeToFnodeMessage msg =
                            new MaxSumAgent.VnodeToFnodeMessage(table, vnode.getID(), fnode.getID(), getCurrentCycle());
                    fnode.getOwner().tell(msg, getSelf());
                }
            }
        }

        // Send messages: FuncNode -> VarNode
        for (MaxSumFactorNode fnode : factorNodes.values()) {
            for (VariableNode vnode : fnode.getNeighbors()) {
                double[] table = fnode.getTable(vnode);
                if (vnode.getOwner().equals(this)) {
                    variableNodes.get(vnode.getID()).copyCostTable(table, fnode.getID());
                } else {
                    MaxSumAgent.FnodeToVnodeMessage msg =
                            new MaxSumAgent.FnodeToVnodeMessage(table, fnode.getID(), vnode.getID(), getCurrentCycle());
                    vnode.getOwner().tell(msg, getSelf());
                }
            }
        }
    }

    @Override
    protected void onCycleStart() {
        // Select best value from all the variables controlled by this agent by calling the routines in variable nodes
        for (MaxSumVariableNode vnode : variableNodes.values()) {
            int val = vnode.selectBestValue();
            getAgentActions().setVariableValue(mapVarPos.get(vnode.getID()), val);
        }

        System.out.println("Agent " + getName() + " Starting cycle: " + getCurrentCycle() +
                " select value: " + getAgentView().getVariableValue() );
    }

    @Override
    protected void onCycleEnd() {
        nbRecvFmsgs = 0;
        nbRecvVmsgs = 0;

        // Save all received messages to be used in the next iteration
        variableNodes.values().forEach(MaxSumVariableNode::saveReceivedCostTables);
        factorNodes.values().forEach(MaxSumFactorNode::saveReceivedCostTables);

        System.out.println("Agent " + getName() + " Terminating cycle  " + getCurrentCycle());
    }

    /// Auxiliary Functions
    private int findVariableID(long id) {
        for (int i = 0; i < getAgentView().getNbVariables(); i++) {
            long vId = getAgentView().getVariableId(i);
            if (vId == id)
                return i;
        }
        assert(true);
        return -1;
    }

    /// Messages ------------------------------- //
    public static class TableMessage extends BasicMessage {
        protected double[] table;
        protected long fNodeId; // sender factor node
        protected long vNodeId; // receiver factor node
        protected int cycleNo;

        public TableMessage(double[] table, long vNodeId, long fNodeId, int currCycle) {
            this.table = table;//table.clone();
            this.vNodeId = vNodeId;
            this.fNodeId = fNodeId;
            this.cycleNo = currCycle;
        }

        public long getFactorNodeID() {
            return fNodeId;
        }

        public long getVariableNodeID() {
            return vNodeId;
        }

        public int getCycleNo() {
            return cycleNo;
        }

        public double[] getTable() {
            return table;
        }

    }

    public static class VnodeToFnodeMessage extends TableMessage {
        /**
         * A Variable to Factor node message
         * @param table The cost table
         * @param vNodeId sender (variable) node ID
         * @param fNodeId receiver (factor) node ID
         * @param currCycle the sender cycle number
         */
        public VnodeToFnodeMessage(double[] table, long vNodeId, long fNodeId, int currCycle) {
            super(table, vNodeId, fNodeId, currCycle);
        }

        @Override
        public String toString() {
            String s = "["+ cycleNo +"]VnodeToFnodeMessage: vId " + vNodeId + " -> fId " + fNodeId + "[";
            for (double d : table) s += d + " ";
            return s + "]";
        }
    }

    public static class FnodeToVnodeMessage extends TableMessage {
        /**
         * A Variable to Factor node message
         * @param table The cost table
         * @param fNodeId sender (factor) node ID
         * @param vNodeId receiver (variable) node ID
         * @param currCycle the sender cycle number
         */
        public FnodeToVnodeMessage(double[] table, long fNodeId, long vNodeId, int currCycle) {
            super(table, vNodeId, fNodeId, currCycle);
        }

        @Override
        public String toString() {
            String s = "["+ cycleNo +"]FnodeToVnodeMessage: fId " + fNodeId + " -> vId " + vNodeId + "[";
            for (double d : table) s += d + " ";
            return s + "]";
        }
    }
}
//
