package com.unifapi.academico;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Sem @TestPropertySource aqui, o Spring importaria .env (spring.config.import) e este
 * teste subiria o contexto inteiro contra o Postgres de PRODUÇÃO configurado ali — já
 * aconteceu uma vez (rodou Flyway e ddl-auto=validate contra produção via um simples
 * `mvn test` local). H2 isola esse teste de qualquer datasource real.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:contextload;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "jwt.secret=context-load-test-secret-nao-usar-em-producao-0123456789ab",
        "jwt.expiration=3600000"
})
class AcademicoBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
