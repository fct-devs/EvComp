-- ============================================================================
-- local-seed.sql — seed adicional de QA, NÃO faz parte do init.sql do projeto.
--
-- Não roda automaticamente (init.sql só é lido por Docker na criação do volume).
-- Execute manualmente contra o banco já rodando:
--
--   docker exec -i evcomp-mysql mysql --default-character-set=utf8mb4 -uroot -proot evcomp < local-seed.sql
--
-- Todos os usuários abaixo compartilham a mesma senha: SecompQA2026
-- (hash bcrypt gerado e verificado contra o login real do backend rodando —
-- ver conversa; jbcrypt:0.4 usado no projeto aceita o prefixo $2a$).
--
-- Idempotência: NÃO é idempotente — como email/RA/título são UNIQUE, rodar
-- duas vezes falha com erro de chave duplicada. Isso é intencional: se algo
-- já existe, é sinal de que o seed já rodou.
--
-- ============================================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------------
-- Usuários — cobre os quatro estados de pagamento de propósito, mais
-- um ADMIN e um COLETOR extras, e um participante com e-mail externo
-- (sem RA) para variar o cadastro.
-- ------------------------------------------------------------------
INSERT INTO `usuário` (`nome_completo`, `email`, `senha_hash`, `tipo_usuario`, `ra`, `secret_seed`) VALUES
('Ana Pendente QA',        'ana.pendente.qa@unesp.br',    '$2a$10$EIEExpTsZ8yTuAVREjGmZ.RfNE.MfVz1ZpBXUbOPozNyKqeDRd9zq', 'PAR', '300000001', '63XEHTAHHRA7BWTDA2NSSNUDFUVKDFVN'),
('Bruno Aguardando QA',    'bruno.aguardando.qa@unesp.br','$2a$10$EIEExpTsZ8yTuAVREjGmZ.RfNE.MfVz1ZpBXUbOPozNyKqeDRd9zq', 'PAR', '300000002', '3AFIQXTYQLNS4JZULKD4ZLYLBORTLR7S'),
('Carla Aprovada QA',      'carla.aprovada.qa@unesp.br',  '$2a$10$EIEExpTsZ8yTuAVREjGmZ.RfNE.MfVz1ZpBXUbOPozNyKqeDRd9zq', 'PAR', '300000003', 'HOWHYT2JPDLCG2RJCS5STZSML4WBWA7V'),
('Diego Recusado QA',      'diego.recusado.qa@unesp.br',  '$2a$10$EIEExpTsZ8yTuAVREjGmZ.RfNE.MfVz1ZpBXUbOPozNyKqeDRd9zq', 'PAR', '300000004', 'N4JF7VYGMBRD2KRPVIFD6V6IA5LX6QA5'),
('Elisa Isenta QA',        'elisa.isenta.qa@unesp.br',    '$2a$10$EIEExpTsZ8yTuAVREjGmZ.RfNE.MfVz1ZpBXUbOPozNyKqeDRd9zq', 'PAR', '300000005', 'EGSA3AI6JHZHV4MWZNCJQAIX2IJPJGA7'),
('Administrador QA',       'admin.qa@unesp.br',           '$2a$10$EIEExpTsZ8yTuAVREjGmZ.RfNE.MfVz1ZpBXUbOPozNyKqeDRd9zq', 'ADM', NULL,        NULL),
('Coletor QA',             'coletor.qa@unesp.br',         '$2a$10$EIEExpTsZ8yTuAVREjGmZ.RfNE.MfVz1ZpBXUbOPozNyKqeDRd9zq', 'COL', '300000006', 'L2P77LJJZPZOIIJ5XSEWKFDDNNRQO36Y'),
('Fábio Multi QA',         'fabio.multi.qa@unesp.br',     '$2a$10$EIEExpTsZ8yTuAVREjGmZ.RfNE.MfVz1ZpBXUbOPozNyKqeDRd9zq', 'PAR', '300000007', 'IYJ3T6DKC5YCY7SIOAPWHGDPBXF5FDTJ'),
('Giovana Externa QA',     'giovana.qa@gmail.com',        '$2a$10$EIEExpTsZ8yTuAVREjGmZ.RfNE.MfVz1ZpBXUbOPozNyKqeDRd9zq', 'PAR', NULL,        'QZFG7PB3E3LEIEWTLWK374GYXST7R4RG');

