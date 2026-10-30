package ecommerce.product;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products", indexes = @Index(name = "idx_product_category", columnList = "category"))
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    private String category;
    @Column(length = 500)
    private String imageUrl;          // رابط خارجي اختياري

    private String imageFile;         // اسم الملف المرفوع على القرص
    private String imageContentType;

    protected Product() {}

    public Product(String name, String description, BigDecimal price, String category, String imageUrl) {
        update(name, description, price, category, imageUrl);
    }

    public void update(String name, String description, BigDecimal price, String category, String imageUrl) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
    }

    public void setImage(String file, String contentType) {
        this.imageFile = file;
        this.imageContentType = contentType;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
    public String getImageFile() { return imageFile; }
    public String getImageContentType() { return imageContentType; }
}
