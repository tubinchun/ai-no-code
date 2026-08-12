package com.lzf.langchain4j;

import com.lzf.langchain4j.manager.MinioManager;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootTest
class MinioManagerTest {

    @Resource
    private MinioManager minioManager;

    @Test
    void uploadCompressedScreenshot() throws Exception {
        Path filePath = Path.of("tmp/screenshots/b06c7bdc/34701_compressed.jpg");
        Assertions.assertTrue(Files.exists(filePath));
        String objectName = "test/screenshots/b06c7bdc/34701_compressed_" + System.currentTimeMillis() + ".jpg";
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            String url = minioManager.uploadFile(inputStream, Files.size(filePath), "image/jpeg", objectName);
            Assertions.assertNotNull(url);
            Assertions.assertTrue(url.contains(objectName));
            System.out.println("MinIO upload url: " + url);
        }
    }
}
