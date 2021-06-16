Reported by Marijn Smits, sometimes the stacktrace of a failed test is empty. The bug has been reproduced with the provided seed and can be reproduced using the following program arguments:
SalaryAnalysisDriver testSalaryAnalysis StackedMutation 6000 3 5

This error produces an incorrect unique error as the mutated input has already been run on the test program and has recorded an error for that run. The error seems to only appear for a specific kind of error (ArrayOutOfBoundException can't be cast to a NumberFormatException)

There was an attempt to trace back which caused the empty stacktrace, but in debug mode the error can't be reproduced.