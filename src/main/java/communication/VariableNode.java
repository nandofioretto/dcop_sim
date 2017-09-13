package communication;

import agent.FactorGraphAgent;
import kernel.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nando on 5/17/17.
 */
public class VariableNode {
    private List<FactorNode> neighbors;

    // Factor Nodes whose constraint is shared with the variable of this variable node, and whose other variables are
    // of higher ID of this variable
    private List<FactorNode> higherPriorityNeighbors;

    private DCOPagent owner;
    private Variable variable;

    public VariableNode(FactorGraphAgent owner, Variable variable) {
        this.owner = owner;
        this.variable = variable;
        neighbors = new ArrayList<>();
        higherPriorityNeighbors = new ArrayList<>();
        owner.addVariableNode(this);
    }

    public void addNeighbor(FactorNode fNode) {
        if (!neighbors.contains(fNode))
            neighbors.add(fNode);

        if (!higherPriorityNeighbors.contains(fNode) &&
                fNode.getOwner().getId() > owner.getId())
            higherPriorityNeighbors.add(fNode);
    }

    public List<FactorNode> getNeighbors() {
        return neighbors;
    }

    public List<FactorNode> getHigherPriorityNeighbors() {
        return higherPriorityNeighbors;
    }

    public DCOPagent getOwner() {
        return owner;
    }

    public Variable getVariable() {
        return variable;
    }

    public long getID() {
        return variable.getID();
    }

    public int getNbNotOwnedNeighbors() {
        int tot = 0;
        for (FactorNode n : neighbors) {
            if (!n.getOwner().equals(owner))
                tot ++;
        }
        return tot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VariableNode that = (VariableNode) o;

        return variable != null ? variable.getID() == that.getVariable().getID() : that.variable == null;

    }

    @Override
    public int hashCode() {
        return variable != null ? variable.hashCode() : 0;
    }

    @Override
    public String toString() {
        String s = "VariableNode: " +
                " owner= " + owner.getName() +
                " variable= " + variable.getName() +
                " neighbors_con= ";
        for (FactorNode f : neighbors)
            s += f.getConstraint().getName() + " ";
        return s;
    }
}
