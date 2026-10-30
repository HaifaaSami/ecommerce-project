package ecommerce.product;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class ProductDtos {
    private ProductDtos() {}

    public record ProductRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal price,
            @Size(max = 100) String category,
            @Size(max = 500) @Pattern(regexp = "^(https?://.*)?$", message = "imageUrl must be an http(s) URL") String imageUrl) {}

    public record ProductResponse(Long id, String name, String description, BigDecimal price,
                                  String category, String imageUrl) {
        public static ProductResponse from(Product p) {
            String image = p.getImageFile() != null
                    ? "/api/v1/products/" + p.getId() + "/image"
                    : p.getImageUrl();
            return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                    p.getCategory(), image);
        }
    }
}
