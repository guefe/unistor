package cen.unistor.app.activity;

import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Locale;

import cen.unistor.app.R;
import cen.unistor.app.adapter.UnistorEntry;
import cen.unistor.app.util.Constants;
import cen.unistor.app.util.SimpleFileDialog;


public class MainActivity extends ActionBarActivity {

    private static int ACCOUNT_NUMBER = 2;
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    private String nameFileToCopy;
    private String pathToCopyMove;
    // Initialize action to the default value, used when there is no action to do.
    private int copyMoveAction = Constants.ACTION_PASTE_DONE;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);




        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);


    
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        boolean result = true;

        switch (item.getItemId()){
            case R.id.action_settings:
                result = true;
                break;

            case R.id.action_upload:
                this.openFilePicker();
                break;

            case R.id.action_paste:
                this.pasteFile();
                invalidateOptionsMenu();
                break;
            default:
                result = super.onOptionsItemSelected(item);
        }

        return result;
    }

    /**
     *  Opens a file picker within a dialog.
     *  When a file is selected, uploadFile is fired.
     */
    private void openFilePicker(){
        final SimpleFileDialog fileOpenDialog =  new SimpleFileDialog(MainActivity.this, "FileOpen",
                new SimpleFileDialog.SimpleFileDialogListener()
                {
                    @Override
                    public void onChosenDir(String chosenDir)
                    {
                        // The code in this function will be executed when the dialog OK button is pushed
                        uploadFile(chosenDir);
                    }
                });

        fileOpenDialog.chooseFile_or_Dir();

    }


    public void uploadFile(String pathToUpload){
        UnistorFragment fragment = (UnistorFragment)getSupportFragmentManager().getFragments().get(mViewPager.getCurrentItem());

        fragment.uploadFile(pathToUpload);

    }

    public void pasteFile(){
        UnistorFragment fragment = (UnistorFragment)getSupportFragmentManager().getFragments().get(mViewPager.getCurrentItem());

        boolean result =fragment.pasteFile(this.getPathToCopyMove(), nameFileToCopy, this.copyMoveAction);

        if(result){
            this.copyMoveAction = Constants.ACTION_PASTE_DONE;
        }



    }



    /**
     * Relies the event handling on the appropriate fragment
     */
    @Override
    public void onBackPressed() {
        UnistorFragment fragment = (UnistorFragment)getSupportFragmentManager().getFragments().get(mViewPager.getCurrentItem());

        // if keyBackPressed returns false, means that app should exit
        if(!fragment.keyBackPressed()){
            super.onBackPressed();
        }
    }

    public String getPathToCopyMove() {
        return pathToCopyMove;
    }

    public int getCopyMoveAction(){
        return this.copyMoveAction;
    }

    public void setPathToCopy(String path) {
        this.pathToCopyMove = path;
        this.copyMoveAction = Constants.ACTION_COPY;
    }

    public void setPathToMove(String path) {
        this.pathToCopyMove = path;
        this.copyMoveAction = Constants.ACTION_MOVE;
    }

    public String getNameFileToCopy() {
        return nameFileToCopy;
    }

    public void setNameFileToCopy(String nameFileToCopy) {
        this.nameFileToCopy = nameFileToCopy;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).

            switch (position){
                case 0:
                    return new DropboxFragment();
                case 1:
                    return new BoxFragment();
                default:
                    return PlaceholderFragment.newInstance(position + 1);
            }


        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return ACCOUNT_NUMBER;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends UnistorFragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
//            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
//            textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }

        @Override
        public boolean keyBackPressed() {
            return false;
        }


        @Override
        public boolean uploadFile(String path) {
            return false;
        }

        @Override
        public boolean pasteFile(String source, String name, int mode) {
            return false;
        }


        @Override
        protected void deleteElement(String path) {

        }

        @Override
        protected ArrayList<UnistorEntry> loadContent(String path) {
            return null;
        }
    }





}
