package com.quickbite.auth.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import com.quickbite.auth.config.OAuth2RequestContext;
import com.quickbite.auth.entity.Customer;
import com.quickbite.auth.entity.DeliveryPartner;
import com.quickbite.auth.entity.RestaurantOwner;
import com.quickbite.auth.repository.CustomerRepository;
import com.quickbite.auth.repository.DeliveryPartnerRepository;
import com.quickbite.auth.repository.RestaurantOwnerRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private DeliveryPartnerRepository deliveryPartnerRepository;

    @Mock
    private RestaurantOwnerRepository restaurantOwnerRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler successHandler;

    private final String email = "test@quickbite.com";
    private final String name = "Avinash Kumar";
    private final String picture = "http://profile.pic";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(successHandler, "frontendUrl", "http://localhost:4200");

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(Map.of(
                "email", email,
                "name", name,
                "picture", picture
        ));
    }

    @Test
    @DisplayName("New Customer Signup - Role From Session")
    void onAuthenticationSuccess_NewCustomer_RoleFromSession() throws Exception {
        mockSessionRole("CUSTOMER");
        mockCustomerNotFound();
        mockToken("customer-jwt");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(customerRepository).save(any(Customer.class));
        verifyUserCreatedEmail("CUSTOMER");
        verifyRedirect("customer-jwt", "CUSTOMER");
    }

    @Test
    @DisplayName("Existing Customer Login - Role From Request Parameter")
    void onAuthenticationSuccess_ExistingCustomer_RoleFromParam() throws Exception {
        Customer customer = createCustomer(1L);
        mockRequestParamRole("CUSTOMER");
        when(customerRepository.findByEmail(email)).thenReturn(Optional.of(customer));
        mockToken("existing-customer-jwt");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(customerRepository).save(customer);
        verifyNoUserCreatedEmail();
        verifyRedirect("existing-customer-jwt", "CUSTOMER");
    }

    @Test
    @DisplayName("New Restaurant Owner Signup - Role From Session")
    void onAuthenticationSuccess_NewRestaurantOwner_RoleFromSession() throws Exception {
        mockSessionRole("RESTAURANT_OWNER");
        mockRestaurantOwnerNotFound();
        mockToken("owner-jwt");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(restaurantOwnerRepository).save(any(RestaurantOwner.class));
        verifyUserCreatedEmail("RESTAURANT_OWNER");
        verifyRedirect("owner-jwt", "RESTAURANT_OWNER");
    }

    @Test
    @DisplayName("Existing Restaurant Owner Login - Role From Request Parameter")
    void onAuthenticationSuccess_ExistingRestaurantOwner_RoleFromParam() throws Exception {
        RestaurantOwner owner = createRestaurantOwner(101L);
        mockRequestParamRole("RESTAURANT_OWNER");
        when(restaurantOwnerRepository.findByEmail(email)).thenReturn(Optional.of(owner));
        mockToken("existing-owner-jwt");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(restaurantOwnerRepository).save(owner);
        verifyNoUserCreatedEmail();
        verifyRedirect("existing-owner-jwt", "RESTAURANT_OWNER");
    }

    @Test
    @DisplayName("New Delivery Partner Signup - DELIVERY_AGENT Mapping")
    void onAuthenticationSuccess_NewDeliveryPartner_DeliveryAgentMapping() throws Exception {
        mockRequestParamRole("DELIVERY_AGENT");
        mockDeliveryPartnerNotFound();
        mockToken("delivery-jwt");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(deliveryPartnerRepository).save(any(DeliveryPartner.class));
        verifyUserCreatedEmail("DELIVERY_PARTNER");
        verifyRedirect("delivery-jwt", "DELIVERY_PARTNER");
    }

    @Test
    @DisplayName("Existing Delivery Partner Login - DELIVERY_PARTNER Role")
    void onAuthenticationSuccess_ExistingDeliveryPartner() throws Exception {
        DeliveryPartner partner = createDeliveryPartner(202L);
        mockRequestParamRole("DELIVERY_PARTNER");
        when(deliveryPartnerRepository.findByEmail(email)).thenReturn(Optional.of(partner));
        mockToken("existing-delivery-jwt");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(deliveryPartnerRepository).save(partner);
        verifyNoUserCreatedEmail();
        verifyRedirect("existing-delivery-jwt", "DELIVERY_PARTNER");
    }

    @Test
    @DisplayName("Default Role Should Be Customer When No Role Provided")
    void onAuthenticationSuccess_DefaultRole_ShouldBeCustomer() throws Exception {
        mockRequestParamRole(null);
        mockCustomerNotFound();
        mockToken("default-customer-jwt");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(customerRepository).save(any(Customer.class));
        verifyUserCreatedEmail("CUSTOMER");
        verifyRedirect("default-customer-jwt", "CUSTOMER");
    }

    @Test
    @DisplayName("Session Role Should Get Priority Over Request Parameter Role")
    void onAuthenticationSuccess_SessionRoleShouldGetPriorityOverParamRole() throws Exception {
        mockSessionRole("RESTAURANT_OWNER");
        mockRestaurantOwnerNotFound();
        mockToken("session-priority-jwt");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(restaurantOwnerRepository).save(any(RestaurantOwner.class));
        verify(customerRepository, never()).save(any(Customer.class));
        verify(request, never()).getParameter("appRole");
        verifyRedirect("session-priority-jwt", "RESTAURANT_OWNER");
    }

    @Test
    @DisplayName("Blank Role Should Default To Customer")
    void onAuthenticationSuccess_BlankRole_ShouldDefaultToCustomer() throws Exception {
        mockRequestParamRole("");
        mockCustomerNotFound();
        mockToken("blank-role-jwt");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(customerRepository).save(any(Customer.class));
        verifyRedirect("blank-role-jwt", "CUSTOMER");
    }

    @Test
    @DisplayName("Unknown Role Should Default To Customer")
    void onAuthenticationSuccess_UnknownRole_ShouldDefaultToCustomer() throws Exception {
        mockRequestParamRole("UNKNOWN_ROLE");
        mockCustomerNotFound();
        mockToken("unknown-role-jwt");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(customerRepository).save(any(Customer.class));
        verifyRedirect("unknown-role-jwt", "CUSTOMER");
    }

    private void mockSessionRole(String role) {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(OAuth2RequestContext.APP_ROLE_SESSION_ATTRIBUTE))
                .thenReturn(role);
    }

    private void mockRequestParamRole(String role) {
        when(request.getSession(false)).thenReturn(null);
        when(request.getParameter("appRole")).thenReturn(role);
    }

    private void mockToken(String token) {
        when(jwtService.generateToken(anyString(), anyString(), any())).thenReturn(token);
    }

    private void mockCustomerNotFound() {
        when(customerRepository.findByEmail(email)).thenReturn(Optional.empty());
    }

    private void mockRestaurantOwnerNotFound() {
        when(restaurantOwnerRepository.findByEmail(email)).thenReturn(Optional.empty());
    }

    private void mockDeliveryPartnerNotFound() {
        when(deliveryPartnerRepository.findByEmail(email)).thenReturn(Optional.empty());
    }

    private Customer createCustomer(Long id) {
        Customer customer = new Customer();
        customer.setCustomerId(id);
        customer.setEmail(email);
        return customer;
    }

    private RestaurantOwner createRestaurantOwner(Long id) {
        RestaurantOwner owner = new RestaurantOwner();
        owner.setOwnerId(id);
        owner.setEmail(email);
        return owner;
    }

    private DeliveryPartner createDeliveryPartner(Long id) {
        DeliveryPartner partner = new DeliveryPartner();
        partner.setPartnerId(id);
        partner.setEmail(email);
        return partner;
    }

    private void verifyUserCreatedEmail(String role) {
        verify(emailService).sendUserCreatedEmail(any(), eq(name), eq(email), eq(role));
    }

    private void verifyNoUserCreatedEmail() {
        verify(emailService, never()).sendUserCreatedEmail(any(), anyString(), anyString(), anyString());
    }

    private void verifyRedirect(String token, String userType) throws Exception {
        verify(response).sendRedirect(argThat(url ->
                url != null &&
                        url.contains("token=" + token) &&
                        url.contains("userType=" + userType)
        ));
    }
}