package cen.unistor.app.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import cen.unistor.app.R;

/**
 * Created by carlos on 17/11/14.
 */
public class ServiceSelectionDialogFragment extends DialogFragment {

    public static String TAG = "ServiceSelection";

    public interface ServiceSelectionDialogListener {
        public void OnServiceSelected(int selection);
    }

    private ServiceSelectionDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (ServiceSelectionDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Integer[] icons = new Integer[] {R.drawable.dropbox_icon, R.drawable.box_icon};
        ListAdapter adapter = new ArrayAdapterWithIcon(getActivity(),
                getResources().getStringArray(R.array.account_options), icons);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                mListener.OnServiceSelected(i);
            }
        });

        return builder.create();
    }


    public class ArrayAdapterWithIcon extends ArrayAdapter<String> {

        private List<Integer> images;

        public ArrayAdapterWithIcon(Context context, List<String> items, List<Integer> images) {
            super(context, R.layout.service_select_dialog_item, items);
            this.images = images;
        }

        public ArrayAdapterWithIcon(Context context, String[] items, Integer[] images) {
            super(context, R.layout.service_select_dialog_item, items);
            this.images = Arrays.asList(images);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //View view = super.getView(position, convertView, parent);
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.service_select_dialog_item, null);
            TextView textView = (TextView) convertView.findViewById(R.id.service_name);
            textView.setText(getItem(position));
            ImageView image = (ImageView) convertView.findViewById(R.id.service_icon);
            image.setImageDrawable(getResources().getDrawable(images.get(position)));
            return convertView;
        }

    }
}
