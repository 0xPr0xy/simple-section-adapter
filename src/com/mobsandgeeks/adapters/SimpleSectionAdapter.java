/*
 * Copyright (C) 2012 Mobs and Geeks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobsandgeeks.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A very simple adapter that adds sections to adapters written for {@link ListView}s.
 * <br />
 * <b>NOTE: The adapter assumes that the data source of the decorated list adapter is sorted.</b>
 *
 * @author Ragunath Jawahar R <rj@mobsandgeeks.com>
 * @version 0.2
 */
public class SimpleSectionAdapter<T> extends BaseAdapter implements SectionIndexer {
    // Debug
    static final boolean DEBUG = false;
    static final String TAG = SimpleSectionAdapter.class.getSimpleName();

    // Constants
    private static final int VIEW_TYPE_SECTION_HEADER = 0;

    // Attributes
    private Context mContext;
    private BaseAdapter mListAdapter;
    private int mSectionHeaderLayoutId;
    private int mSectionTitleTextViewId;
    private LinearLayout mSectionedListSidebar;
    private Sectionizer<T> mSectionizer;
    private LinkedHashMap<String, Integer> mSections;

    /**
     * Constructs a {@linkplain SimpleSectionAdapter}.
     * 
     * @param context The context for this adapter.
     * @param listAdapter A {@link ListAdapter} that has to be sectioned.
     * @param sectionHeaderLayoutId Layout Id of the layout that is to be used for the header. 
     * @param sectionTitleTextViewId Id of a TextView present in the section header layout.
     * @param sectionizer Sectionizer for sectioning the {@link ListView}.
     */
    public SimpleSectionAdapter(Context context, BaseAdapter listAdapter, 
            int sectionHeaderLayoutId, int sectionTitleTextViewId, LinearLayout sectionedListSidebar,
            Sectionizer<T> sectionizer) {
        if(context == null) {
            throw new IllegalArgumentException("context cannot be null.");
        } else if(listAdapter == null) {
            throw new IllegalArgumentException("listAdapter cannot be null.");
        } else if(sectionizer == null) {
            throw new IllegalArgumentException("sectionizer cannot be null.");
        } else if(!isTextView(context, sectionHeaderLayoutId, sectionTitleTextViewId)) {
            throw new IllegalArgumentException("sectionTitleTextViewId should be a TextView.");
        }

        this.mContext = context;
        this.mListAdapter = listAdapter;
        this.mSectionHeaderLayoutId = sectionHeaderLayoutId;
        this.mSectionTitleTextViewId = sectionTitleTextViewId;
        this.mSectionizer = sectionizer;
        this.mSections = new LinkedHashMap<String, Integer>();

        // Find sections
        findSections();

        if(sectionedListSidebar != null){
            mSectionedListSidebar = sectionedListSidebar;
            addAlphabetScroller();
        }
    }

