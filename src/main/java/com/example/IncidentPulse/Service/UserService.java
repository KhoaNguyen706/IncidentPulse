package com.example.IncidentPulse.Service;

import com.example.IncidentPulse.DTO.Request.UpdatedUserRequest;
import com.example.IncidentPulse.DTO.Request.UserRequest;
import com.example.IncidentPulse.DTO.Response.UpdatedUserResponse;
import com.example.IncidentPulse.DTO.Response.UserResponse;
import com.example.IncidentPulse.Exception.AppException;
import com.example.IncidentPulse.Exception.ErrorCode;
import com.example.IncidentPulse.Mapper.UserMapper;
import com.example.IncidentPulse.Model.User;
import com.example.IncidentPulse.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService{

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Autowired
    public UserService(UserRepository userRepository, UserMapper userMapper){
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public UserResponse addAUser(UserRequest userRequest){

        User user = userMapper.toEntity(userRequest);

        Optional<User> existingUser = userRepository.findUserByUsername(user.getUsername());
        if(existingUser.isPresent()) throw new AppException(ErrorCode.USER_EXISTED);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        user.setHashedPassword(passwordEncoder.encode(userRequest.getPassword()));
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    public List<UserResponse> getAllUsers(){
        List<User> user = userRepository.findAll();
        return userMapper.toResponseList(user);
    }

    public boolean IsUserNonExisted(Long id){
        return userRepository.existsById(id);
    }

    public void deleteById(Long id) {

        if(!IsUserNonExisted(id)) throw new AppException(ErrorCode.USER_NON_EXISTED);
        userRepository.deleteById(id);
    }

    public UpdatedUserResponse updateUser(Long id, UpdatedUserRequest updatedUserRequest) {
        User user = userRepository.findById(id).map(
                existUser-> {
                    if (updatedUserRequest.getUsername() != null) existUser.setUsername(updatedUserRequest.getUsername());
                    if (updatedUserRequest.getTeam() != null) existUser.setTeam(updatedUserRequest.getTeam());
                    if (updatedUserRequest.getName() != null) existUser.setName(updatedUserRequest.getName());
                    existUser.setActive(updatedUserRequest.isActive());
                    if (updatedUserRequest.getEmail() != null) existUser.setEmail(updatedUserRequest.getEmail());
                    if (updatedUserRequest.getRole() != null) existUser.setRole(updatedUserRequest.getRole());

                    return userRepository.save(existUser);
                }
        ).orElseThrow(() -> new AppException(ErrorCode.USER_NON_EXISTED));
        return userMapper.toUpdatedResponse(user);
    }

    public UserResponse getMyinfo(Authentication authentication){
        String username = authentication.getName();
        User user = userRepository.findUserByUsername(username).orElseThrow(()-> new AppException(ErrorCode.USER_NOT_FOUND));

        return userMapper.toResponse(user);
    }
}
