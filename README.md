<div align="center">
	<h1>EvComp (Backend)
		<h4>API Restful para o sistema de gestão e eventos computacionais (EvComp).</h4>
</div>

---

Este repositório contém o código-fonte do Backend do sistema **EvComp**, construído em **Java (Spring Boot)**. O sistema é responsável por toda a regra de negócio, gestão de usuários, eventos, emissão de certificados, controle de presenças e relatórios, além de prover o banco de dados via **MySQL**.

## Tecnologias a serem Instaladas

Para rodar o projeto localmente da forma mais fácil e limpa possível, você só precisa ter instalado na sua máquina:

- [Docker](https://www.docker.com/products/docker-desktop)
- [Docker Compose](https://docs.docker.com/compose/install/)


## Como Rodar o Projeto

Este projeto utiliza containers Docker para garantir que o ambiente seja idêntico em qualquer máquina. 

Para ligar a API e o Banco de Dados, siga os seguintes passos:

1. Abra o terminal e navegue até a pasta raiz deste repositório (`EvComp`).
2. Execute o comando de inicialização do Docker Compose:

   ```bash
   docker compose up -d --build
   ```

3. O Docker fará o download das imagens, criará o banco de dados MySQL na porta `3307` e iniciará a API Spring Boot na porta `8080`.
4. Os dados iniciais de teste (usuários, eventos mockados) serão criados automaticamente graças ao script `init.sql` embutido.

> **Atenção sobre a Ordem de Execução:** 
> O Backend **DEVE** estar rodando antes que você inicie o Frontend (`EvComp-Front`). O Frontend depende da rede interna deste repositório (`evcomp_default`) para se conectar à API de forma correta!

### Armazenamento dos comprovantes de pagamento

O módulo de pagamento guarda o comprovante enviado pelo participante através de uma estratégia
configurável, definida por `pagamento.armazenamento`:

| Valor | Onde grava | Observação |
|---|---|---|
| `BANCO` (padrão) | BLOB na tabela `comprovante_blob` | Persiste no volume `evcomp_db_data` e sai junto no dump do banco. Não exige nenhuma configuração extra. |
| `DISCO` | `uploads/comprovantes/` | **Exige mapear um volume** (veja abaixo), senão os arquivos somem a cada rebuild. |
| `S3` | — | Ainda não implementado; falha na inicialização do upload. |

Para usar `DISCO`, defina `PAGAMENTO_ARMAZENAMENTO=DISCO` e acrescente o volume ao serviço `api`
no `docker-compose.yml` — o contêiner da API não tem volume por padrão e seu sistema de arquivos
é descartado a cada `docker compose up --build`:

```yaml
  api:
    volumes:
      - ./uploads:/app/uploads
```

Comprovantes já enviados continuam legíveis mesmo depois de trocar a estratégia: cada pagamento
registra em qual armazenamento seu arquivo foi gravado. O limite é de **1 MB por comprovante**
(WebP, JPEG, PNG ou PDF).

## Estrutura do Repositório

- **src/**: Código-fonte da aplicação Java (Controllers, Services, Repositories e Domains).
- **init.sql**: Script com os dados falsos e estrutura inicial do banco de dados (SEED) usado para testes.
- **docker-compose.yml**: Orquestração dos containers da API e do MySQL.
- **Dockerfile**: Instruções de montagem da imagem do Backend.
- **build.gradle**: Gerenciador de pacotes e dependências do projeto.

## Author ✨

<table>
	<tr>
		<td align="center">
			<a href="https://github.com/Gabriel-Ciriaco">
				<img src="https://avatars.githubusercontent.com/u/66225865" width="100px;" alt=""/>
				<br>
				<sub>
					<b>Gabriel C. de Carvalho</b>
				</sub>
		</td>
		<td align="center">
			<a href="https://github.com/Carol-Nunes">
				<img src="https://avatars.githubusercontent.com/u/18383333" width="100px;" alt=""/>
				<br>
				<sub>
					<b>Caroline N. Araujo</b>
				</sub>
		</td>
	</tr>
</table>

## Contribuição

Se você quiser contribuir para este projeto, sinta-se à vontade para fazer um fork, enviar um pull request com suas melhorias ou abrir uma *issue*, caso tenha alguma dúvida ou sugestão!

## Licença

Este projeto está licenciado sob a Licença GNU General Public License v3.0 - veja o arquivo [LICENSE](LICENSE) para mais detalhes.