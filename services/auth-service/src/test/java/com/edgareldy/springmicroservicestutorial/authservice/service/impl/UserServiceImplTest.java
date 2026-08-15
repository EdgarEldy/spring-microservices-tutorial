package com.edgareldy.springmicroservicestutorial.authservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springmicroservicestutorial.authservice.dto.auth.RegisterRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.user.UpdateProfileRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.user.UserResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Permission;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Role;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;
import com.edgareldy.springmicroservicestutorial.authservice.mapper.UserMapperImpl;
import com.edgareldy.springmicroservicestutorial.authservice.repository.UserRepository;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.BusinessRuleException;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pure Mockito unit tests for {@link UserServiceImpl}: no Spring context, no
 * database, {@link UserRepository} and {@link PasswordEncoder} are mocked so
 * only this class' own branching is exercised (persistence itself is already
 * covered by {@code UserRepositoryTest}, backed by Testcontainers PostgreSQL).
 * The MapStruct-generated {@link UserMapperImpl} is instantiated for real
 * (not mocked) and wired manually rather than via {@code @InjectMocks}:
 * mocking a trivial generated mapper would add nothing and would require
 * stubbing every field of every {@code toResponse} call, so this class is
 * exercised as it will actually run in production. {@code UserMapperImpl}
 * declares its {@link PasswordEncoder} as an {@code @Autowired protected}
 * field (MapStruct's field-injection style for abstract-class mappers), so
 * it is wired with the same mock via {@link ReflectionTestUtils#setField}
 * rather than a constructor argument.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        UserMapperImpl userMapper = new UserMapperImpl();
        ReflectionTestUtils.setField(userMapper, "passwordEncoder", passwordEncoder);
        userService = new UserServiceImpl(userRepository, passwordEncoder, userMapper);
    }

    private final RegisterRequest request =
            new RegisterRequest("Ada", "Lovelace", "ada@example.com", "raw-password");

    @Test
    void createUser_encodesPasswordAndPersistsDisabledNonLockedUser() {
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.createUser(request);

        assertThat(created.getPassword()).isEqualTo("encoded-password");
        assertThat(created.isEnabled()).isFalse();
        assertThat(created.isAccountLocked()).isFalse();
        assertThat(created.getEmail()).isEqualTo("ada@example.com");

        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void createUser_emailAlreadyInUse_throwsBusinessRuleExceptionAndNeverSaves() {
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> userService.createUser(request));

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void findByEmail_found_returnsUser() {
        User user = User.builder().id(1L).email("ada@example.com").build();
        when(userRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Optional.of(user));

        assertThat(userService.findByEmail("ada@example.com")).isEqualTo(user);
    }

    @Test
    void findByEmail_notFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> userService.findByEmail("nobody@example.com"));
    }

    @Test
    void findById_found_returnsUser() {
        User user = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(userService.findById(1L)).isEqualTo(user);
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> userService.findById(99L));
    }

    @Test
    void enableAccount_setsEnabledTrueAndSaves() {
        User user = User.builder().id(1L).enabled(false).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.enableAccount(1L);

        assertThat(user.isEnabled()).isTrue();
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void updatePassword_encodesAndSavesNewPassword() {
        User user = User.builder().id(1L).password("old-encoded").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-raw-password")).thenReturn("new-encoded");
        when(userRepository.save(user)).thenReturn(user);

        userService.updatePassword(1L, "new-raw-password");

        assertThat(user.getPassword()).isEqualTo("new-encoded");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_updatesNamesAndSaves() {
        User user = User.builder().id(1L).firstName("Ada").lastName("Lovelace").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.updateProfile(1L, new UpdateProfileRequest("Grace", "Hopper"));

        assertThat(updated.getFirstName()).isEqualTo("Grace");
        assertThat(updated.getLastName()).isEqualTo("Hopper");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_notFound_throwsResourceNotFoundExceptionAndNeverSaves() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> userService.updateProfile(99L, new UpdateProfileRequest("Grace", "Hopper")));

        verify(userRepository, never()).save(any());
    }

    @Test
    void toResponse_mapsFieldsAndFlattensRoleNames() {
        Role adminRole = Role.builder().id(1L).roleName("ADMIN").permissions(Set.of()).build();
        Role userRole = Role.builder().id(2L).roleName("USER").permissions(Set.of()).build();
        User user = User.builder()
                .id(7L)
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada@example.com")
                .enabled(true)
                .accountLocked(false)
                .roles(Set.of(adminRole, userRole))
                .build();

        UserResponse response = userService.toResponse(user);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.firstName()).isEqualTo("Ada");
        assertThat(response.lastName()).isEqualTo("Lovelace");
        assertThat(response.email()).isEqualTo("ada@example.com");
        assertThat(response.enabled()).isTrue();
        assertThat(response.accountLocked()).isFalse();
        assertThat(response.roles()).containsExactlyInAnyOrder("ADMIN", "USER");
    }

    @Test
    void existsByEmail_delegatesToRepository() {
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase(eq("nobody@example.com"))).thenReturn(false);

        assertThat(userService.existsByEmail("ada@example.com")).isTrue();
        assertThat(userService.existsByEmail("nobody@example.com")).isFalse();
    }
}
