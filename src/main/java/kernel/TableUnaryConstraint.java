package kernel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by fioretto on 7/17/17.
 */
public class TableUnaryConstraint implements Constraint {
    private double[] relation;
    private double defaultValue;
    Variable scope;
    private String name;
    private long ID;

    private int domMin, domSize;
    private double bestValue;
    private double worstValue;

    public TableUnaryConstraint(String name, long ID, Variable scope, double defaultValue) {
        domMin = scope.getDomain().getMin();
        domSize = scope.getDomain().size();

        this.relation = new double[domSize];
        Arrays.fill(relation, defaultValue);
        this.scope = scope;
        this.name = name;
        this.ID = ID;
        this.defaultValue = defaultValue;
        this.bestValue  = Constants.NaN;
        this.worstValue = Constants.NaN;
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public boolean isUnary() {
        return true;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public long getID() {
        return ID;
    }

    @Override
    public void setID(long ID) {
        this.ID = ID;
    }

    @Override
    public void addValue(Tuple key, double value, int optType) {
        relation[key.get(0) - domMin] = value;
        if (Constraint.isSat(value)) {
            if (optType == Constants.OPT_MAXIMIZE) {
                if (bestValue < value || bestValue == Constants.NaN) bestValue = value;
                if (worstValue > value || worstValue == Constants.NaN) worstValue = value;
            } else {
                if (bestValue > value || bestValue == Constants.NaN) bestValue = value;
                if (worstValue < value || worstValue == Constants.NaN) worstValue = value;
            }
        }
    }

    @Override
    public double getValue(Tuple values) {
        return relation[values.get(0)];
    }

    @Override
    public double getBestValue() {
        return bestValue;
    }

    @Override
    public double getWorstValue() {
        return worstValue;
    }

    @Override
    public List<Variable> getScope() {
        List<Variable> l = new ArrayList<Variable>();
        l.add(scope);
        return l;
    }

    @Override
    public Variable getScope(int pos) {
        assert (pos == 0);
        return scope;
    }

    @Override
    public int getArity() {
        return 1;
    }

    @Override
    public double getDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TableUnaryConstraint)) return false;

        TableUnaryConstraint that = (TableUnaryConstraint) o;

        return ID == that.ID;
    }

    @Override
    public int hashCode() {
        return (int) (ID ^ (ID >>> 32));
    }

    @Override
    public String toString() {
        String ret = "TableConstraint " + name + " scope = " + scope.getName() + "\n";
        double val0 = relation[0];
        double val1 = relation[1];
        ret += "0: ";
        ret += val0 == Constants.infinity ? "inf\n" : val0 == -Constants.infinity ? "-inf\n" : val0 + "\n";
        ret += "1: ";
        ret += val1 == Constants.infinity ? "inf\n" : val1 == -Constants.infinity ? "-inf\n" : val1 + "\n";
        return ret;
    }

}
