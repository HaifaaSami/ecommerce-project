package ecommerce.product;

import ecommerce.common.NotFoundException;
import ecommerce.product.ProductDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Duration;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products")
public class ProductController {

    private final ProductRepository products;
    private final ImageStorageService images;

    public ProductController(ProductRepository products, ImageStorageService images) {
        this.products = products;
        this.images = images;
    }

    @Operation(summary = "List products (public) - paginated, optional ?category= filter, ?page=&size=&sort=")
    @GetMapping
    public Page<ProductResponse> list(@RequestParam(required = false) String category, Pageable pageable) {
        Page<Product> page = (category == null || category.isBlank())
                ? products.findAll(pageable)
                : products.findByCategoryIgnoreCase(category.trim(), pageable);
        return page.map(ProductResponse::from);
    }

    @Operation(summary = "Get one product (public)")
    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return products.findById(id).map(ProductResponse::from)
                .orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
    }

    @Operation(summary = "Create product as JSON (ADMIN only). Upload the picture afterwards with PUT /{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest r) {
        Product saved = products.save(new Product(r.name(), r.description(), r.price(), r.category(), r.imageUrl()));
        return created(saved);
    }

    @Operation(summary = "Create product WITH its picture in one request (ADMIN only, multipart: part 'product' = JSON, part 'image' = file)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> createWithImage(
            @Valid @RequestPart("product") ProductRequest r,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        Product p = new Product(r.name(), r.description(), r.price(), r.category(), r.imageUrl());
        ImageStorageService.StoredImage stored = null;
        if (image != null && !image.isEmpty()) {
            stored = images.store(image);
            p.setImage(stored.fileName(), stored.contentType());
        }
        try {
            return created(products.save(p));
        } catch (RuntimeException e) {
            if (stored != null) images.delete(stored.fileName());
            throw e;
        }
    }

    @Operation(summary = "Upload / replace the product picture (ADMIN only, multipart: part 'image')")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductResponse uploadImage(@PathVariable Long id, @RequestPart("image") MultipartFile image) {
        Product p = products.findById(id).orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
        String oldFile = p.getImageFile();
        ImageStorageService.StoredImage stored = images.store(image);
        p.setImage(stored.fileName(), stored.contentType());
        try {
            Product saved = products.save(p);
            images.delete(oldFile);
            return ProductResponse.from(saved);
        } catch (RuntimeException e) {
            images.delete(stored.fileName());
            throw e;
        }
    }

    @Operation(summary = "Display the product picture (public)")
    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> image(@PathVariable Long id) {
        Product p = products.findById(id).orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
        if (p.getImageFile() == null) throw new NotFoundException("Product has no uploaded image");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(p.getImageContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(images.load(p.getImageFile()));
    }

    @Operation(summary = "Update product (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest r) {
        Product p = products.findById(id).orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
        p.update(r.name(), r.description(), r.price(), r.category(), r.imageUrl());
        return ProductResponse.from(products.save(p));
    }

    @Operation(summary = "Delete product (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Product p = products.findById(id).orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
        String file = p.getImageFile();
        products.delete(p);
        images.delete(file);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<ProductResponse> created(Product saved) {
        return ResponseEntity.created(URI.create("/api/v1/products/" + saved.getId())).body(ProductResponse.from(saved));
    }
}
