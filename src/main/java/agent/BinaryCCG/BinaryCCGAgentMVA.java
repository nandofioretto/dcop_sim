package agent.BinaryCCG;

import agent.SynchronousAgent;
import communication.BasicMessage;
import communication.ComAgent;
import kernel.*;

import java.util.*;

/**
 * Created by fioretto on 7/17/17.
 */
public class BinaryCCGAgentMVA extends SynchronousAgent {

    private int nbCycles = Integer.MAX_VALUE;
    private double convergenceDelta = 0.001;
    private int nbRecvMsgs;
    private int nbVarsNeighbor;
    private Random rand = new Random();

    // Cost received by each neighbor by projecting out y if this variable is x [prev cycle]
    // key: variable ID; value: vector of size Dom of this variable
    private HashMap<Long, HashMap<Long, double[]>> costTables;

    // Cost received by each neighbor  at current cycle
    // key: variable ID; value: Key: Neighbor var ID Value: vector of size Dom of this variable
    private HashMap<Long, HashMap<Long, double[]>> recvCostTables;

    // Var ID -> [Var ID (neighbor, ComAgent) ] if ComAgent = null, then the two neighbors belong to the same agent.
    // otherwise ComAgent is the link to the neighbor owning the variable (Var ID [2])
    private HashMap<Long, List<Pair<Long, ComAgent>>> varNeighbors;

    // Weights of the unary constraint
    private HashMap<Long, Double> weights;

    private HashMap<Long, Integer> agtVarIdxMap;

    // A vector of noisy values to allow faster convergence
    public HashMap<Long, double[]> noise;

    AgentState agentState;

    public BinaryCCGAgentMVA(ComAgent statsCollector, AgentState agentState, List<Object> parameters) {
        super(statsCollector, agentState);
        // Check argument:
        assert (parameters.size() == 1);
        // check binary
        assert(getAgentView().getDomainSize() == 2);

        this.nbCycles = (int) parameters.get(0);
        this.costTables = new HashMap<>();
        this.recvCostTables = new HashMap<>();
        for (Variable v : agentState.getVariables()) {
            this.costTables.put(v.getID(), new HashMap<>());
            this.recvCostTables.put(v.getID(), new HashMap<>());
        }
        this.weights = new HashMap<>();
        this.nbRecvMsgs = 0;
        this.nbVarsNeighbor = 0;
        this.agtVarIdxMap = new HashMap<>();

        int i = 0;
        for (Variable v : agentState.getVariables()) {
            agtVarIdxMap.put(v.getID(), i);
            i++;
        }
        this.noise = new HashMap<>();
        for (Variable v : agentState.getVariables()) {
            double[] v_noise = new double[2];
            for (i = 0; i <v_noise.length; i++) {
                v_noise[i] = rand.nextInt(20);//Math.random();
            }
            noise.put(v.getID(), v_noise);
        }
        this.agentState = agentState;
    }

    @Override
    protected void onStart() {

        // Initialize Neighbors
        this.varNeighbors = new HashMap<>();
        for (Variable v : agentState.getVariables()) {
            this.varNeighbors.put(v.getID(), new ArrayList());

            // For every (binary) constraint involving v, find the other variable u.
            List<Long> vars_inserted = new ArrayList<>();
            for (Constraint c : v.getParticipatingConstraints()) {
                if (c.isBinary()) {
                    //System.out.println("agent_" + getId() + " var_" + v.getID() +" " + c.toString());

                    Variable u = c.getScope().get(0) == v ? c.getScope().get(1) : c.getScope().get(0);
                    if (vars_inserted.contains(u.getID()))
                        continue;
                    vars_inserted.add(u.getID());

                    ComAgent n_agt = u.getOwnerAgent().equals(agentState) ? null : u.getOwnerAgent().getComAgent();
                    this.varNeighbors.get(v.getID()).add(new Pair<>(u.getID(), n_agt));
                    nbVarsNeighbor ++;
                }
            }
        }

        // Set weights
        Tuple tuple = new Tuple(new int[]{1});
        for (Variable v : agentState.getVariables()) {
            for (Constraint cv : v.getParticipatingConstraints()) {
                if (cv.isUnary())
                    this.weights.put(v.getID(), cv.getValue(tuple));
            }
        }

        // Initialize messages
        double[] costs = new double[getAgentView().getDomainSize()];
        Arrays.fill(costs, 0);

        for (Variable v : agentState.getVariables()){
            costTables.put(v.getID(), new HashMap<>());
            recvCostTables.put(v.getID(), new HashMap<>());
            for (Pair<Long, ComAgent> p : varNeighbors.get(v.getID())) {
                Long uID = p.getFirst();
                costTables.get(v.getID()).put(uID, costs.clone());
            }
        }

        for (int i = 0; i < getAgentView().getNbVariables(); i++) {
            getAgentActions().setVariableValue(i,  rand.nextInt(1));
        }
        // start cycling
        super.onStart();
    }