-- ------------------------------------------------------------------
-- Eventos — um pago com data futura ampla, um gratuito (gera ISENTO),
-- um pago mais caro, todos com atividade própria.
-- ------------------------------------------------------------------
INSERT INTO `evento` (`titulo`, `data_inicio`, `data_termino`, `descricao`, `link`, `tipo_contabilizacao`, `chave_pix`, `data_inicio_inscricao`, `data_fim_inscricao`) VALUES
('QA - Congresso de Dados 2026', '2026-11-10', '2026-11-12', 'Evento de QA para popular a fila de pagamentos pendentes.', '', 'POR_ATIVIDADE', 'congresso.qa@unesp.br', DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 30 DAY)),
('QA - Meetup Gratuito',         '2026-09-05', '2026-09-05', 'Evento de QA gratuito, gera pagamento ISENTO.', '', 'POR_CARGA_TOTAL', NULL, DATE_SUB(CURDATE(), INTERVAL 15 DAY), DATE_ADD(CURDATE(), INTERVAL 10 DAY)),
('QA - Bootcamp Intensivo',      '2026-10-01', '2026-10-03', 'Evento de QA pago, usado para aprovação e recusa de comprovante.', '', 'POR_ATIVIDADE', '11122233344', DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 25 DAY));

-- ------------------------------------------------------------------
-- Modalidades de inscrição — Sem/Com Camiseta/Kit Completo no evento
-- pago principal, modalidade única nos outros dois, e uma modalidade
-- inativa de propósito (para testar o filtro `ativo` no admin).
-- ------------------------------------------------------------------
INSERT INTO `modalidade_inscricao` (`idEvento`, `nome`, `descricao`, `valor`, `ativo`) VALUES
((SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026'), 'Sem Camiseta',  'Acesso a todas as atividades, sem camiseta do evento.', 30.00, 1),
((SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026'), 'Com Camiseta',  'Acesso a todas as atividades, com camiseta do evento.', 50.00, 1),
((SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026'), 'Kit Completo',  'Acesso a todas as atividades, camiseta e kit de brindes.', 80.00, 1),
((SELECT idEvento FROM `evento` WHERE titulo = 'QA - Meetup Gratuito'),        'Inscrição Geral', 'Modalidade única, sem custo.', 0.00, 1),
((SELECT idEvento FROM `evento` WHERE titulo = 'QA - Bootcamp Intensivo'),    'Padrão',          NULL, 120.00, 1),
((SELECT idEvento FROM `evento` WHERE titulo = 'QA - Bootcamp Intensivo'),    'Estudante UNESP', 'Valor reduzido mediante RA válido.', 80.00, 1),
((SELECT idEvento FROM `evento` WHERE titulo = 'QA - Bootcamp Intensivo'),    'Early Bird (encerrado)', 'Modalidade promocional já encerrada — usada para testar o filtro de inativas no admin.', 90.00, 0);

INSERT INTO `atividade` (`titulo`, `data_inicio`, `data_termino`, `descricao`, `hora_inicio`, `hora_termino`, `max_participantes`, `carga_horaria_total`, `carga_horaria_ministrante`, `idEvento`) VALUES
('QA - Palestra de Abertura do Congresso', '2026-11-10', '2026-11-10', 'Descrição mockada para a palestra de abertura do congresso de QA.', '09:00:00', '12:00:00', 80, 4, 4,
  (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026')),
('QA - Oficina Gratuita de Introdução', '2026-09-05', '2026-09-05', 'Descrição mockada para a oficina gratuita de introdução.', '14:00:00', '17:00:00', 40, 3, 3,
  (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Meetup Gratuito')),
('QA - Trilha Backend', '2026-10-01', '2026-10-03', 'Descrição mockada para a trilha backend do bootcamp de QA.', '09:00:00', '18:00:00', 25, 20, 20,
  (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Bootcamp Intensivo'));

-- ------------------------------------------------------------------
-- Coletor QA vinculado aos dois eventos pagos novos.
-- ------------------------------------------------------------------
INSERT INTO `coletor_presença` (`idUsuário`, `idEvento`) VALUES
((SELECT idUsuário FROM `usuário` WHERE email = 'coletor.qa@unesp.br'), (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026')),
((SELECT idUsuário FROM `usuário` WHERE email = 'coletor.qa@unesp.br'), (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Bootcamp Intensivo'));

-- ------------------------------------------------------------------
-- Inscrições. Fábio se inscreve em três eventos (um deles já existente
-- no init.sql) para a aba "Meus Pagamentos" mostrar vários cards.
--
-- `status` acompanha o `status_pagamento` de cada um (ver INSERT INTO
-- `pagamento` abaixo): 0 para PENDENTE/RECUSADO em evento pago (ingresso
-- bloqueado), 1 para APROVADO/ISENTO (ingresso liberado).
-- ------------------------------------------------------------------
INSERT INTO `inscrição` (`data_inscricao`, `status`, `idUsuário`, `idEvento`, `idModalidade`, `valor_aplicado`) VALUES
(NOW(), 0, (SELECT idUsuário FROM `usuário` WHERE email = 'ana.pendente.qa@unesp.br'),
      (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026'),
      (SELECT idModalidadeInscricao FROM `modalidade_inscricao` WHERE idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026') AND nome = 'Com Camiseta'),
      50.00),
(NOW(), 0, (SELECT idUsuário FROM `usuário` WHERE email = 'bruno.aguardando.qa@unesp.br'),
      (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026'),
      (SELECT idModalidadeInscricao FROM `modalidade_inscricao` WHERE idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026') AND nome = 'Kit Completo'),
      80.00),
(NOW(), 1, (SELECT idUsuário FROM `usuário` WHERE email = 'carla.aprovada.qa@unesp.br'),
      (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026'),
      (SELECT idModalidadeInscricao FROM `modalidade_inscricao` WHERE idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026') AND nome = 'Sem Camiseta'),
      30.00),
(NOW(), 0, (SELECT idUsuário FROM `usuário` WHERE email = 'diego.recusado.qa@unesp.br'),
      (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Bootcamp Intensivo'),
      (SELECT idModalidadeInscricao FROM `modalidade_inscricao` WHERE idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Bootcamp Intensivo') AND nome = 'Estudante UNESP'),
      80.00),
(NOW(), 1, (SELECT idUsuário FROM `usuário` WHERE email = 'elisa.isenta.qa@unesp.br'),
      (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Meetup Gratuito'),
      (SELECT idModalidadeInscricao FROM `modalidade_inscricao` WHERE idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Meetup Gratuito') AND nome = 'Inscrição Geral'),
      0.00),
(NOW(), 0, (SELECT idUsuário FROM `usuário` WHERE email = 'fabio.multi.qa@unesp.br'),
      (SELECT idEvento FROM `evento` WHERE titulo = 'Semana da Computação 2027'),
      (SELECT idModalidadeInscricao FROM `modalidade_inscricao` WHERE idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'Semana da Computação 2027') AND nome = 'Padrão'),
      40.00),
(NOW(), 0, (SELECT idUsuário FROM `usuário` WHERE email = 'fabio.multi.qa@unesp.br'),
      (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026'),
      (SELECT idModalidadeInscricao FROM `modalidade_inscricao` WHERE idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026') AND nome = 'Sem Camiseta'),
      30.00),
(NOW(), 1, (SELECT idUsuário FROM `usuário` WHERE email = 'fabio.multi.qa@unesp.br'),
      (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Bootcamp Intensivo'),
      (SELECT idModalidadeInscricao FROM `modalidade_inscricao` WHERE idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Bootcamp Intensivo') AND nome = 'Padrão'),
      120.00),
(NOW(), 0, (SELECT idUsuário FROM `usuário` WHERE email = 'giovana.qa@gmail.com'),
      (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Bootcamp Intensivo'),
      (SELECT idModalidadeInscricao FROM `modalidade_inscricao` WHERE idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Bootcamp Intensivo') AND nome = 'Estudante UNESP'),
      80.00);

INSERT INTO `inscrição_atividade` (`idInscrição`, `idAtividade`) VALUES
((SELECT i.idInscrição FROM `inscrição` i WHERE i.idUsuário = (SELECT idUsuário FROM `usuário` WHERE email = 'ana.pendente.qa@unesp.br') AND i.idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026')),
 (SELECT idAtividade FROM `atividade` WHERE titulo = 'QA - Palestra de Abertura do Congresso')),
((SELECT i.idInscrição FROM `inscrição` i WHERE i.idUsuário = (SELECT idUsuário FROM `usuário` WHERE email = 'bruno.aguardando.qa@unesp.br') AND i.idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026')),
 (SELECT idAtividade FROM `atividade` WHERE titulo = 'QA - Palestra de Abertura do Congresso')),
((SELECT i.idInscrição FROM `inscrição` i WHERE i.idUsuário = (SELECT idUsuário FROM `usuário` WHERE email = 'carla.aprovada.qa@unesp.br') AND i.idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026')),
 (SELECT idAtividade FROM `atividade` WHERE titulo = 'QA - Palestra de Abertura do Congresso')),
((SELECT i.idInscrição FROM `inscrição` i WHERE i.idUsuário = (SELECT idUsuário FROM `usuário` WHERE email = 'diego.recusado.qa@unesp.br') AND i.idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Bootcamp Intensivo')),
 (SELECT idAtividade FROM `atividade` WHERE titulo = 'QA - Trilha Backend')),
((SELECT i.idInscrição FROM `inscrição` i WHERE i.idUsuário = (SELECT idUsuário FROM `usuário` WHERE email = 'elisa.isenta.qa@unesp.br') AND i.idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Meetup Gratuito')),
 (SELECT idAtividade FROM `atividade` WHERE titulo = 'QA - Oficina Gratuita de Introdução')),
((SELECT i.idInscrição FROM `inscrição` i WHERE i.idUsuário = (SELECT idUsuário FROM `usuário` WHERE email = 'fabio.multi.qa@unesp.br') AND i.idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'Semana da Computação 2027')),
 (SELECT idAtividade FROM `atividade` WHERE titulo = 'Palestra de Abertura')),
((SELECT i.idInscrição FROM `inscrição` i WHERE i.idUsuário = (SELECT idUsuário FROM `usuário` WHERE email = 'fabio.multi.qa@unesp.br') AND i.idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026')),
 (SELECT idAtividade FROM `atividade` WHERE titulo = 'QA - Palestra de Abertura do Congresso')),
((SELECT i.idInscrição FROM `inscrição` i WHERE i.idUsuário = (SELECT idUsuário FROM `usuário` WHERE email = 'fabio.multi.qa@unesp.br') AND i.idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Bootcamp Intensivo')),
 (SELECT idAtividade FROM `atividade` WHERE titulo = 'QA - Trilha Backend')),
((SELECT i.idInscrição FROM `inscrição` i WHERE i.idUsuário = (SELECT idUsuário FROM `usuário` WHERE email = 'giovana.qa@gmail.com') AND i.idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Bootcamp Intensivo')),
 (SELECT idAtividade FROM `atividade` WHERE titulo = 'QA - Trilha Backend'));

-- ------------------------------------------------------------------
-- Comprovantes fake (mesmo PNG 1x1 do init.sql) para os pagamentos que
-- já nascem com arquivo enviado. Um INSERT por vez para poder capturar
-- o LAST_INSERT_ID() de cada blob em variável de sessão.
-- ------------------------------------------------------------------
INSERT INTO `comprovante_blob` (`conteudo`) VALUES
(UNHEX('89504E470D0A1A0A0000000D49484452000000010000000108060000001F15C4890000000A49444154789C63000100000500010D0A2DB40000000049454E44AE426082'));
SET @blob_bruno = LAST_INSERT_ID();

INSERT INTO `comprovante_blob` (`conteudo`) VALUES
(UNHEX('89504E470D0A1A0A0000000D49484452000000010000000108060000001F15C4890000000A49444154789C63000100000500010D0A2DB40000000049454E44AE426082'));
SET @blob_carla = LAST_INSERT_ID();

INSERT INTO `comprovante_blob` (`conteudo`) VALUES
(UNHEX('89504E470D0A1A0A0000000D49484452000000010000000108060000001F15C4890000000A49444154789C63000100000500010D0A2DB40000000049454E44AE426082'));
SET @blob_diego = LAST_INSERT_ID();

INSERT INTO `comprovante_blob` (`conteudo`) VALUES
(UNHEX('89504E470D0A1A0A0000000D49484452000000010000000108060000001F15C4890000000A49444154789C63000100000500010D0A2DB40000000049454E44AE426082'));
SET @blob_fabio = LAST_INSERT_ID();

INSERT INTO `comprovante_blob` (`conteudo`) VALUES
(UNHEX('89504E470D0A1A0A0000000D49484452000000010000000108060000001F15C4890000000A49444154789C63000100000500010D0A2DB40000000049454E44AE426082'));
SET @blob_fabio_bootcamp = LAST_INSERT_ID();

INSERT INTO `comprovante_blob` (`conteudo`) VALUES
(UNHEX('89504E470D0A1A0A0000000D49484452000000010000000108060000001F15C4890000000A49444154789C63000100000500010D0A2DB40000000049454E44AE426082'));
SET @blob_giovana = LAST_INSERT_ID();

-- ------------------------------------------------------------------
-- Pagamentos — cobre os quatro status, com ênfase em PENDENTE +
-- comprovante já enviado (para a fila do admin não nascer vazia).
-- ------------------------------------------------------------------
INSERT INTO `pagamento` (`idInscrição`, `status_pagamento`, `armazenamento_tipo`, `armazenamento_ref`, `nome_arquivo_original`, `tipo_arquivo`, `tamanho_arquivo`, `data_envio`, `data_avaliacao`, `idUsuário_avaliador`, `motivo_recusa`) VALUES
-- Ana: PENDENTE sem comprovante — para testar o fluxo de upload do zero.
((SELECT i.idInscrição FROM `inscrição` i WHERE i.idUsuário = (SELECT idUsuário FROM `usuário` WHERE email = 'ana.pendente.qa@unesp.br') AND i.idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026')),
 'PENDENTE', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

-- Bruno: PENDENTE com comprovante — aparece na fila /pendentes do admin.
((SELECT i.idInscrição FROM `inscrição` i WHERE i.idUsuário = (SELECT idUsuário FROM `usuário` WHERE email = 'bruno.aguardando.qa@unesp.br') AND i.idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026')),
 'PENDENTE', 'BANCO', @blob_bruno, 'comprovante-bruno.png', 'image/png', 70, NOW(), NULL, NULL, NULL),

-- Carla: já APROVADO.
((SELECT i.idInscrição FROM `inscrição` i WHERE i.idUsuário = (SELECT idUsuário FROM `usuário` WHERE email = 'carla.aprovada.qa@unesp.br') AND i.idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026')),
 'APROVADO', 'BANCO', @blob_carla, 'comprovante-carla.png', 'image/png', 70, NOW(), NOW(), (SELECT idUsuário FROM `usuário` WHERE email = 'admin.qa@unesp.br'), NULL),

-- Diego: RECUSADO — para testar o reenvio pelo aluno.
((SELECT i.idInscrição FROM `inscrição` i WHERE i.idUsuário = (SELECT idUsuário FROM `usuário` WHERE email = 'diego.recusado.qa@unesp.br') AND i.idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Bootcamp Intensivo')),
 'RECUSADO', 'BANCO', @blob_diego, 'comprovante-diego.png', 'image/png', 70, NOW(), NOW(), (SELECT idUsuário FROM `usuário` WHERE email = 'admin.qa@unesp.br'), 'Comprovante ilegível: o valor transferido não está visível.'),

-- Elisa: ISENTO (evento gratuito).
((SELECT i.idInscrição FROM `inscrição` i WHERE i.idUsuário = (SELECT idUsuário FROM `usuário` WHERE email = 'elisa.isenta.qa@unesp.br') AND i.idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Meetup Gratuito')),
 'ISENTO', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

-- Fábio: três inscrições, três status diferentes, para a aba dele mostrar vários cards.
((SELECT i.idInscrição FROM `inscrição` i WHERE i.idUsuário = (SELECT idUsuário FROM `usuário` WHERE email = 'fabio.multi.qa@unesp.br') AND i.idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'Semana da Computação 2027')),
 'PENDENTE', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
((SELECT i.idInscrição FROM `inscrição` i WHERE i.idUsuário = (SELECT idUsuário FROM `usuário` WHERE email = 'fabio.multi.qa@unesp.br') AND i.idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Congresso de Dados 2026')),
 'PENDENTE', 'BANCO', @blob_fabio, 'comprovante-fabio.png', 'image/png', 70, NOW(), NULL, NULL, NULL),
((SELECT i.idInscrição FROM `inscrição` i WHERE i.idUsuário = (SELECT idUsuário FROM `usuário` WHERE email = 'fabio.multi.qa@unesp.br') AND i.idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Bootcamp Intensivo')),
 'APROVADO', 'BANCO', @blob_fabio_bootcamp, 'comprovante-fabio-bootcamp.png', 'image/png', 70, NOW(), NOW(), (SELECT idUsuário FROM `usuário` WHERE email = 'admin.qa@unesp.br'), NULL),

-- Giovana: PENDENTE com comprovante — mais um item na fila do admin.
((SELECT i.idInscrição FROM `inscrição` i WHERE i.idUsuário = (SELECT idUsuário FROM `usuário` WHERE email = 'giovana.qa@gmail.com') AND i.idEvento = (SELECT idEvento FROM `evento` WHERE titulo = 'QA - Bootcamp Intensivo')),
 'PENDENTE', 'BANCO', @blob_giovana, 'comprovante-giovana.png', 'image/png', 70, NOW(), NULL, NULL, NULL);
