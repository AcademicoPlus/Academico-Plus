-- Momento da última troca de senha. O filtro JWT recusa tokens emitidos antes
-- disso, para que trocar/redefinir a senha derrube as sessões abertas.
-- Nulo para contas que nunca trocaram a senha: nenhum token é afetado.
ALTER TABLE usuarios ADD COLUMN senha_alterada_em TIMESTAMP;
