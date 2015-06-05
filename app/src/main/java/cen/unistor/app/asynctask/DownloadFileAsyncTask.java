package cen.unistor.app.asynctask;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.box.boxandroidlibv2.BoxAndroidClient;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.exceptions.BoxServerException;
import com.box.boxjavalibv2.filetransfer.IFileTransferListener;
import com.box.restclientv2.exceptions.BoxRestException;
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

    private final String TAG = "DownloadFileAsyncTask";

    private ProgressDialog mProgressDialog;
    private Context mContext;
    private String mFileName;
    private String mErrorMsg;

    /* Dropbox parameters */
    private DropboxAPI<?> mDBApi;
    private String mRemoteFilePath;
    private FileOutputStream mOutputStream;

    /* Box parameters */
    private BoxAndroidClient mBoxClient;
    private String mFileID;
    private double mFileSize;
    private BoxProgressListener mBoxProgressListener;

    public DownloadFileAsyncTask(Context context, DropboxAPI<?> DBApi, String filePath, String fileName){
        this.mContext = context;
        this.mDBApi = DBApi;
        this.mRemoteFilePath = filePath;
        this.mFileName = fileName;

    }

    public DownloadFileAsyncTask(Context context, BoxAndroidClient client, String fileID, String fileName, double size){
        this.mContext = context;
        this.mBoxClient = client;
        this.mFileID = fileID;
        this.mFileName = fileName;
        this.mFileSize = size;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        String btnLbl = mContext.getString(R.string.btn_cancel);

        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setMessage(mContext.getString(R.string.download_dialog_msg) + mFileName);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                Log.i("Info: ", "onCancel fired");
                cancel(true);
                if(mBoxProgressListener != null){
                    mBoxProgressListener.onCanceled();
                }

                mErrorMsg = mContext.getString(R.string.canceled_msg);

            }
        });
        mProgressDialog.setButton(AlertDialog.BUTTON_NEUTRAL, btnLbl, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Log.i("Info: ", "Button fired");
                cancel(true);
                if(mBoxProgressListener != null){
                    mBoxProgressListener.onCanceled();
                }
                mErrorMsg = mContext.getString(R.string.canceled_msg);
            }
        });

        mProgressDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... voids) {

        File localFile;
        String mimeType;

        if(mBoxClient != null){
            // Box download
            localFile = this.downloadBoxFile();
            String extension = mFileName.substring(mFileName.lastIndexOf('.') + 1).toLowerCase();
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }else {
            // Dropbox download
            DropboxAPI.DropboxFileInfo dropBoxFile = this.downloadDropBoxFile();
            localFile = new File(mContext.getFilesDir(), this.mFileName);
            mimeType = dropBoxFile.getMimeType();
        }

        if( localFile != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);

            String path = Uri.fromFile(localFile).getPath();
            Uri fileURI = Uri.parse("content://cen.unistor.app" + path);
            Log.i("File uri: ", fileURI.getPath());
            intent.setDataAndType(fileURI, mimeType);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Task status check
            if (isCancelled()) {
                Log.i("Info: ", "If 3");
                return false;
            }
            mContext.startActivity(intent);
            return true;

        }else{
            return false;
        }
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
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.i("Info: ","Canceled");
        boolean res = mContext.deleteFile(this.mFileName);
        if (res){
            Log.i("Info: ", "File deleted");
        }
    }



    private File downloadBoxFile(){
        // Task status check
        if(isCancelled()){
            return null;
        }

        File f = new File(mContext.getFilesDir(), this.mFileName);
        this.mBoxProgressListener = new BoxProgressListener();
        try {
            mBoxClient.getFilesManager().downloadFile(this.mFileID, f, this.mBoxProgressListener, null);
            //TODO error handling
        } catch (BoxRestException e) {
            e.printStackTrace();
        } catch (BoxServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            mErrorMsg = mContext.getString(R.string.canceled_msg);
            e.printStackTrace();
        } catch (AuthFatalFailureException e) {
            e.printStackTrace();
        }

        return f;
    }


    private DropboxAPI.DropboxFileInfo downloadDropBoxFile(){
        DropboxAPI.DropboxFileInfo file = null;
        try {
            // Task status check
            if(isCancelled()){
                return null;
            }

            // An ouputStream is opened into the app private folder2 to store the file.
            mOutputStream = mContext.openFileOutput(this.mFileName, Context.MODE_PRIVATE);

            // File is downloaded via dropbox_icon api. MyProgressListener handles the progress
            file = mDBApi.getFile(mRemoteFilePath, null, mOutputStream, new DropboxProgressListener());

            Log.i("Info: ","Download Completed!");


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
            mErrorMsg = mContext.getString(R.string.excp_msg);

        }catch (FileNotFoundException e){
            mErrorMsg = mContext.getString(R.string.file_not_found);
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            try {
                mOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    // This class publish the progress of the file download
    class DropboxProgressListener extends ProgressListener {

        //Publishes downloaded bytes of the file
        @Override
        public void onProgress(long downloadedBytes, long totalBytes) {
            Log.i("Progress:", ""+downloadedBytes);
            publishProgress(downloadedBytes, totalBytes);
        }

        // Returns number of milliseconds of publishing
        @Override
        public long progressInterval() {
            return 200;
        }
    }


    class BoxProgressListener implements IFileTransferListener {

        @Override
        public void onComplete(String s) {
            Log.i(TAG, "OnComplete. Status: " + s);

        }

        @Override
        public void onCanceled() {
            Log.i(TAG, "onCanceled. Return: ");
            //return;
        }

        @Override
        public void onProgress(long downloadedBytes) {
            publishProgress(downloadedBytes, (long)mFileSize);
        }

        @Override
        public void onIOException(IOException e) {
            Log.i(TAG, "onIOException. ");
        }
    }


}


