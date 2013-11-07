package morfologik.tools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;
import java.util.Map.Entry;

import morfologik.stemming.*;
import morfologik.stemming.Dictionary;

/**
 * Calculate inflection frames from the Polish dictionary.
 */
public class InflectionFramesTool {
	public static void main(String[] args) throws IOException {
		new InflectionFramesTool().inflectionFrames();
	}

	/* */
	@SuppressWarnings( { "unused" })
	public void inflectionFrames() throws IOException {
		final Dictionary pl = Dictionary.getForLanguage("pl");
		final DictionaryLookup dict = new DictionaryLookup(pl);
		final CharsetDecoder decoder = pl.metadata.getDecoder();

		final HashMap<String, ArrayList<String>> forms = 
			new HashMap<String, ArrayList<String>>();

		ByteBuffer stemBuffer = ByteBuffer.allocate(0);
		ByteBuffer inflBuffer = ByteBuffer.allocate(0);
		ByteBuffer stemDecoded = ByteBuffer.allocate(0);

		int limit = Integer.MAX_VALUE;

		final Iterator<WordData> i = new DictionaryIterator(pl, decoder, false);
		while (i.hasNext() && limit-- > 0) {
			final WordData wd = i.next();

			final CharSequence inflected = wd.getWord();
			final CharSequence stemEncoded = wd.getStem();
			final CharSequence tag = wd.getTag();
			if (tag == null)
				continue;

			inflBuffer.clear();
			inflBuffer = wd.getWordBytes(inflBuffer);

			stemBuffer.clear();
			stemBuffer = wd.getStemBytes(stemBuffer);

			stemDecoded = DictionaryLookup.decodeBaseForm(stemDecoded, stemBuffer
			        .array(), stemBuffer.remaining(), inflBuffer, pl.metadata);
			stemDecoded.flip();

			final String stem = decoder.decode(stemDecoded).toString();
			final String form = tag.toString().intern();

			ArrayList<String> frames = forms.get(stem);
			if (frames == null) {
				forms.put(stem, frames = new ArrayList<String>());
			}

			if (!frames.contains(form)) {
				frames.add(form);
			}
		}

		// Sort the forms so that we get a unique key. Then iteratively add them
		// to another hash (by form this time).
		final HashMap<String, ArrayList<String>> frames = 
			new HashMap<String, ArrayList<String>>();

		StringBuilder key = new StringBuilder();
		for (Map.Entry<String, ArrayList<String>> e : forms.entrySet()) {
			Collections.sort(e.getValue());

			key.setLength(0);
			for (String s : e.getValue())
				key.append(s).append(" ");

			final String k = key.toString();
			ArrayList<String> words = frames.get(k);
			if (words == null) {
				frames.put(k, words = new ArrayList<String>());
			}
			words.add(e.getKey());

			e.setValue(null);
		}

		// Print inflection frames.
		ArrayList<Map.Entry<String, ArrayList<String>>> entries = 
			new ArrayList<Map.Entry<String, ArrayList<String>>>();

		entries.addAll(frames.entrySet());
		Collections.sort(entries,
		        new Comparator<Map.Entry<String, ArrayList<String>>>() {
			        public int compare(Entry<String, ArrayList<String>> o1,
			                Entry<String, ArrayList<String>> o2) {
				        return o2.getValue().size() - o1.getValue().size();
			        }
		        });

		for (Map.Entry<String, ArrayList<String>> e : entries) {
			System.out.println(String.format("%6d   %s %s",
			        e.getValue().size(), e.getKey(), e.getValue()));
		}

		System.out.println("Total frames: " + frames.size());
	}
}
