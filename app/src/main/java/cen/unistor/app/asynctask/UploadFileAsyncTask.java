package cen.unistor.app.asynctask;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.box.boxandroidlibv2.BoxAndroidClient;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.exceptions.BoxJSONException;
import com.box.boxjavalibv2.exceptions.BoxServerException;
import com.box.restclientv2.exceptions.BoxRestException;
import com.box.restclientv2.requestsbase.BoxFileUploadRequestObject;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxFileSizeException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import cen.unistor.app.R;

/**
 * Created by carlos on 28/05/14.
 */



public class UploadFileAsyncTask extends AsyncTask<Void, Long, Boolean> {

    // Interface to be implemented by the fragment to have a notice
    // when the upload has finished.
    public interface OnUploadFinishedListener{
        public void onUploadFinish();
    }

    private ProgressDialog mProgressDialog;
    private OnUploadFinishedListener finishListener;
    private Context mContext;

    /* Path in dropbox, id in Box */
    private String mSourcePath;
    private String mDestPath;

    /* Dropbox parameters */
    private DropboxAPI<?> mDBApi;
    private String mFileName;
    private DropboxAPI.UploadRequest mUploadRequest;


    /* Box parameters */
    private BoxAndroidClient mBoxClient;
    private File mSourceFile;



    private String mErrorMsg;


    public UploadFileAsyncTask(Context context, DropboxAPI<?> dbapi, String sourcePath, String mDestPath, OnUploadFinishedListener listener){
        this.mContext = context;
        this.mDBApi = dbapi;
        this.mSourcePath = sourcePath;
        this.mDestPath = mDestPath;
        this.mFileName = sourcePath.substring(sourcePath.lastIndexOf('/')+1);
        this.finishListener = listener;
    }

    public UploadFileAsyncTask(Context context, BoxAndroidClient client, File file, String parentID, OnUploadFinishedListener listener){
        this.mContext = context;
        this.mBoxClient = client;
        this.mSourceFile = file;
        this.mFileName = file.getName();
        this.mDestPath = parentID;
        this.finishListener = listener;
    }

    protected void onPreExecute() {
        super.onPreExecute();
        String btnLbl = mContext.getString(R.string.btn_cancel);

        this.mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setMessage(mContext.getString(R.string.upload_dialog_msg) + mFileName);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                Log.i("Info: ", "onCancel fired");
                cancel(true);
                mUploadRequest.abort();
                mErrorMsg = mContext.getString(R.string.canceled_msg);
            }
        });

        mProgressDialog.setButton(AlertDialog.BUTTON_NEUTRAL, btnLbl, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Log.i("Info: ", "Button fired");
                cancel(true);
                mUploadRequest.abort();
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
        boolean result = false;
        try {
            // Task status check
            if(isCancelled()){
                Log.i("Info: ", "If 2");
                return false;
            }

            File file = new File(mSourcePath);
            InputStream stream = new FileInputStream(file);

            this.mUploadRequest = this.mDBApi.putFileRequest(mDestPath+this.mFileName, stream, file.length(), null, new MyProgressListener());

            this.mUploadRequest.upload();
            //this.mDBApi.putFile(mDestPath+this.mFileName, stream, DropboxAPI.MAX_UPLOAD_SIZE + 1024, null, new MyProgressListener());
            result = true;

        } catch (FileNotFoundException e) {
            mErrorMsg = mContext.getString(R.string.file_not_found);
            e.printStackTrace();
        } catch (DropboxPartialFileException e) {
            // We canceled the operation
            mErrorMsg = "Upload canceled when "+e.bytesTransferred+" had been transferred";
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
        } catch (DropboxFileSizeException e) {
            // File is bigger than the maximum allowed
            long conversionFactor = 1024*1024;
            mErrorMsg = mContext.getString(R.string.dropbox_filesize_exp_msg)+"("+ DropboxAPI.MAX_UPLOAD_SIZE/conversionFactor +")";
        } catch (DropboxException e) {
            // Unknown error
            mErrorMsg = mContext.getString(R.string.dropbox_excp_msg);
        }finally {

        }

        return result;

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
            Toast.makeText(mContext, R.string.upload_completed, Toast.LENGTH_LONG).show();
            finishListener.onUploadFinish();
        }else{
            Toast.makeText(mContext, mErrorMsg, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCancelled() {
        mProgressDialog.dismiss();
        Toast.makeText(mContext, mErrorMsg, Toast.LENGTH_LONG).show();

    }


    private void uploadBoxElement(){

        try {
            BoxFileUploadRequestObject uploadRequestObject = BoxFileUploadRequestObject.uploadFileRequestObject(mDestPath, mFileName, mSourceFile);
            mBoxClient.getFilesManager().uploadFile(uploadRequestObject);

            //TODO error handling
        } catch (BoxRestException e) {
            e.printStackTrace();
        } catch (BoxJSONException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BoxServerException e) {
            e.printStackTrace();
        } catch (AuthFatalFailureException e) {
            e.printStackTrace();
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
