package com.quickbite.auth.service;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.quickbite.auth.entity.Customer;
import com.quickbite.auth.entity.DeliveryPartner;
import com.quickbite.auth.entity.RestaurantOwner;
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
	
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		// Check Customer
		var customerOpt = customerRepository.findByEmail(email);
		if (customerOpt.isPresent()) {
			Customer customer = customerOpt.get();
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
			return new org.springframework.security.core.userdetails.User(
					partner.getEmail(),
					partner.getPasswordHash() != null ? partner.getPasswordHash() : "",
					List.of(new SimpleGrantedAuthority("ROLE_DELIVERY_AGENT"))
			);
		}
		
		throw new UsernameNotFoundException("User not found with this email!!!");
	}
	
}