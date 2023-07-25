package org.studio.checker.processor;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import studio.core.v1.model.asset.MediaAsset;
import studio.core.v1.model.asset.MediaAssetType;
import studio.core.v1.utils.image.ImageConversion;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.function.Function;

@RequiredArgsConstructor
@Slf4j
public class ImageProcessor implements Function<MediaAsset, byte[]> {

    private static final int WIDTH = 320;
    private static final int HEIGHT = 240;

    private static ImageProcessor singleton;

    private final boolean compress;

    public static ImageProcessor getProcessor(boolean compress) {
        if (singleton == null) {
            singleton = new ImageProcessor(compress);
        }
        return singleton;
    }

    @SneakyThrows
    @Override
    public byte[] apply(MediaAsset mediaAsset) {
        byte[] rawData = mediaAsset.getRawData();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(rawData)) {
            BufferedImage img = ImageIO.read(bais);

            log.debug("Checking image dimensions");
            int width = img.getWidth();
            int height = img.getHeight();
            if (width != WIDTH || height != HEIGHT) {
                throw new IllegalArgumentException("Wrong image dimensions : " + width + "x" + height + " instead of " + WIDTH + "x" + HEIGHT);
            }

            if (compress) {
                // Convert to 4-bits depth / RLE encoding BMP
                if (MediaAssetType.BMP != mediaAsset.getType() || rawData[28] != 0x04 || rawData[30] != 0x02) {
                    log.debug("Converting image asset into 4-bits/RLE BMP");
                    rawData = ImageConversion.anyToRLECompressedBitmap(rawData);
                }
            }
        }
        return rawData;
    }

}
