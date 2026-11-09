
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
    }

    suspend fun cadastrarProfessor(
        nome: String,
        email: String,
        senha: String
    ): UsuarioEntity {

        validarCadastro(nome, email, senha)

        val emailNormalizado = email.trim().lowercase()

        if (usuarioDao.buscarPorEmail(emailNormalizado) != null) {
            throw IllegalArgumentException("Este e-mail já está cadastrado.")
        }

        val usuario = UsuarioEntity(
            nome = nome.trim(),
            email = emailNormalizado,
            senhaHash = gerarHashSenha(senha),
            perfil = PERFIL_PROFESSOR
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

        val professorAtual = SessaoUsuario.usuarioAtual

        require(
            professorAtual != null &&
                    professorAtual.perfil == PERFIL_PROFESSOR &&
                    professorAtual.ativo
        ) {
            "Somente um professor ativo pode criar uma conta de aluno."
        }

        val emailNormalizado = email.trim().lowercase()

        if (usuarioDao.buscarPorEmail(emailNormalizado) != null) {
            throw IllegalArgumentException("Este e-mail já está cadastrado.")
        }

        if (usuarioDao.buscarPorAlunoId(alunoId) != null) {
            throw IllegalArgumentException("Este aluno já possui uma conta.")
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
                    perfilEsperado == PERFIL_ALUNO
        ) {
            "Perfil inválido."
        }

        val usuario = usuarioDao.buscarPorEmail(emailNormalizado)
            ?: throw IllegalArgumentException(
                "E-mail ou senha incorretos."
            )

        if (!usuario.ativo) {
            throw IllegalArgumentException(
                "Esta conta está desativada."
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

        SessaoUsuario.iniciar(usuario)

        return usuario
    }

    // Compatibilidade com a chamada utilizada na TelaLogin.
    suspend fun autenticar(
        email: String,
        senha: String,
        perfilEsperado: String
    ): UsuarioEntity? {

        return try {
            entrar(
                email = email,
                senha = senha,
                perfilEsperado = perfilEsperado
            )
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    suspend fun vincularAlunoAoProfessor(
        alunoUsuarioId: Long,
        professorUsuarioId: Long
    ) {

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

        require(alunoUsuarioId != professorUsuarioId) {
            "Não é possível vincular uma conta a ela mesma."
        }

        usuarioDao.vincularProfessor(
            usuarioId = alunoUsuarioId,
            professorId = professorUsuarioId
        )
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
}