    @Override
    protected void cycle() {
        // Send messages
        for (Long vId : varNeighbors.keySet()) {
            for (Pair<Long, ComAgent> p : varNeighbors.get(vId)) {
                Long uId = p.getFirst();
                Boolean same_agent = (p.getSecond() == null);
                // todo: make this faster by not constructing the table if sender = recev agent
                double[] table = getCostTableSumExcluding(vId, uId);
                Commons.rmValue(table, Commons.getMin(table));
                //Commons.rmValue(table, Commons.getAverage(table));
                Commons.addArray(table, noise.get(vId));

                if (same_agent) {
                    recvCostTables.get(uId).put(vId, table/*.clone()*/);
                    incrRecvMsgs();
                } else {
                    CCGTableMessage msg = new CCGTableMessage(table, vId, uId);
                    p.getSecond().tell(msg, getSelf());
                }
            }
        }
    }

    @Override
    protected void onStop() {
        boolean converged = true;
        for (Long vId : varNeighbors.keySet()) {
            int val = selectBestValue(vId);
            getAgentActions().setVariableValue(agtVarIdxMap.get(vId), val);
            if (Constants.isInf(val)) {
                converged = false;
            }

//            if (getAgentView().getVariableType(agtVarIdxMap.get(vId)) == Variable.DECISION_VAR) {
//                System.out.println("Agent " + getName() + "(" + getCurrentCycle() + ") var_" + vId + " val: " + val);
//            }
        }

        super.onStop();
    }

    @Override
    protected void onReceive(Object message, ComAgent sender) {
        super.onReceive(message, sender);
        if (message instanceof CCGTableMessage) {
            CCGTableMessage msg = (CCGTableMessage)message;
            long vId = msg.getRecverVarId();
            long uId = msg.getSenderVarId();
            recvCostTables.get(vId).put(uId, msg.getTable());
            //System.out.println("var_" + vId + "(" + getCurrentCycle() + ") recv msg: " + Arrays.toString(recvCostTables.get(vId).get(uId)) + " from var_" + uId);
            //System.out.println(getName() + "[v_" + vId + "]"+"(" + getCurrentCycle() + ")  # msg recv: " + "[v_"+ uId + "]" + message.toString() + (nbRecvMsgs+1) + " / " + nbVarsNeighbor);
            incrRecvMsgs();
        }
    }

    private void incrRecvMsgs() {
        nbRecvMsgs++;
        if (nbRecvMsgs >= nbVarsNeighbor) {
            //setAgtState(STOPPED);
            terminateCycle();
        }
    }

    @Override
    protected void onCycleStart() {
        for (Long vId : varNeighbors.keySet()) {
            int val = selectBestValue(vId);
            if (Math.random() > 0.5)
                getAgentActions().setVariableValue(agtVarIdxMap.get(vId), val);

            //System.out.println("Agent " + getName() + "(" + getCurrentCycle() + ") var_" + vId + " val: " + val);
        }
    }

    @Override
    protected void onCycleEnd() {
        nbRecvMsgs = 0;
        // Save all received messages to be used in the next iteration
        for (Long vID : recvCostTables.keySet()) {
            for (Map.Entry<Long, double[]> entry : recvCostTables.get(vID).entrySet()) {
                Long uID = entry.getKey();
                double[] value = entry.getValue();
                costTables.get(vID).put(uID, value.clone());
            }
        }
//        System.out.println("Agent " + getName() + " Terminating cycle  " + getCurrentCycle());
    }

    @Override
    protected boolean terminationCondition() {
        return getCurrentCycle() >= nbCycles;
    }

    /**
     * Returns the aggregated cost table, excluding the costs produced by this agent
     * @param vId The variable ID for which to compute the table cost
     */
    private double[] getCostTableSumExcluding(long vId, long excludedId) {
        double[] sum = new double[]{weights.get(vId), 0};
        double[] s = getCostTableSum(vId, excludedId);
        s[1] += weights.get(vId);
        sum[0] = s[1];
        sum[1] = Math.min(s[0], s[1]);
        return sum;
    }

    /**
     *
     * @param excludedVarId The variable ID to exclude
     * @param vId This variable ID
     * @return
     */
    private double[] getCostTableSum(long vId, long excludedVarId) {
        double[] sum = new double[]{0,0};
        for (Pair<Long, ComAgent> p : varNeighbors.get(vId)) {
            long uId = p.getFirst();
            if (uId == excludedVarId)
                continue;
            Commons.addArray(sum, costTables.get(vId).get(uId));
        }
        return sum;
    }

    /**
     *
     * @param vId This variable ID for which select the best value
     * @return
     */
    private int selectBestValue(long vId) {
        double[] table = getCostTableSum(vId, -1);
        table[1] += weights.get(vId);

        // Add noise to speed up convergence
        Commons.addArray(table, noise.get(vId));

        return Commons.getArgMin(table);
    }

    /// Messages ----------------------- //
    public static class CCGTableMessage extends BasicMessage {
        protected double[] table;
        private long senderVarId;
        private long recverVarId;

        // vId = sender var ID, uId = receiver var ID
        public CCGTableMessage(double[] table, long vId, long uId) {
            this.table = table;
            this.senderVarId = vId;
            this.recverVarId = uId;
        }

        public double[] getTable() {
            return table;
        }

        public long getSenderVarId() {
            return senderVarId;
        }

        public long getRecverVarId() {
            return recverVarId;
        }

        @Override
        public String toString() {
            return "CCGTableMessage{" +
                    "table=" + Arrays.toString(table) +
                    ", sender varId=" + senderVarId +
                    ", recver varId=" + recverVarId +
                    '}';
        }
    }
}
