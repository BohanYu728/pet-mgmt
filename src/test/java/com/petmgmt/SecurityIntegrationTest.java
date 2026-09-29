package com.petmgmt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petmgmt.config.JwtConfig;
import com.petmgmt.mapper.AdminUserMapper;
import com.petmgmt.repository.PetRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import java.util.Date;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AdminUserMapper users;
    @Autowired PetRepository pets;
    @Autowired JwtConfig jwtConfig;

    @Test void csrfRequiredForLoginAndTokenNeverReturnedInJson() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"demo\",\"password\":\"demo123\"}"))
            .andExpect(status().isForbidden());
        MockHttpServletResponse login=mvc.perform(post("/api/auth/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"demo\",\"password\":\"demo123\"}"))
            .andExpect(status().isOk()).andReturn().getResponse();
        assertThat(login.getHeader("Set-Cookie")).contains("token=").contains("HttpOnly");
        assertThat(login.getContentAsString()).doesNotContain("\"token\"").doesNotContain("passwordHash");
    }

    @Test void corsAllowsBothLocalDevelopmentOriginsAndRejectsOthers() throws Exception {
        for (String origin : new String[]{"http://localhost:5173", "http://127.0.0.1:5173"}) {
            mvc.perform(options("/api/auth/login")
                    .header("Origin", origin)
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        }
        mvc.perform(options("/api/auth/login")
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test @Order(1)
    void csrfEndpointSupportsBrowserLoginFlow() throws Exception {
        MockHttpServletResponse csrf=mvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
            .andReturn().getResponse();
        String token=json.readTree(csrf.getContentAsString()).get("token").asText();
        Cookie csrfCookie=csrf.getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        mvc.perform(post("/api/auth/login").cookie(csrfCookie).header("X-XSRF-TOKEN",token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"demo\",\"password\":\"demo123\"}"))
            .andExpect(status().isOk());
    }

    @Test void unauthenticatedAndNonAdminCannotManageUsers() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        Cookie cookie=login("demo","demo123");
        mvc.perform(get("/api/admin/users").cookie(cookie)).andExpect(status().isForbidden());
    }

    @Test void legacyPagesRedirectToSupportedVueRoutes() throws Exception {
        mvc.perform(get("/auth/login"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location","http://localhost:5173/login"));
        mvc.perform(get("/pets/7"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location","http://localhost:5173/pets/7/health"));
        mvc.perform(get("/admin/users"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location","http://localhost:5173/admin/users"));
        mvc.perform(get("/api/auth/profile")).andExpect(status().isUnauthorized());
    }

    @Test void adminCrudHashesPasswordAndRejectsRelatedDeletion() throws Exception {
        Cookie cookie=login("admin","admin123");
        MockHttpServletResponse created=mvc.perform(post("/api/admin/users").cookie(cookie)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"created\",\"email\":\"created@example.com\",\"password\":\"secret123\",\"role\":\"user\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.user.passwordHash").doesNotExist())
            .andReturn().getResponse();
        long id=json.readTree(created.getContentAsString()).path("user").path("id").asLong();
        assertThat(users.find(id).getPasswordHash()).startsWith("$2").isNotEqualTo("secret123");
        mvc.perform(post("/api/admin/users").cookie(cookie).with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"duplicate\",\"email\":\"created@example.com\",\"password\":\"secret123\",\"role\":\"user\"}"))
            .andExpect(status().isConflict());
        mvc.perform(get("/api/admin/users").cookie(cookie))
            .andExpect(status().isOk()).andExpect(jsonPath("$.users").isArray());
        mvc.perform(put("/api/admin/users/"+id).cookie(cookie).with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"updated\",\"email\":\"updated@example.com\",\"role\":\"merchant\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("merchant"));
        mvc.perform(delete("/api/admin/users/"+id).cookie(cookie).with(SecurityMockMvcRequestPostProcessors.csrf()))
            .andExpect(status().isOk());
        mvc.perform(delete("/api/admin/users/2").cookie(cookie)
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
            .andExpect(status().isConflict());
    }

    @Test void crossUserCannotReadOrReplacePetAvatar() throws Exception {
        mvc.perform(post("/api/auth/register").with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"outsider\",\"email\":\"outsider@example.com\",\"password\":\"secret123\"}"))
            .andExpect(status().isOk());
        Cookie cookie=login("outsider","secret123");
        long petId=pets.findAll().get(0).getId();
        mvc.perform(get("/api/pets/"+petId).cookie(cookie)).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/pets/"+petId+"/avatar")
                .file("avatar",new byte[]{1,2,3}).cookie(cookie)
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
            .andExpect(status().isForbidden());
    }

    @Test void tamperedExpiredAndLoggedOutTokensAreDenied() throws Exception {
        Cookie cookie=login("demo","demo123");
        String value=cookie.getValue();
        Cookie tampered=new Cookie("token",value.substring(0,value.length()-1)+(value.endsWith("x")?"y":"x"));
        mvc.perform(get("/api/auth/profile").cookie(tampered)).andExpect(status().isUnauthorized());
        String expired=Jwts.builder().id("expired-test").subject("2")
            .issuedAt(new Date(1_000)).expiration(new Date(2_000))
            .signWith(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8))).compact();
        mvc.perform(get("/api/auth/profile").cookie(new Cookie("token",expired))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout").cookie(cookie).with(SecurityMockMvcRequestPostProcessors.csrf()))
            .andExpect(status().isOk()).andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.containsString("Max-Age=0")));
        mvc.perform(get("/api/auth/profile").cookie(cookie)).andExpect(status().isUnauthorized());
        Cookie nextSession=login("demo","demo123");
        assertThat(nextSession.getValue()).isNotEqualTo(cookie.getValue());
        mvc.perform(get("/api/auth/profile").cookie(nextSession)).andExpect(status().isOk());
    }

    private Cookie login(String username,String password) throws Exception {
        MockHttpServletResponse response=mvc.perform(post("/api/auth/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username",username,"password",password))))
            .andExpect(status().isOk()).andReturn().getResponse();
        return response.getCookie("token");
    }
}
