package cen.unistor.app.util;

import java.util.Comparator;

import cen.unistor.app.adapter.UnistorEntry;

/**
 * Created by carlos on 21/05/14.
 */
public class DropboxEntryComparator implements Comparator<UnistorEntry> {

    @Override
    /**
     * Compares two UnistorEntry so that folders go first and then sorts alphabetically.
     */
    public int compare(UnistorEntry unistorEntry, UnistorEntry unistorEntry2) {
        if(unistorEntry.isFolder() && !unistorEntry2.isFolder()){
            return -1;
        }else if (!unistorEntry.isFolder() && unistorEntry2.isFolder()){
            return 1;
        }else{
            return unistorEntry.getName().compareTo(unistorEntry2.getName());
        }
    }
}
