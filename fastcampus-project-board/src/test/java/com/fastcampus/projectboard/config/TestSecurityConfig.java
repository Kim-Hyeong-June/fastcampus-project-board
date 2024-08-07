package com.fastcampus.projectboard.config;


import com.fastcampus.projectboard.domain.UserAccount;
import com.fastcampus.projectboard.repository.UserAccountRepository;
import org.mockito.BDDMockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.annotation.BeforeTestMethod;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;

@Import(SecurityConfig.class)
public class TestSecurityConfig {
    @MockBean private UserAccountRepository userAccountRepository;


    @BeforeTestMethod
    public void securitySetup(){
        BDDMockito.given(userAccountRepository.findById(anyString()))
                .willReturn(Optional.of(UserAccount.of(
                        "unoTest" ,
                        "pw" ,
                        "uno-test@email.com",
                        "uno-test",
                        "test-memo"
                )));
    }
}
