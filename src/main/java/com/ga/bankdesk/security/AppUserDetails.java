package com.ga.bankdesk.security;

import com.ga.bankdesk.enums.UserStatus;
import com.ga.bankdesk.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

//User database <-Translates and showcases-> what Spring Security expects to see
public class AppUserDetails implements UserDetails {

    @Getter
    private final User user;

    public AppUserDetails(User user){
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        //Tells Spring Security the user's role
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword(){
        return user.getPassword();
    }

    @Override
    public String getUsername(){
        return user.getEmail();
    }

    @Override
    public boolean isEnabled(){
        //blocks login for deactivated (soft-deleted) users
        return user.getStatus() == UserStatus.ACTIVE;
    }

    @Override
    public boolean isAccountNonExpired(){
        return true;
    }

    @Override
    public boolean isAccountNonLocked(){
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired(){
        return true;
    }

}
