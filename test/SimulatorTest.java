
import org.junit.Test;
import problem.*;
import simulator.Simulator;
import simulator.State;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Test class for the Simulator
 */
public class SimulatorTest {


    /**
     * A basic test of the Dynamic.optimalCostDynamic method
     */
    @Test
    public void randomActionTestLevel1() throws IOException {
        // using example file
        int level = 5;
        String inputFile = "examples/level_" + level + "/input_lvl" + level + ".txt";
        String outputFile = "examples/level_" + level + "/output_lvl" + level + ".txt";;
        ProblemSpec ps = new ProblemSpec(inputFile);
        Simulator sim = new Simulator(ps, outputFile);
        System.out.println(ps.toString());

        State state = sim.reset();
        Action action;

        while (state != null) {
            action = selectRandomAction(ps);
            state = sim.step(action);
            if (sim.isGoalState(state)) {
                System.out.println("GOOOOOOAAAAAL!!");
                break;
            }
        }

        if (state == null) {
            System.out.println("Failure :(");
        }
    }

    /** Select a random action
     *
     * @param ps the problem spec
     * @return a random valid action for problem spec
     */
    private Action selectRandomAction(ProblemSpec ps) {

        int fuel;
        String car, driver;
        Tire tire;
        TirePressure pressure;

        List<ActionType> validActionTypes = ps.getLevel().getAvailableActions();
        List<TirePressure> validPressures = new LinkedList<>();
        validPressures.add(TirePressure.FIFTY_PERCENT);
        validPressures.add(TirePressure.SEVENTY_FIVE_PERCENT);
        validPressures.add(TirePressure.ONE_HUNDRED_PERCENT);

        int numActions = validActionTypes.size();
        int CT = ps.getCT();
        int DT = ps.getDT();
        int TiT = ProblemSpec.NUM_TYRE_MODELS;
        int PressureT = ProblemSpec.TIRE_PRESSURE_LEVELS;

        ActionType actionType = validActionTypes.get(aRandomInt(0, numActions));
        Action action;

        switch(actionType.getActionNo()) {
            case 1:
                action = new Action(actionType);
                break;
            case 2:
                car = ps.getCarOrder().get(aRandomInt(0, CT));
                action = new Action(actionType, car);
                break;
            case 3:
                driver = ps.getDriverOrder().get(aRandomInt(0, DT));
                action = new Action(actionType, driver);
                break;
            case 4:
                tire = ps.getTireOrder().get(aRandomInt(0, TiT));
                action = new Action(actionType, tire);
                break;
            case 5:
                fuel = aRandomInt(ProblemSpec.FUEL_MIN, ProblemSpec.FUEL_MAX);
                action = new Action(actionType, fuel);
                break;
            case 6:
                pressure = validPressures.get(aRandomInt(0, PressureT));
                action = new Action(actionType, pressure);
                break;
            case 7:
                car = ps.getCarOrder().get(aRandomInt(0, CT));
                driver = ps.getDriverOrder().get(aRandomInt(0, DT));
                action = new Action(actionType, car, driver);
                break;
            default:
                tire = ps.getTireOrder().get(aRandomInt(0, TiT));
                fuel = aRandomInt(ProblemSpec.FUEL_MIN, ProblemSpec.FUEL_MAX);
                pressure = validPressures.get(aRandomInt(0, PressureT));
                action = new Action(actionType, tire, fuel, pressure);
        }

        return action;
    }

    /** I'm a helper method */
    private static int aRandomInt(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min)) + min;
    }

}
