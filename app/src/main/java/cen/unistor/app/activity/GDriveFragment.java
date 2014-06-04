package cen.unistor.app.activity;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.query.Query;

import android.content.IntentSender.*;



import cen.unistor.app.R;

/**
 * Created by carlos on 3/06/14.
 */
public class GDriveFragment extends UnistorFragment
        implements ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private String TAG = "GDriveFragment";
    private GoogleApiClient mGoogleApiClient;
    private Context mContext;
    private boolean mResolvingError;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final int RESULT_OK = 1;
    private static final int REQUEST_RESOLVE_ERROR = 2;
    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 3;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        this.mContext = rootView.getContext();



        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addScope(Drive.SCOPE_APPFOLDER)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();
    }



    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "API client connected.");

        Toast.makeText(mContext,"conectado", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(getActivity(), RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), getActivity(), 0).show();
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient.connect();

                    DriveFolder folder = Drive.DriveApi.getRootFolder(mGoogleApiClient);

                    folder.listChildren(mGoogleApiClient).setResultCallback(childrenRetrievedCallback);
                }
                break;
        }
    }

    ResultCallback<DriveApi.MetadataBufferResult> childrenRetrievedCallback = new
            ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(DriveApi.MetadataBufferResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Toast.makeText(mContext, "Problem while retrieving files", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Toast.makeText(mContext, "Results: "+result.getMetadataBuffer().getCount(), Toast.LENGTH_LONG).show();
                }
            };

    @Override
    public boolean keyBackPressed() {
        return false;
    }

    @Override
    public boolean uploadFile(String path) {
        return false;
    }

    @Override
    public boolean pasteFile(String source, int mode) {
        return false;
    }

}
