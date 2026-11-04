package vn.edu.bookstore.catalog.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import vn.edu.bookstore.catalog.TestcontainersConfig;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class BookApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    private static final String BOOKS = "/api/v1/books";
    private static final String ISBN = "9780134685991";

    @Test
    void layTheoIsbn() throws Exception {
        mvc.perform(get(BOOKS + "/" + ISBN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Effective Java"));
    }

    @Test
    void phanTrangVaSapXepTheoGiaGiamDan() throws Exception {
        mvc.perform(get(BOOKS)
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "price,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title")
                        .value("Enterprise Integration Patterns"));
    }

    @Test
    void timKiemMicroservicesCoBaKetQua() throws Exception {
        mvc.perform(get(BOOKS).param("q", "microservices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    void sizeVuotQuaGioiHanTraVe400() throws Exception {
        mvc.perform(get(BOOKS).param("size", "1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void etagKhongDoiTraVe304() throws Exception {
        MvcResult first = mvc.perform(get(BOOKS + "/" + ISBN))
                .andExpect(status().isOk())
                .andReturn();

        String etag = first.getResponse().getHeader("ETag");

        mvc.perform(get(BOOKS + "/" + ISBN).header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    @Test
    void isbnKhongTonTaiTraVe404Problem() throws Exception {
        mvc.perform(get(BOOKS + "/9789999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Không tìm thấy sách"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void taoSachMoiTraVe201VaLocation() throws Exception {
        mvc.perform(post(BOOKS)
                        .contentType("application/json")
                        .content("""
                                {
                                  "isbn": "9781098100146",
                                  "title": "Software Architecture The Hard Parts",
                                  "author": "Neal Ford",
                                  "publishedYear": 2021,
                                  "price": {
                                    "amount": 640000.00,
                                    "currency": "VND"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.endsWith("/9781098100146")));
    }

    @Test
    void isbnTrungLapTraVe409() throws Exception {
        mvc.perform(post(BOOKS)
                        .contentType("application/json")
                        .content("""
                                {
                                  "isbn": "9780134685991",
                                  "title": "Another Book",
                                  "author": "Another Author",
                                  "price": {
                                    "amount": 100000,
                                    "currency": "VND"
                                  }
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void putThieuIfMatchTraVe428() throws Exception {
        mvc.perform(put(BOOKS + "/" + ISBN)
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Effective Java Updated",
                                  "author": "Joshua Bloch",
                                  "price": {
                                    "amount": 300000,
                                    "currency": "VND"
                                  }
                                }
                                """))
                .andExpect(status().isPreconditionRequired());
    }

    @Test
    void putDungEtagTraVe200VaEtagMoi() throws Exception {
        MvcResult first = mvc.perform(get(BOOKS + "/" + ISBN))
                .andExpect(status().isOk())
                .andReturn();

        String etag = first.getResponse().getHeader("ETag");

        mvc.perform(put(BOOKS + "/" + ISBN)
                        .header("If-Match", etag)
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Effective Java Updated",
                                  "author": "Joshua Bloch",
                                  "price": {
                                    "amount": 300000,
                                    "currency": "VND"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Effective Java Updated"));
    }

    @Test
    void putEtagCuTraVe412() throws Exception {
        MvcResult first = mvc.perform(get(BOOKS + "/" + ISBN))
                .andExpect(status().isOk())
                .andReturn();

        String etagCu = first.getResponse().getHeader("ETag");

        mvc.perform(put(BOOKS + "/" + ISBN)
                        .header("If-Match", etagCu)
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Effective Java Updated",
                                  "author": "Joshua Bloch",
                                  "price": {
                                    "amount": 300000,
                                    "currency": "VND"
                                  }
                                }
                                """))
                .andExpect(status().isOk());

        mvc.perform(put(BOOKS + "/" + ISBN)
                        .header("If-Match", etagCu)
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Effective Java Old",
                                  "author": "Joshua Bloch",
                                  "price": {
                                    "amount": 310000,
                                    "currency": "VND"
                                  }
                                }
                                """))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    void xoaSachTraVe204() throws Exception {
        String isbn = "9781098100131";

        mvc.perform(post(BOOKS)
                        .contentType("application/json")
                        .content("""
                                {
                                  "isbn": "9781098100131",
                                  "title": "Temporary Book",
                                  "author": "Test Author",
                                  "price": {
                                    "amount": 100000,
                                    "currency": "VND"
                                  }
                                }
                                """))
                .andExpect(status().isCreated());

        mvc.perform(delete(BOOKS + "/" + isbn))
                .andExpect(status().isNoContent());

        mvc.perform(get(BOOKS + "/" + isbn))
                .andExpect(status().isNotFound());
    }
}
