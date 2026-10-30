# E-commerce Backend — المتطلب الأول: تصميم الواجهات البرمجية والأمان

Spring Boot 3.2 / Java 17 · PostgreSQL · JWT · رفع صور المنتجات · أدوار (CUSTOMER / ADMIN) · OpenAPI · Docker

## التشغيل
```bash
docker compose up --build        # REST على 8080
# أو بدون Docker (يلزم PostgreSQL محلي: قاعدة ecommerce ومستخدم ecommerce/ecommerce):
mvn spring-boot:run              # يتطلب Java 17 و Maven
```
- Swagger UI: http://localhost:8080/swagger-ui.html
- حساب المسؤول الافتراضي: `admin / Admin@12345` (غيّره بمتغيرات `ADMIN_USERNAME` / `ADMIN_PASSWORD`)
- **غيّر `JWT_SECRET`** (32 حرف على الأقل) في docker-compose قبل أي استخدام حقيقي.
- التوثيق: استورد `ecommerce.postman_collection.json` في Postman (فيه طلبات "PROOF" تثبت رفض الوصول).

## قاعدة البيانات
PostgreSQL 16 في حاوية مستقلة (`db`) مع volume دائم `pgdata`؛ المخطط في `docs/schema.sql`.
لمعاينة الجداول: `docker compose exec db psql -U ecommerce -d ecommerce -c "\\dt"`
الاختبارات (`mvn test`) تستخدم H2 في الذاكرة فلا تحتاج PostgreSQL.

## الوحدات الأربع
| الوحدة | الحزمة | ملاحظات |
|---|---|---|
| المستخدمون | `ecommerce.user` | تسجيل/دخول/ملفي/قائمة (ADMIN) |
| المنتجات | `ecommerce.product` | قراءة عامة مع صفحات وتصفية؛ إنشاء/تعديل/حذف للمسؤول |
| الطلبات | `ecommerce.order` | الزبون يرى طلباته فقط؛ المسؤول وحده يغيّر الحالة |
| الإشعارات | `ecommerce.notification` | كل مستخدم يرى إشعاراته فقط؛ تُنشأ تلقائياً عند إنشاء/تغيير الطلب |

## الواجهات (REST /api/v1)
| Method | Path | الصلاحية |
|---|---|---|
| POST | /auth/register , /auth/login | عام |
| GET | /users/me | مسجّل |
| GET | /users | ADMIN |
| GET | /products?category=&page=&size=&sort= , /products/{id} | عام |
| POST/PUT/DELETE | /products , /products/{id} | ADMIN |
| POST | /products (multipart: product=JSON + image=ملف) | ADMIN — إضافة منتج مع صورته |
| PUT | /products/{id}/image (multipart: image) | ADMIN — رفع/استبدال الصورة (JPEG/PNG/WEBP حتى 2MB) |
| GET | /products/{id}/image | عام — عرض الصورة |
| POST | /orders | مسجّل (الزبون من التوكن) |
| GET | /orders/my | مسجّل |
| GET | /orders/{id} | صاحب الطلب أو ADMIN، غير ذلك 403 |
| GET | /orders | ADMIN |
| PATCH | /orders/{id}/status | ADMIN |
| GET | /notifications , PATCH /notifications/{id}/read | مسجّل (إشعاراته فقط) |
| POST | /notifications | ADMIN |

أخطاء موحدة: `{timestamp, status, error, message, path, fieldErrors?}`.
رموز الحالة: 201 للإنشاء، 400 تحقق، 401 بلا توكن، 403 بلا صلاحية، 404، 409 اسم مستخدم مكرر، 429 كثرة محاولات الدخول.

## الأمان
- JWT موقّع HS256، صلاحية 60 دقيقة، جلسات stateless.
- كلمات المرور BCrypt ولا تظهر في أي استجابة (DTOs).
- التسجيل العام يعطي دور CUSTOMER دائماً؛ ADMIN فقط من التهيئة.
- `customerId` يؤخذ من التوكن، والسعر والإجمالي والحالة تُحسب في الخادم.
- تحديد محاولات الدخول: 5 محاولات فاشلة/5 دقائق لكل (مستخدم + IP) ثم 429.
- ترويسات أمان: HSTS, CSP, X-Frame-Options, X-Content-Type-Options, Referrer-Policy.
- تحقق من المدخلات (`@Valid`) وحد أقصى لحجم الصفحة (100).

## الاختبارات
`mvn test` ← `SecurityIntegrationTest` يثبت: 401 بلا توكن، 403 عند قراءة طلب غيرك، 403 عند تغيير الحالة بدون صلاحية، منع رفع الصلاحيات، عدم تسريب كلمة المرور، 429 عند التخمين.

## ملاحظة للمتطلب الثاني
لا يوجد gRPC في هذا التسليم (المتطلب الأول فقط). المخزون يُبنى لاحقاً كخدمة مستقلة بلغة/إطار مختلف وقاعدة بيانات خاصة، ويتصل بها هذا التطبيق عبر gRPC.
