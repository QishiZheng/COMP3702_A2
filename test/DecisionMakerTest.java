import org.junit.Test;
import problem.Action;
import problem.DecisionMaker;
import problem.ProblemSpec;
import simulator.Simulator;
import simulator.State;

import java.io.IOException;
import java.util.List;


public class DecisionMakerTest {
    @Test
    public void findBestActions() throws Exception {
        int level = 1;
        ProblemSpec ps = new ProblemSpec("examples/level_" + level + "/input_lvl" + level + ".txt");
        DecisionMaker decisionMaker = new DecisionMaker(ps);

        State state = State.getStartState(ps.getFirstCarType(),
                ps.getFirstDriver(), ps.getFirstTireModel());
        List<Action> bestActions = decisionMaker.findBestActions(state, 0.1f);
        for(int i = 0; i < bestActions.size(); i++) {
            System.out.println("Action " + i +": "+ bestActions.get(i).getActionType());
        }
    }

    @Test
    public void completeTest1() throws IOException {
        int level = 1;
        String inputFile = "examples/level_" + level + "/input_lvl" + level + ".txt";
        String outputFile = "examples/level_" + level + "/test_output_lvl" + level + ".txt";;
        ProblemSpec ps = new ProblemSpec(inputFile);
        Simulator sim = new Simulator(ps, outputFile);
        DecisionMaker dm = new DecisionMaker(ps);
        State state = sim.reset();
        Action action;
        while (state != null) {
            action = dm.getAction(state);
            System.out.println(action.getActionType());
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

}