package morfologik.fsa.characters;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * An immutable {@link Set} of {@link CharSequence}s, backed by a deterministic
 * finite state automaton.
 */
public class FSAImmutableSet extends AbstractSet<CharSequence> {
	/**
	 * FSA constructed from the input sequences.
	 */
	private final State root;

	/**
	 * The number of accepted input sequences.
	 */
	private final int size;

	/**
	 * Construct a set from a sequence of char sequences. The input must be
	 * sorted lexicographically consistently with
	 * {@link FSABuilder#LEXICOGRAPHIC_ORDER}.
	 */
	private FSAImmutableSet(Iterable<CharSequence> strings) {
		final FSABuilder b = new FSABuilder();
		for (CharSequence chs : strings) {
			b.add(chs);
		}
		root = b.complete();
		size = StateUtils.getInfo(root).finalStatesCount;
	}

	/**
	 * Construct from already sorted data. 
	 */
	public static FSAImmutableSet fromSorted(Iterable<CharSequence> strings) {
		return new FSAImmutableSet(strings);
	}

	/**
	 * Construct from already sorted data. 
	 */
	public static FSAImmutableSet fromSorted(CharSequence... strings) {
		return new FSAImmutableSet(Arrays.asList(strings));
	}

	/**
	 * Construct from unsorted data (sorts all sequences). 
	 */
	public static FSAImmutableSet fromUnsorted(CharSequence... strings) {
		Arrays.sort(strings, FSABuilder.LEXICOGRAPHIC_ORDER);
		return new FSAImmutableSet(Arrays.asList(strings));
	}

	/**
	 * Construct from unsorted data (sorts all sequences). 
	 */
	public static FSAImmutableSet fromUnsorted(Iterable<CharSequence> strings) {
		final ArrayList<CharSequence> input = new ArrayList<CharSequence>();
		for (CharSequence chs : strings)
			input.add(chs);
		Collections.sort(input, FSABuilder.LEXICOGRAPHIC_ORDER);
		return new FSAImmutableSet(input);
	}

	@Override
	public boolean contains(Object o) {
		if (o instanceof CharSequence) {
			return contains((CharSequence) o);
		} else {
			return false;
		}
	}

	/**
	 * Signature specification. 
	 */
	public boolean contains(CharSequence o) {
		int pos = 0, max = o.length();
		State next, state = root;
		while (pos < max && (next = state.getState(o.charAt(pos))) != null) {
			state = next;
			pos++;
		}

		return pos == max && state != null && state.isFinal(); 
	}

	@Override
	public Iterator<CharSequence> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean add(CharSequence e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends CharSequence> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}
}
