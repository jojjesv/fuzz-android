package com.fuzz.android.adapter;

import android.content.Context;
import android.graphics.drawable.ShapeDrawable;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.fuzz.android.R;
import com.fuzz.android.view.DefaultTypefaces;

/**
 * Adapter for categories list.
 */
public class CategoriesAdapter extends ArrayAdapter<CategoriesAdapter.CategoryData> {

    public CategoriesAdapter(@NonNull Context context, @NonNull CategoryData[] objects) {
        super(context, 0, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        boolean convertWasNull = convertView == null;

        if (convertWasNull) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.category_item, parent, false);
            convertView.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            //  Initially hidden
            convertView.setTranslationX(-convertView.getMeasuredWidth());
        }

        CategoryData dataAtPosition = getItem(position);

        View wrapper = ((ViewGroup)convertView).getChildAt(0);

        TextView textView = (TextView) wrapper.findViewById(R.id.text);
        textView.setText(dataAtPosition.name);

        wrapper.getBackground().setTint(dataAtPosition.color);

        if (convertWasNull) {
            textView.setTypeface(DefaultTypefaces.getDefaultHeader());
        }

        View removeView = wrapper.findViewById(R.id.remove);
        removeView.setVisibility(dataAtPosition.enabled ? View.VISIBLE : View.GONE);

        return convertView;
    }

    public static class CategoryData {
        public int id;
        public String name;
        public int color;
        public boolean enabled;

        public CategoryData(int id, String name, int color) {
            this.id = id;
            this.name = name;
            this.color = color;
        }
    }
}
