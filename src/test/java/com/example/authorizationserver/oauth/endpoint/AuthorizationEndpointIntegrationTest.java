package com.example.authorizationserver.oauth.endpoint;

import com.example.authorizationserver.annotation.WebIntegrationTest;
import com.example.authorizationserver.security.user.EndUserDetails;
import com.example.authorizationserver.user.model.User;
import com.example.authorizationserver.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebIntegrationTest
class AuthorizationEndpointIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private UserService userService;

  private MockMvc mockMvc;
  private User bwayne_user;

  @BeforeEach
  void initMockMvc() {
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    Optional<User> bwayne = userService.findOneByUsername("bwayne");
    bwayne.ifPresent(user -> bwayne_user = user);
  }

  @Test
  void authorizationRequest() throws Exception {
    var multiValueMap = new LinkedMultiValueMap<String, String>();
    multiValueMap.add("response_type", "code");
    multiValueMap.add("scope", "openid");
    multiValueMap.add("client_id", "confidential-demo");
    multiValueMap.add("redirect_uri", "http://localhost:9090/demo-client/login/oauth2/code/demo");
    this.mockMvc
        .perform(
            MockMvcRequestBuilders.get(AuthorizationEndpoint.ENDPOINT)
                .accept(MediaType.APPLICATION_JSON)
                .queryParams(multiValueMap)
                .with(user(new EndUserDetails(bwayne_user))))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("http://localhost:9090/demo-client/login/oauth2/code/demo?code=**"));
  }

  @Test
  void authorizationRequestForPublicClient() throws Exception {
    var multiValueMap = new LinkedMultiValueMap<String, String>();
    multiValueMap.add("response_type", "code");
    multiValueMap.add("scope", "openid");
    multiValueMap.add("client_id", "public-demo");
    multiValueMap.add("code_challenge", "123456789");
    multiValueMap.add("redirect_uri", "http://localhost:9090/demo-client/login/oauth2/code/demo");
    this.mockMvc
            .perform(
                    MockMvcRequestBuilders.get(AuthorizationEndpoint.ENDPOINT)
                            .accept(MediaType.APPLICATION_JSON)
                            .queryParams(multiValueMap)
                            .with(user(new EndUserDetails(bwayne_user))))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("http://localhost:9090/demo-client/login/oauth2/code/demo?code=**"));
  }

  @Test
  void authorizationRequestForPublicClientMissingCodeChallenge() throws Exception {
    var multiValueMap = new LinkedMultiValueMap<String, String>();
    multiValueMap.add("response_type", "code");
    multiValueMap.add("scope", "openid");
    multiValueMap.add("client_id", "public-demo");
    multiValueMap.add("redirect_uri", "http://localhost:9090/demo-client/login/oauth2/code/demo");
    this.mockMvc
            .perform(
                    MockMvcRequestBuilders.get(AuthorizationEndpoint.ENDPOINT)
                            .accept(MediaType.APPLICATION_JSON)
                            .queryParams(multiValueMap)
                            .with(user(new EndUserDetails(bwayne_user))))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("http://localhost:9090/demo-client/login/oauth2/code/demo?error=invalid_request&error_description=code_challenge**"));
  }

  @Test
  void authorizationRequestWithEmptyScope() throws Exception {
    var multiValueMap = new LinkedMultiValueMap<String, String>();
    multiValueMap.add("response_type", "code");
    multiValueMap.add("scope", "");
    multiValueMap.add("client_id", "confidential-demo");
    multiValueMap.add("redirect_uri", "http://localhost:9090/demo-client/login/oauth2/code/demo");
    this.mockMvc
            .perform(
                    MockMvcRequestBuilders.get(AuthorizationEndpoint.ENDPOINT)
                            .accept(MediaType.APPLICATION_JSON)
                            .queryParams(multiValueMap)
                            .with(user(new EndUserDetails(bwayne_user))))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("http://localhost:9090/demo-client/login/oauth2/code/demo?error=invalid_scope**"));
  }

  @Test
  void authorizationRequestWrongResponseType() throws Exception {
    var multiValueMap = new LinkedMultiValueMap<String, String>();
    multiValueMap.add("response_type", "id_token");
    multiValueMap.add("scope", "openid");
    multiValueMap.add("client_id", "confidential-demo");
    multiValueMap.add("redirect_uri", "http://localhost:9090/demo-client/login/oauth2/code/demo");
    this.mockMvc
            .perform(
                    MockMvcRequestBuilders.get(AuthorizationEndpoint.ENDPOINT)
                            .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED)
                            .queryParams(multiValueMap)
                            .with(user(new EndUserDetails(bwayne_user))))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().string(endsWith("error=unsupported_response_type")));
  }

  @Test
  void authorizationRequestMissingClientId() throws Exception {
    var multiValueMap = new LinkedMultiValueMap<String, String>();
    multiValueMap.add("response_type", "code");
    multiValueMap.add("scope", "openid");
    multiValueMap.add("redirect_uri", "http://localhost:9090/demo-client/login/oauth2/code/demo");
    this.mockMvc
            .perform(
                    MockMvcRequestBuilders.get(AuthorizationEndpoint.ENDPOINT)
                            .accept(MediaType.APPLICATION_JSON)
                            .queryParams(multiValueMap)
                            .with(user(new EndUserDetails(bwayne_user))))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(endsWith("'client_id' is not present")));
  }

  @Test
  void authorizationRequestEmptyClientId() throws Exception {
    var multiValueMap = new LinkedMultiValueMap<String, String>();
    multiValueMap.add("response_type", "code");
    multiValueMap.add("scope", "openid");
    multiValueMap.add("client_id", "");
    multiValueMap.add("redirect_uri", "http://localhost:9090/demo-client/login/oauth2/code/demo");
    this.mockMvc
            .perform(
                    MockMvcRequestBuilders.get(AuthorizationEndpoint.ENDPOINT)
                            .accept(MediaType.APPLICATION_JSON)
                            .queryParams(multiValueMap)
                            .with(user(new EndUserDetails(bwayne_user))))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(endsWith("error=invalid client")));
  }

  @Test
  void authorizationRequestWithWrongRedirectUri() throws Exception {
    var multiValueMap = new LinkedMultiValueMap<String, String>();
    multiValueMap.add("response_type", "code");
    multiValueMap.add("scope", "openid");
    multiValueMap.add("client_id", "confidential-demo");
    multiValueMap.add("redirect_uri", "http://localhost:9090/dummy");
    this.mockMvc
            .perform(
                    MockMvcRequestBuilders.get(AuthorizationEndpoint.ENDPOINT)
                            .accept(MediaType.APPLICATION_JSON)
                            .queryParams(multiValueMap)
                            .with(user(new EndUserDetails(bwayne_user))))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().string("error=redirect uri mismatch"));
  }


  @Test
  void authorizationRequestWithAnonymousShouldRedirectToLogin() throws Exception {
    var multiValueMap = new LinkedMultiValueMap<String, String>();
    multiValueMap.add("response_type", "code");
    multiValueMap.add("scope", "openid");
    multiValueMap.add("client_id", "confidential-demo");
    multiValueMap.add("redirect_uri", "http://localhost:9090/demo-client/login/oauth2/code/demo");
    this.mockMvc
            .perform(
                    MockMvcRequestBuilders.get(AuthorizationEndpoint.ENDPOINT)
                            .accept(MediaType.APPLICATION_JSON)
                            .queryParams(multiValueMap)
                            .with(anonymous()))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));
  }

  @Test
  void authorizationRequestWithMissingUserShouldRedirectToLogin() throws Exception {
    var multiValueMap = new LinkedMultiValueMap<String, String>();
    multiValueMap.add("response_type", "code");
    multiValueMap.add("scope", "openid");
    multiValueMap.add("client_id", "confidential-demo");
    multiValueMap.add("redirect_uri", "http://localhost:9090/demo-client/login/oauth2/code/demo");
    this.mockMvc
            .perform(
                    MockMvcRequestBuilders.get(AuthorizationEndpoint.ENDPOINT)
                            .accept(MediaType.APPLICATION_JSON)
                            .queryParams(multiValueMap))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));
  }

  @Test
  void authorizationRequestWithWrongClientId() throws Exception {
    var multiValueMap = new LinkedMultiValueMap<String, String>();
    multiValueMap.add("response_type", "code");
    multiValueMap.add("scope", "openid");
    multiValueMap.add("client_id", "dummy");
    multiValueMap.add("redirect_uri", "http://localhost:9090/demo-client/login/oauth2/code/demo");
    this.mockMvc
            .perform(
                    MockMvcRequestBuilders.get(AuthorizationEndpoint.ENDPOINT)
                            .accept(MediaType.APPLICATION_JSON)
                            .queryParams(multiValueMap)
                            .with(user(new EndUserDetails(bwayne_user))))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().string(endsWith("error=invalid client")));
  }
}
