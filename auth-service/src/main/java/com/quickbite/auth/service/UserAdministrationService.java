package com.quickbite.auth.service;

import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.auth.client.RestaurantServiceClient;
import com.quickbite.auth.dto.InternalUserSummaryDto;
import com.quickbite.auth.dto.PlatformUserDto;
import com.quickbite.auth.entity.AdminUser;
import com.quickbite.auth.entity.Customer;
import com.quickbite.auth.entity.DeliveryPartner;
import com.quickbite.auth.entity.RestaurantOwner;
import com.quickbite.auth.enums.UserRole;
import com.quickbite.auth.enums.UserStatus;
import com.quickbite.auth.exception.IllegalOperationException;
import com.quickbite.auth.repository.AdminUserRepository;
import com.quickbite.auth.repository.CustomerRepository;
import com.quickbite.auth.repository.DeliveryPartnerRepository;
import com.quickbite.auth.repository.RestaurantOwnerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAdministrationService {

    private final CustomerRepository customerRepository;
    private final RestaurantOwnerRepository restaurantOwnerRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final AdminUserRepository adminUserRepository;
    private final UserStatusSupport userStatusSupport;
    private final EmailService emailService;
    private final RestaurantServiceClient restaurantServiceClient;

    @Transactional(readOnly = true)
    public List<PlatformUserDto> getAllUsers() {
        return List.of(
            customerRepository.findAll().stream().map(this::toCustomerDto).toList(),
            restaurantOwnerRepository.findAll().stream().map(this::toRestaurantOwnerDto).toList(),
            deliveryPartnerRepository.findAll().stream().map(this::toDeliveryPartnerDto).toList(),
            adminUserRepository.findAll().stream().map(this::toAdminDto).toList()
        ).stream().flatMap(List::stream).toList();
    }

    @Transactional(readOnly = true)
    public List<PlatformUserDto> getUsersByRole(UserRole role) {
        return switch (role) {
            case CUSTOMER -> customerRepository.findAll().stream().map(this::toCustomerDto).toList();
            case RESTAURANT_OWNER -> restaurantOwnerRepository.findAll().stream().map(this::toRestaurantOwnerDto).toList();
            case DELIVERY_PARTNER -> deliveryPartnerRepository.findAll().stream().map(this::toDeliveryPartnerDto).toList();
            case ADMIN -> adminUserRepository.findAll().stream().map(this::toAdminDto).toList();
        };
    }

    @Transactional(readOnly = true)
    public InternalUserSummaryDto getUserSummary(UserRole role, Long userId) {
        return switch (role) {
            case CUSTOMER -> toInternal(toCustomerDto(customerRepository.findByCustomerId(userId)
                .orElseThrow(() -> new RuntimeException("Customer not found"))));
            case RESTAURANT_OWNER -> toInternal(toRestaurantOwnerDto(restaurantOwnerRepository.findByOwnerId(userId)
                .orElseThrow(() -> new RuntimeException("Restaurant owner not found"))));
            case DELIVERY_PARTNER -> toInternal(toDeliveryPartnerDto(deliveryPartnerRepository.findByPartnerId(userId)
                .orElseThrow(() -> new RuntimeException("Delivery partner not found"))));
            case ADMIN -> toInternal(toAdminDto(adminUserRepository.findByAdminId(userId)
                .orElseThrow(() -> new RuntimeException("Admin not found"))));
        };
    }

    @Transactional(readOnly = true)
    public List<InternalUserSummaryDto> getUsersByRoleForInternal(UserRole role) {
        return getUsersByRole(role).stream().map(this::toInternal).toList();
    }

    @Transactional
    public void suspendUser(UserRole role, Long userId) {
        AdminUser actingAdmin = getAuthenticatedAdmin();
        assertAdminCanModifyTarget(role, userId, actingAdmin);
        switch (role) {
            case CUSTOMER -> updateCustomerStatus(userId, UserStatus.SUSPENDED, actingAdmin.getAdminId());
            case RESTAURANT_OWNER -> updateRestaurantOwnerStatus(userId, UserStatus.SUSPENDED, actingAdmin.getAdminId());
            case DELIVERY_PARTNER -> updateDeliveryPartnerStatus(userId, UserStatus.SUSPENDED, actingAdmin.getAdminId());
            case ADMIN -> updateAdminStatus(userId, UserStatus.SUSPENDED, actingAdmin.getAdminId());
        }
    }

    @Transactional
    public void reactivateUser(UserRole role, Long userId) {
        AdminUser actingAdmin = getAuthenticatedAdmin();
        assertAdminCanModifyTarget(role, userId, actingAdmin);
        switch (role) {
            case CUSTOMER -> updateCustomerStatus(userId, UserStatus.ACTIVE, actingAdmin.getAdminId());
            case RESTAURANT_OWNER -> updateRestaurantOwnerStatus(userId, UserStatus.ACTIVE, actingAdmin.getAdminId());
            case DELIVERY_PARTNER -> updateDeliveryPartnerStatus(userId, UserStatus.ACTIVE, actingAdmin.getAdminId());
            case ADMIN -> updateAdminStatus(userId, UserStatus.ACTIVE, actingAdmin.getAdminId());
        }
    }

    @Transactional
    public void deleteUser(UserRole role, Long userId) {
        AdminUser actingAdmin = getAuthenticatedAdmin();
        assertAdminCanModifyTarget(role, userId, actingAdmin);
        switch (role) {
            case CUSTOMER -> deleteCustomer(userId, actingAdmin.getAdminId());
            case RESTAURANT_OWNER -> deleteRestaurantOwner(userId, actingAdmin.getAdminId());
            case DELIVERY_PARTNER -> deleteDeliveryPartner(userId, actingAdmin.getAdminId());
            case ADMIN -> deleteAdmin(userId, actingAdmin.getAdminId());
        }
    }

    private void deleteCustomer(Long userId, Long adminId) {
        Customer customer = customerRepository.findByCustomerId(userId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        log.info("Admin {} deleted customer {}", adminId, customer.getCustomerId());
        emailService.sendUserDeletedEmail(
            customer.getCustomerId(),
            customer.getFullName(),
            customer.getEmail(),
            UserRole.CUSTOMER.name()
        );
        customerRepository.delete(customer);
    }

    private void deleteRestaurantOwner(Long userId, Long adminId) {
        RestaurantOwner owner = restaurantOwnerRepository.findByOwnerId(userId)
                .orElseThrow(() -> new RuntimeException("Restaurant owner not found"));
        restaurantServiceClient.deleteRestaurantsByOwnerId(owner.getOwnerId());
        log.info("Admin {} deleted restaurant owner {}", adminId, owner.getOwnerId());
        emailService.sendUserDeletedEmail(
            owner.getOwnerId(),
            owner.getFullName(),
            owner.getEmail(),
            UserRole.RESTAURANT_OWNER.name()
        );
        restaurantOwnerRepository.delete(owner);
    }

    private void deleteDeliveryPartner(Long userId, Long adminId) {
        DeliveryPartner partner = deliveryPartnerRepository.findByPartnerId(userId)
                .orElseThrow(() -> new RuntimeException("Delivery partner not found"));
        log.info("Admin {} deleted delivery partner {}", adminId, partner.getPartnerId());
        emailService.sendUserDeletedEmail(
            partner.getPartnerId(),
            partner.getFullName(),
            partner.getEmail(),
            UserRole.DELIVERY_PARTNER.name()
        );
        deliveryPartnerRepository.delete(partner);
    }

    private void deleteAdmin(Long userId, Long adminId) {
        AdminUser admin = adminUserRepository.findByAdminId(userId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        log.info("Admin {} deleted admin {}", adminId, admin.getAdminId());
        emailService.sendUserDeletedEmail(
            admin.getAdminId(),
            admin.getFullName(),
            admin.getEmail(),
            UserRole.ADMIN.name()
        );
        adminUserRepository.delete(admin);
    }

    private void updateCustomerStatus(Long userId, UserStatus status, Long adminId) {
        Customer customer = customerRepository.findByCustomerId(userId)
            .orElseThrow(() -> new RuntimeException("Customer not found"));
        customer.setStatus(status);
        customer.setIsActive(status == UserStatus.ACTIVE);
        customerRepository.save(customer);
        log.info("Admin {} changed customer {} status to {}", adminId, customer.getCustomerId(), status);
        if (status == UserStatus.SUSPENDED) {
            emailService.sendUserSuspendedEmail(
                customer.getCustomerId(),
                customer.getFullName(),
                customer.getEmail(),
                UserRole.CUSTOMER.name()
            );
        } else if (status == UserStatus.ACTIVE) {
            emailService.sendUserReactivatedEmail(
                customer.getCustomerId(),
                customer.getFullName(),
                customer.getEmail(),
                UserRole.CUSTOMER.name()
            );
        }
    }

    private void updateRestaurantOwnerStatus(Long userId, UserStatus status, Long adminId) {
        RestaurantOwner owner = restaurantOwnerRepository.findByOwnerId(userId)
            .orElseThrow(() -> new RuntimeException("Restaurant owner not found"));
        owner.setStatus(status);
        owner.setIsActive(status == UserStatus.ACTIVE);
        restaurantOwnerRepository.save(owner);
        log.info("Admin {} changed restaurant owner {} status to {}", adminId, owner.getOwnerId(), status);
        if (status == UserStatus.SUSPENDED) {
            emailService.sendUserSuspendedEmail(
                owner.getOwnerId(),
                owner.getFullName(),
                owner.getEmail(),
                UserRole.RESTAURANT_OWNER.name()
            );
        } else if (status == UserStatus.ACTIVE) {
            emailService.sendUserReactivatedEmail(
                owner.getOwnerId(),
                owner.getFullName(),
                owner.getEmail(),
                UserRole.RESTAURANT_OWNER.name()
            );
        }
    }

    private void updateDeliveryPartnerStatus(Long userId, UserStatus status, Long adminId) {
        DeliveryPartner partner = deliveryPartnerRepository.findByPartnerId(userId)
            .orElseThrow(() -> new RuntimeException("Delivery partner not found"));
        partner.setStatus(status);
        partner.setIsActive(status == UserStatus.ACTIVE);
        deliveryPartnerRepository.save(partner);
        log.info("Admin {} changed delivery partner {} status to {}", adminId, partner.getPartnerId(), status);
        if (status == UserStatus.SUSPENDED) {
            emailService.sendUserSuspendedEmail(
                partner.getPartnerId(),
                partner.getFullName(),
                partner.getEmail(),
                UserRole.DELIVERY_PARTNER.name()
            );
        } else if (status == UserStatus.ACTIVE) {
            emailService.sendUserReactivatedEmail(
                partner.getPartnerId(),
                partner.getFullName(),
                partner.getEmail(),
                UserRole.DELIVERY_PARTNER.name()
            );
        }
    }

    private void updateAdminStatus(Long userId, UserStatus status, Long adminId) {
        AdminUser admin = adminUserRepository.findByAdminId(userId)
            .orElseThrow(() -> new RuntimeException("Admin not found"));
        admin.setStatus(status);
        admin.setIsActive(status == UserStatus.ACTIVE);
        adminUserRepository.save(admin);
        log.info("Admin {} changed admin {} status to {}", adminId, admin.getAdminId(), status);
        if (status == UserStatus.SUSPENDED) {
            emailService.sendUserSuspendedEmail(
                admin.getAdminId(),
                admin.getFullName(),
                admin.getEmail(),
                UserRole.ADMIN.name()
            );
        } else if (status == UserStatus.ACTIVE) {
            emailService.sendUserReactivatedEmail(
                admin.getAdminId(),
                admin.getFullName(),
                admin.getEmail(),
                UserRole.ADMIN.name()
            );
        }
    }

    private void assertAdminCanModifyTarget(UserRole role, Long userId, AdminUser actingAdmin) {
        if (role != UserRole.ADMIN) {
            return;
        }

        AdminUser targetAdmin = adminUserRepository.findByAdminId(userId)
            .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (actingAdmin.getAdminId().equals(targetAdmin.getAdminId())) {
            log.warn("Admin attempted self-account modification. adminId={}", actingAdmin.getAdminId());
            throw new IllegalOperationException("You cannot modify your own admin account");
        }
    }

    private AdminUser getAuthenticatedAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("Authenticated admin not found");
        }

        String email = authentication.getName();
        return adminUserRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Authenticated admin not found"));
    }

    private PlatformUserDto toCustomerDto(Customer customer) {
        return new PlatformUserDto(
            customer.getCustomerId(),
            customer.getFullName(),
            customer.getEmail(),
            customer.getPhone(),
            UserRole.CUSTOMER.name(),
            userStatusSupport.resolve(customer.getStatus(), customer.getIsActive()).name(),
            customer.getIsActive()
        );
    }

    private PlatformUserDto toRestaurantOwnerDto(RestaurantOwner owner) {
        return new PlatformUserDto(
            owner.getOwnerId(),
            owner.getFullName(),
            owner.getEmail(),
            owner.getPhone(),
            UserRole.RESTAURANT_OWNER.name(),
            userStatusSupport.resolve(owner.getStatus(), owner.getIsActive()).name(),
            owner.getIsActive()
        );
    }

    private PlatformUserDto toDeliveryPartnerDto(DeliveryPartner partner) {
        return new PlatformUserDto(
            partner.getPartnerId(),
            partner.getFullName(),
            partner.getEmail(),
            partner.getPhone(),
            UserRole.DELIVERY_PARTNER.name(),
            userStatusSupport.resolve(partner.getStatus(), partner.getIsActive()).name(),
            partner.getIsActive()
        );
    }

    private PlatformUserDto toAdminDto(AdminUser admin) {
        return new PlatformUserDto(
            admin.getAdminId(),
            admin.getFullName(),
            admin.getEmail(),
            null,
            UserRole.ADMIN.name(),
            userStatusSupport.resolve(admin.getStatus(), admin.getIsActive()).name(),
            admin.getIsActive()
        );
    }

    private InternalUserSummaryDto toInternal(PlatformUserDto user) {
        return new InternalUserSummaryDto(
            user.userId(),
            user.fullName(),
            user.email(),
            user.phone(),
            user.role(),
            user.status(),
            user.isActive()
        );
    }
}
