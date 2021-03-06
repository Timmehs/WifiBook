package com.tim.wifibook;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;

/**
 * Created by Tim Sandberg on 2/3/14.
 */
public class SettingsFragment extends Fragment {
    public static final String MyPREFERENCES = "MyPrefs";
    private static SharedPreferences prefs;
    public static ArrayList<WifiConfiguration> mScanResults;
    public boolean scanRetrieved = false;
    boolean wasRunning;
    boolean launchOnStartup;
    public static String SCAN_INTERVAL = "Scan Interval";
    public static String LAUNCH_ON_STARTUP = "Startup settings";
    private static final String TAG = "SettingsFragment";


    public static  ArrayList<WifiConfiguration> getScanResults() {
        return mScanResults;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        prefs = getActivity().getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        launchOnStartup = prefs.getBoolean(LAUNCH_ON_STARTUP, false);
        //Note if service is running (may need to pause for net mgmt)
        wasRunning = WifiService.isRunning;
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            getActivity().getActionBar().setTitle("Settings");
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_settings, container, false);

        /* STARTUP SEGMENT */
        CheckBox cb = (CheckBox) v.findViewById(R.id.startup_cb);
        cb.setChecked(launchOnStartup);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor edit = prefs.edit();
                if(isChecked) {
                    launchOnStartup = true;
                    edit.putBoolean(LAUNCH_ON_STARTUP, launchOnStartup);
                    edit.commit();
                    Log.d(TAG,"Preferences Updated (startup)");
                }
                else
                    launchOnStartup = false;
                    edit.putBoolean(LAUNCH_ON_STARTUP, launchOnStartup);
                    edit.commit();
                    Log.d(TAG,"Preferences Updated (startup)");
            }
        });

        /* SEEK BAR SEGMENT */
        Spinner freq_spinner = (Spinner) v.findViewById(R.id.scan_spinner);
        final TextView frequencyTv = (TextView) v.findViewById(R.id.frequency_view);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),R.array.frequency_values,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        freq_spinner.setAdapter(adapter);
        int interval = prefs.getInt(SCAN_INTERVAL, 30000);
        switch(interval) {
            case 10000:
                freq_spinner.setSelection(0);
                break;
            case 30000:
                freq_spinner.setSelection(1);
                break;
            case 60000:
                freq_spinner.setSelection(2);
                break;
            case 300000:
                freq_spinner.setSelection(3);
                break;
            default:
                freq_spinner.setSelection(1);
                break;
        }

        freq_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int[] freqs = new int[]{10000, 30000, 60000, 300000};
                SharedPreferences.Editor edit = prefs.edit();
                edit.putInt(SCAN_INTERVAL, freqs[position]);
                edit.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Button manageButton = (Button) v.findViewById(R.id.manage_btn);
        manageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(WifiService.isRunning) {
                    wasRunning = true;
                    WifiService.isRunning = false;
                    getActivity().stopService(new Intent(getActivity(), WifiService.class));
                    makeImageToast(getActivity(), "WifiBook Paused for Network Management",Toast.LENGTH_SHORT).show();
                }
                Intent i = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivityForResult(i, 45);
            }
        });

        Button contactBtn = (Button) v.findViewById(R.id.contact_btn);
        contactBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    Intent i = new Intent(Intent.ACTION_SEND);

                    i.setData(Uri.parse("mailto:"));
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_EMAIL, new String[] {"tasandberg@gmail.com"});
                    i.putExtra(Intent.EXTRA_SUBJECT, "WifiBook Feedback");
                    try {
                        startActivity(Intent.createChooser(i, "Send feedback..."));
                        getActivity().finish();
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(getActivity(), "No suitable email client found.  Please email developer at tasandberg@gmail.com", Toast.LENGTH_SHORT).show();
                        Log.d(TAG,"No email client found");
                    }

            }
        });
        Button ratetBtn = (Button) v.findViewById(R.id.rate_btn);
        ratetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=com.tim.wifibook"));
                startActivity(intent);
            }
        });

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(wasRunning) {
            getActivity().startService(new Intent(getActivity(), WifiService.class));
        }
    }

    public static Toast makeImageToast(Context context, CharSequence text, int length) {
        Toast toast = Toast.makeText(context, text, length);

        View rootView = toast.getView();
        LinearLayout linearLayout = null;
        View messageTextView = null;

        // check (expected) toast layout
        if (rootView instanceof LinearLayout) {
            linearLayout = (LinearLayout) rootView;

            if (linearLayout.getChildCount() == 1) {
                View child = linearLayout.getChildAt(0);

                if (child instanceof TextView) {
                    messageTextView = child;
                }
            }
        }

        // cancel modification because toast layout is not what we expected
        if (linearLayout == null || messageTextView == null) {
            return toast;
        }

        ViewGroup.LayoutParams textParams = messageTextView.getLayoutParams();
        ((LinearLayout.LayoutParams) textParams).gravity = Gravity.CENTER_VERTICAL;

        // convert dip dimension
        float density = context.getResources().getDisplayMetrics().density;
        int imageSize = (int) (density * 25 + 0.5f);
        int imageMargin = (int) (density * 15 + 0.5f);

        // setup image view layout parameters
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(imageSize, imageSize);
        imageParams.setMargins(0, 0, imageMargin, 0);
        imageParams.gravity = Gravity.CENTER_VERTICAL;

        // setup image view
        ImageView imageView = new ImageView(context);
        imageView.setImageResource(R.drawable.icon);
        imageView.setLayoutParams(imageParams);

        // modify root layout
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.addView(imageView, 0);

        return toast;
    }
}




