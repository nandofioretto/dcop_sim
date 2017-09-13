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

package kernel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ffiorett on 7/17/15.
 * This class is used to view the agent's content: Variables values, constraints, and domains.
 */
public class AgentView {
    protected final AgentState agentState;
    protected Evaluator evaluator;

    public AgentView(AgentState agentState) {
        this.agentState = agentState;
        this.evaluator = new Evaluator();
    }

    public String getAgentName() {
        return agentState.getName();
    }

    public long getAgentID() {return agentState.getID();}
    /**
     * Gets the value currently held by the variable queried.
     * @param pos The position of the variable in the array agentState.variables.
     */
    public int getVariableValue(int pos) {
        return agentState.getVariable(pos).getValue();
    }

    public int getVariableValue() {
        return getVariableValue(0);
    }

    public int getVariableType(int pos) { return agentState.getVariable(pos).getType(); }

    public int getVariableType() { return agentState.getVariable().getType(); }

    public String getVariableName(int pos) {
        return agentState.getVariable(pos).getName();
    }

    public String getVariableName() {
        return agentState.getVariable().getName();
    }

    public long getVariableId(int pos) {
        return agentState.getVariable(pos).getID();
    }

    public long getVariableId() {
        return agentState.getVariable().getID();
    }

    public int getNbVariables() {
        return agentState.getVariables().size();
    }

    public int getDomainMin(int pos) {
        return agentState.getVariable(pos).getDomain().getMin();
    }

    public int getDomainMin() {
        return getDomainMin(0);
    }

    public int getDomainMax(int pos) {
        return agentState.getVariable(pos).getDomain().getMax();
    }

    public int getDomainMax() {
        return getDomainMax(0);
    }

    public int getDomainSize(int pos) {
        return agentState.getVariable(pos).getDomain().size();
    }

    public int getDomainSize() {
        return getDomainSize(0);
    }

    public int getDomainElement(int vIdx, int dIdx) {
        return agentState.getVariable(vIdx).getDomain().getElement(dIdx);
    }

    public int getDomainElement(int dIdx) {
        return agentState.getVariable().getDomain().getElement(dIdx);
    }

    /**
     * @return The worst aggregated value, assuming this is a maximization problem.
     */
    public int getAggregatedLB() {
        int res = 0;
        for (Constraint c : agentState.getConstraints()) {
            res += c.getWorstValue();
        }
        return res;
    }

    /**
     * @return The best aggregated value, assuming this is a maximization problem.
     */
    public int getAggregatedUB() {
        int res = 0;
        for (Constraint c : agentState.getConstraints()) {
            res += c.getBestValue();
        }
        return res;
    }

    public Evaluator getEvaluator() {
        return evaluator;
    }

    /**
     * Class used to evaluate constraints of a set of variables.
     * @note: only binary constraints.
     */
    public class Evaluator {
        protected List<Constraint> constraints;

        public Evaluator() {
            // assign only constraints involving variables in a gent with ID with no lower priority agent w.r.t. this one.
            constraints   = new ArrayList<>();
            for (Constraint c : agentState.getConstraints()) {
                boolean insert = true;
                for (Variable v : c.getScope()) {
                    if (v.getOwnerAgent().getID() < getAgentID()) {
                        insert = false;
                        continue;
                    }
                }
                if (insert && !constraints.contains(c))
                    constraints.add(c);
            }
        }

        public double evaluate() {
            double cost = 0;
            for (Constraint c : constraints) {
                int arity = c.getArity();
                Tuple t = new Tuple(arity);
                int i = 0;
                for (Variable v : c.getScope()) {
                    t.set(i, v.getValue());
                    i++;
                }
                double val = c.getValue(t);
                if (Constants.isInf(val)) {
                    cost = Constants.infinity;
                    break;
                } else {
                    cost += val;
                }
            }
            return cost;
        }

        /**
         * Extracts the list of agentsID whose given the values in input, have unsatisfied
         * constraint with the current agent.
         * @param values
         * @return
         */
        public ArrayList<Long> getNogoods(Tuple values) {
            ArrayList<Long> nogoods = new ArrayList<>();
//
//            for (int c = 0; c < nConstraints; c++) {
//                pair.set(0, values.get(varIDToValIdx[ constraintScope.get(c*2 + 0) ] ));
//                pair.set(1, values.get(varIDToValIdx[ constraintScope.get(c*2 + 1) ] ));
//                double value = constraints.get(c).getValue(pair);
//                if (Constraint.isUnsat(value)) {
//                    long id0 = constraints.get(c).getScope(0).getOwnerAgent().getID();
//                    long id1 = constraints.get(c).getScope(1).getOwnerAgent().getID();
//
//                    if (getAgentID() == id0)
//                        nogoods.add(id1);
//                    else
//                        nogoods.add(id0);
//                }
//            }
            return nogoods;
        }
    }

}
