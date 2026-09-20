package com.aquanexus.service;

import com.aquanexus.dto.*;
import com.aquanexus.model.User;
import com.aquanexus.repository.UserRepository;
import com.aquanexus.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final GoogleOAuthService googleOAuthService;

    // Strong password: min 8 chars, 1 uppercase, 1 number
    private static final Pattern STRONG_PASSWORD =
            Pattern.compile("^(?=.*[A-Z])(?=.*\\d).{8,}$");

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       GoogleOAuthService googleOAuthService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.googleOAuthService = googleOAuthService;
    }

    // =========================================================
    // REGISTER
    // =========================================================
    public AuthResponse register(RegisterRequest req) {
        // Validate passwords match
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match.");
        }

        // Validate password strength
        if (!STRONG_PASSWORD.matcher(req.getPassword()).matches()) {
            throw new RuntimeException(
                "Password must be at least 8 characters, contain 1 uppercase letter and 1 number.");
        }

        // Check duplicate email
        if (userRepository.existsByEmail(req.getEmail().toLowerCase())) {
            throw new RuntimeException("Email is already registered.");
        }

        // Check duplicate username
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("Username is already taken.");
        }

        // Create user
        User user = new User();
        user.setFullName(req.getFullName());
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail().toLowerCase());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole("User");
        user.setOAuth(false);

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return buildAuthResponse(token, user);
    }

    // =========================================================
    // LOGIN
    // =========================================================
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Invalid email or password."));

        // OAuth users cannot login with password
        if (user.isOAuth()) {
            throw new RuntimeException("This account uses Google Sign-In. Please use 'Sign in with Google'.");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password.");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return buildAuthResponse(token, user);
    }

    // =========================================================
    // GOOGLE LOGIN / REGISTER
    // =========================================================
    public AuthResponse googleLogin(GoogleLoginRequest req) {
        // Verify token with Google and get profile
        Map<String, Object> profile = googleOAuthService.verifyAndGetProfile(req.getAccessToken());

        String email = ((String) profile.get("email")).toLowerCase();
        String fullName = (String) profile.get("name");
        String photoUrl = (String) profile.get("picture");
        String googleSub = (String) profile.get("sub");

        // Find existing user or create new one
        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Always refresh name and photo from Google
            user.setFullName(fullName);
            user.setPhotoUrl(photoUrl);
            userRepository.save(user);
        } else {
            // Register new Google user
            user = new User();
            user.setFullName(fullName);
            user.setUsername(generateUsername(email));
            user.setEmail(email);
            user.setPassword(null); // No password for OAuth users
            user.setPhotoUrl(photoUrl);
            user.setRole("User");
            user.setOAuth(true);
            userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return buildAuthResponse(token, user);
    }

    // =========================================================
    // GET CURRENT USER (from JWT)
    // =========================================================
    public AuthResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));
        String token = jwtUtil.generateToken(user.getEmail());
        return buildAuthResponse(token, user);
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private AuthResponse buildAuthResponse(String token, User user) {
        return new AuthResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getPhotoUrl(),
                user.getRole(),
                user.isOAuth()
        );
    }

    private String generateUsername(String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }
}
