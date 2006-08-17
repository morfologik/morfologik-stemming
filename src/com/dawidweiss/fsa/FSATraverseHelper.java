
package com.dawidweiss.fsa;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * FSATraverseHelper implements some common match/ traverse/ find operations on a generic FSA.
 * Optimized implementations may be provided my specific versions of FSA, therefore objects
 * of this class shouls ALWAYS be instantiated via FSA.getTraverseHelper
 */
public final class FSATraverseHelper
{
    /** 
     * Creates a traverse object for some generic FSA automaton 
     */
    protected FSATraverseHelper()
    {
    	// Input argument currently unused.
    }


    /**
     * Returns an Iterator of all subsequences available from the given node to
     * all reachable final states. The iterator returns byte arrays.
     *
     * The iterator operates on a stack and traverses the FSA on-demand.
     *
     * Please note this object is NOT synchronized. Synchronize externally if needed.
     *
     * @throws NullPointerException if node parameter is null.
     */
    public Iterator getAllSubsequences( final FSA.Node node )
    {
        if (node==null)
            throw new NullPointerException("Node cannot be null.");

        // Create a custom iterator in the FSA
        final Iterator iterator;
        iterator = new Iterator() {
                final LinkedList nodes = new LinkedList();
                final LinkedList arcs  = new LinkedList();
                final StringBuffer buffer = new StringBuffer( );

                // static initializer.
                {   // initialize instance. push initial Node to stack.
                    if (node.getFirstArc() != null)
                    {
                        nodes.add(node);
                        arcs.add(node.getFirstArc());
                    }
                }

                // A cache for the next element in the FSA
                private Object nextElement = null;

                /** Returns true if there are still elements in this iterator. */
                public boolean hasNext()
                {
                    if (nextElement==null) {
                        nextElement = advance();
                    }
                    return nextElement!=null;
                }

                /**
                 * Returns the next element.
                 * @throws NoSuchElementException If this method is called after hasNext returned
                 *         false.
                 */
                public Object next()
                {
                    if (nextElement!=null)
                    {
                        Object cache = nextElement;
                        nextElement  = null;
                        return cache;
                    }
                    else
                    {
                        Object cache = advance();
                        if (cache==null)
                            throw new NoSuchElementException("No next element to traverse.");
                        return cache;
                    }
                }

                /** advance to the next final state (and build a new sentence) */
                protected Object advance()
                {
                    if (arcs.isEmpty())
                        return null;

                    while (!arcs.isEmpty())
                    {
                        final FSA.Arc arc = ((FSA.Arc)arcs.get(0));

                        if (arc == null)
                        {
                            // remove the current node from the queue
                            nodes.remove(0);
                            arcs.remove(0);
                            continue;
                        }

                        arcs.set(0, ((FSA.Node)nodes.get(0)).getNextArc(arc)) ;

                        if (buffer.length() < nodes.size() )
                        {
                            buffer.setLength( nodes.size() );
                        }
                        buffer.setCharAt( nodes.size()-1, (char) arc.getLabel());

                        if (arc.pointsToFinalNode())
                        {
                            // a full sequence has been found.
                            final char [] chars = new char [ nodes.size() ];
                            buffer.getChars(0, nodes.size(), chars, 0);
                            final byte [] sequence = new byte [ nodes.size() ];
                            for (int i=chars.length-1;i>=0;i--)
                                sequence[i] = (byte)(chars[i] & 0xff);
                            return sequence;
                        }
                        else
                        {
                            // continue drilling down.
                            nodes.add(0, arc.getDestinationNode());
                            arcs.add(0, arc.getDestinationNode().getFirstArc());
                            continue;
                        }
                    }
                    return null;
                }

                public void remove()
                {
                    throw new UnsupportedOperationException("Cannot remove nodes of a FSA. Iterator is always read-only.");
                }
            };

        return iterator;
    }


    /**
     * Finds a matching path in the dictionary for a given sequence of labels.
     * There are several cases possible:
     * <ol>
     *      <li><code>EXACT_MATCH</code>The sequence ends exactly on the final node. A
     *          match has been found.
     *
     *      <li><code>PREFIX_FOUND</code>The sequence ends on an intermediate
     *          automaton node. The sequence is therefore
     *          a prefix of at least one other sequence stored in the dictionary. The result
     *          Match will contain an index of the first character in the input sequence not present
     *          in the dictionary and a pointer to the FSA.Node where mismatch occurred.
     *
     *      <li><code>PREMATURE_PATH_END_FOUND</code>Dictionary's path ends before the sequence.
     *          It means a prefix of the input sequence
     *          is stored in the dictionary. (i.e. an empty sequence is a prefix of all other
     *          sequences). The result Match will contain an index of the first character
     *          not present in the dictionary.
     *
     *      <li><code>PREMATURE_WORD_END_FOUND</code>
     *          The input sequence ends on an intermediate automaton node. This is a special
     *          case of PREFIX_FOUND. A Node where the mismatch (lack of input sequence's characters)
     *          occurred is returned in Match.
     * </ol>
     */
    public FSAMatch matchSequence( byte [] word, FSA fsa )
    {
        return matchSequence( word, fsa.getStartNode() );
    }


    /**
     * Finds a matching path in the dictionary for a given sequence of labels,
     * starting from some internal dictionary's node.
     *
     * Please refer to documentation of the <code>matchSequence(byte [] word, FSA.Node node)</code> method.
     *
     * @see #matchSequence(byte [], FSA.Node)
     */
    public FSAMatch matchSequence( byte [] word, FSA.Node node )
    {
        FSA.Arc  arc;

        if (node == null)
            return new FSAMatch( FSAMatch.NO_MATCH );

        for (int i=0;i<word.length;i++)
        {
            arc = node.getArcLabelledWith( word[i] );
            if (arc != null)
            {
                if (arc.pointsToFinalNode())
                {
                    if (i+1==word.length)
                    {
                        // the word has been found (exact match).
                        return new FSAMatch( FSAMatch.EXACT_MATCH );
                    }
                    else
                    {
                        // a prefix of the word has been found
                        // (there are still characters in the word, but the path is over)
                        return new FSAMatch( FSAMatch.PREMATURE_PATH_END_FOUND, i+1, null );
                    }
                }
                else
                {
                    // make a transition along the arc.
                    node = arc.getDestinationNode();
                }
            }
            else
            {
                // the label was not found. i.e. there possibly are prefixes
                // of the word in the dictionary, but an exact match doesn't exist.
                // [an empty string is also considered a prefix!]
                return new FSAMatch( FSAMatch.PREFIX_FOUND, i, node );
            }
        }

        // the word is a prefix of some other sequence(s) present in the dictionary.
        return new FSAMatch( FSAMatch.PREMATURE_WORD_END_FOUND, 0, node );
    }

}