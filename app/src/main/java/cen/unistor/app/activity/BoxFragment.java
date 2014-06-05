package cen.unistor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.box.boxandroidlibv2.BoxAndroidClient;
import com.box.boxandroidlibv2.activities.OAuthActivity;
import com.box.boxandroidlibv2.dao.BoxAndroidOAuthData;
import com.box.boxandroidlibv2.jsonparsing.AndroidBoxResourceHub;
import com.box.boxjavalibv2.authorization.OAuthRefreshListener;
import com.box.boxjavalibv2.dao.IAuthData;
import com.box.boxjavalibv2.jsonparsing.BoxJSONParser;
import com.box.boxjavalibv2.jsonparsing.IBoxJSONParser;
import com.box.boxjavalibv2.jsonparsing.IBoxResourceHub;

import org.apache.commons.lang.StringUtils;

import cen.unistor.app.R;
import cen.unistor.app.asynctask.UploadFileAsyncTask.*;
import cen.unistor.app.util.Constants;

/**
 * Created by carlos on 5/06/14.
 */
public class BoxFragment extends UnistorFragment implements OnUploadFinishedListener {

    private final String TAG = "BoxFragment";

    private final String CLIENT_ID = "trybv0asmi4v693ildrqpakn1642x5q5";
    private final String CLIENT_SECRET = "jBhlEPtO8Hat0WSczsDx9PVmetjJfMjd";

    private final String AUTH_KEY = "AUTH_KEY";
    private final static int AUTH_REQUEST = 1;
    private final static int UPLOAD_REQUEST = 2;
    private final static int DOWNLOAD_REQUEST = 3;

    private Context mContext;
    private BoxAndroidClient mBoxClient;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mContext = rootView.getContext();

        startAuthentication();

        return rootView;
    }

    public void startAuthentication(){
        Intent intent = OAuthActivity.createOAuthActivityIntent(mContext, CLIENT_ID, CLIENT_SECRET);
        this.startActivityForResult(intent, AUTH_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case AUTH_REQUEST:
                BoxAndroidOAuthData oauth = data.getParcelableExtra(OAuthActivity.BOX_CLIENT_OAUTH);
                this.mBoxClient = new BoxAndroidClient(CLIENT_ID, CLIENT_SECRET, null, null, null);
                this.mBoxClient.authenticate(oauth);
                this.mBoxClient.addOAuthRefreshListener(new OAuthRefreshListener() {

                    @Override
                    public void onRefresh(IAuthData newAuthData) {
                        saveAuth((BoxAndroidOAuthData) newAuthData);
                    }

                });

                break;
        }
    }

    @Override
    public void onUploadFinish() {

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



    private void saveAuth(BoxAndroidOAuthData auth) {
        try {
            IBoxJSONParser parser = getJSONParser();
            String authString = parser.convertBoxObjectToJSONString(auth);
            mContext.getSharedPreferences(Constants.PREFS_NAME, 0).edit().putString(AUTH_KEY, authString).commit();
        }
        catch (Exception e) {
        }
    }

    private BoxAndroidOAuthData loadSavedAuth() {
        String authString = mContext.getSharedPreferences(Constants.PREFS_NAME, 0).getString(AUTH_KEY, "");
        if (StringUtils.isNotEmpty(authString)) {
            try {
                IBoxJSONParser parser = getJSONParser();
                BoxAndroidOAuthData auth = parser.parseIntoBoxObject(authString, BoxAndroidOAuthData.class);
                return auth;
            }
            catch (Exception e) {
                // failed, null will be returned. You can also add more logging, error handling here.
            }
        }
        return null;
    }

    public IBoxJSONParser getJSONParser() {
        return new BoxJSONParser(getResourceHub());
    }

    public IBoxResourceHub getResourceHub() {
        return new AndroidBoxResourceHub();
    }
}
