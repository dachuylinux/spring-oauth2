package com.example.springoauth2.controllers;

import lombok.SneakyThrows;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: kratos : 11/17/20
 */
@Controller
@RequestMapping("/")
public class HomeController {

    @Value("${sso.uri.endpoint}")
    private String ssoEndpoint;
    @Value("${sso.client.id}")
    private String clientId;
    @Value("${sso.client.secret}")
    private String secretKey;

    @Value("${lgsp.uri.endpoint}")
    private String lgspEndpoint;
    @Value("${lgsp.token.base64}")
    private String lgspToken;

    @GetMapping(value = {"/", "/home"})
    public String home(Model model,
                       @RequestParam(name = "access_token", required = false, defaultValue = "") String accessToken,
                       @RequestParam(name = "refresh_token", required = false, defaultValue = "") String refreshToken,
                       @RequestParam(name = "id_token", required = false, defaultValue = "") String idToken) {
        model.addAttribute("accessToken", accessToken);
        model.addAttribute("refreshToken", refreshToken);
        model.addAttribute("idToken", idToken);
        model.addAttribute("clientId", clientId);
        return "home";
    }

    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }

    @SneakyThrows
    @GetMapping("/sso/login")
    public ResponseEntity ssoLogin(HttpServletRequest request, Model model) {
        int port = request.getServerPort();
        String hostName = request.getServerName();
        String callback = URLEncoder.encode("http://" + hostName + ":" + port + "/callback", "utf-8");
        String url = ssoEndpoint + "/sso/oauth2?response_type=code&client_id=" + clientId + "&redirect_uri=" + callback;
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(new URI(url)).build();
    }

    @SneakyThrows
    @GetMapping("/sso/logout")
    public ResponseEntity ssoLogout(HttpServletRequest request, Model model, @RequestParam(name = "id_token") String idToken) {
        //1. thực hiện logout ở client
        //2. gửi request tới cổng xác thực yêu cầu single logout những ứng dụng đang dùng chung session
        //client_id: (bắt buộc)
        //id_token_hint: (bắt buộc) giá trị ở bước get access_token
        //post_logout_redirect_uri: giá trị khi logout thành công, sẽ redirect về
        SecurityContextHolder.clearContext();

        String idTokenEncode = URLEncoder.encode(idToken, "utf-8");
        String postLogoutRedirectUri = URLEncoder.encode("http://localhost:" + request.getServerPort() + "/home", "utf-8");
        String sloRequest = lgspEndpoint + "/oidc/logout?client_id=" + clientId + "&id_token_hint=" + idTokenEncode + "&post_logout_redirect_uri=" + postLogoutRedirectUri;
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(new URI(sloRequest)).build();
    }

    @SneakyThrows
    @GetMapping("/callback")
    public String callback(HttpServletRequest request, @RequestParam(required = false) String code) {
        if (code != null && !code.isEmpty()) {
            String tokenEndPoint = lgspEndpoint + "/sso/oauth2/token";
            JSONObject obj = new JSONObject();
            obj.put("grant_type", "authorization_code");
            obj.put("client_id", this.clientId);
            obj.put("client_secret", this.secretKey);
            obj.put("code", code);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, lgspToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> req = new HttpEntity<>(obj.toString(), headers);
            ResponseEntity<String> response = new RestTemplate().postForEntity(tokenEndPoint, req, String.class);
            JSONObject res = new JSONObject(response.getBody());
            if (res.getInt("code") == 0) {
                System.out.println(res.toString());
                String accessToken = res.getJSONObject("data").getString("access_token");
                String refreshToken = res.getJSONObject("data").getString("refresh_token");
                String idToken = res.getJSONObject("data").getString("id_token");
                //get user info not implement
                //fake user data loged, put to SecurityContextHolder
                if (true) {
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    UserDetails userDetail = new User("fake", "fake", authorities);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetail, null,
                            userDetail.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                return "redirect:/home?access_token=" + accessToken + "&refresh_token=" + refreshToken + "&id_token=" + URLEncoder.encode(idToken, "utf-8");
            }
        }
        return "";
    }
}
