package cen.unistor.app.asynctask;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ListView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cen.unistor.app.R;
import cen.unistor.app.adapter.UnistorEntry;
import cen.unistor.app.adapter.UnistorEntryListAdapter;
import cen.unistor.app.util.Constants;

/**
 * Created by carlos on 10/05/14.
 */
public class ListContentsAsync extends AsyncTask<Void, Integer, List<UnistorEntry>>{

    private UnistorEntry[] resultArray;
    private String mPath;
    private ListView mView;

    private Context mContext;

    private DropboxAPI<?> mDBApi;

    private String mErrorMsg;

    public ListContentsAsync(Context context, DropboxAPI<?> mDBApi, String path, ListView view, UnistorEntry[] resultArray){
        this.mPath = path;
        this.mView = view;
        this.mDBApi = mDBApi;
        this.mContext = context.getApplicationContext();
        this.resultArray = resultArray;
    }

    @Override
    protected List<UnistorEntry> doInBackground(Void... voids) {
        DropboxAPI.Entry root = null;
        /*ArrayList<String> folderList = new ArrayList<String>();
        ArrayList<String> fileList = new ArrayList<String>();
        try {
            root = mDBApi.metadata("/", 0, null, true, null);
            for(DropboxAPI.Entry entry: root.contents){
                if(entry.isDir){
                    folderList.add(entry.path.substring(1));
                }else{
                    fileList.add(entry.fileName());
                }
                Log.i("Dropbox entry", entry.isDir ? "Dir: " + entry.path : "File: " + entry.fileName());

            }
        } catch (DropboxException e) {
            e.printStackTrace();
        }*/

        ArrayList<UnistorEntry> entryList = new ArrayList<UnistorEntry>();

        try {
            root = mDBApi.metadata(this.mPath, 0, null, true, null);

            UnistorEntry tmp;
            if(root.parentPath() != null && !root.contents.isEmpty()){
                tmp = new UnistorEntry();
                tmp.setName("Volver a "+root.parentPath().substring(root.parentPath().lastIndexOf('/')+1));
                tmp.setPath(root.parentPath());
                tmp.setFolder(true);
                entryList.add(tmp);
            }
            for(DropboxAPI.Entry entry: root.contents){
                tmp = new UnistorEntry();
                Log.i("parentpath",entry.parentPath());
                if(entry.isDir){
                    tmp.setName(entry.path.substring(entry.path.lastIndexOf('/')+1));
                    tmp.setEntryType(Constants.ENTRY_TYPE_FOLDER);
                }else{
                    tmp.setName(entry.fileName());
                    tmp.setEntryType(Constants.ENTRY_TYPE_FILE);
                }
                tmp.setPath(entry.path);
                tmp.setFolder(entry.isDir);

                Log.i("Dropbox entry", entry.isDir ? "Dir: " + entry.path +" "+entry.mimeType: "File: " + entry.fileName()+" "+entry.mimeType);
                entryList.add(tmp);
            }
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
        Collections.sort(entryList, new EntryComparator());
//        Collections.sort(folderList);
//        Collections.sort(fileList);
//        ArrayList<String>entryList = new ArrayList<String>();
//        entryList.addAll(folderList);
//        entryList.addAll(fileList);
//        return entryList.toArray(new String[entryList.size()]);
        return entryList;
    }


    @Override
    protected void onPostExecute(List<UnistorEntry> content) {
        Log.i("TraceUnistor", "ListContentsAsync.onPostExecute");
        if ( content != null ){
            Log.i("TraceUnistor", "ListContentsAsync.onPostExecute: content not null");
            //ArrayAdapter<String> array = new ArrayAdapter<String>(mContext, R.layout.dropbox_list_item, content);
            //this.mView.setAdapter(array);
            final UnistorEntryListAdapter adapter = new UnistorEntryListAdapter(this.mContext, content);

            this.mView.setAdapter(adapter);




        }
            //showToast ( "fnames==null" );
    }

    class EntryComparator implements Comparator<UnistorEntry> {

        @Override
        /**
         * Compares two UnistorEntry so that folders go first and then sorts alphabetically.
         */
        public int compare(UnistorEntry unistorEntry, UnistorEntry unistorEntry2) {
            if(unistorEntry.isFolder() && !unistorEntry2.isFolder()){
                return -1;
            }else if (!unistorEntry.isFolder() && unistorEntry2.isFolder()){
                return 1;
            }else{
                return unistorEntry.getName().compareTo(unistorEntry2.getName());
            }
        }
    }
}
