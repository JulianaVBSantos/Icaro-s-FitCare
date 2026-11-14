package br.com.icaros.fitcare.data

import kotlinx.coroutines.flow.Flow

class PeriodizacaoRepository(
    private val dao: PeriodizacaoDao
) {

    // MACROCICLOS
    fun listarMacrociclos(alunoId: Long): Flow<List<MacrocicloEntity>> {
        val professorId = SessaoUsuario.exigirProfessorId()
        require(alunoId > 0) { "Aluno inválido." }
        return dao.listarMacrociclosDoAluno(alunoId, professorId)
    }

    suspend fun buscarMacrociclo(id: Long): MacrocicloEntity? {
        val professorId = SessaoUsuario.exigirProfessorId()
        return dao.buscarMacrocicloPorId(id, professorId)
    }

    suspend fun salvarMacrociclo(macrociclo: MacrocicloEntity): Long {
        val professorId = SessaoUsuario.exigirProfessorId()
        require(macrociclo.alunoId > 0) { "Aluno inválido." }
        require(macrociclo.nome.isNotBlank()) { "Informe o nome do macrociclo." }
        require(macrociclo.dataInicio.isNotBlank()) { "Informe a data de início do macrociclo." }
        require(dao.alunoPertenceAoProfessor(macrociclo.alunoId, professorId)) {
            "Este aluno não pertence ao seu cadastro."
        }

        return if (macrociclo.id == 0L) {
            dao.inserirMacrociclo(macrociclo)
        } else {
            val existente = dao.buscarMacrocicloPorId(macrociclo.id, professorId)
                ?: throw IllegalArgumentException("Macrociclo não encontrado ou sem permissão.")
            require(existente.alunoId == macrociclo.alunoId) {
                "Não é permitido alterar o aluno do macrociclo."
            }
            dao.atualizarMacrociclo(macrociclo)
            macrociclo.id
        }
    }

    suspend fun excluirMacrociclo(macrociclo: MacrocicloEntity) {
        val professorId = SessaoUsuario.exigirProfessorId()
        val existente = dao.buscarMacrocicloPorId(macrociclo.id, professorId)
            ?: throw IllegalArgumentException("Macrociclo não encontrado ou sem permissão.")
        dao.excluirMacrociclo(existente)
    }

    // MESOCICLOS
    fun listarMesociclos(macrocicloId: Long): Flow<List<MesocicloEntity>> {
        val professorId = SessaoUsuario.exigirProfessorId()
        require(macrocicloId > 0) { "Macrociclo inválido." }
        return dao.listarMesociclos(macrocicloId, professorId)
    }

    suspend fun buscarMesociclo(id: Long): MesocicloEntity? {
        val professorId = SessaoUsuario.exigirProfessorId()
        return dao.buscarMesocicloPorId(id, professorId)
    }

    suspend fun salvarMesociclo(mesociclo: MesocicloEntity): Long {
        val professorId = SessaoUsuario.exigirProfessorId()
        require(mesociclo.macrocicloId > 0) { "Macrociclo inválido." }
        require(mesociclo.nome.isNotBlank()) { "Informe o nome do mesociclo." }
        require(mesociclo.dataInicio.isNotBlank()) { "Informe a data de início do mesociclo." }
        require(dao.buscarMacrocicloPorId(mesociclo.macrocicloId, professorId) != null) {
            "Macrociclo não encontrado ou sem permissão."
        }

        return if (mesociclo.id == 0L) {
            dao.inserirMesociclo(mesociclo)
        } else {
            val existente = dao.buscarMesocicloPorId(mesociclo.id, professorId)
                ?: throw IllegalArgumentException("Mesociclo não encontrado ou sem permissão.")
            require(existente.macrocicloId == mesociclo.macrocicloId) {
                "Não é permitido transferir o mesociclo."
            }
            dao.atualizarMesociclo(mesociclo)
            mesociclo.id
        }
    }

    suspend fun excluirMesociclo(mesociclo: MesocicloEntity) {
        val professorId = SessaoUsuario.exigirProfessorId()
        val existente = dao.buscarMesocicloPorId(mesociclo.id, professorId)
            ?: throw IllegalArgumentException("Mesociclo não encontrado ou sem permissão.")
        dao.excluirMesociclo(existente)
    }

    // MICROCICLOS
    fun listarMicrociclos(mesocicloId: Long): Flow<List<MicrocicloEntity>> {
        val professorId = SessaoUsuario.exigirProfessorId()
        require(mesocicloId > 0) { "Mesociclo inválido." }
        return dao.listarMicrociclos(mesocicloId, professorId)
    }

    suspend fun buscarMicrociclo(id: Long): MicrocicloEntity? {
        val professorId = SessaoUsuario.exigirProfessorId()
        return dao.buscarMicrocicloPorId(id, professorId)
    }

    suspend fun salvarMicrociclo(microciclo: MicrocicloEntity): Long {
        val professorId = SessaoUsuario.exigirProfessorId()
        require(microciclo.mesocicloId > 0) { "Mesociclo inválido." }
        require(microciclo.nome.isNotBlank()) { "Informe o nome do microciclo." }
        require(microciclo.dataInicio.isNotBlank()) { "Informe a data de início do microciclo." }
        require(dao.buscarMesocicloPorId(microciclo.mesocicloId, professorId) != null) {
            "Mesociclo não encontrado ou sem permissão."
        }

        return if (microciclo.id == 0L) {
            dao.inserirMicrociclo(microciclo)
        } else {
            val existente = dao.buscarMicrocicloPorId(microciclo.id, professorId)
                ?: throw IllegalArgumentException("Microciclo não encontrado ou sem permissão.")
            require(existente.mesocicloId == microciclo.mesocicloId) {
                "Não é permitido transferir o microciclo."
            }
            dao.atualizarMicrociclo(microciclo)
            microciclo.id
        }
    }

    suspend fun excluirMicrociclo(microciclo: MicrocicloEntity) {
        val professorId = SessaoUsuario.exigirProfessorId()
        val existente = dao.buscarMicrocicloPorId(microciclo.id, professorId)
            ?: throw IllegalArgumentException("Microciclo não encontrado ou sem permissão.")
        dao.excluirMicrociclo(existente)
    }

    // SESSÕES DE PERIODIZAÇÃO
    fun listarSessoes(microcicloId: Long): Flow<List<SessaoPeriodizacaoEntity>> {
        val professorId = SessaoUsuario.exigirProfessorId()
        require(microcicloId > 0) { "Microciclo inválido." }
        return dao.listarSessoes(microcicloId, professorId)
    }

    suspend fun salvarSessao(sessao: SessaoPeriodizacaoEntity): Long {
        val professorId = SessaoUsuario.exigirProfessorId()
        require(sessao.microcicloId > 0) { "Microciclo inválido." }
        require(sessao.diaSemana.isNotBlank()) { "Selecione o dia da semana." }
        require(sessao.titulo.isNotBlank()) { "Informe o título da sessão." }

        val microciclo = dao.buscarMicrocicloPorId(sessao.microcicloId, professorId)
            ?: throw IllegalArgumentException("Microciclo não encontrado ou sem permissão.")
        val mesociclo = dao.buscarMesocicloPorId(microciclo.mesocicloId, professorId)
            ?: throw IllegalArgumentException("Mesociclo não encontrado.")
        val macrociclo = dao.buscarMacrocicloPorId(mesociclo.macrocicloId, professorId)
            ?: throw IllegalArgumentException("Macrociclo não encontrado.")

        val treinoVinculadoId = sessao.treinoId
        if (treinoVinculadoId != null && treinoVinculadoId > 0L) {
            require(
                dao.treinoPertenceAoAlunoEProfessor(
                    treinoId = treinoVinculadoId,
                    alunoId = macrociclo.alunoId,
                    professorId = professorId
                )
            ) {
                "O treino vinculado não pertence a este aluno."
            }
        }

        return if (sessao.id == 0L) {
            dao.inserirSessao(sessao)
        } else {
            val existente = dao.buscarSessaoPorId(sessao.id, professorId)
                ?: throw IllegalArgumentException("Sessão não encontrada ou sem permissão.")
            require(existente.microcicloId == sessao.microcicloId) {
                "Não é permitido transferir a sessão."
            }
            dao.atualizarSessao(sessao)
            sessao.id
        }
    }

    suspend fun excluirSessao(sessao: SessaoPeriodizacaoEntity) {
        val professorId = SessaoUsuario.exigirProfessorId()
        val existente = dao.buscarSessaoPorId(sessao.id, professorId)
            ?: throw IllegalArgumentException("Sessão não encontrada ou sem permissão.")
        dao.excluirSessao(existente)
    }
}