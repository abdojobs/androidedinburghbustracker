/*
 * Copyright (C) 2011 - 2013 Niall 'Rivernile' Scott
 *
 * This software is provided 'as-is', without any express or implied
 * warranty.  In no event will the authors or contributors be held liable for
 * any damages arising from the use of this software.
 *
 * The aforementioned copyright holder(s) hereby grant you a
 * non-transferrable right to use this software for any purpose (including
 * commercial applications), and to modify it and redistribute it, subject to
 * the following conditions:
 *
 *  1. This notice may not be removed or altered from any file it appears in.
 *
 *  2. Any modifications made to this software, except those defined in
 *     clause 3 of this agreement, must be released under this license, and
 *     the source code of any modifications must be made available on a
 *     publically accessible (and locateable) website, or sent to the
 *     original author of this software.
 *
 *  3. Software modifications that do not alter the functionality of the
 *     software but are simply adaptations to a specific environment are
 *     exempt from clause 2.
 */

package uk.org.rivernile.edinburghbustracker.android.fragments.general;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import uk.org.rivernile.edinburghbustracker.android.BusStopDatabase;
import uk.org.rivernile.edinburghbustracker.android.R;
import uk.org.rivernile.edinburghbustracker.android.alerts.AlertManager;
import uk.org.rivernile.edinburghbustracker.android.fragments.dialogs
        .ServicesChooserDialogFragment;
import uk.org.rivernile.edinburghbustracker.android.fragments.dialogs
        .TimeLimitationsDialogFragment;

/**
 * This fragment allows the user to add a new time alert. This alerts the user
 * when a service that they have selected is within a certain time of a chosen
 * bus stop.
 * 
 * @author Niall Scott
 */
