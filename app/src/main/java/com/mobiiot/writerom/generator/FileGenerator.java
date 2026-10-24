package com.mobiiot.writerom.generator;

import androidx.annotation.NonNull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class FileGenerator {

    public interface ProgressCallback {
        boolean shouldCancel();
        boolean isPaused();
        void onBytesWritten(long deltaBytes);
    }

    private static final int BUFFER_SIZE = 2 * 1024 * 1024; // 2MB 缓冲区

    @NonNull
    public static File createDataFile(@NonNull File parentDir,
                                      long targetFileBytes,
                                      boolean isRandomData,
                                      int fileIndex,
                                      @NonNull ProgressCallback callback) throws IOException {
        String fileName = String.format("filler_data_%04d_%d.bin", fileIndex, System.currentTimeMillis());
        File file = new File(parentDir, fileName);

        byte[] buffer = new byte[BUFFER_SIZE];
        Random random = isRandomData ? new Random() : null;
        if (!isRandomData) {
            // 零字节填充
            java.util.Arrays.fill(buffer, (byte) 0);
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            long remaining = targetFileBytes;

            while (remaining > 0) {
                if (callback.shouldCancel()) {
                    break;
                }

                while (callback.isPaused() && !callback.shouldCancel()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                int toWrite = (int) Math.min(remaining, buffer.length);
                if (isRandomData && random != null) {
                    random.nextBytes(buffer);
                }

                fos.write(buffer, 0, toWrite);
                remaining -= toWrite;
                callback.onBytesWritten(toWrite);
            }
            fos.flush();
        }

        return file;
    }
}
