package com.mobiiot.writerom.generator;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import androidx.annotation.NonNull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class ImageGenerator {

    private static final int DEFAULT_WIDTH = 2560;
    private static final int DEFAULT_HEIGHT = 1440;

    @NonNull
    public static File createImageFile(@NonNull File parentDir,
                                       int fileIndex,
                                       @NonNull FileGenerator.ProgressCallback callback) throws IOException {
        String fileName = String.format("filler_img_%04d_%d.jpg", fileIndex, System.currentTimeMillis());
        File file = new File(parentDir, fileName);

        if (callback.shouldCancel()) {
            return file;
        }

        Bitmap bitmap = Bitmap.createBitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Random rnd = new Random();
        int color1 = Color.rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        int color2 = Color.rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));

        Paint bgPaint = new Paint();
        bgPaint.setShader(new LinearGradient(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT, color1, color2, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT, bgPaint);

        // 绘制随机几何图形增加数据丰富度
        Paint shapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (int i = 0; i < 20; i++) {
            shapePaint.setColor(Color.argb(120, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)));
            canvas.drawCircle(rnd.nextInt(DEFAULT_WIDTH), rnd.nextInt(DEFAULT_HEIGHT), 50 + rnd.nextInt(200), shapePaint);
        }

        // 绘制文本水印标识与时间戳
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(64);
        textPaint.setFakeBoldText(true);
        textPaint.setShadowLayer(8f, 2f, 2f, Color.BLACK);

        String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
        String info = String.format(Locale.getDefault(), "ROM Filler Image #%04d | %s", fileIndex, timeStr);
        canvas.drawText(info, 80, 160, textPaint);

        long startLen = file.length();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos);
            fos.flush();
        } finally {
            bitmap.recycle();
        }

        long written = file.length() - startLen;
        callback.onBytesWritten(written);

        return file;
    }
}
