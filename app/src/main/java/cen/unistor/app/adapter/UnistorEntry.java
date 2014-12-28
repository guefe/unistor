package cen.unistor.app.adapter;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import cen.unistor.app.R;
import cen.unistor.app.util.Constants;

/**
 * Created by carlos on 12/05/14.
 */
public class UnistorEntry implements Parcelable {
    private String name;
    private int entryType;
    private String path;
    private boolean isFolder;
    private double size;
    private String sizeString;
    private String lastModification;


    public UnistorEntry(){

    }

    public UnistorEntry(String name, int entryType, String path, boolean isFolder,
                        double size, String sizeString, String lastModification) {
        this.name = name;
        this.entryType = entryType;
        this.path = path;
        this.isFolder = isFolder;
        this.size = size;
        this.sizeString = sizeString;
        this.lastModification = lastModification;
    }

    public UnistorEntry(Parcel in){
        this.name = in.readString();
        this.entryType = in.readInt();
        this.path = in.readString();
        this.isFolder = in.readInt() == 1;
        this.size = in.readDouble();
        this.sizeString = in.readString();
        this.lastModification = in.readString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getEntryType() {
        return entryType;
    }

    public void setEntryType(int entryType) {
        this.entryType = entryType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean isFolder) {
        this.isFolder = isFolder;
    }

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public String getLastModification() {
        return lastModification;
    }

    public void setLastModification(String lastModification) {
        this.lastModification = lastModification;
    }

    public String getSizeString() {
        return sizeString;
    }

    public void setSizeString(String sizeString) {
        this.sizeString = sizeString;
    }

    /**
     * Return the icon identifier according the entry type
     * @return resource id
     */
    public int getEntryIcon(Context context){
        int iconID;

        String extension = this.getName().substring(this.getName().lastIndexOf('.')+1).toLowerCase();

        switch (getEntryType()){
            case Constants.ENTRY_TYPE_FOLDER:
                iconID = R.drawable.folder;
                break;

            case Constants.ENTRY_TYPE_BOOKMARK:
                iconID = R.drawable.bookmark;
                break;

            case Constants.ENTRY_TYPE_BACK:
                iconID = R.drawable.circle_back_arrow;
                break;
            default:
                if(extension.equals("apk")){
                    iconID = android.R.drawable.sym_def_app_icon;
                }else {
                    iconID = context.getResources().getIdentifier(extension, "drawable", context.getPackageName());
                }
                //iconID = R.drawable.file;
                if(iconID == 0){
                    iconID = R.drawable._blank;
                }
        }

        return iconID;
    }


    public static final Creator<UnistorEntry> CREATOR = new Creator<UnistorEntry>(){

        @Override
        public UnistorEntry createFromParcel(Parcel parcel) {
            return new UnistorEntry(parcel);
        }

        @Override
        public UnistorEntry[] newArray(int i) {
            return new UnistorEntry[i];
        }
    };



    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.getName());
        parcel.writeInt(this.getEntryType());
        parcel.writeString(this.getPath());
        parcel.writeInt(this.isFolder() ? 1 : 0);
        parcel.writeDouble(this.getSize());
        parcel.writeString(this.getSizeString());
        parcel.writeString(this.getLastModification());
    }



}
