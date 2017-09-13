package communication;

import agent.FactorGraphAgent;
import kernel.Constraint;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nando on 5/17/17.
 */
public class FactorNode {
    private List<VariableNode> neighbors;
    private Constraint constraint;
    private DCOPagent owner;

    public FactorNode(FactorGraphAgent owner, Constraint constraint) {
        this.owner = owner;
        this.constraint = constraint;
        neighbors = new ArrayList<>();
        owner.addFunctionNode(this);
    }

    public void addNeighbor(VariableNode varNode) {
        if (!neighbors.contains(varNode))
            neighbors.add(varNode);
    }

    public List<VariableNode> getNeighbors() {
        return neighbors;
    }

    public Constraint getConstraint() {
        return constraint;
    }

    public DCOPagent getOwner() {
        return owner;
    }

    public long getID() {
        return constraint.getID();
    }

    public int getNbNotOwnedNeighbors() {
        int tot = 0;
        for (VariableNode n : neighbors) {
            if (!n.getOwner().equals(owner))
                tot ++;
        }
        return tot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FactorNode that = (FactorNode) o;

        return constraint != null ? constraint.equals(that.constraint) : that.constraint == null;
    }

    @Override
    public int hashCode() {
        return constraint != null ? constraint.hashCode() : 0;
    }

    @Override
    public String toString() {
        String s = "FactorNode: " +
                " owner= " + owner.getName() +
                " variable= " + constraint.getName() +
                " neighbors_var= ";
        for (VariableNode v : neighbors)
            s += v.getVariable().getName() + " ";
        return s;
    }
}
