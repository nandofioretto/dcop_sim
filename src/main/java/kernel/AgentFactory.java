package kernel;

import agent.BinaryCCG.BinaryCCGAgentMVA;
import agent.MaxSum.MaxSumAgent;
import communication.ComAgent;
import communication.DCOPagent;

import java.util.List;

/**
 * Created by nandofioretto on 5/22/17.
 */
public class AgentFactory {

    public static DCOPagent create(ComAgent statsCollector, AgentState agtState, List<Object> algParameters) {
        String agt = (String)algParameters.get(0);
        if (agt.toUpperCase().equals("CCG"))
            return new BinaryCCGAgentMVA(statsCollector, agtState, algParameters.subList(1, algParameters.size()));
        else if (agt.toUpperCase().equals("MINSUM"))
            return new MaxSumAgent(statsCollector, agtState, algParameters.subList(1, algParameters.size()));
        else
            return null;
    }

}
