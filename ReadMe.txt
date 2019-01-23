*****************************************************************************************
Spike Pattern Classifier:
This program identifies various transient and steady-state elements in a firing pattern 
and outputs a class label and related parameters
******************************************************************************************
Main class is classifier.SpikePatternClassifier.java, which takes the input (spike.csv) in the following format:

There are 3 lines of input for a spike pattern:
line 1: input current, current duration(ms)
line 2: spike times(ms)
line 3: swa amplitude (This is usually >5mV for bursting types, and <5mV for non-bursting types)
******************************************************************************************

Refer to the following article for more information:
Quantitative firing pattern phenotyping of hippocampal neuron types
Alexander O. Komendantov, Siva Venkadesh, Christopher L. Rees, Diek W. Wheeler, David J. Hamilton and Giorgio A. Ascoli 
doi.org/10.1101/212084
