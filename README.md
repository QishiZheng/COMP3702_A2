# COMP3702_A2
2018 Semester 1 COMP3702 Assignment 2

# COMP3702/7702 Assignment 2 Supporting code

Here is some code to help you get across the line

### What's included

**Problem Package**

The main problem class and parser.

To use it simply create a new ProblemSpec object and pass in the path to the input file of your choosing as an argument.

```$xslt
ProblemSpec ps = new ProblemSpec("path/to/inputFile.txt");
```

Enjoy the problem instance object

**Simulator package**

The simulator for you to test your policies and for use when we assess your implementation.
The simulator class will **automatically output the steps taken to the output file** provided when a terminal state is reached (i.e. the goal is reached or max timesteps is reached).

Initialize the simulator by either:
 1. passing your problem spec along with output file path to its constructor
 2. passing the input file path along with output file path to its constructor 

```$xslt
ProblemSpec ps = new ProblemSpec("path/to/inputFile.txt");
Simulator sim = new Simulator(ps, "path/to/outputFile.txt");
// or 
Simulator sim = new Simulator("path/to/inputFile.txt", "path/to/outputFile.txt");
```

To utilize the simulator, simply call the step function which accepts an
Action class instance and returns the next state.
You can also call the getSteps method to get the current number of steps for the simulation.

```$xslt
Action a = new Action(ActionType.MOVE)    // or some other action
State nextState = sim.step(a);
int stepsTakenSoFar = sim.getSteps();
```

Check out the SimulatorTest for an example of using random actions.


### Changing the files

Don't do it.. For you assessment we will get you to run your program using files we provide (i.e. you'll have to
replace the supporting code classes with clean ones we provide on the day). If you really want to make use of or edit
the supporting code in some way other than calling the step and getSteps functions, then feel free to copy the code
into your own classes.

### Assessment

For your assessment, we expect your implementation to take the given input, generate a policy however you wish, and then before the given time limits
call the simulator.Step function with your chosen action and then use the returned next state to inform your next action. Once
a terminal state is reached the simulator will output the results of the run to the provided output file.

### Test Cases

*26/10*: We have provided some example inputs and generated output for each level so you have an idea of the input format and 
can at least test your implementations are functionin correctly. We definitely encourage you to edit the examples to come up with
your own input cases to test on. 