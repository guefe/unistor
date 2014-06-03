package cen.unistor.app.activity;

import android.support.v4.app.Fragment;

/**
 * Created by carlos on 26/05/14.
 */
public abstract class UnistorFragment extends Fragment{


    public abstract boolean keyBackPressed();

    public abstract boolean uploadFile(String path);

    public abstract boolean pasteFile(String source, int mode);
}
