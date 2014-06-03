package cen.unistor.app;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by carlos on 10/05/14.
 */
public class ListContentsAsync extends AsyncTask<Void, Integer, String[]>{

    private String mPath;
    private ListView mView;

    private Context mContext;

    private DropboxAPI<?> mDBApi;

    public ListContentsAsync(Context context, DropboxAPI<?> mDBApi, String path, ListView view){
        this.mPath = path;
        this.mView = view;
        this.mDBApi = mDBApi;
        this.mContext = context.getApplicationContext();
    }

    @Override
    protected String[] doInBackground(Void... voids) {
        DropboxAPI.Entry root = null;
        ArrayList<String> folderList = new ArrayList<String>();
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
        }
        Collections.sort(folderList);
        Collections.sort(fileList);
        ArrayList<String>entryList = new ArrayList<String>();
        entryList.addAll(folderList);
        entryList.addAll(fileList);
        return entryList.toArray(new String[entryList.size()]);
    }


    @Override
    protected void onPostExecute(String[] content) {
        if ( content != null ){
            ArrayAdapter<String> array = new ArrayAdapter<String>(mContext, R.layout.dropbox_list_item, content);

            this.mView.setAdapter(array);
        }
            //showToast ( "fnames==null" );
    }
}
