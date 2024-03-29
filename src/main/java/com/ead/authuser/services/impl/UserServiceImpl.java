package com.ead.authuser.services.impl;

import com.ead.authuser.controllers.UserController;
import com.ead.authuser.dtos.InstructorDTO;
import com.ead.authuser.dtos.UserDTO;
import com.ead.authuser.enums.ActionType;
import com.ead.authuser.enums.RoleType;
import com.ead.authuser.enums.UserType;
import com.ead.authuser.exceptions.BadRequestHttpException;
import com.ead.authuser.exceptions.NotFoundHttpException;
import com.ead.authuser.mappers.UserMapper;
import com.ead.authuser.models.UserModel;
import com.ead.authuser.publishers.UserEventPublisher;
import com.ead.authuser.repositories.UserRepository;
import com.ead.authuser.services.RoleService;
import com.ead.authuser.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.ead.authuser.enums.RoleType.ROLE_ADMIN;
import static com.ead.authuser.enums.RoleType.ROLE_INSTRUCTOR;
import static com.ead.authuser.enums.RoleType.ROLE_STUDENT;
import static com.ead.authuser.enums.UserType.ADMIN;
import static com.ead.authuser.enums.UserType.INSTRUCTOR;
import static com.ead.authuser.enums.UserType.STUDENT;
import static com.ead.authuser.enums.UserType.USER;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository repository;

    private final UserEventPublisher publisher;

    private final RoleService roleService;

    private final UserMapper mapper;

    public UserServiceImpl(
            final UserRepository repository,
            final UserEventPublisher publisher,
            final UserMapper mapper,
            final RoleService roleService
    ) {
        this.repository = repository;
        this.publisher = publisher;
        this.mapper = mapper;
        this.roleService = roleService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserModel findById(final UUID id) {
        return repository.findById(id).orElseThrow(NotFoundHttpException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserModel> findAll(
            final Pageable pageable,
            final Specification<UserModel> spec
    ) {

        final Page<UserModel> users = repository.findAll(spec, pageable);

        users.forEach(u -> u.add(linkTo(methodOn(UserController.class).findById(u.getId())).withSelfRel()));

        return users;

    }

    @Override
    @Transactional
    public UserDTO save(final UserDTO dto) {

        final var role = roleService.findByName(RoleType.ROLE_USER);

        final var domain = mapper.toDomain(dto, role, USER);

        repository.save(domain);

        return mapper.toDTOWithoutPassword(domain);

    }

    @Override
    @Transactional
    public UserDTO saveAdmin(final UserDTO dto) {

        final var role = roleService.findByName(ROLE_ADMIN);

        final var domain = mapper.toDomain(dto, role, ADMIN);

        repository.save(domain);

        return mapper.toDTOWithoutPassword(domain);

    }

    @Override
    @Transactional
    public void publishUserBy(final ActionType type, final UserDTO dto) {

        final var userEventDTO = mapper.toUserEventDTO(dto, type);

        publisher.publishEvent(userEventDTO);
    }

    @Override
    @Transactional
    public UserDTO update(final UUID id, final UserDTO dto) {

        final var domain = findById(id);

        final var domainUpdated = repository.save(mapper.update(domain, dto));

        return mapper.toDTOWithoutPassword(domainUpdated);

    }

    @Override
    @Transactional
    public UserDTO updateToInstructor(final InstructorDTO dto) {

        final var domain = findById(dto.getUserId());

        final var role = roleService.findByName(ROLE_INSTRUCTOR);

        domain.setType(INSTRUCTOR);
        domain.getRoles().add(role);

        final var domainUpdated = repository.save(domain);

        return mapper.toDTOWithoutPassword(domainUpdated);

    }

    @Override
    @Transactional
    public void updatePassword(final UUID id, final UserDTO dto) {

        final var domain = findById(id);

        if (!domain.getPassword().equals(dto.oldPassword())) {
            throw new BadRequestHttpException("Passwords do not match!");
        }

        repository.save(mapper.updatePassword(domain, dto));

    }

    @Override
    @Transactional
    public UserDTO updateImage(final UUID id, final UserDTO dto) {

        final var domain = findById(id);

        repository.save(mapper.updateImage(domain, dto));

        return mapper.toDTOWithoutPassword(domain);

    }

    @Override
    @Transactional
    public UserDTO delete(final UUID userId) {

        final var entity = findById(userId);

        repository.delete(entity);

        return mapper.toDTOWithoutPassword(entity);
    }
}
