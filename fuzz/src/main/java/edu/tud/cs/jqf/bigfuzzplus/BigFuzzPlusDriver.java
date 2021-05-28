package edu.tud.cs.jqf.bigfuzzplus;

//import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;

import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.tud.cs.jqf.bigfuzzplus.stackedMutation.HighOrderMutation;
import edu.tud.cs.jqf.bigfuzzplus.stackedMutation.MutationPair;
import edu.tud.cs.jqf.bigfuzzplus.stackedMutation.StackedMutation;
import edu.tud.cs.jqf.bigfuzzplus.stackedMutation.StackedMutationEnum;

import java.io.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class BigFuzzPlusDriver {
    // These booleans are for debugging purposes only, toggle them if you want to see the information
    public static boolean PRINT_METHOD_NAMES = false;
    public static boolean PRINT_MUTATION_DETAILS = false;
    public static boolean PRINT_ERRORS = false;
    public static boolean PRINT_MUTATIONS = false;
    public static boolean PRINT_TEST_RESULTS = false;

    public static StringBuilder program_configuration = new StringBuilder();
    public static StringBuilder iteration_results = new StringBuilder();
    public static StringBuilder summarized_results = new StringBuilder();

    /**
     * Run the BigFuzzPlus program with the following parameters:
     * [0] - test class
     * [1] - test method
     * [2] - mutation method
     * [3] - max Trials                (default = Long.MAXVALUE)
     * [4] - stacked mutation method   (default = 0)
     *          0 = Disabled
     *          1 = Permute_random (permute between 1 and the max amount of mutations)
     *          2 = Permute_max (Always permute until the max amount of mutations)
     *          3 = Smart_stack (Apply highorder mutation exclusion rules)
     *          4 = Single mutate (Only apply 1 mutation per column)
     * [5] - max mutation stack        (default = 2)
     *
     * @param args program arguments
     */
    public static void main(String[] args) {

        // LOAD PROGRAM ARGUMENTS
        if (args.length < 3) {
            System.err.println("Usage: java " + BigFuzzPlusDriver.class + " TEST_CLASS TEST_METHOD MUTATION_CLASS [MAX_TRIALS]");
            System.exit(1);
        }

        String testClassName = args[0];
        String testMethodName = args[1];
        String mutationMethodClassName = args[2];

        Long maxTrials = args.length > 3 ? Long.parseLong(args[3]) : Long.MAX_VALUE;

        int intStackedMutationMethod = args.length > 4 ? Integer.parseInt(args[4]) : 0;
        StackedMutationEnum.StackedMutationMethod stackedMutationMethod = StackedMutationEnum.intToStackedMutationMethod(intStackedMutationMethod);

        // This variable is used for the stackedMutationMethod: Smart_mutate
        // If the selected stackedMutationMethod is smart_mutate and this argument is not given, default is set to 2. If smart_mutate is not selected, set to 0
        int intMutationStackCount = args.length > 5 ? Integer.parseInt(args[5]) : stackedMutationMethod == StackedMutationEnum.StackedMutationMethod.Smart_stack ? 2 : 0;

        // **************

        long programStartTime = System.currentTimeMillis();
        File outputDir = new File("output/" + programStartTime);

        program_configuration.append("Program started with the following parameters: ");
        program_configuration.append("\n\tTest class: " + testClassName);
        program_configuration.append("\n\tTest method: " + testMethodName);
        program_configuration.append("\n\tMutation class: " + mutationMethodClassName);
        if(mutationMethodClassName.equals("StackedMutation")) {
            program_configuration.append("\n\tTest stackedMutation method: " + stackedMutationMethod);
            program_configuration.append("\n\tTest maximal stacked mutations: " + intMutationStackCount);
        }
        program_configuration.append("\nOutput directory is set to: " + outputDir);
        program_configuration.append("\nProgram is started at: " + programStartTime);

        boolean newOutputDirCreated = outputDir.mkdir();
        if (!newOutputDirCreated) {
            System.err.println("Something went wrong with making the output directory for this run");
            System.exit(0);
        }
        System.out.println(program_configuration);

        ArrayList<ArrayList<Integer>> uniqueFailureResults = new ArrayList();
        ArrayList<ArrayList<String>> inputs = new ArrayList();
        ArrayList<ArrayList<String>> methods = new ArrayList();
        ArrayList<ArrayList<String>> columns = new ArrayList();
        ArrayList<ArrayList<String>> mutationStacks = new ArrayList();

        ArrayList<Long> errorInputCount = new ArrayList();
        ArrayList<Long> validInputCount = new ArrayList();
        ArrayList<Long> durations = new ArrayList();
        for (int i = 0; i < 5; i++) {
            int atIteration = i + 1;
            System.out.println("******** START OF PROGRAM ITERATION: " + atIteration + "**********************");

            String file = "dataset/conf";
            try {
                long iterationStartTime = System.currentTimeMillis();

                Duration maxDuration = Duration.of(5, ChronoUnit.MINUTES);
                //NoGuidance guidance = new NoGuidance(file, maxTrials, System.err);
                String iterationOutputDir = outputDir + "/Test" + atIteration;
                BigFuzzPlusGuidance guidance = new BigFuzzPlusGuidance("Test" + atIteration, file, maxTrials, iterationStartTime, maxDuration, System.err, iterationOutputDir, mutationMethodClassName);

                // Set the provided input argument stackedMutationMethod in the guidance mutation
                if(guidance.mutation instanceof StackedMutation) {
                    ((StackedMutation)guidance.mutation).setStackedMutationMethod(stackedMutationMethod);
                    ((StackedMutation)guidance.mutation).setMutationStackCount(intMutationStackCount);
                }
                
                // Set the randomization seed to the program start time. Seed is passed to allow for custom seeds, independent of the program start time
                guidance.setRandomizationSeed(iterationStartTime);

                // Set the test class name in the guidance for the failure tracking
                guidance.setTestClassName(testClassName);

                // Run the Junit test
                GuidedFuzzing.run(testClassName, testMethodName, guidance, System.out);
                long endTime = System.currentTimeMillis();

                // Evaluate the results
                evaluation(testClassName, testMethodName, file, maxTrials, maxDuration, iterationStartTime, endTime, guidance, atIteration);
                writeToLists(guidance, maxTrials, inputs, uniqueFailureResults, methods, columns, mutationStacks, errorInputCount, validInputCount);
                durations.add(endTime - iterationStartTime);
                System.out.println("************************* END OF PROGRAM ITERATION ************************");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        summarizeProgramIterations(uniqueFailureResults, inputs, methods, columns, durations, mutationStacks, errorInputCount, validInputCount);
        writeLogToFile(outputDir);
    }

    /**
     * Write collected log in the variables log, summarized results and iteration results to a file in the output folder named log.txt.
     *
     * @param outputDir Directory where the log file should be written to
     */
    private static void writeLogToFile(File outputDir) {
        File f_out = new File(outputDir + "/log.txt");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f_out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        }

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        String output = program_configuration.append("\n\n").append(summarized_results).append("\n\n").append(iteration_results).toString();

        try {
            bw.write(output);
            bw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Convert collected data lists to a readable string in summarized_results
     * @param uniqueFailureResults List of unique failures per program iteration
     * @param inputs               List of sequential mutated inputs per program iteration
     * @param methods              List of summed HighOrderMutation methods applied per iteration
     * @param columns              List of count per column how many times mutation was applied to said column
     * @param durations            List of iteration durations
     * @param mutationStackCountPerIteration
     * @param errorInputCount
     * @param validInputCount
     */
    private static void summarizeProgramIterations(ArrayList<ArrayList<Integer>> uniqueFailureResults, ArrayList<ArrayList<String>> inputs, ArrayList<ArrayList<String>> methods, ArrayList<ArrayList<String>> columns, ArrayList<Long> durations, ArrayList<ArrayList<String>> mutationStackCountPerIteration, ArrayList<Long> errorInputCount, ArrayList<Long> validInputCount) {
        summarized_results.append("\n#********* PROGRAM SUMMARY **********");
        // --------------- UNIQUE FAILURES --------------
        summarized_results.append("\n#CUMULATIVE UNIQUE FAILURE PER TEST PER ITERATION");
        for (int i = 0; i < uniqueFailureResults.size(); i++) {
            summarized_results.append("\nRun_" + (i + 1) + "= " + uniqueFailureResults.get(i));
        }

        // --------------- INPUTS --------------
        summarized_results.append("\n\n#MUTATION RESULTS PER ITERATION");
        summarized_results.append(dataPerIterationListToLog(inputs));

        // --------------- MUTATION COUNTER --------------
        summarized_results.append("\n\n #MUTATED INPUTS PER ITERATION");
        summarized_results.append(dataPerIterationListToLog(methods));

        // --------------- COLUMN COUNTER --------------
        summarized_results.append("\n\n MUTATIONS APPLIED ON COLUMN PER ITERATION");
        summarized_results.append(dataPerIterationListToLog(columns));

        // --------------- DURATION --------------
        summarized_results.append("\n\n #DURATION PER ITERATION");
        summarized_results.append("\ndurations= " + durations);
        for (int i = 0; i < durations.size(); i++) {
            summarized_results.append("\nRun_" + (i + 1) + "= \"" + durations.get(i) + " ms\"");
        }

        // --------------- MUTATION STACK ---------------------
        summarized_results.append("\n\n #STACKED COUNT PER MUTATION PER ITERATION");
        summarized_results.append(dataPerIterationListToLog(mutationStackCountPerIteration));

        // --------------- ERRORS ---------------------
        summarized_results.append("\n\n #ERROR/VALID COUNT PER ITERATION");
        summarized_results.append("\ntotal_errors= " + errorInputCount);
        for (int i = 0; i < errorInputCount.size(); i++) {
            summarized_results.append("\nRun_" + (i + 1) + "= " + errorInputCount.get(i) + " ");
        }

        summarized_results.append("\ntotal_valid_inputs: " + validInputCount);
        for (int i = 0; i < validInputCount.size(); i++) {
            summarized_results.append("\nRun_" + (i + 1) + "= " + validInputCount.get(i) + " ");
        }

        System.out.println(summarized_results);

    }

    private static StringBuilder dataPerIterationListToLog(ArrayList<ArrayList<String>> lists) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < lists.size(); i++) {
            res.append("\nRun_" + (i + 1) + " [");
            for (int j = 0; j < lists.get(i).size(); j++) {
                if (j != 0) {
                    res.append(", ");
                }
                res.append("\"" + lists.get(i).get(j) + "\"");
            }
            res.append("]");
        }
        return res;
    }

    /**
     * Transforms data in guidance to required lists.
     * @param guidance             guidance class which contains all data
     * @param maxTrials            maximal amount of trials (configuration)
     * @param inputs               list of inputs passed to the program that is being tested
     * @param uniqueFailureResults list of unique failures
     * @param methods              list of methods applied
     * @param columns              list of count how many times mutation was applied per column
     * @param mutationStacks
     * @param errorInputCount
     * @param validInputCount
     */
    private static void writeToLists(BigFuzzPlusGuidance guidance, Long maxTrials, ArrayList<ArrayList<String>> inputs, ArrayList<ArrayList<Integer>> uniqueFailureResults, ArrayList<ArrayList<String>> methods, ArrayList<ArrayList<String>> columns, ArrayList<ArrayList<String>> mutationStacks, ArrayList<Long> errorInputCount, ArrayList<Long> validInputCount) {
        // Unique failure results
        int cumulative = 0;
        ArrayList<Integer> runFoundUniqueFailureCumulative = new ArrayList<>();
        for (long j = 0; j < maxTrials; j++) {
            if (guidance.uniqueFailureRuns.contains(j))
                cumulative++;
            runFoundUniqueFailureCumulative.add(cumulative);
        }

        uniqueFailureResults.add(runFoundUniqueFailureCumulative);
        // Methods and columns
        if (guidance.mutation instanceof StackedMutation) {

            ArrayList<HighOrderMutation.HighOrderMutationMethod> methodTracker = new ArrayList<>();
            ArrayList<Integer> columnTracker = new ArrayList<>();
            methodTracker = ((StackedMutation) guidance.mutation).getMutationMethodTracker();
            columnTracker = ((StackedMutation) guidance.mutation).getMutationColumnTracker();

            HashMap<HighOrderMutation.HighOrderMutationMethod, Integer> methodMap = new HashMap();
            HashMap<Integer, Integer> columnMap = new HashMap();
            for (int i = 0; i < methodTracker.size(); i++) {
                HighOrderMutation.HighOrderMutationMethod method = methodTracker.get(i);
                int column = columnTracker.get(i);
                if (methodMap.containsKey(method)) {
                    methodMap.put(method, methodMap.get(method) + 1);
                } else {
                    methodMap.put(method, 1);
                }
                if (columnMap.containsKey(column)) {
                    columnMap.put(column, columnMap.get(column) + 1);
                } else {
                    columnMap.put(column, 1);
                }
            }
            Iterator<Map.Entry<HighOrderMutation.HighOrderMutationMethod, Integer>> it = methodMap.entrySet().iterator();
            ArrayList<String> methodStringList = new ArrayList();
            while (it.hasNext()) {
                Map.Entry e = it.next();
                methodStringList.add(e.getKey() + ": " + e.getValue());
            }


            Iterator<Map.Entry<Integer, Integer>> it2 = columnMap.entrySet().iterator();
            ArrayList<String> columnStringList = new ArrayList();
            while (it2.hasNext()) {
                Map.Entry e = it2.next();
                columnStringList.add(e.getKey() + ": " + e.getValue());
            }
            methods.add(methodStringList);
            columns.add(columnStringList);
            errorInputCount.add((long)guidance.totalFailures);
            validInputCount.add(guidance.numValid);
        }


        //Mutation stack
        if (guidance.mutation instanceof StackedMutation) {
            ArrayList<Integer> stackCountList = ((StackedMutation) guidance.mutation).getMutationStackTracker();
            // Create a hashmap of the count and how many times it occured
            HashMap<Integer,Integer> stackCount = new HashMap();
            for (int i = 0; i < stackCountList.size(); i++) {
                if (stackCount.containsKey(stackCountList.get(i))) {
                    stackCount.put(stackCountList.get(i), stackCount.get(stackCountList.get(i)) + 1);
                } else {
                    stackCount.put(stackCountList.get(i), 1);
                }
            }
            Iterator<Map.Entry<Integer, Integer>> it = stackCount.entrySet().iterator();
            ArrayList<String> mutationStackStringList = new ArrayList();
            while (it.hasNext()) {
                Map.Entry e = it.next();
                mutationStackStringList.add(e.getKey() + ": " + e.getValue());
            }

            mutationStacks.add(mutationStackStringList);
        }



        inputs.add(guidance.inputs);
    }

    /**
     * Prints the configuration and the results from the run to the Terminal.
     *
     * @param testClassName  Class name which is being tested
     * @param testMethodName Test method name which is used to perform the test
     * @param file           Input file for the testing
     * @param maxTrials      maximal amount of trials configuration
     * @param duration       maximal duration of the trials configuration
     * @param startTime      start time of the program
     * @param endTime        end time of the program
     * @param guidance       guidance class which is used to perform the BigFuzz testing
     * @param atIteration    Counter indicating for which iteration this evaluation is.
     */
    private static void evaluation(String testClassName, String testMethodName, String file, Long maxTrials, Duration duration, long startTime, long endTime, BigFuzzPlusGuidance guidance, int atIteration) {
        StringBuilder e_log = new StringBuilder();
        // Print configuration
        e_log.append("\n*** TEST " + atIteration + " LOG ***");
        e_log.append("\n---CONFIGURATION---");
        e_log.append("\nFiles used..." + "\n\tconfig:\t\t" + file + "\n\ttestClass:\t" + testClassName + "\n\ttestMethod:\t" + testMethodName);
        e_log.append("\n\nMax trials: " + maxTrials);
        e_log.append("\nMax duration: " + duration.toMillis() + "ms");

        e_log.append("\n---REPRODUCIBILITY---");
        if (guidance.mutation instanceof StackedMutation) {
            e_log.append("\n\tRandomization seed: " + ((StackedMutation) guidance.mutation).getRandomizationSeed());
        }
        e_log.append("\n\tMutated inputs: [");
        for (int i = 0; i < guidance.inputs.size(); i++) {
            if (i != 0) {
                e_log.append(", ");
            }
            e_log.append("\"" + guidance.inputs.get(i) + "\"");
        }
        e_log.append("]");

        // Print results
        e_log.append("\n---RESULTS---");

        // Failures
        e_log.append("\nTotal run count: " + guidance.numTrials);
        e_log.append("\n\tTotal Failures: " + guidance.totalFailures);
        e_log.append("\n\tTotal Valid: " + guidance.numValid);
        e_log.append("\n\tTotal Invalid: " + guidance.numDiscards);
        e_log.append("\n\tUnique Failures: " + guidance.uniqueFailures.size());
        e_log.append("\n\tUnique Failures found at: " + guidance.uniqueFailureRuns);
        List<Boolean> runFoundUniqueFailure = new ArrayList<>();
        int cumulative = 0;
        List<Integer> runFoundUniqueFailureCumulative = new ArrayList<>();
        for (long i = 0; i < maxTrials; i++) {
            runFoundUniqueFailure.add(guidance.uniqueFailureRuns.contains(i));
            if (guidance.uniqueFailureRuns.contains(i))
                cumulative++;
            runFoundUniqueFailureCumulative.add(cumulative);
        }
        e_log.append("\n\tUnique Failure found per run: " + runFoundUniqueFailure);
        e_log.append("\n\tUnique Failure found per run: " + runFoundUniqueFailureCumulative);

        // Run time
        long totalDuration = endTime - startTime;
        if (guidance.numTrials != maxTrials) {
            e_log.append("Could not complete all trials in the given duration.");
        }
        e_log.append("\n\nRun time");
        e_log.append("\n\tTotal run time：" + totalDuration + "ms");
        e_log.append("\n\tAverage test run time: " + (float) totalDuration / guidance.numTrials + "ms");

        // Coverage
        int totalCov = guidance.totalCoverage.getNonZeroCount();
        int validCov = guidance.validCoverage.getNonZeroCount();
        e_log.append("\n\nCoverage: ");
        e_log.append("\n\tTotal coverage: " + totalCov);
        e_log.append("\n\tValid coverage: " + validCov);
        e_log.append("\n\tPercent valid coverage: " + (float) validCov / totalCov * 100 + "%");

        e_log.append(printUniqueFailuresWithMutations(guidance));

        System.out.println(e_log);
        iteration_results.append(e_log);
    }

    private static StringBuilder printUniqueFailuresWithMutations(BigFuzzPlusGuidance guidance) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n # Unique errors");
        Iterator<List<StackTraceElement>> uFailuresIterator = guidance.uniqueFailures.iterator();
        int counter = 1;
        while(uFailuresIterator.hasNext()) {
            List<StackTraceElement> e = uFailuresIterator.next();
            ArrayList<MutationPair> mutationPerformedAtTrial =guidance.mutationsPerRun.get(Math.toIntExact(guidance.uniqueFailuresWithTrial.get(e)));

            sb.append("\n*** UNIQUE FAILURE #" + counter + " ***");
            sb.append("\n-- failure triggered at trial " + mutationPerformedAtTrial + " --");
            String headerRow = "\n#\t\t";
            String classRow = "\nFile\t";
            String methodRow = "\nMethod\t";
            String lineRow = "\nLine\t";
            for (int i = 0; i < e.size(); i++) {
                // Usually the filename and method name are much longer than the line number. Use this amount to create tabs
                int maxLengthColumn = Math.max(e.get(i).getFileName().length(), e.get(i).getMethodName().length());
                headerRow += i + getAmountOfSpaces(maxLengthColumn, i+"");
                classRow += e.get(i).getFileName() + getAmountOfSpaces(maxLengthColumn, e.get(i).getFileName());
                methodRow += e.get(i).getMethodName() + getAmountOfSpaces(maxLengthColumn, e.get(i).getMethodName());
                lineRow += e.get(i).getLineNumber() + getAmountOfSpaces(maxLengthColumn, e.get(i).getLineNumber() + "");
            }
            sb.append(headerRow).append(classRow).append(methodRow).append(lineRow);
            counter ++;

            sb.append("\nMutation(s) triggering the error: ");
            sb.append(generateMutationLog(mutationPerformedAtTrial));

        }
        return sb;
    }

    private static StringBuilder generateMutationLog(ArrayList<MutationPair> mutationPerformedAtTrial) {
        StringBuilder sb = new StringBuilder();
        sb.append( " \n\t # \t column \t mutation");
        for (int j = 0; j < mutationPerformedAtTrial.size(); j++) {
            // Add one to the column nr to make it not 0 indexed
            int columnNr = mutationPerformedAtTrial.get(j).getElementId() + 1;
            String mutation = String.valueOf(mutationPerformedAtTrial.get(j).getMutation());
            sb.append("\n\tM_" +j + ": " + columnNr + "\t\t - \t" + mutation);
        }
        return sb;
    }

    private static String getAmountOfSpaces(int maxLengthColumn, String s) {
        String res = "\t";
        int diff = maxLengthColumn - s.length();
        for (int i = 0; i < Math.ceil(diff/4.0); i++) {
            res += "\t";
        }
        return res;
    }
}