package v3.utils;

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;

import v3.exception.CoordNotFoundException;
import v3.exception.WindowNotFoundException;
import v3.model.Compass;
import v3.model.Digit;
import v3.model.Point;

public class WindowCapture {

	private static Rectangle prevFoundCoords = null;
	private static long lostCoordsTimestamp = 0;

	public static void resetPrevFoundCoords()
	{
		WindowCapture.prevFoundCoords = null;
		
		if (WindowCapture.lostCoordsTimestamp == 0)
		{
			WindowCapture.lostCoordsTimestamp = System.currentTimeMillis();
		}
	}
	
	private static boolean shouldSearchCoords()
	{
		boolean shouldSearch = WindowCapture.lostCoordsTimestamp == 0 || (System.currentTimeMillis() - WindowCapture.lostCoordsTimestamp) >= 5000;
		
		return shouldSearch;
	}
	
	public static BufferedImage getCoordCrop() throws AWTException, IOException {
		BufferedImage coordCrop = null;

		BufferedImage ss = null;
		//ImageIO.write(ss, "png", new File("uwoss.png"));

		Rectangle found = null;
		if (prevFoundCoords == null && WindowCapture.shouldSearchCoords()) {
			ss = WindowCapture.getAllMonitorScreenshot();
			
			WindowCapture.lostCoordsTimestamp = 0;
			// Try to locate the coordinate display by scanning the lower-right quadrant			
			found = Compass.findCoordCropFromCompass(ss);

			if (found != null)
			{
				WindowCapture.prevFoundCoords = found;
				
				// System.out.printf("Found coord crop at (%d,%d)\n", found.x, found.y);
			}
			else
			{
				throw new CoordNotFoundException();
			}
		} else {
			found = WindowCapture.prevFoundCoords;
			// System.out.printf("Using previously found coord crop at (%d,%d)\n", found.x, found.y);
		}

		if (found != null) {
			try {
				if (ss != null)
				{
					coordCrop = cropImage(ss, found);
				}
				else
				{
					coordCrop = WindowCapture.getScreenshotOfRectangle(found);
				}

				// optionally write debug
				// ImageIO.write(coordCrop, "png", new File("found_coord_crop.png"));
				// Also attempt parsing to show result
				try {
					Point p = CoordExtractor.getPoint(coordCrop, true);
					//System.out.printf("Parsed coords: %s\n", p);
				} catch (Exception e) {
					WindowCapture.resetPrevFoundCoords();
					//System.err.println("Parsing failed on found crop (unexpected): " + e.getMessage());
				}
			} catch (Exception e) {
				WindowCapture.resetPrevFoundCoords();
				e.printStackTrace();
			}
		}
		else
		{
			throw new CoordNotFoundException();
		}

		return coordCrop;
	}

	public static BufferedImage getScreenshotOfRectangle(Rectangle bounds) throws AWTException
	{
        Robot robot = new Robot();
        BufferedImage screenshot = robot.createScreenCapture(bounds);
        
        return screenshot;
	}
	
	public static BufferedImage getAllMonitorScreenshot() throws AWTException
	{
		// Get the union of all monitor bounds
        Rectangle allBounds = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        for (GraphicsDevice gd : ge.getScreenDevices()) {
            Rectangle bounds = gd.getDefaultConfiguration().getBounds();
            allBounds = allBounds.union(bounds);
        }
        
        return getScreenshotOfRectangle(allBounds);
	}

	private static BufferedImage cropImage(BufferedImage src, Rectangle rect) {
		BufferedImage dest = src.getSubimage(rect.x, rect.y, rect.width, rect.height);
		return dest;
	}
}