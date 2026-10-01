package com.unifapi.academico.dto.mensagem;

import com.unifapi.academico.dto.projeto.ProjetoRefResponse;

/**
 * Item da caixa "Mensagens": uma conversa por projeto em que o usuário é
 * criador ou membro. ultimaMensagem é null quando ninguém escreveu ainda.
 */
public record ConversaResponse(
        ProjetoRefResponse projeto,
        String bannerUrl,
        MensagemProjetoResponse ultimaMensagem,
        long naoLidas
) {
}
