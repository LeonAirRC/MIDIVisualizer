import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * uses JCodec to render a video from BufferedImages
 */
public final class VideoRenderer {
    /** true if finished was already called to disallow new frames after finish was called */
    private boolean finished = false;
    /** the encoder */
    private SequenceEncoder encoder;

    /**
     * create a video renderer
     *
     * @param outputFile the output file
     * @param fps        frames per second
     * @throws IOException when {@link SequenceEncoder#createSequenceEncoder(File, int)} fails
     */
    public VideoRenderer(File outputFile, int fps) throws IOException {
        encoder = SequenceEncoder.createSequenceEncoder(outputFile, fps);
    }

    /**
     * add frame to the video
     *
     * @param img the image
     * @throws IOException when {@link SequenceEncoder#encodeNativeFrame(Picture)} fails
     */
    public synchronized void addFrame(BufferedImage img) throws IOException {
        if (!finished)
            encoder.encodeNativeFrame(AWTUtil.fromBufferedImageRGB(img));
    }

    /**
     * finish rendering
     */
    public synchronized void finish() {
        if (finished)
            return;
        try {
            encoder.finish();
        } catch (IOException ignored) {
        }
        encoder = null;
        finished = true;
    }
}
