package com.example.IncidentPulse.Security;

import com.example.IncidentPulse.ApplicationCofig.CachingConfig;
import com.example.IncidentPulse.Exception.AppException;
import com.example.IncidentPulse.Exception.ErrorCode;
import com.example.IncidentPulse.Model.User;
import com.example.IncidentPulse.Repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    /**
     * Hit on every authenticated request (by JwtFilter), so it is cached for a
     * short TTL. The cache is evicted whenever a user is updated or deleted, so
     * role/active changes take effect immediately rather than after the TTL.
     */
    @Override
    @Cacheable(cacheNames = CachingConfig.USERS_BY_USERNAME, key = "#username")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findUserByUsername(username).orElseThrow(()->new AppException(ErrorCode.USER_NOT_FOUND));
        return new UserPrincipal(user);
    }
}
