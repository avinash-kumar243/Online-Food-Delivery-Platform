package com.quickbite.auth.service;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.quickbite.auth.entity.AdminUser;
import com.quickbite.auth.entity.Customer;
import com.quickbite.auth.entity.DeliveryPartner;
import com.quickbite.auth.entity.RestaurantOwner;
import com.quickbite.auth.enums.UserStatus;
import com.quickbite.auth.exception.AccountAccessException;
import com.quickbite.auth.repository.AdminUserRepository;
import com.quickbite.auth.repository.CustomerRepository;
import com.quickbite.auth.repository.DeliveryPartnerRepository;
import com.quickbite.auth.repository.RestaurantOwnerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final CustomerRepository customerRepository;
	private final DeliveryPartnerRepository deliveryPartnerRepository;
	private final RestaurantOwnerRepository restaurantOwnerRepository;
	private final AdminUserRepository adminUserRepository;
	private final UserStatusSupport userStatusSupport;
	
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		var adminOpt = adminUserRepository.findByEmail(email);
		if (adminOpt.isPresent()) {
			AdminUser admin = adminOpt.get();
			ensureActive(userStatusSupport.resolve(admin.getStatus(), admin.getIsActive()), "Admin");
			return new org.springframework.security.core.userdetails.User(
					admin.getEmail(),
					admin.getPasswordHash(),
					List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
			);
		}

		// Check Customer
		var customerOpt = customerRepository.findByEmail(email);
		if (customerOpt.isPresent()) {
			Customer customer = customerOpt.get();
			ensureActive(userStatusSupport.resolve(customer.getStatus(), customer.getIsActive()), "Customer");
			return new org.springframework.security.core.userdetails.User(
					customer.getEmail(),
					customer.getPasswordHash() != null ? customer.getPasswordHash() : "",
					List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
			);
		}
		
		// Check RestaurantOwner
		var ownerOpt = restaurantOwnerRepository.findByEmail(email);
		if (ownerOpt.isPresent()) {
			RestaurantOwner owner = ownerOpt.get();
			ensureActive(userStatusSupport.resolve(owner.getStatus(), owner.getIsActive()), "Restaurant owner");
			return new org.springframework.security.core.userdetails.User(
					owner.getEmail(),
					owner.getPasswordHash() != null ? owner.getPasswordHash() : "",
					List.of(new SimpleGrantedAuthority("ROLE_RESTAURANT_OWNER"))
			);
		}
		
		// Check DeliveryPartner
		var partnerOpt = deliveryPartnerRepository.findByEmail(email);
		if (partnerOpt.isPresent()) {
			DeliveryPartner partner = partnerOpt.get();
			ensureActive(userStatusSupport.resolve(partner.getStatus(), partner.getIsActive()), "Delivery partner");
			return new org.springframework.security.core.userdetails.User(
					partner.getEmail(),
					partner.getPasswordHash() != null ? partner.getPasswordHash() : "",
					List.of(new SimpleGrantedAuthority("ROLE_DELIVERY_PARTNER"))
			);
		}
		
		throw new UsernameNotFoundException("User not found with this email!!!");
	}

	private void ensureActive(UserStatus status, String userType) {
		if (status == UserStatus.SUSPENDED || status == UserStatus.DELETED) {
			throw new AccountAccessException(status, userType + " account is " + status.name().toLowerCase());
		}
	}
	
}
