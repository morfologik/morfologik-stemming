

INTRODUCTION
============

This package contains a Java implementation of a finite-state automaton (FSA) 
traversal routine, based on data structures published in the FSA package by Jan 
Daciuk.

_Lametyzator_ is a purely dictionary-driven stemmer for Polish, based on the 
automaton code above and a dictionary of forms collected and compiled in the Morfologik
project. Lametyzator can also be used as a stand-alone program for traversing 
dictionaries compiled for other languages of course.

_Stempelator_ is a hybrid (dictionary-heuristic) stemmer for Polish; 
Lametyzator is used first, if it fails to provide a stem for a word, 
Stempel is used. Stempel is a heuristic stemmer for Polish, available free of charge from 
http://www.getopt.org/stempel.


NOTE
====

Lametyzator by itself _does not_ require any additional libraries. However,
command-line utilities within the package may do.

The stemming package comes with several utilities for diagnosing FSA dictionaries
and for preprocessing texts. Type:

java -jar morfologik-stemming-*.jar

for an up-to-date list of all tools.


AUTHORS
=======

Marcin Miłkowski
Dawid Weiss (http://www.dawidweiss.com)


QUESTIONS, COMMENTS
===================

www.morfologik.blogspot.com
