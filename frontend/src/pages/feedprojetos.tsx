import FiltrosBusca from '../components/filtrosbusca'
import CardProjeto from '../components/cardprojeto'

export default function Feed() {
  // Nossa lista fictícia para apresentar o projeto
  const projetos = [
    {
      id: 1,
      titulo: "Sistema Inteligente de Proteção Web",
      descricao: "Projeto de IA voltado para segurança online utilizando análise de comportamento visual, eye-tracking e identificação de conteúdo para proteger os usuários.",
      autor: "Luana Caldas",
      curso: "Ciências da Computação",
      vagas: 3,
      tags: ["Inteligência Artificial", "Segurança", "Visão Computacional"]
    },
    {
      id: 2,
      titulo: "Design de Interface para App Educacional",
      descricao: "Buscamos estudantes de Design e Computação para remodelar a interface gráfica e a experiência de usuário (UX) do nosso novo aplicativo de monitoria.",
      autor: "Eduarda Resende",
      curso: "Ciências da Computação",
      vagas: 2,
      tags: ["UX/UI Design", "Mobile", "Educação"]
    },
    {
      id: 3,
      titulo: "Plano de Negócios para Startup de Logística",
      descricao: "Precisamos de alunos de Administração e Direito para nos ajudar na estruturação de um modelo de negócios e análise de viabilidade jurídica de uma startup.",
      autor: "Profa. Carla Souza",
      curso: "Administração",
      vagas: 4,
      tags: ["Empreendedorismo", "Direito Digital", "Gestão"]
    },
    {
      id: 4,
      titulo: "Pesquisa em Saúde Pública e Bem-estar",
      descricao: "Grupo de estudo focado na coleta e análise de dados sobre o impacto das mídias sociais na saúde mental de jovens universitários da instituição.",
      autor: "Mariana Alves",
      curso: "Psicologia",
      vagas: 1,
      tags: ["Saúde Mental", "Análise de Dados", "Pesquisa"]
    }
  ];

  return (
    <div className="flex flex-col gap-8 pb-10">
      
      {/* O componente da barra de busca entra aqui */}
      <FiltrosBusca />

      {/* Grid que espalha os cards na tela */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-2 xl:grid-cols-3 gap-6">
        {projetos.map((projetoItem) => (
          // O componente do Card entra aqui, recebendo os dados
          <CardProjeto key={projetoItem.id} projeto={projetoItem} />
        ))}
      </div>
      
    </div>
  )
}