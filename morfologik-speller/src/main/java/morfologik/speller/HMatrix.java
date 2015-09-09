package morfologik.speller;

/**
 * Keeps track of already computed values of edit distance. 
 * Remarks: To save space, the matrix is kept in a vector.
 */
public class HMatrix {
	private int[] p; /* the vector */
	private int rowLength; /* row length of matrix */
	int columnHeight; /* column height of matrix */
	int editDistance; /* edit distance */

	/**
	 * Allocates memory and initializes matrix (constructor).
	 * 
	 * @param distance (int) max edit distance allowed for
	 *        candidates;
	 * @param maxLength (int) max length of words.
	 *
     *          Remarks: See Oflazer. To save space, the matrix is
	 *          stored as a vector. To save time, additional rows and
	 *          columns are added. They are initialized to their distance in
	 *          the matrix, so that no bound checking is necessary during
	 *          access.
	 */
	public HMatrix(final int distance, final int maxLength) {
		rowLength = maxLength + 2;
		columnHeight = 2 * distance + 3;
		editDistance = distance;
		final int size = rowLength * columnHeight;
		p = new int[size];
		// Initialize edges of the diagonal band to distance + 1 (i.e.
		// distance too big)
		for (int i = 0; i < rowLength - distance - 1; i++) {
			p[i] = distance + 1; // H(distance + j, j) = distance + 1
			p[size - i - 1] = distance + 1; // H(i, distance + i) = distance
			// + 1
		}
		// Initialize items H(i,j) with at least one index equal to zero to
		// |i - j|
		for (int j = 0; j < 2 * distance + 1; j++) {
			p[j * rowLength] = distance + 1 - j; // H(i=0..distance+1,0)=i
			// FIXME: fordistance == 2 we exceed the array size here.
      // there's a bug in spell.cc, Jan Daciuk has been notified about it.
			p[Math.min(p.length - 1, (j + distance + 1) * rowLength + j)] = j; // H(0,j=0..distance+1)=j
		}
	}

	/**
	 * Provide an item of hMatrix indexed by indices.
	 * 
	 * @param i
	 *            - (int) row number;
	 * @param j
	 *            - (int) column number.
	 * @return Item <code>H[i][j]</code>. Remarks: H matrix is really simulated. What is needed is only
	 *         <code>2 * edit_distance + 1</code> wide band around the diagonal. In fact
	 *         this diagonal has been pushed up to the upper border of the
	 *         matrix.
	 * 
	 *         The matrix in the vector looks likes this:

	 * <pre>
	 * 	    +---------------------+
	 * 	0   |#####################| j=i-e-1
	 * 	1   |                     | j=i-e
	 * 	    :                     :
	 * 	e+1 |                     | j=i-1
	 * 	    +---------------------+
	 * 	e+2 |                     | j=i
	 * 	    +---------------------+
	 * 	e+3 |                     | j=i+1
	 * 	    :                     :
	 * 	2e+2|                     | j=i+e
	 * 	2e+3|#####################| j=i+e+1
	 * 	    +---------------------+
	 * </pre>
	 */
	public int get(final int i, final int j) {
		return p[(j - i + editDistance + 1) * rowLength + j];
	}

	/**
	 * Set an item in hMatrix.
   * No checking for i &amp; j is done. They must be correct.
	 * 
	 * @param i
	 *            - (int) row number;
	 * @param j
	 *            - (int) column number;
	 * @param val
	 *            - (int) value to put there.
	 */
	public void set(final int i, final int j, final int val) {
		p[(j - i + editDistance + 1) * rowLength + j] = val;
	}

}