public class AddTimeAlertFragment extends Fragment
        implements ServicesChooserDialogFragment.EventListener {
    
    /** The argument for the stopCode. */
    public static final String ARG_STOPCODE = "stopCode";
    /** The argument for the default service. */
    public static final String ARG_DEFAULT_SERVICE =
            ServicesChooserDialogFragment.ARG_DEFAULT_SERVICE;
    
    private static final String LIMITATIONS_DIALOG_TAG =
            "timeLimitationsDialog";
    private static final String SERVICES_CHOOSER_DIALOG_TAG =
            "servicesChooserDialogTag";
    
    private BusStopDatabase bsd;
    private AlertManager alertMan;
    private AlertFragmentEvent callback;
    private String stopCode;
    private int timeTrigger = 0;
    private ServicesChooserDialogFragment servicesChooser;
    
    private Button btnOkay;
    private TextView txtServices, txtTimeDialogStop;
    
    /**
     * Create a new instance of the AddTimeAlertFragment.
     * 
     * @param stopCode The stopCode this alert setting should be for.
     * @return A new instance of this Fragment.
     */
    public static AddTimeAlertFragment newInstance(final String stopCode) {
        final AddTimeAlertFragment f = new AddTimeAlertFragment();
        final Bundle b = new Bundle();
        b.putString(ARG_STOPCODE, stopCode);
        f.setArguments(b);
        
        return f;
    }
    
    /**
     * Create a new instance of the AddTimeAlertFragment.
     * 
     * @param stopCode The stopCode this alert setting should be for.
     * @return A new instance of this Fragment.
     */
    public static AddTimeAlertFragment newInstance(final String stopCode,
            final String defaultService) {
        final AddTimeAlertFragment f = new AddTimeAlertFragment();
        final Bundle b = new Bundle();
        b.putString(ARG_STOPCODE, stopCode);
        b.putString(ARG_DEFAULT_SERVICE, defaultService);
        f.setArguments(b);
        
        return f;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Tell the underlying Activity that the instance must be retained
        // across configuration changes.
        setRetainInstance(true);
        
        // Cast the hosting Activity to our callback interface. If this fails,
        // throw an IllegalStateException.
        try {
            callback = (AlertFragmentEvent)getActivity();
        } catch(ClassCastException e) {
            throw new IllegalStateException("The underlying Activity must " +
                    "implement AlertFragmentEvent.");
        }
        
        // Get the various resources.
        final Context context = getActivity().getApplicationContext();
        bsd = BusStopDatabase.getInstance(context);
        alertMan = AlertManager.getInstance(context);
        
        final Bundle args = getArguments();
        // Get the stop code from the arguments.
        stopCode = args.getString(ARG_STOPCODE);

        // Make sure a stopcode exists.
        if(stopCode == null || stopCode.length() == 0)
            throw new IllegalArgumentException("A stop code must be " +
                    "supplied.");
        
        if(args.containsKey(
                ServicesChooserDialogFragment.ARG_DEFAULT_SERVICE)) {
            servicesChooser = ServicesChooserDialogFragment.newInstance(
                    bsd.getBusServicesForStop(stopCode),
                    getString(R.string.addtimealert_services_title),
                    args.getString(ARG_DEFAULT_SERVICE), this);
        } else {
            servicesChooser = ServicesChooserDialogFragment.newInstance(
                    bsd.getBusServicesForStop(stopCode),
                    getString(R.string.addtimealert_services_title), this);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(final LayoutInflater inflater,
            final ViewGroup container, final Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.addtimealert, container,
                false);
        
        btnOkay = (Button)v.findViewById(R.id.btnOkay);
        txtServices = (TextView)v.findViewById(R.id.txtTimeAlertServices);
        txtTimeDialogStop = (TextView)v.findViewById(R.id.txtTimeDialogStop);
        
        btnOkay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                // Add the alert.
                alertMan.addTimeAlert(stopCode,
                        servicesChooser.getChosenServices(), timeTrigger);
                // Tell the underlying Activity that a new alert has been added.
                callback.onAlertAdded();
            }
        });
        
        // Set up the spinner.
        final Spinner spinner = (Spinner)v.findViewById(R.id.time_time_select);
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter
                .createFromResource(getActivity(),
                    R.array.addtimealert_array,
                    android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent,
                    final View view, final int pos, final long id) {
                switch(pos) {
                    case 0:
                        timeTrigger = 1;
                        break;
                    case 1:
                        timeTrigger = 2;
                        break;
                    case 2:
                        timeTrigger = 5;
                        break;
                    case 3:
                        timeTrigger = 10;
                        break;
                    case 4:
                        timeTrigger = 15;
                        break;
                    case 5:
                        timeTrigger = 20;
                        break;
                    case 6:
                        timeTrigger = 25;
                        break;
                    case 7:
                        timeTrigger = 30;
                        break;
                    default:
                        timeTrigger = 0;
                        break;
                }
            }

            @Override
            public void onNothingSelected(final AdapterView parent) {
                timeTrigger = 0;
            }
        });
        
        Button btn = (Button)v.findViewById(R.id.btnCancel);
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                // Tell the underlying Activity that the user has cancelled.
                callback.onCancel();
            }
        });
        
        btn = (Button)v.findViewById(R.id.btnAlertTimeServices);
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                // Show the services chooser DialogFragment.
                servicesChooser.show(getFragmentManager(),
                        SERVICES_CHOOSER_DIALOG_TAG);
            }
        });
        
        btn = (Button)v.findViewById(R.id.btnLimitations);
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                // Show the DialogFragment.
                new TimeLimitationsDialogFragment()
                        .show(getFragmentManager(), LIMITATIONS_DIALOG_TAG);
            }
        });
        
        // Set a piece of informative text with the stopCode, stopName and
        // locality (if available).
        final String locality = bsd.getLocalityForStopCode(stopCode);
        String stopNameCode;
        if(locality == null) {
            // Format the string for when we do not have locality.
            stopNameCode = bsd.getNameForBusStop(stopCode) + " (" + stopCode +
                    ")";
        } else {
            // Format the string for when we do have locality.
            stopNameCode = bsd.getNameForBusStop(stopCode) + ", " + locality +
                    " (" + stopCode + ")";
        }
        
        txtTimeDialogStop.setText(getString(R.string.addtimealert_busstop,
                stopNameCode));
        
        // Force a refresh of the TextView that shows the services that have
        // been chosen.
        onServicesChosen();
        
        return v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServicesChosen() {
        // Get the services String.
        final String services = servicesChooser.getChosenServicesAsString();
        
        if(services.length() == 0) {
            // If the services list is empty, put the default text in the view
            // and disable the okay button.
            txtServices.setText(getString(R.string.addtimealert_noservices));
            btnOkay.setEnabled(false);
        } else {
            // If the services list is not empty, put the services list in the
            // view and enable the okay button.
            txtServices.setText(
                    BusStopDatabase.getColouredServiceListString(services));
            btnOkay.setEnabled(true);
        }
    }
}