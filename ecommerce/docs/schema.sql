-- مخطط قاعدة البيانات (للتوثيق والشرح). Hibernate ينشئ هذه الجداول تلقائياً (ddl-auto=update).
CREATE TABLE users (
    id        BIGSERIAL PRIMARY KEY,
    username  VARCHAR(50)  NOT NULL UNIQUE,
    password  VARCHAR(255) NOT NULL,            -- BCrypt hash
    role      VARCHAR(255) NOT NULL             -- CUSTOMER | ADMIN
);

CREATE TABLE products (
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(255)  NOT NULL,
    description        VARCHAR(2000),
    price              NUMERIC(12,2) NOT NULL,
    category           VARCHAR(255),
    image_url          VARCHAR(500),            -- رابط خارجي اختياري
    image_file         VARCHAR(255),            -- اسم الصورة المرفوعة على القرص
    image_content_type VARCHAR(255)
);
CREATE INDEX idx_product_category ON products (category);

CREATE TABLE orders (
    id           BIGSERIAL PRIMARY KEY,
    customer_id  BIGINT        NOT NULL,        -- users.id (من التوكن)
    status       VARCHAR(255)  NOT NULL,        -- PENDING|CONFIRMED|SHIPPED|DELIVERED|CANCELLED
    total_amount NUMERIC(14,2) NOT NULL,
    created_at   TIMESTAMP     NOT NULL
);
CREATE INDEX idx_order_customer ON orders (customer_id);

CREATE TABLE order_items (
    order_id   BIGINT        NOT NULL REFERENCES orders(id),
    product_id BIGINT        NOT NULL,
    quantity   INTEGER       NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL           -- السعر وقت الطلب
);

CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL,           -- users.id
    message    VARCHAR(500) NOT NULL,
    is_read    BOOLEAN      NOT NULL,
    created_at TIMESTAMP    NOT NULL
);
CREATE INDEX idx_notif_user ON notifications (user_id);
