package com.github.alexgopen.javaish.provider;

import java.awt.AWTException;
import java.awt.image.BufferedImage;
import java.io.IOException;

import com.github.alexgopen.javaish.exception.CoordNotFoundException;
import com.github.alexgopen.javaish.model.PixelCoord;
import com.github.alexgopen.javaish.model.WorldCoord;
import com.github.alexgopen.javaish.provider.internal.coords.CoordCropFinder;

public class CoordProvider {
    
    private final CoordCropFinder coordCropFinder;
    
    public CoordProvider() throws AWTException
    {
        this.coordCropFinder = new CoordCropFinder();
    }
    
    public WorldCoord getCoord() throws AWTException, IOException {
        BufferedImage coordCrop = coordCropFinder.getCoordCrop();

        if (coordCrop == null)
        {
            throw new CoordNotFoundException("CoordCropFinder failed to get coordCrop. coordCrop=null");
        }
        
        WorldCoord wc = coordCropFinder.extractWorldCoordFromCrop(coordCrop);

        if (wc == null) {
            throw new CoordNotFoundException("CoordExtractor failed to get point. p=null");
        }
        
        return wc;
    }
    
    public void resetPrevFoundCoordLoc()
    {
        coordCropFinder.resetPrevFoundCoordLoc();
    }
    
    public boolean onCooldown()
    {
        return coordCropFinder.onCooldown();
    }
}
