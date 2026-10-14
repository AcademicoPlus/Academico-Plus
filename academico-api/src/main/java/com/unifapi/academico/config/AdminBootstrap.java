package com.unifapi.academico.config;

import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.repository.NivelAcessoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Promove um usuário já cadastrado a ADMIN na inicialização, a partir do
 * e-mail em {@code ADMIN_BOOTSTRAP_EMAIL}.
 *
 * <p>Existe porque alterar o nível de acesso de alguém exige
 * {@code hasRole('ADMIN')} (ver UsuarioController) e o cadastro público sempre
 * atribui ALUNO — ou seja, num banco de produção novo não haveria nenhum
 * caminho para criar o primeiro admin, e o painel /admin (incluindo a fila de
 * denúncias) ficaria inacessível para sempre.
 *
 * <p>Promove em vez de criar a conta do zero: assim não é preciso passar senha
 * por variável de ambiente. O fluxo é cadastrar-se normalmente pela tela de
 * cadastro, depois definir a variável e reiniciar o serviço.
 *
 * <p>É idempotente e não faz nada quando a variável está vazia — o que vale
 * para desenvolvimento local e para os testes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap {

    private static final String NIVEL_ADMIN = "ADMIN";

    private final UsuarioRepository usuarioRepository;
    private final NivelAcessoRepository nivelAcessoRepository;

    @Value("${app.admin-bootstrap.email:}")
    private String emailAdminInicial;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void promoverAdminInicial() {
        if (emailAdminInicial == null || emailAdminInicial.isBlank()) {
            return;
        }

        String emailNorm = emailAdminInicial.trim().toLowerCase();

        // findByEmail só enxerga contas ativas (@SQLRestriction na entidade):
        // uma conta excluída não deve ser ressuscitada como admin.
        Usuario usuario = usuarioRepository.findByEmail(emailNorm).orElse(null);
        if (usuario == null) {
            log.warn("ADMIN_BOOTSTRAP_EMAIL aponta para '{}', que não é uma conta ativa. "
                    + "Cadastre-se com esse e-mail pela tela de cadastro e reinicie o serviço.", emailNorm);
            return;
        }

        if (NIVEL_ADMIN.equalsIgnoreCase(usuario.getNivelAcesso().getNome())) {
            log.info("Usuário '{}' já é ADMIN — nada a fazer.", emailNorm);
            return;
        }

        NivelAcesso admin = nivelAcessoRepository.findByNome(NIVEL_ADMIN).orElse(null);
        if (admin == null) {
            log.error("Nível de acesso ADMIN não existe no banco — as migrations do Flyway rodaram?");
            return;
        }

        usuario.setNivelAcesso(admin);
        usuarioRepository.save(usuario);
        log.info("Usuário '{}' promovido a ADMIN via ADMIN_BOOTSTRAP_EMAIL.", emailNorm);
    }
}
