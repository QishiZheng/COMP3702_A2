package problem;

import simulator.State;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static problem.ProblemSpec.CAR_MIN_MOVE;
import static problem.ProblemSpec.CAR_MOVE_RANGE;

public class DecisionMaker {
    private LinkedList<Action> actionSequence;
    private ProblemSpec ps;
    private List<TirePressure> pressures;

    public DecisionMaker(ProblemSpec ps) {
        this.ps = ps;
        actionSequence = new LinkedList<>();
        pressures = new LinkedList<>();
        pressures.add(TirePressure.FIFTY_PERCENT);
        pressures.add(TirePressure.SEVENTY_FIVE_PERCENT);
        pressures.add(TirePressure.ONE_HUNDRED_PERCENT);
    }

    /**
     * Get action based on current position
     *
     * @param state State instance
     * @return Action taken
     */
    public Action getAction(State state) {
        if (actionSequence.isEmpty()) {
            actionSequence = (LinkedList<Action>) findBestActions(state);
        }
        return actionSequence.poll();
    }

    /**
     * Get all possible combinations of actions in this level
     *
     * @return List of action sequence
     */
    public List<List<Action>> getAllAction(State state) {
        List<List<Action>> rtn = new LinkedList<>();
        List<ActionType> actions = new LinkedList<>();
        actions.add(ActionType.CHANGE_CAR);
        actions.add(ActionType.CHANGE_DRIVER);
        actions.add(ActionType.CHANGE_TIRES);
        if (ps.getLevel().getLevelNumber() > 1) {
            actions.add(ActionType.CHANGE_PRESSURE);
        }
        LinkedList<Action> move = new LinkedList<>();
        move.add(new Action(ActionType.MOVE));
        rtn.add(move);
        while (actions.size() != 0) {
            List<ActionType> chain = new LinkedList<>();
            rtn.addAll(buildActionTypeList(chain, ((LinkedList<ActionType>) actions).getFirst(), ps, state));
            rtn.addAll(subActions(actions, ((LinkedList<ActionType>) actions).getFirst(), ps, state, chain));
            ((LinkedList<ActionType>) actions).removeFirst();
        }
        return rtn;
    }

    /**
     * based on given state and discount factor, find out best reward actions
     *
     * @param state
     * @return
     */
    public List<Action> findBestActions(State state) {
        //out of gas, we change car type
        if(outOfGas(state)) {
            Action newAction = new Action(ActionType.CHANGE_CAR, randomCarType(state));
            List<Action> newActions = new LinkedList<>();
            newActions.add(newAction);
            return newActions;
        }

        List<List<Action>> actionSequences = this.getAllAction(state);
        float values[] = new float[actionSequences.size()];
        float maxValue = getValue(state, actionSequences.get(0));
        int maxIndex = 0;
        for(int i = 1; i < actionSequences.size(); i++) {
            values[i] = getValue(state, actionSequences.get(i));
            if(maxValue < values[i]) {
                maxIndex = i;
                maxValue = values[i];
            }
        }
        return actionSequences.get(maxIndex);
    }

//    public List<Action> findBestActionsDepth(State state, int depth) {
//        List<List<Action>> actionSequences = getAllAction(state);
//        //loop through all actions
//        for(int i = 0; i < actionSequence.size(); i++) {
//            List<StateProbs> subStatesProbs = getSubState(state, actionSequences.get(i));
//            //for loop through all 12 different index [-4, 5, slip, breakdown]
//            for(int j = 0; j < CAR_MOVE_RANGE; j++) {
//                State tempState = subStatesProbs.get(j).getState().copyState();
//                int tempDepth = depth - 1;
//                tempState.changePosition(i + CAR_MIN_MOVE, ps.getN());
//                findBestActionsDepth(tempState, tempDepth);
//                depth--;
//            }
//        }
//        if(depth == 1) {
//            findBestActions(state);
//        }
//
//        return null;
//    }
    
    public List<Action> prophetSearch(State state, int depth) {
        List<List<Action>> actionSequences = getAllAction(state);
        List<Action> rtn = null;
        double max = -100;
        for (List<Action> actions: actionSequences) {
            List<StateProbs> subStates = getSubState(state, actions);

//            for (int i = 0; i < subStates.size(); i++) {
//                System.out.println(subStates.get(i));
//            }


            double tmp = 0;
            for (StateProbs subState: subStates) {
                tmp += subState.getProbability() * goDeep(subState.getState(), depth);
            }
            if (tmp > max) {
                rtn = actions;
                max = tmp;
            }
        }
        return rtn;
    }

