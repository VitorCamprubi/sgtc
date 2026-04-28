package com.vitorcamprubi.sgtc.web.auth;

public class LoginResponse {
    private final String token;
    private final String type;

    public LoginResponse(String token) {
        this(token, "Bearer");
    }

    public LoginResponse(String token, String type) {
        this.token = token;
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public String getType() {
        return type;
    }
}
