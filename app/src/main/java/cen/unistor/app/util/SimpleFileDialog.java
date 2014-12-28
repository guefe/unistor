// SimpleFileDialog.java
package cen.unistor.app.util;

/*
* 
* This file is licensed under The Code Project Open License (CPOL) 1.02 
* http://www.codeproject.com/info/cpol10.aspx
* http://www.codeproject.com/info/CPOL.zip
* 
* License Preamble:
* This License governs Your use of the Work. This License is intended to allow developers to use the Source
* Code and Executable Files provided as part of the Work in any application in any form.
* 
* The main points subject to the terms of the License are:
*    Source Code and Executable Files can be used in commercial applications;
*    Source Code and Executable Files can be redistributed; and
*    Source Code can be modified to create derivative works.
*    No claim of suitability, guarantee, or any warranty whatsoever is provided. The software is provided "as-is".
*    The Article(s) accompanying the Work may not be distributed or republished without the Author's consent
* 
* This License is entered between You, the individual or other entity reading or otherwise making use of
* the Work licensed pursuant to this License and the individual or other entity which offers the Work
* under the terms of this License ("Author").
*  (See Links above for full license text)
*/

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
//import android.content.DialogInterface.OnKeyListener;
import android.os.Environment;
import android.text.Editable;
import android.util.Log;
import android.view.Gravity;
//import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SimpleFileDialog 
{
	private final int fileOpen = 0;
	private final int fileSave = 1;
	private final int folderChoose = 2;
	private int selectType = fileSave;
	private String mSdcardDirectory = "";
	private Context mContext;
	private TextView mTitleView1;
	private TextView mTitleView;
	public String defaultFileName = "default.txt";
	private String selectedFileName = defaultFileName;
	private EditText inputText;
	
	private String mDir = "";
	private List<String> mSubdirs = null;
	private SimpleFileDialogListener mSimpleFileDialogListener = null;
	private ArrayAdapter<String> mListAdapter = null;

	//////////////////////////////////////////////////////
	// Callback interface for selected directory
	//////////////////////////////////////////////////////
	public interface SimpleFileDialogListener 
	{
		public void onChosenDir(String chosenDir);
	}

	public SimpleFileDialog(Context context, String file_select_type, SimpleFileDialogListener SimpleFileDialogListener)
	{
		if (file_select_type.equals("fileOpen"))          selectType = fileOpen;
		else if (file_select_type.equals("fileSave"))     selectType = fileSave;
		else if (file_select_type.equals("folderChoose")) selectType = folderChoose;
		else selectType = fileOpen;
		
		mContext = context;
        //if(Environment.getExternalStorageDirectory().equals(Environment.MEDIA_MOUNTED)){
            mSdcardDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
            Log.i("external storage path", mSdcardDirectory);
        //mSdcardDirectory="/storage/"

        //}else{
          //  mSdcardDirectory = Environment.get
        //}

		mSimpleFileDialogListener = SimpleFileDialogListener;

		try
		{
			mSdcardDirectory = new File(mSdcardDirectory).getCanonicalPath();
		}
		catch (IOException ioe)
		{
		}
	}

	///////////////////////////////////////////////////////////////////////
	// chooseFileOrDir() - load directory chooser dialog for initial
	// default sdcard directory
	///////////////////////////////////////////////////////////////////////
	public void chooseFileOrDir()
	{
		// Initial directory is sdcard directory
		if (mDir.equals(""))	chooseFileOrDir(mSdcardDirectory);
		else chooseFileOrDir(mDir);
	}

	////////////////////////////////////////////////////////////////////////////////
	// chooseFileOrDir(String dir) - load directory chooser dialog for initial
	// input 'dir' directory
	////////////////////////////////////////////////////////////////////////////////
	public void chooseFileOrDir(String dir)
	{
		File dirFile = new File(dir);
		if (! dirFile.exists() || ! dirFile.isDirectory())
		{
			dir = mSdcardDirectory;
		}

		try
		{
			dir = new File(dir).getCanonicalPath();
		}
		catch (IOException ioe)
		{
			return;
		}

		mDir = dir;
		mSubdirs = getDirectories(dir);

		class SimpleFileDialogOnClickListener implements DialogInterface.OnClickListener
		{
			public void onClick(DialogInterface dialog, int item) 
			{
				String m_dir_old = mDir;
				String sel = "" + ((AlertDialog) dialog).getListView().getAdapter().getItem(item);
				if (sel.charAt(sel.length()-1) == '/')	sel = sel.substring(0, sel.length()-1);
				
				// Navigate into the sub-directory
				if (sel.equals(".."))
				{
					   mDir = mDir.substring(0, mDir.lastIndexOf("/"));
				}
				else
				{
					   mDir += "/" + sel;
				}
				selectedFileName = defaultFileName;
				
				if ((new File(mDir).isFile())) // If the selection is a regular file
				{
					mDir = m_dir_old;
					selectedFileName = sel;
                    mSimpleFileDialogListener.onChosenDir(mDir + "/" + selectedFileName);
                    dialog.dismiss();
				}
				
				updateDirectory();
			}
		}

		AlertDialog.Builder dialogBuilder = createDirectoryChooserDialog(dir, mSubdirs,
				new SimpleFileDialogOnClickListener());

		dialogBuilder.setNegativeButton("Cancel", null);

		final AlertDialog dirsDialog = dialogBuilder.create();

		// Show directory chooser dialog
		dirsDialog.show();
	}

	private boolean createSubDir(String newDir)
	{
		File newDirFile = new File(newDir);
		if   (! newDirFile.exists() ) return newDirFile.mkdir();
		else return false;
	}
	
	private List<String> getDirectories(String dir)
	{
		List<String> dirs = new ArrayList<String>();
		try
		{
			File dirFile = new File(dir);
			
			// if directory is not the base sd card directory add ".." for going up one directory
			if (! mDir.equals(mSdcardDirectory) ) dirs.add("..");
			
			if (! dirFile.exists() || ! dirFile.isDirectory())
			{
				return dirs;
			}

			for (File file : dirFile.listFiles()) 
			{
				if ( file.isDirectory())
				{
					// Add "/" to directory names to identify them in the list
					dirs.add( file.getName() + "/" );
				}
				else if (selectType == fileSave || selectType == fileOpen)
				{
					// Add file names to the list if we are doing a file save or file open operation
					dirs.add( file.getName() );
				}
			}
		}
		catch (Exception e)	{}

		Collections.sort(dirs, new Comparator<String>()
		{	
			public int compare(String o1, String o2) 
			{
				return o1.compareTo(o2);
			}
		});
		return dirs;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////                                   START DIALOG DEFINITION                                    //////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	private AlertDialog.Builder createDirectoryChooserDialog(String title, List<String> listItems,
			DialogInterface.OnClickListener onClickListener)
	{
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);

		////////////////////////////////////////////////
		// Create title text showing file select type // 
		////////////////////////////////////////////////
		mTitleView1 = new TextView(mContext);
		mTitleView1.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		//mTitleView1.setTextAppearance(mContext, android.R.style.TextAppearance_Large);
		//mTitleView1.setTextColor( mContext.getResources().getColor(android.R.color.black) );
				
		if (selectType == fileOpen) mTitleView1.setText("Open:");
		if (selectType == fileSave) mTitleView1.setText("Save As:");
		if (selectType == folderChoose) mTitleView1.setText("Folder Select:");
		
		//need to make this a variable Save as, Open, Select Directory
		mTitleView1.setGravity(Gravity.CENTER_VERTICAL);
		mTitleView1.setBackgroundColor(-12303292); // dark gray 	-12303292
		mTitleView1.setTextColor(mContext.getResources().getColor(android.R.color.white));

		// Create custom view for AlertDialog title
		LinearLayout titleLayout1 = new LinearLayout(mContext);
		titleLayout1.setOrientation(LinearLayout.VERTICAL);
		titleLayout1.addView(mTitleView1);


		if (selectType == folderChoose || selectType == fileSave)
		{
			///////////////////////////////
			// Create New Folder Button  //
			///////////////////////////////
			Button newDirButton = new Button(mContext);
			newDirButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			newDirButton.setText("New Folder");
			newDirButton.setOnClickListener(new View.OnClickListener() 
			{
				@Override
				public void onClick(View v) 
				{
					final EditText input = new EditText(mContext);

					// Show new folder2 name input dialog
					new AlertDialog.Builder(mContext).
					setTitle("New Folder Name").
					setView(input).setPositiveButton("OK", new DialogInterface.OnClickListener() 
					{
						public void onClick(DialogInterface dialog, int whichButton) 
						{
							Editable newDir = input.getText();
							String newDirName = newDir.toString();
							// Create new directory
							if ( createSubDir(mDir + "/" + newDirName) )
							{
								// Navigate into the new directory
								mDir += "/" + newDirName;
								updateDirectory();
							}
							else
							{
								Toast.makeText(mContext, "Failed to create '"
										+ newDirName + "' folder2", Toast.LENGTH_SHORT).show();
							}
						}
					}).setNegativeButton("Cancel", null).show(); 
				}
			}
					);
			titleLayout1.addView(newDirButton);
		}

		/////////////////////////////////////////////////////
		// Create View with folder2 path and entry text box //
		/////////////////////////////////////////////////////
		LinearLayout titleLayout = new LinearLayout(mContext);
		titleLayout.setOrientation(LinearLayout.VERTICAL);
		
		mTitleView = new TextView(mContext);
		mTitleView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		//mTitleView.setBackgroundColor(-12303292); // dark gray -12303292
        mTitleView.setBackgroundColor(mContext.getResources().getColor(android.R.color.white)); // dark gray -12303292
		mTitleView.setTextColor(mContext.getResources().getColor(android.R.color.white));
		mTitleView.setGravity(Gravity.CENTER_VERTICAL);
		mTitleView.setText(title);

		titleLayout.addView(mTitleView);
		
		if (selectType == fileSave)
		{
			inputText = new EditText(mContext);
			inputText.setText(defaultFileName);
			titleLayout.addView(inputText);
		}
		//////////////////////////////////////////
		// Set Views and Finish Dialog builder  //
		//////////////////////////////////////////
		dialogBuilder.setView(titleLayout);
        dialogBuilder.setTitle(title);
		//dialogBuilder.setCustomTitle(titleLayout1);
		mListAdapter = createListAdapter(listItems);
		dialogBuilder.setSingleChoiceItems(mListAdapter, -1, onClickListener);
		dialogBuilder.setCancelable(false);
		return dialogBuilder;
	}

	private void updateDirectory()
	{
		mSubdirs.clear();
		mSubdirs.addAll(getDirectories(mDir));
		mTitleView.setText(mDir);
		mListAdapter.notifyDataSetChanged();
		//#scorch
		if (selectType == fileSave)
		{
			inputText.setText(selectedFileName);
		}
	}

	private ArrayAdapter<String> createListAdapter(List<String> items)
	{
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_item, android.R.id.text1, items)
        {

            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView)
                {
                    // Enable list item (directory) text wrapping
                    TextView tv = (TextView) v;
                    tv.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
                    tv.setEllipsize(null);
                }

                return v;
            }
        };

		return adapter;
	}
} 
