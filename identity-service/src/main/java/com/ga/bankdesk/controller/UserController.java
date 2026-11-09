package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.ChangePassword;
import com.ga.bankdesk.dto.UpdateProfileRequest;
import com.ga.bankdesk.dto.UserResponse;
import com.ga.bankdesk.mapper.UserMapper;
import com.ga.bankdesk.security.AppUserDetails;
import com.ga.bankdesk.service.AuthService;
import com.ga.bankdesk.service.FileStorageService;
import com.ga.bankdesk.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;


@RequestMapping("/api/users")
@RequiredArgsConstructor
@RestController
public class UserController {

    private final UserMapper userMapper;
    private final AuthService authService;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    //get currently logged user for the token that was checked in JwtAuthFilter
    @GetMapping("/me")
    public UserResponse getCurrentLoggedUser(@AuthenticationPrincipal AppUserDetails userDetails){
        return userMapper.toRespond(userDetails.getUser());
    }

    @PutMapping("/me")
    public UserResponse updateProfile(@AuthenticationPrincipal AppUserDetails userDetails,
                                      @Valid @RequestBody UpdateProfileRequest request){
        return userService.updateProfile(userDetails.getUser(), request.fullName());
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@AuthenticationPrincipal AppUserDetails userDetails,
                                                 @Valid @RequestBody ChangePassword request){
        authService.changePassword(userDetails.getUser(), request.currentPassword(), request.newPassword());
        return ResponseEntity.ok("Your password has been changed successfully");
    }

    @PostMapping("/me/profile-pic")
    public UserResponse uploadProfilePic(@AuthenticationPrincipal AppUserDetails userDetails,
                                         @RequestParam("file") MultipartFile file){
        return userService.uploadProfilePic(userDetails.getUser(), file);
    }

    @GetMapping("/me/profile-pic")
    public ResponseEntity<Resource> myProfilePic(@AuthenticationPrincipal AppUserDetails userDetails) throws IOException {
        String stored = userDetails.getUser().getProfilePicPath();
        if (stored == null) {
            return ResponseEntity.notFound().build();
        }
        Path file = fileStorageService.resolve(stored);
        MediaType type = MediaTypeFactory.getMediaType(stored).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(type).body(new UrlResource(file.toUri()));
    }
}
