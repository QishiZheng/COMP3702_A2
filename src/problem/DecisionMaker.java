package problem;

import java.util.LinkedList;
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

    public List<List<Action>> getAllAction(ProblemSpec ps) {
        List<List<Action>> rtn = new LinkedList<>();
        List<ActionType> actions = new LinkedList<>();
        actions.add(ActionType.CHANGE_CAR);
        actions.add(ActionType.CHANGE_DRIVER);
        actions.add(ActionType.CHANGE_TIRES);
        actions.add(ActionType.CHANGE_PRESSURE);
        LinkedList<Action> move = new LinkedList<>();
        move.add(new Action(ActionType.MOVE));
        rtn.add(move);
        while (actions.size() != 0) {
            List<ActionType> chain = new LinkedList<>();
            rtn.addAll(buildActionList(chain, ((LinkedList<ActionType>) actions).getFirst()));
            rtn.addAll(subActions(actions, ((LinkedList<ActionType>) actions).getFirst(), ps, chain));
            ((LinkedList<ActionType>) actions).removeFirst();
        }
        return rtn;
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

    private List<List<Action>> subActions(List<ActionType> available, ActionType parentAction, ProblemSpec ps, List<ActionType> chain) {
        chain.add(parentAction);
        List<List<Action>> rtn = new LinkedList<>();
        List<ActionType> list = new LinkedList<>();
        for (ActionType type:available) {
            if (type == parentAction) {
                continue;
            }
            list.add(type);
        }
        while (list.size() != 0) {
            ActionType type = ((LinkedList<ActionType>) list).getFirst();
            switch (type) {
                case CHANGE_CAR:
                    rtn.addAll(buildActionList(chain, type));
                    break;
                case CHANGE_DRIVER:
                    rtn.addAll(buildActionList(chain, type));
                    break;
                case CHANGE_TIRES:
                    rtn.addAll(buildActionList(chain, type));
                    break;
                case CHANGE_PRESSURE:
                    rtn.addAll(buildActionList(chain, type));
                    break;
            }
            List<ActionType> childChain = new LinkedList<>();
            childChain.addAll(chain);
            ((LinkedList<ActionType>) list).removeFirst();
            rtn.addAll(subActions(list, type, ps, childChain));
        }
        return rtn;
    }

    private List<List<Action>> buildActionList (List<ActionType> chain, ActionType currentAction) {
        List<ActionType> actionList = new LinkedList<>();
        for (ActionType type:chain) {
            actionList.add(type);
        }
        actionList.add(currentAction);
        //TODO action parameters
        return null;
    }
}
