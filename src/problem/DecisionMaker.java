package problem;

import simulator.Simulator;
import simulator.State;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static problem.ProblemSpec.CAR_MIN_MOVE;
import static problem.ProblemSpec.CAR_MOVE_RANGE;

public class DecisionMaker {

    private Queue<Action> actionSequence;
    //might not need ps in DecisionMaker
    //private ProblemSpec ps;
    private List<TirePressure> pressures;

    //might not need ps in DecisionMaker
    //Construct a DecisionMaker with an input txt file
//    public DecisionMaker(String fileName) {
//        try {
//            this.ps = new ProblemSpec(fileName);
//            this.actionSequence = new LinkedBlockingQueue<>();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public DecisionMaker() {
        actionSequence = new LinkedBlockingQueue<>();
        pressures = new LinkedList<>();
        pressures.add(TirePressure.FIFTY_PERCENT);
        pressures.add(TirePressure.SEVENTY_FIVE_PERCENT);
        pressures.add(TirePressure.ONE_HUNDRED_PERCENT);
    }

    public Action getAction() {
        if (actionSequence.isEmpty()) {
            getSequence();
        }
        return actionSequence.poll();
    }

    private void getSequence() {
        //TODO implementation
        this.actionSequence = null;
    }

    /**
     * Get all possible combinations of actions in this level
     *
     * @return List of action sequence
     */
    public List<List<Action>> getAllAction() {
        List<List<Action>> rtn = new LinkedList<>();
        List<ActionType> actions = new LinkedList<>();
        actions.add(ActionType.CHANGE_CAR);
        actions.add(ActionType.CHANGE_DRIVER);
        if (ps.getLevel().getLevelNumber() > 1) {
            actions.add(ActionType.CHANGE_TIRES);
        }
        actions.add(ActionType.CHANGE_PRESSURE);
        LinkedList<Action> move = new LinkedList<>();
        move.add(new Action(ActionType.MOVE));
        rtn.add(move);
        while (actions.size() != 0) {
            List<ActionType> chain = new LinkedList<>();
            rtn.addAll(buildActionTypeList(chain, ((LinkedList<ActionType>) actions).getFirst(), ps));
            rtn.addAll(subActions(actions, ((LinkedList<ActionType>) actions).getFirst(), ps, chain));
            ((LinkedList<ActionType>) actions).removeFirst();
        }
        return rtn;
    }

    /**
     * based on given state and discount factor, find out best reward actions
     *
     * @param state
     * @param discount
     * @return
     */
    private List<Action> findBestActions(State state, float discount) {
        List<List<Action>> actionSequences = this.getAllAction();
        float values[] = new float[actionSequences.size()];
        float maxValue = 0f;
        int maxIndex = 0;
        for(int i = 0; i < actionSequences.size(); i++) {
            values[i] = getValue(state, actionSequences.get(i), discount);
            if(maxValue < values[i]) {
                maxIndex = i;
                maxValue = values[i];
            }
        }

        return actionSequences.get(maxIndex);
    }

    /**
     * return the value of given state among all actions
     *
     * Value = R(s) + discount * (Σ T(s, a, s')V(s'))
     * @param state
     * @return value of the sequence actions
     */
    private float getValue(State state, List<Action> actions, float discount) {
        float value = getReward(state);
        State tempState;
        for(int i = 0; i < actions.size() - 1; i++) {
            tempState = act(state, actions.get(i));
            value += Math.pow(discount, i + 1) * (getReward(tempState));
        }
        return value;
    }

    /**
     * perform given action(except action A1) and return a new State
     *
     * @param state current state
     * @param action given action
     * @return state after performing the action
     */
    private State act(State state, Action action) {
        State nextState = state.copyState();
        switch(action.getActionType().getActionNo()) {
            case 1:
                break;
            case 2:
                nextState = actA2(state, action);
                break;
            case 3:
                nextState = actA3(state, action);
                break;
            case 4:
                nextState = actA4(state, action);
                break;
            case 5:
                nextState = actA5(state, action);
                break;
            case 6:
                nextState = actA6(state, action);
                break;
            case 7:
                nextState = actA7(state, action);
                break;
            default:
                nextState = actA8(state, action);
        }
        return nextState;
    }

    /**
     * The reward will based on current state and will foc on
     * the distance that each action can take
     * R(s) = (Σprobs[i] * stepDistance) - (slipTime / totalTime) * slipTime
     *          - (breakTime / totalTime) * breakTime;
     * @return current reward
     */
    private float getReward(State state) {
        List<Double> probs = getProbs(state);
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
                    reward -=  (slipTime / totalTime) * probs.get(i);
                } else if (CAR_MOVE_RANGE + i == 7) {
                    reward -= (breakTime / totalTime) * probs.get(i);
                } else {
                    reward += (CAR_MIN_MOVE + i) * probs.get(i);
                }
            }
        }
        return reward;
    }

    /**
     * Get all probs that moving at the currentState might cause
     * @param currentState the current state of the car is at
     * @param ps the current problmeSpec loaded
     * @return a list of Double,
     *         represent the probability of moving from -4 to 5, plus slip and breakdown
     */
    public List<Double> getProbs(State currentState, ProblemSpec ps) {
        List<Double> probs = new LinkedList<>();

        // get parameters of current state
        Terrain terrain = ps.getEnvironmentMap()[currentState.getPos() - 1];
        int terrainIndex = ps.getTerrainIndex(terrain);
        String car = currentState.getCarType();
        String driver = currentState.getDriver();
        Tire tire = currentState.getTireModel();

        // calculate priors
        double priorK = 1.0 / ProblemSpec.CAR_MOVE_RANGE;
        double priorCar = 1.0 / ps.getCT();
        double priorDriver = 1.0 / ps.getDT();
        double priorTire = 1.0 / ProblemSpec.NUM_TYRE_MODELS;
        double priorTerrain = 1.0 / ps.getNT();
        double priorPressure = 1.0 / ProblemSpec.TIRE_PRESSURE_LEVELS;

        // get probabilities of k given parameter
        double[] pKGivenCar = ps.getCarMoveProbability().get(car);
        double[] pKGivenDriver = ps.getDriverMoveProbability().get(driver);
        double[] pKGivenTire = ps.getTireModelMoveProbability().get(tire);
        double pSlipGivenTerrain = ps.getSlipProbability()[terrainIndex];
        double[] pKGivenPressureTerrain = convertSlipProbs(pSlipGivenTerrain, currentState.getTirePressure(), ps);

        // use bayes rule to get probability of parameter given k
        double[] pCarGivenK = bayesRule(pKGivenCar, priorCar, priorK);
        double[] pDriverGivenK = bayesRule(pKGivenDriver, priorDriver, priorK);
        double[] pTireGivenK = bayesRule(pKGivenTire, priorTire, priorK);
        double[] pPressureTerrainGivenK = bayesRule(pKGivenPressureTerrain,
                (priorTerrain * priorPressure), priorK);

        // use conditional probability formula on assignment sheet to get what
        // we want (but what is it that we want....)
        double[] kProbs = new double[ProblemSpec.CAR_MOVE_RANGE];
        double kProbsSum = 0;
        double kProb;
        for (int k = 0; k < ProblemSpec.CAR_MOVE_RANGE; k++) {
            kProb = magicFormula(pCarGivenK[k], pDriverGivenK[k],
                    pTireGivenK[k], pPressureTerrainGivenK[k], priorK);
            kProbsSum += kProb;
            kProbs[k] = kProb;
        }

        // Normalize
        for (int k = 0; k < ProblemSpec.CAR_MOVE_RANGE; k++) {
            kProbs[k] /= kProbsSum;
        }

        //convert to List
        for(int i = 0; i < kProbs.length; ++i) {
            probs.add(kProbs[i]);
        }

        return probs;

    }

    /**
     * Convert the probability of slipping on a given terrain with 50% tire
     * pressure into a probability list, of move distance versus current
     * terrain and tire pressure.
     *
     * @param slipProb probability of slipping on current terrain and 50%
     *                 tire pressure
     * @param pressure the tire pressure at current state
     * @param ps the current problmeSpec loaded
     * @return list of move probabilities given current terrain and pressure
     */
    private double[] convertSlipProbs(double slipProb, TirePressure pressure, ProblemSpec ps) {

        // Adjust slip probability based on tire pressure
        //TirePressure pressure = currentState.getTirePressure();
        if (pressure == TirePressure.SEVENTY_FIVE_PERCENT) {
            slipProb *= 2;
        } else if (pressure == TirePressure.ONE_HUNDRED_PERCENT) {
            slipProb *= 3;
        }
        // Make sure new probability is not above max
        if (slipProb > ProblemSpec.MAX_SLIP_PROBABILITY) {
            slipProb = ProblemSpec.MAX_SLIP_PROBABILITY;
        }

        // for each terrain, all other action probabilities are uniform over
        // remaining probability
        double[] kProbs = new double[ProblemSpec.CAR_MOVE_RANGE];
        double leftOver = 1 - slipProb;
        double otherProb = leftOver / (ProblemSpec.CAR_MOVE_RANGE - 1);
        for (int i = 0; i < ProblemSpec.CAR_MOVE_RANGE; i++) {
            if (i == ps.getIndexOfMove(ProblemSpec.SLIP)) {
                kProbs[i] = slipProb;
            } else {
                kProbs[i] = otherProb;
            }
        }

        return kProbs;
    }


    /**
     * Apply bayes rule to all values in cond probs list.
     * This is from support code.
     *
     * @param condProb list of P(B|A)
     * @param priorA prior probability of parameter A
     * @param priorB prior probability of parameter B
     * @return list of P(A|B)
     */
    private double[] bayesRule(double[] condProb, double priorA, double priorB) {

        double[] swappedProb = new double[condProb.length];

        for (int i = 0; i < condProb.length; i++) {
            swappedProb[i] = (condProb[i] * priorA) / priorB;
        }
        return swappedProb;
    }


    /**
     * Conditional probability formula from assignment 2 sheet
     *
     * @param pA P(A | E)
     * @param pB P(B | E)
     * @param pC P(C | E)
     * @param pD P(D | E)
     * @param priorE P(E)
     * @return numerator of the P(E | A, B, C, D) formula (still need to divide
     *      by sum over E)
     */
    private double magicFormula(double pA, double pB, double pC, double pD,
                                double priorE) {
        return pA * pB * pC * pD * priorE;
    }

    /**
     * All possible actions may take after the previous action
     *
     * @param available List all available actions
     * @param parentAction ActionType its previous action
     * @param ps ProblemSpec instance
     * @param chain List previous taken actions
     * @return List of action sequence
     */
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
            ((LinkedList<ActionType>) list).removeFirst();
            rtn.addAll(buildActionTypeList(chain, type, ps));
            List<ActionType> childChain = new LinkedList<>();
            childChain.addAll(chain);
            rtn.addAll(subActions(list, type, ps, childChain));
        }
        return rtn;
    }

    /**
     * Hard copy elements in chain into a new List
     *
     * @param chain List where the actions are
     * @param currentAction ActionType current action
     * @param ps ProblemSpec instance
     * @return List of action sequence
     */
    private List<List<Action>> buildActionTypeList (List<ActionType> chain, ActionType currentAction, ProblemSpec ps) {
        List<ActionType> actionTypes = new LinkedList<>();
        LinkedList<Action> actionList = new LinkedList<>();
        List<List<Action>> collector = new LinkedList<>();
        for (ActionType type:chain) {
            actionTypes.add(type);
        }
        actionTypes.add(currentAction);
        iterateParams(actionTypes, actionList, ps, collector);
        return collector;
    }

    /**
     * All possible combinations of actions with its parameters
     *
     * @param actionTypes List sequence of action types
     * @param actionList List sequence of actions
     * @param ps ProblemSpec instance
     * @param collector List result sequence of actions
     */
    private void iterateParams(List<ActionType> actionTypes, LinkedList<Action> actionList, ProblemSpec ps, List<List<Action>> collector) {
        ActionType type = actionTypes.get(0);
        Action action;
        switch (type) {
            case CHANGE_CAR:
                for (String brand: ps.getCarOrder()) {
                    action = new Action(type, brand);
                    actionList.add(action);
                    checkIfEnd(actionTypes, actionList, collector, ps);
                    actionList.removeLast();
                }
                break;
            case CHANGE_DRIVER:
                for (String driver: ps.getDriverOrder()) {
                    action = new Action(type, driver);
                    actionList.add(action);
                    checkIfEnd(actionTypes, actionList, collector, ps);
                    actionList.removeLast();
                }
                break;
            case CHANGE_TIRES:
                for (Tire tire: ps.getTireOrder()) {
                    action = new Action(type, tire);
                    actionList.add(action);
                    checkIfEnd(actionTypes, actionList, collector, ps);
                    actionList.removeLast();
                }
                break;
            case CHANGE_PRESSURE:
                for (TirePressure pressure: this.pressures) {
                    action = new Action(type, pressure);
                    actionList.add(action);
                    checkIfEnd(actionTypes, actionList, collector, ps);
                    actionList.removeLast();
                }
                break;
        }
    }

    /**
     * iterateParams Help method
     * Check if it reaches the end of iterateParams recursion
     *
     * @param actionTypes List sequence of action types
     * @param actionList List sequence of actions
     * @param collector List result sequence of actions
     * @param ps ProblemSpec instance
     */
    private void checkIfEnd(List<ActionType> actionTypes, LinkedList<Action> actionList, List<List<Action>> collector, ProblemSpec ps) {
        if (actionTypes.size() == 1) {
            List<Action> result = new LinkedList<>();
            result.addAll(actionList);
            result.add(new Action(ActionType.MOVE));
            collector.add(result);
        } else {
            iterateParams(actionTypes.subList(1, actionTypes.size()), actionList, ps, collector);
        }
    }

    /**
     * Perform CHANGE_CAR action
     *
     * @param a a CHANGE_CAR action object
     * @return the next state
     */
    private State actA2(State state, Action a) {
        State currentState = state.copyState();
        if (currentState.getCarType().equals(a.getCarType())) {
            // changing to same car type does not change state but still costs a step
            // no cheap refill here, muhahaha
            return currentState;
        }

        return currentState.changeCarType(a.getCarType());
    }

    /**
     * Perform CHANGE_DRIVER action
     *
     * @param a a CHANGE_DRIVER action object
     * @return the next state
     */
    private State actA3(State state, Action a) {
        State currentState = state.copyState();
        return currentState.changeDriver(a.getDriverType()); }

    /**
     * Perform the CHANGE_TIRES action
     *
     * @param a a CHANGE_TIRES action object
     * @return the next state
     */
    private State actA4(State state, Action a) {
        State currentState = state.copyState();
        return currentState.changeTires(a.getTireModel());
    }

    /**
     * Perform the ADD_FUEL action
     *
     * @param a a ADD_FUEL action object
     * @return the next state
     */
    private State actA5(State state, Action a) {
        // calculate number of steps used for refueling (minus 1 since we add
        // 1 in main function
        State currentState = state.copyState();
        int stepsRequired = (int) Math.ceil(a.getFuel() / (float) 10);
        //steps += (stepsRequired - 1);
        return currentState.addFuel(a.getFuel());
    }

    /**
     * Perform the CHANGE_PRESSURE action
     *
     * @param a a CHANGE_PRESSURE action object
     * @return the next state
     */
    private State actA6(State state, Action a) {
        State currentState = state.copyState();
        return currentState.changeTirePressure(a.getTirePressure());
    }

    /**
     * Perform the CHANGE_CAR_AND_DRIVER action
     *
     * @param a a CHANGE_CAR_AND_DRIVER action object
     * @return the next state
     */
    private State actA7(State state, Action a) {
        State currentState = state.copyState();
        if (currentState.getCarType().equals(a.getCarType())) {
            // if car the same, only change driver so no sneaky fuel exploit
            return currentState.changeDriver(a.getDriverType());
        }
        return currentState.changeCarAndDriver(a.getCarType(),
                a.getDriverType());
    }

    /**
     * Perform the CHANGE_TIRE_FUEL_PRESSURE action
     *
     * @param a a CHANGE_TIRE_FUEL_PRESSURE action object
     * @return the next state
     */
    private State actA8(State state, Action a) {
        State currentState = state.copyState();
        return currentState.changeTireFuelAndTirePressure(a.getTireModel(),
                a.getFuel(), a.getTirePressure());
    }

}
