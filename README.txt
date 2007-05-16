

INTRODUCTION
============

This package contains a Java implementation of a finite-state automaton 
traversal routine, based on data structures published in the FSA package by Jan 
Daciuk.

_Lametyzator_ is a purely dictionary-driven stemmer for Polish, based on the 
automaton code above and a dictionary of forms compiled from alt-ispell-pl and 
http://www.kurnik.pl/dictionary. Lametyzator can also be used as a stand-alone 
program for traversing dictionaries compiled for other languages.

_Stempelator_ is a hybrid stemmer for Polish: Lametyzator is used first, if it 
fails to provide a stem for a word, Stempel is used. Stempel is a heuristic 
stemmer for Polish and is available free of charge from 
http://www.getopt.org/stempel.


NOTE
====

Lametyzator by itself _does not_ require any additional libraries.
Stempelator requires Stempel to be present in Java classpath 
(look in the lib/ folder).

Both programs come with command-line demos and can be launched 
from command line:

java -cp [classpath to jars] com.dawidweiss.stemmers.Lametyzator
java -cp [classpath to jars] com.dawidweiss.stemmers.Stempelator


AUTHORS
=======

Marcin Miłkowski
Dawid Weiss (http://www.dawidweiss.com)


QUESTIONS, COMMENTS
===================

www.morfologik.blogspot.com
