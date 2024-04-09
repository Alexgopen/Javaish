package tess;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.ImageHelper;
import net.sourceforge.tess4j.util.LoadLibs;

public class TessUtil {
    public static String parseImage(BufferedImage img) {
        String ret = "error";
        try {

            File tmpFolder = LoadLibs.extractTessResources("win32-x86-64");
            for (File f : tmpFolder.listFiles()) {
                System.load(f.getAbsolutePath());
            }

            Tesseract tess = new Tesseract();
            tess.setLanguage("eng");
            tess.setOcrEngineMode(1);
            tess.setPageSegMode(7);
            Path dataDirectory = Paths.get(TessUtil.class.getResource("data").toURI());
            tess.setDatapath(dataDirectory.toString());

            tess.setVariable("tessedit_char_whitelist", "123456789,. ");
            ret = tess.doOCR(ImageHelper.convertImageToGrayscale(img));
        }
        catch (TesseractException | URISyntaxException te) {
            te.printStackTrace();
        }
        return ret;
    }
}
