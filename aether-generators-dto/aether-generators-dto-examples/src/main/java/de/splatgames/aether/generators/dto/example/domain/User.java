/*
 * Copyright (c) 2025 Splatgames.de Software and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.splatgames.aether.generators.dto.example.domain;

import de.splatgames.aether.generators.dto.annotations.Dto;
import de.splatgames.aether.generators.dto.example.domain.dto.MetadataDto;
import de.splatgames.aether.generators.dto.example.domain.dto.UserDataDto;
import de.splatgames.aether.generators.dto.example.domain.dto.UserDto;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class User {
    @Dto(order = 1)
    private UUID uuid;

    @Dto(order = 2)
    private String username;

    @Dto(order = 3)
    private String email;

    @Dto(order = 4)
    private String encryptedPassword;

    @Dto(order = 5)
    private boolean deleted;

    @Dto(order = 1, value = "Metadata")
    private Instant createdAt;

    @Dto(order = 2, value = "Metadata")
    private boolean deactivated;

    @Dto(order = 3, value = "Metadata")
    private Instant deactivatedAt;

    @Dto(order = 4, value = "Metadata")
    private User deactivatedBy;

    @Dto(order = 5, value = "Metadata")
    private String deactivatedReason;

    @Dto(order = 6, value = "Metadata")
    private Instant lastLogin;

    @Dto(order = 7, value = "Metadata")
    private String lastIp;

    @Dto(order = 8, value = "Metadata")
    private List<User> supervisors;

    @Dto(order = 9, value = "UserData")
    private String firstName;

    @Dto(order = 10, value = "UserData")
    private String lastName;

    @Dto(order = 11, value = "UserData")
    private String position;

    @Dto(order = 12, value = "UserData")
    private String department;

    @Dto(order = 13, value = "UserData")
    private String employeeId;

    @Dto(order = 14, value = "UserData")
    private String phoneNumber;

    @Dto(order = 15, value = "UserData")
    private String address;

    @Dto(order = 16, value = "UserData")
    private String city;

    @Dto(order = 17, value = "UserData")
    private String zipCode;

    @Dto(order = 18, value = "UserData")
    private String country;

    @Dto(order = 19, value = "UserData")
    private String birthday;

    @Dto(order = 20, value = "UserData")
    private String hireDate;

    public User() {
        // default constructor
    }

    public User(final UserDto userDto,
                final UserDataDto userDataDto,
                final MetadataDto metadataDto) {
        this.change(userDto);
        this.change(userDataDto);
        this.change(metadataDto);
    }

    public void change(final UserDto userDto) {
        Objects.requireNonNull(userDto, "userDto must not be null");

        this.uuid = userDto.getUuid();
        this.username = userDto.getUsername();
        this.email = userDto.getEmail();
        this.encryptedPassword = userDto.getEncryptedPassword();
        this.deleted = userDto.isDeleted();
    }

    public UserDto buildUserDto() {
        return new UserDto(this.uuid,
                this.username,
                this.email,
                this.encryptedPassword,
                this.deleted);
    }

    public void change(final UserDataDto userDataDto) {
        Objects.requireNonNull(userDataDto, "userDataDto must not be null");

        this.firstName = userDataDto.getFirstName();
        this.lastName = userDataDto.getLastName();
        this.position = userDataDto.getPosition();
        this.department = userDataDto.getDepartment();
        this.employeeId = userDataDto.getEmployeeId();
        this.phoneNumber = userDataDto.getPhoneNumber();
        this.address = userDataDto.getAddress();
        this.city = userDataDto.getCity();
        this.zipCode = userDataDto.getZipCode();
        this.country = userDataDto.getCountry();
        this.birthday = userDataDto.getBirthday();
        this.hireDate = userDataDto.getHireDate();
    }

    public void change(final MetadataDto metadataDto) {
        Objects.requireNonNull(metadataDto, "metadataDto must not be null");

        this.createdAt = metadataDto.getCreatedAt();
        this.deactivated = metadataDto.isDeactivated();
        this.deactivatedAt = metadataDto.getDeactivatedAt();
        this.deactivatedBy = metadataDto.getDeactivatedBy();
        this.deactivatedReason = metadataDto.getDeactivatedReason();
        this.lastLogin = metadataDto.getLastLogin();
        this.lastIp = metadataDto.getLastIp();
        this.supervisors = metadataDto.getSupervisors();
    }

    public MetadataDto buildMetadataDto() {
        return new MetadataDto(this.createdAt,
                this.deactivated,
                this.deactivatedAt,
                this.deactivatedBy,
                this.deactivatedReason,
                this.lastLogin,
                this.lastIp,
                this.supervisors);
    }

    public UserDataDto buildUserDataDto() {
        return new UserDataDto(this.firstName,
                this.lastName,
                this.position,
                this.department,
                this.employeeId,
                this.phoneNumber,
                this.address,
                this.city,
                this.zipCode,
                this.country,
                this.birthday,
                this.hireDate);
    }
}
