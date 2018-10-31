import org.junit.Test;
import problem.DecisionMaker;
import problem.ProblemSpec;
import simulator.Simulator;
import simulator.State;

import static org.junit.Assert.*;

public class DecisionMakerTest {
    @Test
    public void findBestActions() throws Exception {
        int level = 1;
        DecisionMaker decisionMaker = new DecisionMaker("examples/level_" + level + "/input_lvl" + level + ".txt");
        ProblemSpec ps = new ProblemSpec("examples/level_" + level + "/input_lvl" + level + ".txt");
        State state = State.getStartState(ps.getFirstCarType(),
                ps.getFirstDriver(), ps.getFirstTireModel());
        decisionMaker.findBestActions(state, 0.95f);
    }

}