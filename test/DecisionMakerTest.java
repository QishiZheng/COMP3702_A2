import org.junit.Test;
import problem.Action;
import problem.DecisionMaker;
import problem.ProblemSpec;
import simulator.Simulator;
import simulator.State;

import java.util.List;

import static org.junit.Assert.*;

public class DecisionMakerTest {
    @Test
    public void findBestActions() throws Exception {
        int level = 1;
        ProblemSpec ps = new ProblemSpec("examples/level_" + level + "/input_lvl" + level + ".txt");
        DecisionMaker decisionMaker = new DecisionMaker(ps);

        State state = State.getStartState(ps.getFirstCarType(),
                ps.getFirstDriver(), ps.getFirstTireModel());
        List<Action> bestActions = decisionMaker.findBestActions(state, 0.8f);
        for(int i = 0; i < bestActions.size(); i++) {
            System.out.println("Action " + i +": "+ bestActions.get(i).getActionType());
        }
    }

}