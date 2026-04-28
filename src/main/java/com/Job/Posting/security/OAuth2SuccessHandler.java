package com.Job.Posting.security;

import com.Job.Posting.auth.service.AuthService;
import com.Job.Posting.dto.security.LoginResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final String CALLBACK_SCHEME = "posting";
    private static final String CALLBACK_HOST   = "oauth2callback";  // Bug 5 fix: Flutter expects posting://oauth2callback

    private final AuthService authService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String registrationId = token.getAuthorizedClientRegistrationId();

        try {
            ResponseEntity<LoginResponseDto> loginResponse =
                    authService.handleOAuh2LoginRequests(oAuth2User, registrationId);

            LoginResponseDto body = loginResponse.getBody();
            if (body == null) {
                redirectError(response, "empty_response");
                return;
            }

            String redirectUrl = buildRedirectUrl(body);
            log.debug("OAuth2 success → redirecting to: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception ex) {
            log.error("OAuth2 success handler error: {}", ex.getMessage());
            redirectError(response, encode(ex.getMessage()));
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String buildRedirectUrl(LoginResponseDto body) {
        StringBuilder sb = new StringBuilder();
        sb.append(CALLBACK_SCHEME).append("://").append(CALLBACK_HOST).append("?");
        sb.append("accessToken=").append(encode(body.getAccessToken()));
        sb.append("&refreshToken=").append(encode(body.getRefreshToken()));
        sb.append("&appUserId=").append(body.getAppUserId() != null ? body.getAppUserId() : "");
        sb.append("&profileId=").append(body.getProfileId() != null ? body.getProfileId() : "");
        sb.append("&oauthName=").append(encode(body.getOauthName()));   // Bug 5 fix: was missing
        return sb.toString();
    }

    private void redirectError(HttpServletResponse response, String errorMsg) throws IOException {
        String url = CALLBACK_SCHEME + "://" + CALLBACK_HOST + "?error=" + encode(errorMsg);
        response.sendRedirect(url);
    }

    private static String encode(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}