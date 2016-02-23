package cen.unistor.app.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

import cen.unistor.app.R;
import cen.unistor.app.adapter.UnistorEntry;
import cen.unistor.app.adapter.UnistorEntryListAdapter;
import cen.unistor.app.adapter.ViewHolder;
import cen.unistor.app.asynctask.UploadFileAsyncTask;
import cen.unistor.app.util.Constants;
import cen.unistor.app.util.ContentStatus;
import cen.unistor.app.util.UnistorEntryComparator;

/**
 * Created by carlos on 26/05/14.
 */
public abstract class UnistorFragment extends Fragment implements UploadFileAsyncTask.OnUploadFinishedListener{

    protected Context mContext;
    protected ListView listView;

    // Used to minimize api calls
    protected ArrayList<UnistorEntry> currentContent;
    protected String currentPath;
    protected Stack<ContentStatus> statusHistory;


    public abstract int getServiceType();

    public abstract boolean keyBackPressed();

    public abstract boolean uploadFile(String path);

    public abstract boolean pasteFile(String source, String name, int mode);

    protected abstract void deleteElement(String path);

    protected abstract ArrayList<UnistorEntry> loadContent(String path);

    public abstract void logOut();

    protected abstract void prepareChildOptionsMenu (Menu menu);


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("currentContent", this.currentContent);
        outState.putString("currentPath", currentPath);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MainActivity activity = (MainActivity)getActivity();
        if(activity.getCopyMoveAction() != Constants.ACTION_PASTE_DONE){
            if(menu.findItem(R.id.action_paste) != null)
                menu.findItem(R.id.action_paste).setVisible(true);
        }else{
            if(menu.findItem(R.id.action_paste) != null)
                menu.findItem(R.id.action_paste).setVisible(false);
        }

        prepareChildOptionsMenu(menu);

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

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // getUserVisibleHint is needed in a multi-fragment environment.
        // returns true if the fragment is the visible one.
        if (getUserVisibleHint()) {
            // Handle menu events and return true

            // Gets the itemView in the adapter
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            View view = info.targetView;
            ViewHolder itemViewHolder = (ViewHolder)view.getTag();

            switch (item.getItemId()){
                case 0:// Copy
                    //Se guarda el nombre del fichero, el path origen a copiar y el servicio origen.
                    ((MainActivity)getActivity()).setPathToCopy(itemViewHolder.getEntry().getPath());
                    ((MainActivity)getActivity()).setNameFileToCopy(itemViewHolder.getEntry().getName());
                    ((MainActivity)getActivity()).setCopyMoveOrigin(getServiceType());
                    getActivity().invalidateOptionsMenu();
                    break;

                case 1:// Move
                    //Se guarda el nombre del fichero, el path origen a mover y el servicio origen.
                    ((MainActivity)getActivity()).setPathToMove(itemViewHolder.getEntry().getPath());
                    ((MainActivity)getActivity()).setNameFileToCopy(itemViewHolder.getEntry().getName());
                    ((MainActivity)getActivity()).setCopyMoveOrigin(getServiceType());
                    getActivity().invalidateOptionsMenu();
                    break;

                case 2:// Delete
                    deleteElement(itemViewHolder.getEntry().getPath());
                    currentContent.remove(itemViewHolder.getEntry());
                    //currentContent = loadContent(currentPath);
                    populateContentListView(currentContent);
                    break;
            }

            return true;
        } else
            return false;
    }






    /**
     *  Fills the listview with the content provided.
     * @param content
     */
    protected void populateContentListView(ArrayList<UnistorEntry> content){

        // If first entry is back button, the content array
        // is sorted without this item, which will be added in the first position
        if(content.get(0).getEntryType() == Constants.ENTRY_TYPE_BACK){
            UnistorEntry backEntry = content.remove(0);
            Collections.sort(content, new UnistorEntryComparator());
            content.add(0, backEntry);
        }else{
            Collections.sort(content, new UnistorEntryComparator());
        }

        // Setting the adapter with the new items.
        // If the adapter have been previously created, we use notifyDataSetChanged to refresh,
        // that uses fairly less resources than creating a new one.
        if(this.listView.getAdapter() == null){
            UnistorEntryListAdapter listViewAdapter = new UnistorEntryListAdapter(mContext, content);
            this.listView.setAdapter(listViewAdapter);
            // Set context menu for the listview
            registerForContextMenu(listView);

        }else{
            UnistorEntryListAdapter listViewAdapter = (UnistorEntryListAdapter)this.listView.getAdapter();
            listViewAdapter.clear();
            listViewAdapter.addAll(content);
            listViewAdapter.notifyDataSetChanged();
        }


    }


    /**
     * Callback to refresh data when upload finishes
     */
    @Override
    public void onUploadFinish() {
        currentContent = this.loadContent(currentPath);
        populateContentListView(currentContent);
    }




}
