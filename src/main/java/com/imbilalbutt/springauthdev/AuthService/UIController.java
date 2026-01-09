package com.imbilalbutt.springauthdev.AuthService;

import com.imbilalbutt.springauthdev.AuthService.AuthenticationRequest;
import com.imbilalbutt.springauthdev.AuthService.AuthenticationResponse;
import com.imbilalbutt.springauthdev.AuthService.RegisterRequest;
import com.imbilalbutt.springauthdev.AuthService.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
        import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ui")
@RequiredArgsConstructor
public class UIController {

    private final UserService userService;

    // Store token in session
    private static final String AUTH_TOKEN = "authToken";
    private static final String USER_EMAIL = "userEmail";
    private static final String USER_NAME = "userName";

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
            return "redirect:/ui/register";
        }

        try {
            AuthenticationResponse authResponse = userService.register(registerRequest);

            // Store authentication in session
            response.addHeader("Set-Cookie",
                    "token=" + authResponse.getToken() + "; HttpOnly; Path=/; Max-Age=86400");

            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration successful! Welcome " + registerRequest.getFirstname());

            // Store user info in session
            redirectAttributes.addFlashAttribute("userEmail", authResponse.getEmail());
            redirectAttributes.addFlashAttribute("userName",
                    authResponse.getFirstname() + " " + authResponse.getLastname());
            redirectAttributes.addFlashAttribute("userRole", authResponse.getRole());

            return "redirect:/ui/dashboard";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
            return "redirect:/ui/register";
        }
    }

    // ========== LOGIN PAGES ==========

    @GetMapping("/login")
    public String showLoginForm(Model model,
                                @RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout) {

        if (!model.containsAttribute("authRequest")) {
            model.addAttribute("authRequest", new AuthenticationRequest());
        }

        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password!");
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
            return "redirect:/ui/login";
        }

        try {
            AuthenticationResponse authResponse = userService.authenticate(authRequest);

            // Store token in cookie (HttpOnly for security)
            response.addHeader("Set-Cookie",
                    "token=" + authResponse.getToken() + "; HttpOnly; Path=/; Max-Age=86400");

            // Store user info in session via flash attributes
            redirectAttributes.addFlashAttribute("successMessage",
                    "Welcome back, " + authResponse.getFirstname() + "!");
            redirectAttributes.addFlashAttribute("userEmail", authResponse.getEmail());
            redirectAttributes.addFlashAttribute("userName",
                    authResponse.getFirstname() + " " + authResponse.getLastname());
            redirectAttributes.addFlashAttribute("userRole", authResponse.getRole());

            return "redirect:/ui/dashboard";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Invalid email or password!");
            redirectAttributes.addFlashAttribute("authRequest", authRequest);
            return "redirect:/ui/login?error";
        }
    }

    // ========== DASHBOARD & PROTECTED PAGES ==========

    @GetMapping("/dashboard")
    public String showDashboard(Model model, HttpServletRequest request) {
        // Check if user is authenticated (you might want to use an interceptor for this)
        String token = getTokenFromCookie(request);
        if (token == null) {
            return "redirect:/ui/login";
        }

        // Add user info to model (you could extract this from JWT token)
        model.addAttribute("pageTitle", "Dashboard");
        return "dashboard/index";
    }

    @GetMapping("/profile")
    public String showProfile(Model model, HttpServletRequest request) {
        String token = getTokenFromCookie(request);
        if (token == null) {
            return "redirect:/ui/login";
        }

        model.addAttribute("pageTitle", "My Profile");
        return "dashboard/profile";
    }

    // ========== LOGOUT ==========

    @GetMapping("/logout")
    public String logout(HttpServletResponse response, RedirectAttributes redirectAttributes) {
        // Clear authentication cookie
        response.addHeader("Set-Cookie",
                "token=; HttpOnly; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT");

        redirectAttributes.addFlashAttribute("successMessage",
                "You have been logged out successfully.");
        return "redirect:/ui/login?logout";
    }

    // ========== PUBLIC PAGES ==========

    @GetMapping("/")
    public String homePage(Model model) {
        model.addAttribute("pageTitle", "Welcome to Spring Auth");
        return "home";
    }

    @GetMapping("/about")
    public String aboutPage(Model model) {
        model.addAttribute("pageTitle", "About Us");
        return "about";
    }

    @GetMapping("/contact")
    public String contactPage(Model model) {
        model.addAttribute("pageTitle", "Contact Us");
        return "contact";
    }

    // ========== HELPER METHODS ==========

    private String getTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}