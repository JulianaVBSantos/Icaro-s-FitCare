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

## Principais funcionalidades

- Cadastro e gerenciamento de alunos;
- Consulta e edição de informações dos alunos;
- Registro de condições e restrições físicas;
- Classificação dos alunos conforme as regras de acompanhamento;
- Pesquisa e filtros para facilitar a localização de alunos;
- Visualização do perfil e do histórico de acompanhamento;
- Cadastro, consulta, edição e exclusão de treinos;
- Gerenciamento dos exercícios associados aos treinos;
- Indicadores e informações para auxiliar o profissional na organização dos atendimentos.

As funcionalidades são desenvolvidas de forma incremental, conforme os requisitos definidos para cada sprint.

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

Concluída. Configuração inicial do projeto mobile com Kotlin e Jetpack Compose, estruturação da navegação e desenvolvimento das primeiras interfaces, incluindo login, dashboard, listagem e perfil do aluno.

**Sprint 02 — Gerenciamento, persistência e qualidade**

Concluída com sucesso. Implementação da persistência local de dados utilizando Room e desenvolvimento das funcionalidades de cadastro, consulta, edição e exclusão de alunos. Também foram implementados filtros de busca, registro de condições e restrições físicas e regras iniciais de classificação para o acompanhamento dos alunos.

A sprint contemplou ainda o desenvolvimento do gerenciamento de treinos e exercícios, a integração entre telas e repositórios e a realização de testes funcionais e correções de problemas identificados durante o desenvolvimento.

**Sprint 03 — Treinamentos e acompanhamento**

Prevista para ampliar as ferramentas de acompanhamento, incluindo registro de dores, anamnese, gráficos de evolução e implementação da Central de Atenção para destacar alunos que necessitam de cuidados específicos, conforme os requisitos priorizados.

**Sprint 04 — Finalização e validação**

Prevista para a integração dos fluxos, implementação e validação do acesso de usuários com foco na proteção de dados, realização de testes integrados, correções de usabilidade e finalização do MVP.

## Status do projeto

**Sprint 02 de 4 concluída com sucesso.**

As Sprints 01 e 02 foram concluídas, estabelecendo a estrutura inicial do aplicativo e implementando as funcionalidades de gerenciamento de alunos, persistência local de dados, classificação de restrições e gerenciamento de treinos.

A Sprint 02 também contemplou testes funcionais e correções de bugs relacionados aos fluxos implementados. O projeto seguirá para a Sprint 03, com foco na ampliação dos recursos de acompanhamento dos alunos.

## Licença

O Ícaro's FitCare é um projeto de uso restrito, desenvolvido no contexto acadêmico e destinado à utilização prevista pelos responsáveis pela solução.

O código-fonte e os demais artefatos do projeto não podem ser copiados, modificados, distribuídos ou utilizados para outras finalidades sem autorização prévia dos responsáveis pelo projeto.

Consulte o arquivo `LICENSE` para obter informações sobre as condições de uso.