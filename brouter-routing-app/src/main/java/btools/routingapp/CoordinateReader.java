package btools.routingapp;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import btools.router.OsmNodeNamed;
import btools.router.RoutingHelper;

/**
 * Read coordinates from a gpx-file
 */
public abstract class CoordinateReader {
    protected static String[] posnames
            = new String[]{"from", "via1", "via2", "via3", "via4", "via5", "via6", "via7", "via8", "via9", "to"};
    public List<OsmNodeNamed> waypoints;
    public List<OsmNodeNamed> nogopoints;
    public String basedir;
    public String rootdir;
    public String tracksdir;
    public Map<String, OsmNodeNamed> allpoints;
    private HashMap<String, OsmNodeNamed> pointmap;

    public CoordinateReader(String basedir) {
        this.basedir = basedir;
    }

    public static CoordinateReader obtainValidReader(String basedir, String segmentDir) throws Exception {
        CoordinateReader cor = null;
        ArrayList<CoordinateReader> rl = new ArrayList<CoordinateReader>();

        AppLogger.log("adding standard maptool-base: " + basedir);
        rl.add(new CoordinateReaderOsmAnd(basedir));
        rl.add(new CoordinateReaderLocus(basedir));
        rl.add(new CoordinateReaderOrux(basedir));

        // eventually add standard-sd
        File standardbase = Environment.getExternalStorageDirectory();
        if (standardbase != null) {
            String base2 = standardbase.getAbsolutePath();
            if (!base2.equals(basedir)) {
                AppLogger.log("adding internal sd maptool-base: " + base2);
                rl.add(new CoordinateReaderOsmAnd(base2));
                rl.add(new CoordinateReaderLocus(base2));
                rl.add(new CoordinateReaderOrux(base2));
            }
        }

        // eventually add explicit directory
        File additional = RoutingHelper.getAdditionalMaptoolDir(segmentDir);
        if (additional != null) {
            String base3 = additional.getAbsolutePath();

            AppLogger.log("adding maptool-base from storage-config: " + base3);

            rl.add(new CoordinateReaderOsmAnd(base3));
            rl.add(new CoordinateReaderLocus(base3));
            rl.add(new CoordinateReaderOrux(base3));
        }

        long tmax = 0;
        for (CoordinateReader r : rl) {
            if (AppLogger.isLogging()) {
                AppLogger.log("reading timestamp at systime " + new Date());
            }

            long t = r.getTimeStamp();

            if (t != 0) {
                if (AppLogger.isLogging()) {
                    AppLogger.log("found coordinate source at " + r.basedir + r.rootdir + " with timestamp " + new Date(t));
                }
            }

            if (t > tmax) {
                tmax = t;
                cor = r;
            }
        }
        if (cor == null) {
            cor = new CoordinateReaderNone();
        }
        cor.readFromTo();
        return cor;
    }

    public abstract long getTimeStamp() throws Exception;

    /*
     * read the from, to and via-positions from a gpx-file
     */
    public void readFromTo() throws Exception {
        pointmap = new HashMap<String, OsmNodeNamed>();
        waypoints = new ArrayList<OsmNodeNamed>();
        nogopoints = new ArrayList<OsmNodeNamed>();
        readPointmap();
        boolean fromToMissing = false;
        for (int i = 0; i < posnames.length; i++) {
            String name = posnames[i];
            OsmNodeNamed n = pointmap.get(name);
            if (n != null) {
                waypoints.add(n);
            } else {
                if ("from".equals(name)) fromToMissing = true;
                if ("to".equals(name)) fromToMissing = true;
            }
        }
        if (fromToMissing) waypoints.clear();
    }

    protected void checkAddPoint(OsmNodeNamed n) {
        if (allpoints != null) {
            allpoints.put(n.name, n);
            return;
        }

        boolean isKnown = false;
        for (int i = 0; i < posnames.length; i++) {
            if (posnames[i].equals(n.name)) {
                isKnown = true;
                break;
            }
        }

        if (isKnown) {
            if (pointmap.put(n.name, n) != null) {
                throw new IllegalArgumentException("multiple " + n.name + "-positions!");
            }
        } else if (n.name != null && n.name.startsWith("nogo")) {
            n.isNogo = true;
            nogopoints.add(n);
        }

    }

    protected abstract void readPointmap() throws Exception;
}
