# Ícaro's FitCare

Aplicativo mobile voltado ao acompanhamento de alunos durante a prática de exercícios físicos, com foco em pessoas que apresentam necessidades específicas, como limitações decorrentes de lesões, condições físicas ou períodos de recuperação.

## Sobre o projeto

O **Ícaro's FitCare** é uma solução desenvolvida por meio de um projeto extensionista para auxiliar o profissional de Educação Física no acompanhamento individualizado de seus alunos.

A proposta é centralizar informações relevantes sobre cada aluno e facilitar o planejamento e o acompanhamento dos treinamentos, considerando características, restrições, observações e registros relacionados à evolução durante o processo de acompanhamento.

O aplicativo busca contribuir para um atendimento mais organizado e personalizado, auxiliando o profissional na tomada de decisões relacionadas ao acompanhamento dos alunos.

## Objetivo

Desenvolver uma solução mobile que facilite o gerenciamento das informações dos alunos e o acompanhamento de seus treinamentos, especialmente daqueles que possuem necessidades específicas ou estão em processo de recuperação física.

## Público-alvo

- Profissionais de Educação Física;
- Alunos acompanhados pelo profissional;
- Pessoas que necessitam de acompanhamento de exercícios adaptado às suas condições e limitações.

## Principais funcionalidades previstas

- Cadastro e gerenciamento de alunos;
- Registro de informações e observações sobre os alunos;
- Registro de condições e restrições físicas;
- Acompanhamento de treinamentos;
- Registro de relatos de dor e ocorrências;
- Histórico de acompanhamento;
- Organização das informações para auxiliar o profissional durante os atendimentos.

As funcionalidades poderão ser ajustadas conforme o levantamento de requisitos e a evolução do projeto durante as sprints.

## Tecnologias

O Ícaro's FitCare está sendo desenvolvido como um aplicativo mobile para a plataforma Android, utilizando:

- **Android Studio:** ambiente de desenvolvimento;
- **Kotlin:** linguagem de programação;
- **Jetpack Compose:** construção da interface;
- **Room:** persistência local de dados;
- **Figma:** planejamento e prototipação das interfaces;
- **Git e GitHub:** versionamento do código-fonte e acompanhamento da evolução do projeto.

## Desenvolvimento

O projeto está organizado em quatro sprints, com desenvolvimento incremental e acompanhamento das atividades por meio de um quadro Kanban e do histórico de commits no GitHub.

### Planejamento das sprints

**Sprint 01 — Estrutura inicial e autenticação**

Configuração inicial do projeto mobile (Kotlin/Jetpack Compose), estruturação da navegação e desenvolvimento das primeiras interfaces, incluindo login, dashboard, listagem e perfil do aluno.

**Sprint 02 — Gerenciamento e persistência de alunos**

Implementação da persistência local de dados e das funcionalidades do CRUD de alunos (cadastro, consulta, edição e exclusão), além de filtros de busca e lógica inicial de classificação de restrições dos alunos.

**Sprint 03 — Treinamentos e acompanhamento**

Desenvolvimento das ferramentas de acompanhamento, como registro de dores, anamnese, gráficos de evolução e implementação da Central de Atenção para destacar alunos que necessitam de cuidados específicos.

**Sprint 04 — Finalização e validação**

Integração de todos os fluxos, implementação e validação do acesso de usuários com foco em proteção de dados, realização de testes integrados, correções de usabilidade e fechamento do MVP.

## Status

**Em desenvolvimento — Sprint 02 de 4.**

A Sprint 01 foi concluída, estabelecendo a estrutura inicial do aplicativo e suas primeiras interfaces. Atualmente, o projeto está na Sprint 02, com foco no gerenciamento dos alunos, na persistência local de dados utilizando Room e na implementação do cadastro com suas respectivas validações.

As funcionalidades das sprints seguintes serão desenvolvidas de forma incremental, de acordo com o planejamento e a priorização dos requisitos.

## Licença

O Ícaro's FitCare é um projeto de uso restrito, desenvolvido no contexto acadêmico e destinado à utilização prevista pelos responsáveis pela solução.

O código-fonte e os demais artefatos do projeto não podem ser copiados, modificados, distribuídos ou utilizados para outras finalidades sem autorização prévia dos responsáveis pelo projeto.

Consulte o arquivo `LICENSE` para obter informações sobre as condições de uso.
