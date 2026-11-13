package br.com.icaros.fitcare.data

import android.content.Context
import android.util.Base64
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UsuarioRepository(
    private val usuarioDao: UsuarioDao,
    private val context: Context
) {

    companion object {
        private const val PERFIL_PROFESSOR = "PROFESSOR"
        private const val PERFIL_ALUNO = "ALUNO"
        private const val PERFIL_ADMIN = "ADMIN"

        private const val UID_ADMIN_AUTORIZADO = "Bcqsue17eVVdJ4Ajc9EhoTQfJ8b2"
        private const val NOME_FIREBASE_APP_SECUNDARIO = "FitCareTeacherProvisioning"

        private const val EMAIL_PROFESSOR_INICIAL =
            "icaroalvesdasilva2005@gmail.com"

        private const val EMAIL_ADMIN_INICIAL =
            "julianavieirabsantos@gmail.com"
    }

    // FIREBASE AUTHENTICATION

    private suspend fun autenticarFirebase(
        usuario: UsuarioEntity,
        senha: String
    ): UsuarioEntity {

        val auth = FirebaseAuth.getInstance()

        val firebaseUser = if (usuario.firebaseUid.isNullOrBlank()) {
            try {
                auth.createUserWithEmailAndPassword(
                    usuario.email,
                    senha
                ).await().user
            } catch (e: FirebaseAuthUserCollisionException) {
                auth.signInWithEmailAndPassword(
                    usuario.email,
                    senha
                ).await().user
            }
        } else {
            auth.signInWithEmailAndPassword(
                usuario.email,
                senha
            ).await().user
        } ?: throw IllegalStateException(
            "O Firebase não retornou os dados da conta."
        )

        val uid = firebaseUser.uid

        try {
            if (
                !usuario.firebaseUid.isNullOrBlank() &&
                usuario.firebaseUid != uid
            ) {
                throw IllegalStateException(
                    "A identificação da conta no Firebase não corresponde ao cadastro local."
                )
            }

            var usuarioAtualizado = usuario

            if (usuario.firebaseUid.isNullOrBlank()) {
                val alterados = usuarioDao.atualizarFirebaseUid(
                    usuarioId = usuario.id,
                    firebaseUid = uid
                )

                check(alterados == 1) {
                    "Não foi possível associar a conta ao Firebase no banco local."
                }

                usuarioAtualizado = usuario.copy(firebaseUid = uid)
            }

            if (usuarioAtualizado.perfil == PERFIL_ALUNO) {
                vincularAlunoAoFirebase(
                    usuario = usuarioAtualizado,
                    uid = uid
                )
            }

            return usuarioAtualizado

        } catch (e: Exception) {
            auth.signOut()
            throw e
        }
    }

    /**
     * Vincula a conta autenticada do aluno ao documento remoto.
     *
     * O documento precisa existir previamente na coleção alunos.
     * O UID nunca é associado apenas pela existência do e-mail:
     * também é verificado se já existe outro vínculo.
     */
    private suspend fun vincularAlunoAoFirebase(
        usuario: UsuarioEntity,
        uid: String
    ) {
        require(usuario.perfil == PERFIL_ALUNO) {
            "Operação permitida somente para alunos."
        }

        require(usuario.email.isNotBlank()) {
            "O aluno não possui e-mail cadastrado."
        }

        require(uid.isNotBlank()) {
            "UID Firebase inválido."
        }

        val emailNormalizado = usuario.email.trim().lowercase()

        val resultado = FirebaseFirestore.getInstance()
            .collection("alunos")
            .whereEqualTo("email", emailNormalizado)
            .get()
            .await()

        require(resultado.documents.size == 1) {
            "Não foi encontrado um cadastro remoto único para este aluno. Sincronize o cadastro com o professor."
        }

        val documento = resultado.documents.first()

        val emailRemoto = documento.getString("email")
            ?.trim()
            ?.lowercase()

        require(emailRemoto == emailNormalizado) {
            "O e-mail do cadastro remoto não corresponde ao aluno autenticado."
        }

        val uidExistente = documento.getString("alunoAuthUid")

        require(uidExistente.isNullOrBlank() || uidExistente == uid) {
            "Este cadastro de aluno já está vinculado a outra conta Firebase."
        }

        if (uidExistente.isNullOrBlank()) {
            documento.reference.update(
                "alunoAuthUid",
                uid
            ).await()
        }
    }

    // LOGIN DO PROFESSOR PELO FIREBASE

    private suspend fun entrarProfessorFirebase(
        email: String,
        senha: String
    ): UsuarioEntity {

        val auth = FirebaseAuth.getInstance()

        val firebaseUser = auth.signInWithEmailAndPassword(
            email,
            senha
        ).await().user ?: throw IllegalStateException(
            "O Firebase não retornou os dados da conta."
        )

        val uid = firebaseUser.uid

        try {
            val documento = FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .get()
                .await()

            if (!documento.exists()) {
                throw IllegalStateException(
                    "Não existe autorização cadastrada para esta conta."
                )
            }

            val perfilRemoto = documento.getString("perfil")
            val ativoRemoto = documento.getBoolean("ativo")

            if (
                perfilRemoto != PERFIL_PROFESSOR ||
                ativoRemoto != true
            ) {
                throw IllegalStateException(
                    "Esta conta não possui autorização ativa para acessar o perfil de professor."
                )
            }

            val usuarioPorUid =
                usuarioDao.buscarPorFirebaseUid(uid)

            val usuarioPorEmail =
                usuarioDao.buscarPorEmail(email)

            if (
                usuarioPorUid != null &&
                usuarioPorEmail != null &&
                usuarioPorUid.id != usuarioPorEmail.id
            ) {
                throw IllegalStateException(
                    "Existem cadastros locais conflitantes para esta conta."
                )
            }

            val usuarioLocal = usuarioPorUid ?: usuarioPorEmail

            val usuarioFinal = if (usuarioLocal != null) {

                if (usuarioLocal.perfil != PERFIL_PROFESSOR) {
                    throw IllegalStateException(
                        "O cadastro local não pertence ao perfil de professor."
                    )
                }

                if (usuarioLocal.email != email) {
                    throw IllegalStateException(
                        "O e-mail do cadastro local não corresponde à conta autenticada."
                    )
                }

                if (
                    !usuarioLocal.firebaseUid.isNullOrBlank() &&
                    usuarioLocal.firebaseUid != uid
                ) {
                    throw IllegalStateException(
                        "Este e-mail já está associado a outra conta Firebase."
                    )
                }

                if (!usuarioLocal.ativo) {
                    val alterados = usuarioDao.atualizarStatus(
                        id = usuarioLocal.id,
                        ativo = true
                    )

                    check(alterados == 1) {
                        "Não foi possível atualizar o status local do professor."
                    }
                }

                if (usuarioLocal.firebaseUid.isNullOrBlank()) {
                    val alterados = usuarioDao.atualizarFirebaseUid(
                        usuarioId = usuarioLocal.id,
                        firebaseUid = uid
                    )

                    check(alterados == 1) {
                        "Não foi possível vincular a conta ao Firebase."
                    }
                }

                usuarioLocal.copy(
                    ativo = true,
                    firebaseUid = uid
                )

            } else {

                val novoUsuario = UsuarioEntity(
                    nome = firebaseUser.displayName
                        ?.takeIf { it.isNotBlank() }
                        ?: email.substringBefore("@"),
                    email = email,
                    senhaHash = "",
                    perfil = PERFIL_PROFESSOR,
                    ativo = true,
                    firebaseUid = uid
                )

                val novoId = usuarioDao.inserir(novoUsuario)

                novoUsuario.copy(id = novoId)
            }

            SessaoUsuario.iniciar(usuarioFinal)

            return usuarioFinal

        } catch (e: Exception) {
            auth.signOut()
            throw e
        }
    }

    private fun obterFirebaseAppSecundario(): FirebaseApp {
        return try {
            FirebaseApp.getInstance(NOME_FIREBASE_APP_SECUNDARIO)
        } catch (_: IllegalStateException) {
            FirebaseApp.initializeApp(
                context.applicationContext,
                FirebaseApp.getInstance().options,
                NOME_FIREBASE_APP_SECUNDARIO
            ) ?: throw IllegalStateException(
                "Não foi possível inicializar o Firebase Authentication secundário."
            )
        }
    }

    // LOGIN ADMINISTRATIVO

    private suspend fun entrarAdminFirebase(
        email: String,
        senha: String
    ): UsuarioEntity {

        val auth = FirebaseAuth.getInstance()

        val firebaseUser = try {
            auth.signInWithEmailAndPassword(email, senha).await().user
        } catch (e: Exception) {
            auth.signOut()
            throw e
        } ?: throw IllegalStateException(
            "O Firebase Authentication não retornou a conta administrativa."
        )

        try {
            require(firebaseUser.uid == UID_ADMIN_AUTORIZADO) {
                "Esta conta não está autorizada como administradora do Ícaros FitCare."
            }

            require(email == EMAIL_ADMIN_INICIAL) {
                "E-mail administrativo inválido."
            }

            val usuarioPorEmail =
                usuarioDao.buscarPorEmail(EMAIL_ADMIN_INICIAL)

            val usuarioPorUid =
                usuarioDao.buscarPorFirebaseUid(UID_ADMIN_AUTORIZADO)

            val usuarioLocal = usuarioPorEmail ?: usuarioPorUid

            val usuarioAutenticado = if (usuarioLocal != null) {

                require(
                    usuarioLocal.perfil == PERFIL_ADMIN &&
                            usuarioLocal.ativo &&
                            usuarioLocal.email == EMAIL_ADMIN_INICIAL
                ) {
                    "Já existe um cadastro local incompatível com a conta administrativa."
                }

                require(
                    usuarioLocal.firebaseUid.isNullOrBlank() ||
                            usuarioLocal.firebaseUid == UID_ADMIN_AUTORIZADO
                ) {
                    "O cadastro administrativo está vinculado a outro UID do Firebase."
                }

                if (usuarioLocal.firebaseUid.isNullOrBlank()) {
                    val atualizados = usuarioDao.atualizarFirebaseUid(
                        usuarioId = usuarioLocal.id,
                        firebaseUid = UID_ADMIN_AUTORIZADO
                    )

                    check(atualizados == 1) {
                        "Não foi possível vincular o administrador ao Firebase."
                    }
                }

                usuarioLocal.copy(
                    firebaseUid = UID_ADMIN_AUTORIZADO
                )

            } else {

                val novoAdmin = UsuarioEntity(
                    nome = firebaseUser.displayName
                        ?.takeIf { it.isNotBlank() }
                        ?: "Administrador",
                    email = EMAIL_ADMIN_INICIAL,
                    senhaHash = gerarHashSenha(senha),
                    perfil = PERFIL_ADMIN,
                    ativo = true,
                    firebaseUid = UID_ADMIN_AUTORIZADO
                )

                val novoId = usuarioDao.inserir(novoAdmin)

                novoAdmin.copy(id = novoId)
            }

            registrarConfiguracaoAdmin(UID_ADMIN_AUTORIZADO)
            SessaoUsuario.iniciar(usuarioAutenticado)

            return usuarioAutenticado

        } catch (e: Exception) {
            auth.signOut()
            throw e
        }
    }

    private suspend fun registrarConfiguracaoAdmin(uid: String) {
        require(uid == UID_ADMIN_AUTORIZADO) {
            "UID administrativo não autorizado."
        }

        FirebaseFirestore.getInstance()
            .collection("configuracao")
            .document("admin")
            .set(
                mapOf(
                    "uid" to UID_ADMIN_AUTORIZADO,
                    "configurado" to true
                )
            )
            .await()
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

        require(emailProfessorNormalizado == EMAIL_PROFESSOR_INICIAL) {
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

        val adminAutenticado = autenticarFirebase(
            usuario = adminCriado,
            senha = senha
        )

        require(adminAutenticado.firebaseUid == UID_ADMIN_AUTORIZADO) {
            "A conta autenticada não corresponde ao UID administrativo autorizado."
        }

        registrarConfiguracaoAdmin(UID_ADMIN_AUTORIZADO)
        SessaoUsuario.iniciar(adminAutenticado)

        return adminAutenticado
    }

    // CADASTRO DE PROFESSOR

    suspend fun cadastrarProfessor(
        nome: String,
        email: String,
        senha: String
    ): UsuarioEntity {

        exigirAdminAtivo()
        validarCadastro(nome, email, senha)

        val emailNormalizado = email.trim().lowercase()

        require(
            FirebaseAuth.getInstance().currentUser?.uid == UID_ADMIN_AUTORIZADO
        ) {
            "A sessão administrativa do Firebase não está autorizada para cadastrar professores."
        }

        require(
            usuarioDao.buscarPorEmail(emailNormalizado) == null
        ) {
            "Este e-mail já está cadastrado."
        }

        val appSecundario = obterFirebaseAppSecundario()
        val authSecundario = FirebaseAuth.getInstance(appSecundario)

        var contaCriadaAgora = false

        val firebaseUser = try {
            authSecundario.createUserWithEmailAndPassword(
                emailNormalizado,
                senha
            ).await().user.also {
                contaCriadaAgora = true
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            authSecundario.signInWithEmailAndPassword(
                emailNormalizado,
                senha
            ).await().user
        } ?: throw IllegalStateException(
            "O Firebase Authentication não retornou a conta do professor."
        )

        val uidProfessor = firebaseUser.uid

        try {
            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uidProfessor)
                .set(
                    mapOf(
                        "nome" to nome.trim(),
                        "email" to emailNormalizado,
                        "perfil" to PERFIL_PROFESSOR,
                        "ativo" to true,
                        "dataCadastro" to System.currentTimeMillis(),
                        "criadoPor" to UID_ADMIN_AUTORIZADO
                    )
                )
                .await()

            val usuario = UsuarioEntity(
                nome = nome.trim(),
                email = emailNormalizado,
                senhaHash = gerarHashSenha(senha),
                perfil = PERFIL_PROFESSOR,
                ativo = true,
                firebaseUid = uidProfessor
            )

            val id = usuarioDao.inserir(usuario)

            return usuario.copy(id = id)

        } catch (e: Exception) {
            if (contaCriadaAgora) {
                try {
                    authSecundario.currentUser?.delete()?.await()
                } catch (_: Exception) {
                    // Mantém o erro original.
                }
            }

            throw e

        } finally {
            authSecundario.signOut()
        }
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

        if (perfilEsperado == PERFIL_PROFESSOR) {
            return entrarProfessorFirebase(
                email = emailNormalizado,
                senha = senha
            )
        }

        if (perfilEsperado == PERFIL_ADMIN) {
            return entrarAdminFirebase(
                email = emailNormalizado,
                senha = senha
            )
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

        if (
            usuario.firebaseUid.isNullOrBlank() &&
            !verificarSenha(senha, usuario.senhaHash)
        ) {
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
        }

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

        val firestore = FirebaseFirestore.getInstance()

        val resultado = firestore
            .collection("usuarios")
            .whereEqualTo("perfil", PERFIL_PROFESSOR)
            .get()
            .await()

        for (documento in resultado.documents) {

            val uid = documento.id

            val nome = documento.getString("nome")
                ?.trim()
                .orEmpty()

            val email = documento.getString("email")
                ?.trim()
                ?.lowercase()
                .orEmpty()

            val ativo = documento.getBoolean("ativo") ?: false

            if (uid.isBlank() || nome.isBlank() || email.isBlank()) {
                android.util.Log.w(
                    "FitCareSync",
                    "Professor ignorado. Documento: ${documento.id}. Dados: ${documento.data}"
                )
                continue
            }

            val usuarioPorUid =
                usuarioDao.buscarPorFirebaseUid(uid)

            val usuarioPorEmail =
                usuarioDao.buscarPorEmail(email)

            require(
                usuarioPorUid == null ||
                        usuarioPorEmail == null ||
                        usuarioPorUid.id == usuarioPorEmail.id
            ) {
                "Existem cadastros locais conflitantes para o professor $email."
            }

            val usuarioLocal = usuarioPorUid ?: usuarioPorEmail

            if (usuarioLocal != null) {

                require(usuarioLocal.perfil == PERFIL_PROFESSOR) {
                    "O e-mail $email já pertence a outro perfil."
                }

                require(
                    usuarioLocal.firebaseUid.isNullOrBlank() ||
                            usuarioLocal.firebaseUid == uid
                ) {
                    "O professor $email está vinculado a outra conta Firebase."
                }

                val atualizado =
                    usuarioDao.atualizarProfessorComDadosFirebase(
                        usuarioId = usuarioLocal.id,
                        nome = nome,
                        email = email,
                        ativo = ativo,
                        firebaseUid = uid
                    )

                check(atualizado == 1) {
                    "Não foi possível atualizar o professor $email no banco local."
                }

            } else {

                val novoProfessor = UsuarioEntity(
                    nome = nome,
                    email = email,
                    senhaHash = "",
                    perfil = PERFIL_PROFESSOR,
                    ativo = ativo,
                    firebaseUid = uid
                )

                usuarioDao.inserir(novoProfessor)
            }
        }

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

        professor.firebaseUid
            ?.takeIf { it.isNotBlank() }
            ?.let { uid ->
                FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(uid)
                    .update("ativo", ativo)
                    .await()
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

        require(!usuarioDao.professorPossuiAlunos(professorId)) {
            "Transfira os alunos deste professor antes de excluir sua conta."
        }

        professor.firebaseUid
            ?.takeIf { it.isNotBlank() }
            ?.let { uid ->
                FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(uid)
                    .update("ativo", false)
                    .await()
            }

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

        if (usuarioDao.contarAdmins() > 0) {
            return true
        }

        return try {
            val documento = FirebaseFirestore.getInstance()
                .collection("configuracao")
                .document("admin")
                .get()
                .await()

            documento.exists()

        } catch (e: Exception) {
            true
        }
    }
}