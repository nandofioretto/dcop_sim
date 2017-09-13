package agent;

import communication.ComAgent;
import communication.CycleTickerDeamon;
import communication.DCOPagent;
import communication.Message;
import kernel.AgentState;
import kernel.DCOPinfo;

/**
 * Created by nando on 6/2/17.
 */
public abstract class SynchronousAgent extends DCOPagent {

    int currentCycle;
    CycleTickerDeamon cycleTickerDeamon;

    public SynchronousAgent(ComAgent statsCollector, AgentState agentState) {
        super(statsCollector, agentState);
        this.currentCycle = 0;
        cycleTickerDeamon = DCOPinfo.cycleTickerDeamon;
    }

    @Override
    protected void onStart() {
        runCycle();
    }

    @Override
    protected void onReceive(Object message, ComAgent sender) {
        super.onReceive(message, sender);

        if (message instanceof Message.StartNewCycle) {
            currentCycle++;
            runCycle();
        }
    }


    protected void runCycle() {
        onCycleStart();
        cycle();
    }

    protected abstract void cycle();

    protected abstract void onCycleEnd();

    protected abstract void onCycleStart();

    protected void terminateCycle() {
        onCycleEnd();
        cycleTickerDeamon.terminateAgentCycle(getSelf());
    }

    public int getCurrentCycle() {
        return currentCycle;
    }
}
