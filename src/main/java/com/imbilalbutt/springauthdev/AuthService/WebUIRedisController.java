package com.imbilalbutt.springauthdev.AuthService;

import com.imbilalbutt.springauthdev.Session.Redis.SessionRegistry;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/ui/auth/redis")
@RequiredArgsConstructor
public class WebUIRedisController {

    private final WebUserService userService; // Use UserService instead of ApiUserService
    private final SessionRegistry sessionRegistry;

    private static final String SESSION_COOKIE_NAME = "SESSION_ID";

    // ========== REGISTRATION PAGES ==========

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(
            @Valid @ModelAttribute RegisterRequest registerRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletResponse response) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.registerRequest",
                    bindingResult
            );
            redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
            return "redirect:/ui/auth/redis/register";
        }

        try {
            // Register user (this should create user in DB)
            User user = userService.createUserAccount(registerRequest);

            // Create Redis session
            String sessionId = sessionRegistry.registerSession(
                    user.getEmail()
            );

            // Set Redis session cookie
            Cookie sessionCookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
            sessionCookie.setPath("/");
            sessionCookie.setHttpOnly(true);
            sessionCookie.setMaxAge(24 * 60 * 60); // 24 hours
            response.addCookie(sessionCookie);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration successful! Welcome " + user.getFirstname());

            // Store user info in flash attributes (temporary for this request)
            redirectAttributes.addFlashAttribute("userEmail", user.getEmail());
            redirectAttributes.addFlashAttribute("userName",
                    user.getFirstname() + " " + user.getLastname());
            redirectAttributes.addFlashAttribute("userRole", user.getRole());

            return "redirect:/ui/dashboard";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
            return "redirect:/ui/auth/redis/register";
        }
    }

    // ========== LOGIN PAGES ==========

    @GetMapping("/login")
    public String showLoginForm(Model model,
                                @RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "expired", required = false) String expired) {

        if (!model.containsAttribute("authRequest")) {
            model.addAttribute("authRequest", new AuthenticationRequest());
        }

        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password!");
        }

        if (expired != null) {
            model.addAttribute("errorMessage", "Session expired. Please login again.");
        }

        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }

        return "auth/login";
    }

    @PostMapping("/login")
    public String authenticateUser(
            @Valid @ModelAttribute AuthenticationRequest authRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletResponse response) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.authRequest",
                    bindingResult
            );
            redirectAttributes.addFlashAttribute("authRequest", authRequest);
            return "redirect:/ui/auth/redis/login";
        }

        try {
            // Authenticate user (this validates credentials)
            AuthenticationResponse authResponse = userService.authenticate(authRequest);

            // Create Redis session
            String sessionId = sessionRegistry.registerSession(authResponse.getEmail());

            // Set Redis session cookie
            Cookie sessionCookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
            sessionCookie.setPath("/");
            sessionCookie.setHttpOnly(true);
            sessionCookie.setMaxAge(24 * 60 * 60); // 24 hours
            response.addCookie(sessionCookie);

            // Store user info in flash attributes
            redirectAttributes.addFlashAttribute("successMessage",
                    "Welcome back, " + authResponse.getFirstname() + "!");
            redirectAttributes.addFlashAttribute("userEmail", authResponse.getEmail());
            redirectAttributes.addFlashAttribute("userName",
                    authResponse.getFirstname() + " " + authResponse.getLastname());
            redirectAttributes.addFlashAttribute("userRole", authResponse.getRole());

            return "redirect:/ui/auth/redis/dashboard";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Invalid email or password!");
            redirectAttributes.addFlashAttribute("authRequest", authRequest);
            return "redirect:/ui/auth/redis/login?error";
        }
    }

    // ========== DASHBOARD & PROTECTED PAGES ==========

    @GetMapping("/dashboard")
    public String showDashboard(Model model, HttpServletRequest request, HttpServletResponse response) {
        // Get session from cookie
        String sessionId = getSessionIdFromCookie(request);

        if (sessionId == null) {
            return "redirect:/ui/auth/redis/login";
        }

        // Validate session in Redis
        String username = sessionRegistry.getUsernameForSession(sessionId);

        if (username == null) {
            // Session expired or invalid
            clearSessionCookie(request, response);
            return "redirect:/ui/auth/redis/login?expired";
        }

        // Add user info to model from Redis session
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("userEmail", username);

        return "dashboard/index";
    }

    @GetMapping("/profile")
    public String showProfile(Model model, HttpServletRequest request, HttpServletResponse response) {
        String sessionId = getSessionIdFromCookie(request);

        if (sessionId == null) {
            return "redirect:/ui/auth/redis/login";
        }

        String username = sessionRegistry.getUsernameForSession(sessionId);

        if (username == null) {
            clearSessionCookie(request, response);
            return "redirect:/ui/auth/redis/login?expired";
        }

        model.addAttribute("pageTitle", "My Profile");
        model.addAttribute("userEmail", username);
//        model.addAttribute("userName", sessionInfo.getDisplayName());
//        model.addAttribute("userRole", sessionInfo.getRole());

        return "dashboard/profile";
    }

    // ========== LOGOUT ==========

    @GetMapping("/logout")
    public String logout(HttpServletRequest request,
                         HttpServletResponse response,
                         RedirectAttributes redirectAttributes) {

        String sessionId = getSessionIdFromCookie(request);

        if (sessionId != null) {
            // Invalidate Redis session
            sessionRegistry.invalidateSession(sessionId);

            // Clear session cookie
            clearSessionCookie(response);
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "You have been logged out successfully.");
        return "redirect:/ui/auth/redis/login?logout";
    }

    // ========== HELPER METHODS ==========

    private String getSessionIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void clearSessionCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0); // Delete cookie
        response.addCookie(cookie);
    }

    // Helper method with request parameter for consistency
    private void clearSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        clearSessionCookie(response);
    }
}