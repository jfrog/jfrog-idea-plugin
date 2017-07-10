package org.jfrog.idea.ui.utils;

import com.intellij.openapi.util.IconLoader;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Created by romang on 4/12/17.
 */
public class IconUtils {

    private static Icon defaultIcon = IconLoader.findIcon("/icons/default.png");

    public static Icon load(String severity) {
        try {
            InputStream stream = IconUtils.class.getResourceAsStream("/icons/" + severity.toLowerCase() + ".svg");
            BufferedImage image = loadImage(stream, 10, 10);
            return new ImageIcon(image);
        } catch (Exception e) {
            return defaultIcon;
        }
    }

    private static BufferedImage loadImage(InputStream svgFile, float width, float height) throws TranscoderException {
        BufferedImageTranscoder imageTranscoder = new BufferedImageTranscoder();

        imageTranscoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, width);
        imageTranscoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, height);

        TranscoderInput input = new TranscoderInput(svgFile);
        imageTranscoder.transcode(input, null);

        return imageTranscoder.getBufferedImage();
    }

    private static class BufferedImageTranscoder extends ImageTranscoder {
        @Override
        public BufferedImage createImage(int w, int h) {
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            return bi;
        }

        @Override
        public void writeImage(BufferedImage img, TranscoderOutput output) {
            this.img = img;
        }

        public BufferedImage getBufferedImage() {
            return img;
        }

        private BufferedImage img = null;
    }
}
