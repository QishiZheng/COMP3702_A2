package problem;

import simulator.State;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static problem.ProblemSpec.CAR_MIN_MOVE;
import static problem.ProblemSpec.CAR_MOVE_RANGE;

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

    /**
     * The reward will based on current state and will foc on
     * the distance that each action can take
     * R(s) = (Σprobs[i] * stepDistance) - (slipTime / totalTime) * slipTime
     *          - (breakTime / totalTime) * breakTime;
     * @return current reward
     */
    private float getReward(ProblemSpec ps, State state, Double[] probs) {
        float reward = 0f;
        int breakTime = ps.getRepairTime();
        int slipTime = ps.getSlipRecoveryTime();
        int totalTime = ps.getMaxT();
        if(state.isInBreakdownCondition()) {
            //breakdown
            reward -= breakTime * (breakTime / totalTime);
        } else if(state.isInSlipCondition()) {
            //slip
            reward -= slipTime * (slipTime / totalTime);
        } else {
            //good condition
            for(int i = 0; i < CAR_MOVE_RANGE; i++) {
                //slip or breakdown
                if(CAR_MOVE_RANGE + i == 6) {
                    reward -=  (slipTime / totalTime) * probs[i];
                } else if (CAR_MOVE_RANGE + i == 7) {
                    reward -= (breakTime / totalTime) * probs[i];
                } else {
                    reward += (CAR_MIN_MOVE + i) * probs[i];
                }
            }
        }
        return reward;
    }

    private List getProbs() {
        //TODO implementation
        return null;
    }
}
