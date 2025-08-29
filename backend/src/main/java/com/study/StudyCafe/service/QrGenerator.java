package com.study.StudyCafe.service;

import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Component
public class QrGenerator {

    public String generateQr(String data, String fileName) throws WriterException, IOException {
        int width = 300;
        int height = 300;

        String folderPath = "src/main/resources/static/qr";
        File dir = new File(folderPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String filePath = folderPath + "/" + fileName + ".png";
        Path path = Paths.get(filePath);

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, new Random().nextInt(3) + 1);  // 1~3

        BitMatrix matrix = new MultiFormatWriter().encode(
                data,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
        );

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        Random rand = new Random();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (matrix.get(x, y)) {
                    g.setColor(Color.BLACK);
                    int shape = rand.nextInt(3);
                    switch (shape) {
                        case 0 -> g.fillRect(x, y, 1, 1);
                        case 1 -> g.fillOval(x, y, 1, 1);
                        case 2 -> g.drawLine(x, y, x, y);
                    }
                }
            }
        }
        g.dispose();
        ImageIO.write(image, "PNG", path.toFile());

        return "/qr/" + fileName + ".png";
    }
}

