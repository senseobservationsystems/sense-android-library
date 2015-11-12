package nl.sense_os.util.json;

import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main.Advanced;

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Base64;

public class EncryptionHelper {

	public static class EncryptionHelperException extends RuntimeException {

        private static final long serialVersionUID = -4628390225138450908L;

        public EncryptionHelperException(Throwable e) {
			super(e);
		}

	}

	private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
	private static final String SECRET_KEY_HASH_TRANSFORMATION = "SHA-256";
	private static final String DEFAULT_KEY_SALT = "3XnMxOy3E&jsdSSHWM941D89yK!RlRVH";

	private static SharedPreferences sMainPrefs;

	private final Cipher writer;
	private final Cipher reader;

	public EncryptionHelper(Context context) throws EncryptionHelperException {
		this(context, null);
	}

	/**
	 * This will initialize an instance of the EncryptionHelper class
	 * @param context your current context.
	 * @param secureKey the key used for encryption, finding a good key scheme is hard. 
	 * Hardcoding your key in the application is bad, but better than plaintext preferences. Having the user enter the key upon application launch is a safe(r) alternative, but annoying to the user.
	 * @throws EncryptionHelperException
	 */
	public EncryptionHelper(Context context, String secureKey) throws EncryptionHelperException {
		try {
			this.writer = Cipher.getInstance(TRANSFORMATION);
			this.reader = Cipher.getInstance(TRANSFORMATION);

			initCiphers(context, secureKey);
		}
		catch (GeneralSecurityException e) {
			throw new EncryptionHelperException(e);
		}
	}

	protected void initCiphers(Context context, String secureKey) throws NoSuchAlgorithmException, InvalidKeyException,
			InvalidAlgorithmParameterException {
		if (null == secureKey) {
			TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			String imei = telephonyManager.getDeviceId();

			secureKey = imei;
		}

		if (null == sMainPrefs) {
			sMainPrefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
						Context.MODE_PRIVATE);
		}

		String salt = sMainPrefs.getString(Advanced.ENCRYPT_CREDENTIAL_SALT, DEFAULT_KEY_SALT);

		IvParameterSpec ivSpec = getIv();
		SecretKeySpec secretKey = getSecretKey(secureKey+salt);

		writer.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
		reader.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
	}

	protected IvParameterSpec getIv() {
		byte[] iv = new byte[writer.getBlockSize()];
		System.arraycopy("fldsjfodasjifudslfjdsaofshaufihadsf".getBytes(), 0, iv, 0, writer.getBlockSize());
		return new IvParameterSpec(iv);
	}

	protected SecretKeySpec getSecretKey(String key) throws NoSuchAlgorithmException {
		byte[] keyBytes = createKeyBytes(key);
		return new SecretKeySpec(keyBytes, TRANSFORMATION);
	}

	protected byte[] createKeyBytes(String key) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance(SECRET_KEY_HASH_TRANSFORMATION);
		md.reset();
		byte[] keyBytes = md.digest(key.getBytes());
		return keyBytes;
	}

	public String encrypt(String value) throws EncryptionHelperException {
		byte[] secureValue;
		secureValue = convert(writer, value.getBytes());
		String secureValueEncoded = Base64.encodeToString(secureValue, Base64.NO_WRAP);
		return secureValueEncoded;
	}

	public String decrypt(String securedEncodedValue) throws EncryptionHelperException {
		if (securedEncodedValue == null || securedEncodedValue == "") {
                  return "";
		}
		try {
			byte[] securedValue = Base64.decode(securedEncodedValue, Base64.NO_WRAP);
			byte[] value = convert(reader, securedValue);
			return new String(value);
		} catch (IllegalArgumentException e) {
			// maybe data is not encrypted
                	throw new EncryptionHelperException(e);
		} catch (EncryptionHelperException e) {
                        throw e;
                }
	}

	private static byte[] convert(Cipher cipher, byte[] bs) throws EncryptionHelperException {
		try {
			return cipher.doFinal(bs);
		}
		catch (Exception e) {
			throw new EncryptionHelperException(e);
		}
	}
}
