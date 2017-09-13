package kernel;

import agent.FactorGraphAgent;
import communication.FactorNode;
import communication.VariableNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by nando on 5/17/17.
 * Build a factor graph in a centralized fashion
 */
public class FactorGraph {

    private List<VariableNode> variableNodes;
    private List<FactorNode> factorNodes;

    private HashMap<Variable, VariableNode> varToVariableNodeMap;
    private HashMap<Constraint, FactorNode> conToFunctionNodeMap;

    /**
     * We assumem that the agents calling this function are of class FactorGraphAgent
     * @param DCOP
     */
    public FactorGraph(DCOPInstance DCOP) {
        varToVariableNodeMap = new HashMap<>();
        conToFunctionNodeMap = new HashMap<>();

        variableNodes = new ArrayList<>();
        factorNodes = new ArrayList<>();

        // Create variable nodes
        for (Variable v : DCOP.getDCOPVariables()) {
            long aId = v.getOwnerAgent().getID();
            FactorGraphAgent agent = (FactorGraphAgent)DCOPinfo.agentsRef.get(aId);
            VariableNode vnode = new VariableNode(agent, v);
            variableNodes.add(vnode);
            varToVariableNodeMap.put(v, vnode);
        }

        // Create function nodes
        for (Constraint c : DCOP.getDCOPConstraints()) {
            long aId = getOnwerId(c);
            FactorGraphAgent agent = (FactorGraphAgent)DCOPinfo.agentsRef.get(aId);
            FactorNode fnode = new FactorNode(agent, c);
            factorNodes.add(fnode);
            conToFunctionNodeMap.put(c, fnode);

            // Add function nodes neighbors (i.e., all variable nodes connected to it)
            // and add variable nodes neighbors (i.e., all function nodes whose scope contains this variable)
            for (Variable v : c.getScope()) {
                VariableNode vnode = varToVariableNodeMap.get(v);
                fnode.addNeighbor(vnode);
                vnode.addNeighbor(fnode);
            }
        }
    }

    public long getOnwerId(Constraint c) {
        long id = Integer.MAX_VALUE;
        for (Variable v : c.getScope())
            if (v.getOwnerAgent().getID() < id)
                id = v.getOwnerAgent().getID();
        return id;
    }

    @Override
    public String toString() {
        String s = "FactorGraph:\n\t";
        for (VariableNode v : variableNodes) {
            s += v +"\n\t";
        }
        for (FactorNode f : factorNodes) {
            s += f +"\n\t";
        }
        return s;
    }
}
