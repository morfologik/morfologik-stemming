
package com.dawidweiss.fsa;

/**
 * An abstract class implementing some of FSA.Node functionality. This class
 * can only be accessed from within package scope.
 *
 * @author Dawid Weiss
 */
abstract class FSAAbstractNode
    implements FSA.Node
{
    /** Returns an arc with a given label, if it exists in this node.
     *  The default implementation in FSAAbstractNode simply traverses
     *  the list of arcs from the first, to the last - implementations
     *  of Node may implement a more efficient algorithm, if possible.
     */
    public FSA.Arc getArcLabelledWith( byte label )
    {
        FSA.Arc arc;

        for (arc = getFirstArc(); arc!=null; arc = getNextArc( arc ))
        {
            if (arc.getLabel() == label)
                return arc;
        }
        // arc labelled with "label" not found.
        return null;
    }
}