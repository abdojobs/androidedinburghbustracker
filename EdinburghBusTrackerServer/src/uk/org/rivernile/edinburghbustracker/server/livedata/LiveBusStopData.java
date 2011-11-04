/*
 * Copyright (C) 2009 Niall 'Rivernile' Scott
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

package uk.org.rivernile.edinburghbustracker.server.livedata;

import java.io.Writer;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONWriter;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class holds the live bus stop data. It gets the values from the parsed
 * XML file and stores them in a structure which can be retrieved at a later
 * time.
 *
 * @author Niall Scott
 */
public class LiveBusStopData extends DefaultHandler {

    private String thisStopCode = "";
    private String thisStopName = "";
    private String route;
    private String str;
    private boolean span = false;
    private LiveBus bus;
    private BusService service;

    private ArrayList<BusService> busServices;

    /**
     * Create a new instance of LiveBusStopData.
     */
    public LiveBusStopData() {
        super();

        busServices = new ArrayList<BusService>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String localName,
            final String qname, final Attributes attributes) {
        if(localName.toLowerCase().equals("a")) {
            str = "";
        } else if(localName.toLowerCase().equals("pre")) {
            bus = new LiveBus();
            str = "";
        } else if(localName.toLowerCase().equals("span")) {
            if(bus != null) bus.setAccessible(true);
            span = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String qName)
    {
        if(localName.toLowerCase().equals("a")) {
            handleStopInformation(str);
        } else if(localName.toLowerCase().equals("pre")) {
            handleBusInformation(str);
        } else if(localName.toLowerCase().equals("span")) {
            span = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start, final int length) {
        if(span) return;
        StringBuffer sb = new StringBuffer();
        for(int i = start; i < start + length; i++) {
            if(ch[i] == ' ' && i > start) {
                if(ch[i-1] != ' ') {
                    sb.append(ch[i]);
                }
            } else {
                sb.append(ch[i]);
            }
        }

        str = str + sb.toString();
    }

    /**
     * Handle the information regarding a bus service's route.
     *
     * @param infoLine The string to parse.
     */
    private void handleStopInformation(final String infoLine) {
        char[] chars = infoLine.toCharArray();
        int stage = 0;

        service = new BusService();
        busServices.add(service);
        route = "";
        thisStopCode = "";
        thisStopName = "";
        for(int i = 0; i < chars.length; i++) {
            switch(stage) {
                case 0:
                    if(chars[i] == ' ') {
                        thisStopCode = thisStopCode.trim();
                        stage++;
                    } else {
                        thisStopCode = thisStopCode + chars[i];
                    }
                    break;
                case 1:
                    if(chars[i] == '/') {
                        thisStopCode = thisStopCode.trim();
                        stage++;
                    } else {
                        thisStopName = thisStopName + chars[i];
                    }
                    break;
                case 2:
                    route = route + chars[i];
                    break;
                default:
                    break;
            }
        }
        thisStopName = thisStopName.trim();
        route = route.trim();
        service.setRoute(route);
    }

    /**
     * Handle the information regarding when a bus is due at a stop and it's
     * destination.
     *
     * @param infoLine The string to parse.
     */
    private void handleBusInformation(final String infoLine) {
        String[] splitted = infoLine.split("\\s+");
        if(splitted.length < 3) return;
        service.setServiceName(splitted[0].trim());
        bus.setArrivalTime(splitted[splitted.length-1].trim());
        if(splitted.length == 3) {
            bus.setDestination(splitted[1].trim());
        } else {
            String dest = splitted[1];
            for(int i = 2; i < splitted.length-1; i++) {
                dest = dest + " " + splitted[i];
            }
            bus.setDestination(dest);
        }
        service.addLiveBus(bus);
    }

    /**
     * Get all of the bus stop information available in this object and output
     * it in JSON format to the supplied Writer stream.
     *
     * @param out The stream to write the JSON text to.
     */
    public void writeJSONToStream(final Writer out) {
        if(out == null) throw new IllegalArgumentException("The Writer object" +
                " cannot be null.");
        
        JSONWriter jw = new JSONWriter(out);
        try {
            jw.object().key("stopCode").value(thisStopCode).key("stopName")
                    .value(thisStopName).key("services").array();
            for(BusService s : busServices) {
                jw.object().key("serviceName").value(s.getServiceName())
                        .key("route").value(s.getRoute()).key("buses").array();
                for(LiveBus b : s.buses) {
                    jw.object().key("destination").value(b.getDestination())
                            .key("arrivalTime").value(b.getArrivalTime())
                            .key("accessible").value(b.getAccessible())
                            .endObject();
                }
                jw.endArray().endObject();
            }
            jw.endArray().endObject();
        } catch(JSONException e) {
            System.err.println("A JSON exception has occurred. The exception " +
                    "reported was:");
            System.err.println(e.toString());
        }
    }
}