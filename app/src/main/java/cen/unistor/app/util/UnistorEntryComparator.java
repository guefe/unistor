package cen.unistor.app.util;

import java.util.Comparator;

import cen.unistor.app.adapter.UnistorEntry;

/**
 * Created by carlos on 21/05/14.
 */
public class UnistorEntryComparator implements Comparator<UnistorEntry> {

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
            String ext1 = unistorEntry.getName().substring(unistorEntry.getName().lastIndexOf('.')+1).toLowerCase();
            String ext2 = unistorEntry2.getName().substring(unistorEntry2.getName().lastIndexOf('.')+1).toLowerCase();
            if (ext1.compareTo(ext2) != 0){
                return ext1.compareTo(ext2);
            }else{
                return unistorEntry.getName().compareToIgnoreCase(unistorEntry2.getName());
            }

        }
    }
}
