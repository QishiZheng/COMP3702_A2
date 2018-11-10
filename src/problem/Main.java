package problem;

import simulator.Simulator;
import simulator.State;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        //the start time of the program
        long startTime = System.currentTimeMillis();
        //the memory before running
        long beforeUsedMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();

        String inputFile = args[0];
        String outputFile = args[1];
        System.out.println(inputFile + "\n");
        ProblemSpec ps = new ProblemSpec(inputFile);
        simulator.Simulator sim = new Simulator(ps, outputFile);
        DecisionMaker dm = new DecisionMaker(ps);
        State state = sim.reset();
        Action action;
        while (state != null) {
            action = dm.getAction(state);
            //System.out.println(action.getActionType());
            state = sim.step(action);
            if (sim.isGoalState(state)) {
                System.out.println("GOOOOOOAAAAAL!!\n");
                break;
            }
        }
        if (state == null) {
            System.out.println("Failure :(\n");
        }

        //elapsed time
        long endTime = System.currentTimeMillis();
        long timeElapsed = endTime - startTime;
        System.out.println("Execution time in milliseconds: " + timeElapsed + "\n");

        //memory used
        long afterUsedMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        long actualMemUsed=afterUsedMem-beforeUsedMem;
        System.out.println("Memory Used: " + actualMemUsed/1000 + "KB");
    }
}
