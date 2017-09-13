# MaxSum Actions:
- Give to each agent:
    1. One variable x (the one it controls)   [initNodes]
    2. The set of constraints whose scope S is s.t. x \in S is the 
       variable with smaller ID among all those in S
    3. Set A random assignment for the variable (0)
    4. At each cycle [onMailBoxEmpty]
       1. The variable node sums all messages received and chooses the lowest value
       2. If this node is a VariableNode:
          neighbors= *all* the constraints nodes this variable is connected to.
          For each n : neighbor
             T = sumMessages(n);
             addUnaryConstraints(T);
             subtractMinimumValue(T);
             addPreferences(T); [Adds the variable's dust (used as tie-breakers)]
             sendMessage(n, T);
       3. If this node is a FactorNode:
          neighbors= *all* variablesNodes in the scope of this factor node
          For each n : neighbor
             T = copy of constraint table involving var n
             if this agent controls n: (do nothing)
             else: T = T + msg_recv_from_n   <<< contains the values of the other vars for when its var is assigned 
             T_proj = getBestValues(T);          [projects the variable (n) where we are sending this message]
   		     subtractMinimumValue(T_proj);
		     sendMessage(n, T_proj);

# TODO List

[] Debug 3-constraint behavior [why all zeros?]