# Ícaro's FitCare

Aplicativo mobile voltado ao acompanhamento individualizado de alunos durante a prática de exercícios físicos, com foco em pessoas que apresentam necessidades específicas, como limitações decorrentes de lesões, condições clínicas ou períodos de recuperação pós-operatória.

## Sobre o projeto

O **Ícaro's FitCare** é uma solução mobile desenvolvida por meio de um projeto de extensão universitária para auxiliar o profissional de Educação Física na gestão integrada de seus atendimentos remotos e presenciais.

A proposta centraliza as informações cadastrais, clínicas e de treinamento de cada aluno, substituindo controles antes dispersos entre planilhas e aplicativos de mensagens. Com acesso multiperfil sincronizado em nuvem, a solução permite ao profissional planejar treinos estruturados, acompanhar a evolução histórica e identificar prioridades de atenção, enquanto o próprio aluno participa ativamente respondendo anamneses digitais, anexando documentos médicos e consultando suas rotinas.

## Objetivo

Desenvolver uma solução mobile para Android que facilite o gerenciamento de alunos, a prescrição estruturada de treinamentos e o acompanhamento clínico individualizado, apoiando a tomada de decisão do profissional e a adesão dos atendidos.

## Perfis de acesso e público-alvo

- **Administrador da Plataforma:** gerencia a autorização, o bloqueio e o monitoramento de dispositivos das contas profissionais;
- **Profissional de Educação Física:** gerencia alunos, prescreve treinos com vídeos e periodização, atribui anamneses, registra ocorrências na linha do tempo e monitora alertas da Central de Atenção;
- **Alunos:** preenchem anamneses digitais obrigatórias, anexam exames médicos e fotos de comparação física e acompanham seus treinos sincronizados.

## Principais funcionalidades

- **Acesso Multiperfil e Segurança:** seleção de perfil (Administrador, Professor e Aluno), autenticação independente via Firebase Authentication, persistência de sessão e isolamento de dados por vínculo profissional;
- **Gestão Completa de Alunos:** cadastro, consulta, edição, exclusão segura, pesquisa e filtros avançados por situação de acompanhamento;
- **Anamnese Digital e Documentos:** criação de modelos padrão e personalizados, atribuição ao aluno com controle de status, preenchimento pelo app, vínculo automático com condições e restrições clínicas e anexo privado de documentos médicos e imagens de comparação;
- **Linha do Tempo e Evolução:** histórico cronológico individual com registro e filtragem de dores, avaliações físicas, treinos e contatos profissionais, acompanhado de gráficos de evolução;
- **Central de Atenção Automatizada:** triagem inteligente baseada em sete regras clínicas e operacionais (baixa frequência, dor recorrente, avaliações pendentes/atrasadas, ausência de contato, vencimento de treino e condições/restrições ativas), classificando os alunos em *Normal*, *Atenção* ou *Prioridade*;
- **Treinos, Vídeos e Periodização:** prescrição de fichas de treino com suporte a vídeos demonstrativos (links externos ou arquivos locais) e estrutura hierárquica opcional de periodização (macrociclos, mesociclos, microciclos e sessões semanais);
- **Notificações e Experiência Visual:** alertas nativos integrados ao Firebase Cloud Messaging, painel de notificações no Dashboard, indicadores de frequência da carteira e suporte ao Tema Dark.

## Tecnologias

O **Ícaro's FitCare** é desenvolvido para a plataforma Android utilizando:

- **Android Studio:** ambiente de desenvolvimento, compilação e depuração;
- **Kotlin:** linguagem principal de programação;
- **Jetpack Compose:** construção das interfaces declarativas e suporte aos temas Claro e Dark;
- **SQLite / Room:** persistência local imediata e operação offline;
- **Firebase (Authentication, Cloud Firestore e Cloud Messaging):** autenticação multiperfil, sincronização em tempo real entre dispositivos e notificações nativas;
- **WorkManager e FileProvider:** processamento assíncrono da fila de sincronização e gerenciamento privado de arquivos médicos e mídias;
- **Figma:** prototipação inicial das interfaces;
- **Git e GitHub:** controle de versões e histórico evolutivo do código-fonte.

## Desenvolvimento

O projeto está organizado em quatro sprints, com desenvolvimento incremental e acompanhamento das atividades por meio de quadro Kanban (Trello) e versionamento contínuo no GitHub.

### Planejamento das sprints

**Sprint 01 — Estrutura inicial e interfaces**

Concluída. Configuração inicial do projeto mobile com Kotlin e Jetpack Compose, estruturação da navegação e desenvolvimento das primeiras interfaces, incluindo login, dashboard, listagem e perfil do aluno.

**Sprint 02 — Gerenciamento, persistência local e classificação inicial**

Concluída. Implementação da persistência local com Room e desenvolvimento das funcionalidades de cadastro, consulta, edição e exclusão de alunos, filtros de busca, checklist de condições e restrições físicas, regras iniciais de classificação e gerenciamento básico de treinos e exercícios.

**Sprint 03 — Inteligência de acompanhamento, acesso multiperfil e arquitetura híbrida**

Concluída. Implementação da Linha do Tempo individual, módulo de anamnese digital, anexação privada de documentos médicos e fotos de comparação, gráficos de evolução, Central de Atenção automatizada com sete regras de triagem, associação de vídeos aos exercícios e periodização hierárquica opcional. O ciclo também antecipou a implementação do sistema de acesso multiperfil (Administrador, Professor e Aluno) e a evolução para a arquitetura híbrida (Room + Firebase Auth, Cloud Firestore e FCM) com otimização de cota e notificações nativas.

**Sprint 04 — Finalização, refinamentos de UX/UI e validação integrada**

Em andamento. Conclusão do fluxo obrigatório de anamnese padrão no primeiro acesso do aluno, unificação integral do checklist clínico, consulta e atualização de cargas/observações nos treinos pela área do aluno, catálogo pesquisável de exercícios predefinidos/personalizados, visualização em calendário e seletor gráfico de datas na periodização, extensão completa do Tema Dark com degradê nos cabeçalhos, testes integrados ponta a ponta e geração do pacote instalável APK.

## Status do projeto

**Sprint 03 concluída — Sprint 04 em andamento.**

As funcionalidades centrais de acompanhamento clínico, prescrição estruturada, acesso multiperfil e sincronização em nuvem entre dispositivos estão implementadas e validadas. O projeto encontra-se na reta final para fechamento dos últimos ajustes de usabilidade, validação integrada do MVP e geração do APK de entrega.

## Licença

O **Ícaro's FitCare** é um projeto de uso restrito, desenvolvido no contexto acadêmico e destinado à utilização prevista pelos responsáveis pela solução.

O código-fonte e os demais artefatos do projeto não podem ser copiados, modificados, distribuídos ou utilizados para outras finalidades sem autorização prévia dos responsáveis pelo projeto.

Consulte o arquivo `LICENSE` para obter informações sobre as condições de uso.