    private double findBestValue(State state) {
        List<List<Action>> actionSequences = this.getAllAction(state);
        float values[] = new float[actionSequences.size()];
        float maxValue = getValue(state, actionSequences.get(0));
        for(int i = 1; i < actionSequences.size(); i++) {
            values[i] = getValue(state, actionSequences.get(i));
            if(maxValue < values[i]) {
                maxValue = values[i];
            }
        }
        return maxValue;
    }

    private double goDeep(State state, int depth) {
        List<List<Action>> actionSequences = getAllAction(state);
        double max = -100;
        for (List<Action> actions: actionSequences) {
            List<StateProbs> subStates = getSubState(state, actions);
            if (depth == 0) {
                double tmp = atEndState(subStates);
                if (tmp > max) {
                    max = tmp;
                }
            } else {
                double sum = 0;
                for (StateProbs subState: subStates) {
                    sum += subState.getProbability() * goDeep(subState.getState(), depth - 1);
                }
                if (sum > max) {
                    max = sum;
                }
            }
        }
        return max;
    }

    private double atEndState(List<StateProbs> subStates) {
        double sum = 0;
        for (StateProbs subState: subStates) {
            sum += subState.getProbability() * findBestValue(subState.getState());
        }
        return sum;
    }

    /**
     * get all substates of the current state after performing a series of given actions
     * @param state current state
     * @param actions given actions to perform
     * @return a list of sub state and probs pairs
     */
    public List<StateProbs> getSubState(State state, List<Action> actions) {
        List<StateProbs> subStatesProbs = new ArrayList<>();
        State tempState = state.copyState();
        if(actions.size() == 1) {
            List<Double> probs= getProbs(tempState);
            for(int i = 0; i < CAR_MOVE_RANGE; i++) {
                subStatesProbs.add(new StateProbs(tempState, probs.get(i)));
            }
        }
        for(int i = 0; i < actions.size() - 1; i++) {
            tempState = act(tempState, actions.get(i));
            List<Double> probs= getProbs(tempState);
            for(int j = 0; j < CAR_MOVE_RANGE; j++) {
                tempState.changePosition(j + CAR_MIN_MOVE, ps.getN());
                subStatesProbs.add(new StateProbs(tempState, probs.get(j)));
            }
        }

        return subStatesProbs;
    }

