package ecommerce.product;

import ecommerce.common.BadRequestException;
import ecommerce.common.NotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * تخزين صور المنتجات على القرص بأمان:
 * - نتحقق من نوع الملف من بايتاته الأولى (لا نثق باسم الملف ولا بـ Content-Type القادم من العميل)
 * - الاسم على القرص عشوائي (UUID) فلا path traversal ولا تعارض أسماء
 * - الحجم الأقصى يحدده spring.servlet.multipart.max-file-size
 */
@Service
public class ImageStorageService {

    public record StoredImage(String fileName, String contentType) {}

    private static final Map<String, String> EXTENSIONS =
            Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");

    private final Path dir;

    public ImageStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.dir = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create upload directory " + dir, e);
        }
    }

    public StoredImage store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is empty");
        }
        try {
            byte[] head;
            try (InputStream in = file.getInputStream()) {
                head = in.readNBytes(12);
            }
            String type = detect(head);
            if (type == null) {
                throw new BadRequestException("Only JPEG, PNG or WEBP images are allowed");
            }
            String name = UUID.randomUUID() + "." + EXTENSIONS.get(type);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(name));
            }
            return new StoredImage(name, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Resource load(String fileName) {
        Path p = dir.resolve(fileName).normalize();
        if (!p.startsWith(dir) || !Files.isRegularFile(p)) {
            throw new NotFoundException("Image not found");
        }
        return new FileSystemResource(p);
    }

    public void delete(String fileName) {
        if (fileName == null) return;
        try {
            Path p = dir.resolve(fileName).normalize();
            if (p.startsWith(dir)) Files.deleteIfExists(p);
        } catch (IOException ignored) {
            // حذف الملف ليس حرجاً
        }
    }

    private static String detect(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "image/png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}
