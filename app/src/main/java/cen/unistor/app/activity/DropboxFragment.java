package cen.unistor.app.activity;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

import cen.unistor.app.R;
import cen.unistor.app.adapter.UnistorEntry;
import cen.unistor.app.adapter.UnistorEntryListAdapter;
import cen.unistor.app.adapter.ViewHolder;
import cen.unistor.app.asynctask.DownloadFileAsyncTask;
import cen.unistor.app.asynctask.UploadFileAsyncTask;
import cen.unistor.app.util.Constants;
import cen.unistor.app.util.ContentStatus;
import cen.unistor.app.util.UnistorEntryComparator;

/**
 * Created by carlos on 10/05/14.
 */
public class DropboxFragment extends UnistorFragment implements UploadFileAsyncTask.OnUploadFinishedListener{



    private final String TAG = "DropboxFragment";

    private static final String APP_KEY = "t820v1xgtrvep1l";
    private static final String APP_SECRET = "m1aki6lwux6phy5";


    private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
    private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";

    private DropboxAPI<AndroidAuthSession> mDBApi;

    private boolean loggedIn;

    private Context mContext;

    private Stack<ContentStatus> statusHistory;
    // Used to minimize api calls
    private ArrayList<UnistorEntry> currentContent;
    private String currentHash;
    private String currentPath;

    //TODO mErrorMsg is useful?
    private String mErrorMsg;

    private ListView listView;

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
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        this.mContext = rootView.getContext();

        listView = (ListView)rootView.findViewById(R.id.listView);

        if (savedInstanceState != null){
            currentContent = savedInstanceState.getParcelableArrayList("content");
            //statusHistory = savedInstanceState.put
        }else{
            this.init();
        }

        // Only load content if session has been correctly established
        if(mDBApi.getSession().isLinked()) {
            //Load root content
            if (currentContent == null || currentContent.size() == 0) {
                currentContent = loadContent(this.currentPath);
            }
            populateContentListView(currentContent);

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
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("content", this.currentContent);
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
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
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
            edit.putString(ACCESS_KEY_NAME, "oauth2:");
            edit.putString(ACCESS_SECRET_NAME, oauth2AccessToken);
            edit.commit();
            return;
        }
        // Store the OAuth 1 access token, if there is one.  This is only necessary if
        // you're still using OAuth 1.
        AccessTokenPair oauth1AccessToken = session.getAccessTokenPair();
        if (oauth1AccessToken != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences(Constants.PREFS_NAME, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, oauth1AccessToken.key);
            edit.putString(ACCESS_SECRET_NAME, oauth1AccessToken.secret);
            edit.commit();
            return;
        }
    }


    private void logOut() {
        // Remove credentials from the session
        mDBApi.getSession().unlink();

        // Clear our stored keys
        clearKeys();
        // Change UI state to display logged out version
        //setLoggedIn(false);
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
    private ArrayList<UnistorEntry> loadContent(String path){
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
            mErrorMsg = mContext.getString(R.string.dropbox_excp_msg);
        }
        return entryList;
    }

    /**
     *  Fills the listview with the content provided.
     * @param content
     */
    private void populateContentListView(ArrayList<UnistorEntry> content){

        // If first entry is back button, the content array
        // is sorted without this item, which will be added in the first position
        if(content.get(0).getEntryType() == Constants.ENTRY_TYPE_BACK){
            UnistorEntry backEntry = content.remove(0);
            Collections.sort(content, new UnistorEntryComparator());
            content.add(0, backEntry);
        }else{
            Collections.sort(content, new UnistorEntryComparator());
        }

        // Setting the adapter with the new items.
        // If the adapter have been previously created, we use notifyDataSetChanged,
        // which uses pretty less resources than creating a new one.
        if(this.listView.getAdapter() == null){
            UnistorEntryListAdapter listViewAdapter = new UnistorEntryListAdapter(this.mContext, content);
            this.listView.setAdapter(listViewAdapter);
            // Set context menu for the listview
            registerForContextMenu(listView);
        }else{
            UnistorEntryListAdapter listViewAdapter = (UnistorEntryListAdapter)this.listView.getAdapter();
            listViewAdapter.clear();
            listViewAdapter.addAll(content);
            listViewAdapter.notifyDataSetChanged();
        }

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // Gets the itemView in the adapter
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        View view = info.targetView;
        ViewHolder itemViewHolder = (ViewHolder)view.getTag();

        switch (item.getItemId()){
            case 0:// Copy
                ((MainActivity)getActivity()).setPathToCopy(itemViewHolder.getEntry().getPath());
                getActivity().invalidateOptionsMenu();
                break;
            case 1:// Move
                ((MainActivity)getActivity()).setPathToMove(itemViewHolder.getEntry().getPath());
                getActivity().invalidateOptionsMenu();
                break;
            case 2:// Delete
                this.deleteElement(itemViewHolder.getEntry().getPath());
                currentContent = loadContent(currentPath);
                populateContentListView(currentContent);
                break;
        }

        Toast.makeText(mContext,((MainActivity)getActivity()).getPathToCopyMove(),Toast.LENGTH_LONG).show();
        return true;
    }




    private void deleteElement(String path){

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
            mErrorMsg = mContext.getString(R.string.dropbox_excp_msg);
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
        UploadFileAsyncTask task = new UploadFileAsyncTask(mContext, mDBApi, path, currentPath, this);
        task.execute();
        return true;
    }

    /**
     * Listener to refresh data when upload finishes
     */
    @Override
    public void onUploadFinish() {
        currentContent = this.loadContent(currentPath);
        populateContentListView(currentContent);
    }


    @Override
    public boolean pasteFile(String source, int mode) {
        boolean result = false;
        try {
            String namefile = source.substring(source.lastIndexOf('/'));
            String dest = this.currentPath.concat(namefile);

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
            mErrorMsg = mContext.getString(R.string.dropbox_excp_msg);
        }

        if(mErrorMsg != null){
            Toast.makeText(mContext,mErrorMsg, Toast.LENGTH_LONG).show();
        }

        getActivity().invalidateOptionsMenu();
        return result;
    }
}
