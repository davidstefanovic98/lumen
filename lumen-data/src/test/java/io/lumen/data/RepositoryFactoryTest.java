package io.lumen.data;

import io.lumen.data.repository.JpaRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositoryFactoryTest {

    static class User {}

    private final RepositoryFactory factory = new RepositoryFactory(null);

    interface DirectRepository extends JpaRepository<User, Long> {}

    @Test
    void directlyParameterizedRepository_resolvesNormally() {
        assertEquals(User.class, factory.resolveEntityClass(DirectRepository.class));
    }

    interface BaseRepo<T> extends JpaRepository<T, Long> {}
    interface OneLevelRepository extends BaseRepo<User> {}

    @Test
    void oneLevelOfIndirection_resolvesEntityType() {
        assertEquals(User.class, factory.resolveEntityClass(OneLevelRepository.class));
    }

    // Regression: an intermediate interface that already binds the entity concretely, then
    // extended raw one more level down — UserRepository.getGenericInterfaces() alone isn't even
    // a ParameterizedType here, so a single-level check finds nothing and used to throw.
    interface UserOps extends BaseRepo<User> {}
    interface PassThroughRepository extends UserOps {}

    @Test
    void nonGenericIntermediateInterfaceExtendedRaw_stillResolvesEntityType() {
        assertEquals(User.class, factory.resolveEntityClass(PassThroughRepository.class));
    }

    interface AuditableRepo<T, ID> extends BaseRepoTwoParams<T, ID> {}
    interface BaseRepoTwoParams<T, ID> extends JpaRepository<T, ID> {}
    interface ThreeLevelRepository extends AuditableRepo<User, Long> {}

    @Test
    void multipleLevelsOfIndirection_resolvesEntityType() {
        assertEquals(User.class, factory.resolveEntityClass(ThreeLevelRepository.class));
    }

    interface NotARepository {}

    @Test
    void unrelatedInterface_throwsWithClearMessage() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> factory.resolveEntityClass(NotARepository.class));
        assertEquals("Could not resolve entity type for " + NotARepository.class.getName(), ex.getMessage());
    }
}