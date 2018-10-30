package problem;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class DecisionMaker {

    private Queue<Action> ActionSequence;

    public void DecisionMaker () {
        ActionSequence = new LinkedBlockingQueue<>();
    }

    public Action getAction() {
        if (ActionSequence.isEmpty()) {
            getSequence();
        }
        return ActionSequence.poll();
    }

    private void getSequence() {
        //TODO implementation
        this.ActionSequence = null;
    }

    private List<Action> getAllAction() {
        //TODO implementation
        return null;
    }

    private float getReward() {
        //TODO implementation
        return 0;
    }

    private List getProbs() {
        //TODO implementation
        return null;
    }
}
