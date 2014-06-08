package cen.unistor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.box.boxandroidlibv2.BoxAndroidClient;
import com.box.boxandroidlibv2.activities.OAuthActivity;
import com.box.boxandroidlibv2.dao.BoxAndroidFile;
import com.box.boxandroidlibv2.dao.BoxAndroidOAuthData;
import com.box.boxandroidlibv2.jsonparsing.AndroidBoxResourceHub;
import com.box.boxjavalibv2.authorization.OAuthRefreshListener;
import com.box.boxjavalibv2.dao.BoxCollection;
import com.box.boxjavalibv2.dao.BoxItem;
import com.box.boxjavalibv2.dao.BoxTypedObject;
import com.box.boxjavalibv2.dao.IAuthData;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.exceptions.BoxServerException;
import com.box.boxjavalibv2.jsonparsing.BoxJSONParser;
import com.box.boxjavalibv2.jsonparsing.IBoxJSONParser;
import com.box.boxjavalibv2.jsonparsing.IBoxResourceHub;
import com.box.boxjavalibv2.requests.requestobjects.BoxPagingRequestObject;
import com.box.restclientv2.exceptions.BoxRestException;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

import cen.unistor.app.R;
import cen.unistor.app.adapter.UnistorEntry;
import cen.unistor.app.adapter.UnistorEntryListAdapter;
import cen.unistor.app.adapter.ViewHolder;
import cen.unistor.app.asynctask.DownloadFileAsyncTask;
import cen.unistor.app.asynctask.UploadFileAsyncTask.*;
import cen.unistor.app.util.Constants;
import cen.unistor.app.util.ContentStatus;
import cen.unistor.app.util.UnistorEntryComparator;

/**
 * Created by carlos on 5/06/14.
 */
public class BoxFragment extends UnistorFragment implements OnUploadFinishedListener {

    private final String TAG = "BoxFragment";

    private final String CLIENT_ID = "trybv0asmi4v693ildrqpakn1642x5q5";
    private final String CLIENT_SECRET = "jBhlEPtO8Hat0WSczsDx9PVmetjJfMjd";
    private final String REDIRECT_URI="http://localhost";

    private final String AUTH_KEY = "AUTH_KEY";
    private final static int AUTH_REQUEST = 1;
    private final static int UPLOAD_REQUEST = 2;
    private final static int DOWNLOAD_REQUEST = 3;

    private Context mContext;
    private BoxAndroidClient mBoxClient;

    private ListView listView;
    private ArrayList<UnistorEntry> currentContent;
    private String currentPath;
    private Stack<ContentStatus> statusHistory;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mContext = rootView.getContext();

        startAuthentication();

        this.listView = (ListView)rootView.findViewById(R.id.listView);
        this.statusHistory = new Stack<ContentStatus>();
        // Initilize with the ID of the root folder.
        this.currentPath = "0";

