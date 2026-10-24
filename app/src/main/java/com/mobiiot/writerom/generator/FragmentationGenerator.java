package com.mobiiot.writerom.generator;

import androidx.annotation.NonNull;
import com.mobiiot.writerom.model.FragmentSizeLevel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class FragmentationGenerator {

    private static final int FILES_PER_BUCKET = 500;
    private static final byte[] ZERO_CHUNK = new byte[64 * 1024];

    @NonNull
    public static File createFragmentFile(@NonNull File rootDir,
                                          int globalIndex,
                                          int workerId,
                                          @NonNull FragmentSizeLevel level,
                                          boolean isRandomData,
                                          boolean punchHoles,
                                          @NonNull FileGenerator.ProgressCallback callback) throws IOException {
        int bucketIndex = globalIndex / FILES_PER_BUCKET;
        File bucketDir = new File(rootDir, String.format("frag_bucket_%03d", bucketIndex));
        if (!bucketDir.exists()) {
            bucketDir.mkdirs();
        }

        Random rnd = new Random();
        int minSize = level.getMinBytes();
        int maxSize = level.getMaxBytes();
        int targetSize = minSize + rnd.nextInt(Math.max(1, maxSize - minSize + 1));

        String fileName = String.format("frag_w%d_%06d_%d.bin", workerId, globalIndex, System.currentTimeMillis() % 100000);
        File file = new File(bucketDir, fileName);

        byte[] buffer = new byte[Math.min(targetSize, ZERO_CHUNK.length)];
        if (isRandomData) {
            rnd.nextBytes(buffer);
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            int remaining = targetSize;
            while (remaining > 0) {
                if (callback.shouldCancel()) break;

                while (callback.isPaused() && !callback.shouldCancel()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                int toWrite = Math.min(remaining, buffer.length);
                if (isRandomData) {
                    rnd.nextBytes(buffer);
                }
                fos.write(buffer, 0, toWrite);
                remaining -= toWrite;
                callback.onBytesWritten(toWrite);
            }
            fos.flush();
        }

        // 穿插打孔模式：当一个桶达到500个文件时，随机删除其中30%的文件制造空洞碎片
        if (punchHoles && (globalIndex % FILES_PER_BUCKET == FILES_PER_BUCKET - 1)) {
            punchHolesInBucket(bucketDir);
        }

        return file;
    }

    private static void punchHolesInBucket(File bucketDir) {
        File[] files = bucketDir.listFiles();
        if (files == null || files.length < 50) return;

        List<File> list = new ArrayList<>();
        for (File f : files) {
            if (f.isFile() && f.getName().startsWith("frag_")) {
                list.add(f);
            }
        }

        Collections.shuffle(list);
        int deleteCount = (int) (list.size() * 0.3); // 删除 30%
        for (int i = 0; i < deleteCount; i++) {
            list.get(i).delete();
        }
    }
}