    private void addAlphabetScroller()
    {
        ViewGroup.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        for(String section : this.getSiderbarSectionLetters()){
            TextView v = new TextView(mContext);
            v.setTextColor(Color.BLACK);
            v.setText(section);
            v.setTag(section);
            v.setGravity(Gravity.CENTER);
            mSectionedListSidebar.addView(v, lp);
        }

        mSectionedListSidebar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getActionMasked() == MotionEvent.ACTION_UP){
                    LinearLayout layout = (LinearLayout)v;
                    for(int i =0; i< layout.getChildCount(); i++)
                    {
                        View view = layout.getChildAt(i);
                        if(view != null){
                            Rect outRect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
                            if(outRect.contains((int)event.getX(), (int)event.getY()))
                            {
                                int position = getPositionForSectionName((String)view.getTag());
                                mSectionizer.smoothScrollToPosition(position);
                            }
                        }
                    }
                }
                return true;
            }
        });
    }

    private boolean isTextView(Context context, int layoutId, int textViewId) {
        View inflatedView = View.inflate(context, layoutId, null);
        View foundView = inflatedView.findViewById(textViewId);

        return foundView instanceof TextView;
    }

    @Override
    public int getCount() {
        return mListAdapter.getCount() + getSectionCount();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        SectionHolder sectionHolder = null;

        switch (getItemViewType(position)) {
        case VIEW_TYPE_SECTION_HEADER:
            if(view == null) {
                view = View.inflate(mContext, mSectionHeaderLayoutId, null);

                sectionHolder = new SectionHolder();
                sectionHolder.titleTextView = (TextView) view.findViewById(mSectionTitleTextViewId);

                view.setTag(sectionHolder);
            } else {
                sectionHolder = (SectionHolder) view.getTag();
            }
            break;

        default:
            view = mListAdapter.getView(getIndexForPosition(position), 
                    convertView, parent);
            break;
        }

        if(sectionHolder != null) {
            String sectionName = sectionTitleForPosition(position);
            sectionHolder.titleTextView.setText(sectionName);
        }

        return view;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return mListAdapter.areAllItemsEnabled() ? 
                mSections.size() == 0 : false;
    }

    @Override
    public int getItemViewType(int position) {
        int positionInCustomAdapter = getIndexForPosition(position);
        return mSections.values().contains(position) ? 
                VIEW_TYPE_SECTION_HEADER : 
                    mListAdapter.getItemViewType(positionInCustomAdapter) + 1;
    }

    @Override
    public int getViewTypeCount() {
        return mListAdapter.getViewTypeCount() + 1;
    }

    @Override
    public boolean isEnabled(int position) {
        return mSections.values().contains(position) ? 
                false : mListAdapter.isEnabled(getIndexForPosition(position));
    }

    @Override
    public Object getItem(int position) {
        return mListAdapter.getItem(getIndexForPosition(position));
    }

    @Override
    public long getItemId(int position) {
        return mListAdapter.getItemId(getIndexForPosition(position));
    }

    @Override
    public void notifyDataSetChanged() {
        mListAdapter.notifyDataSetChanged();
        findSections();
        super.notifyDataSetChanged();
    }

    /**
     * Returns the actual index of the object in the data source linked to the this list item.
     * 
     * @param position List item position in the {@link ListView}.
     * @return Index of the item in the wrapped list adapter's data source.
     */
    public int getIndexForPosition(int position) {
        int nSections = 0;

        Set<Entry<String, Integer>> entrySet = mSections.entrySet();
        for(Entry<String, Integer> entry : entrySet) {
            if(entry.getValue() < position) {
                nSections++;
            }
        }

        return position - nSections;
    }

    public String[] getSiderbarSectionLetters() {

        List<String> list = new ArrayList<String>();
        for(Entry<String, Integer> entry : mSections.entrySet()) {
            list.add(entry.getKey().substring(0,1));
        }
        String[] strArray = list.toArray(new String[0]);
        return list.toArray(strArray);
    }

    @Override
    public String[] getSections() {

        List<String> list = new ArrayList<String>();
        for(Entry<String, Integer> entry : mSections.entrySet()) {
            list.add(entry.getKey());
        }
        String[] strArray = list.toArray(new String[0]);
        return list.toArray(strArray);
    }

    @Override
    public int getPositionForSection(int section) {
        int i = 0;
        for(Entry<String, Integer> entry : mSections.entrySet()) {
            if(i == section) {
                return entry.getValue();
            }
            i++;
        }
        return section;
    }

    @Override
    public int getSectionForPosition(int position) {
        int i = 0;
        for(Entry<String, Integer> entry : mSections.entrySet()) {
            if(entry.getValue() == position) {
                return i;
            }
            i++;
        }
        return position;
    }

    private int getPositionForSectionName(String name){
        for(Entry<String, Integer> entry : mSections.entrySet()) {
            if(entry.getKey().substring(0,1).equals(name)){
                return entry.getValue();
            }
        }
        return 0;
    }

    static class SectionHolder {
        public TextView titleTextView;
    }

    private void findSections() {
        int n = mListAdapter.getCount();
        int nSections = 0;
        mSections.clear();

        for(int i=0; i<n; i++) {
            String sectionName = mSectionizer.getSectionTitleForItem((T) mListAdapter.getItem(i));

            if(!mSections.containsKey(sectionName)) {
                mSections.put(sectionName, i + nSections);
                nSections ++;
            }
        }

        if(DEBUG) {
            Log.d(TAG, String.format("Found %d sections.", mSections.size()));
        }
    }

    private int getSectionCount() {
        return mSections.size();
    }

    private String sectionTitleForPosition(int position) {
        String title = null;

        Set<Entry<String, Integer>> entrySet = mSections.entrySet();
        for(Entry<String, Integer> entry : entrySet) {
            if(entry.getValue() == position) {
                title = entry.getKey();
                break;
            }
        }
        return title;
    }

}
