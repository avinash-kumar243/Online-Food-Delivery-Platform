package com.quickbite.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import java.util.List;
import java.util.Optional;

import com.quickbite.auth.client.RestaurantServiceClient;
import com.quickbite.auth.exception.IllegalOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.quickbite.auth.entity.AdminUser;
import com.quickbite.auth.entity.Customer;
import com.quickbite.auth.entity.DeliveryPartner;
import com.quickbite.auth.entity.RestaurantOwner;
import com.quickbite.auth.enums.UserRole;
import com.quickbite.auth.enums.UserStatus;
import com.quickbite.auth.repository.AdminUserRepository;
import com.quickbite.auth.repository.CustomerRepository;
import com.quickbite.auth.repository.DeliveryPartnerRepository;
import com.quickbite.auth.repository.RestaurantOwnerRepository;

@ExtendWith(MockitoExtension.class)
class UserAdministrationServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RestaurantOwnerRepository restaurantOwnerRepository;

    @Mock
    private RestaurantServiceClient restaurantServiceClient;

    @Mock
    private DeliveryPartnerRepository deliveryPartnerRepository;

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private UserStatusSupport userStatusSupport;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserAdministrationService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private AdminUser authenticateAdmin(Long adminId, String email) {
        AdminUser actingAdmin = new AdminUser();
        actingAdmin.setAdminId(adminId);
        actingAdmin.setEmail(email);
        when(adminUserRepository.findByEmail(email)).thenReturn(Optional.of(actingAdmin));
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            )
        );
        return actingAdmin;
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        Customer customer = new Customer();
        customer.setCustomerId(1L);
        customer.setEmail("customer@test.com");

        RestaurantOwner owner = new RestaurantOwner();
        owner.setOwnerId(2L);
        owner.setEmail("owner@test.com");

        DeliveryPartner partner = new DeliveryPartner();
        partner.setPartnerId(3L);
        partner.setEmail("partner@test.com");

        AdminUser admin = new AdminUser();
        admin.setAdminId(4L);
        admin.setEmail("admin@test.com");

        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(restaurantOwnerRepository.findAll()).thenReturn(List.of(owner));
        when(deliveryPartnerRepository.findAll()).thenReturn(List.of(partner));
        when(adminUserRepository.findAll()).thenReturn(List.of(admin));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.ACTIVE);

        var result = service.getAllUsers();

        assertEquals(4, result.size());
        verify(customerRepository).findAll();
        verify(restaurantOwnerRepository).findAll();
        verify(deliveryPartnerRepository).findAll();
        verify(adminUserRepository).findAll();
    }

    @Test
    void getAllUsers_WhenNoUsers_ShouldReturnEmptyList() {
        when(customerRepository.findAll()).thenReturn(List.of());
        when(restaurantOwnerRepository.findAll()).thenReturn(List.of());
        when(deliveryPartnerRepository.findAll()).thenReturn(List.of());
        when(adminUserRepository.findAll()).thenReturn(List.of());

        var result = service.getAllUsers();

        assertTrue(result.isEmpty());
    }

    @Test
    void getUsersByRole_Customer_ShouldReturnCustomers() {
        Customer customer = new Customer();
        customer.setCustomerId(1L);
        customer.setEmail("customer@test.com");

        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.ACTIVE);

        var result = service.getUsersByRole(UserRole.CUSTOMER);

        assertEquals(1, result.size());
        verify(customerRepository).findAll();
        verifyNoInteractions(restaurantOwnerRepository, deliveryPartnerRepository, adminUserRepository);
    }

    @Test
    void getUsersByRole_RestaurantOwner_ShouldReturnOwners() {
        RestaurantOwner owner = new RestaurantOwner();
        owner.setOwnerId(2L);
        owner.setEmail("owner@test.com");

        when(restaurantOwnerRepository.findAll()).thenReturn(List.of(owner));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.ACTIVE);

        var result = service.getUsersByRole(UserRole.RESTAURANT_OWNER);

        assertEquals(1, result.size());
        verify(restaurantOwnerRepository).findAll();
    }

    @Test
    void getUsersByRole_DeliveryPartner_ShouldReturnPartners() {
        DeliveryPartner partner = new DeliveryPartner();
        partner.setPartnerId(3L);
        partner.setEmail("partner@test.com");

        when(deliveryPartnerRepository.findAll()).thenReturn(List.of(partner));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.ACTIVE);

        var result = service.getUsersByRole(UserRole.DELIVERY_PARTNER);

        assertEquals(1, result.size());
        verify(deliveryPartnerRepository).findAll();
    }

    @Test
    void getUsersByRole_Admin_ShouldReturnAdmins() {
        AdminUser admin = new AdminUser();
        admin.setAdminId(4L);
        admin.setEmail("admin@test.com");

        when(adminUserRepository.findAll()).thenReturn(List.of(admin));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.ACTIVE);

        var result = service.getUsersByRole(UserRole.ADMIN);

        assertEquals(1, result.size());
        verify(adminUserRepository).findAll();
    }

    @Test
    void suspendUser_Customer_ShouldSuspendUser() {
        authenticateAdmin(99L, "admin@test.com");
        Customer customer = new Customer();
        customer.setCustomerId(1L);
        customer.setEmail("customer@test.com");

        when(customerRepository.findByCustomerId(1L)).thenReturn(Optional.of(customer));

        service.suspendUser(UserRole.CUSTOMER, 1L);

        assertEquals(UserStatus.SUSPENDED, customer.getStatus());
        verify(customerRepository).save(customer);
        verify(emailService).sendUserSuspendedEmail(eq(1L), any(), any(), eq("CUSTOMER"));
    }

    @Test
    void suspendUser_RestaurantOwner_ShouldSuspendUser() {
        authenticateAdmin(99L, "admin@test.com");
        RestaurantOwner owner = new RestaurantOwner();
        owner.setOwnerId(2L);
        owner.setEmail("owner@test.com");

        when(restaurantOwnerRepository.findByOwnerId(2L)).thenReturn(Optional.of(owner));

        service.suspendUser(UserRole.RESTAURANT_OWNER, 2L);

        assertEquals(UserStatus.SUSPENDED, owner.getStatus());
        verify(restaurantOwnerRepository).save(owner);
        verify(emailService).sendUserSuspendedEmail(eq(2L), any(), any(), eq("RESTAURANT_OWNER"));
    }

    @Test
    void suspendUser_DeliveryPartner_ShouldSuspendUser() {
        authenticateAdmin(99L, "admin@test.com");
        DeliveryPartner partner = new DeliveryPartner();
        partner.setPartnerId(3L);
        partner.setEmail("partner@test.com");

        when(deliveryPartnerRepository.findByPartnerId(3L)).thenReturn(Optional.of(partner));

        service.suspendUser(UserRole.DELIVERY_PARTNER, 3L);

        assertEquals(UserStatus.SUSPENDED, partner.getStatus());
        verify(deliveryPartnerRepository).save(partner);
        verify(emailService).sendUserSuspendedEmail(eq(3L), any(), any(), eq("DELIVERY_PARTNER"));
    }

    @Test
    void suspendUser_Admin_ShouldSuspendUser() {
        authenticateAdmin(99L, "admin@test.com");
        AdminUser admin = new AdminUser();
        admin.setAdminId(4L);
        admin.setEmail("admin@test.com");

        when(adminUserRepository.findByAdminId(4L)).thenReturn(Optional.of(admin));

        service.suspendUser(UserRole.ADMIN, 4L);

        assertEquals(UserStatus.SUSPENDED, admin.getStatus());
        verify(adminUserRepository).save(admin);
        verify(emailService).sendUserSuspendedEmail(eq(4L), any(), any(), eq("ADMIN"));
    }

    @Test
    void suspendUser_CustomerNotFound_ShouldThrowException() {
        authenticateAdmin(99L, "admin@test.com");
        when(customerRepository.findByCustomerId(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.suspendUser(UserRole.CUSTOMER, 1L));

        verify(customerRepository, never()).save(any());
        verify(emailService, never()).sendUserSuspendedEmail(anyLong(), any(), any(), anyString());
    }

    @Test
    void suspendUser_RestaurantOwnerNotFound_ShouldThrowException() {
        authenticateAdmin(99L, "admin@test.com");
        when(restaurantOwnerRepository.findByOwnerId(2L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.suspendUser(UserRole.RESTAURANT_OWNER, 2L));

        verify(restaurantOwnerRepository, never()).save(any());
    }

    @Test
    void suspendUser_DeliveryPartnerNotFound_ShouldThrowException() {
        authenticateAdmin(99L, "admin@test.com");
        when(deliveryPartnerRepository.findByPartnerId(3L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.suspendUser(UserRole.DELIVERY_PARTNER, 3L));

        verify(deliveryPartnerRepository, never()).save(any());
    }

    @Test
    void suspendUser_AdminNotFound_ShouldThrowException() {
        authenticateAdmin(99L, "admin@test.com");
        when(adminUserRepository.findByAdminId(4L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.suspendUser(UserRole.ADMIN, 4L));

        verify(adminUserRepository, never()).save(any());
    }

    @Test
    void reactivateUser_Customer_ShouldReactivateUser() {
        authenticateAdmin(99L, "admin@test.com");
        Customer customer = new Customer();
        customer.setCustomerId(1L);
        customer.setEmail("customer@test.com");

        when(customerRepository.findByCustomerId(1L)).thenReturn(Optional.of(customer));

        service.reactivateUser(UserRole.CUSTOMER, 1L);

        assertEquals(UserStatus.ACTIVE, customer.getStatus());
        verify(customerRepository).save(customer);
        verify(emailService).sendUserReactivatedEmail(eq(1L), any(), any(), eq("CUSTOMER"));
    }

    @Test
    void reactivateUser_RestaurantOwner_ShouldReactivateUser() {
        authenticateAdmin(99L, "admin@test.com");
        RestaurantOwner owner = new RestaurantOwner();
        owner.setOwnerId(2L);
        owner.setEmail("owner@test.com");

        when(restaurantOwnerRepository.findByOwnerId(2L)).thenReturn(Optional.of(owner));

        service.reactivateUser(UserRole.RESTAURANT_OWNER, 2L);

        assertEquals(UserStatus.ACTIVE, owner.getStatus());
        verify(restaurantOwnerRepository).save(owner);
        verify(emailService).sendUserReactivatedEmail(eq(2L), any(), any(), eq("RESTAURANT_OWNER"));
    }

    @Test
    void reactivateUser_DeliveryPartner_ShouldReactivateUser() {
        authenticateAdmin(99L, "admin@test.com");
        DeliveryPartner partner = new DeliveryPartner();
        partner.setPartnerId(3L);
        partner.setEmail("partner@test.com");

        when(deliveryPartnerRepository.findByPartnerId(3L)).thenReturn(Optional.of(partner));

        service.reactivateUser(UserRole.DELIVERY_PARTNER, 3L);

        assertEquals(UserStatus.ACTIVE, partner.getStatus());
        verify(deliveryPartnerRepository).save(partner);
        verify(emailService).sendUserReactivatedEmail(eq(3L), any(), any(), eq("DELIVERY_PARTNER"));
    }

    @Test
    void reactivateUser_Admin_ShouldReactivateUser() {
        authenticateAdmin(99L, "admin@test.com");
        AdminUser admin = new AdminUser();
        admin.setAdminId(4L);
        admin.setEmail("admin@test.com");

        when(adminUserRepository.findByAdminId(4L)).thenReturn(Optional.of(admin));

        service.reactivateUser(UserRole.ADMIN, 4L);

        assertEquals(UserStatus.ACTIVE, admin.getStatus());
        verify(adminUserRepository).save(admin);
        verify(emailService).sendUserReactivatedEmail(eq(4L), any(), any(), eq("ADMIN"));
    }

    @Test
    void reactivateUser_CustomerNotFound_ShouldThrowException() {
        authenticateAdmin(99L, "admin@test.com");
        when(customerRepository.findByCustomerId(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.reactivateUser(UserRole.CUSTOMER, 1L));

        verify(customerRepository, never()).save(any());
    }

    @Test
    void reactivateUser_RestaurantOwnerNotFound_ShouldThrowException() {
        authenticateAdmin(99L, "admin@test.com");
        when(restaurantOwnerRepository.findByOwnerId(2L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.reactivateUser(UserRole.RESTAURANT_OWNER, 2L));

        verify(restaurantOwnerRepository, never()).save(any());
    }

    @Test
    void reactivateUser_DeliveryPartnerNotFound_ShouldThrowException() {
        authenticateAdmin(99L, "admin@test.com");
        when(deliveryPartnerRepository.findByPartnerId(3L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.reactivateUser(UserRole.DELIVERY_PARTNER, 3L));

        verify(deliveryPartnerRepository, never()).save(any());
    }

    @Test
    void reactivateUser_AdminNotFound_ShouldThrowException() {
        authenticateAdmin(99L, "admin@test.com");
        when(adminUserRepository.findByAdminId(4L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.reactivateUser(UserRole.ADMIN, 4L));

        verify(adminUserRepository, never()).save(any());
    }

    @Test
    void deleteUser_Customer_ShouldDeleteUser() {
        authenticateAdmin(99L, "admin@test.com");
        Customer customer = new Customer();
        customer.setCustomerId(1L);
        customer.setEmail("customer@test.com");

        when(customerRepository.findByCustomerId(1L)).thenReturn(Optional.of(customer));

        service.deleteUser(UserRole.CUSTOMER, 1L);

        verify(customerRepository).delete(customer);
        verify(emailService).sendUserDeletedEmail(eq(1L), any(), any(), eq("CUSTOMER"));
    }

    @Test
    void deleteUser_RestaurantOwner_ShouldDeleteUser() {
        authenticateAdmin(99L, "admin@test.com");
        RestaurantOwner owner = new RestaurantOwner();
        owner.setOwnerId(2L);
        owner.setEmail("owner@test.com");

        when(restaurantOwnerRepository.findByOwnerId(2L)).thenReturn(Optional.of(owner));

        service.deleteUser(UserRole.RESTAURANT_OWNER, 2L);

        verify(restaurantOwnerRepository).delete(owner);
        verify(emailService).sendUserDeletedEmail(eq(2L), any(), any(), eq("RESTAURANT_OWNER"));
    }

    @Test
    void deleteUser_DeliveryPartner_ShouldDeleteUser() {
        authenticateAdmin(99L, "admin@test.com");
        DeliveryPartner partner = new DeliveryPartner();
        partner.setPartnerId(3L);
        partner.setEmail("partner@test.com");

        when(deliveryPartnerRepository.findByPartnerId(3L)).thenReturn(Optional.of(partner));

        service.deleteUser(UserRole.DELIVERY_PARTNER, 3L);

        verify(deliveryPartnerRepository).delete(partner);
        verify(emailService).sendUserDeletedEmail(eq(3L), any(), any(), eq("DELIVERY_PARTNER"));
    }

    @Test
    void deleteUser_Admin_ShouldDeleteUser() {
        authenticateAdmin(99L, "admin@test.com");
        AdminUser admin = new AdminUser();
        admin.setAdminId(4L);
        admin.setEmail("admin@test.com");

        when(adminUserRepository.findByAdminId(4L)).thenReturn(Optional.of(admin));

        service.deleteUser(UserRole.ADMIN, 4L);

        verify(adminUserRepository).delete(admin);
        verify(emailService).sendUserDeletedEmail(eq(4L), any(), any(), eq("ADMIN"));
    }

    @Test
    void deleteUser_CustomerNotFound_ShouldThrowException() {
        authenticateAdmin(99L, "admin@test.com");
        when(customerRepository.findByCustomerId(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.deleteUser(UserRole.CUSTOMER, 1L));

        verify(customerRepository, never()).delete(any());
    }

    @Test
    void deleteUser_RestaurantOwnerNotFound_ShouldThrowException() {
        authenticateAdmin(99L, "admin@test.com");
        when(restaurantOwnerRepository.findByOwnerId(2L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.deleteUser(UserRole.RESTAURANT_OWNER, 2L));

        verify(restaurantOwnerRepository, never()).delete(any());
    }

    @Test
    void deleteUser_DeliveryPartnerNotFound_ShouldThrowException() {
        authenticateAdmin(99L, "admin@test.com");
        when(deliveryPartnerRepository.findByPartnerId(3L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.deleteUser(UserRole.DELIVERY_PARTNER, 3L));

        verify(deliveryPartnerRepository, never()).delete(any());
    }

    @Test
    void deleteUser_AdminNotFound_ShouldThrowException() {
        authenticateAdmin(99L, "admin@test.com");
        when(adminUserRepository.findByAdminId(4L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.deleteUser(UserRole.ADMIN, 4L));

        verify(adminUserRepository, never()).delete(any());
    }

    @Test
    void getUserSummary_Customer_ShouldReturnSummary() {
        Customer customer = new Customer();
        customer.setCustomerId(1L);
        customer.setEmail("customer@test.com");

        when(customerRepository.findByCustomerId(1L)).thenReturn(Optional.of(customer));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.ACTIVE);

        var result = service.getUserSummary(UserRole.CUSTOMER, 1L);

        assertNotNull(result);
    }

    @Test
    void getUserSummary_RestaurantOwner_ShouldReturnSummary() {
        RestaurantOwner owner = new RestaurantOwner();
        owner.setOwnerId(2L);
        owner.setEmail("owner@test.com");

        when(restaurantOwnerRepository.findByOwnerId(2L)).thenReturn(Optional.of(owner));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.ACTIVE);

        var result = service.getUserSummary(UserRole.RESTAURANT_OWNER, 2L);

        assertNotNull(result);
    }

    @Test
    void getUserSummary_DeliveryPartner_ShouldReturnSummary() {
        DeliveryPartner partner = new DeliveryPartner();
        partner.setPartnerId(3L);
        partner.setEmail("partner@test.com");

        when(deliveryPartnerRepository.findByPartnerId(3L)).thenReturn(Optional.of(partner));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.ACTIVE);

        var result = service.getUserSummary(UserRole.DELIVERY_PARTNER, 3L);

        assertNotNull(result);
    }

    @Test
    void getUserSummary_Admin_ShouldReturnSummary() {
        AdminUser admin = new AdminUser();
        admin.setAdminId(4L);
        admin.setEmail("admin@test.com");

        when(adminUserRepository.findByAdminId(4L)).thenReturn(Optional.of(admin));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.ACTIVE);

        var result = service.getUserSummary(UserRole.ADMIN, 4L);

        assertNotNull(result);
    }

    @Test
    void getUserSummary_CustomerNotFound_ShouldThrowException() {
        when(customerRepository.findByCustomerId(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.getUserSummary(UserRole.CUSTOMER, 1L));
    }

    @Test
    void getUserSummary_RestaurantOwnerNotFound_ShouldThrowException() {
        when(restaurantOwnerRepository.findByOwnerId(2L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.getUserSummary(UserRole.RESTAURANT_OWNER, 2L));
    }

    @Test
    void getUserSummary_DeliveryPartnerNotFound_ShouldThrowException() {
        when(deliveryPartnerRepository.findByPartnerId(3L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.getUserSummary(UserRole.DELIVERY_PARTNER, 3L));
    }

    @Test
    void getUserSummary_AdminNotFound_ShouldThrowException() {
        when(adminUserRepository.findByAdminId(4L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.getUserSummary(UserRole.ADMIN, 4L));
    }

    @Test
    void getUsersByRoleForInternal_Customer_ShouldReturnUsers() {
        Customer customer = new Customer();
        customer.setCustomerId(1L);
        customer.setEmail("customer@test.com");

        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.ACTIVE);

        var result = service.getUsersByRoleForInternal(UserRole.CUSTOMER);

        assertEquals(1, result.size());
    }

    @Test
    void getUsersByRoleForInternal_RestaurantOwner_ShouldReturnUsers() {
        RestaurantOwner owner = new RestaurantOwner();
        owner.setOwnerId(2L);
        owner.setEmail("owner@test.com");

        when(restaurantOwnerRepository.findAll()).thenReturn(List.of(owner));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.ACTIVE);

        var result = service.getUsersByRoleForInternal(UserRole.RESTAURANT_OWNER);

        assertEquals(1, result.size());
    }

    @Test
    void getUsersByRoleForInternal_DeliveryPartner_ShouldReturnUsers() {
        DeliveryPartner partner = new DeliveryPartner();
        partner.setPartnerId(3L);
        partner.setEmail("partner@test.com");

        when(deliveryPartnerRepository.findAll()).thenReturn(List.of(partner));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.ACTIVE);

        var result = service.getUsersByRoleForInternal(UserRole.DELIVERY_PARTNER);

        assertEquals(1, result.size());
    }

    @Test
    void getUsersByRoleForInternal_Admin_ShouldReturnUsers() {
        AdminUser admin = new AdminUser();
        admin.setAdminId(4L);
        admin.setEmail("admin@test.com");

        when(adminUserRepository.findAll()).thenReturn(List.of(admin));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.ACTIVE);

        var result = service.getUsersByRoleForInternal(UserRole.ADMIN);

        assertEquals(1, result.size());
    }

    @Test
    void suspendUser_AdminSelf_ShouldThrowForbiddenAndNotSave() {
        authenticateAdmin(4L, "admin@test.com");
        AdminUser admin = new AdminUser();
        admin.setAdminId(4L);
        admin.setEmail("admin@test.com");
        when(adminUserRepository.findByAdminId(4L)).thenReturn(Optional.of(admin));

        assertThrows(IllegalOperationException.class, () -> service.suspendUser(UserRole.ADMIN, 4L));

        verify(adminUserRepository, never()).save(any(AdminUser.class));
        verify(emailService, never()).sendUserSuspendedEmail(anyLong(), any(), any(), anyString());
    }

    @Test
    void reactivateUser_AdminSelf_ShouldThrowForbiddenAndNotSave() {
        authenticateAdmin(4L, "admin@test.com");
        AdminUser admin = new AdminUser();
        admin.setAdminId(4L);
        admin.setEmail("admin@test.com");
        when(adminUserRepository.findByAdminId(4L)).thenReturn(Optional.of(admin));

        assertThrows(IllegalOperationException.class, () -> service.reactivateUser(UserRole.ADMIN, 4L));

        verify(adminUserRepository, never()).save(any(AdminUser.class));
        verify(emailService, never()).sendUserReactivatedEmail(anyLong(), any(), any(), anyString());
    }

    @Test
    void deleteUser_AdminSelf_ShouldThrowForbiddenAndNotDelete() {
        authenticateAdmin(4L, "admin@test.com");
        AdminUser admin = new AdminUser();
        admin.setAdminId(4L);
        admin.setEmail("admin@test.com");
        when(adminUserRepository.findByAdminId(4L)).thenReturn(Optional.of(admin));

        assertThrows(IllegalOperationException.class, () -> service.deleteUser(UserRole.ADMIN, 4L));

        verify(adminUserRepository, never()).delete(any(AdminUser.class));
        verify(emailService, never()).sendUserDeletedEmail(anyLong(), any(), any(), anyString());
    }
}
