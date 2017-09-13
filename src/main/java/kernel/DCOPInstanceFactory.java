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

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Created by ffiorett on 7/7/15
 * This is not a real Factory, but more an importer.
 * The term Factory was used for consistency
 */
public class DCOPInstanceFactory {

    private static final int XCSP_TYPE = 0;
    private static final int DZINC_TYPE = 1;
    private static final int USC_TYPE = 2;
    private static final int WCSP_TYPE = 3;
    private static final int DIMACS_TYPE = 4;
    private static final int CCG_TYPE = 5;
    private static final int JSON_TYPE = 6;

    public static DCOPInstance importDCOPInstance(String filename) {
        return importDCOPInstance(filename, -1);
    }

    public static DCOPInstance importDCOPInstance(String filename, int type) {
        String ext = FilenameUtils.getExtension(filename);
        if (ext.equalsIgnoreCase("xcsp") || ext.equalsIgnoreCase("xml") || type == XCSP_TYPE) {
            return createXCSPInstance(filename);
        } else if (ext.equalsIgnoreCase("usc") || type == USC_TYPE) {
            return createUSCInstance(filename);
        } else if (ext.equalsIgnoreCase("dzn") || type == DZINC_TYPE) {
            return createDZINCInstance(filename);
        } else if (ext.equalsIgnoreCase("dimacs") || type == DIMACS_TYPE ||
                ext.equalsIgnoreCase("wcsp") || type == WCSP_TYPE) {
            return createWCSPInstance(filename);
        } else if (ext.equalsIgnoreCase("ccg") || type == CCG_TYPE) {
            return createCCGInstance(filename);
        } else if (ext.equalsIgnoreCase("json") || type == JSON_TYPE) {
            return createJSONInstance(filename);
        }
        return null;
    }

