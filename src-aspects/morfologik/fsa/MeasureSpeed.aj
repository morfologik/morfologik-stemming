package morfologik.fsa;

import java.util.HashMap;

import org.omg.CORBA.IntHolder;

/**
 * Aspects useful for debugging.
 */
public aspect MeasureSpeed issingleton() {
    public int addSuffixTime; 
    public int replaceOrRegisterTime;

    Object around(): 
        execution(* morfologik.fsa.FSABuilder.addSuffix(..))
    {
        final long start = System.currentTimeMillis();
        try {
            return proceed();
        } finally {
            addSuffixTime += (System.currentTimeMillis() - start);
        }
    }

    Object around():
        withincode(* morfologik.fsa.FSABuilder.add(..)) &&
        call(* morfologik.fsa.FSABuilder.replaceOrRegister(..))
    {
        final long start = System.currentTimeMillis();
        try {
            return proceed();
        } finally {
            replaceOrRegisterTime += (System.currentTimeMillis() - start);
        }
    }

    State around(): 
        execution(State morfologik.fsa.FSABuilder.build(..))
    {
        long start = System.currentTimeMillis();
        State result = proceed();
        long end = System.currentTimeMillis();

        System.out.println(
                "Total: " +
                ((end - start) / 1000.0) + 
                ", ror: " + 
                (replaceOrRegisterTime / 1000.0) + 
                ", adds: " + 
                addSuffixTime / 1000.0);

        // Check hash code collision rate.
        final HashMap<Integer, IntHolder> collisions = new HashMap<Integer, IntHolder>();
        result.preOrder(new Visitor<State>() {
            public void accept(State s) {
                int hash = s.hashCode();
                if (!collisions.containsKey(hash)) {
                    collisions.put(hash, new IntHolder(1));
                } else {
                    collisions.get(hash).value++;
                }
            };
        });
        
        int collSlots = 0;
        int collCumulative = 0;
        for (IntHolder ih : collisions.values()) {
            if (ih.value > 1) {
                collSlots++;
                collCumulative += ih.value;
            }
        }

        System.out.println("States: " + collisions.size()
                + ", coll: " + collSlots + ", cum: " + collCumulative);

        replaceOrRegisterTime = 0;
        addSuffixTime = 0;

        return result;
    }
}
