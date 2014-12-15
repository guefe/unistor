package cen.unistor.app.activity;

import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cen.unistor.app.R;
import cen.unistor.app.util.Constants;
import cen.unistor.app.util.ServiceSelectionDialogFragment;
import cen.unistor.app.util.SimpleFileDialog;
import cen.unistor.app.util.ZoomOutPageTransformer;




public class MainActivity extends ActionBarActivity implements ServiceSelectionDialogFragment.ServiceSelectionDialogListener{

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


    private Button btnAddAccount;
    private boolean logged = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, 0);
        if(prefs != null){
            Map valores = prefs.getAll();
            if (!valores.isEmpty()){
                logged = true;
            }
        }

        if(logged) {
            buildActiveView();
        }else {
            this.buildInactiveView();
        }

    
    }

    public void addAccount(){
        //DialogFragment dialog = new ServiceSelectionDialogFragment();
        DialogFragment dialog;
        if (logged){
            dialog = ServiceSelectionDialogFragment
                    .newInstance(mSectionsPagerAdapter.getAccountStatus());
        } else {
            boolean[] accountStatus = {false, false};
            dialog = ServiceSelectionDialogFragment
                    .newInstance(accountStatus);
        }

        dialog.show(getFragmentManager(), ServiceSelectionDialogFragment.TAG);
    }


    @Override
    public void OnServiceSelected(int selection) {
//        Toast.makeText(this, "Selección: "+selection, Toast.LENGTH_LONG).show();

        if(!logged) {
            setContentView(R.layout.activity_main);

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            // Create the adapter that will return a fragment for each of the three
            // primary sections of the activity.
            mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());



            if (selection == 0){
                mSectionsPagerAdapter.addDropbox();
            }else{
                mSectionsPagerAdapter.addBox();
            }

            // Set up the ViewPager with the sections adapter.
            mViewPager = (ViewPager) findViewById(R.id.pager);
            mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());
            mViewPager.setAdapter(mSectionsPagerAdapter);


            mViewPager.setCurrentItem(selection);
            logged = true;

        } else {
            if (selection == 0){
                mSectionsPagerAdapter.addDropbox();
                mSectionsPagerAdapter.notifyDataSetChanged();
                mViewPager.setCurrentItem(0);
            }else{
                mSectionsPagerAdapter.addBox();
                mSectionsPagerAdapter.notifyDataSetChanged();
                mViewPager.setCurrentItem(1);
            }
        }

    }

    private void buildActiveView(){




    }

    /**
     *
     * Builds the view showed when there is no active account for neither DropBox nor Box
     */
    public void buildInactiveView(){
        setContentView(R.layout.main_activity_no_account);

        this.btnAddAccount = (Button)findViewById(R.id.add_account_btn);

        this.btnAddAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                addAccount();


            }
        });

        invalidateOptionsMenu();
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        if(logged)
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
            case R.id.action_logout:

                for (Fragment fragment : getSupportFragmentManager().getFragments()){

                    ((UnistorFragment)fragment).logOut();
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                    transaction.remove(fragment);
                    transaction.commit();
                }

                logged = false;

                this.buildInactiveView();

                break;

            case R.id.action_add_account:
                addAccount();
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


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBooleanArray("accountStatus", this.mSectionsPagerAdapter.getAccountStatus());
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private int accountNumber = 0;
        private boolean dropboxVisible = false;
        private boolean boxVisible = false;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).

            if (position == 0 && dropboxVisible){
                return new DropboxFragment();

            } else if (position == 1 && boxVisible){
                return new BoxFragment();

            } else {
                return null;
            }

        }

        @Override
        public int getCount() {
            // Show one fragment per account
            return this.accountNumber;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();

            if (position == 0){
                return getString(R.string.title_section1).toUpperCase(l);

            } else if (position == 1){
                return getString(R.string.title_section2).toUpperCase(l);

            } else {
                return null;
            }

        }

        public void addDropbox() {
            this.dropboxVisible = true;
            this.accountNumber++;
        }

        public void addBox() {
            this.boxVisible = true;
            this.accountNumber++;
        }


        public boolean[] getAccountStatus(){
            boolean[] result = {dropboxVisible, boxVisible};
            return result;
        }
    }


}