// GRAVEYARD //

        /*

         if(WifiService.isRunning) radioGroup.check(R.id.radio_auto);
        else radioGroup.check(R.id.radio_manual);
        switch(checked_button){
            case R.id.radio_auto:
                tv.setText(R.string.auto_blurb);
                break;
            case R.id.radio_manual:
                tv.setText(R.string.manual_blurb);
                break;
            default:
                break;
        }

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int checked_button = radioGroup.getCheckedRadioButtonId();
                Intent i = new Intent(getActivity(), WifiService.class);
                switch(checked_button){
                    case R.id.radio_auto:
                        if(!WifiService.isRunning)
                            getActivity().startService(i);
                        tv.setText(R.string.auto_blurb);
                        break;
                    case R.id.radio_manual:
                        if(WifiService.isRunning)
                            getActivity().stopService(i);
                        Log.d(TAG, "Service stopped");
                        tv.setText(R.string.manual_blurb);
                        break;
                    default:
                        break;
                }
            }
        });

        final LinearLayout myNetworksCntr = (LinearLayout) v.findViewById(R.id.mynetworksCntr);
        final LinearLayout localNetworkCntr = (LinearLayout) v.findViewById(R.id.localNetworksCntr);

        final ListView myNetworkListView = (ListView) v.findViewById(R.id.myNetworksList);
        final ListView localNetworkListView = (ListView) v.findViewById(R.id.newNetworksList);

        myNetworkListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                NetworkOptionsFragment dialog = NetworkOptionsFragment
                        .newInstance(((WifiConfiguration)parent.getItemAtPosition(position)).SSID);
                dialog.show(fm,"NetOptions");
            }
        });

        localNetworkListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                NewNetworkFragment dialog =NewNetworkFragment
                        .newInstance(((WifiConfiguration)parent.getItemAtPosition(position)).SSID);
                dialog.show(fm,"NEW_NETWORK");
            }
        });


        if(scanRetrieved) {
            NetworkAdapter scanAdapter = new NetworkAdapter(mScanResults);
            localNetworkListView.setAdapter(scanAdapter);
        }


        myNetworkListView.setAdapter(new NetworkAdapter(mNetworks));


        Button mgmt_button = (Button) v.findViewById(R.id.manage_btn);
        mgmt_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myNetworksCntr.getVisibility() == View.GONE) {
                    myNetworksCntr.setVisibility(View.VISIBLE);
                    localNetworkCntr.setVisibility(View.GONE);
                } else {
                   myNetworksCntr.setVisibility(View.GONE);
                }
            }
        });

        Button newNetwork_button = (Button) v.findViewById(R.id.newNetwork_button);
        final ProgressBar spinner = (ProgressBar) v.findViewById(R.id.progressBar1);

        newNetwork_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"New Btn Clicked");
                if(localNetworkCntr.getVisibility() == View.GONE) {
                    while(!scanRetrieved) {
                        spinner.setVisibility(View.VISIBLE);
                    }
                    if(scanRetrieved) {
                        NetworkAdapter scanAdapter = new NetworkAdapter(mScanResults);
                        localNetworkListView.setAdapter(scanAdapter);
                    }
                    localNetworkCntr.setVisibility(View.VISIBLE);
                    myNetworksCntr.setVisibility(View.GONE);


                } else {
                    localNetworkCntr.setVisibility(View.GONE);
                }
            }
        });



        return v;
    }

    private class NetworkAdapter extends ArrayAdapter<WifiConfiguration> {

        public NetworkAdapter(ArrayList<WifiConfiguration> networks) {
            super(getActivity(), 0,networks);
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup Parent) {
            if(convertView == null) {
                convertView = getActivity().getLayoutInflater()
                        .inflate(R.layout.network_list_item,null);
            }

            WifiConfiguration wc = getItem(pos);

            TextView networkName =
                    (TextView)convertView.findViewById(R.id.network_name_view);
            networkName.setText(wc.SSID.replace("\"",""));
            return convertView;
        }
    }
*/



