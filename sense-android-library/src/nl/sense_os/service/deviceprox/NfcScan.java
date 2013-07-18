package nl.sense_os.service.deviceprox;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.BaseDataProducer;

import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.nfc.tech.TagTechnology;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

/**
 * Activity for handling NFC scan events. Should be registered to handle intents with the NFC
 * intents in the AndroidManifest.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
@TargetApi(10)
public class NfcScan extends BaseDataProducer {

	public static class NfcDialog extends DialogFragment {

		private WeakReference<NfcFragmetActivity> listenerRef;

		public static NfcDialog create(NfcFragmetActivity nfcFragmetActivity) {
			NfcDialog dialog = new NfcDialog();
			dialog.listenerRef = new WeakReference<NfcFragmetActivity>(nfcFragmetActivity);
			return dialog;
		}

		@TargetApi(11)
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			String tagId = getArguments().getString("tagId");

			// create builder
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				// specifically set dark theme for Android 3.0+
				builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_DARK);
			}

			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setTitle(R.string.nfc_dialog_title);
			builder.setMessage(getString(R.string.nfc_dialog_msg, tagId));
			builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
				    listenerRef.get().submit();
				}
			});
			builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});

			return builder.create();
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			Log.v(TAG, "Dialog dismissed...");
			listenerRef.get().finish();
		}
	}

	private class ParseTask extends AsyncTask<Tag, Void, Boolean> {
		private String error = null;

		@Override
		protected Boolean doInBackground(Tag... params) {
			Tag tag = params[0];

			try {
				fragmentActivity.parseTag(tag);
			} catch (IllegalArgumentException e) {
				error = "Failed to parse tag: " + e;
				return false;
			} catch (ClassNotFoundException e) {
				error = "Failed to parse tag: " + e;
				return false;
			} catch (NoSuchMethodException e) {
				error = "Failed to parse tag: " + e;
				return false;
			} catch (IllegalAccessException e) {
				error = "Failed to parse tag: " + e;
				return false;
			} catch (InvocationTargetException e) {
				error = "Failed to parse tag: " + e;
				return false;
			}

			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				fragmentActivity.onParseSuccess();
			} else {
				fragmentActivity.onParseFailure(error);
			}
		}
	}

	private static final String TAG = "Sense NFC";

	private static final int NOTIF_ID = 0x06FC;

	private String tagId;
	private String tagTech;
	private String tagMsg;
	private static NfcFragmetActivity fragmentActivity;
	private class NfcFragmetActivity extends FragmentActivity	
	{
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			/* parse the tag */
			Tag tag = getIntent().<Tag> getParcelableExtra(NfcAdapter.EXTRA_TAG);
			new ParseTask().execute(tag);
		}

		@Override
		protected void onDestroy() {
			cancelNotification();
			super.onDestroy();
		}

		private void onParseFailure(String error) {
			Log.e(TAG, error);
			Toast.makeText(this, "Failed to parse tag!", Toast.LENGTH_LONG).show();
			finish();
		}

		private void onParseSuccess() {
			showNotification();
			showNfcDialog();
		}

		private void showNfcDialog() {
			NfcDialog dialog = NfcDialog.create(this);
			Bundle arguments = new Bundle();
			arguments.putString("tagId", tagId);
			dialog.setArguments(arguments);
			dialog.show(fragmentActivity.getSupportFragmentManager(), "nfc");
		}

		private void cancelNotification() {
			NotificationManager nm = (NotificationManager) getSystemService(FragmentActivity.NOTIFICATION_SERVICE);
			nm.cancel(NOTIF_ID);
		}

		private void showNotification() {
			NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
			builder.setDefaults(Notification.DEFAULT_SOUND);
			builder.setWhen(System.currentTimeMillis());
			builder.setOngoing(true);
			builder.setContentTitle(getString(R.string.stat_notify_title));
			builder.setContentText(getString(android.R.string.dialog_alert_title));
			builder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(), 0));

			NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			nm.notify(NOTIF_ID, builder.build());
		}

		private void parseTag(Tag tag) throws ClassNotFoundException, NoSuchMethodException,
		IllegalArgumentException, IllegalAccessException, InvocationTargetException {

			// get the tag ID
			tagId = "";
			for (byte idByte : tag.getId()) {
				tagId += (idByte & 0x0FF) + ":";
			}
			if (tagId.length() > 0) {
				tagId = tagId.substring(0, tagId.length() - 1);
			}

			// get the tag technology
			String[] techList = tag.getTechList();
			TagTechnology tech = null;
			for (String techClass : techList) {
				Class<?> nfcTechClass = Class.forName(techClass);
				Method getMethod = nfcTechClass.getMethod("get", Tag.class);
				tech = (TagTechnology) getMethod.invoke(null, tag);
				if (tech instanceof NfcA || tech instanceof NfcB) {
					// continue to see if there are more technology classes
				} else {
					break;
				}
			}

			// get tag content (if available)
			if (tech instanceof NfcA) {
				tagTech = "NFC-A";
			} else if (tech instanceof NfcB) {
				tagTech = "NFC-B";
			} else if (tech instanceof NfcF) {
				tagTech = "NFC-F";
			} else if (tech instanceof NfcV) {
				tagTech = "NFC-V";
			} else if (tech instanceof IsoDep) {
				tagTech = "ISO-DEP";
			} else if (tech instanceof Ndef) {
				tagTech = "NDEF";
				Ndef ndef = (Ndef) tech;
				try {
					ndef.connect();
					NdefMessage msg = ndef.getNdefMessage();
					byte[] msgBytes = msg.toByteArray();
					tagMsg = "";
					for (byte b : msgBytes) {
						String hex = "" + Integer.toHexString(b);
						if (hex.length() == 1) {
							tagMsg += "0";
						}
						tagMsg += hex;
					}
				} catch (TagLostException e) {
					Log.e(TAG, "Failed to read from tag: " + e);
				} catch (FormatException e) {
					Log.e(TAG, "Failed to read from tag: " + e);
				} catch (IOException e) {
					Log.e(TAG, "Failed to read from tag: " + e);
				} finally {
					try {
						ndef.close();
					} catch (Exception e) {
						// ignore
					}
				}
			} else if (tech instanceof NdefFormatable) {
				tagTech = "NDEF";
			} else if (tech instanceof MifareClassic) {
				tagTech = "MIFARE Classic";
				// MifareClassic mifare = (MifareClassic) tech;
				// try {
				// mifare.connect();
				// for (int i = 0; i < mifare.getBlockCount(); i++) {
				// int sector = mifare.blockToSector(i);
				// Log.d(TAG, "authenticate sector " + sector + " with KEY_DEFAULT...");
				// boolean auth = mifare.authenticateSectorWithKeyA(sector,
				// MifareClassic.KEY_DEFAULT);
				// if (!auth) {
				// Log.d(TAG, "authenticate sector " + sector + " with MAD KEY...");
				// auth = mifare.authenticateSectorWithKeyA(sector,
				// MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY);
				// }
				// if (!auth) {
				// Log.d(TAG, "authenticate sector " + sector + " with NFC FORUM KEY...");
				// auth = mifare.authenticateSectorWithKeyA(sector,
				// MifareClassic.KEY_NFC_FORUM);
				// }
				// if (auth) {
				// Log.d(TAG, "Authenticated sector: " + sector);
				// byte[] block = mifare.readBlock(i);
				// Log.d(TAG, "read block " + i + ". " + block);
				// }
				// }
				// } catch (IOException e) {
				// Log.e(TAG, "Failed to read from tag: " + e);
				// } finally {
				// try {
				// mifare.close();
				// } catch (IOException e) {
				// // ignore
				// }
				// }
			} else if (tech instanceof MifareUltralight) {
				tagTech = "MIFARE Ultralight";
			} else {
				Log.w(TAG, "Unexpected NFC tag technology: " + tech);
			}
		}

		/**
		 * Submits a new data point with the NFC info.
		 */
		private void submit() {

			// create data point value
			HashMap<String, Object> jsonFields = new HashMap<String, Object>();
			jsonFields.put("id", tagId);
			jsonFields.put("technology", tagTech);
			if (null != tagMsg) {
				jsonFields.put("message", tagMsg);
			}
			JSONObject jsonObj = new JSONObject(jsonFields); 
			String value = jsonObj.toString();

			notifySubscribers();
			SensorDataPoint sensorDataPoint = new SensorDataPoint(jsonObj);
			sensorDataPoint.sensorName = SensorNames.NFC_SCAN;
			sensorDataPoint.sensorDescription = SensorNames.NFC_SCAN;
			sensorDataPoint.timeStamp = SNTP.getInstance().getTime();        
			sendToSubscribers(sensorDataPoint);
			
			// submit value
			Intent dataPoint = new Intent(getString(R.string.action_sense_new_data));
			dataPoint.putExtra(DataPoint.SENSOR_NAME, SensorNames.NFC_SCAN);
			dataPoint.putExtra(DataPoint.SENSOR_DESCRIPTION, SensorNames.NFC_SCAN);
			dataPoint.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
			dataPoint.putExtra(DataPoint.TIMESTAMP, sensorDataPoint.timeStamp);
			dataPoint.putExtra(DataPoint.VALUE, value);
			startService(dataPoint);
		}
	}
}
