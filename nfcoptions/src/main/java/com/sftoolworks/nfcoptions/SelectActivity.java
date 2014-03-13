/*!
 * The MIT License (MIT)
 *
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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class SelectActivity extends ActionBarActivity {

    protected NfcAdapter nfcAdapter = null;
    protected PendingIntent pendingIntent = null;
    protected Boolean handledIntent = false;
    protected Boolean launchIntent = true;

    protected final String CHECKBOX_STATE = "checkboxList";
    protected final String KEY_STATE = "key";

    protected String selectKey = null;

    private static final String TAG = SelectActivity.class.getName();

    @Override
    protected void onPause() {

        nfcAdapter.disableForegroundDispatch(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!handledIntent) {
            if (getIntent().getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
                if (!launchIntent) {

                    Log.d( TAG, "This is a second tap" );
                    writeSelection( getIntent() );

                }
            }
            handledIntent = true;
        }

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[]{
                new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)}, null);

    }

    protected void writeSelection( Intent intent )
    {
        ListView list = (ListView)findViewById(R.id.listView1);
        if( list.getCount() == 0 ) return;

        try {

            Object[] results = ((OptionListAdapter)list.getAdapter()).getCheckedValues();
            JSONObject obj = new JSONObject();

            JSONArray array = new JSONArray();

            if (null != results) {
                for( Object result : results ) array.put(result.toString());
            }

            obj.put( "selection", array );
            obj.put( "key", selectKey );

            SharedPreferences sharedPref = getSharedPreferences(
                    getString(R.string.preference_file_key), Context.MODE_PRIVATE);

            // android studio (0.5.1) decorates this line as an error (some
            // of the time, anyway) but it's not an error.

            String identifier = sharedPref.getString( getString( R.string.user_id_key ), "" );
            if( identifier.length() > 0 ) obj.put( "user", identifier );

            String json = obj.toString(0);

            String outbound = "\u0002en";
            outbound += json;

            NdefRecord textRecord = new NdefRecord(
                    NdefRecord.TNF_WELL_KNOWN,
                    new byte[] { 'T' },
                    null,
                    outbound.getBytes() );

            NdefMessage message = new NdefMessage( new NdefRecord[]{ textRecord });
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Ndef ndef = Ndef.get(tag);
            ndef.connect();

            ndef.writeNdefMessage(message);
            ndef.close();

            Toast.makeText( this, R.string.write_ok, Toast.LENGTH_LONG ).show();
            new Timer().schedule( new TimerTask() {
                @Override
                public void run() { finish(); }
            }, 1500 );

        }
        catch( Exception e ) {

            Log.d( TAG, e.toString());
            String err = getString(R.string.tag_write_err) + "\n" + e.getMessage();
            Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {

        super.onRestoreInstanceState(savedInstanceState);
        ListView list = (ListView)findViewById(R.id.listView1);
        if( list.getCount() == 0 ) return;

        if( savedInstanceState.containsKey( CHECKBOX_STATE )) {
            boolean[] checkboxes = savedInstanceState.getBooleanArray(CHECKBOX_STATE );
            ((OptionListAdapter)(list.getAdapter())).restoreCheckState(checkboxes);
        }
        if( savedInstanceState.containsKey( KEY_STATE )) {
            selectKey = savedInstanceState.getString( KEY_STATE );
        }

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        ListView list = (ListView)findViewById(R.id.listView1);
        if( list.getCount() > 0 ) {
            savedInstanceState.putBooleanArray( CHECKBOX_STATE, ((OptionListAdapter)list.getAdapter()).getCheckState() );
        }
        if( null != selectKey )
            savedInstanceState.putString( KEY_STATE, selectKey );

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        handledIntent = false;
        launchIntent = false;

        setIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity( this, 0, new Intent(this, this.getClass()).addFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP  ), 0);

        Intent intent = getIntent();
        Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        String title = getString(R.string.default_select_title);

        TextView textViewBottom = (TextView)findViewById(R.id.textView2);
        textViewBottom.setText("");

        if (rawMessages != null) {

            ArrayList< Object > entries = new ArrayList< Object >();

            String data = null;

            for( Parcelable rawMessage : rawMessages  ) {
                NdefMessage message = (NdefMessage)rawMessage;
                NdefRecord[] records = message.getRecords();
                if( records.length > 1 )
                {
                    byte[] bArray = records[1].getPayload();
                    byte languageLength = bArray[0];

                    languageLength++; // because of the length byte

                    data = new String( bArray, languageLength, bArray.length - languageLength );
                }
            }

            try
            {
                JSONObject json = (JSONObject)new JSONTokener(data).nextValue();
                if( json.has("title")) title = json.getString("title");
                if( json.has("key")) selectKey = json.getString("key");
                if( json.has("options"))
                {
                    JSONArray arr = json.getJSONArray("options");
                    for( int i = 0; i< arr.length(); i++ ) {
                        entries.add(parseJObject(arr.getJSONObject(i)));
                    }
                }

                ListView list = (ListView)findViewById(R.id.listView1);
                list.setAdapter( new OptionListAdapter( this, entries.toArray()));

                list.setOnItemClickListener(new ListView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
                        CheckBox cb = (CheckBox)view.findViewById(R.id.optionListCheckBox);
                        cb.setChecked(!cb.isChecked());
                    }
                });

                textViewBottom.setText(getString(R.string.tap_again));

            }
            catch( Exception e )
            {
                String message = getString( R.string.json_err );
                message += "\n";
                message += e.getMessage();

                Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                Log.d( TAG, getString( R.string.json_err ));
                Log.d( TAG, e.toString());
                Log.d( TAG, data );
            }
        }
        else {
            title = getString( R.string.no_tag );
        }

        TextView msg = (TextView)findViewById(R.id.textView1);
        msg.setText( title );

    }


    protected Object parseJObject( JSONObject obj ) throws JSONException {

        String[] elements = new String[3];
        if( obj.has( "title" ) && obj.has( "label" ))
        {
            elements[0] = obj.getString("title");
            elements[1] = obj.getString("label");

            if( obj.has( "value" )) elements[2] = obj.getString("value");
            else elements[2] = obj.getString("title");

            return elements;
        }
        else throw new JSONException( "Missing required element ");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.select, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
