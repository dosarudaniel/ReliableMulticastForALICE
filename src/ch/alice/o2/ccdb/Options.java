package ch.alice.o2.ccdb;

/**
 * @author costing
 * @since 2017-09-28
 */
public class Options {
	/**
	 * Search the JVM arguments for the given key falling back to the environment variable (in uppercase and with '_' instead of '.') to extract the value for this variable. If no other option is
	 * available then return the default value. For example passing key="some.key" would check if '-Dsome.key=value' was passed to JVM or if 'SOME_KEY' is set in the environment.
	 *
	 * @param key
	 * @param defaultValue
	 * @return the value for this key, from JVM arguments, environment or finally the default value if nothing else is available.
	 */
	public static String getOption(final String key, final String defaultValue) {
		String tmp = System.getProperty(key);

		if (tmp != null && tmp.length() > 0)
			return tmp;

		final String envKey = key.toUpperCase().replace('.', '_');

		tmp = System.getenv(envKey);

		if (tmp != null && tmp.length() > 0)
			return tmp;

		return defaultValue;
	}

	/**
	 * Similar to {@link #getOption(String, String)} but casting the value to integer before returning it, falling back to the default value if no other integer value is found in either JVM arguments
	 * or the environment.
	 *
	 * @param key
	 * @param defaultValue
	 * @return integer value for this key
	 */
	public static int getIntOption(final String key, final int defaultValue) {
		final String value = getOption(key, null);

		if (value != null)
			try {
				return Integer.parseInt(value);
			} catch (@SuppressWarnings("unused") final NumberFormatException nfe) {
				// ignore
			}

		return defaultValue;
	}
}
