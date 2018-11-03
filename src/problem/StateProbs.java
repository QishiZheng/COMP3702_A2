package problem;

import simulator.State;

public class StateProbs {
    private State state;
    private double probability;

    public StateProbs(State state, double probability) {
        this.state = state;
        this.probability = probability;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }
}
