package com.bikestore.api.controller;

import com.bikestore.api.dto.response.UserResponse;
import com.bikestore.api.entity.User;
import com.bikestore.api.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController")
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController controller;

    private static final User DUMMY_USER = User.builder().build();

    @Nested
    @DisplayName("GET /api/v1/users/me (getMyProfile)")
    class GetMyProfile {

        @Test
        @DisplayName("returns 200 OK with the UserResponse from service")
        void returns200_withUserResponse() {
            UserResponse expected = new UserResponse(
                    1L, "user@example.com", "John", "Doe",
                    "CUSTOMER", true, true, "LOCAL", "+5491122334455"
            );
            when(userService.getMyProfile(DUMMY_USER)).thenReturn(expected);

            ResponseEntity<UserResponse> response = controller.getMyProfile(DUMMY_USER);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(expected, response.getBody());
            verify(userService).getMyProfile(DUMMY_USER);
        }

        @Test
        @DisplayName("delegates to userService.getMyProfile with the authenticated user")
        void delegatesToService_withAuthenticatedUser() {
            User specificUser = User.builder().build();
            UserResponse anyResponse = new UserResponse(
                    2L, "other@example.com", "Jane", "Doe",
                    "CUSTOMER", true, false, "GOOGLE", null
            );
            when(userService.getMyProfile(specificUser)).thenReturn(anyResponse);

            controller.getMyProfile(specificUser);

            verify(userService, times(1)).getMyProfile(specificUser);
            verifyNoMoreInteractions(userService);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/users/me (deactivateMyAccount)")
    class DeactivateMyAccount {

        @Test
        @DisplayName("returns 204 No Content and delegates to service")
        void returns204_delegatesToService() {
            doNothing().when(userService).deactivateUser(DUMMY_USER);

            ResponseEntity<Void> response = controller.deactivateMyAccount(DUMMY_USER);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            assertNull(response.getBody());
            verify(userService).deactivateUser(DUMMY_USER);
        }
    }
}
