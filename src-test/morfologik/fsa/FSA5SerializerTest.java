package morfologik.fsa;

import static morfologik.fsa.FSAFlags.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import morfologik.util.BufferUtils;

import org.junit.Assert;
import org.junit.Test;

public class FSA5SerializerTest {
	/**
	 * 
	 */
	@Test
	public void testFSA5SerializerSimple() throws IOException {
		byte[][] input = new byte[][] { 
				{ 'a' }, 
				{ 'a', 'b', 'a' },
		        { 'a', 'c' }, 
		        { 'b' }, 
		        { 'b', 'a' }, 
		        { 'c' }, };

		Arrays.sort(input, FSABuilder.LEXICAL_ORDERING);
		State s = FSABuilder.build(input);

		checkSerialization(input, s);
	}

	/*
	 * 
	 */
	@Test
	public void testNotMinimal() throws IOException {
		byte[][] input = new byte[][] { 
				{ 'a', 'b', 'a' },
		        { 'b' }, 
		        { 'b', 'a' }, 
		        };

		Arrays.sort(input, FSABuilder.LEXICAL_ORDERING);
		State s = FSABuilder.build(input);

		checkSerialization(input, s);

		// Dump the created automata.
		// System.out.println(FSAUtils.toDot(fsa, fsa.getRootNode()));
		// System.out.println(StateUtils.toDot(s));
	}

	/*
	 * 
	 */
	@Test
	public void testAutomatonWithNodeNumbers() throws IOException {
		byte[][] input = new byte[][] {
				{ 'a' },
				{ 'a', 'b', 'a' },
				{ 'a', 'c' },
		        { 'b' }, 
		        { 'b', 'a' },
		        { 'c' },
		};

		Arrays.sort(input, FSABuilder.LEXICAL_ORDERING);
		State s = FSABuilder.build(input);

		final byte[] fsaData = 
				new FSA5Serializer()
				.withNumbers()
				.serialize(s, new ByteArrayOutputStream())
				.toByteArray();

		FSA5 fsa = (FSA5) FSA.read(new ByteArrayInputStream(fsaData));

		// Ensure we have the NUMBERS flag set.
		assertTrue(fsa.getFlags().contains(NUMBERS));
		
		// A single byte should be enough to store numbers in this automaton.
		assertEquals(1, fsa.nodeDataLength);

		// Get all numbers from nodes.
		byte[] buffer = new byte[128];
		final ArrayList<String> result = new ArrayList<String>();
		FSA5Test.walkNode(buffer, 0, fsa, fsa.getRootNode(), 0, result);

		Collections.sort(result);
		assertEquals(Arrays
		        .asList("0 a", "1 aba", "2 ac", "3 b", "4 ba", "5 c"), result);
	}

	/**
	 * 
	 */
	@Test
	public void testFSA5Bug0() throws IOException {
		checkCorrect(new String [] {
				"3-D+A+JJ",
				"3-D+A+NN",
				"4-F+A+NN",
				"z+A+NN",
		});
	}

	/**
	 * 
	 */
	@Test
	public void testFSA5Bug1() throws IOException {
		checkCorrect(new String [] {
				"+NP",
				"n+N",
				"n+NP",
		});
	}
	
	private void checkCorrect(String [] strings) throws IOException {
		byte [][] input = new byte [strings.length][];
		for (int i = 0; i < strings.length; i++) {
			input[i] = strings[i].getBytes("ISO8859-1");
		}

		Arrays.sort(input, FSABuilder.LEXICAL_ORDERING);
		State s = FSABuilder.build(input);

		checkSerialization(input, s);
	}


	/**
	 * 
	 */
	@Test
	public void testFSA5SerializerEmpty() throws IOException {
		byte[][] input = new byte[][] {};
		State s = FSABuilder.build(input);

		checkSerialization(input, s);
	}
	
	/**
	 * 
	 */
	@Test
	public void test_abc() throws IOException {
		testBuiltIn(FSA.read(FSA5Test.class.getResourceAsStream("abc.fsa")));
	}

	/**
	 * 
	 */
	@Test
	public void test_minimal() throws IOException {
		testBuiltIn(FSA.read(FSA5Test.class.getResourceAsStream("minimal.fsa")));
	}

	/**
	 * 
	 */
	@Test
	public void test_minimal2() throws IOException {
		testBuiltIn(FSA.read(FSA5Test.class.getResourceAsStream("minimal2.fsa")));
	}

	/**
	 * 
	 */
	@Test
	public void test_en_tst() throws IOException {
		testBuiltIn(FSA.read(FSA5Test.class.getResourceAsStream("en_tst.dict")));
	}

	/*
	 * 
	 */
	private void testBuiltIn(FSA fsa) throws IOException {
		final ArrayList<byte[]> sequences = new ArrayList<byte[]>();

		sequences.clear();
		for (ByteBuffer bb : fsa) {
			sequences.add(morfologik.util.Arrays.copyOf(bb.array(), bb.remaining()));
		}

		Collections.sort(sequences, FSABuilder.LEXICAL_ORDERING);

		final byte[][] in = sequences.toArray(new byte[sequences.size()][]);
		State root = FSABuilder.build(in);
		
		// Check if the DFSA is correct first.
		FSABuilderTest.checkCorrect(in, root);

		// Check serialization.
		checkSerialization(in, root);
    }

	/*
	 * 
	 */
	private void checkSerialization(byte[][] input, State root) throws IOException {
		checkSerialization0(new FSA5Serializer(), input, root);
		checkSerialization0(new FSA5Serializer().withNumbers(), input, root);
	}

	/*
	 * 
	 */
	private void checkSerialization0(FSA5Serializer serializer, final byte[][] in, State root)
            throws IOException {
	    final byte[] fsaData = serializer.serialize(
	    		root, new ByteArrayOutputStream()).toByteArray();

		FSA5 fsa2 = (FSA5) FSA.read(new ByteArrayInputStream(fsaData));

		checkCorrect(in, fsa2);
    }

	/**
	 * Check if the FSA is correct with respect to the given input.
	 */
	protected void checkCorrect(byte[][] input, FSA fsa) {
		// (1) All input sequences are in the right language.
		HashSet<ByteBuffer> rl = new HashSet<ByteBuffer>();
		for (ByteBuffer bb : fsa) {
			byte[] array = bb.array();
			int length = bb.remaining();
			rl.add(ByteBuffer.wrap(morfologik.util.Arrays.copyOf(array, length)));
		}

		HashSet<ByteBuffer> uniqueInput = new HashSet<ByteBuffer>();
		for (byte[] sequence : input) {
			uniqueInput.add(ByteBuffer.wrap(sequence));
		}

		for (ByteBuffer sequence : uniqueInput) {
			Assert.assertTrue("Not present in the right language: " + 
					BufferUtils.toString(sequence),
			        rl.remove(sequence));
		}

		// (2) No other sequence _other_ than the input is in the right language.
		Assert.assertEquals(0, rl.size());
	}
}
