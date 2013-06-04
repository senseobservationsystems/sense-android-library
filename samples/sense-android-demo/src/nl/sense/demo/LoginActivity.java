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
 * Activity which displays a login screen to the user, offering registration as well.
 */
public class LoginActivity extends Activity {

    /**
     * The default email to populate the email field with.
     */
    public static final String EXTRA_EMAIL = "com.example.android.authenticatordemo.extra.EMAIL";

    private static final String TAG = "LoginActivity";

    // Values for email and password at the time of the login attempt
    private String mEmail;
    private String mPassword;

    // UI references
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mLoginFormView;
    private View mLoginStatusView;
    private TextView mLoginStatusMessageView;

    // Sense specific members
    private SenseApplication mApplication;
    private ISenseServiceCallback mServiceCallback = new ISenseServiceCallback.Stub() {

        @Override
        public void onChangeLoginResult(int result) throws RemoteException {

            busy = false;

            if (result == -2) {
                // login forbidden
                onLoginFailure(true);

            } else if (result == -1) {
                // login failed
                onLoginFailure(false);

            } else {
                onLoginSuccess();
            }
        }

        @Override
        public void onRegisterResult(int result) throws RemoteException {
            // not used
        }

        @Override
        public void statusReport(int status) throws RemoteException {
            // not used
        }
    };

    private boolean busy;

    /**
     * Attempts to sign in or register the account specified by the login form. If there are form
     * errors (invalid email, missing fields, etc.), the errors are presented and no actual login
     * attempt is made.
     */
    private void attemptLogin() {
        if (busy) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        mEmail = mEmailView.getText().toString();
        mPassword = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(mEmail)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
            showProgress(true);

            // log in (you only need to do this once, Sense will remember the login)
            try {
                mApplication.getSensePlatform().login(mEmail, SenseApi.hashPassword(mPassword),
                        mServiceCallback);
                // this is an asynchronous call, we get a callback when the login is complete
                busy = true;
            } catch (IllegalStateException e) {
                Log.w(TAG, "Failed to log in at SensePlatform!", e);
                onLoginFailure(false);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to log in at SensePlatform!", e);
                onLoginFailure(false);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        // the activity needs to be part of a SenseApplication so it can talk to the SensePlatform
        mApplication = (SenseApplication) getApplication();

        // Set up the login form.
        mEmail = getIntent().getStringExtra(EXTRA_EMAIL);
        mEmailView = (EditText) findViewById(R.id.email);
        mEmailView.setText(mEmail);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mLoginStatusView = findViewById(R.id.login_status);
        mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
    }

    private void onLoginFailure(final boolean forbidden) {

        // update UI
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                showProgress(false);

                if (forbidden) {
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                    mPasswordView.requestFocus();
                } else {
                    Toast.makeText(LoginActivity.this, R.string.login_failure, Toast.LENGTH_LONG)
                            .show();
                }
            }
        });
    }

    private void onLoginSuccess() {

        // update UI
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                showProgress(false);
                Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_LONG)
                        .show();
            }
        });

        setResult(RESULT_OK);
        finish();
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginStatusView.setVisibility(View.VISIBLE);
            mLoginStatusView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });

            mLoginFormView.setVisibility(View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
