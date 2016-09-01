package org.ekstep.language.measures.meta;

import java.util.HashMap;
import java.util.Map;

/**
 * The Class OrthographicVectors is meta class to hold orthographic vector and
 * weightage map data
 *
 * @author rayulu
 */
public class OrthographicVectors {

	/** The vector map. */
	private static Map<String, Map<String, Integer[]>> vectorMap = new HashMap<String, Map<String, Integer[]>>();

	/** The incr map. */
	private static Map<String, Map<String, Integer>> incrMap = new HashMap<String, Map<String, Integer>>();

	/** The weightage map. */
	private static Map<String, Double[]> weightageMap = new HashMap<String, Double[]>();

	/**
	 * Gets the orthographic vector.
	 *
	 * @param language
	 *            the language
	 * @param s
	 *            the s
	 * @return the orthographic vector
	 */
	public static Integer[] getOrthographicVector(String language, String s) {
		return Vectors.getVector(language, s, vectorMap);
	}

	/**
	 * Gets the increment.
	 *
	 * @param language
	 *            the language
	 * @param s
	 *            the s
	 * @return the increment
	 */
	public static int getIncrement(String language, String s) {
		return Vectors.getIncrement(language, s, incrMap);
	}

	/**
	 * Gets the weightage.
	 *
	 * @param language
	 *            the language
	 * @return the weightage
	 */
	public static Double[] getWeightage(String language) {
		return Vectors.getWeightage(language, weightageMap);
	}

	/**
	 * Gets the vector count.
	 *
	 * @param language
	 *            the language
	 * @return the vector count
	 */
	public static int getVectorCount(String language) {
		return Vectors.getVectorCount(language, weightageMap);
	}

	/**
	 * Load.
	 *
	 * @param language
	 *            the language
	 * @throws Exception
	 *             the exception
	 */
	public static void load(String language) throws Exception {
		Vectors.load(language, vectorMap, incrMap, weightageMap, "OrthographicVectors.csv",
				"OrthographicWeightage.csv");
	}

}
