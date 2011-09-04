package nl.sense_os.phonegap.plugins;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class InstallSenseDialog {

    public static AlertDialog create(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Install Sense Platform");
        builder.setMessage("The Sense Platform is not installed on your device. This application is required to use your phone's sensors. In order to continue, you first need to install the Sense Platform app from the Android Market."
                + "\n\n"
                + "When you press 'OK', you will be redirected to the Android Market to install it.");
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent goToMarket = null;
                goToMarket = new Intent(Intent.ACTION_VIEW, Uri
                        .parse("market://details?id=nl.sense_os.app"));

                try {
                    context.startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    Log.e("Dialogs", "Could not start the Android Market app!");
                    Toast.makeText(context, "Could not start the Android Market app!",
                            Toast.LENGTH_LONG).show();
                    ((Activity) context).finish();
                }
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        return builder.create();
    }
}