        return rootView;
    }



    @Override
    public void onResume() {
        super.onResume();

        if(mBoxClient != null){
            ArrayList<UnistorEntry> content = loadContent(this.currentPath);
            populateContentListView(content);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    ViewHolder holder = (ViewHolder) view.getTag();
                    if (holder.getEntry().getName().contains(".apk")) {
                        Toast.makeText(mContext, R.string.open_apk_error, Toast.LENGTH_LONG).show();
                    } else if (!holder.getEntry().isFolder()) {
                        DownloadFileAsyncTask downloadTask
                                = new DownloadFileAsyncTask(mContext, mBoxClient, holder.getEntry().getPath(),
                                    holder.getEntry().getName(), holder.getEntry().getSize());

                        downloadTask.execute();
                    } else {
                        //TODO backButton historial o parentPath?? --> parentPath implica consumoDAtos
                        ContentStatus currentStatus = new ContentStatus(
                                new ArrayList<UnistorEntry>(currentContent),
                                null,
                                currentPath);
                        statusHistory.push(currentStatus);
                        currentContent = loadContent(holder.getEntry().getPath());
                        populateContentListView(currentContent);
                    }

                }
            });
        }



    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void startAuthentication(){
        BoxAndroidOAuthData oauth = loadSavedAuth();
        if (oauth != null){
            this.mBoxClient = this.buildBoxClient(oauth);
        }else {
            Intent intent = OAuthActivity.createOAuthActivityIntent(mContext, CLIENT_ID, CLIENT_SECRET, false, REDIRECT_URI);
            this.startActivityForResult(intent, AUTH_REQUEST);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case AUTH_REQUEST:
                BoxAndroidOAuthData oauth = data.getParcelableExtra(OAuthActivity.BOX_CLIENT_OAUTH);
                this.mBoxClient = buildBoxClient(oauth);
                saveAuth(oauth);
                break;
        }
    }

    private BoxAndroidClient buildBoxClient(BoxAndroidOAuthData oauth){
        BoxAndroidClient client;

        client = new BoxAndroidClient(CLIENT_ID, CLIENT_SECRET, null, null, null);
        client.authenticate(oauth);
        client.addOAuthRefreshListener(new OAuthRefreshListener() {

            @Override
            public void onRefresh(IAuthData newAuthData) {
                saveAuth((BoxAndroidOAuthData) newAuthData);
            }

        });

        return client;
    }


    private ArrayList<UnistorEntry> loadContent(String folderID){
        ArrayList<UnistorEntry> entryList = new ArrayList<UnistorEntry>();

        try {
            BoxPagingRequestObject requestObject = BoxPagingRequestObject.pagingRequestObject(10000,0);
            ArrayList<String> itemFields = new ArrayList<String>();
            itemFields.add(BoxItem.FIELD_SIZE);
            itemFields.add(BoxItem.FIELD_NAME);
            itemFields.add(BoxItem.FIELD_MODIFIED_AT);
            itemFields.add(BoxItem.FIELD_PARENT);
            requestObject.getRequestExtras().addFields(itemFields);
            BoxItem folder = this.mBoxClient.getFoldersManager().getFolder(folderID,requestObject);
            BoxCollection itemCollection = this.mBoxClient.getFoldersManager().getFolderItems(folderID, requestObject);

            // Building back entry into the entry list
            UnistorEntry entry;

            if(!folderID.equals("0")){
                entry = new UnistorEntry();
                entry.setName("Volver...");
                entry.setPath(folderID);
                entry.setFolder(true);
                entry.setEntryType(Constants.ENTRY_TYPE_BACK);
                entryList.add(entry);
            }


            for (BoxTypedObject typedObject : itemCollection.getEntries()){
                BoxItem item = (BoxItem) typedObject;
                entry = new UnistorEntry();
                entry.setName(item.getName());
                entry.setPath(item.getId());
                entry.setSize(item.getSize());
                entry.setFolder(item.getType().equals(Constants.BOX_TYPE_FOLDER));

                if(item.getType().equals(Constants.BOX_TYPE_FOLDER)){
                    entry.setEntryType(Constants.ENTRY_TYPE_FOLDER);

                }else if(item.getType().equals(Constants.BOX_TYPE_FILE)){
                    entry.setEntryType(Constants.ENTRY_TYPE_FILE);

                }else if(item.getType().equals(Constants.BOX_TYPE_WEBLINK)){
                    entry.setEntryType(Constants.ENTRY_TYPE_BOOKMARK);
                }


                String extension = item.getName().substring(item.getName().lastIndexOf('.') + 1).toLowerCase();

                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                Log.i(TAG, item.getName() + ": " + mimeType);
                entryList.add(entry);
            }
        } catch (BoxRestException e) {
            e.printStackTrace();
        } catch (BoxServerException e) {
            e.printStackTrace();
        } catch (AuthFatalFailureException e) {
            e.printStackTrace();
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
    public void onUploadFinish() {

    }

    @Override
    public boolean keyBackPressed(){
        Log.i("DropboxFragment", "back pressed. History "+statusHistory.size());

        if(!statusHistory.empty()){
            ContentStatus lastStatus = this.statusHistory.pop();
            this.currentContent = lastStatus.getContent();
            this.currentPath = lastStatus.getPath();
            this.populateContentListView(currentContent);
            return true;
        }
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
