package agent.MaxSum;

import communication.DCOPagent;
import communication.FactorNode;
import communication.VariableNode;
import kernel.Commons;
import kernel.Domain;
import kernel.Variable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by nando on 5/24/17.
 */
public class MaxSumVariableNode {

    // node not needed - only need domain size
    public VariableNode node;

    // Cost received by each function f(x,y) by projecting out y if this variable is x [prev cycle]
    // key: functionNode ID; value: vector of size Dom of this variable
    private HashMap<Long, double[]> costTable;

    // Cost received by each function f(x,y) at currenct cycle
    // key: functionNode ID; value: vector of size Dom of this variable
    private HashMap<Long, double[]> recvCostTables;

    // A vector of noisy values to allow faster convergence
    public double[] noise;

    public MaxSumVariableNode(VariableNode node) {
        this.node = node;
        costTable = new HashMap<>();
        recvCostTables = new HashMap<>();

        for (FactorNode f : node.getNeighbors()) {
            double[] costs = new double[node.getVariable().getDomain().size()];
            Arrays.fill(costs, 0);
            costTable.put(f.getID(), costs);
            recvCostTables.put(f.getID(), costs.clone());
        }

        noise = new  double[node.getVariable().getDomain().size()];
        for (int i = 0; i <noise.length; i++)
            noise[i] = Math.random();
    }

    public long getID() {
        return node.getID();
    }

    public DCOPagent getOwner() {
        return node.getOwner();
    }

    public List<FactorNode> getNeighbors() {
        return node.getNeighbors();
    }

    public Variable getVariable() {
        return node.getVariable();
    }

    public int selectBestValue() {
        double[] table = getCostTableSum();
        Domain dom = node.getVariable().getDomain();
        int val_idx = Commons.getArgMin(table);
        return dom.getElement(val_idx);
    }

    public double[] getNoise() {
        return noise;
    }

    @Deprecated
    public void sendMessages(int currCycle) {
        for (FactorNode fnode : node.getNeighbors()) {
            double[] table = getCostTableSumExcluding(fnode.getID());
            // addUnaryConstraints(table);  // todo later
            //Commons.rmValue(table, Commons.getMin(table));
            Commons.rmValue(table, Commons.getAverage(table));
            //Commons.addArray(table, getNoise());

            MaxSumAgent.VnodeToFnodeMessage msg =
                        new MaxSumAgent.VnodeToFnodeMessage(table, getID(), fnode.getID(), currCycle);
            fnode.getOwner().tell(msg, node.getOwner().getSelf());
        }
    }

    public double[] getTable(FactorNode fNode) {
        double[] table = getCostTableSumExcluding(fNode.getID());
        // addUnaryConstraints(table);  // todo later
        //Commons.rmValue(table, Commons.getMin(table));
        Commons.rmValue(table, Commons.getAverage(table));
        //Commons.addArray(table, getNoise());
        return table;
    }

    public void copyCostTable(double[] table, long fNodeId) {
        recvCostTables.put(fNodeId, table);
    }

    public void saveReceivedCostTables() {
        for (Map.Entry<Long, double[]> entry : recvCostTables.entrySet()) {
            Long key = entry.getKey();
            double[] value = entry.getValue();
            // todo: you could avoid to clone table here
            costTable.put(key, value.clone());
        }
    }

    /**
     *
     * @param excludedId The ID of the function node which is excluded from summing the values of the cost table
     * @return The aggregated cost table
     * @// TODO: 5/25/17 If you need to search for more than one ID, then pass a HashSet.
     */
    private double[] getCostTableSumExcluding(long excludedId) {
        double[] sum = new double[node.getVariable().getDomain().size()];
        Arrays.fill(sum, 0);
        for (FactorNode f : node.getNeighbors()) {
            if (f.getID() == excludedId)
                continue;
            Commons.addArray(sum, costTable.get(f.getID()));
        }
        return sum;
    }

    private double[] getCostTableSum() {
        return getCostTableSumExcluding(-1);
    }

}
