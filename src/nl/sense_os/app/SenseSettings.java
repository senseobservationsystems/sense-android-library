package nl.sense_os.app;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import nl.sense_os.service.ISenseService;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SenseSettings extends PreferenceActivity {
	/**
	 * AsyncTask to check the login data with CommonSense. Takes no arguments to execute. Clears any
	 * open login dialogs before start, and displays a progress dialog during operation. If the
	 * check fails, the login dialog is shown again.
	 */
	private class CheckLoginTask extends AsyncTask<Void, Void, Boolean> {
		private static final String TAG = "CheckLoginTask";

		@Override
		protected Boolean doInBackground(Void... params) {
			boolean success = false;
			if (SenseSettings.this.service != null) {
				try {
					success = SenseSettings.this.service.serviceLogin();
				} catch (final RemoteException e) {

					e.printStackTrace();
				}
			} else {
				Log.d(TAG, "Service not bound. Skipping login task.");
			}
			return success;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			try {
				dismissDialog(DIALOG_PROGRESS);
			} catch (final IllegalArgumentException e) {
				// do nothing
			}
			if (result != true) {
				Toast.makeText(SenseSettings.this, R.string.toast_login_fail, Toast.LENGTH_LONG)
				.show();
				showDialog(DIALOG_LOGIN);
			} else {
				Toast.makeText(SenseSettings.this, R.string.toast_login_ok, Toast.LENGTH_LONG)
				.show();

				final Intent serviceIntent = new Intent(ISenseService.class.getName());
				if (null == startService(serviceIntent)) {
					Log.w(TAG, "Could not start Sense service after login!");
				}
			}
			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
			// close the login dialog before showing the progress dialog
			try {
				dismissDialog(DIALOG_LOGIN);
			} catch (final IllegalArgumentException e) {
				e.printStackTrace();
			}

			showDialog(DIALOG_PROGRESS);

			super.onPreExecute();
		}
	}

	/**
	 * AsyncTask to register a new phone/user with CommonSense. Takes no arguments to execute.
	 * Clears any open login dialogs before start, and displays a progress dialog during operation.
	 * If the check fails, the registration dialog is shown again.
	 */
	private class CheckRegisterTask extends AsyncTask<Void, Void, Boolean> {
		private static final String TAG = "CheckRegisterTask";

		@Override
		protected Boolean doInBackground(Void... params) {
			boolean success = false;
			if (SenseSettings.this.service != null) {
				try {
					success = SenseSettings.this.service.serviceRegister();
				} catch (final RemoteException e) {
					e.printStackTrace();
				}
			} else {
				Log.d(TAG, "Service not bound. Skipping registration task.");
			}
			return success;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			try {
				dismissDialog(DIALOG_PROGRESS);
			} catch (final IllegalArgumentException e) {
				// do nothing
			}

			if (result != true) {
				Toast.makeText(SenseSettings.this, getString(R.string.toast_reg_fail),
						Toast.LENGTH_LONG).show();
				showDialog(DIALOG_REGISTER);
			} else {
				Toast.makeText(SenseSettings.this, getString(R.string.toast_reg_ok),
						Toast.LENGTH_LONG).show();
			}

			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
			// close the login dialog before showing the progress dialog
			try {
				dismissDialog(DIALOG_REGISTER);
			} catch (final IllegalArgumentException e) {
				e.printStackTrace();
			}
			showDialog(DIALOG_PROGRESS);

			super.onPreExecute();
		}
	}

	private static final int DIALOG_LOGIN = 1;
	private static final int DIALOG_PROGRESS = 2;
	private static final int DIALOG_REGISTER = 3;
	/** Key for preference to autostart the sense service in boot. */
	public static final String PREF_AUTOSTART = "autostart";
	/** Key for preference that controls communication interval with CommonSense. */
	public static final String PREF_COMMONSENSE_RATE = "commonsense_rate";
	/** Key for preference that saves the last running services. */
	public static final String PREF_LAST_STATUS = "last_status";
	/** Key for preference that toggles use of GPS in location sensor. */
	public static final String PREF_LOCATION_GPS = "location_gps";
	/** Key for preference that toggles use of Network in location sensor. */
	public static final String PREF_LOCATION_NETWORK = "location_network";
	/** Key for generic login preference that displays the login dialog when clicked. */
	public static final String PREF_LOGIN = "login";
	/** Key for login preference for session cookie. */
	public static final String PREF_LOGIN_COOKIE = "login_cookie";
	/** Key for login preference for email address. */
	public static final String PREF_LOGIN_MAIL = "login_mail";
	/** Key for login preference for username. */
	public static final String PREF_LOGIN_NAME = "login_name";
	/** Key for login preference for hashed password. */
	public static final String PREF_LOGIN_PASS = "login_pass";
	/** Key for preference that sets the interval between pop quizzes. */
	public static final String PREF_QUIZ_RATE = "popquiz_rate";
	/** Key for preference that sets the silent mode for pop quizzes. */
	public static final String PREF_QUIZ_SILENT_MODE = "popquiz_silent_mode";
	/** Key for generic preference that starts an update of the quiz questions when clicked. */
	public static final String PREF_QUIZ_SYNC = "popquiz_sync";
	/** Key for preference that holds the last update time of the quiz questions with CommonSense. */
	public static final String PREF_QUIZ_SYNC_TIME = "popquiz_sync_time";
	/** Key for generic preference that shows the registration dialog when clicked. */
	public static final String PREF_REGISTER = "register";
	/** Name of the private preference file, used for storing login data. */
	public static final String PRIVATE_PREFS = "login";
	private static final String TAG = "SenseSettings";
	public static final String URL_BASE = "http://demo.almende.com/commonSense2/";
	public static final String URL_CHECK_PHONE = URL_BASE + "device_check.php";
	//public static final String URL_LOCATION_ADD = URL_BASE + "sp_position_add.php";
	public static final String URL_LOGIN = URL_BASE + "login.php";
	//public static final String URL_PHONESTATE_ADD = URL_BASE + "sp_state_add.php";
	public static final String URL_QUIZ_ADD_ANSWER = URL_BASE + "pop_quiz_answer_add.php";
	public static final String URL_QUIZ_ADD_QUESTION = URL_BASE + "pop_quiz_question_add.php";
	public static final String URL_QUIZ_CONNECT_ANSW_QSTN = URL_BASE
	+ "pop_quiz_connect_answer_to_question.php";
	public static final String URL_QUIZ_GET_QSTNS = URL_BASE + "get_pop_quiz_questions.php";
	//public static final String URL_QUIZ_SEND_ANSWER = URL_BASE
	//+ "sp_pop_quiz_answer_to_question_add.php";
	public static final String URL_REG = URL_BASE + "register.php";
	public static final String URL_REG_PHONE = URL_BASE + "device_add.php";
	public static final String URL_SEND_SENSOR_DATA = URL_BASE + "device_sensor_data_add.php";
	public static final String URL_SEND_SENSOR_DATA_FILE = URL_BASE + "device_sensor_add_file.php";
	
	public static final String SENSOR_DATA_TYPE_STRING = "string";
	public static final String SENSOR_DATA_TYPE_JSON = "json";
	public static final String SENSOR_DATA_TYPE_INT = "int";
	public static final String SENSOR_DATA_TYPE_FLOAT = "float";
	public static final String SENSOR_DATA_TYPE_BOOL = "bool";
	
	private boolean isServiceBound;
	private ISenseService service = null;
	private final ServiceConnection serviceConn = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			SenseSettings.this.service = ISenseService.Stub.asInterface(binder);
		}

		public void onServiceDisconnected(ComponentName className) {
			/* this is not called when the service is stopped, only when it is suddenly killed! */
			SenseSettings.this.service = null;
		}
	};

	private Dialog createDialogLogin() {
		// start the service if it was not running already
		if (this.service == null) {
			final Intent serviceIntent = new Intent(ISenseService.class.getName());
			bindService(serviceIntent, this.serviceConn, BIND_AUTO_CREATE);
		}

		// create View with input fields for dialog content
		final LinearLayout login = new LinearLayout(this);
		login.setOrientation(LinearLayout.VERTICAL);
		final EditText emailField = new EditText(this);
		emailField.setLayoutParams(new LayoutParams(-1, -2));
		emailField.setHint(R.string.dialog_login_hint_mail);
		emailField.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		emailField.setImeOptions(EditorInfo.IME_ACTION_NEXT);
		login.addView(emailField);
		final EditText passField = new EditText(this);
		passField.setLayoutParams(new LayoutParams(-1, -2));
		passField.setHint(R.string.dialog_login_hint_pass);
		passField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		passField.setTransformationMethod(new PasswordTransformationMethod());
		passField.setImeOptions(EditorInfo.IME_ACTION_DONE);
		login.addView(passField);

		// get current login email from preferences
		final SharedPreferences prefs = getSharedPreferences(PRIVATE_PREFS, MODE_PRIVATE);
		emailField.setText(prefs.getString(PREF_LOGIN_MAIL, ""));

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dialog_login_title);
		builder.setView(login);
		builder.setPositiveButton(R.string.button_login, new OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				final String name = emailField.getText().toString();
				final String pass = passField.getText().toString();

				final Editor editor = prefs.edit();
				editor.putString(PREF_LOGIN_MAIL, name);
				// put md5 string
				String MD5Pass = "";
				// Register to the Database
				final byte[] defaultBytes = pass.getBytes();
				try {
					final MessageDigest algorithm = MessageDigest.getInstance("MD5");
					algorithm.reset();
					algorithm.update(defaultBytes);
					final byte messageDigest[] = algorithm.digest();

					final StringBuffer hexString = new StringBuffer();
					for (final byte element : messageDigest) {
						final String hex = Integer.toHexString(0xFF & element);
						if (hex.length() == 1) {
							hexString.append(0);
						}
						hexString.append(hex);
					}
					MD5Pass = hexString.toString();
				} catch (final NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
				editor.putString(PREF_LOGIN_PASS, MD5Pass);
				editor.commit();

				// initiate Login
				new CheckLoginTask().execute();
			}
		});
		builder.setNeutralButton(R.string.button_cancel, null);
		return builder.create();
	}

	private Dialog createDialogLoginProgress() {
		final ProgressDialog dialog = new ProgressDialog(this);
		dialog.setTitle(R.string.dialog_progress_title);
		dialog.setMessage(getString(R.string.dialog_progress_login_msg));
		dialog.setCancelable(false);
		return dialog;
	}

	private Dialog createDialogRegister() {
		// start the service if it was not running already
		if (this.service == null) {
			final Intent serviceIntent = new Intent(ISenseService.class.getName());
			bindService(serviceIntent, this.serviceConn, BIND_AUTO_CREATE);
		}

		// create View with input fields for dialog content
		final LinearLayout register = new LinearLayout(this);
		register.setOrientation(LinearLayout.VERTICAL);
		final EditText emailField = new EditText(this);
		emailField.setLayoutParams(new LayoutParams(-1, -2));
		emailField.setHint(R.string.dialog_reg_hint_mail);
		emailField.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		emailField.setImeOptions(EditorInfo.IME_ACTION_NEXT);
		register.addView(emailField);
		final EditText nameField = new EditText(this);
		nameField.setLayoutParams(new LayoutParams(-1, -2));
		nameField.setHint(R.string.dialog_reg_hint_name);
		nameField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
		nameField.setImeOptions(EditorInfo.IME_ACTION_NEXT);
		register.addView(nameField);
		final EditText passField1 = new EditText(this);
		passField1.setLayoutParams(new LayoutParams(-1, -2));
		passField1.setHint(R.string.dialog_reg_hint_pass);
		passField1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		passField1.setTransformationMethod(new PasswordTransformationMethod());
		passField1.setImeOptions(EditorInfo.IME_ACTION_NEXT);
		register.addView(passField1);
		final EditText passField2 = new EditText(this);
		passField2.setLayoutParams(new LayoutParams(-1, -2));
		passField2.setHint(R.string.dialog_reg_hint_pass2);
		passField2.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		passField2.setTransformationMethod(new PasswordTransformationMethod());
		passField2.setImeOptions(EditorInfo.IME_ACTION_DONE);
		register.addView(passField2);

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dialog_reg_title);
		builder.setView(register);
		builder.setPositiveButton(R.string.button_reg, new OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				final String email = emailField.getText().toString();
				final String name = nameField.getText().toString();
				final String pass1 = passField1.getText().toString();
				final String pass2 = passField2.getText().toString();

				if (pass1.equals(pass2)) {
					String MD5Pass = "";
					// Register to the Database
					final byte[] defaultBytes = pass1.getBytes();
					try {
						final MessageDigest algorithm = MessageDigest.getInstance("MD5");
						algorithm.reset();
						algorithm.update(defaultBytes);
						final byte messageDigest[] = algorithm.digest();

						final StringBuffer hexString = new StringBuffer();
						for (final byte element : messageDigest) {
							final String hex = Integer.toHexString(0xFF & element);
							if (hex.length() == 1) {
								hexString.append(0);
							}
							hexString.append(hex);
						}
						MD5Pass = hexString.toString();

						// store the login value
						final SharedPreferences prefs = getSharedPreferences(PRIVATE_PREFS,
								MODE_PRIVATE);
						final Editor editor = prefs.edit();
						editor.putString(PREF_LOGIN_MAIL, email);
						editor.putString(PREF_LOGIN_NAME, name);
						editor.putString(PREF_LOGIN_PASS, MD5Pass);
						editor.commit();
						// start registration
						new CheckRegisterTask().execute();
					} catch (final NoSuchAlgorithmException e) {
						e.printStackTrace();
					}
				} else {
					Toast.makeText(SenseSettings.this, R.string.toast_reg_pass, Toast.LENGTH_SHORT)
					.show();

					passField1.setText("");
					passField2.setText("");
					removeDialog(DIALOG_REGISTER);
					showDialog(DIALOG_REGISTER);
				}
			}
		});
		builder.setNeutralButton(R.string.button_cancel, null);
		return builder.create();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		final Preference loginPref = findPreference(PREF_LOGIN);
		loginPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			public boolean onPreferenceClick(Preference preference) {
				showDialog(DIALOG_LOGIN);
				return true;
			}
		});

		final Preference registerPref = findPreference(PREF_REGISTER);
		registerPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			public boolean onPreferenceClick(Preference preference) {
				showDialog(DIALOG_REGISTER);
				return true;
			}
		});

		final Preference popQuizRefresh = findPreference(PREF_QUIZ_SYNC);
		popQuizRefresh.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			public boolean onPreferenceClick(Preference preference) {
				// start quiz sync broadcast
				final Intent refreshIntent = new Intent(
						"nl.sense_os.service.AlarmPopQuestionUpdate");
				final PendingIntent refreshPI = PendingIntent.getBroadcast(SenseSettings.this, 0,
						refreshIntent, 0);
				final AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
				mgr.set(AlarmManager.RTC_WAKEUP, 0, refreshPI);

				// show confirmation Toast
				Toast.makeText(SenseSettings.this, R.string.toast_quiz_refresh, Toast.LENGTH_LONG)
				.show();

				return true;
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;

		switch (id) {
		case DIALOG_LOGIN:
			dialog = createDialogLogin();
			break;
		case DIALOG_REGISTER:
			dialog = createDialogRegister();
			break;
		case DIALOG_PROGRESS:
			dialog = createDialogLoginProgress();
			break;
		default:
			dialog = super.onCreateDialog(id);
			break;
		}
		return dialog;
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (true == this.isServiceBound) {
			unbindService(this.serviceConn);
			this.service = null;
			this.isServiceBound = false;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (this.service == null) {
			Log.d(TAG, "onResume. Trying to bind to service...");

			// do not use BIND_AUTO_CREATE so the service is only bound if it already exists
			final Intent serviceIntent = new Intent(ISenseService.class.getName());
			this.isServiceBound = bindService(serviceIntent, this.serviceConn, 0);

			Log.d(TAG, "onResume. Service bound " + this.isServiceBound);
		}
	}
}
