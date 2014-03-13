/*!
 * Copyright(c) 2014 SF Toolworks <info@sftoolworks.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.sftoolworks.nfcoptions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;

public class OptionListAdapter extends ArrayAdapter< Object >{

    private final Context context;
    private final Object[] values;
    private boolean[] checkState;

    public OptionListAdapter(Context context, Object[] values) {
        super(context, R.layout.list_entry, values);
        this.context = context;
        this.values = values;
        checkState = new boolean[values.length];
    }

    public void restoreCheckState( boolean[] checks )
    {
        System.arraycopy( checks, 0, checkState, 0, checks.length );
        notifyDataSetChanged();
    }

    public boolean[] getCheckState(){
        return checkState;
    }

    public Object[] getCheckedValues(){
        ArrayList< Object > results = new ArrayList< Object >();

        for( int i = 0; i< values.length; i++ ){
            if( checkState[i] ) {
                String[] elements = (String[])values[i];
                results.add( elements[2] );
            }
        }
        return results.size() > 0 ? results.toArray() : null;
    }

    protected CompoundButton.OnCheckedChangeListener checkChangeListener = new
            CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            checkState[(Integer)compoundButton.getTag()] = b;
        }
    };

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.list_entry, parent, false);

        TextView textView = (TextView) rowView.findViewById(R.id.optionListTextView);
        CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.optionListCheckBox);

        String[] elements = (String[])values[position];

        checkBox.setText(elements[0]);
        checkBox.setTag(position);
        checkBox.setOnCheckedChangeListener( checkChangeListener );
        checkBox.setChecked( checkState[position] );

        textView.setText(elements[1]);

        checkBox.setFocusable(false);
        textView.setFocusable(false);

        return rowView;
    }

}
