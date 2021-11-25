import io.humble.video.*;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * renders a video from BufferedImages
 */
public final class VideoRenderer {
    /** true if finished was already called to disallow new frames after finish was called */
    private boolean finished = false;
    private final Muxer muxer;
    private final Encoder encoder;
    private MediaPictureConverter converter;
    private final MediaPicture picture;
    private final MediaPacket packet;
    private int frame = 0;

    /**
     * create a video renderer
     *
     * @param outputFile the output file
     * @param fps        frames per second
     * @throws IOException          when {@link Muxer#open(KeyValueBag, KeyValueBag)} fails
     * @throws InterruptedException when {@link Muxer#open(KeyValueBag, KeyValueBag)} fails
     */
    public VideoRenderer(String outputFile, String formatName, String codecName, int fps, int width, int height) throws InterruptedException, IOException {
        muxer = Muxer.make(outputFile, null, formatName);
        Codec codec = (codecName == null) ? Codec.findEncodingCodec(muxer.getFormat().getDefaultVideoCodecId()) : Codec.findEncodingCodecByName(codecName);
        encoder = Encoder.make(codec);
        encoder.setWidth(width);
        encoder.setHeight(height);
        PixelFormat.Type pixelFormat = PixelFormat.Type.PIX_FMT_YUV420P;
        encoder.setPixelFormat(pixelFormat);
        Rational timeBase = Rational.make(1, fps);
        encoder.setTimeBase(timeBase);

        if (muxer.getFormat().getFlag(MuxerFormat.Flag.GLOBAL_HEADER))
            encoder.setFlag(Encoder.Flag.FLAG_GLOBAL_HEADER, true);
        encoder.open(null, null);
        muxer.addNewStream(encoder);
        muxer.open(null, null);

        picture = MediaPicture.make(width, height, pixelFormat);
        picture.setTimeBase(timeBase);
        packet = MediaPacket.make();
    }

    /**
     * add frame to the video
     *
     * @param img the image
     */
    public synchronized void addFrame(BufferedImage img) {
        if (converter == null)
            converter = MediaPictureConverterFactory.createConverter(img, picture);
        converter.toPicture(picture, img, frame);
        do {
            encoder.encode(packet, picture);
            if (packet.isComplete())
                muxer.write(packet, false);
        } while (packet.isComplete());
        frame++;
    }

    /**
     * finish rendering
     */
    public synchronized void finish() {
        if (finished)
            return;
        do {
            encoder.encode(packet, null);
            if (packet.isComplete())
                muxer.write(packet, false);
        } while (packet.isComplete());
        muxer.close();
        finished = true;
    }
}
