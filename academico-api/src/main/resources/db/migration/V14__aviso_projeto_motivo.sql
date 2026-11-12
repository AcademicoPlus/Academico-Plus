-- Motivo informado pelo criador ao cancelar o projeto. O frontend já enviava
-- esse campo (AlterarStatusProjetoRequest.motivo), mas ele era descartado:
-- agora vai junto no aviso que os membros recebem. Nulo nos demais tipos de
-- aviso (conclusão) e quando o criador não informa motivo.
ALTER TABLE projeto_cancelamento_aviso ADD COLUMN motivo VARCHAR(500);
