package com.mcarchive.security;

import com.mcarchive.model.User;
import com.mcarchive.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security 的 UserDetailsService 实现
 * 
 *  这个 Bean 的存在会让 Spring Security 停止生成随机密码，
 *  auth/login 和 auth/register 通过 AuthController 手动处理，
 *  但有了这个 Bean，SecurityContext 中的 Principal 才是标准类型。
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        return new CustomUserDetails(user);
    }
}
