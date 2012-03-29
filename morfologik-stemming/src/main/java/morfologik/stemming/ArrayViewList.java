package morfologik.stemming;

import java.util.*;

/**
 * A view over a range of an array.
 */
@SuppressWarnings("serial")
final class ArrayViewList<E> extends AbstractList<E> 
	implements RandomAccess, java.io.Serializable
{
	/** Backing array. */
	private E[] a;
	private int start;
	private int length;

	/*
     * 
     */
	ArrayViewList(E[] array, int start, int length) {
		if (array == null)
			throw new IllegalArgumentException();
		wrap(array, start, length);
	}

	/*
     * 
     */
	public int size() {
		return length;
	}

	/*
     * 
     */
	public E get(int index) {
		return a[start + index];
	}

	/*
     * 
     */
	public E set(int index, E element) {
		throw new UnsupportedOperationException();
	}

	/*
     * 
     */
	public void add(int index, E element) {
		throw new UnsupportedOperationException();
	}

	/*
     * 
     */
	public E remove(int index) {
		throw new UnsupportedOperationException();
	}

	/*
     * 
     */
	public boolean addAll(int index, Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	/*
     * 
     */
	public int indexOf(Object o) {
		if (o == null) {
			for (int i = start; i < start + length; i++)
				if (a[i] == null)
					return i - start;
		} else {
			for (int i = start; i < start + length; i++)
				if (o.equals(a[i]))
					return i - start;
		}
		return -1;
	}

	public ListIterator<E> listIterator() {
		return listIterator(0);
	}

	/*
     * 
     */
	public ListIterator<E> listIterator(final int index) {
		return Arrays.asList(a).subList(start, start + length).listIterator(
		        index);
	}

	/*
     * 
     */
	public boolean contains(Object o) {
		return indexOf(o) != -1;
	}

	/*
     * 
     */
	void wrap(E[] array, int start, int length) {
		this.a = array;
		this.start = start;
		this.length = length;
	}
}
