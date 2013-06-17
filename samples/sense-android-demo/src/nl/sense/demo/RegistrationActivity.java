package nl.sense.demo;

import nl.sense_os.platform.SenseApplication;
import nl.sense_os.service.ISenseServiceCallback;
import nl.sense_os.service.commonsense.SenseApi;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity which displays a registration screen to the user.
 */
public class RegistrationActivity extends Activity {

    private static final String TAG = "RegistrationActivity";

    // Values for email and password at the time of the registration attempt.
    private String mEmail;
    private String mPassword;
    private String mPasswordConfirmation;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mRegistrationFormView;
    private EditText mConfirmPasswordView;
    private TextView mRegistrationStatusMessageView;
    private View mRegistrationStatusView;

    private SenseApplication mApplication;
    private boolean mBusy;
    private ISenseServiceCallback mSenseCallback = new ISenseServiceCallback.Stub() {

        @Override
        public void onChangeLoginResult(int result) throws RemoteException {
            // not used
        }

        @Override
        public void onRegisterResult(int result) throws RemoteException {
            mBusy = false;

            if (result == -2) {
                // registration forbidden
                onRegistrationFailure(true);

            } else if (result == -1) {
                // registration failed
                onRegistrationFailure(false);

            } else {
                onRegistrationSuccess();
            }
        }

        @Override
        public void statusReport(int status) throws RemoteException {
            // not used
        }
    };

    /**
     * Attempts to register the account specified by the registration form. If there are form errors
     * (invalid email, missing fields, etc.), the errors are presented and no actual registration
     * attempt is made.
     */
    public void attemptRegistration() {
        if (mBusy) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the registration attempt.
        mEmail = mEmailView.getText().toString();
        mPassword = mPasswordView.getText().toString();
        mPasswordConfirmation = mConfirmPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid password confirmation.
        if (TextUtils.isEmpty(mPasswordConfirmation)) {
            mConfirmPasswordView.setError(getString(R.string.error_field_required));
            focusView = mConfirmPasswordView;
            cancel = true;
        } else if (!mPassword.equals(mPasswordConfirmation)) {
            mConfirmPasswordView.setError(getString(R.string.error_password_match));
            focusView = mConfirmPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(mEmail)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt registration and focus the first form field with an
            // error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to perform the user
            // registration attempt.
            mRegistrationStatusMessageView.setText(R.string.progress_registering);
            showProgress(true);

            // register using SensePlatform
            try {
                mApplication.getSensePlatform().registerUser(mEmail,
                        SenseApi.hashPassword(mPassword), mEmail, null, null, null, null, null,
                        null, mSenseCallback);
                // this is an asynchronous call, we get a callback when the registration is complete
                mBusy = true;
            } catch (IllegalStateException e) {
                Log.w(TAG, "Failed to register at SensePlatform!", e);
                onRegistrationFailure(false);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to register at SensePlatform!", e);
                onRegistrationFailure(false);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_registration);

        // the activity needs to be part of a SenseApplication so it can talk to the SensePlatform
        mApplication = (SenseApplication) getApplication();

        // Set up the registration form.
        mEmailView = (EditText) findViewById(R.id.email);

        mPasswordView = (EditText) findViewById(R.id.password);
        mConfirmPasswordView = (EditText) findViewById(R.id.confirm_password);
        mConfirmPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.register || id == EditorInfo.IME_NULL) {
                    attemptRegistration();
                    return true;
                }
                return false;
            }
        });

        mRegistrationFormView = findViewById(R.id.registration_form);
        mRegistrationStatusView = findViewById(R.id.registration_status);
        mRegistrationStatusMessageView = (TextView) findViewById(R.id.registration_status_message);

        findViewById(R.id.registration_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegistration();
            }
        });
    }

    private void onRegistrationFailure(final boolean forbidden) {

        // update UI
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                showProgress(false);

                if (forbidden) {
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                    mPasswordView.requestFocus();
                } else {
                    Toast.makeText(RegistrationActivity.this, R.string.register_failure,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void onRegistrationSuccess() {

        // update UI
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                showProgress(false);
                Toast.makeText(RegistrationActivity.this, R.string.register_success,
                        Toast.LENGTH_LONG).show();
            }
        });

        // finish registration activity
        setResult(RESULT_OK);
        finish();
    }

    /**
     * Shows the progress UI and hides the registration form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow for very easy
        // animations. If available, use these APIs to fade-in the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mRegistrationStatusView.setVisibility(View.VISIBLE);
            mRegistrationStatusView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mRegistrationStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });

            mRegistrationFormView.setVisibility(View.VISIBLE);
            mRegistrationFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mRegistrationFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show and hide the relevant
            // UI components.
            mRegistrationStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            mRegistrationFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
