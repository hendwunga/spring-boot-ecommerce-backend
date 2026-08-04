package com.hendro.ecommerce.dao;

import com.hendro.ecommerce.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    AppUser findByEmail(String email);

    boolean existsByEmail(String email);

}
