package cen.unistor.app.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;

import cen.unistor.app.R;

/**
 * Created by carlos on 3/06/14.
 */
public class GDriveFragment extends UnistorFragment {
    private GoogleApiClient mGoogleApiClient;
    private Context mContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        this.mContext = rootView.getContext();

//        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
//                .addApi(Drive.API)
//                .addScope(new Scope("https://www.googleapis.com/auth/drive"))
//                .addConnectionCallbacks(mContext)
//                .addOnConnectionFailedListener(mContext)
//                .build();

        return rootView;
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
