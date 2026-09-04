// Página estática com o conteúdo de legal/POLITICA_DE_PRIVACIDADE.md, linkada
// a partir do checkbox de aceite em cadastro.tsx (o backend exige aceitouTermos=true).
import { Link } from 'react-router-dom';

export default function PoliticaDePrivacidade() {
  return (
    <div className="min-h-screen w-full bg-[#F8F9FA] font-sans py-10 px-6">
      <div className="max-w-2xl mx-auto bg-white rounded-3xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-100 p-8">
        <Link to="/cadastro" className="text-[#1472dd] text-sm font-semibold hover:underline">
          ← Voltar para o cadastro
        </Link>

        <h1 className="text-2xl font-bold text-gray-900 mt-4 mb-1">Política de Privacidade — Academico+</h1>
        <p className="text-sm text-gray-400 mb-6">Última atualização: agosto de 2026</p>

        <div className="flex flex-col gap-5 text-gray-700 text-sm leading-relaxed">
          <p>
            Esta Política descreve como a Academico+ ("Plataforma") coleta, usa e protege seus
            dados pessoais, em conformidade com a Lei Geral de Proteção de Dados (Lei nº
            13.709/2018 — LGPD).
          </p>

          <section>
            <h2 className="font-semibold text-gray-900 mb-2">1. Quais dados coletamos</h2>
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-gray-200">
                    <th className="py-2 pr-4 font-semibold text-gray-900">Dado</th>
                    <th className="py-2 font-semibold text-gray-900">Finalidade</th>
                  </tr>
                </thead>
                <tbody>
                  <tr className="border-b border-gray-100">
                    <td className="py-2 pr-4">Nome, e-mail, senha (com hash, nunca em texto puro)</td>
                    <td className="py-2">Autenticação e identificação na Plataforma</td>
                  </tr>
                  <tr className="border-b border-gray-100">
                    <td className="py-2 pr-4">Curso, período, biografia, links (LinkedIn/GitHub), foto de perfil</td>
                    <td className="py-2">Montar seu perfil público na rede acadêmica</td>
                  </tr>
                  <tr className="border-b border-gray-100">
                    <td className="py-2 pr-4">Habilidades cadastradas</td>
                    <td className="py-2">Recomendação de projetos e colaboradores por compatibilidade</td>
                  </tr>
                  <tr className="border-b border-gray-100">
                    <td className="py-2 pr-4">Projetos criados, candidaturas enviadas, participação em equipes</td>
                    <td className="py-2">Funcionamento do mural de projetos</td>
                  </tr>
                  <tr>
                    <td className="py-2 pr-4">Avaliações recebidas e enviadas (nota e comentário)</td>
                    <td className="py-2">Reputação entre colaboradores</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p className="mt-2">
              Não coletamos dados sensíveis (saúde, biometria, opinião política/religiosa) nem
              dados de menores de idade sem consentimento dos responsáveis.
            </p>
          </section>

          <section>
            <h2 className="font-semibold text-gray-900 mb-2">2. Base legal para o tratamento</h2>
            <p>
              Tratamos seus dados com base no seu <strong>consentimento</strong>, dado no momento
              do cadastro (aceite destes Termos e desta Política), e na <strong>execução do
              próprio serviço</strong> que você solicitou ao se cadastrar.
            </p>
          </section>

          <section>
            <h2 className="font-semibold text-gray-900 mb-2">3. Com quem compartilhamos dados</h2>
            <ul className="list-disc pl-5 flex flex-col gap-1">
              <li>Não vendemos nem compartilhamos seus dados com terceiros para fins de marketing.</li>
              <li>Seu perfil (nome, curso, foto, habilidades, nota média) é visível para outros usuários autenticados da Plataforma — esse é o propósito da rede de networking acadêmico.</li>
              <li>Usamos um provedor externo de e-mail (Resend) apenas para enviar códigos de verificação de recuperação de senha.</li>
            </ul>
          </section>

          <section>
            <h2 className="font-semibold text-gray-900 mb-2">4. Seus direitos como titular dos dados (LGPD)</h2>
            <p className="mb-2">Você pode, a qualquer momento:</p>
            <ul className="list-disc pl-5 flex flex-col gap-1">
              <li><strong>Acessar</strong> os dados que temos sobre você (GET /usuarios/me);</li>
              <li><strong>Corrigir</strong> seus dados (PUT /usuarios/me);</li>
              <li>
                <strong>Excluir sua conta</strong>, através da própria Plataforma — seus dados de
                perfil deixam de ser exibidos publicamente; registros de projetos/candidaturas/
                avaliações já existentes podem ser mantidos de forma anonimizada para preservar o
                histórico de outros usuários (ex.: um projeto que você criou continua existindo
                para quem participou dele);
              </li>
              <li><strong>Solicitar informações</strong> sobre como seus dados são tratados, através dos canais de contato do projeto.</li>
            </ul>
          </section>

          <section>
            <h2 className="font-semibold text-gray-900 mb-2">5. Retenção de dados</h2>
            <p>
              Mantemos seus dados enquanto sua conta estiver ativa. Após a exclusão da conta, dados
              de identificação pessoal deixam de ser exibidos publicamente; registros que envolvem
              terceiros (ex.: uma avaliação que você recebeu de um colega) podem ser preservados
              para manter a integridade do histórico de colaboração de outras pessoas.
            </p>
          </section>

          <section>
            <h2 className="font-semibold text-gray-900 mb-2">6. Segurança</h2>
            <p>
              Senhas são armazenadas com hash (BCrypt), o acesso à API é protegido por autenticação
              JWT, e há limites de tentativas em endpoints sensíveis (login e recuperação de senha)
              contra ataques de força bruta.
            </p>
          </section>

          <section>
            <h2 className="font-semibold text-gray-900 mb-2">7. Contato</h2>
            <p>
              Dúvidas sobre esta Política ou solicitações relacionadas aos seus dados podem ser
              encaminhadas à equipe responsável pelo projeto, através dos canais informados no
              README do projeto.
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}
