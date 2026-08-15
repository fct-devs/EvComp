-- MySQL dump formatado para testes EvComp
SET NAMES utf8mb4;
SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT;
SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS;
SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION;
SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO';

-- ------------------------------------------------------
-- DDL - CRIAÇÃO DE TABELAS
-- ------------------------------------------------------

DROP TABLE IF EXISTS `usuário`;
CREATE TABLE `usuário` (
  `idUsuário` int NOT NULL AUTO_INCREMENT,
  `nome_completo` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `senha_hash` varchar(60) DEFAULT NULL,
  `ra` char(9) DEFAULT NULL,
  `tipo_usuario` char(3) NOT NULL,
  `secret_seed` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`idUsuário`),
  UNIQUE KEY `email_UNIQUE` (`email`),
  UNIQUE KEY `ra_UNIQUE` (`ra`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

DROP TABLE IF EXISTS `evento`;
CREATE TABLE `evento` (
  `idEvento` int NOT NULL AUTO_INCREMENT,
  `titulo` varchar(155) NOT NULL,
  `data_inicio` date NOT NULL,
  `data_termino` date NOT NULL,
  `descricao` text NOT NULL,
  `link` varchar(255) DEFAULT NULL,
  `tipo_contabilizacao` varchar(20) NOT NULL,
  `data_inicio_inscricao` date NOT NULL,
  `data_fim_inscricao` date NOT NULL,
  PRIMARY KEY (`idEvento`),
  UNIQUE KEY `titulo_UNIQUE` (`titulo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

DROP TABLE IF EXISTS `atividade`;
CREATE TABLE `atividade` (
  `idAtividade` int NOT NULL AUTO_INCREMENT,
  `titulo` varchar(155) NOT NULL,
  `data_inicio` date NOT NULL,
  `data_termino` date NOT NULL,
  `descricao` text NOT NULL,
  `pre_requisitos` text,
  `hora_inicio` time NOT NULL,
  `hora_termino` time NOT NULL,
  `max_participantes` smallint(3) unsigned zerofill NOT NULL,
  `carga_horaria_total` smallint(2) unsigned zerofill NOT NULL,
  `carga_horaria_ministrante` smallint(2) unsigned zerofill DEFAULT NULL,
  `idEvento` int NOT NULL,
  PRIMARY KEY (`idAtividade`),
  KEY `fk_atividade_evento` (`idEvento`),
  CONSTRAINT `fk_atividade_evento` FOREIGN KEY (`idEvento`) REFERENCES `evento` (`idEvento`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

DROP TABLE IF EXISTS `inscrição`;
CREATE TABLE `inscrição` (
  `idInscrição` int NOT NULL AUTO_INCREMENT,
  `idEvento` int NOT NULL,
  `idUsuário` int NOT NULL,
  `data_inscricao` datetime NOT NULL,
  `status` tinyint(1) NOT NULL,
  PRIMARY KEY (`idInscrição`),
  UNIQUE KEY `uk_usuario_evento` (`idEvento`,`idUsuário`),
  KEY `fk_Inscrição_Evento_idx` (`idEvento`),
  KEY `fk_Inscrição_Usuário1_idx` (`idUsuário`),
  CONSTRAINT `fk_Inscrição_Evento` FOREIGN KEY (`idEvento`) REFERENCES `evento` (`idEvento`) ON DELETE CASCADE,
  CONSTRAINT `fk_Inscrição_Usuário1` FOREIGN KEY (`idUsuário`) REFERENCES `usuário` (`idUsuário`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

DROP TABLE IF EXISTS `inscrição_atividade`;
CREATE TABLE `inscrição_atividade` (
  `idInscrição` int NOT NULL,
  `idAtividade` int NOT NULL,
  PRIMARY KEY (`idInscrição`,`idAtividade`),
  KEY `fk_Atividade_has_Inscrição_Inscrição1_idx` (`idInscrição`),
  KEY `fk_Atividade_has_Inscrição_Atividade1_idx` (`idAtividade`),
  CONSTRAINT `fk_Atividade_has_Inscrição_Atividade1` FOREIGN KEY (`idAtividade`) REFERENCES `atividade` (`idAtividade`) ON DELETE CASCADE,
  CONSTRAINT `fk_Atividade_has_Inscrição_Inscrição1` FOREIGN KEY (`idInscrição`) REFERENCES `inscrição` (`idInscrição`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

DROP TABLE IF EXISTS `ministrante_atividade`;
CREATE TABLE `ministrante_atividade` (
  `idUsuário` int NOT NULL,
  `idAtividade` int NOT NULL,
  PRIMARY KEY (`idUsuário`,`idAtividade`),
  KEY `fk_Usuário_has_Atividade_Atividade1_idx` (`idAtividade`),
  KEY `fk_Usuário_has_Atividade_Usuário1_idx` (`idUsuário`),
  CONSTRAINT `fk_Usuário_has_Atividade_Atividade1` FOREIGN KEY (`idAtividade`) REFERENCES `atividade` (`idAtividade`) ON DELETE CASCADE,
  CONSTRAINT `fk_Usuário_has_Atividade_Usuário1` FOREIGN KEY (`idUsuário`) REFERENCES `usuário` (`idUsuário`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

DROP TABLE IF EXISTS `presença`;
CREATE TABLE `presença` (
  `idPresença` int NOT NULL AUTO_INCREMENT,
  `idAtividade` int NOT NULL,
  `idUsuário` int NOT NULL,
  `data_registro` datetime NOT NULL,
  `presente` tinyint(1) NOT NULL,
  PRIMARY KEY (`idPresença`,`idAtividade`),
  UNIQUE KEY `uk_usuario_presenca` (`idUsuário`,`idAtividade`),
  KEY `fk_Presença_Usuário1_idx` (`idUsuário`),
  KEY `fk_Presença_Atividade1_idx` (`idAtividade`),
  CONSTRAINT `fk_Presença_Atividade1` FOREIGN KEY (`idAtividade`) REFERENCES `atividade` (`idAtividade`) ON DELETE CASCADE,
  CONSTRAINT `fk_Presença_Usuário1` FOREIGN KEY (`idUsuário`) REFERENCES `usuário` (`idUsuário`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

DROP TABLE IF EXISTS `coletor_presença`;
CREATE TABLE `coletor_presença` (
  `idUsuário` int NOT NULL,
  `idEvento` int NOT NULL,
  PRIMARY KEY (`idUsuário`,`idEvento`),
  UNIQUE KEY `uk_usuario_coletor` (`idUsuário`,`idEvento`),
  KEY `fk_Usuário_has_Evento_Evento1_idx` (`idEvento`),
  KEY `fk_Usuário_has_Evento_Usuário1_idx` (`idUsuário`),
  CONSTRAINT `fk_Usuário_has_Evento_Evento1` FOREIGN KEY (`idEvento`) REFERENCES `evento` (`idEvento`) ON DELETE CASCADE,
  CONSTRAINT `fk_Usuário_has_Evento_Usuário1` FOREIGN KEY (`idUsuário`) REFERENCES `usuário` (`idUsuário`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

DROP TABLE IF EXISTS `certificado`;
CREATE TABLE `certificado` (
  `idCertificado` int NOT NULL AUTO_INCREMENT,
  `idAtividade` int NOT NULL,
  `idUsuário` int NOT NULL,
  `pdf_path` varchar(255) DEFAULT NULL,
  `data_emissao` datetime NOT NULL,
  `percentual_presenca` float NOT NULL,
  `tipo_certificado` varchar(255) NOT NULL,
  PRIMARY KEY (`idCertificado`),
  UNIQUE KEY `idCertificado_UNIQUE` (`idCertificado`),
  UNIQUE KEY `uk_usuario_certificado` (`idUsuário`,`idAtividade`),
  KEY `fk_Certificado_Atividade1_idx` (`idAtividade`),
  KEY `fk_Certificado_Usuário1_idx` (`idUsuário`),
  CONSTRAINT `fk_Certificado_Atividade1` FOREIGN KEY (`idAtividade`) REFERENCES `atividade` (`idAtividade`) ON DELETE CASCADE,
  CONSTRAINT `fk_Certificado_Usuário1` FOREIGN KEY (`idUsuário`) REFERENCES `usuário` (`idUsuário`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

-- ------------------------------------------------------
-- DML - INSERÇÃO DE DADOS MOCK (SEED)
-- ------------------------------------------------------

LOCK TABLES `usuário` WRITE;
INSERT INTO `usuário` (`idUsuário`, `nome_completo`, `email`, `senha_hash`, `tipo_usuario`, `ra`, `secret_seed`) VALUES
(1, 'Administrador do Sistema', 'admin@unesp.br', '$2a$10$/xybksYlPCvFzAhZkY940.AsbmDu2J1IiJwZdjHHnc396NONjiUJW', 'ADM', NULL, NULL),
(2, 'João da Silva', 'joao@unesp.br', '$2a$10$/xybksYlPCvFzAhZkY940.AsbmDu2J1IiJwZdjHHnc396NONjiUJW', 'PAR', '123456789', 'JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP'),
(3, 'Maria Coletora', 'maria.coletora@unesp.br', '$2a$10$/xybksYlPCvFzAhZkY940.AsbmDu2J1IiJwZdjHHnc396NONjiUJW', 'COL', '987654321', 'KRSXG5DSNFXGOIDBNZSXIYLNMVZXI2LO'),
(4, 'Carlos Professor', 'carlos.prof@unesp.br', '$2a$10$/xybksYlPCvFzAhZkY940.AsbmDu2J1IiJwZdjHHnc396NONjiUJW', 'PAR', '241250676', 'PX5ZS6FECPJRQRQNOYDE2YD5O35DVTZI');
UNLOCK TABLES;

LOCK TABLES `evento` WRITE;
INSERT INTO `evento` (`idEvento`, `titulo`, `data_inicio`, `data_termino`, `descricao`, `link`, `tipo_contabilizacao`, `data_inicio_inscricao`, `data_fim_inscricao`) VALUES
(1, 'Semana da Computação 2027', '2027-08-10', '2027-08-15', 'Evento Futuro para testes de Inscrição e Gestão.', 'http://secomp2027.com.br', 'POR_ATIVIDADE', DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 30 DAY)),
(2, 'Workshop de IA 2025', '2025-05-10', '2025-05-12', 'Evento Passado para testes de Certificados e Relatórios.', '', 'POR_CARGA_TOTAL', '2025-04-01', '2025-05-08'),
(3, 'Hackathon de Testes', CURDATE(), CURDATE(), 'Evento Ocorrendo Hoje para testar Registro de Presença com Coletor.', '', 'POR_ATIVIDADE', DATE_SUB(CURDATE(), INTERVAL 7 DAY), CURDATE());
UNLOCK TABLES;

LOCK TABLES `atividade` WRITE;
INSERT INTO `atividade` (`idAtividade`, `titulo`, `data_inicio`, `data_termino`, `descricao`, `pre_requisitos`, `hora_inicio`, `hora_termino`, `max_participantes`, `carga_horaria_total`, `carga_horaria_ministrante`, `idEvento`) VALUES(1, 'Palestra de Abertura', '2027-08-10', '2027-08-10', 'Descrição mockada para a palestra de abertura.', NULL, '08:00:00', '12:00:00', 50, 4, 4, 1),
(2, 'Minicurso de Python', '2025-05-10', '2025-05-10', 'Aprenda do zero a linguagem Python.', 'Noções de lógica de programação.', '14:00:00', '18:00:00', 30, 4, 8, 2),
(3, 'Maratona de Programação', CURDATE(), CURDATE(), 'Competição intensiva de resolução de problemas algorítmicos.', 'Saber programar em C, C++, Java ou Python.', '00:00:00', '23:59:00', 100, 10, 10, 3);UNLOCK TABLES;

LOCK TABLES `ministrante_atividade` WRITE;
INSERT INTO `ministrante_atividade` (`idUsuário`, `idAtividade`) VALUES
(4, 2);
UNLOCK TABLES;

LOCK TABLES `inscrição` WRITE;
INSERT INTO `inscrição` (`idInscrição`, `data_inscricao`, `status`, `idUsuário`, `idEvento`) VALUES
(1, NOW(), 1, 2, 2),
(2, NOW(), 1, 2, 3);
UNLOCK TABLES;

LOCK TABLES `inscrição_atividade` WRITE;
INSERT INTO `inscrição_atividade` (`idInscrição`, `idAtividade`) VALUES
(1, 2),
(2, 3);
UNLOCK TABLES;

LOCK TABLES `coletor_presença` WRITE;
INSERT INTO `coletor_presença` (`idUsuário`, `idEvento`) VALUES
(3, 1),
(3, 3);
UNLOCK TABLES;

LOCK TABLES `presença` WRITE;
INSERT INTO `presença` (`idPresença`, `idAtividade`, `idUsuário`, `data_registro`, `presente`) VALUES
(1, 2, 2, '2025-05-10 15:00:00', 1);
UNLOCK TABLES;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
