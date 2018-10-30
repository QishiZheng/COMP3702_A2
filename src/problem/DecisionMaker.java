package problem;

import simulator.State;

import java.io.IOException;
import java.util.LinkedList;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static problem.ProblemSpec.CAR_MIN_MOVE;
import static problem.ProblemSpec.CAR_MOVE_RANGE;

public class DecisionMaker {

    private Queue<Action> ActionSequence;
    private ProblemSpec ps;

    //Construct a DecisionMaker with an input txt file
    public DecisionMaker(String fileName) {
        try {
            ps = new ProblemSpec(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void DecisionMaker() {
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
        double[] pKGivenPressureTerrain = convertSlipProbs(pSlipGivenTerrain, currentState.getTirePressure());

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
     *
     * @return list of move probabilities given current terrain and pressure
     */
    private double[] convertSlipProbs(double slipProb, TirePressure pressure) {

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
