package cen.unistor.app.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.util.ArrayList;
import java.util.Stack;
import java.util.UUID;

import cen.unistor.app.R;
import cen.unistor.app.adapter.UnistorEntry;
import cen.unistor.app.adapter.ViewHolder;
import cen.unistor.app.asynctask.DownloadFileAsyncTask;
import cen.unistor.app.asynctask.UploadFileAsyncTask;
import cen.unistor.app.util.Constants;
import cen.unistor.app.util.ContentStatus;

/**
 * Created by carlos on 10/05/14.
 */
public class DropboxFragment extends UnistorFragment{


    private final String TAG = "DropboxFragment";
    private static final String APP_KEY = "t820v1xgtrvep1l";
    private static final String APP_SECRET = "m1aki6lwux6phy5";


    private static final String DROPBOX_ACCESS_KEY = "DROPBOX_ACCESS_KEY";
    private static final String DROPBOX_ACCESS_SECRET = "DROPBOX_ACCESS_SECRET";

    private DropboxAPI<AndroidAuthSession> mDBApi;

    private boolean loggedIn;

    private String currentHash;


    //TODO mErrorMsg is useful?
    private String mErrorMsg;


    @Override
    public String getTitle() {
        return "Dropbox";
    }

    private void init(){

        // Start application in the root path
        this.currentPath = "/";
        this.statusHistory = new Stack<ContentStatus>();
        AndroidAuthSession session = buildSession();
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);

        if(!mDBApi.getSession().isLinked()){
            mDBApi.getSession().startOAuth2Authentication(this.getActivity().getApplicationContext());
        }

