package cen.unistor.app.util;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import cen.unistor.app.adapter.UnistorEntry;

/**
 * Created by carlos on 21/05/14.
 *
 * Used to store past status in order to minimize Dropbox API metadata calls.
 * Implements Parcelable to be able to be stored in savedInstance.
 */
public class ContentStatus implements Parcelable{

    private ArrayList<UnistorEntry> content;
    private String statusHash;
    private String path;


    public ContentStatus(ArrayList<UnistorEntry> content, String statusHash, String path) {
        this.content = content;
        this.statusHash = statusHash;
        this.path = path;
    }

    // Used in CREATOR
    public ContentStatus(Parcel parcel){
        parcel.readTypedList(this.content, UnistorEntry.CREATOR);
        this.statusHash = parcel.readString();
        this.path = parcel.readString();
    }

    public ArrayList<UnistorEntry> getContent() {
        return content;
    }

    public void setContent(ArrayList<UnistorEntry> content) {
        this.content = content;
    }

    public String getStatusHash() {
        return statusHash;
    }

    public void setStatusHash(String statusHash) {
        this.statusHash = statusHash;
    }

    public String getPath() {
        return path;
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(getContent());
        parcel.writeString(getStatusHash());
        parcel.writeString(getPath());

    }

    public static final Creator<ContentStatus> CREATOR = new Creator<ContentStatus>() {
        @Override
        public ContentStatus createFromParcel(Parcel parcel) {
            return new ContentStatus(parcel);
        }

        @Override
        public ContentStatus[] newArray(int size) {
            return new ContentStatus[size];
        }
    };



}
