package com.inspectorsgadget.app;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PdfCompressorTest {
    @Test
    public void shrinksSkiaPdfImages() throws Exception {
        File in = new File(System.getProperty("pdfIn"));
        byte[] src = Files.readAllBytes(in.toPath());
        String[] names = {"email", "full"};
        int[][] cfg = {{ReportGenerator.PHOTO_PX[0], ReportGenerator.JPEG_Q[0]}, {ReportGenerator.PHOTO_PX[1], ReportGenerator.JPEG_Q[1]}};
        for (int i = 0; i < 2; i++) {
            PdfCompressor.Result r = PdfCompressor.compress(src, cfg[i][0], cfg[i][1]);
            System.out.println(names[i] + ": " + src.length + " -> " + r.data.length + " bytes, images " + r.imagesCompressed);
            assertTrue(r.imagesCompressed > 0);
            assertTrue(r.data.length < src.length);
            Files.write(new File(in.getParentFile(), "out_" + names[i] + ".pdf").toPath(), r.data);
        }
    }
}