    /**
     * return the value of given state among all actions
     *
     * Value = R(s) + discount * (Σ T(s, a, s')V(s'))
     * @param state
     * @return value of the sequence actions
     */
    private float getValue(State state, List<Action> actions) {
        float value;
        State tempState = state.copyState();
        //average step distance for each time step
        float averageStep = ps.getN()/ps.getMaxT();
        for(int i = 0; i < actions.size() - 1; i++) {
            tempState = act(tempState, actions.get(i));
            //value += Math.pow(discount, i + 1) * (getReward(tempState));
        }
        value = getReward(tempState) - actions.size() * averageStep;
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
     * R(s) = (Σprobs[i] * stepDistance) - averageStep * (slipTime / totalTime) * slipTime
     *          - averageStep * (breakTime / totalTime) * breakTime;
     * @return current reward
     */
    private float getReward(State state) {
        List<Double> probs = getProbs(state);
        //average step distance for each time step
        float averageStep = ps.getN()/ps.getMaxT();
        float reward = 0f;
        int breakTime = ps.getRepairTime();
        int slipTime = ps.getSlipRecoveryTime();
        int totalTime = ps.getMaxT();
        if(state.isInBreakdownCondition()) {
            //breakdown
            reward = reward -  (float)((breakTime / totalTime) * probs.get(10));
        } else if(state.isInSlipCondition()) {
            //slip
            reward = reward - (float)((slipTime / totalTime) * probs.get(11));
        } else {
            //good condition
            for(int i = 0; i < CAR_MOVE_RANGE; i++) {
                //slip or breakdown
                if(i == 10) {
                    reward = reward - averageStep * (float)(slipTime * ((float)slipTime / totalTime) * probs.get(i));
                } else if (i == 11) {
                    reward = reward -  averageStep * (float)(breakTime * ((float)breakTime / totalTime) * probs.get(i));
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
     * @return a list of Double,
     *         represent the probability of moving from -4 to 5, plus slip and breakdown
     */
    public List<Double> getProbs(State currentState) {
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
    private List<List<Action>> subActions(List<ActionType> available, ActionType parentAction, ProblemSpec ps, State state, List<ActionType> chain) {
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
            rtn.addAll(buildActionTypeList(chain, type, ps, state));
            List<ActionType> childChain = new LinkedList<>();
            childChain.addAll(chain);
            rtn.addAll(subActions(list, type, ps, state, childChain));
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
    private List<List<Action>> buildActionTypeList (List<ActionType> chain, ActionType currentAction, ProblemSpec ps, State state) {
        List<ActionType> actionTypes = new LinkedList<>();
        LinkedList<Action> actionList = new LinkedList<>();
        List<List<Action>> collector = new LinkedList<>();
        for (ActionType type:chain) {
            actionTypes.add(type);
        }
        actionTypes.add(currentAction);
        iterateParams(actionTypes, actionList, ps, state, collector);
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
    private void iterateParams(List<ActionType> actionTypes, LinkedList<Action> actionList, ProblemSpec ps, State state, List<List<Action>> collector) {
        ActionType type = actionTypes.get(0);
        Action action;
        switch (type) {
            case CHANGE_CAR:
                for (String brand: ps.getCarOrder()) {
                    if (state.getCarType().equals(brand)) {
                        continue;
                    }
                    action = new Action(type, brand);
                    actionList.add(action);
                    checkIfEnd(actionTypes, actionList, collector, ps, state);
                    actionList.removeLast();
                }
                break;
            case CHANGE_DRIVER:
                for (String driver: ps.getDriverOrder()) {
                    if (state.getDriver().equals(driver)) {
                        continue;
                    }
                    action = new Action(type, driver);
                    actionList.add(action);
                    checkIfEnd(actionTypes, actionList, collector, ps, state);
                    actionList.removeLast();
                }
                break;
            case CHANGE_TIRES:
                for (Tire tire: ps.getTireOrder()) {
                    if (state.getTireModel().asString().equals(tire.asString())) {
                        continue;
                    }
                    action = new Action(type, tire);
                    actionList.add(action);
                    checkIfEnd(actionTypes, actionList, collector, ps, state);
                    actionList.removeLast();
                }
                break;
            case CHANGE_PRESSURE:
                for (TirePressure pressure: this.pressures) {
                    if (pressure.asString().equals(state.getTirePressure().asString())) {
                        continue;
                    }
                    action = new Action(type, pressure);
                    actionList.add(action);
                    checkIfEnd(actionTypes, actionList, collector, ps, state);
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
    private void checkIfEnd(List<ActionType> actionTypes, LinkedList<Action> actionList, List<List<Action>> collector, ProblemSpec ps, State state) {
        if (actionTypes.size() == 1) {
            List<Action> result = new LinkedList<>();
            result.addAll(actionList);
            result.add(new Action(ActionType.MOVE));
            collector.add(result);
        } else {
            iterateParams(actionTypes.subList(1, actionTypes.size()), actionList, ps, state, collector);
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

    /**
     * helper method that check whether current gas in tank is enough for next move
     *
     * @param state
     * @return
     */
    private boolean outOfGas(State state) {
        int consumption = getFuelConsumption(state);
        //out of oil
        if(state.getFuel() <= consumption) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Get the fuel consumption of moving given the current state
     *
     * @return move fuel consumption for current state
     */
    private int getFuelConsumption(State state) {
        // get parameters of current state
        Terrain terrain = ps.getEnvironmentMap()[state.getPos() - 1];
        String car = state.getCarType();
        TirePressure pressure = state.getTirePressure();

        // get fuel consumption
        int terrainIndex = ps.getTerrainIndex(terrain);
        int carIndex = ps.getCarIndex(car);
        int fuelConsumption = ps.getFuelUsage()[terrainIndex][carIndex];

        if (pressure == TirePressure.FIFTY_PERCENT) {
            fuelConsumption *= 3;
        } else if (pressure == TirePressure.SEVENTY_FIVE_PERCENT) {
            fuelConsumption *= 2;
        }
        return fuelConsumption;
    }


    private String randomCarType(State state) {
        List<String> carTypes = ps.getCarOrder();
        for(int i = 0; i < carTypes.size(); i++) {
            if(!carTypes.get(i).equals(state.getCarType())) {
                return carTypes.get(i);
            }
        }
        return state.getCarType();
    }

}
