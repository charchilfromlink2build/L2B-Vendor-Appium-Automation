package com.l2b.vendor.modules.onboarding.data.dto;

import java.util.Map;

/** Swagger {@code LoginResponse} for POST /api/v1/auth/verify-otp (200). */
public class LoginResponse {

    public boolean success;
    public String message;
    public boolean is_new_user;
    public String user_id;
    public String role;
    public String profile_category;
    public String onboarding_status;
    public Map<String, Boolean> completed_steps;
    public String access_token;
    public String refresh_token;
    public String token_type;
    public Integer expires_in;
    public Integer refresh_expires_in;
}
