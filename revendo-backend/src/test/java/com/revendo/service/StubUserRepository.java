package com.revendo.service;

import com.revendo.model.User;
import com.revendo.repository.UserRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Minimal no-op base implementation of {@link UserRepository} for unit tests.
 *
 * Override only the methods you need in each test.
 * The generic {@code <S extends User> S save(S entity)} covers all save calls —
 * do NOT add an additional {@code User save(User)} override; that causes an
 * erasure clash at compile time.
 */
@SuppressWarnings("all")
class StubUserRepository implements UserRepository {

    // ----- UserRepository custom methods -----

    @Override
    public Optional<User> findByEmail(String email) {
        return Optional.empty();
    }

    @Override
    public Page<User> findBySearchQuery(String query, Pageable pageable) {
        // Empty or null query = return all (no filter)
        return Page.empty(pageable);
    }

    // ----- JpaRepository / CrudRepository boilerplate -----

    @Override public <S extends User> S save(S entity) { return entity; }
    @Override public <S extends User> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
    @Override public Optional<User> findById(Long id) { return Optional.empty(); }
    @Override public boolean existsById(Long id) { return false; }
    @Override public List<User> findAll() { throw new UnsupportedOperationException(); }
    @Override public List<User> findAllById(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
    @Override public long count() { return 0; }
    @Override public void deleteById(Long id) {}
    @Override public void delete(User entity) {}
    @Override public void deleteAllById(Iterable<? extends Long> ids) {}
    @Override public void deleteAll(Iterable<? extends User> entities) {}
    @Override public void deleteAll() {}
    @Override public List<User> findAll(Sort sort) { throw new UnsupportedOperationException(); }
    @Override public Page<User> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    @Override public void flush() {}
    @Override public <S extends User> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
    @Override public void deleteAllInBatch(Iterable<User> entities) {}
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
    @Override public void deleteAllInBatch() {}
    @Override public User getOne(Long id) { throw new UnsupportedOperationException(); }
    @Override public User getById(Long id) { throw new UnsupportedOperationException(); }
    @Override public User getReferenceById(Long id) { throw new UnsupportedOperationException(); }
    @Override public <S extends User> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends User> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends User> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
    @Override public <S extends User> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
    @Override public <S extends User> long count(Example<S> example) { return 0; }
    @Override public <S extends User> boolean exists(Example<S> example) { return false; }
    @Override public <S extends User, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
}
