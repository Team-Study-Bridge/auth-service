package com.example.authservice.config.security.oauth;

import com.example.authservice.model.User;
import com.example.authservice.type.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    private boolean needsLinking = false;
    private final User user;
    private final String jwtToken;
    private final String refreshToken;

    public String getJwtToken() {
        return jwtToken;
    }
    public String getRefreshToken() {
        return refreshToken;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(user.getRole().name()));
    }

    @Override
    public Map<String, Object> getAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", user.getEmail());
        attributes.put("name", user.getNickname());
        attributes.put("phoneNumber", user.getPhoneNumber());
        attributes.put("providerId", user.getProviderId());
        attributes.put("provider", user.getProvider().name());
        attributes.put("role", user.getRole().name());
        return attributes;
    }

    @Override
    public String getName() {
        return user.getEmail();
    }

    public User getUser() {
        return user;
    }


    public Provider getProvider() {
        return user.getProvider();
    }

    public void setProvider(Provider provider) {
        this.user.setProvider(provider);
    }

    public boolean isNeedsLinking() {
        return needsLinking;
    }

    public void setNeedsLinking(boolean needsLinking) {
        this.needsLinking = needsLinking;
    }

}
