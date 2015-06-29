package cen.unistor.app.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.box.boxandroidlibv2.BoxAndroidClient;
import com.box.boxandroidlibv2.activities.OAuthActivity;
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
import com.box.boxjavalibv2.requests.requestobjects.BoxItemCopyRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxPagingRequestObject;
import com.box.restclientv2.exceptions.BoxRestException;

import org.apache.commons.lang.StringUtils;

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
 * Created by carlos on 5/06/14.
 */
public class BoxFragment extends UnistorFragment{



    private final String TAG = "BoxFragment" + UUID.randomUUID().toString();

    private final String CLIENT_ID = "trybv0asmi4v693ildrqpakn1642x5q5";
    private final String CLIENT_SECRET = "jBhlEPtO8Hat0WSczsDx9PVmetjJfMjd";
    private final String REDIRECT_URI = "http://localhost";

    private final String BOX_AUTH_KEY = "BOX_AUTH_KEY";
    private final static int AUTH_REQUEST = 1;

    private BoxAndroidClient mBoxClient;

    private String previousPath;


    @Override
    public int getServiceType() {
        return Constants.ACCOUNT_BOX;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        listView = (ListView)rootView.findViewById(R.id.listView);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            /**
             * Controla las acciones al pulsar en un elemento de la lista.
             * En las carpetas, vuelve a pedir contenido
             * En los ficheros:
             *      • Excluye apk para no abrirlos.
             *      • Si es archivo, intentará abrirlo si ya ha sido descargado.
             *      • Si no ha sido descargado, lo descargará e intentará abrirlo.
             *      • Si ninguna premisa se cumple, es la entrada de subir directorio.
             *          Se carga el contenido anterior, almacenado en memoria.
             */
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ViewHolder holder = (ViewHolder) view.getTag();

                if (holder.getEntry().getName().contains(".apk")) {
                    Toast.makeText(mContext, mContext.getText(R.string.no_application_found), Toast.LENGTH_LONG).show();

                } else if (!holder.getEntry().isFolder()) {

                    File localFile = new File(mContext.getFilesDir(), holder.getEntry().getName());
                    if (localFile.exists()) {
                        String extension = holder.getEntry().getName().substring(holder.getEntry().getName().lastIndexOf('.') + 1).toLowerCase();
                        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

                        String path = Uri.fromFile(localFile).getPath();
                        Uri fileURI = Uri.parse("content://cen.unistor.app" + path);
                        Log.i("File uri: ", fileURI.getPath());
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(fileURI, mimeType);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);

                    }else {
                        DownloadFileAsyncTask downloadTask
                                = new DownloadFileAsyncTask(mContext, mBoxClient, holder.getEntry().getPath(),
                                holder.getEntry().getName(), holder.getEntry().getSize());

                        downloadTask.execute();
                    }

                } else {

                    ContentStatus currentStatus = new ContentStatus(
                            new ArrayList<UnistorEntry>(currentContent),
                            null,
                            new String(currentPath));
                    statusHistory.push(currentStatus);
                    previousPath = currentPath;
                    currentContent = loadContent(holder.getEntry().getPath());
                    populateContentListView(currentContent);
                }

            }
        });

        mContext = rootView.getContext();
        statusHistory = new Stack<ContentStatus>();
        // Initilize with the ID of the root folder.
        currentPath = "0";

        if (savedInstanceState != null){
            currentContent = savedInstanceState.getParcelableArrayList("currentContent");
            currentPath = savedInstanceState.getString("currentPath");
        }else{
            startAuthentication();

        }
        return rootView;
    }




    private void startAuthentication(){
        BoxAndroidOAuthData oauth = loadSavedAuth();

        if (oauth != null){
            this.mBoxClient = this.buildBoxClient(oauth);
            currentContent = loadContent(this.currentPath);
            populateContentListView(currentContent);
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
                currentContent = loadContent(this.currentPath);
                populateContentListView(currentContent);
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

    @Override
    protected ArrayList<UnistorEntry> loadContent(String folderID){
        ArrayList<UnistorEntry> entryList = new ArrayList<UnistorEntry>();

        try {
            BoxPagingRequestObject requestObject = BoxPagingRequestObject.pagingRequestObject(10000,0);
            ArrayList<String> itemFields = new ArrayList<String>();
            itemFields.add(BoxItem.FIELD_SIZE);
            itemFields.add(BoxItem.FIELD_NAME);
            itemFields.add(BoxItem.FIELD_MODIFIED_AT);
            itemFields.add(BoxItem.FIELD_PARENT);
            requestObject.getRequestExtras().addFields(itemFields);
            BoxCollection itemCollection = this.mBoxClient.getFoldersManager().getFolderItems(folderID, requestObject);

            // Building back entry into the entry list
            UnistorEntry entry;

            if(!folderID.equals("0")){
                entry = new UnistorEntry();
                entry.setName("Volver...");
                entry.setPath(previousPath);
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
                    entry.setSizeString(String.valueOf(item.getSize()));
                    entry.setLastModification(item.getModifiedAt());

                }else if(item.getType().equals(Constants.BOX_TYPE_WEBLINK)){
                    entry.setEntryType(Constants.ENTRY_TYPE_BOOKMARK);
                }


                String extension = item.getName().substring(item.getName().lastIndexOf('.') + 1).toLowerCase();

                entryList.add(entry);
            }

            currentPath = folderID;
            //TODO error handling
        } catch (BoxRestException e) {
            e.printStackTrace();
        } catch (BoxServerException e) {
            e.printStackTrace();
        } catch (AuthFatalFailureException e) {
            Toast.makeText(mContext, R.string.box_auth_error,Toast.LENGTH_LONG).show();
            this.refreshAuth();

            Log.i(TAG, "Auth error: credentials refreshed");
        }

        return entryList;
    }

    private void refreshAuth() {
        this.saveAuth(null);
        this.startAuthentication();
    }

    @Override
    public void logOut() {
        Log.i(TAG, "logout");
    }

    @Override
    protected void prepareChildOptionsMenu(Menu menu) {

        if (((MainActivity)getActivity()).getCopyMoveOrigin() != Constants.ACCOUNT_BOX){
            menu.findItem(R.id.action_paste).setVisible(false);
        }
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
        File file = new File(path);
        UploadFileAsyncTask task = new UploadFileAsyncTask(mContext, mBoxClient, file, currentPath, this);
        task.execute();

        return true;
    }

    @Override
    public boolean pasteFile(String source, String name, int mode) {

        try {
            BoxItemCopyRequestObject copyRequestObject = BoxItemCopyRequestObject.copyItemRequestObject(currentPath);
            copyRequestObject.setName(name);
            mBoxClient.getFilesManager().copyFile(source, copyRequestObject);

            if( mode == Constants.ACTION_MOVE ){
                deleteElement(source);
            }

            // Refresh the current view to reflect the changes

            this.currentContent = loadContent(this.currentPath);
            populateContentListView(this.currentContent);
        } catch (BoxRestException e) {
            e.printStackTrace();
        } catch (BoxServerException e) {
            e.printStackTrace();
        } catch (AuthFatalFailureException e) {
            e.printStackTrace();
        }

        return true;
    }


    @Override
    protected void deleteElement(String path) {
        Log.i(TAG, "deleteElement "+path);
        try {
            mBoxClient.getFilesManager().deleteFile(path,null);
            //TODO error handling
        } catch (BoxRestException e) {

            e.printStackTrace();
        } catch (BoxServerException e) {
            e.getStatusCode();
            e.printStackTrace();
        } catch (AuthFatalFailureException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save credentials into sharedPreferences.
     * If auth == null, clear previous stored credentials
     * @param auth
     */
    private void saveAuth(BoxAndroidOAuthData auth) {
        try {
            if (auth !=null){
                IBoxJSONParser parser = getJSONParser();
                String authString = parser.convertBoxObjectToJSONString(auth);
                mContext.getSharedPreferences(Constants.PREFS_NAME, 0).edit().putString(BOX_AUTH_KEY, authString).commit();

            }else {
                mContext.getSharedPreferences(Constants.PREFS_NAME, 0).edit().remove(BOX_AUTH_KEY).commit();
            }

        }
        catch (Exception e) {
        }
    }


    /**
     *
     * @return Auth data loaded from SharedPreferences
     */
    private BoxAndroidOAuthData loadSavedAuth() {
        String authString = mContext.getSharedPreferences(Constants.PREFS_NAME, 0).getString(BOX_AUTH_KEY, "");
        if (StringUtils.isNotEmpty(authString)) {
            try {
                IBoxJSONParser parser = getJSONParser();
                BoxAndroidOAuthData auth = parser.parseIntoBoxObject(authString, BoxAndroidOAuthData.class);
                Log.i(TAG, "loadSavedAuth: " + auth.toString());

                return auth;
            }
            catch (Exception e) {
                // failed, null will be returned. You can also add more logging, error handling here.
                e.printStackTrace();
            }
        }
        return null;
    }

    private IBoxJSONParser getJSONParser() {
        return new BoxJSONParser(getResourceHub());
    }

    private IBoxResourceHub getResourceHub() {
        return new AndroidBoxResourceHub();
    }
}
