package com.fintech.ewallet.admin.application;

import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchUsersUseCase {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<User> execute(String query) {
        return userRepository.searchUsers(query);
    }
}
