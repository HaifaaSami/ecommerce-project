package ecommerce;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/** اختبارات "الإثبات": الوصول لمورد غيرك أو تنفيذ عملية بلا صلاحية => رفض. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.upload.dir=target/test-uploads",
        "app.security.login.max-attempts=3"})
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private void register(String u, String p) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + u + "\",\"password\":\"" + p + "\"}")).andExpect(status().isCreated());
    }

    private String login(String u, String p) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + u + "\",\"password\":\"" + p + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("accessToken").asText();
    }

    private long createProduct(String adminToken) throws Exception {
        String body = mvc.perform(post("/api/v1/products").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Phone\",\"price\":199.99,\"category\":\"electronics\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private long createOrder(String token, long productId) throws Exception {
        String body = mvc.perform(post("/api/v1/orders").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":" + productId + ",\"quantity\":2}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    @Test
    void unauthenticatedAccessIsRejected() throws Exception {
        mvc.perform(get("/api/v1/orders/my")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/orders/1").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void productsArePublicToReadButAdminOnlyToWrite() throws Exception {
        register("cust1", "password123");
        String cust = login("cust1", "password123");
        mvc.perform(get("/api/v1/products")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/products").header("Authorization", cust)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"price\":1}")).andExpect(status().isForbidden());
    }

    @Test
    void customerCannotReadAnotherCustomersOrder() throws Exception {
        String admin = login("admin", "Admin@12345");
        long pid = createProduct(admin);
        register("alice", "password123");
        register("bob", "password123");
        String alice = login("alice", "password123");
        String bob = login("bob", "password123");

        long aliceOrder = createOrder(alice, pid);

        mvc.perform(get("/api/v1/orders/" + aliceOrder).header("Authorization", alice)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/orders/" + aliceOrder).header("Authorization", bob)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/orders/" + aliceOrder).header("Authorization", admin)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/orders/my").header("Authorization", bob))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void customerCannotChangeOrderStatusButAdminCan() throws Exception {
        String admin = login("admin", "Admin@12345");
        long pid = createProduct(admin);
        register("carol", "password123");
        String carol = login("carol", "password123");
        long oid = createOrder(carol, pid);

        mvc.perform(patch("/api/v1/orders/" + oid + "/status").header("Authorization", carol)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/orders/" + oid + "/status").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SHIPPED"));
        // الزبون استلم إشعاراً بالتغيير
        mvc.perform(get("/api/v1/notifications").header("Authorization", carol))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void registrationCannotEscalateRoleAndNeverLeaksPassword() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"mallory\",\"password\":\"password123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist());
        String t = login("mallory", "password123");
        mvc.perform(get("/api/v1/users").header("Authorization", t)).andExpect(status().isForbidden());
        String admin = login("admin", "Admin@12345");
        String list = mvc.perform(get("/api/v1/users").header("Authorization", admin)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode n = json.readTree(list);
        org.junit.jupiter.api.Assertions.assertFalse(list.contains("password"), "password must never be serialized");
        org.junit.jupiter.api.Assertions.assertTrue(n.get("content").size() >= 2);
    }

    @Test
    void validationAndPaginationWork() throws Exception {
        String admin = login("admin", "Admin@12345");
        mvc.perform(post("/api/v1/products").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
        mvc.perform(get("/api/v1/products/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        createProduct(admin);
        mvc.perform(get("/api/v1/products?category=ELECTRONICS&page=0&size=1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(1));
    }

    @Test
    void bruteForceLoginIsThrottled() throws Exception {
        register("dave", "password123");
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"dave\",\"password\":\"wrongpass1\"}")).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"dave\",\"password\":\"password123\"}")).andExpect(status().isTooManyRequests());
    }

    @Test
    void productImageUploadIsAdminOnlyValidatedAndPublic() throws Exception {
        String admin = login("admin", "Admin@12345");
        register("erin", "password123");
        String erin = login("erin", "password123");
        long pid = createProduct(admin);
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

        // الزبون لا يرفع صورة
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/v1/products/" + pid + "/image")
                .file(new MockMultipartFile("image", "a.png", "image/png", png)).header("Authorization", erin))
                .andExpect(status().isForbidden());
        // ملف ليس صورة (حتى لو ادّعى image/png) => 400
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/v1/products/" + pid + "/image")
                .file(new MockMultipartFile("image", "a.png", "image/png", "not an image".getBytes()))
                .header("Authorization", admin)).andExpect(status().isBadRequest());
        // المسؤول يرفع صورة صحيحة
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/v1/products/" + pid + "/image")
                .file(new MockMultipartFile("image", "a.png", "image/png", png)).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("/api/v1/products/" + pid + "/image"));
        // العرض عام بدون توكن
        mvc.perform(get("/api/v1/products/" + pid + "/image"))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"));
    }
}
