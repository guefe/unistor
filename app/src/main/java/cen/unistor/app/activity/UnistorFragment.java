package cen.unistor.app.activity;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;

import cen.unistor.app.R;
import cen.unistor.app.util.Constants;

/**
 * Created by carlos on 26/05/14.
 */
public abstract class UnistorFragment extends Fragment{


    public abstract boolean keyBackPressed();

    public abstract boolean uploadFile(String path);

    public abstract boolean pasteFile(String source, int mode);

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setHasOptionsMenu(true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MainActivity activity = (MainActivity)getActivity();
        if(activity.getCopyMoveAction() != Constants.ACTION_PASTE_DONE){
            menu.findItem(R.id.action_paste).setVisible(true);
        }else{
            menu.findItem(R.id.action_paste).setVisible(false);
        }

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if(v.getId() == R.id.listView){//context_menu_array
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
            String[] menuEntries = getResources().getStringArray(R.array.context_menu_array);
            for(int i=0;i<menuEntries.length;i++){
                menu.add(Menu.NONE,i,Menu.NONE,menuEntries[i]);
            }
        }
    }


}
