package br.unesp.fct.evcomp.controller;

import br.unesp.fct.evcomp.config.JwtUtil;
import br.unesp.fct.evcomp.domain.Administrador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.unesp.fct.evcomp.repository.UsuarioRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre os novos campos dataInicioInscricao/dataFimInscricao no POST /api/eventos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventoControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private String tokenAdmin;

    @BeforeEach
    void setUp() {
        Administrador admin = (Administrador) usuarioRepository.save(new Administrador("Admin", "admin@teste.com", null));
        tokenAdmin = jwtUtil.gerarToken(admin.getId(), admin.getEmail(), admin.getNomeCompleto(), "ADMIN", false);
    }

    @Test
    void deveCriarEventoComCamposDeInscricao() throws Exception {
        mockMvc.perform(post("/api/eventos")
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "titulo": "Evento de Teste",
                      "descricao": "descricao",
                      "link": "",
                      "tipoContabilizacao": "POR_ATIVIDADE",
                      "dataInicio": "2027-01-10",
                      "dataTermino": "2027-01-12",
                      "dataInicioInscricao": "2026-01-01",
                      "dataFimInscricao": "2027-01-05"
                    }
                    """))
            .andExpect(status().isOk());
    }

    @Test
    void deveRejeitarCriacaoSemCamposDeInscricao() throws Exception {
        mockMvc.perform(post("/api/eventos")
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "titulo": "Evento Sem Campos Novos",
                      "descricao": "descricao",
                      "link": "",
                      "tipoContabilizacao": "POR_ATIVIDADE",
                      "dataInicio": "2027-02-10",
                      "dataTermino": "2027-02-12"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }
}
