package cen.unistor.app.adapter;

import android.widget.ImageView;
import android.widget.TextView;

public class ViewHolder{
    private TextView name;
    private ImageView icon;
    private UnistorEntry entry;

    public TextView getName() {
        return name;
    }

    public void setName(TextView name) {
        this.name = name;
    }

    public ImageView getIcon() {
        return icon;
    }

    public void setIcon(ImageView icon) {
        this.icon = icon;
    }

    public UnistorEntry getEntry() {
        return entry;
    }

    public void setEntry(UnistorEntry entry) {
        this.entry = entry;
    }
}
