package agent.MaxSum;

import communication.DCOPagent;
import communication.FactorNode;
import communication.VariableNode;
import kernel.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by nando on 5/24/17.
 */
public class MaxSumFactorNode {

    private FactorNode node;

    // !!!!!!!!!!! We assume now that all constraints are binary !!!!!!!!!!!!!!

    // Cost received by each variable participating to this factor - if this factor is f(x, y),
    // then it will receive two cost tables, one from x and  one from y
    // key: variable ID; value: vector of size Dom of variable
    // [prev cycle] - used to sum up and implement agent logic
    private Map<Long, double[]> costTable;

    // [current cycle]
    // used to store new coming messages
    private Map<Long, double[]> recvCostTables;

    public MaxSumFactorNode (FactorNode node) {
        this.node = node;
        costTable = new HashMap<>();
        recvCostTables = new HashMap<>();

        for (VariableNode v : node.getNeighbors()) {
            double[] costs = new double[v.getVariable().getDomain().size()];
            Arrays.fill(costs, 0);
            costTable.put(v.getID(), costs);
            recvCostTables.put(v.getID(), costs.clone());
        }
    }

    public long getID() {
        return node.getID();
    }

    public DCOPagent getOwner() {
        return node.getOwner();
    }

    public List<VariableNode> getNeighbors() {
        return node.getNeighbors();
    }

    @Deprecated
    public void sendMessages(int currCycle) {
        for (VariableNode vnode : node.getNeighbors()) {
            // Note: this need to be extedned if we handle multiple constraints [todo]
            double[][] cTable = getConstraintTable(vnode.getVariable());
            // Add values received from other agents (variable nodes)
            sumCostTablesExcluding(cTable, vnode.getID());
            // Project on the (first variable) dest. of message
            double[] table = project(cTable);
            //Commons.rmValue(table, Commons.getMin(table));

            // Send messages to Funcation Nodes
            MaxSumAgent.FnodeToVnodeMessage msg =
                    new MaxSumAgent.FnodeToVnodeMessage(table, getID(), vnode.getID(), currCycle);
            vnode.getOwner().tell(msg, node.getOwner().getSelf());
        }
    }

    public double[] getTable(VariableNode vNode) {
        // todo: To handle nary constraints, we need to modify this function
        double[][] cTable = getConstraintTable(vNode.getVariable());
        // Add values received from other agents (variable nodes)
        sumCostTablesExcluding(cTable, vNode.getID());
        // Project on the (first variable) dest. of message
        double[] table = project(cTable);
        //Commons.rmValue(table, Commons.getMin(table));
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
    
    // GET TABLE CONSTRAINT (BINARY).
    // I am going to order the constraint Table so that the variable vIdx ( to send  )
    // is going to be positioned as FIRST element of the table scope
    private double[][] getConstraintTable(Variable variable) {
        TableBinaryConstraint c = (TableBinaryConstraint)node.getConstraint();
        int v_idx = Commons.getIdx(c.getScope(), variable);
        assert (v_idx >= 0);
        int map_dom_1 = v_idx;      // I wont this to be = to v_dix
        int map_dom_2 = v_idx == 1 ? 0 : 1; // I wont this to be != from v_dix
        Domain dom_1 = c.getScope(map_dom_1).getDomain();
        Domain dom_2 = c.getScope(map_dom_2).getDomain();
        Tuple tuple = new Tuple(2);

        double[][] cTable = new double[dom_1.size()][dom_2.size()];
        for (int i = 0; i < dom_1.size(); i++) {
            tuple.set(map_dom_1, dom_1.getElement(i));
            for (int j = 0; j < dom_2.size(); j++) {
                tuple.set(map_dom_2, dom_2.getElement(j));
                cTable[i][j] = c.getValue(tuple);
            }
        }
        return cTable;
    }

    // Add values received from other agents (variable nodes)
    // Note: this need to be extedned if we handle multiple constraints [todo]
    private void sumCostTablesExcluding(double[][] cTable, long vnodeID)
    {
        for (VariableNode v : node.getNeighbors()) {
            if (v.getID() == vnodeID)
                continue;

            Domain vDom = v.getVariable().getDomain();
            double[] recvTable = costTable.get(v.getID());

            for (int i = 0; i < vDom.size(); i++) {
                Commons.addValue(cTable[i], recvTable[i]);
            }
        }
    }

    /**
     * Project the table on its first dimension (column) by minimizing all other dimensions
     * @return
     */
    private double[] project(double[][] cTable) {
        int size = cTable.length;
        double[] table = new double[size];
        for (int i = 0; i < size; i++) {
            table[i] = Commons.getMin(cTable[i]);
        }
        return table;
    }

}
