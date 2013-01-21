package nl.sense_os.service.constants;

import java.util.Locale;

/**
 * General utility class.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class Util {

	private Util() {
		// do not instantiate
	}

	/**
	 * @param bytes
	 *            Byte count;
	 * @param si
	 *            true to use SI system, where 1000 B = 1 kB
	 * @return A String with human-readable byte count, including suffix.
	 */
	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit) {
			return bytes + " B";
		}
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format(Locale.ENGLISH, "%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
}
