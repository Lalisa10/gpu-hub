//package com.trucdnd.gpu_hub_backend.common.security;
//
//import com.trucdnd.gpu_hub_backend.user.entity.User;
//import com.trucdnd.gpu_hub_backend.user.repository.UserRepository;
//import lombok.AllArgsConstructor;
//import org.jspecify.annotations.NullMarked;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//@Service
//@AllArgsConstructor
//public class MyUserDetailsService implements UserDetailsService {
//    UserRepository userRepository;
//    @Override
//    @NullMarked
//    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found!"));
//        return new UserPrincipal(user);
//    }
//}
