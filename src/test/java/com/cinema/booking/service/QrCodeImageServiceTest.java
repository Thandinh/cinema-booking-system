package com.cinema.booking.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QrCodeImageServiceTest {

    QrCodeImageService qrCodeImageService = new QrCodeImageService();

    @Test
    void generatedPngCanBeDecoded() throws Exception {
        String content = "CBT1.0123456789ABCDEF0123456789ABCDEF.abcdefghijklmnopqrstu1.abcdefghijklmnopqrstuvwxy1234567";
        byte[] pngBytes = qrCodeImageService.toPngBytes(content, 360);

        var image = ImageIO.read(new ByteArrayInputStream(pngBytes));
        var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        var result = new MultiFormatReader().decode(bitmap);

        assertEquals(content, result.getText());
    }
}
