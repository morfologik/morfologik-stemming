
MORFOLOGIK
==========

morfologik-fsa:
morfologik-fsa-builders:

  - Creation of byte-based, efficient finite state automata in Java, including
    custom, efficient data storage formats.

  - Compatibility with FSA5, binary format of finite state automata produced by
    Jan Daciuk's "fsa" package.

morfologik-stemming:

  - Automata-based dictionary stemming infrastructure.

morfologik-polish:

  - Precompiled dictionary of inflected forms, stems and tags for Polish.

morfologik-tools:

  - Command line tools to preprocess, build and dump FSA automata and dictionaries.

  - There are a few command-line tools you may find useful. Type:
    java -jar lib/morfologik-tools-${version}.jar
    for an up-to-date list of all tools.

morfologik-speller:

  - Simplistic automaton-based spelling correction (suggester).


COMPILING
=========

mvn package


AUTHORS
=======

Marcin Miłkowski (http://marcinmilkowski.pl) [linguistic data lead, code]
Dawid Weiss (http://www.dawidweiss.com) [fsa lead, code]


CONTRIBUTORS
============

Daniel Naber
Jaume Ortolà
Grzegorz Słowikowski


QUESTIONS, COMMENTS
===================

Web site:     http://www.morfologik.blogspot.com
Mailing list: morfologik-devel@lists.sourceforge.net
