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

import communication.DCOPagent;
import communication.Spawner;
import kernel.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ffiorett on 7/7/15.
 */
public class dcop_jtools {

    public static void main(String argv[]) {
        String agentType = "CCG";
        List<Object> algParams = new ArrayList<>();
        int nbIterations = 5;
        long timeoutMs = Constants.infinity; // no-timeout
        String file = "../data/2-constraint.dimacs";

        if (argv.length < 1) {
            System.out.println(getUsage());
            return;
        }
        file = argv[0];
        String fileout_stats = "";
        for (int i = 1; i < argv.length; i++) {
            if (argv[i].equals("-a") || argv[i].equals("--agent")) {
                agentType = argv[i+1];
            }
            if (argv[i].equals("-i") || argv[i].equals("--iterations")) {
                nbIterations = Integer.parseInt(argv[i+1]);
            }
            if (argv[i].equals("-t") || argv[i].equals("--timeout")) {
                timeoutMs = Long.parseLong(argv[i+1]);
            }
            if (argv[i].equals("-o")) {
                fileout_stats = argv[i+1];
            }
        }
        algParams.add(agentType);
        algParams.add(nbIterations);
        algParams.add(timeoutMs);

        DCOPInstance dcopInstance = DCOPInstanceFactory.importDCOPInstance(file);
        System.out.println("Read DCOP instance. N_agents=" + dcopInstance.getDCOPAgents().size()
                + " N_vars=" + dcopInstance.getDCOPVariables().size()
                + " N_cons=" + dcopInstance.getDCOPConstraints().size()
                + "\nStarting algorithm... ");

        Spawner spawner = new Spawner(dcopInstance);
        spawner.spawn(algParams);

        // Summary Output
        printSummary(spawner.getSpawnedAgents(), fileout_stats);
//        System.out.println(getSummary(spawner.getSpawnedAgents(), nbIterations));
    }

    public static String getUsage() {
        return "dcop_jtool FILE.xml [options]\n" +
                "  where options is one of the following:\n" +
                "  --alg (-a) [MINSUM|CCG]. The Agent type\n" +
                "  --repair (-r) [GDBR, TDBR(default)]. The DLNS repair phase.\n" +
                "  --destroy (-d) [RAND(default), MEETINGS]. The DLNS destroy phase.\n" +
                "  --iterations (-i) (default=500). The number of iterations of DLNS.\n" +
                "  --timeout (-t) (default=no timeout (0)). The simulated time maximal execution time.\n";
    }

    public static void printSummary(Collection<DCOPagent> agents, String fileout) {
        JSONObject jsonObject = new JSONObject();
        JSONObject jsonVars = new JSONObject();
        int maxIter = 0;

        for (DCOPagent agt : agents) {
            HashMap<String, List<Integer>> agtValueList = agt.getAgentStatistics().getSolutionValue();
            for (String vname : agtValueList.keySet()) {
                jsonVars.put(vname, agtValueList.get(vname));
                maxIter = Math.max(maxIter, agtValueList.get(vname).size());
            }
        }
        jsonObject.put("values", jsonVars);
        jsonObject.put("iterations", maxIter);

        JSONArray jsonNetLoad = new JSONArray();
        JSONArray jsonSimTime = new JSONArray();
        long maxTime = 0;
        int netLoad = 0;

        for (int iter = 0; iter < maxIter; iter++) {
            int agtMsgs = 0;
            for (DCOPagent agt : agents) {
                //if (iter >= agt.getAgentStatistics().size()) continue;
                maxTime = Math.max(maxTime, agt.getAgentStatistics().getMilliTime(iter));
                int msgNow = agt.getAgentStatistics().getSentMessages(iter);
                int msgPrev = iter == 0 ? 0 : agt.getAgentStatistics().getSentMessages(iter - 1);
                agtMsgs = Math.max(agtMsgs, (msgNow - msgPrev));
                netLoad += (msgNow - msgPrev);
            }
            jsonSimTime.add(maxTime);
            jsonNetLoad.add(netLoad);
        }
        jsonObject.put("simTime", jsonSimTime);
        jsonObject.put("netLoad", jsonNetLoad);

        if (!fileout.isEmpty()) {
            try (FileWriter file = new FileWriter(fileout)) {
                file.write(jsonObject.toJSONString());
                file.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println(jsonObject);
        }

    }

    public static String getSummary(Collection<DCOPagent> agents, int nbIterations) {
        String res = "time\tLB\tUB\tIterAgtMsgs\tnAgtMsgs\tNetLoad\n";
        int maxIter = DCOPinfo.leaderAgent.getAgentStatistics().size();
        int bestLB = DCOPinfo.leaderAgent.getAgentStatistics().getBounds(maxIter-1)[0];
        long maxTime = 0; int nMsgs = 0; int netLoad = 0;
        int lb = 0; int ub = 0;
        int prevLB = Constants.NaN; int prevUB = Constants.NaN;

        for (int iter = 0; iter < maxIter; iter++) {
            int agtMsgs = 0;
            boolean save = false;
            for (DCOPagent agt : agents) {
                if (iter >= agt.getAgentStatistics().size()) continue;
                maxTime = Math.max(maxTime, agt.getAgentStatistics().getMilliTime(iter));
                int msgNow =  agt.getAgentStatistics().getSentMessages(iter);
                int msgPrev = iter == 0 ? 0 : agt.getAgentStatistics().getSentMessages(iter-1);
                nMsgs = Math.max(nMsgs, msgNow);
                agtMsgs = Math.max(agtMsgs, (msgNow - msgPrev));
                netLoad += (msgNow - msgPrev);
                if (agt.isLeader()) {
                    lb = agt.getAgentStatistics().getBounds(iter)[0];
                    ub = agt.getAgentStatistics().getBounds(iter)[1];
                    if (ub < bestLB) ub = bestLB;         // TODO: !!!!!!!!!!! Fix this !!!!!!!!!!!!
                    if (prevLB != lb) {prevLB = lb; save = true;}
                    if (prevUB != ub) {prevUB = ub; save = true;}
                }
            }
            if (save) {
                res += maxTime + "\t";
                if (Constraint.isSat(ub) && Constraint.isSat(lb)) {
                    res += lb + "\t" + ub + "\t";
                } else {
                    res += "NA\tNA\t";
                }
                res += + agtMsgs + "\t" + nMsgs + "\t" + netLoad + "\n";
            }
        }
        return  res;
    }

}

