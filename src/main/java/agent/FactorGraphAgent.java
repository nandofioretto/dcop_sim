package agent;

import communication.ComAgent;
import communication.FactorNode;
import communication.VariableNode;
import kernel.AgentState;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nando on 5/19/17.eedq
 */
public abstract class FactorGraphAgent extends SynchronousAgent {

    // The list of function nodes and variable nodes owned by this agent
    private List<FactorNode> factorNodes;
    private List<VariableNode> variableNodes;

    public FactorGraphAgent(ComAgent statsCollector, AgentState agentState) {
        super(statsCollector, agentState);
        variableNodes = new ArrayList<>();
        factorNodes = new ArrayList<>();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void addFunctionNode(FactorNode node) {
        if (!factorNodes.contains(node)) {
            factorNodes.add(node);
            // System.out.println("Agent " + this.getName() + " registers function node " + node.toString());

        }
    }

    public void addVariableNode(VariableNode node) {
        if (!variableNodes.contains(node)) {
            variableNodes.add(node);
            // System.out.println("Agent " + this.getName() + " registers variable node " + node.toString());
        }
    }


    public List<FactorNode> getFactorNodes() {
        return factorNodes;
    }

    public List<VariableNode> getVariableNodes() {
        return variableNodes;
    }
}
