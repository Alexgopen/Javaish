package com.github.alexgopen.javaish.utils;

import com.github.alexgopen.javaish.model.Point;
import com.github.alexgopen.javaish.model.TrackPoint;
import com.github.alexgopen.javaish.exception.CoordNotFoundException;

public class CoordUpdater implements Runnable {
    private final TrackManager trackManager;
    private final CoordProvider coordProvider;
    private final CoordConverter converter;

    private static final long TICK_RATE = 250;

    public CoordUpdater(TrackManager trackManager, CoordProvider coordProvider, CoordConverter converter) {
        this.trackManager = trackManager;
        this.coordProvider = coordProvider;
        this.converter = converter;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(TICK_RATE);
                Point coord = coordProvider.getCoord();
                Point mapCoord = converter.worldToMap(coord);
                int distTp = 0; // compute from last trackpoint if needed
                long deltaTimeTp = 0;
                
                TrackPoint last = null;
                if (trackManager.getTrackPoints().size() > 0)
                {
                    last = trackManager.getTrackPoints().get(trackManager.getTrackPoints().size()-1);
                }
                // Only track if change is visible on the map
                if (last != null && last.map != null && (last.map.x != mapCoord.x || last.map.y != mapCoord.y))
                {
                    trackManager.addTrackPoint(coord, mapCoord, distTp, deltaTimeTp);
                    System.out.println("Updated coord: "+coord.toString());
                }
            }
            catch (CoordNotFoundException e) {
                if (!CoordProvider.onCooldown())
                    System.err.println("Coord not found.");
                CoordProvider.resetPrevFoundCoordLoc();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
