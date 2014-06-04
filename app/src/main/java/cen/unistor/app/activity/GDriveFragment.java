package cen.unistor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.drive.DriveScopes;


import java.util.Iterator;

import cen.unistor.app.R;
import cen.unistor.app.adapter.ResultsAdapter;
import cen.unistor.app.adapter.UnistorEntry;

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

    private ListView listView;
    private ResultsAdapter mResultsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        this.mContext = rootView.getContext();

        listView = (ListView)rootView.findViewById(R.id.listView);
        //Credential credential = Credential.usingOAuth2(this, DriveScopes.DRIVE);
        //com.google.api.services.drive.Drive dr = new com.google.api.services.drive.Drive.Builder()
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_APPFOLDER)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }



    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "API client connected.");

        Toast.makeText(mContext,"conectado", Toast.LENGTH_LONG).show();


        Drive.DriveApi.getRootFolder(mGoogleApiClient).getMetadata(mGoogleApiClient).setResultCallback(new ResultCallback<DriveResource.MetadataResult>() {
            @Override
            public void onResult(DriveResource.MetadataResult metadataResult) {
                DriveId id = metadataResult.getMetadata().getDriveId();


                Log.i(TAG,metadataResult.getMetadata().getDriveId().toString());

            }
        });


    }

    @Override
    public void onConnectionSuspended(int i) {

        mGoogleApiClient.disconnect();

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(getActivity(), RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
                Log.e(TAG, "Unable to resolve");
                Toast.makeText(mContext,"Unable to resolve", Toast.LENGTH_LONG).show();
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

                }
                break;
        }
    }





    public void loadContent(DriveId folderID){

        Drive.DriveApi.getFolder(mGoogleApiClient, folderID).listChildren(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(DriveApi.MetadataBufferResult metadataBufferResult) {
                        if (!metadataBufferResult.getStatus().isSuccess()) {
                            Toast.makeText(mContext, "Problem while retrieving files", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Problem while retrieving files");
                            return;
                        }

                        Log.i(TAG, "Retrieved!");

                        Iterator i = metadataBufferResult.getMetadataBuffer().iterator();

                        UnistorEntry entry;
                        while (i.hasNext()) {
                            entry = new UnistorEntry();
                            Metadata metadata = (Metadata) i.next();
                            entry.setName(metadata.getTitle());
                            entry.setFolder(metadata.isFolder());
                            Log.i(TAG, metadata.getTitle());
                        }


                        mResultsAdapter = new ResultsAdapter(mContext, metadataBufferResult.getMetadataBuffer());
                        listView.setAdapter(mResultsAdapter);
                    }

                });
    }

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
