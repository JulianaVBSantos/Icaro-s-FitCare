
package br.com.icaros.fitcare.data

import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.tasks.await
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

    // FIREBASE AUTHENTICATION
    /**
     * Autentica uma conta já migrada ou migra uma conta antiga
     * do Room para o Firebase Authentication.
     *
     * A senha nunca é enviada como hash PBKDF2 ao Firebase.
     * O Firebase recebe somente a senha digitada pelo usuário.
     */
    private suspend fun autenticarFirebase(
        usuario: UsuarioEntity,
        senha: String
    ): UsuarioEntity {

        val auth = FirebaseAuth.getInstance()

        val firebaseUser = if (usuario.firebaseUid.isNullOrBlank()) {

            // Conta ainda não migrada.
            // A validação PBKDF2 precisa ter sido feita antes
            // de chamar este método.

            try {
                auth.createUserWithEmailAndPassword(
                    usuario.email,
                    senha
                ).await().user

            } catch (e: FirebaseAuthUserCollisionException) {

                // O e-mail já existe no Firebase.
                // Só associamos a conta se a senha também
                // autenticar com sucesso no Firebase.

                auth.signInWithEmailAndPassword(
                    usuario.email,
                    senha
                ).await().user
            }

        } else {

            // Conta já migrada: autenticação diretamente
            // pelo Firebase.
            auth.signInWithEmailAndPassword(
                usuario.email,
                senha
            ).await().user
        } ?: throw IllegalStateException(
            "O Firebase não retornou os dados da conta."
        )

        val uid = firebaseUser.uid

        // Se já existia UID local, ele deve corresponder
        // exatamente à conta autenticada.
        if (
            !usuario.firebaseUid.isNullOrBlank() &&
            usuario.firebaseUid != uid
        ) {
            auth.signOut()

            throw IllegalStateException(
                "A identificação da conta no Firebase não corresponde ao cadastro local."
            )
        }

        // Primeira migração: grava o UID no Room.
        if (usuario.firebaseUid.isNullOrBlank()) {

            val alterados = usuarioDao.atualizarFirebaseUid(
                usuarioId = usuario.id,
                firebaseUid = uid
            )

            if (alterados != 1) {
                auth.signOut()

                throw IllegalStateException(
                    "Não foi possível associar a conta ao Firebase no banco local."
                )
            }

            return usuario.copy(firebaseUid = uid)
        }

        return usuario
    }

    // CRIAÇÃO DO ADMINISTRADOR INICIAL
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

        // Cria ou autentica a conta administrativa no Firebase.
        val adminAutenticado = autenticarFirebase(
            usuario = adminCriado,
            senha = senha
        )

        SessaoUsuario.iniciar(adminAutenticado)

        return adminAutenticado
    }

    // CADASTRO DE PROFESSOR
    suspend fun cadastrarProfessor(
        nome: String,
        email: String,
        senha: String
    ): UsuarioEntity {

        // Somente um administrador ativo pode cadastrar
        // novos professores.
        exigirAdminAtivo()

        validarCadastro(nome, email, senha)

        val emailNormalizado = email.trim().lowercase()

        require(
            usuarioDao.buscarPorEmail(emailNormalizado) == null
        ) {
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

        // O professor ainda não é criado no Firebase.
        // A migração ocorrerá quando ele fizer o primeiro login,
        // depois de ser autorizado pelo administrador.
        return usuario.copy(id = id)
    }

    // CADASTRO DE ALUNO
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

        require(
            usuarioDao.buscarPorEmail(emailNormalizado) == null
        ) {
            "Este e-mail já está cadastrado."
        }

        require(
            usuarioDao.buscarPorAlunoId(alunoId) == null
        ) {
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

        // Não criar a conta Firebase aqui.
        // O FirebaseAuth compartilhado poderia trocar a sessão
        // do professor pela sessão do aluno.
        // O aluno fará a própria migração no primeiro login.

        return usuario.copy(id = id)
    }

    // LOGIN
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

        // Antes da migração, valida a senha pelo PBKDF2 local.
        // Depois da migração, o Firebase será responsável
        // pela validação da senha.
        if (
            usuario.firebaseUid.isNullOrBlank() &&
            !verificarSenha(senha, usuario.senhaHash)
        ) {
            throw IllegalArgumentException(
                "E-mail ou senha incorretos."
            )
        }

        // Mantém as verificações locais de vínculo.
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

                val professor =
                    usuarioDao.buscarPorId(professorId)

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

        // Depois de validar o usuário local, autentica no Firebase
        // ou realiza a primeira migração.
        val usuarioAutenticado = autenticarFirebase(
            usuario = usuario,
            senha = senha
        )

        SessaoUsuario.iniciar(usuarioAutenticado)

        return usuarioAutenticado
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
        } catch (e: Exception) {
            null
        }
    }

    // BUSCAS

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

    // ADMINISTRAÇÃO DE PROFESSORES
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
            ?: throw IllegalArgumentException(
                "Professor não encontrado."
            )

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
            ?: throw IllegalArgumentException(
                "Professor não encontrado."
            )

        require(professor.perfil == PERFIL_PROFESSOR) {
            "A conta informada não pertence a um professor."
        }

        // Atenção: por enquanto, altera somente o hash local.
        // A atualização da senha Firebase será feita na etapa
        // de integração com backend seguro.
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
            ?: throw IllegalArgumentException(
                "Professor não encontrado."
            )

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

        // A exclusão da conta Firebase deverá ser realizada
        // posteriormente por uma operação segura de backend.
        val excluidos = usuarioDao.excluirProfessor(professorId)

        check(excluidos == 1) {
            "Não foi possível excluir a conta profissional."
        }
    }

    // VÍNCULO ALUNO / PROFESSOR

    suspend fun vincularAlunoAoProfessor(
        alunoUsuarioId: Long,
        professorUsuarioId: Long
    ) {

        exigirAdminAtivo()

        require(alunoUsuarioId > 0 && professorUsuarioId > 0) {
            "Informe contas válidas."
        }

        val aluno = usuarioDao.buscarPorId(alunoUsuarioId)
            ?: throw IllegalArgumentException(
                "Aluno não encontrado."
            )

        val professor = usuarioDao.buscarPorId(professorUsuarioId)
            ?: throw IllegalArgumentException(
                "Professor não encontrado."
            )

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

        // Atenção: por enquanto, altera somente o hash local.
        // A atualização da senha Firebase será feita na etapa
        // de integração com backend seguro.
        val alterados = usuarioDao.atualizarSenha(
            id = usuarioAluno.id,
            senhaHash = gerarHashSenha(novaSenha)
        )

        check(alterados == 1) {
            "Não foi possível atualizar a senha do aluno."
        }
    }

    // VALIDAÇÃO DE SESSÃO
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

    // ENCERRAR SESSÃO
    fun sair() {
        FirebaseAuth.getInstance().signOut()
        SessaoUsuario.encerrar()
    }

    // VALIDAÇÃO DE CADASTRO
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

    // HASH PBKDF2
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

    // ADMINISTRADOR
    suspend fun existeAdministrador(): Boolean {
        return usuarioDao.contarAdmins() > 0
    }
}
