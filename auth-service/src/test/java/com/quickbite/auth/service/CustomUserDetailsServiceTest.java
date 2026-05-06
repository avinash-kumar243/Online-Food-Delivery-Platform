package com.quickbite.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.quickbite.auth.entity.*;
import com.quickbite.auth.enums.UserStatus;
import com.quickbite.auth.repository.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private DeliveryPartnerRepository deliveryPartnerRepository;
    @Mock private RestaurantOwnerRepository restaurantOwnerRepository;
    @Mock private AdminUserRepository adminUserRepository;
    @Mock private UserStatusSupport userStatusSupport;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Load Admin - Success")
    void loadUser_AdminSuccess() {
        AdminUser admin = new AdminUser();
        admin.setEmail("admin@test.com");
        admin.setPasswordHash("hash");
        admin.setStatus(UserStatus.ACTIVE);
        admin.setIsActive(true);

        when(adminUserRepository.findByEmail(anyString())).thenReturn(Optional.of(admin));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.ACTIVE);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("admin@test.com");

        assertEquals("admin@test.com", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Load Customer - Success with null password check")
    void loadUser_CustomerSuccess() {
        Customer customer = new Customer();
        customer.setEmail("cust@test.com");
        customer.setPasswordHash(null); // Testing the ternary operator for null passwords
        customer.setStatus(UserStatus.ACTIVE);
        customer.setIsActive(true);

        when(adminUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(customerRepository.findByEmail(anyString())).thenReturn(Optional.of(customer));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.ACTIVE);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("cust@test.com");

        assertEquals("", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER")));
    }

    @Test
    @DisplayName("Load Restaurant Owner - Success")
    void loadUser_OwnerSuccess() {
        RestaurantOwner owner = new RestaurantOwner();
        owner.setEmail("owner@test.com");
        owner.setPasswordHash("pass");
        owner.setStatus(UserStatus.ACTIVE);
        owner.setIsActive(true);

        when(adminUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(customerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(restaurantOwnerRepository.findByEmail(anyString())).thenReturn(Optional.of(owner));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.ACTIVE);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("owner@test.com");

        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESTAURANT_OWNER")));
    }

    @Test
    @DisplayName("Load Delivery Partner - Success")
    void loadUser_PartnerSuccess() {
        DeliveryPartner partner = new DeliveryPartner();
        partner.setEmail("driver@test.com");
        partner.setPasswordHash("pass");
        partner.setStatus(UserStatus.ACTIVE);
        partner.setIsActive(true);

        when(adminUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(customerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(restaurantOwnerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(deliveryPartnerRepository.findByEmail(anyString())).thenReturn(Optional.of(partner));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.ACTIVE);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("driver@test.com");

        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DELIVERY_PARTNER")));
    }

    @Test
    @DisplayName("Load User - Throws Exception when not found in any repo")
    void loadUser_NotFound() {
        when(adminUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(customerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(restaurantOwnerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(deliveryPartnerRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                customUserDetailsService.loadUserByUsername("none@test.com")
        );
    }

    @Test
    @DisplayName("EnsureActive - Throws Exception for SUSPENDED account")
    void ensureActive_SuspendedThrowsException() {
        AdminUser admin = new AdminUser();
        admin.setEmail("suspended@test.com");
        admin.setStatus(UserStatus.SUSPENDED);

        when(adminUserRepository.findByEmail(anyString())).thenReturn(Optional.of(admin));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.SUSPENDED);

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class, () ->
                customUserDetailsService.loadUserByUsername("suspended@test.com")
        );
        assertTrue(ex.getMessage().contains("account is not active"));
    }

    @Test
    @DisplayName("EnsureActive - Throws Exception for DELETED account")
    void ensureActive_DeletedThrowsException() {
        Customer customer = new Customer();
        customer.setEmail("deleted@test.com");
        customer.setStatus(UserStatus.DELETED);

        when(adminUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(customerRepository.findByEmail(anyString())).thenReturn(Optional.of(customer));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.DELETED);

        assertThrows(UsernameNotFoundException.class, () ->
                customUserDetailsService.loadUserByUsername("deleted@test.com")
        );
    }
}