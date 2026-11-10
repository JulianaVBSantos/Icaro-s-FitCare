
package br.com.icaros.fitcare.data

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UsuarioRepository(
    private val usuarioDao: UsuarioDao
) {

    companion object {
        private const val PERFIL_PROFESSOR = "PROFESSOR"
        private const val PERFIL_ALUNO = "ALUNO"
        private const val PERFIL_ADMIN = "ADMIN"

        private const val EMAIL_PROFESSOR_INICIAL =
            "icaroalvesdasilva2005@gmail.com"

        private const val EMAIL_ADMIN_INICIAL =
            "julianavieirabsantos@gmail.com"
    }

    suspend fun criarAdminInicial(
        nome: String,
        email: String,
        senha: String,
        emailProfessorInicial: String,
        senhaProfessorInicial: String
    ): UsuarioEntity {

        validarCadastro(nome, email, senha)

        val emailNormalizado = email.trim().lowercase()
        val emailProfessorNormalizado =
            emailProfessorInicial.trim().lowercase()

        require(emailNormalizado == EMAIL_ADMIN_INICIAL) {
            "O e-mail informado não está autorizado para a conta administrativa."
        }

        require(
            emailProfessorNormalizado == EMAIL_PROFESSOR_INICIAL
        ) {
            "Informe o e-mail do professor inicial."
        }

        require(senhaProfessorInicial.isNotBlank()) {
            "Informe a senha do professor inicial."
        }

        val professorInicial =
            usuarioDao.buscarPorEmail(EMAIL_PROFESSOR_INICIAL)
                ?: throw IllegalArgumentException(
                    "A conta do professor inicial não foi encontrada."
                )

        require(
            professorInicial.perfil == PERFIL_PROFESSOR &&
                    professorInicial.ativo
        ) {
            "A conta do professor inicial precisa estar ativa."
        }

        require(
            verificarSenha(
                senhaProfessorInicial,
                professorInicial.senhaHash
            )
        ) {
            "A autenticação do professor inicial falhou."
        }

        require(
            usuarioDao.buscarPorEmail(EMAIL_ADMIN_INICIAL) == null
        ) {
            "A conta administrativa inicial já foi criada."
        }

        val admin = UsuarioEntity(
            nome = nome.trim(),
            email = EMAIL_ADMIN_INICIAL,
            senhaHash = gerarHashSenha(senha),
            perfil = PERFIL_ADMIN,
            ativo = true
        )

        val id = usuarioDao.inserirAdminInicial(admin)
        val adminCriado = admin.copy(id = id)

        SessaoUsuario.iniciar(adminCriado)

        return adminCriado
    }

    suspend fun cadastrarProfessor(
        nome: String,
        email: String,
        senha: String
    ): UsuarioEntity {

        validarCadastro(nome, email, senha)

        val emailNormalizado = email.trim().lowercase()

        require(usuarioDao.buscarPorEmail(emailNormalizado) == null) {
            "Este e-mail já está cadastrado."
        }

        val usuario = UsuarioEntity(
            nome = nome.trim(),
            email = emailNormalizado,
            senhaHash = gerarHashSenha(senha),
            perfil = PERFIL_PROFESSOR,
            ativo = false
        )

        val id = usuarioDao.inserir(usuario)

        return usuario.copy(id = id)
    }

    suspend fun cadastrarAluno(
        nome: String,
        email: String,
        senha: String,
        alunoId: Long
    ): UsuarioEntity {

        validarCadastro(nome, email, senha)

        require(alunoId > 0) {
            "É necessário informar um cadastro de aluno válido."
        }

        val professorAtual = exigirProfessorAtivo()

        require(
            usuarioDao.alunoPertenceAoProfessor(
                alunoId,
                professorAtual.id
            )
        ) {
            "Você não tem permissão para criar acesso para este aluno."
        }

        val emailNormalizado = email.trim().lowercase()

        require(usuarioDao.buscarPorEmail(emailNormalizado) == null) {
            "Este e-mail já está cadastrado."
        }

        require(usuarioDao.buscarPorAlunoId(alunoId) == null) {
            "Este aluno já possui uma conta."
        }

        val usuario = UsuarioEntity(
            nome = nome.trim(),
            email = emailNormalizado,
            senhaHash = gerarHashSenha(senha),
            perfil = PERFIL_ALUNO,
            alunoId = alunoId,
            professorId = professorAtual.id
        )

        val id = usuarioDao.inserir(usuario)

        return usuario.copy(id = id)
    }

    suspend fun entrar(
        email: String,
        senha: String,
        perfilEsperado: String
    ): UsuarioEntity {

        val emailNormalizado = email.trim().lowercase()

        require(emailNormalizado.isNotBlank()) {
            "Informe seu e-mail."
        }

        require(senha.isNotBlank()) {
            "Informe sua senha."
        }

        require(
            perfilEsperado == PERFIL_PROFESSOR ||
                    perfilEsperado == PERFIL_ALUNO ||
                    perfilEsperado == PERFIL_ADMIN
        ) {
            "Perfil inválido."
        }

        val usuario = usuarioDao.buscarPorEmail(emailNormalizado)
            ?: throw IllegalArgumentException(
                "E-mail ou senha incorretos."
            )

        if (!usuario.ativo) {
            throw IllegalArgumentException(
                "Esta conta está desativada ou aguarda autorização."
            )
        }

        if (usuario.perfil != perfilEsperado) {
            throw IllegalArgumentException(
                "Esta conta não pertence ao perfil selecionado."
            )
        }

        if (!verificarSenha(senha, usuario.senhaHash)) {
            throw IllegalArgumentException(
                "E-mail ou senha incorretos."
            )
        }

        when (usuario.perfil) {
            PERFIL_ALUNO -> {
                val alunoId = usuario.alunoId
                    ?: throw IllegalArgumentException(
                        "Esta conta de aluno não possui cadastro vinculado."
                    )

                val professorId = usuario.professorId
                    ?: throw IllegalArgumentException(
                        "Esta conta não possui professor responsável."
                    )

                if (
                    !usuarioDao.alunoPertenceAoProfessor(
                        alunoId,
                        professorId
                    )
                ) {
                    throw IllegalArgumentException(
                        "O vínculo desta conta de aluno é inválido."
                    )
                }

                val professor = usuarioDao.buscarPorId(professorId)

                if (
                    professor == null ||
                    professor.perfil != PERFIL_PROFESSOR ||
                    !professor.ativo
                ) {
                    throw IllegalArgumentException(
                        "O professor responsável por esta conta está inativo."
                    )
                }
            }

            PERFIL_PROFESSOR -> {
                if (
                    usuario.email == EMAIL_PROFESSOR_INICIAL &&
                    !usuario.ativo
                ) {
                    throw IllegalArgumentException(
                        "A conta do professor inicial está inativa."
                    )
                }
            }

            PERFIL_ADMIN -> {
                if (usuario.email != EMAIL_ADMIN_INICIAL) {
                    throw IllegalArgumentException(
                        "Conta administrativa inválida."
                    )
                }
            }
        }

        SessaoUsuario.iniciar(usuario)

        return usuario
    }

    suspend fun autenticar(
        email: String,
        senha: String,
        perfilEsperado: String
    ): UsuarioEntity? {
        return try {
            entrar(email, senha, perfilEsperado)
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: IllegalStateException) {
            null
        }
    }

    suspend fun buscarPorId(id: Long): UsuarioEntity? {
        if (id <= 0) return null
        return usuarioDao.buscarPorId(id)
    }

    suspend fun buscarUsuarioAtivoPorId(
        id: Long
    ): UsuarioEntity? {

        if (id <= 0) return null

        val usuario = usuarioDao.buscarPorId(id)
            ?: return null

        if (!usuario.ativo) return null

        if (
            usuario.perfil !in listOf(
                PERFIL_PROFESSOR,
                PERFIL_ALUNO,
                PERFIL_ADMIN
            )
        ) return null

        if (usuario.perfil == PERFIL_ALUNO) {
            val alunoId = usuario.alunoId ?: return null
            val professorId = usuario.professorId ?: return null

            if (
                !usuarioDao.alunoPertenceAoProfessor(
                    alunoId,
                    professorId
                )
            ) return null

            val professor = usuarioDao.buscarPorId(professorId)
                ?: return null

            if (
                professor.perfil != PERFIL_PROFESSOR ||
                !professor.ativo
            ) return null
        }

        if (
            usuario.perfil == PERFIL_ADMIN &&
            usuario.email != EMAIL_ADMIN_INICIAL
        ) return null

        return usuario
    }

    suspend fun listarProfissionaisParaAdmin(): List<UsuarioEntity> {
        exigirAdminAtivo()
        return usuarioDao.listarProfissionaisParaAdmin()
    }

    suspend fun alterarStatusProfissional(
        professorId: Long,
        ativo: Boolean
    ) {

        exigirAdminAtivo()

        require(professorId > 0) {
            "Professor inválido."
        }

        val professor = usuarioDao.buscarPorId(professorId)
            ?: throw IllegalArgumentException("Professor não encontrado.")

        require(professor.perfil == PERFIL_PROFESSOR) {
            "A conta informada não pertence a um professor."
        }

        require(
            professor.email != EMAIL_PROFESSOR_INICIAL || ativo
        ) {
            "A conta do professor inicial não pode ser bloqueada."
        }

        val alterados = usuarioDao.atualizarStatus(
            id = professorId,
            ativo = ativo
        )

        check(alterados == 1) {
            "Não foi possível atualizar a autorização do professor."
        }
    }

    suspend fun redefinirSenhaProfissional(
        professorId: Long,
        novaSenha: String
    ) {

        exigirAdminAtivo()

        require(professorId > 0) {
            "Professor inválido."
        }

        require(novaSenha.length >= 8) {
            "A senha deve ter pelo menos 8 caracteres."
        }

        val professor = usuarioDao.buscarPorId(professorId)
            ?: throw IllegalArgumentException("Professor não encontrado.")

        require(professor.perfil == PERFIL_PROFESSOR) {
            "A conta informada não pertence a um professor."
        }

        val alterados = usuarioDao.atualizarSenha(
            id = professorId,
            senhaHash = gerarHashSenha(novaSenha)
        )

        check(alterados == 1) {
            "Não foi possível redefinir a senha."
        }
    }

    suspend fun excluirContaProfissional(
        professorId: Long
    ) {

        exigirAdminAtivo()

        require(professorId > 0) {
            "Professor inválido."
        }

        val professor = usuarioDao.buscarPorId(professorId)
            ?: throw IllegalArgumentException("Professor não encontrado.")

        require(professor.perfil == PERFIL_PROFESSOR) {
            "Somente contas profissionais podem ser excluídas."
        }

        require(professor.email != EMAIL_PROFESSOR_INICIAL) {
            "A conta do professor inicial não pode ser excluída."
        }

        require(
            !usuarioDao.professorPossuiAlunos(professorId)
        ) {
            "Transfira os alunos deste professor antes de excluir sua conta."
        }

        val excluidos = usuarioDao.excluirProfessor(professorId)

        check(excluidos == 1) {
            "Não foi possível excluir a conta profissional."
        }
    }

    suspend fun vincularAlunoAoProfessor(
        alunoUsuarioId: Long,
        professorUsuarioId: Long
    ) {

        exigirAdminAtivo()

        require(alunoUsuarioId > 0 && professorUsuarioId > 0) {
            "Informe contas válidas."
        }

        val aluno = usuarioDao.buscarPorId(alunoUsuarioId)
            ?: throw IllegalArgumentException("Aluno não encontrado.")

        val professor = usuarioDao.buscarPorId(professorUsuarioId)
            ?: throw IllegalArgumentException("Professor não encontrado.")

        require(aluno.perfil == PERFIL_ALUNO) {
            "O usuário informado não é um aluno."
        }

        require(professor.perfil == PERFIL_PROFESSOR) {
            "O usuário informado não é um professor."
        }

        require(aluno.ativo && professor.ativo) {
            "Não é possível vincular contas inativas."
        }

        val alunoId = aluno.alunoId
            ?: throw IllegalArgumentException(
                "A conta não possui cadastro de aluno vinculado."
            )

        val alterados = usuarioDao.vincularProfessorCompleto(
            usuarioId = alunoUsuarioId,
            alunoId = alunoId,
            professorId = professorUsuarioId
        )

        check(alterados) {
            "Não foi possível atualizar o vínculo do aluno."
        }
    }

    suspend fun alunoPossuiAcesso(alunoId: Long): Boolean {

        require(alunoId > 0) {
            "Aluno inválido."
        }

        val professor = exigirProfessorAtivo()

        require(
            usuarioDao.alunoPertenceAoProfessor(
                alunoId,
                professor.id
            )
        ) {
            "Você não tem permissão para gerenciar o acesso deste aluno."
        }

        val usuarioAluno = usuarioDao.buscarPorAlunoId(alunoId)
            ?: return false

        if (usuarioAluno.professorId != professor.id) {
            throw IllegalStateException(
                "O vínculo da conta do aluno não corresponde ao professor."
            )
        }

        return true
    }

    suspend fun redefinirSenhaDoAluno(
        alunoId: Long,
        novaSenha: String
    ) {

        val professor = exigirProfessorAtivo()

        require(novaSenha.length >= 8) {
            "A senha deve ter pelo menos 8 caracteres."
        }

        require(
            usuarioDao.alunoPertenceAoProfessor(
                alunoId,
                professor.id
            )
        ) {
            "Você não tem permissão para alterar a senha deste aluno."
        }

        val usuarioAluno = usuarioDao.buscarPorAlunoId(alunoId)
            ?: throw IllegalStateException(
                "Este aluno ainda não possui uma conta de acesso."
            )

        require(usuarioAluno.professorId == professor.id) {
            "Você não tem permissão para alterar a senha deste aluno."
        }

        val alterados = usuarioDao.atualizarSenha(
            id = usuarioAluno.id,
            senhaHash = gerarHashSenha(novaSenha)
        )

        check(alterados == 1) {
            "Não foi possível atualizar a senha do aluno."
        }
    }

    private suspend fun exigirProfessorAtivo(): UsuarioEntity {

        val sessao = SessaoUsuario.usuarioAtual
            ?: throw IllegalStateException(
                "Nenhum professor está conectado."
            )

        require(sessao.perfil == PERFIL_PROFESSOR) {
            "Operação permitida somente para professores."
        }

        val professor = usuarioDao.buscarPorId(sessao.id)
            ?: throw IllegalStateException(
                "Conta profissional não encontrada."
            )

        require(
            professor.perfil == PERFIL_PROFESSOR &&
                    professor.ativo
        ) {
            "Esta conta profissional está inativa."
        }

        return professor
    }

    private suspend fun exigirAdminAtivo(): UsuarioEntity {

        val sessao = SessaoUsuario.usuarioAtual
            ?: throw IllegalStateException(
                "Nenhum administrador está conectado."
            )

        require(sessao.perfil == PERFIL_ADMIN) {
            "Operação permitida somente ao administrador."
        }

        val admin = usuarioDao.buscarPorId(sessao.id)
            ?: throw IllegalStateException(
                "Conta administrativa não encontrada."
            )

        require(
            admin.perfil == PERFIL_ADMIN &&
                    admin.ativo &&
                    admin.email == EMAIL_ADMIN_INICIAL
        ) {
            "A conta administrativa está inativa ou inválida."
        }

        return admin
    }

    fun sair() {
        SessaoUsuario.encerrar()
    }

    private fun validarCadastro(
        nome: String,
        email: String,
        senha: String
    ) {

        require(nome.trim().length >= 2) {
            "Informe o nome completo."
        }

        require(
            android.util.Patterns.EMAIL_ADDRESS
                .matcher(email.trim())
                .matches()
        ) {
            "Informe um e-mail válido."
        }

        require(senha.length >= 8) {
            "A senha deve possuir pelo menos 8 caracteres."
        }
    }

    private fun gerarHashSenha(senha: String): String {

        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val hash = derivarSenha(senha, salt)

        val saltBase64 = Base64.encodeToString(
            salt,
            Base64.NO_WRAP
        )

        val hashBase64 = Base64.encodeToString(
            hash,
            Base64.NO_WRAP
        )

        return "PBKDF2_SHA1_120000:$saltBase64:$hashBase64"
    }

    private fun verificarSenha(
        senha: String,
        senhaHash: String
    ): Boolean {

        return try {
            val partes = senhaHash.split(":")

            if (
                partes.size != 3 ||
                partes[0] != "PBKDF2_SHA1_120000"
            ) {
                return false
            }

            val salt = Base64.decode(
                partes[1],
                Base64.NO_WRAP
            )

            val hashOriginal = Base64.decode(
                partes[2],
                Base64.NO_WRAP
            )

            val hashInformado = derivarSenha(senha, salt)

            MessageDigest.isEqual(
                hashOriginal,
                hashInformado
            )
        } catch (e: Exception) {
            false
        }
    }

    private fun derivarSenha(
        senha: String,
        salt: ByteArray
    ): ByteArray {

        val spec = PBEKeySpec(
            senha.toCharArray(),
            salt,
            120000,
            256
        )

        return try {
            SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA1")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }
    suspend fun existeAdministrador(): Boolean {
        return usuarioDao.contarAdmins() > 0
    }
}