    private static DCOPInstance createXCSPInstance(String filename) {

        DCOPInstance instance = new DCOPInstance();

        try {
            File fXmlFile = new File(filename);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            // Presentation
            NodeList presentation = doc.getElementsByTagName("presentation");
            String maximizeStr = presentation.item(0).getAttributes().getNamedItem("maximize").getNodeValue();
            Boolean maximize = Boolean.valueOf(maximizeStr);
            instance.setOptimization(maximize);

            int optType = maximize ? Constants.OPT_MAXIMIZE : Constants.OPT_MINIMIZE;

            // Agents
            NodeList agents = doc.getElementsByTagName("agent");
            for (int i = 0; i < agents.getLength(); i++) {
                Node nodeAgent = agents.item(i);
                String name = nodeAgent.getAttributes().getNamedItem("name").getNodeValue();

                // Create and store Agent in DCOP instance
                instance.addAgent(new AgentState(name, i));
            }

            // Variables
            NodeList variables = doc.getElementsByTagName("variable");
            for (int i = 0; i < variables.getLength(); i++) {
                Node nodeVariable = variables.item(i);
                String name = nodeVariable.getAttributes().getNamedItem("name").getNodeValue();
                String domainName = nodeVariable.getAttributes().getNamedItem("domain").getNodeValue();
                String agentName = nodeVariable.getAttributes().getNamedItem("agent").getNodeValue();

                // Get domain
                Node domainNode = getXMLNode(doc, "domain", domainName);
                String[] valuesStr = domainNode.getTextContent().split(Pattern.quote(".."));
                int min = Integer.valueOf(valuesStr[0]);
                int max = Integer.valueOf(valuesStr[1]);

                // Create and store Variable in DCOP instance
                AgentState agtOwner = instance.getAgent(agentName);
                Variable variable = VariableFactory.getVariable(name, min, max, "INT-BOUND", agtOwner);
                instance.addVariable(variable);
            }

            // Constraints
            NodeList constraints = doc.getElementsByTagName("constraint");
            for (int i = 0; i < constraints.getLength(); i++) {
                Node constraintNode = constraints.item(i);
                String name = constraintNode.getAttributes().getNamedItem("name").getNodeValue();
                int arity = Integer.valueOf(constraintNode.getAttributes().getNamedItem("arity").getNodeValue());
                String[] scopeStr = constraintNode.getAttributes().getNamedItem("scope").getTextContent().split(" ");
                String relName = constraintNode.getAttributes().getNamedItem("reference").getNodeValue();

                // Retrieve scope:
                ArrayList<Variable> scope = new ArrayList<Variable>();
                for (String s : scopeStr) {
                    scope.add(instance.getVariable(s));
                }

                // Get Relation
                Node relationNode = getXMLNode(doc, "relation", relName);
                String defCostStr = relationNode.getAttributes().getNamedItem("defaultCost").getNodeValue();
                int defaultValue = 0;
                if (defCostStr.equalsIgnoreCase("infinity"))
                    defaultValue = Constants.infinity;
                else if (defCostStr.equalsIgnoreCase("-infinity"))
                    defaultValue = -Constants.infinity;
                else
                    defaultValue = Integer.valueOf(defCostStr);
                String semantics = relationNode.getAttributes().getNamedItem("semantics").getNodeValue();

                // Create constraint
                Constraint constraint = ConstraintFactory.getConstraint(name, scope, defaultValue, semantics);

                // Add values
                int values[] = new int[arity];
                String[] valuesStr = relationNode.getTextContent().split(Pattern.quote("|"));

                for (String s : valuesStr) {
                    String costValue[] = s.split(Pattern.quote(":"));
                    String utilStr = costValue[0];
                    int utility =
                            utilStr.equalsIgnoreCase("infinity") ? Constants.infinity
                                    : utilStr.equalsIgnoreCase("-infinity") ? -Constants.infinity
                                    : Integer.valueOf(utilStr);
                    String tupleStr[] = costValue[1].split(Pattern.quote(" "));
                    assert (tupleStr.length == arity);
                    for (int t = 0; t < arity; t++) {
                        values[t] = Integer.valueOf(tupleStr[t]);
                    }
                    constraint.addValue(new Tuple(values), utility, optType);
                }

                // Store Constraint in DCOP instance
                instance.addConstraint(constraint);
            }

            return instance;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Line 1:
     *   <Problem name> <N> <K> <C> <UB>
     * where
     *   <N> is the number of variables (integer)
     *   <K> is the maximum domain size (integer)
     *   <C> is the total number of constraints (integer)
     *   <UB> is the global upper bound of the problem (long integer)
     * Variables:
     *   <domain size of variable with index 0> ...
     *   <domain size of variable with index N-1>
     * Constraints:
     *  <Arity of the constraint>
     *  <Index of the first variable in the scope of the constraint>
     *  ...
     *  <Index of the last variable in the scope of the constraint>
     *  <Default cost value>
     *  <Number of tuples with a cost different than the default cost>
     *  and for every tuple (again in one line):
     * @param filename
     * @return
     */
    private static DCOPInstance createWCSPInstance(String filename) {
        DCOPInstance instance = new DCOPInstance();
        int optType = Constants.OPT_MINIMIZE;

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            // First line (problem preamble)
            String[] tokens = br.readLine().split(" ");
            String instance_name = tokens[0];
            int n = Integer.parseInt(tokens[1]);
            int k = Integer.parseInt(tokens[2]);
            int c = Integer.parseInt(tokens[3]);
            long ub = Integer.parseInt(tokens[4]);
            //System.out.println("read: " + n + " " + " " + k + " " + c + " " + ub);

            // Second line (problem variables)
            tokens = br.readLine().split(" ");
            assert (tokens.length == n);
            for (int i = 0; i < n; i++) {
                String agtName = "a" + Integer.toString(i);
                String varName = "v" + Integer.toString(i);

                // Create and store Agent in DCOP instance
                AgentState agt = new AgentState(agtName, i);
                instance.addAgent(agt);

                // Create Variable and it in the DCOP instance
                int min = 0;
                int max = Integer.parseInt(tokens[i]) - 1;
                Variable variable = VariableFactory.getVariable(varName, min, max, "INT-BOUND", agt);
                instance.addVariable(variable);
                //System.out.println(variable.toString());
            }

            // Create Constraints
            int cIdx = 0;
            String line;
            while ((line = br.readLine()) != null) {
                tokens = line.split(" ");
                String name = "c" + cIdx++;
                int j = 0;
                // Retrieve scope:
                ArrayList<Variable> scope = new ArrayList<Variable>();
                int arity = Integer.parseInt(tokens[0]);
                for (j = 1; j <= arity; j++) {
                    scope.add(instance.getVariable(Integer.parseInt(tokens[j])));
                }
                Integer defaultValue = Integer.parseInt(tokens[j++]);
                long numEntries = Integer.parseInt(tokens[j]);

                Constraint constraint = ConstraintFactory.getConstraint(name, scope, defaultValue, "soft");
                // Fill in constraint
                int values[] = new int[arity];
                for (int eid = 0; eid < numEntries; eid++)
                {
                    tokens = br.readLine().split(" ");
                    for (int i = 0; i < arity; i++)
                        values[i] = Integer.parseInt(tokens[i]);
                    double cost = Double.parseDouble(tokens[arity]);
                    constraint.addValue(new Tuple(values), cost, optType);
                }
                instance.addConstraint(constraint);
                //System.out.println(constraint);
            }
            return instance;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get the XMLNode of the given categroy matching the given name.
     * @param doc The XML document.
     * @param tag The tag to match.
     * @param name The name to match.
     * @return
     */
    private static Node getXMLNode(Document doc, String tag, String name) {
        NodeList nlist = doc.getElementsByTagName(tag);
        for (int i = 0; i < nlist.getLength(); i++) {
            Node node = nlist.item(i);
            String nodeName = node.getAttributes().getNamedItem("name").getNodeValue();
            if (nodeName.equalsIgnoreCase(name)) {
                return node;
            }
        }
        return null;
    }


    /**
     * Line 1:
     *   p edges <N> <E>
     * where
     *   p and edges are keywords
     *   <N> is the number of variables (integer)
     *   <E> is the total number of edges (integer)
     * Variables:
     *   Have all binary domains. A unary constraint is associated to a variable and described as:
     *   v <v_id> <cost>
     *   where
     *     'v' is a keyword
     *     <v_id> is the ID of the variable
     *     <cost> is the cost associated to the choice: v = 1. When v = 0 , the cost = 0
     * Edges:
     *   Each edge is described as:
     *   e <v1_id> <v2_id>
     *   where:
     *     'e' is a keyword
     *     <v1_id>, <v2_id> are the ID of the two nodes in the edge
     *     The constraint is : e(x1, x2) = INF if d1=d2=0; 0, otherwise.
     *  --- vertex types begin ---
     *  For each variable of the problem lists:
     *  <vid> <type>
     *      where
     *      <vid> is the variable ID
     *      <type> \in {0, -,1, -2} denoting, respectively, a problem variable, and two auxiliary variables.
     *  --- vertex types end ---
     * @param filename
     * @return
     */
    private static DCOPInstance createCCGInstance(String filename) {
        DCOPInstance instance = new DCOPInstance();
        int optType = Constants.OPT_MINIMIZE;

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            int cIdx = 0; // number of constraints

            // First line (problem preamble)
            String[] tokens = br.readLine().split(" ");
            assert (tokens.length == 4);
            int n = Integer.parseInt(tokens[2]);
            int e = Integer.parseInt(tokens[3]);
            //System.out.println("read: " + n + " variables and " + e + " edges.");

            // Parse Variables
            for (int i = 0; i < n; i++) {
                tokens = br.readLine().split(" ");
                assert (tokens[0].equalsIgnoreCase("v"));
                int id = Integer.parseInt(tokens[1]);
                double cost = Double.parseDouble(tokens[2]);

                String varName = "v" + Integer.toString(id);
                // todo: Associate mutliple (auxliary) variables to one agent.
                String agtName = "a" + Integer.toString(id);

                // Create and store Agent in DCOP instance
                AgentState agt = new AgentState(agtName, id);
                instance.addAgent(agt);


                // Create Variable and it in the DCOP instance
                Variable variable = VariableFactory.getVariable(varName, 0, 1, "INT-BOUND", agt);
                instance.addVariable(variable);
                //System.out.println(variable.toString());

                // Create Constraint
                ArrayList<Variable> scope = new ArrayList<Variable>();
                scope.add(variable);
                String cname = "c" + cIdx++;
                Constraint constraint = ConstraintFactory.getConstraint(cname, scope, 0, "soft");
                constraint.addValue(new Tuple(new int[]{0}), 0, optType);
                constraint.addValue(new Tuple(new int[]{1}), cost, optType);
                instance.addConstraint(constraint);
                //System.out.println(constraint);
            }


            // Create Constraints
            for (int i = 0; i < e; i++) {
                tokens = br.readLine().split(" ");
                assert (tokens[0].equalsIgnoreCase("e"));
                int id1 = Integer.parseInt(tokens[1]);
                int id2 = Integer.parseInt(tokens[2]);
                Variable v1 = instance.getVariable(id1);
                Variable v2 = instance.getVariable(id2);
                ArrayList<Variable> scope = new ArrayList<Variable>();
                scope.add(v1);
                scope.add(v2);
                String cname = "c" + cIdx++;
                Constraint constraint = ConstraintFactory.getConstraint(cname, scope, 0, "soft");
                constraint.addValue(new Tuple(new int[]{0,0}), Constants.infinity, optType);
                constraint.addValue(new Tuple(new int[]{0,1}), 0, optType);
                constraint.addValue(new Tuple(new int[]{1,0}), 0, optType);
                constraint.addValue(new Tuple(new int[]{1,1}), 0, optType);
                instance.addConstraint(constraint);
                //System.out.println(constraint);
            }

            // Process variable type
            String line = br.readLine();
            assert (line.contains("vertex types begin"));
            for (int i = 0; i < n; i++) {
                tokens = br.readLine().split(" ");
                assert (tokens.length == 2);
                int id   = Integer.parseInt(tokens[0]);
                int type = Integer.parseInt(tokens[1]);
                instance.getVariable(id).setType(type);
            }
            return instance;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static DCOPInstance createJSONInstance(String filename) {
        DCOPInstance instance = new DCOPInstance();
        int optType = Constants.OPT_MINIMIZE;

        JSONParser parser = new JSONParser();
        try {

            Object obj = parser.parse(new FileReader(filename));
            JSONObject jsonObject = (JSONObject) obj;

            JSONObject agents = (JSONObject) jsonObject.get("agents");
            JSONObject variables = (JSONObject) jsonObject.get("variables");
            JSONObject constraints = (JSONObject) jsonObject.get("constraints");

            // Parse Agents
            for(Iterator it = agents.keySet().iterator(); it.hasNext();) {
                String name = (String) it.next();
                JSONObject agt = (JSONObject) agents.get(name);
                long id = (Long) agt.get("id");

                // Create and store Agent in DCOP instance
                AgentState agent = new AgentState(name, id);
                instance.addAgent(agent);
                //System.out.println(agent.toString());
            }

            // Parse Variables
            for(Iterator it = variables.keySet().iterator(); it.hasNext();) {
                String name = (String) it.next();
                JSONObject var = (JSONObject) variables.get(name);
                long id = (Long) var.get("id");
                long type = (Long) var.get("type");
                String agt_name = (String) var.get("agent");

                JSONArray domain = (JSONArray) var.get("domain");
                long min = (Long)domain.get(0);
                long max = (Long)domain.get(domain.size()-1);
                assert (domain.size() == 2);

                // Create Variable and it in the DCOP instance
                Variable variable = VariableFactory.getVariable(name, id, (int)min, (int)max, "INT-BOUND", (int)type,
                                                                instance.getAgent(agt_name));
                instance.addVariable(variable);
                //System.out.println(variable.toString());
            }

            // Parase Constraints
            for(Iterator it = constraints.keySet().iterator(); it.hasNext();) {
                String name = (String) it.next();
                JSONObject con = (JSONObject) constraints.get(name);
                JSONArray jscope = (JSONArray) con.get("scope");
                ArrayList<Variable> scope = new ArrayList<Variable>();

                Iterator<String> iterator = jscope.iterator();
                while (iterator.hasNext()) {
                    scope.add(instance.getVariable(iterator.next()));
                }

                Constraint constraint = ConstraintFactory.getConstraint(name, scope, 0, "soft");

                JSONArray jvals = (JSONArray) con.get("vals");
                assert (scope.size() < 2);

                if (scope.size() == 1) {
                    Double val1 = ((Double) jvals.get(0)) <= -999.0 ? Constants.infinity : (Double) jvals.get(0);
                    constraint.addValue(new Tuple(new int[]{0}), val1, optType);
                    Double val2 = ((Double) jvals.get(1)) < -999.0 ? Constants.infinity : (Double) jvals.get(1);
                    constraint.addValue(new Tuple(new int[]{1}), val2, optType);
                }
                else if (scope.size() == 2) {
                    Double val1 = ((Double) jvals.get(0)) < -999.0 ? Constants.infinity : (Double) jvals.get(0);
                    constraint.addValue(new Tuple(new int[]{0, 0}), val1, optType);
                    Double val2 = ((Double) jvals.get(1)) < -999.0 ? Constants.infinity : (Double) jvals.get(1);
                    constraint.addValue(new Tuple(new int[]{0, 1}), val2, optType);
                    Double val3 = ((Double) jvals.get(2)) < -999.0 ? Constants.infinity : (Double) jvals.get(2);
                    constraint.addValue(new Tuple(new int[]{1, 0}), val3, optType);
                    Double val4 = ((Double) jvals.get(3)) < -999.0 ? Constants.infinity : (Double) jvals.get(3);
                    constraint.addValue(new Tuple(new int[]{1, 1}), val4, optType);
                }

                instance.addConstraint(constraint);
                //System.out.println(constraint.toString());
            }
            return instance;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    private static DCOPInstance createUSCInstance(String filename) {
        return null;
    }

    private static DCOPInstance createDZINCInstance(String filename) {
        return null;
    }

}




