# Política de Privacidade — Academico+

Última atualização: agosto de 2026

Esta Política descreve como a Academico+ ("Plataforma") coleta, usa e protege seus dados pessoais, em conformidade com a Lei Geral de Proteção de Dados (Lei nº 13.709/2018 — LGPD).

## 1. Quais dados coletamos

| Dado | Finalidade |
|---|---|
| Nome, e-mail, senha (com hash, nunca em texto puro) | Autenticação e identificação na Plataforma |
| Curso, período, biografia, links (LinkedIn/GitHub), foto de perfil | Montar seu perfil público na rede acadêmica |
| Habilidades cadastradas | Recomendação de projetos e colaboradores por compatibilidade |
| Projetos criados, candidaturas enviadas, participação em equipes | Funcionamento do mural de projetos |
| Avaliações recebidas e enviadas (nota e comentário) | Reputação entre colaboradores |

Não coletamos dados sensíveis (saúde, biometria, opinião política/religiosa) nem dados de menores de idade sem consentimento dos responsáveis.

## 2. Base legal para o tratamento

Tratamos seus dados com base no seu **consentimento**, dado no momento do cadastro (aceite destes Termos e desta Política), e na **execução do próprio serviço** que você solicitou ao se cadastrar.

## 3. Com quem compartilhamos dados

- **Não vendemos nem compartilhamos seus dados com terceiros** para fins de marketing.
- Seu perfil (nome, curso, foto, habilidades, nota média) é visível para outros usuários autenticados da Plataforma — esse é o propósito da rede de networking acadêmico.
- Usamos um provedor externo de e-mail (Resend) apenas para enviar códigos de verificação de recuperação de senha.

## 4. Seus direitos como titular dos dados (LGPD)

Você pode, a qualquer momento:

- **Acessar** os dados que temos sobre você (`GET /usuarios/me`);
- **Corrigir** seus dados (`PUT /usuarios/me`);
- **Excluir sua conta**, através da própria Plataforma — seus dados de perfil deixam de ser exibidos publicamente; registros de projetos/candidaturas/avaliações já existentes podem ser mantidos de forma anonimizada para preservar o histórico de outros usuários (ex.: um projeto que você criou continua existindo para quem participou dele);
- **Solicitar informações** sobre como seus dados são tratados, através dos canais de contato do projeto.

## 5. Retenção de dados

Mantemos seus dados enquanto sua conta estiver ativa. Após a exclusão da conta, dados de identificação pessoal deixam de ser exibidos publicamente; registros que envolvem terceiros (ex.: uma avaliação que você recebeu de um colega) podem ser preservados para manter a integridade do histórico de colaboração de outras pessoas.

## 6. Segurança

Senhas são armazenadas com hash (BCrypt), o acesso à API é protegido por autenticação JWT, e há limites de tentativas em endpoints sensíveis (login e recuperação de senha) contra ataques de força bruta.

## 7. Contato

Dúvidas sobre esta Política ou solicitações relacionadas aos seus dados podem ser encaminhadas à equipe responsável pelo projeto, através dos canais informados no [README do projeto](../README.md).
