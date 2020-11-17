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
    public String home(Model model, @RequestParam(name = "access_token", required = false, defaultValue = "abc") String accessToken) {
        model.addAttribute("accessToken", accessToken);
        return "home";
    }

    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }

    @SneakyThrows
    @GetMapping("/sso/login")
    public ResponseEntity ssoLogin(Model model) {
        String callback = URLEncoder.encode("http://localhost:8080/callback", "utf-8");
        String url = ssoEndpoint + "/sso/oauth2?response_type=code&client_id=" + clientId + "&redirect_uri=" + callback;
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(new URI(url)).build();
    }

    @SneakyThrows
    @GetMapping("/sso/logout")
    public ResponseEntity ssoLogout(Model model, @RequestParam(name = "access_token") String accessToken) {
        String logoutnEdpoint = lgspEndpoint + "/sso/oauth2/logout";
        JSONObject obj = new JSONObject();
        obj.put("access_token", accessToken);
        obj.put("client_id", this.clientId);
        obj.put("client_secret", this.secretKey);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, lgspToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(obj.toString(), headers);
        ResponseEntity<String> response = new RestTemplate().postForEntity(logoutnEdpoint, req, String.class);
        JSONObject res = new JSONObject(response.getBody());
        if (res.getInt("code") == 0) {
            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(new URI("/logout")).build();
        }
        return null;
    }

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
                String accessToken = res.getJSONObject("data").getString("access_token");
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
                return "redirect:/home?access_token=" + accessToken;
            }
        }
        return "";
    }
}
