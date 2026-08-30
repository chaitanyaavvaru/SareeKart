package com.sareekart.service;

import com.sareekart.dto.address.AddressRequest;
import com.sareekart.dto.address.AddressResponse;
import com.sareekart.entity.Address;
import com.sareekart.entity.User;
import com.sareekart.entity.enums.AddressType;
import com.sareekart.exception.ResourceNotFoundException;
import com.sareekart.repository.AddressRepository;
import com.sareekart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        List<Address> addresses = addressRepository.findByUserId(user.getId());
        return addresses.stream()
            .map(AddressResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AddressResponse getDefaultAddress(String userEmail, AddressType type) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        return addressRepository.findByUserIdAndIsDefaultTrue(user.getId())
            .map(AddressResponse::from)
            .orElse(null);
    }

    @Transactional
    public AddressResponse addAddress(String userEmail, AddressRequest request) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        // If this is set as default, unset other defaults of same type
        if (request.getIsDefault()) {
            addressRepository.findByUserIdAndType(user.getId(), request.getType())
                .stream()
                .filter(Address::getIsDefault)
                .forEach(addr -> {
                    addr.setIsDefault(false);
                    addressRepository.save(addr);
                });
        }

        Address address = new Address();
        address.setUser(user);
        address.setType(request.getType());
        address.setName(request.getName());
        address.setPhone(request.getPhone());
        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setCountry(request.getCountry() != null ? request.getCountry() : "India");
        address.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);

        address = addressRepository.save(address);
        log.info("Added address for user: {}", userEmail);
        return AddressResponse.from(address);
    }

    @Transactional
    public AddressResponse updateAddress(String userEmail, Long addressId, AddressRequest request) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Address address = addressRepository.findById(addressId)
            .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Address", "id", addressId);
        }

        // If this is set as default, unset other defaults of same type
        if (request.getIsDefault() && !address.getIsDefault()) {
            addressRepository.findByUserIdAndType(user.getId(), request.getType())
                .stream()
                .filter(Address::getIsDefault)
                .forEach(addr -> {
                    addr.setIsDefault(false);
                    addressRepository.save(addr);
                });
        }

        address.setType(request.getType());
        address.setName(request.getName());
        address.setPhone(request.getPhone());
        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setCountry(request.getCountry());
        address.setIsDefault(request.getIsDefault());

        address = addressRepository.save(address);
        log.info("Updated address for user: {}", userEmail);
        return AddressResponse.from(address);
    }

    @Transactional
    public void deleteAddress(String userEmail, Long addressId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Address address = addressRepository.findById(addressId)
            .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Address", "id", addressId);
        }

        addressRepository.delete(address);
        log.info("Deleted address for user: {}", userEmail);
    }

    @Transactional
    public AddressResponse setDefaultAddress(String userEmail, Long addressId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Address address = addressRepository.findById(addressId)
            .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Address", "id", addressId);
        }

        // Unset other defaults of same type
        addressRepository.findByUserIdAndType(user.getId(), address.getType())
            .stream()
            .filter(Address::getIsDefault)
            .forEach(addr -> {
                addr.setIsDefault(false);
                addressRepository.save(addr);
            });

        address.setIsDefault(true);
        address = addressRepository.save(address);
        log.info("Set default address for user: {}", userEmail);
        return AddressResponse.from(address);
    }
}