        loggedIn = mDBApi.getSession().isLinked();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ViewHolder holder = (ViewHolder) view.getTag();
                if (holder.getEntry().getName().contains(".apk")) {
                    Toast.makeText(mContext, R.string.open_apk_error, Toast.LENGTH_LONG).show();
                } else if (!holder.getEntry().isFolder()) {
                    DownloadFileAsyncTask downloadTask
                            = new DownloadFileAsyncTask(mContext, mDBApi, holder.getEntry().getPath(), holder.getEntry().getName());
                    downloadTask.execute();
                } else {
                    //TODO backButton historial o parentPath?? --> parentPath implica consumoDAtos
                    ContentStatus currentStatus = new ContentStatus(
                            new ArrayList<UnistorEntry>(currentContent),
                            new String(currentHash),
                            new String(currentPath));
                    statusHistory.push(currentStatus);
                    currentContent = loadContent(holder.getEntry().getPath());
                    populateContentListView(currentContent);
                }

            }
        });
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);


        this.mContext = rootView.getContext();

        listView = (ListView)rootView.findViewById(R.id.listView);


        this.init();


        if (savedInstanceState != null){
            currentContent = savedInstanceState.getParcelableArrayList("currentContent");
            currentPath = savedInstanceState.getString("currentPath");
            currentHash = savedInstanceState.getString("currentHash");
            //statusHistory = savedInstanceState.put
        }

        // Only load content if session has been correctly established
        if(mDBApi.getSession().isLinked()) {
            //Load root content
            if (currentContent == null || currentContent.size() == 0) {
                currentContent = loadContent(this.currentPath);
            }
            populateContentListView(currentContent);


        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();
                loggedIn = true;
                // Store it locally in our app for later use
                storeAuth(mDBApi.getSession());

                if(currentContent == null || currentContent.isEmpty()) {
                    currentContent = loadContent(this.currentPath);

                }
                populateContentListView(currentContent);
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentHash", currentHash);
    }

    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);

        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        loadAuth(session);
        return session;
    }

    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     */
    private void loadAuth(AndroidAuthSession session) {
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.PREFS_NAME, 0);
        String key = prefs.getString(DROPBOX_ACCESS_KEY, null);
        String secret = prefs.getString(DROPBOX_ACCESS_SECRET, null);
        if (key == null || secret == null || key.length() == 0 || secret.length() == 0) return;

        if (key.equals("oauth2:")) {
            // If the key is set to "oauth2:", then we can assume the token is for OAuth 2.
            session.setOAuth2AccessToken(secret);
        } else {
            // Still support using old OAuth 1 tokens.
            session.setAccessTokenPair(new AccessTokenPair(key, secret));
        }
    }

    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     */
    private void storeAuth(AndroidAuthSession session) {
        // Store the OAuth 2 access token, if there is one.
        String oauth2AccessToken = session.getOAuth2AccessToken();
        if (oauth2AccessToken != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences(Constants.PREFS_NAME, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(DROPBOX_ACCESS_KEY, "oauth2:");
            edit.putString(DROPBOX_ACCESS_SECRET, oauth2AccessToken);
            edit.commit();
            return;
        }
        // Store the OAuth 1 access token, if there is one.  This is only necessary if
        // you're still using OAuth 1.
        AccessTokenPair oauth1AccessToken = session.getAccessTokenPair();
        if (oauth1AccessToken != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences(Constants.PREFS_NAME, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(DROPBOX_ACCESS_KEY, oauth1AccessToken.key);
            edit.putString(DROPBOX_ACCESS_SECRET, oauth1AccessToken.secret);
            edit.commit();
            return;
        }
    }


    public void logOut() {
        // Remove credentials from the session
        mDBApi.getSession().unlink();

        // Clear our stored keys
        clearKeys();
        // Change UI state to display logged out version
        loggedIn = false;


        Log.i(TAG, "logout");
    }

    private void clearKeys() {
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.PREFS_NAME, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.clear();
        edit.commit();
    }

    /**
     * Gets the content allocated in the path provided.
     * @param path
     * @return
     */
    @Override
    protected ArrayList<UnistorEntry> loadContent(String path){
        DropboxAPI.Entry root = null;
        ArrayList<UnistorEntry> entryList = new ArrayList<UnistorEntry>();
        try {
            root = mDBApi.metadata(path, 0, null, true, null);

            UnistorEntry tmp;

            //Saving hash for the current content
            currentHash = root.hash;

            // Building back entry into the entry list
            if(!root.parentPath().equals("") && !root.contents.isEmpty()){
                tmp = new UnistorEntry();
                if(root.parentPath().equals("/")){
                    tmp.setName("Volver a Carpeta Raíz");
                }else{
                    tmp.setName("Volver a "+root.parentPath().substring(root.parentPath().lastIndexOf('/')+1));
                }

                tmp.setPath(root.parentPath());
                tmp.setFolder(true);
                tmp.setEntryType(Constants.ENTRY_TYPE_BACK);
                entryList.add(tmp);
            }

            // Building UnistorEntry array with metadata result.
            for(DropboxAPI.Entry entry: root.contents){

                tmp = new UnistorEntry();
                if(entry.isDir){
                    tmp.setName(entry.path.substring(entry.path.lastIndexOf('/')+1));
                    tmp.setEntryType(Constants.ENTRY_TYPE_FOLDER);
                }else{
                    tmp.setName(entry.fileName());
                    tmp.setEntryType(Constants.ENTRY_TYPE_FILE);
                    tmp.setSizeString(entry.size);
                    tmp.setLastModification(entry.modified);
                }
                tmp.setPath(entry.path);
                tmp.setFolder(entry.isDir);


                entryList.add(tmp);
            }

            currentPath = path;
        } catch (DropboxUnlinkedException e) {
            // The AuthSession wasn't properly authenticated or user unlinked.
        } catch (DropboxPartialFileException e) {
            // We canceled the operation
            mErrorMsg = "Download canceled";
        } catch (DropboxServerException e) {
            // Server-side exception.  These are examples of what could happen,
            // but we don't do anything special with them here.
            if (e.error == DropboxServerException._304_NOT_MODIFIED) {
                // won't happen since we don't pass in revision with metadata
            } else if (e.error == DropboxServerException._401_UNAUTHORIZED) {
                // Unauthorized, so we should unlink them.  You may want to
                // automatically log the user out in this case.
                //TODO reautenticar
            } else if (e.error == DropboxServerException._403_FORBIDDEN) {
                // Not allowed to access this
                mErrorMsg = mContext.getString(R.string.dropbox_403_forbidden_msg);
            } else if (e.error == DropboxServerException._404_NOT_FOUND) {
                // path not found (or if it was the thumbnail, can't be
                // thumbnailed)
            } else if (e.error == DropboxServerException._406_NOT_ACCEPTABLE) {
                // too many entries to return
            } else if (e.error == DropboxServerException._415_UNSUPPORTED_MEDIA) {
                // can't be thumbnailed
            } else if (e.error == DropboxServerException._507_INSUFFICIENT_STORAGE) {
                // user is over quota
                mErrorMsg = mContext.getString(R.string.dropbox_507_insufficient_storage_msg);
            } else {
                // Something else
            }
            // This gets the Dropbox error, translated into the user's language
            mErrorMsg = e.body.userError;
            if (mErrorMsg == null) {
                mErrorMsg = e.body.error;
            }
        } catch (DropboxIOException e) {
            // Happens all the time, probably want to retry automatically.
            mErrorMsg = mContext.getString(R.string.dropbox_IO_excp_msg);
        } catch (DropboxParseException e) {
            // Probably due to Dropbox server restarting, should retry
            mErrorMsg = mContext.getString(R.string.dropbox_parse_excp_msg);
        } catch (DropboxException e) {
            // Unknown error
            mErrorMsg = mContext.getString(R.string.excp_msg);
        }
        return entryList;
    }



    @Override
    protected void deleteElement(String path){

        try {
            mDBApi.delete(path);
        } catch (DropboxUnlinkedException e) {
            // The AuthSession wasn't properly authenticated or user unlinked.
        } catch (DropboxServerException e) {
            // Server-side exception.  These are examples of what could happen,
            // but we don't do anything special with them here.
            if (e.error == DropboxServerException._304_NOT_MODIFIED) {
                // won't happen since we don't pass in revision with metadata
            } else if (e.error == DropboxServerException._401_UNAUTHORIZED) {
                // Unauthorized, so we should unlink them.  You may want to
                // automatically log the user out in this case.
                //TODO reautenticar
            } else if (e.error == DropboxServerException._403_FORBIDDEN) {
                // Not allowed to access this
                mErrorMsg = mContext.getString(R.string.dropbox_403_forbidden_msg);
            } else if (e.error == DropboxServerException._404_NOT_FOUND) {
                // path not found (or if it was the thumbnail, can't be
                // thumbnailed)
            } else if (e.error == DropboxServerException._406_NOT_ACCEPTABLE) {
                // too many entries to return
            } else if (e.error == DropboxServerException._415_UNSUPPORTED_MEDIA) {
                // can't be thumbnailed
            } else if (e.error == DropboxServerException._507_INSUFFICIENT_STORAGE) {
                // user is over quota
                mErrorMsg = mContext.getString(R.string.dropbox_507_insufficient_storage_msg);
            } else {
                // Something else
            }
            // This gets the Dropbox error, translated into the user's language
            mErrorMsg = e.body.userError;
            if (mErrorMsg == null) {
                mErrorMsg = e.body.error;
            }
        } catch (DropboxIOException e) {
            // Happens all the time, probably want to retry automatically.
            mErrorMsg = mContext.getString(R.string.dropbox_IO_excp_msg);
        } catch (DropboxParseException e) {
            // Probably due to Dropbox server restarting, should retry
            mErrorMsg = mContext.getString(R.string.dropbox_parse_excp_msg);
        } catch (DropboxException e) {
            // Unknown error
            mErrorMsg = mContext.getString(R.string.excp_msg);
        }
    }

    @Override
    public boolean keyBackPressed(){
        Log.i("DropboxFragment", "back pressed. History "+statusHistory.size());

        if(!statusHistory.empty()){
            ContentStatus lastStatus = this.statusHistory.pop();
            this.currentContent = lastStatus.getContent();
            this.currentHash = lastStatus.getStatusHash();
            this.currentPath = lastStatus.getPath();
            this.populateContentListView(currentContent);
            return true;
        }
        return false;
    }


    @Override
    public boolean uploadFile(String path) {
        UploadFileAsyncTask task = new UploadFileAsyncTask(mContext, mDBApi, new File(path), currentPath, this);
        task.execute();
        return true;
    }




    @Override
    public boolean pasteFile(String source, String namefile, int mode) {
        boolean result = false;
        try {

            String dest = this.currentPath.equals("/") ?
                            this.currentPath.concat(namefile) : this.currentPath.concat("/" + namefile);

            if( source.equals(dest) ) {
                namefile = getString(R.string.copy_prefix) + namefile;
                dest = this.currentPath.equals("/") ?
                        this.currentPath.concat(namefile) : this.currentPath.concat("/" + namefile);
            }

            switch (mode) {
                case Constants.ACTION_COPY:
                    mDBApi.copy(source, dest);
                    break;
                case Constants.ACTION_MOVE:
                    mDBApi.move(source, dest);
                    break;
            }

            // Refresh the current view to reflect the changes
            this.currentContent = loadContent(this.currentPath);
            populateContentListView(this.currentContent);
            result = true;
        }catch (DropboxServerException e) {
            switch (e.error){
                case DropboxServerException._401_UNAUTHORIZED:
                    // Unauthorized, so we should unlink them.  You may want to
                    // automatically log the user out in this case.
                    //TODO reautenticar

                    break;

                case DropboxServerException._403_FORBIDDEN:
                    // Not allowed to access this
                    mErrorMsg = mContext.getString(R.string.dropbox_403_forbidden_msg);
                    break;

                case DropboxServerException._404_NOT_FOUND:
                    // path not found (or if it was the thumbnail, can't be
                    // thumbnailed)
                    mErrorMsg = mContext.getString(R.string.dropbox_404_not_found);
                    break;

                case DropboxServerException._502_BAD_GATEWAY:
                    mErrorMsg = mContext.getString(R.string.dropbox_502_bad_gateway);
                    break;

                case DropboxServerException._503_SERVICE_UNAVAILABLE:
                    mErrorMsg = mContext.getString(R.string.dropbox_503_service_unavailable);
                    break;

                case DropboxServerException._507_INSUFFICIENT_STORAGE:
                    mErrorMsg = mContext.getString(R.string.dropbox_507_insufficient_storage_msg);
                    break;

            }
        }catch (DropboxUnlinkedException e){
            //TODO
        } catch (DropboxIOException e) {
            // Happens all the time, probably want to retry automatically.
            mErrorMsg = mContext.getString(R.string.dropbox_IO_excp_msg);
        }catch (DropboxException e) {
            // Unknown error
            mErrorMsg = mContext.getString(R.string.excp_msg);
        }

        if(mErrorMsg != null){
            Toast.makeText(mContext,mErrorMsg, Toast.LENGTH_LONG).show();
        }

        getActivity().invalidateOptionsMenu();
        return result;
    }


}
