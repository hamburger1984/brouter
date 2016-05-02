package btools.routingapp;


import android.os.Bundle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import btools.router.OsmNodeNamed;
import btools.router.OsmTrack;
import btools.router.RoutingContext;
import btools.router.RoutingEngine;

public class BRouterWorker {
    public String segmentDir;
    public String profilePath;
    public String rawTrackPath;
    public List<OsmNodeNamed> nogoList;

    public String getTrackFromParams(Bundle params) {
        String pathToFileResult = params.getString("pathToFileResult");

        if (pathToFileResult != null) {
            File f = new File(pathToFileResult);
            File dir = f.getParentFile();
            if (!dir.exists() || !dir.canWrite()) {
                return "file folder does not exists or can not be written!";
            }
        }

        long maxRunningTime = 60000;
        String sMaxRunningTime = params.getString("maxRunningTime");
        if (sMaxRunningTime != null) {
            maxRunningTime = Integer.parseInt(sMaxRunningTime) * 1000;
        }

        RoutingContext rc = new RoutingContext();
        rc.rawTrackPath = rawTrackPath;
        rc.localFunction = profilePath;
        if (nogoList != null) {
            rc.prepareNogoPoints(nogoList);
            rc.nogopoints = nogoList;
        }

        readNogos(params); // add interface provides nogos

        RoutingEngine cr = new RoutingEngine(null, null, segmentDir, readPositions(params), rc);
        cr.quite = true;
        cr.doRun(maxRunningTime);
        if (cr.getErrorMessage() != null) {
            return cr.getErrorMessage();
        }

        // store new reference track if any
        if (cr.getFoundRawTrack() != null) {
            try {
                cr.getFoundRawTrack().writeBinary(rawTrackPath);
            } catch (Exception e) {
            }
        }


        String format = params.getString("trackFormat");
        boolean writeKml = format != null && "kml".equals(format);

        OsmTrack track = cr.getFoundTrack();
        if (track != null) {
            if (pathToFileResult == null) {
                if (writeKml) return track.formatAsKml();
                return track.formatAsGpx();
            }
            try {
                if (writeKml) track.writeKml(pathToFileResult);
                else track.writeGpx(pathToFileResult);
            } catch (Exception e) {
                return "error writing file: " + e;
            }
        }
        return null;
    }

    private List<OsmNodeNamed> readPositions(Bundle params) {
        List<OsmNodeNamed> wplist = new ArrayList<OsmNodeNamed>();

        double[] lats = params.getDoubleArray("lats");
        double[] lons = params.getDoubleArray("lons");

        if (lats == null || lats.length < 2 || lons == null || lons.length < 2) {
            throw new IllegalArgumentException("we need two lat/lon points at least!");
        }

        for (int i = 0; i < lats.length && i < lons.length; i++) {
            OsmNodeNamed n = new OsmNodeNamed();
            n.name = "via" + i;
            n.ilon = (int) ((lons[i] + 180.) * 1000000. + 0.5);
            n.ilat = (int) ((lats[i] + 90.) * 1000000. + 0.5);
            wplist.add(n);
        }
        wplist.get(0).name = "from";
        wplist.get(wplist.size() - 1).name = "to";

        return wplist;
    }

    private void readNogos(Bundle params) {
        double[] lats = params.getDoubleArray("nogoLats");
        double[] lons = params.getDoubleArray("nogoLons");
        double[] radi = params.getDoubleArray("nogoRadi");

        if (lats == null || lons == null || radi == null) return;

        for (int i = 0; i < lats.length && i < lons.length && i < radi.length; i++) {
            OsmNodeNamed n = new OsmNodeNamed();
            n.name = "nogo" + (int) radi[i];
            n.ilon = (int) ((lons[i] + 180.) * 1000000. + 0.5);
            n.ilat = (int) ((lats[i] + 90.) * 1000000. + 0.5);
            n.isNogo = true;
            nogoList.add(n);
        }
    }
}
