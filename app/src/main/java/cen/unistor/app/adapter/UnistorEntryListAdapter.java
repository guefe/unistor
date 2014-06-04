package cen.unistor.app.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import cen.unistor.app.R;
import cen.unistor.app.util.Constants;

/**
 * Created by carlos on 12/05/14.
 */
public class UnistorEntryListAdapter extends ArrayAdapter<UnistorEntry>{

    private Context mContext;


    /*
     * Using the ViewHolder pattern, the number of findViewById calls gets reduced
     * by wrapping direct references to all inner views from a row. Thus, findViewById is only called
     * when the row layout is first created.
     * http://lucasr.org/2012/04/05/performance-tips-for-androids-listview/
     */


    public UnistorEntryListAdapter(Context context, List<UnistorEntry> itemList){
        super(context, R.layout.dropbox_list_item, itemList);
        //this.mItemList = itemList;
        mContext = context;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        /*
         * The convertView argument is essentially a "ScrapView" as described is Lucas post
         * http://lucasr.org/2012/04/05/performance-tips-for-androids-listview/
         * It will have a non-null value when ListView is asking you recycle the row layout.
         * So, when convertView is not null, you should simply update its contents instead of inflating a new row layout.
         */

        if(convertView==null){

            convertView = LayoutInflater.from(mContext).inflate(R.layout.dropbox_list_item, null);

            holder = new ViewHolder();
            holder.setIcon((ImageView)convertView.findViewById(R.id.entryIcon));
            holder.setName((TextView)convertView.findViewById(R.id.entryName));

            convertView.setTag(holder);
        }else {

            holder = (ViewHolder)convertView.getTag();
        }


        holder.getName().setText(this.getItem(position).getName());
        holder.getIcon().setImageResource(this.getItem(position).getEntryIcon(mContext));
        holder.setEntry(this.getItem(position));
        return convertView;
    }







}

