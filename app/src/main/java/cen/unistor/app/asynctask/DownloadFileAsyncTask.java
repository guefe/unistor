package cen.unistor.app.asynctask;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import cen.unistor.app.R;

/**
 * Created by carlos on 14/05/14.
 */
public class DownloadFileAsyncTask extends AsyncTask<Void, Long, Boolean> {

    private ProgressDialog mProgressDialog;
    private Context mContext;
    private DropboxAPI<?> mDBApi;
    private String mRemoteFilePath;
    private String fileName;
    private boolean mDoStore;
    private String mStoringPath;
    private String mErrorMsg;
    private FileOutputStream outputStream;

    public DownloadFileAsyncTask(Context context, DropboxAPI<?> DBApi, String filePath){
        this.mContext = context;
        this.mDBApi = DBApi;
        this.mRemoteFilePath = filePath;
        this.fileName = filePath.substring(filePath.lastIndexOf('/')+1);

    }

    public DownloadFileAsyncTask(Context context, DropboxAPI<?> DBApi, boolean doStore, String storingPath, String filePath){
        this(context,DBApi,filePath);
        this.mDoStore = doStore;
        this.mStoringPath = storingPath;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        String fileName = mRemoteFilePath.substring(mRemoteFilePath.lastIndexOf('/') + 1);
        String btnLbl = mContext.getString(R.string.btn_cancel);

        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setMessage(mContext.getString(R.string.download_dialog_msg) + fileName);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                Log.i("Info: ", "onCancel fired");
                cancel(true);

            }
        });
        mProgressDialog.setButton(AlertDialog.BUTTON_NEUTRAL, btnLbl, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Log.i("Info: ", "Button fired");
                cancel(true);
                mErrorMsg = mContext.getString(R.string.canceled_msg);

                // This will cancel the getThumbnail operation by closing
                // its stream
//                if (mFos != null) {
//                    try {
//                        mFos.close();
//                    } catch (IOException e) {
//                    }
//                }
            }
        });

        mProgressDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... voids) {

        try {
            // Task status check
            if(isCancelled()){
                return false;
            }

            // An ouputStream is opened into the app private folder to store the file.
            outputStream = mContext.openFileOutput(this.fileName, Context.MODE_PRIVATE);

            // File is downloaded via dropbox api. MyProgressListener handles the progress
            DropboxAPI.DropboxFileInfo file = mDBApi.getFile(mRemoteFilePath, null, outputStream, new MyProgressListener());

            Log.i("Info: ","Download Completed!");

            Intent intent = new Intent(Intent.ACTION_VIEW);
            File newFile = new File(mContext.getFilesDir(), this.fileName);
            String path = Uri.fromFile(newFile).getPath();
            Uri fileURI = Uri.parse("content://cen.unistor.app" + path);
            Log.i("File uri: ",fileURI.getPath());
            intent.setDataAndType(fileURI, file.getMimeType());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Task status check
            if(isCancelled()){
                Log.i("Info: ", "If 3");
                return false;
            }
            mContext.startActivity(intent);
        } catch (DropboxUnlinkedException e) {
            // The AuthSession wasn't properly authenticated or user unlinked.
        } catch (DropboxPartialFileException e) {
            // We canceled the operation
            mErrorMsg = "Download canceled";
            Toast.makeText(mContext,mErrorMsg,Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (DropboxServerException e) {
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

        } catch (DropboxIOException e) {
            // Happens all the time, probably want to retry automatically.
            mErrorMsg = mContext.getString(R.string.dropbox_IO_excp_msg);

        } catch (DropboxParseException e) {
            // Probably due to Dropbox server restarting, should retry
            mErrorMsg = mContext.getString(R.string.dropbox_parse_excp_msg);

        } catch (DropboxException e) {
            // Unknown error
            mErrorMsg = mContext.getString(R.string.dropbox_excp_msg);

        }catch (FileNotFoundException e){
            mErrorMsg = mContext.getString(R.string.file_not_found);
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }



    protected void onProgressUpdate(Long... progress) {

        super.onProgressUpdate();
        int percent = (int)(100.0*(double)progress[0]/progress[1] + 0.5);
        Log.i("Progress percent", ""+percent);
        mProgressDialog.setProgress(percent);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mProgressDialog.dismiss();

        if(result){
            Toast.makeText(mContext,R.string.download_completed,Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCancelled(Boolean aBoolean) {
        mProgressDialog.dismiss();
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i("Info: ","Canceled");
        boolean res = mContext.deleteFile(this.fileName);
        if (res){
            Log.i("Info: ", "File deleted");
        }
    }

    // This class publish the progress of the file download
    class MyProgressListener extends ProgressListener {

        //Publishes downloaded bytes of the file
        @Override
        public void onProgress(long downloadedBytes, long totalBytes) {
            publishProgress(downloadedBytes, totalBytes);
        }

        // Returns number of milliseconds of publishing
        @Override
        public long progressInterval() {
            return 200;
        }
    }


}


