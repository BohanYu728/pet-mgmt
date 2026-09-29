package com.petmgmt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.datasource.url=jdbc:h2:mem:petmgmt-http-test;MODE=MySQL")
@ActiveProfiles("demo")
class SecurityHttpIntegrationTest {
    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    private final HttpClient http = HttpClient.newHttpClient();

    @Test void liveHttpPreservesCsrfAndAuthorizationStatuses() throws Exception {
        URI loginUri=URI.create("http://127.0.0.1:"+port+"/api/auth/login");
        HttpResponse<String> noCsrf=http.send(HttpRequest.newBuilder(loginUri)
            .header("Content-Type","application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"demo\",\"password\":\"demo123\"}"))
            .build(),HttpResponse.BodyHandlers.ofString());
        assertThat(noCsrf.statusCode()).isEqualTo(403);

        HttpResponse<String> csrf=http.send(HttpRequest.newBuilder(
            URI.create("http://127.0.0.1:"+port+"/api/auth/csrf")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertThat(csrf.statusCode()).isEqualTo(200);
        String csrfCookie=csrf.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];
        String csrfToken=json.readTree(csrf.body()).get("token").asText();
        String csrfHeader=json.readTree(csrf.body()).get("headerName").asText();
        HttpResponse<String> login=http.send(HttpRequest.newBuilder(loginUri)
            .header("Content-Type","application/json")
            .header("Cookie",csrfCookie)
            .header(csrfHeader,csrfToken)
            .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"demo\",\"password\":\"demo123\"}"))
            .build(),HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).isEqualTo(200);
        String sessionCookie=login.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];
        HttpResponse<String> forbidden=http.send(HttpRequest.newBuilder(
            URI.create("http://127.0.0.1:"+port+"/api/admin/users"))
            .header("Cookie",sessionCookie).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertThat(forbidden.statusCode()).isEqualTo(403);
    }
}
