package br.unesp.fct.evcomp.controller;

import br.unesp.fct.evcomp.config.JwtUtil;
import br.unesp.fct.evcomp.domain.Administrador;
import br.unesp.fct.evcomp.domain.Atividade;
import br.unesp.fct.evcomp.domain.Evento;
import br.unesp.fct.evcomp.domain.Inscrição;
import br.unesp.fct.evcomp.domain.Participante;
import br.unesp.fct.evcomp.domain.TipoContabilizacao;
import br.unesp.fct.evcomp.repository.AtividadeRepository;
import br.unesp.fct.evcomp.repository.EventoRepository;
import br.unesp.fct.evcomp.repository.InscricaoRepository;
import br.unesp.fct.evcomp.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre a janela de inscrição no POST /api/inscricoes e a troca de atividades
 * no PUT /api/inscricoes/{id}, reproduzindo os cenários validados manualmente via curl.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InscricaoControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private AtividadeRepository atividadeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    private Evento eventoAberto;
    private Evento eventoFechado;
    private Atividade atividade1;
    private Atividade atividade2;
    private Atividade atividadeDoEventoFechado;
    private Participante dono;
    private String tokenDono;
    private String tokenOutro;
    private String tokenAdmin;

    @BeforeEach
    void setUp() {
        eventoAberto = eventoRepository.save(Evento.criarEvento(
            "Evento Aberto", LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(1).plusDays(2),
            "descricao", "", TipoContabilizacao.POR_ATIVIDADE,
            LocalDate.now().minusDays(10), LocalDate.now().plusDays(10)
        ));

        eventoFechado = eventoRepository.save(Evento.criarEvento(
            "Evento Fechado", LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(2).plusDays(2),
            "descricao", "", TipoContabilizacao.POR_ATIVIDADE,
            LocalDate.now().minusMonths(3), LocalDate.now().minusMonths(2).minusDays(1)
        ));

        atividade1 = atividadeRepository.save(criarAtividade("Atividade 1", eventoAberto));
        atividade2 = atividadeRepository.save(criarAtividade("Atividade 2", eventoAberto));
        atividadeDoEventoFechado = atividadeRepository.save(criarAtividade("Atividade do Evento Fechado", eventoFechado));

        dono = (Participante) usuarioRepository.save(new Participante("Dono da Inscrição", "dono@teste.com", null));
        Participante outro = (Participante) usuarioRepository.save(new Participante("Outro Participante", "outro@teste.com", null));
        Administrador admin = (Administrador) usuarioRepository.save(new Administrador("Admin", "admin@teste.com", null));

        tokenDono = jwtUtil.gerarToken(dono.getId(), dono.getEmail(), dono.getNomeCompleto(), "PARTICIPANTE", false);
        tokenOutro = jwtUtil.gerarToken(outro.getId(), outro.getEmail(), outro.getNomeCompleto(), "PARTICIPANTE", false);
        tokenAdmin = jwtUtil.gerarToken(admin.getId(), admin.getEmail(), admin.getNomeCompleto(), "ADMIN", false);
    }

    private Atividade criarAtividade(String titulo, Evento evento) {
        Atividade atividade = new Atividade(titulo, "descricao", "pre-requisitos", evento.getDataInicio(), LocalTime.of(9, 0), evento.getDataFim(), LocalTime.of(11, 0), 50, 2, 2);
        atividade.setEvento(evento);
        return atividade;
    }

    private Inscrição criarInscricao(Participante participante, Evento evento, Atividade... atividades) {
        return inscricaoRepository.save(new Inscrição(LocalDateTime.now(), true, participante, evento, List.of(atividades)));
    }

    // ---------- POST /api/inscricoes — janela de inscrição ----------

    @Test
    void deveInscreverQuandoDentroDaJanela() throws Exception {
        mockMvc.perform(post("/api/inscricoes")
                .header("Authorization", "Bearer " + tokenDono)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"participanteId": %d, "eventoId": %d, "atividadeIds": [%d]}
                    """.formatted(dono.getId(), eventoAberto.getId(), atividade1.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(true))
            .andExpect(jsonPath("$.atividade[0].id").value(atividade1.getId()));
    }

    @Test
    void deveRejeitarQuandoForaDaJanela() throws Exception {
        mockMvc.perform(post("/api/inscricoes")
                .header("Authorization", "Bearer " + tokenDono)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"participanteId": %d, "eventoId": %d, "atividadeIds": [%d]}
                    """.formatted(dono.getId(), eventoFechado.getId(), atividadeDoEventoFechado.getId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("O período de inscrições para este evento não está aberto."));
    }

    // ---------- PUT /api/inscricoes/{id} — troca de atividades ----------

    @Test
    void devePermitirDonoTrocarAtividades() throws Exception {
        Inscrição inscricao = criarInscricao(dono, eventoAberto, atividade1);

        mockMvc.perform(put("/api/inscricoes/" + inscricao.getId())
                .header("Authorization", "Bearer " + tokenDono)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"atividadeIds\": [" + atividade2.getId() + "]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.atividade.length()").value(1))
            .andExpect(jsonPath("$.atividade[0].id").value(atividade2.getId()));
    }

    @Test
    void deveRejeitarListaVaziaDeAtividades() throws Exception {
        Inscrição inscricao = criarInscricao(dono, eventoAberto, atividade1);

        mockMvc.perform(put("/api/inscricoes/" + inscricao.getId())
                .header("Authorization", "Bearer " + tokenDono)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"atividadeIds\": []}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Nenhuma atividade válida selecionada."));
    }

    @Test
    void deveFiltrarAtividadeDeOutroEvento() throws Exception {
        Inscrição inscricao = criarInscricao(dono, eventoAberto, atividade1);

        mockMvc.perform(put("/api/inscricoes/" + inscricao.getId())
                .header("Authorization", "Bearer " + tokenDono)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"atividadeIds\": [" + atividadeDoEventoFechado.getId() + "]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Nenhuma atividade válida selecionada."));
    }

    @Test
    void deveRejeitarQuandoNaoEhDonoNemAdmin() throws Exception {
        Inscrição inscricao = criarInscricao(dono, eventoAberto, atividade1);

        mockMvc.perform(put("/api/inscricoes/" + inscricao.getId())
                .header("Authorization", "Bearer " + tokenOutro)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"atividadeIds\": [" + atividade2.getId() + "]}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void devePermitirAdminAlterarInscricaoDeOutroUsuario() throws Exception {
        Inscrição inscricao = criarInscricao(dono, eventoAberto, atividade1);

        mockMvc.perform(put("/api/inscricoes/" + inscricao.getId())
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"atividadeIds\": [" + atividade2.getId() + "]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.atividade[0].id").value(atividade2.getId()));
    }

    @Test
    void deveRetornar404QuandoInscricaoNaoExiste() throws Exception {
        mockMvc.perform(put("/api/inscricoes/999999")
                .header("Authorization", "Bearer " + tokenDono)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"atividadeIds\": [" + atividade1.getId() + "]}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void deveRejeitarSemToken() throws Exception {
        Inscrição inscricao = criarInscricao(dono, eventoAberto, atividade1);

        mockMvc.perform(put("/api/inscricoes/" + inscricao.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"atividadeIds\": [" + atividade1.getId() + "]}"))
            .andExpect(status().isForbidden());
    }
}
